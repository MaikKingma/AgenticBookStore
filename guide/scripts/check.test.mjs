import {test} from 'node:test';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {mkdtempSync, mkdirSync, writeFileSync, rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join, dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {runChecks} from './check.mjs';
import {goodPackage} from './lib/test-fixtures.mjs';

const EM_DASH = String.fromCodePoint(0x2014);
const CHECK_SCRIPT = join(dirname(fileURLToPath(import.meta.url)), 'check.mjs');

function makeTree(files = {}) {
  const root = mkdtempSync(join(tmpdir(), 'guide-check-test-'));
  const all = {
    'package.json': JSON.stringify(goodPackage()),
    'docs/index.md': '---\ntitle: Welcome\nslug: /\n---\n\nHello.\n',
    ...files,
  };
  for (const [rel, content] of Object.entries(all)) {
    if (content === null) continue;
    const abs = join(root, rel);
    mkdirSync(dirname(abs), {recursive: true});
    writeFileSync(abs, content);
  }
  return root;
}

function withTree(files, fn) {
  const root = makeTree(files);
  try {
    return fn(root);
  } finally {
    rmSync(root, {recursive: true, force: true});
  }
}

test('runChecks: a clean tree has no errors and one ok line per rule', () => {
  withTree({}, (root) => {
    const {lines, errors} = runChecks(root);
    assert.deepEqual(errors, []);
    for (const rule of ['ascii', 'lab', 'dependencies']) {
      assert.ok(
        lines.some((line) => line.startsWith(`ok   ${rule}`)),
        `no ok line for ${rule}: ${lines.join(' | ')}`,
      );
    }
  });
});

test('runChecks: an em dash in docs/index.md fails the ascii rule with the dedicated message', () => {
  withTree({'docs/index.md': `Hello ${EM_DASH} world\n`}, (root) => {
    const {lines, errors} = runChecks(root);
    assert.equal(errors.length, 1);
    assert.match(errors[0], /^ascii: docs\/index\.md:1:7 em dash/);
    assert.ok(lines.some((line) => line.startsWith('FAIL ascii')));
  });
});

test('runChecks: Lab 1 in docs/index.md fails the lab rule', () => {
  withTree({'docs/index.md': 'Start Lab 1 now\n'}, (root) => {
    const {errors} = runChecks(root);
    assert.equal(errors.length, 1);
    assert.match(errors[0], /^lab: docs\/index\.md:1:7 /);
  });
});

test('runChecks: the lab rule skips scripts/** but the ascii rule scans it', () => {
  withTree({'scripts/x.mjs': `// Lab 1 ${EM_DASH}\n`}, (root) => {
    const {errors} = runChecks(root);
    assert.equal(errors.length, 1);
    assert.match(errors[0], /^ascii: scripts\/x\.mjs:1:10 /);
  });
});

test('runChecks: the lab rule skips only the top-level lockfile, which names third-party packages', () => {
  const lockText = '"node_modules/postcss-lab-function": {}\n';
  withTree({'package-lock.json': lockText}, (root) => {
    assert.deepEqual(runChecks(root).errors, []);
  });
  withTree({'docs/package-lock.json': lockText, 'package.json.bak': lockText}, (root) => {
    const files = runChecks(root).errors.map((e) => e.split(':')[1].trim());
    assert.deepEqual(files, ['docs/package-lock.json', 'package.json.bak']);
  });
});

test('runChecks: the ascii rule still scans the lockfile', () => {
  withTree({'package-lock.json': `{"x": "${EM_DASH}"}\n`}, (root) => {
    const {errors} = runChecks(root);
    assert.equal(errors.length, 1);
    assert.match(errors[0], /^ascii: package-lock\.json:1:8 em dash/);
  });
});

test('runChecks: the ascii rule reaches README, sidebars, scripts and nested docs', () => {
  const dash = `x${EM_DASH}\n`;
  withTree(
    {
      'README.md': dash,
      'sidebars.ts': dash,
      'scripts/a.mjs': dash,
      'scripts/rules/b.mjs': dash,
      'docs/x/y.md': dash,
    },
    (root) => {
      const {errors} = runChecks(root);
      const files = errors.map((e) => e.split(':')[1].trim()).sort();
      assert.deepEqual(files, ['README.md', 'docs/x/y.md', 'scripts/a.mjs', 'scripts/rules/b.mjs', 'sidebars.ts']);
    },
  );
});

test('runChecks: generated folders are not scanned', () => {
  withTree(
    {
      'build/index.html': `${EM_DASH} Lab 1`,
      'build-offline/index.html': `${EM_DASH} Lab 1`,
      'node_modules/x/index.js': `${EM_DASH} Lab 1`,
      '.docusaurus/x.js': `${EM_DASH} Lab 1`,
    },
    (root) => {
      assert.deepEqual(runChecks(root).errors, []);
    },
  );
});

test('runChecks: an extra dependency key fails the dependencies rule', () => {
  const pkg = goodPackage();
  pkg.devDependencies['left-pad'] = '1.0.0';
  withTree({'package.json': JSON.stringify(pkg)}, (root) => {
    const {errors} = runChecks(root);
    assert.equal(errors.length, 1);
    assert.match(errors[0], /^dependencies: devDependencies: unexpected key "left-pad"/);
  });
});

test('runChecks: a missing or unparsable package.json is an error, not a silent pass', () => {
  withTree({'package.json': null}, (root) => {
    assert.match(runChecks(root).errors.join('\n'), /dependencies: package\.json is missing/);
  });
  withTree({'package.json': '{not json'}, (root) => {
    assert.match(runChecks(root).errors.join('\n'), /dependencies: package\.json is not valid JSON/);
  });
});

test('runChecks: a root with no text files is an error (the walker must not pass vacuously)', () => {
  const root = mkdtempSync(join(tmpdir(), 'guide-check-empty-'));
  try {
    const {errors} = runChecks(root);
    assert.ok(errors.some((e) => e.startsWith('ascii: no text files found')), errors.join(' | '));
  } finally {
    rmSync(root, {recursive: true, force: true});
  }
});

test('runChecks: lists the pages that still carry the skeleton marker', () => {
  withTree(
    {
      'docs/01-level.mdx': '---\ntitle: One\n---\n\n{/* guide-skeleton */}\n',
      'docs/reference/cheat.md': '---\ntitle: Cheat\n---\n\n{/* guide-skeleton */}\n',
      'docs/done.md': '---\ntitle: Done\n---\n',
    },
    (root) => {
      const {lines, errors} = runChecks(root);
      assert.deepEqual(errors, []);
      assert.ok(lines.includes('pending skeleton pages: 2'), lines.join(' | '));
      assert.ok(lines.includes('  pending docs/01-level.mdx'));
      assert.ok(lines.includes('  pending docs/reference/cheat.md'));
    },
  );
});

test('runChecks: reports no pending pages when the marker is absent', () => {
  withTree({}, (root) => {
    assert.ok(runChecks(root).lines.includes('pending skeleton pages: 0'));
  });
});

test('cli: exits 0 on a clean tree and 1 on a failing tree', () => {
  withTree({}, (root) => {
    const result = spawnSync(process.execPath, [CHECK_SCRIPT, root], {encoding: 'utf8'});
    assert.equal(result.status, 0, result.stdout + result.stderr);
    assert.match(result.stdout, /ok {3}ascii/);
  });
  withTree({'docs/index.md': `bad ${EM_DASH}\n`}, (root) => {
    const result = spawnSync(process.execPath, [CHECK_SCRIPT, root], {encoding: 'utf8'});
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.match(result.stdout + result.stderr, /ascii: docs\/index\.md:1:5 em dash/);
  });
});
