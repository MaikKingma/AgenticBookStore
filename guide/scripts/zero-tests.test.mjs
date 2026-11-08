import {test} from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync, mkdirSync, writeFileSync, readFileSync, rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join, dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {testGlobs, countTestCases, zeroTestErrors} from './rules/zero-tests.mjs';
import {runChecks} from './check.mjs';
import {goodPackage, goodSiteFiles} from './lib/test-fixtures.mjs';

const HERE = dirname(fileURLToPath(import.meta.url));
const GLOB = 'scripts/**/*.test.mjs';
const SCRIPT = `node --test "${GLOB}" && node scripts/check.mjs`;
const REAL = "import {test} from 'node:test';\ntest('real case', () => {});\n";

test('testGlobs: reads the quoted pattern of the node --test command', () => {
  assert.deepEqual(testGlobs(SCRIPT), [GLOB]);
});

test('testGlobs: reads several patterns, quoted or bare, and stops at the && chain', () => {
  assert.deepEqual(testGlobs('node --test "a/*.test.mjs" b.test.mjs \'c/**/*.test.mjs\' && node x.mjs'), [
    'a/*.test.mjs',
    'b.test.mjs',
    'c/**/*.test.mjs',
  ]);
});

test('testGlobs: a script without node --test has no patterns', () => {
  assert.deepEqual(testGlobs('node scripts/check.mjs'), []);
  assert.deepEqual(testGlobs(undefined), []);
});

test('countTestCases: counts test and it registrations, indented ones included', () => {
  assert.equal(countTestCases("test('a', () => {});\n  it('b', () => {});\ntest(\n  'c',\n  () => {},\n);\n"), 3);
});

test('countTestCases: skipped, todo, commented and mid-line mentions do not count', () => {
  const text = [
    "test.skip('a', () => {});",
    "test.todo('b');",
    "// test('c', () => {});",
    " * test('d')",
    "const note = 'call test(x) later';",
    "mytest('e', () => {});",
    '',
  ].join('\n');
  assert.equal(countTestCases(text), 0);
});

test('countTestCases: an empty or whitespace-only file counts zero', () => {
  assert.equal(countTestCases(''), 0);
  assert.equal(countTestCases('\n  \n'), 0);
});

test('zeroTestErrors: a script that matches test files with real cases passes', () => {
  assert.deepEqual(zeroTestErrors({script: SCRIPT, matched: {[GLOB]: [{file: 'scripts/a.test.mjs', text: REAL}]}}), []);
});

test('zeroTestErrors: a missing check script or one without node --test is an error', () => {
  assert.deepEqual(zeroTestErrors({script: undefined, matched: {}}), [
    'zero-test guard: package.json has no check script with a node --test command',
  ]);
  assert.deepEqual(zeroTestErrors({script: 'node scripts/check.mjs', matched: {}}), [
    'zero-test guard: package.json has no check script with a node --test command',
  ]);
});

test('zeroTestErrors: a glob that matches no file quotes the glob (every test file deleted, or the glob changed)', () => {
  assert.deepEqual(zeroTestErrors({script: SCRIPT, matched: {[GLOB]: []}}), [
    `zero-test guard: the node --test pattern "${GLOB}" matches no test file`,
  ]);
});

test('zeroTestErrors: files that register no test case fail with the counts (every test file emptied)', () => {
  const matched = {
    [GLOB]: [
      {file: 'scripts/a.test.mjs', text: ''},
      {file: 'scripts/b.test.mjs', text: '\n'},
    ],
  };
  assert.deepEqual(zeroTestErrors({script: SCRIPT, matched}), [
    'zero-test guard: 2 test files match but none registers a test case, so node:test would execute nothing',
  ]);
  assert.deepEqual(zeroTestErrors({script: SCRIPT, matched: {[GLOB]: [{file: 'scripts/a.test.mjs', text: ''}]}}), [
    'zero-test guard: 1 test file matches but none registers a test case, so node:test would execute nothing',
  ]);
});

test('zeroTestErrors: one real case among empty files is enough', () => {
  const matched = {
    [GLOB]: [
      {file: 'scripts/a.test.mjs', text: ''},
      {file: 'scripts/b.test.mjs', text: REAL},
    ],
  };
  assert.deepEqual(zeroTestErrors({script: SCRIPT, matched}), []);
});

function withGuardTree(files, fn) {
  const root = mkdtempSync(join(tmpdir(), 'guide-zero-test-'));
  try {
    const all = {
      'package.json': JSON.stringify({...goodPackage(), scripts: {check: SCRIPT}}),
      'docs/index.md': '---\ntitle: Welcome\n---\n\nHello.\n',
      'scripts/a.test.mjs': REAL,
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

const guardLines = (errors) => errors.filter((error) => error.startsWith('zero-test guard'));

test('runChecks: a tree with a real test case has an ok tests line and no guard error', () => {
  withGuardTree({}, (root) => {
    const {lines, errors} = runChecks(root);
    assert.deepEqual(errors, []);
    assert.ok(lines.some((line) => line.startsWith('ok   tests (1 test files, 1 test cases)')), lines.join(' | '));
  });
});

test('runChecks: every test file deleted fails the guard', () => {
  withGuardTree({'scripts/a.test.mjs': null}, (root) => {
    const {lines, errors} = runChecks(root);
    assert.deepEqual(guardLines(errors), [`zero-test guard: the node --test pattern "${GLOB}" matches no test file`]);
    assert.ok(lines.some((line) => line.startsWith('FAIL tests')));
  });
});

test('runChecks: every test file emptied fails the guard', () => {
  withGuardTree({'scripts/a.test.mjs': '', 'scripts/rules/b.test.mjs': ''}, (root) => {
    assert.deepEqual(guardLines(runChecks(root).errors), [
      'zero-test guard: 2 test files match but none registers a test case, so node:test would execute nothing',
    ]);
  });
});

test('runChecks: a check glob that matches nothing fails the guard even when test files exist', () => {
  const script = 'node --test "scripts/**/*.nothing.mjs" && node scripts/check.mjs';
  withGuardTree({'package.json': JSON.stringify({...goodPackage(), scripts: {check: script}})}, (root) => {
    assert.deepEqual(guardLines(runChecks(root).errors), [
      'zero-test guard: the node --test pattern "scripts/**/*.nothing.mjs" matches no test file',
    ]);
  });
});

test('runChecks: a package.json without a check script fails the guard instead of passing vacuously', () => {
  withGuardTree({'package.json': JSON.stringify({...goodPackage(), scripts: {}})}, (root) => {
    assert.deepEqual(guardLines(runChecks(root).errors), [
      'zero-test guard: package.json has no check script with a node --test command',
    ]);
  });
});

test('check script of guide/: node --test with the self-test glob, chained with && before the checker', () => {
  const pkg = JSON.parse(readFileSync(join(HERE, '..', 'package.json'), 'utf8'));
  assert.equal(pkg.scripts.check, SCRIPT);
});
