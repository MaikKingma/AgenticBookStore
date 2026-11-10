import {test} from 'node:test';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {mkdtempSync, mkdirSync, writeFileSync, rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join, dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {builtErrors, scanBuilt} from './rules/built.mjs';

const CLI = join(dirname(fileURLToPath(import.meta.url)), 'check-built.mjs');
const page = (body) => `<html><body><h1>Welcome</h1>${body}</body></html>\n`;
// The form the Docusaurus build minifier gives a task-list item: unquoted attributes.
const BARE = '<li class=task-list-item><input type=checkbox disabled/> Spec artefacts committed.</li>';
const NAMED = '<li class=task-list-item><input type=checkbox disabled aria-label="Spec artefacts committed."/> Spec artefacts committed.</li>';

function withBuild(files, fn) {
  const root = mkdtempSync(join(tmpdir(), 'guide-built-checkbox-test-'));
  const buildDir = join(root, 'build');
  mkdirSync(buildDir);
  for (const [rel, content] of Object.entries(files)) {
    const abs = join(buildDir, rel);
    mkdirSync(dirname(abs), {recursive: true});
    writeFileSync(abs, content);
  }
  try {
    return fn(buildDir, root);
  } finally {
    rmSync(root, {recursive: true, force: true});
  }
}

const errorsFor = (html) => withBuild({'index.html': html}, builtErrors);

test('built checkbox: a checkbox with an aria-label passes, quoted or unquoted', () => {
  assert.deepEqual(errorsFor(page(NAMED)), []);
  assert.deepEqual(errorsFor(page('<input type=checkbox aria-label=Done>')), []);
  assert.deepEqual(errorsFor(page('<input type="checkbox" aria-label=\'Done it\'>')), []);
});

test('built checkbox: a bare checkbox as Docusaurus renders a task list item fails with file and line', () => {
  const errors = errorsFor(`<html>\n<body>\n${BARE}\n</body></html>\n`);
  assert.equal(errors.length, 1);
  assert.match(errors[0], /^built: index\.html:3 checkbox has no accessible name; /);
});

test('built checkbox: the line is found on a single minified line too', () => {
  const errors = errorsFor(`<html>\n<body>${NAMED}${BARE}</body></html>`);
  assert.equal(errors.length, 1);
  assert.match(errors[0], /^built: index\.html:2 /);
});

test('built checkbox: an empty or blank aria-label is no name', () => {
  assert.equal(errorsFor(page('<input type=checkbox aria-label="">')).length, 1);
  assert.equal(errorsFor(page('<input type=checkbox aria-label="   ">')).length, 1);
  assert.equal(errorsFor(page('<input type=checkbox aria-label>')).length, 1);
});

test('built checkbox: the type is matched in any case and with quotes', () => {
  assert.equal(errorsFor(page('<INPUT TYPE="CHECKBOX" disabled>')).length, 1);
  assert.equal(errorsFor(page("<input type='checkbox'>")).length, 1);
  assert.equal(errorsFor(page('<input disabled type=checkbox/>')).length, 1);
});

test('built checkbox: each unnamed checkbox is its own error, named ones are not counted', () => {
  const errors = errorsFor(`${NAMED}\n${BARE}\n${NAMED}\n${BARE}\n`);
  assert.equal(errors.length, 2);
  assert.match(errors[0], /index\.html:2 /);
  assert.match(errors[1], /index\.html:4 /);
});

test('built checkbox: every html file is scanned and the error names its file', () => {
  withBuild({'index.html': page(NAMED), 'reference/probe.html': page(BARE), 'a/b/deep.html': page(BARE)}, (dir) => {
    const errors = builtErrors(dir);
    assert.equal(errors.length, 2);
    assert.ok(errors.some((e) => e.includes('reference/probe.html:1 ')));
    assert.ok(errors.some((e) => e.includes('a/b/deep.html:1 ')));
  });
});

test('built checkbox: aria-labelledby passes only when every id it names is on the page', () => {
  assert.deepEqual(errorsFor(page('<span id=t>Done</span><input type=checkbox aria-labelledby=t>')), []);
  assert.deepEqual(errorsFor(page('<span id=a>One</span><p id="b">Two</p><input type=checkbox aria-labelledby="a b">')), []);
  assert.equal(errorsFor(page('<span id=t>Done</span><input type=checkbox aria-labelledby="t gone">')).length, 1);
  assert.equal(errorsFor(page('<input type=checkbox aria-labelledby=gone>')).length, 1);
  assert.equal(errorsFor(page('<input type=checkbox aria-labelledby="">')).length, 1);
});

test('built checkbox: a label for the checkbox id or wrapped around it names it, but only with text', () => {
  assert.deepEqual(errorsFor(page('<input id=c type=checkbox><label for=c>Accept</label>')), []);
  assert.deepEqual(errorsFor(page('<label for=c><span>Accept</span></label><input id=c type=checkbox>')), []);
  assert.deepEqual(errorsFor(page('<label><input type=checkbox> Accept</label>')), []);
  assert.equal(errorsFor(page('<input id=c type=checkbox><label for=c></label>')).length, 1);
  assert.equal(errorsFor(page('<label for=c>   </label><input id=c type=checkbox>')).length, 1);
  assert.equal(errorsFor(page('<label><input type=checkbox></label>')).length, 1);
  assert.equal(errorsFor(page('<input id=c type=checkbox><label for=other>Accept</label>')).length, 1);
  assert.equal(errorsFor(page('<label>Text</label><input type=checkbox>')).length, 1);
});

test('built checkbox: other inputs are not checked', () => {
  const others = '<input type=text><input type=radio><input type=search placeholder=Search><input>';
  assert.deepEqual(errorsFor(page(others)), []);
});

test('built checkbox: a ">" inside a quoted attribute value does not end the tag', () => {
  assert.deepEqual(errorsFor(page('<input aria-label="a > b" type=checkbox>')), []);
  assert.equal(errorsFor(page('<input title="x>y" type=checkbox>')).length, 1);
});

test('built checkbox: markup shown as text, in a comment, in a script or in a style is not a checkbox', () => {
  assert.deepEqual(errorsFor(page('<code>&lt;input type=checkbox&gt;</code>')), []);
  assert.deepEqual(errorsFor(page('<!-- <input type=checkbox> --><p>x</p>')), []);
  assert.deepEqual(errorsFor(page('<script>document.write("<input type=checkbox>")</script>')), []);
  assert.deepEqual(errorsFor(page('<style>/* <input type=checkbox> */</style>')), []);
  assert.equal(errorsFor(page('<script>1</script><input type=checkbox>')).length, 1);
});

test('built checkbox: a stray "<" in text does not hide a later checkbox', () => {
  assert.equal(errorsFor(page('<p>1 < 2</p>' + BARE)).length, 1);
});

test('built checkbox: the literal ":::" rule still fires next to the checkbox rule', () => {
  const errors = errorsFor(`<html>\n<p>:::info Title</p>\n${BARE}\n</html>`);
  assert.equal(errors.length, 2);
  assert.ok(errors.some((e) => /index\.html:2 literal ":::"/.test(e)));
  assert.ok(errors.some((e) => /index\.html:3 checkbox has no accessible name/.test(e)));
});

test('built checkbox: scanBuilt reports how many checkboxes it saw, named or not', () => {
  withBuild({'index.html': page(NAMED + NAMED + BARE), 'b.html': page(NAMED)}, (dir) => {
    const result = scanBuilt(dir);
    assert.equal(result.count, 2);
    assert.equal(result.checkboxes, 4);
    assert.equal(result.errors.length, 1);
  });
  withBuild({'index.html': page('<p>none</p>')}, (dir) => {
    assert.equal(scanBuilt(dir).checkboxes, 0);
  });
});

test('built checkbox: check:built exits 1 on an unnamed checkbox and 0 on named ones, and reports the count', () => {
  withBuild({'index.html': page(BARE)}, (dir) => {
    const result = spawnSync(process.execPath, [CLI, dir], {encoding: 'utf8'});
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.match(result.stderr, /built: index\.html:1 checkbox has no accessible name/);
    assert.match(result.stderr, /check:built failed with 1 error\(s\)/);
  });
  withBuild({'index.html': page(NAMED + NAMED)}, (dir) => {
    const result = spawnSync(process.execPath, [CLI, dir], {encoding: 'utf8'});
    assert.equal(result.status, 0, result.stdout + result.stderr);
    assert.match(result.stdout, /^ok {3}built: 1 html files, no literal ":::", 2 checkboxes, all named$/m);
  });
});
