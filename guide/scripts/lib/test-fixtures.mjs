// Shared fixtures for the node:test self-tests. Not a test file itself.
import assert from 'node:assert/strict';
import {mkdtempSync, mkdirSync, readFileSync, readdirSync, rmSync, writeFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join, dirname} from 'node:path';
import {fileURLToPath} from 'node:url';

const GUIDE_ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const FONT_DIR = 'src/css/fonts';

// The woff2 files of the real site, as {file name: Buffer}.
export function goodFontFiles() {
  return Object.fromEntries(
    readdirSync(join(GUIDE_ROOT, FONT_DIR))
      .filter((name) => name.endsWith('.woff2'))
      .sort()
      .map((name) => [name, readFileSync(join(GUIDE_ROOT, FONT_DIR, name))]),
  );
}

// The brand files of the real site, as {path relative to guide/: text or Buffer}: the
// stylesheet, the config, the README with the provenance, the licence and the fonts.
// A fixture tree that holds them passes the brand, contrast and font rules.
export function goodSiteFiles() {
  const text = (rel) => readFileSync(join(GUIDE_ROOT, rel), 'utf8');
  const files = {
    'src/css/custom.css': text('src/css/custom.css'),
    'docusaurus.config.ts': text('docusaurus.config.ts'),
    'README.md': text('README.md'),
    'static/fonts/OFL.txt': text('static/fonts/OFL.txt'),
  };
  for (const [name, buffer] of Object.entries(goodFontFiles())) files[`${FONT_DIR}/${name}`] = buffer;
  return files;
}

// The text with its first `from` replaced by `to`. Fails when `from` is not there, so
// a fixture edit can never silently do nothing.
export function replaceOnce(text, from, to) {
  assert.ok(text.includes(from), `fixture lacks ${from}`);
  return text.replace(from, () => to);
}

// Writes a temp tree for runChecks: a minimal valid guide plus the files given (a null
// content leaves the file out), runs fn(root) and removes the tree again.
export function withSiteTree(files, fn) {
  const root = mkdtempSync(join(tmpdir(), 'guide-site-test-'));
  try {
    const all = {
      'package.json': JSON.stringify(goodPackage()),
      'docs/index.md': '---\ntitle: Welcome\nslug: /\n---\n\nHello.\n',
      'scripts/a.test.mjs': "import {test} from 'node:test';\ntest('real case', () => {});\n",
      ...goodSiteFiles(),
      ...files,
    };
    for (const [rel, content] of Object.entries(all)) {
      if (content === null) continue;
      const abs = join(root, rel);
      mkdirSync(dirname(abs), {recursive: true});
      writeFileSync(abs, content);
    }
    return fn(root);
  } finally {
    rmSync(root, {recursive: true, force: true});
  }
}

// A package.json equal to the allowlist, with the check script, as a fresh object.
export function goodPackage() {
  return {
    scripts: {check: 'node --test "scripts/**/*.test.mjs" && node scripts/check.mjs'},
    dependencies: {
      '@docusaurus/core': '3.10.2',
      '@docusaurus/faster': '3.10.2',
      '@docusaurus/preset-classic': '3.10.2',
      '@mdx-js/react': '^3.0.0',
      clsx: '^2.0.0',
      'prism-react-renderer': '^2.3.0',
      react: '^19.0.0',
      'react-dom': '^19.0.0',
    },
    devDependencies: {
      '@docusaurus/module-type-aliases': '3.10.2',
      '@docusaurus/tsconfig': '3.10.2',
      '@docusaurus/types': '3.10.2',
      '@types/react': '^19.0.0',
      typescript: '~6.0.2',
    },
  };
}
