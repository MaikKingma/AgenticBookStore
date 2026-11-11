import {test} from 'node:test';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {closeSync, mkdtempSync, openSync, readdirSync, readFileSync, rmSync, writeFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {runChecks} from './check.mjs';
import {skeletonBanErrors} from './rules/skeleton-ban.mjs';
import {SKELETON_MARKER} from './rules/structure.mjs';
import {replaceOnce, withSiteTree} from './lib/test-fixtures.mjs';
import {withSkeletonGuideCopy} from './fixtures/skeleton-docs.mjs';

// The skeleton marker ban (spec 11, T11): once the guide is finished, any page that still carries the
// marker is an error, never a warning. The rule reads only the docs/** files through the same marker
// as the skeleton exemption; runChecks applies it only with options.banSkeleton, which the command
// line sets for its default root (the same opt-in pattern as the structure and content rules).

const FINISHED = '---\ntitle: Welcome\nslug: /\n---\n\nHello.\n';
const MARKED = `---\ntitle: Welcome\nslug: /\n---\n\n${SKELETON_MARKER}\n`;
const TWELVE = [
  'docs/00-setup-check.md',
  'docs/01-level-0-break-the-agent.mdx',
  'docs/02-level-1-context-engineering.mdx',
  'docs/03-level-2-give-your-agent-eyes.mdx',
  'docs/04-level-3-behavioral-control.mdx',
  'docs/05-level-4-spec-driven-development.mdx',
  'docs/06-bonus-level-orchestration.mdx',
  'docs/06-wrap-up.md',
  'docs/index.md',
  'docs/reference/architecture.md',
  'docs/reference/cheat-sheet.md',
  'docs/reference/troubleshooting.md',
];

const banLines = (lines) => lines.filter((line) => /^(ok {2}|FAIL) skeleton /.test(line));
const banErrors = (errors) => errors.filter((error) => error.startsWith('skeleton: '));

test('skeleton ban rule: a docs page with the marker is an error that names the page and the marker', () => {
  const errors = skeletonBanErrors([{file: 'docs/index.md', text: MARKED}]);
  assert.equal(errors.length, 1, errors.join('\n'));
  assert.match(errors[0], /^skeleton: docs\/index\.md: /);
  assert.ok(errors[0].includes(SKELETON_MARKER), errors[0]);
});

test('skeleton ban rule: a finished docs page passes', () => {
  assert.deepEqual(skeletonBanErrors([{file: 'docs/index.md', text: FINISHED}]), []);
});

test('skeleton ban rule: reads only docs/**, so the 17 marker mentions under scripts/ and a README never count', () => {
  const files = [
    {file: 'scripts/fixtures/skeleton-docs/docs/index.md', text: MARKED},
    {file: 'scripts/rules/structure.mjs', text: `export const SKELETON_MARKER = '${SKELETON_MARKER}';`},
    {file: 'README.md', text: `A page carries ${SKELETON_MARKER} while unfinished.`},
    {file: 'src/pages/x.md', text: MARKED},
    {file: 'mydocs/index.md', text: MARKED},
    {file: 'docs.md', text: MARKED},
  ];
  assert.deepEqual(skeletonBanErrors(files), []);
});

test('skeleton ban rule: the marker counts anywhere in the page, like the skeleton exemption', () => {
  const inFence = `${FINISHED}\n\`\`\`\n${SKELETON_MARKER}\n\`\`\`\n`;
  const inSpan = `${FINISHED}\nWrite \`${SKELETON_MARKER}\` here.\n`;
  const errors = skeletonBanErrors([
    {file: 'docs/a.md', text: inFence},
    {file: 'docs/b.md', text: inSpan},
  ]);
  assert.deepEqual(
    errors.map((error) => error.split(':')[1].trim()),
    ['docs/a.md', 'docs/b.md'],
  );
});

test('skeleton ban rule: a near-miss marker is no marker, for the ban and for the exemption alike', () => {
  const nearMisses = [
    '{/*guide-skeleton*/}',
    '{/* guide-skeleton*/}',
    '{/*guide-skeleton */}',
    '{/* Guide-Skeleton */}',
    '{/* guide skeleton */}',
    '<!-- guide-skeleton -->',
    'guide-skeleton',
  ];
  for (const near of nearMisses) {
    const text = `${FINISHED}\n${near}\n`;
    assert.deepEqual(skeletonBanErrors([{file: 'docs/index.md', text}]), [], near);
    assert.equal(text.includes(SKELETON_MARKER), false, `the exemption predicate agrees for ${near}`);
  }
});

test('skeleton ban rule: errors come sorted by file name, one per page', () => {
  const files = ['docs/reference/troubleshooting.md', 'docs/index.md', 'docs/00-setup-check.md', 'docs/reference/cheat-sheet.md'].map(
    (file) => ({file, text: MARKED}),
  );
  const named = skeletonBanErrors(files).map((error) => error.split(':')[1].trim());
  assert.deepEqual(named, ['docs/00-setup-check.md', 'docs/index.md', 'docs/reference/cheat-sheet.md', 'docs/reference/troubleshooting.md']);
});

test('skeleton ban rule: a marker twice on one page is still one error for that page', () => {
  const errors = skeletonBanErrors([{file: 'docs/index.md', text: `${MARKED}\n${SKELETON_MARKER}\n`}]);
  assert.equal(errors.length, 1, errors.join('\n'));
});

test('runChecks with banSkeleton: a marked page fails with a FAIL line and an error, never a warning', () => {
  withSiteTree({'docs/index.md': MARKED}, (root) => {
    const {lines, errors} = runChecks(root, {banSkeleton: true});
    assert.equal(banErrors(errors).length, 1, errors.join('\n'));
    assert.match(banErrors(errors)[0], /^skeleton: docs\/index\.md: /);
    assert.deepEqual(banLines(lines), ['FAIL skeleton (1 errors)']);
    // the pending lines still report the page, the ban is an addition to them
    assert.ok(lines.includes('pending skeleton pages: 1'), lines.join('\n'));
    assert.ok(lines.includes('  pending docs/index.md'), lines.join('\n'));
  });
});

test('runChecks with banSkeleton: a finished tree passes, the ban adds no line and no error', () => {
  withSiteTree({'docs/index.md': FINISHED}, (root) => {
    const plain = runChecks(root);
    const banned = runChecks(root, {banSkeleton: true});
    assert.deepEqual(banned.errors, []);
    assert.deepEqual(banned.lines, plain.lines, 'a passing ban prints nothing beyond the other rules');
  });
});

test('runChecks without banSkeleton: a marked page stays a pending page, not an error (the opt-in pattern)', () => {
  withSiteTree({'docs/index.md': MARKED}, (root) => {
    const {lines, errors} = runChecks(root);
    assert.deepEqual(errors, []);
    assert.deepEqual(banLines(lines), []);
    assert.ok(lines.includes('pending skeleton pages: 1'), lines.join('\n'));
  });
});

test('runChecks with banSkeleton: every marked page is named, a marker in scripts/ is ignored', () => {
  withSiteTree(
    {
      'docs/index.md': MARKED,
      'docs/reference/cheat-sheet.md': MARKED,
      'scripts/fixtures/skeleton-docs/docs/index.md': MARKED,
    },
    (root) => {
      const {lines, errors} = runChecks(root, {banSkeleton: true});
      assert.deepEqual(
        banErrors(errors).map((error) => error.split(':')[1].trim()),
        ['docs/index.md', 'docs/reference/cheat-sheet.md'],
      );
      assert.deepEqual(banLines(lines), ['FAIL skeleton (2 errors)']);
    },
  );
});

const CHECK_SCRIPT = join(import.meta.dirname, 'check.mjs');
const runCli = (args, cwd) => spawnSync(process.execPath, [CHECK_SCRIPT, ...args], {encoding: 'utf8', cwd});

test('cli: an explicit root keeps the marker pages pending, the ban applies to the default root only', () => {
  withSiteTree({'docs/index.md': MARKED}, (root) => {
    const result = runCli([root], tmpdir());
    assert.equal(result.status, 0, result.stdout + result.stderr);
    assert.match(result.stdout, /^pending skeleton pages: 1$/m);
    assert.doesNotMatch(result.stdout + result.stderr, /skeleton:/);
  });
});

test('cli: the default root of a guide copy with the 12 skeleton pages fails with exactly the ban errors from every cwd', () => {
  withSkeletonGuideCopy((guide) => {
    for (const cwd of [tmpdir(), join(guide, '..'), guide]) {
      const result = spawnSync(process.execPath, [join(guide, 'scripts', 'check.mjs')], {encoding: 'utf8', cwd});
      const stderr = result.stderr.split('\n').filter(Boolean);
      assert.equal(result.status, 1, `cwd ${cwd}\n${result.stdout}${result.stderr}`);
      assert.deepEqual(
        stderr.slice(0, -1).map((line) => /^skeleton: (docs\/\S+): /.exec(line)?.[1]),
        TWELVE,
        `cwd ${cwd}\n${result.stderr}`,
      );
      assert.equal(stderr.at(-1), 'check failed with 12 error(s)', `cwd ${cwd}`);
      assert.match(result.stdout, /^FAIL skeleton \(12 errors\)$/m);
      assert.match(result.stdout, /^pending skeleton pages: 12$/m);
    }
  });
});

test('cli: no flag switches the ban off, the default root still fails with the skeleton pages', () => {
  withSkeletonGuideCopy((guide) => {
    const flags = ['--allow-skeleton', '--no-skeleton-ban', '--skeleton', '--no-ban', '--warn', '--warn-only', '--structure', '--content'];
    for (const flag of flags) {
      const result = spawnSync(process.execPath, [join(guide, 'scripts', 'check.mjs'), flag], {encoding: 'utf8', cwd: guide});
      // the flag is taken as a flag, so the root stays the default one and the ban stays on
      assert.equal(result.status, 1, `${flag}\n${result.stdout}${result.stderr}`);
      assert.match(result.stderr, /^skeleton: docs\/index\.md: /m, flag);
    }
  });
});

// The CLI ends a failing run with process.exit(1). Over a pipe (spawnSync's default, and the Linux CI runner) Node writes
// asynchronously, so a long stderr that is still queued at that moment is cut off (seen on CI at byte 19555 of 33219, and
// reproduced in a Linux container). A file write is synchronous, so a run captured through files always arrives whole.
// Use it for any run whose output is long; the short runs above fit the pipe and stay on spawnSync.
function runCliCaptured(script, cwd) {
  const dir = mkdtempSync(join(tmpdir(), 'cli-capture-'));
  const stdoutFile = join(dir, 'stdout');
  const stderrFile = join(dir, 'stderr');
  const stdoutFd = openSync(stdoutFile, 'w');
  const stderrFd = openSync(stderrFile, 'w');
  try {
    const {status} = spawnSync(process.execPath, [script], {cwd, stdio: ['ignore', stdoutFd, stderrFd]});
    return {status, stdout: readFileSync(stdoutFile, 'utf8'), stderr: readFileSync(stderrFile, 'utf8')};
  } finally {
    closeSync(stdoutFd);
    closeSync(stderrFd);
    rmSync(dir, {recursive: true, force: true});
  }
}

// Strip every marker but one in a guide copy, as a finished guide with one relapse would look.
function stripAllMarkersBut(guide, keepName) {
  const docs = join(guide, 'docs');
  for (const entry of readdirSync(docs, {recursive: true, withFileTypes: true})) {
    if (!entry.isFile() || !/\.(md|mdx)$/.test(entry.name)) continue;
    const abs = join(entry.parentPath, entry.name);
    const text = readFileSync(abs, 'utf8');
    if (!text.includes(SKELETON_MARKER)) continue;
    writeFileSync(abs, entry.name === keepName ? text : replaceOnce(text, SKELETON_MARKER, ''));
  }
}

test('cli: a failing run with hundreds of errors delivers its whole stderr, the last line is the failed count', () => {
  withSkeletonGuideCopy((guide) => {
    stripAllMarkersBut(guide, '06-wrap-up.md');
    const result = runCliCaptured(join(guide, 'scripts', 'check.mjs'), guide);
    const stderr = result.stderr.split('\n').filter(Boolean);
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.ok(stderr.length > 200, `only ${stderr.length} stderr lines arrived`);
    assert.match(stderr.at(-1), /^check failed with \d+ error\(s\)$/);
    assert.equal(stderr.at(-1), `check failed with ${stderr.length - 1} error(s)`);
  });
});

test('cli: one marker put back on one finished page fails the default root and names that page', () => {
  withSkeletonGuideCopy((guide) => {
    // The guide copy holds the skeleton snapshot: strip every marker but one to model a finished guide with one
    // relapse. The other rules judge the stripped pages as empty finished pages, so only the ban lines are asserted.
    stripAllMarkersBut(guide, '06-wrap-up.md');
    // Over 33 KB of stderr: read through files, see runCliCaptured.
    const result = runCliCaptured(join(guide, 'scripts', 'check.mjs'), guide);
    const named = result.stderr.split('\n').filter((line) => line.startsWith('skeleton: '));
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.equal(named.length, 1, result.stderr);
    assert.match(named[0], /^skeleton: docs\/06-wrap-up\.md: /);
  });
});
