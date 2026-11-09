import {test} from 'node:test';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {readdirSync, readFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {dirname, join, relative, sep} from 'node:path';
import {fileURLToPath} from 'node:url';
import {runChecks} from './check.mjs';
import {ENTRIES, MANUAL_ROWS} from './coverage.mjs';
import {SKELETON_MARKER} from './rules/structure.mjs';

// The finished live guide (spec 11, T11): every page is done, so the whole checker passes on the real
// docs with the skeleton ban on, from every working directory. This is the one self-test that reads the
// live guide/docs on purpose; it fails as soon as a marker comes back or a page loses a manifest entry.
const CHECK_SCRIPT = join(dirname(fileURLToPath(import.meta.url)), 'check.mjs');
const GUIDE_ROOT = join(dirname(CHECK_SCRIPT), '..');

test('the finished live guide: no docs page carries the skeleton marker', () => {
  const docs = join(GUIDE_ROOT, 'docs');
  const marked = readdirSync(docs, {recursive: true, withFileTypes: true})
    .filter((entry) => entry.isFile() && readFileSync(join(entry.parentPath, entry.name), 'utf8').includes(SKELETON_MARKER))
    .map((entry) => relative(GUIDE_ROOT, join(entry.parentPath, entry.name)).split(sep).join('/'));
  assert.deepEqual(marked, []);
});

test('the finished live guide: the command line passes every rule from every working directory, with 0 pending', () => {
  const summary = `anchored ${ENTRIES.length}/${ENTRIES.length}, ${MANUAL_ROWS.length} manual rows, 0 entries pending`;
  const inProcess = runChecks(GUIDE_ROOT, {structure: true, content: true, banSkeleton: true});
  assert.deepEqual(inProcess.errors, []);
  for (const cwd of [tmpdir(), join(GUIDE_ROOT, '..'), GUIDE_ROOT]) {
    const result = spawnSync(process.execPath, [CHECK_SCRIPT], {encoding: 'utf8', cwd});
    assert.equal(result.status, 0, `cwd ${cwd}\n${result.stdout}${result.stderr}`);
    assert.equal(result.stderr, '', `cwd ${cwd}`);
    assert.match(result.stdout, /^ok {3}structure \(11 pages/m, `cwd ${cwd}`);
    assert.ok(result.stdout.split('\n').includes(`ok   coverage (${summary})`), `cwd ${cwd}\n${result.stdout}`);
    assert.match(result.stdout, /^ok {3}content \(11 pages/m, `cwd ${cwd}`);
    assert.match(result.stdout, /^pending skeleton pages: 0$/m, `cwd ${cwd}`);
    assert.match(result.stdout, /^pending coverage entries: 0 on 0 skeleton pages$/m, `cwd ${cwd}`);
    assert.doesNotMatch(result.stdout, /^FAIL /m, `cwd ${cwd}`);
    assert.doesNotMatch(result.stdout, /^ {2}pending /m, `cwd ${cwd}`);
    assert.deepEqual(result.stdout.trimEnd().split('\n'), inProcess.lines, `cwd ${cwd}`);
  }
});
