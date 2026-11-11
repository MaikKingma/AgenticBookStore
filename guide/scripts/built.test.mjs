import {test} from 'node:test';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {mkdtempSync, mkdirSync, writeFileSync, rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join, dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {builtErrors} from './rules/built.mjs';

const CLI = join(dirname(fileURLToPath(import.meta.url)), 'check-built.mjs');
const GOOD_PAGE = '<html><body><h1>Welcome</h1><p>Hello.</p></body></html>\n';
const SPACE_FORM = '<html><body><p>:::info Heads up</p><p>Look up.</p><p>:::</p></body></html>\n';

function withBuild(files, fn) {
  const root = mkdtempSync(join(tmpdir(), 'guide-built-test-'));
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

test('built: a build of clean pages passes', () => {
  withBuild({'index.html': GOOD_PAGE, 'setup-check.html': GOOD_PAGE}, (dir) => {
    assert.deepEqual(builtErrors(dir), []);
  });
});

test('built: a space-form admonition left as literal text fails with file and line', () => {
  withBuild({'index.html': `<html>\n${SPACE_FORM}`}, (dir) => {
    const errors = builtErrors(dir);
    assert.equal(errors.length, 1);
    assert.match(errors[0], /^built: index\.html:2 literal ":::" /);
  });
});

test('built: a bare closing fence left as literal text fails', () => {
  withBuild({'index.html': '<p>:::</p>'}, (dir) => {
    assert.equal(builtErrors(dir).length, 1);
  });
});

test('built: a nested page is scanned recursively', () => {
  withBuild({'index.html': GOOD_PAGE, 'reference/probe.html': SPACE_FORM, 'a/b/c/deep.html': SPACE_FORM}, (dir) => {
    const errors = builtErrors(dir);
    assert.equal(errors.length, 2);
    assert.ok(errors.some((e) => e.includes('reference/probe.html')));
    assert.ok(errors.some((e) => e.includes('a/b/c/deep.html')));
  });
});

test('built: a missing build folder fails and says to build first', () => {
  const root = mkdtempSync(join(tmpdir(), 'guide-built-missing-'));
  try {
    const errors = builtErrors(join(root, 'build'));
    assert.equal(errors.length, 1);
    assert.match(errors[0], /^built: .*build is missing; run npm run build first/);
  } finally {
    rmSync(root, {recursive: true, force: true});
  }
});

test('built: a build folder without any html file fails (anti-vacuity)', () => {
  withBuild({'assets/js/main.js': 'x', 'sitemap.xml': '<urlset/>'}, (dir) => {
    const errors = builtErrors(dir);
    assert.equal(errors.length, 1);
    assert.match(errors[0], /no \.html file found/);
  });
});

test('built: only html files are scanned', () => {
  withBuild({'index.html': GOOD_PAGE, 'assets/js/main.js': 'a ? b :::c'}, (dir) => {
    assert.deepEqual(builtErrors(dir), []);
  });
});

test('cli: exits 0 on a clean build, 1 on a literal admonition and 1 when the folder is missing', () => {
  withBuild({'index.html': GOOD_PAGE}, (dir) => {
    const result = spawnSync(process.execPath, [CLI, dir], {encoding: 'utf8'});
    assert.equal(result.status, 0, result.stdout + result.stderr);
    assert.match(result.stdout, /ok {3}built: 1 html files/);
  });
  withBuild({'index.html': GOOD_PAGE, 'reference/probe.html': SPACE_FORM}, (dir) => {
    const result = spawnSync(process.execPath, [CLI, dir], {encoding: 'utf8'});
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.match(result.stderr, /built: reference\/probe\.html:1 literal ":::"/);
  });
  withBuild({}, (dir, root) => {
    const result = spawnSync(process.execPath, [CLI, join(root, 'nope')], {encoding: 'utf8'});
    assert.equal(result.status, 1, result.stdout + result.stderr);
  });
});
