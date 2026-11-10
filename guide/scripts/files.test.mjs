import {test} from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync, mkdirSync, writeFileSync, rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join, dirname} from 'node:path';
import {collectTextFiles} from './lib/files.mjs';

function makeTree(files) {
  const root = mkdtempSync(join(tmpdir(), 'guide-files-test-'));
  for (const [rel, content] of Object.entries(files)) {
    const abs = join(root, rel);
    mkdirSync(dirname(abs), {recursive: true});
    writeFileSync(abs, content);
  }
  return root;
}

test('collectTextFiles: finds text files at every depth, dot files included, with posix paths', () => {
  const root = makeTree({
    'README.md': 'r',
    'sidebars.ts': 's',
    'package.json': '{}',
    '.hidden': 'h',
    'docs/index.md': 'i',
    'docs/x/y.md': 'y',
    'scripts/a.mjs': 'a',
    'scripts/rules/b.mjs': 'b',
  });
  try {
    const files = collectTextFiles(root).map((f) => f.file);
    assert.deepEqual(files, [
      '.hidden',
      'README.md',
      'docs/index.md',
      'docs/x/y.md',
      'package.json',
      'scripts/a.mjs',
      'scripts/rules/b.mjs',
      'sidebars.ts',
    ]);
  } finally {
    rmSync(root, {recursive: true, force: true});
  }
});

test('collectTextFiles: returns the file text', () => {
  const root = makeTree({'docs/index.md': 'hello\n'});
  try {
    assert.deepEqual(collectTextFiles(root), [{file: 'docs/index.md', text: 'hello\n'}]);
  } finally {
    rmSync(root, {recursive: true, force: true});
  }
});

test('collectTextFiles: skips the top-level generated and installed folders', () => {
  const root = makeTree({
    'docs/index.md': 'i',
    'node_modules/pkg/index.js': 'n',
    'build/index.html': 'b',
    'build-offline/index.html': 'o',
    '.docusaurus/registry.js': 'd',
  });
  try {
    assert.deepEqual(
      collectTextFiles(root).map((f) => f.file),
      ['docs/index.md'],
    );
  } finally {
    rmSync(root, {recursive: true, force: true});
  }
});

test('collectTextFiles: a nested folder that only shares a skipped name is still scanned', () => {
  const root = makeTree({'docs/build/page.md': 'p', 'docs/node_modules/page.md': 'q'});
  try {
    assert.deepEqual(
      collectTextFiles(root).map((f) => f.file),
      ['docs/build/page.md', 'docs/node_modules/page.md'],
    );
  } finally {
    rmSync(root, {recursive: true, force: true});
  }
});

test('collectTextFiles: skips font and image binaries by extension and any file with a NUL byte', () => {
  const root = makeTree({
    'docs/index.md': 'i',
    'static/fonts/Montserrat.woff2': Buffer.from([0x77, 0x4f, 0x46, 0x32, 0xff, 0x00]),
    'static/fonts/a.woff': 'x',
    'static/fonts/a.ttf': 'x',
    'static/img/a.png': 'x',
    'static/img/a.ico': 'x',
    'static/blob.dat': Buffer.from([0x41, 0x00, 0x42]),
  });
  try {
    assert.deepEqual(
      collectTextFiles(root).map((f) => f.file),
      ['docs/index.md'],
    );
  } finally {
    rmSync(root, {recursive: true, force: true});
  }
});

test('collectTextFiles: text with invalid UTF-8 is still returned so the ASCII rule can flag it', () => {
  const root = makeTree({'docs/bad.md': Buffer.from([0x61, 0xff, 0x62])});
  try {
    const [file] = collectTextFiles(root);
    assert.equal(file.file, 'docs/bad.md');
    assert.ok(file.text.length > 0);
    assert.ok([...file.text].some((ch) => ch.codePointAt(0) > 0x7f));
  } finally {
    rmSync(root, {recursive: true, force: true});
  }
});
