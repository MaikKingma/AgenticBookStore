import {test} from 'node:test';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {readdirSync, readFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {dirname, join, relative, sep} from 'node:path';
import {fileURLToPath} from 'node:url';
import {runChecks} from './check.mjs';
import {FIXTURE_LEVELS, fixturePlan, goodDocs, levelPage, withStructureTree} from './lib/structure-fixtures.mjs';
import {replaceOnce} from './lib/test-fixtures.mjs';

const CHECK_SCRIPT = join(dirname(fileURLToPath(import.meta.url)), 'check.mjs');
const GUIDE_ROOT = join(dirname(CHECK_SCRIPT), '..');

// Runs the whole checker with the structure rules on a temp guide.
function check(files, pack) {
  return withStructureTree(files, (root, workshopDir) => runChecks(root, {structure: true, workshopDir}), pack);
}

const failing = (errors) => errors.filter((e) => e.startsWith('structure:'));

test('runChecks with structure: a good guide has no errors and prints one structure line', () => {
  const {lines, errors} = check({});
  assert.deepEqual(errors, []);
  assert.ok(lines.some((line) => /^ok {3}structure \(11 pages/.test(line)), lines.join(' | '));
});

test('runChecks: the structure rules run only when asked for, other trees keep their old behaviour', () => {
  withStructureTree({}, (root) => {
    const {lines, errors} = runChecks(root);
    assert.deepEqual(errors, []);
    assert.ok(lines.every((line) => !line.includes('structure')), lines.join(' | '));
  });
});

test('runChecks with structure: a swapped checklist item fails with a FAIL line', () => {
  const [a, b] = FIXTURE_LEVELS[3].items;
  let text = replaceOnce(levelPage(3), `- [ ] ${a}`, '- [ ] SWAP');
  text = replaceOnce(text, `- [ ] ${b}`, `- [ ] ${a}`);
  text = replaceOnce(text, '- [ ] SWAP', `- [ ] ${b}`);
  const {lines, errors} = check({[FIXTURE_LEVELS[3].file]: text});
  assert.equal(failing(errors).length, 2, errors.join('\n'));
  assert.ok(lines.some((line) => line.startsWith('FAIL structure')), lines.join(' | '));
});

test('runChecks with structure: each rule family reaches the whole checker', () => {
  const l = FIXTURE_LEVELS;
  const cases = [
    [{[l[1].file]: replaceOnce(levelPage(1), 'current={1}', 'current={2}')}, /LevelProgress has current=\{2\}/],
    [{[l[2].file]: replaceOnce(levelPage(2), '**Time:** 20 minutes', '**Time:** 8 minutes')}, /the Time is 8 minutes/],
    [{[l[2].file]: replaceOnce(levelPage(2), ' {/* #part-b-mcp */}', '')}, /part-b-mcp/],
    [{[l[0].file]: `${levelPage(0)}\nA trailing paragraph.\n`}, /follows the checklist/],
    [{'docs/extra.md': '---\ntitle: X\n---\n'}, /unexpected file docs\/extra\.md/],
    [{'docs/06-wrap-up.md': '---\ntitle: Wrap-up\nsidebar_position: 8\n---\n\n:::tip Checkpoint\nx\n:::\n'}, /space form/],
    [{'docs/reference/_category_.json': '{"label": "Reference", "position": 3}'}, /position/],
  ];
  for (const [files, pattern] of cases) {
    const errors = failing(check(files).errors);
    assert.ok(errors.some((e) => pattern.test(e)), `${pattern}: ${errors.join(' | ')}`);
  }
});

test('runChecks with structure: a docs file with a NUL byte is an error', () => {
  const wrapUp = `${goodDocs()['docs/06-wrap-up.md']}\0`;
  const {errors} = check({'docs/06-wrap-up.md': wrapUp});
  assert.ok(errors.some((e) => /^structure: docs\/06-wrap-up\.md: .*NUL byte/.test(e)), errors.join('\n'));
});

test('runChecks with structure: a missing plan or SDD guide names the file', () => {
  const noPlan = failing(check({}, {plan: null}).errors);
  assert.ok(noPlan.some((e) => /workshop plan: .*workshop-plan\.md is missing/.test(e)), noPlan.join('\n'));
  const noSdd = failing(check({}, {sdd: null}).errors);
  assert.ok(noSdd.some((e) => /SDD framework guide: .*sdd-framework-guide\.md is missing/.test(e)), noSdd.join('\n'));
});

test('runChecks with structure: a plan change reaches the pages (the pages are compared to the plan)', () => {
  const changed = fixturePlan().replace('(20 min)', '(21 min)');
  const errors = failing(check({}, {plan: changed}).errors);
  assert.equal(errors.length, 1, errors.join('\n'));
  assert.match(errors[0], /03-level-2.*the Time is 20 minutes, the plan heading says 21/);
});

// The docs of the real guide as {path relative to guide/: text}, for a temp tree.
function realDocs() {
  const docs = join(GUIDE_ROOT, 'docs');
  return Object.fromEntries(
    readdirSync(docs, {recursive: true, withFileTypes: true})
      .filter((entry) => entry.isFile())
      .map((entry) => {
        const abs = join(entry.parentPath, entry.name);
        return [relative(GUIDE_ROOT, abs).split(sep).join('/'), readFileSync(abs, 'utf8')];
      }),
  );
}

test('cli: --structure runs the structure rules on an explicit root, and a failure exits 1', () => {
  // The CLI finds the pack relative to the checker, so these trees hold the real docs.
  withStructureTree(realDocs(), (root) => {
    const result = spawnSync(process.execPath, [CHECK_SCRIPT, root, '--structure'], {encoding: 'utf8'});
    assert.equal(result.status, 0, result.stdout + result.stderr);
    assert.match(result.stdout, /^ok {3}structure \(11 pages/m);
  });
  withStructureTree({...realDocs(), 'docs/extra.md': '---\ntitle: X\n---\n'}, (root) => {
    const result = spawnSync(process.execPath, [CHECK_SCRIPT, root, '--structure'], {encoding: 'utf8'});
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.match(result.stdout, /^FAIL structure/m);
    assert.match(result.stderr, /unexpected file docs\/extra\.md/);
  });
});

test('cli: an explicit root without --structure skips the structure rules', () => {
  withStructureTree({}, (root) => {
    const result = spawnSync(process.execPath, [CHECK_SCRIPT, root], {encoding: 'utf8'});
    assert.equal(result.status, 0, result.stdout + result.stderr);
    assert.doesNotMatch(result.stdout, /structure/);
  });
});

test('cli: the real guide passes the structure rules from any working directory', () => {
  for (const cwd of [tmpdir(), join(GUIDE_ROOT, '..'), GUIDE_ROOT]) {
    const result = spawnSync(process.execPath, [CHECK_SCRIPT], {encoding: 'utf8', cwd});
    assert.equal(result.status, 0, `cwd ${cwd}\n${result.stdout}${result.stderr}`);
    assert.match(result.stdout, /^ok {3}structure \(11 pages/m);
  }
});
