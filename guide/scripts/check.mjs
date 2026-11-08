// Entry point of `npm run check` (after the node:test self-tests). It reads source
// files only and never needs a build. Usage: node scripts/check.mjs [guide-root]
import {readFileSync, existsSync, globSync} from 'node:fs';
import {join} from 'node:path';
import {fileURLToPath, pathToFileURL} from 'node:url';
import {collectTextFiles} from './lib/files.mjs';
import {asciiErrors, labErrors, dependencyErrors} from './rules/hygiene.mjs';
import {testGlobs, countTestCases, zeroTestErrors} from './rules/zero-tests.mjs';
import {cssColorErrors, styleColorErrors} from './rules/colors.mjs';
import {siteRules} from './lib/site.mjs';

const SKELETON_MARKER = '{/* guide-skeleton */}';
const LOCKFILE = 'package-lock.json';
const CUSTOM_CSS = 'src/css/custom.css';

function dependencyRule(root) {
  const path = join(root, 'package.json');
  if (!existsSync(path)) return ['dependencies: package.json is missing'];
  let pkg;
  try {
    pkg = JSON.parse(readFileSync(path, 'utf8'));
  } catch {
    return ['dependencies: package.json is not valid JSON'];
  }
  return dependencyErrors(pkg);
}

// The zero-test guard: finds the files that the patterns of the check script's
// `node --test` command match and hands them to the pure rule. Returns the error
// lines and a summary of what was found.
function zeroTestRule(root) {
  const path = join(root, 'package.json');
  let script;
  try {
    script = JSON.parse(readFileSync(path, 'utf8')).scripts?.check;
  } catch {
    return {errors: ['zero-test guard: package.json is missing or not valid JSON'], summary: ''};
  }
  const matched = {};
  for (const pattern of testGlobs(script)) {
    matched[pattern] = globSync(pattern, {cwd: root})
      .sort()
      .map((file) => ({file, text: readFileSync(join(root, file), 'utf8')}));
  }
  const distinct = new Map(Object.values(matched).flat().map(({file, text}) => [file, text]));
  const cases = [...distinct.values()].reduce((sum, text) => sum + countTestCases(text), 0);
  return {
    errors: zeroTestErrors({script, matched}),
    summary: `${distinct.size} test files, ${cases} test cases`,
  };
}

// Runs every source rule against the tree under root. Returns the report lines and
// the flat error list; the caller decides the exit code.
export function runChecks(root) {
  const files = collectTextFiles(root);
  const lines = [];
  const errors = [];

  const report = (rule, ruleErrors, scanned, okDetail = `${scanned} files`) => {
    const status = ruleErrors.length === 0 ? 'ok  ' : 'FAIL';
    const detail = ruleErrors.length === 0 ? okDetail : `${ruleErrors.length} errors`;
    lines.push(`${status} ${rule} (${detail})`);
    errors.push(...ruleErrors);
  };

  // Anti-vacuity: a walker that finds nothing must never pass.
  const asciiResult =
    files.length === 0
      ? [`ascii: no text files found under ${root}`]
      : files.flatMap(({file, text}) => asciiErrors(file, text));
  report('ascii', asciiResult, files.length);

  // The lab rule skips scripts/** (the checker names the word) and the lockfile
  // (it lists third-party package names such as postcss-lab-function, which the
  // guide cannot rename). Every other text file is scanned.
  const labFiles = files.filter(({file}) => !file.startsWith('scripts/') && file !== LOCKFILE);
  report(
    'lab',
    labFiles.flatMap(({file, text}) => labErrors(file, text)),
    labFiles.length,
  );

  report('dependencies', dependencyRule(root), 1);

  // Colours: stylesheets other than custom.css, and the style props of components and pages.
  const colorFiles = files.filter(
    ({file}) => (file.endsWith('.css') && file !== CUSTOM_CSS) || /\.(tsx|jsx|mdx|md)$/.test(file),
  );
  report(
    'colors',
    colorFiles.flatMap(({file, text}) =>
      file.endsWith('.css') ? cssColorErrors(file, text) : styleColorErrors(file, text),
    ),
    colorFiles.length,
  );

  const zeroTests = zeroTestRule(root);
  report('tests', zeroTests.errors, 0, zeroTests.summary);

  // Brand, contrast and fonts. Every contrast pair is printed with its ratio.
  const site = siteRules(root, files);
  lines.push(...site.contrast.lines);
  report('contrast', site.contrast.errors, 0, site.contrast.summary);
  report('brand', site.brand.errors, 0, site.brand.summary);
  report('fonts', site.fonts.errors, 0, site.fonts.summary);

  const pending = files
    .filter(({file, text}) => file.startsWith('docs/') && text.includes(SKELETON_MARKER))
    .map(({file}) => file);
  lines.push(`pending skeleton pages: ${pending.length}`);
  for (const file of pending) lines.push(`  pending ${file}`);

  return {lines, errors};
}

function main() {
  const root = process.argv[2] ?? fileURLToPath(new URL('..', import.meta.url));
  const {lines, errors} = runChecks(root);
  for (const line of lines) console.log(line);
  for (const error of errors) console.error(error);
  if (errors.length > 0) {
    console.error(`check failed with ${errors.length} error(s)`);
    process.exit(1);
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main();
}
