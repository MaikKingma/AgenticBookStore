import {test} from 'node:test';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {readFileSync, readdirSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {dirname, join, relative, sep} from 'node:path';
import {fileURLToPath} from 'node:url';
import {runChecks} from './check.mjs';
import {ENTRIES, MANUAL_ROWS, PAGE_FILES, SOURCE_FILES} from './coverage.mjs';
import {packCommandLines} from './rules/content.mjs';
import {SKELETON_MARKER} from './rules/structure.mjs';
import {replaceOnce, withSiteTree} from './lib/test-fixtures.mjs';

// The coverage and content rules through the whole checker (runChecks and the command line),
// on temp trees that hold the real docs plus the change under test. The pack is the real one.
const CHECK_SCRIPT = join(dirname(fileURLToPath(import.meta.url)), 'check.mjs');
const GUIDE_ROOT = join(dirname(CHECK_SCRIPT), '..');
const SETUP = PAGE_FILES.SU;
const LEVEL0 = PAGE_FILES.L0;
const LEVEL2 = PAGE_FILES.L2;
const LEVEL4 = PAGE_FILES.L4;

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
const REAL = realDocs();

const check = (files = {}, options = {}) => withSiteTree({...REAL, ...files}, (root) => runChecks(root, {content: true, ...options}));
const page = (file, extra) => ({[file]: `${REAL[file]}\n${extra}\n`});
// A report line of the coverage or content rule ("ok   coverage (...", "FAIL content (...").
const RULE_LINE = /^(ok {2}|FAIL) (coverage|content) /;

test('runChecks with content: the real skeleton guide has no errors, prints anchored N/N and the pending entries per page', () => {
  const {lines, errors} = check();
  assert.deepEqual(errors, []);
  const summary = `anchored ${ENTRIES.length}/${ENTRIES.length}, ${MANUAL_ROWS.length} manual rows, ${ENTRIES.length} entries pending`;
  assert.ok(lines.includes(`ok   coverage (${summary})`), lines.join('\n'));
  assert.ok(lines.some((line) => /^ok {3}content \(11 pages/.test(line)), lines.join('\n'));
  assert.ok(lines.includes(`pending coverage entries: ${ENTRIES.length} on 10 skeleton pages`), lines.join('\n'));
  for (const [code, file] of Object.entries(PAGE_FILES)) {
    const ids = ENTRIES.filter((e) => e.page === code).map((e) => e.id).sort();
    if (ids.length === 0) continue;
    assert.ok(lines.includes(`  pending entries ${file}: ${ids.length} (${ids.join(', ')})`), `${file}\n${lines.join('\n')}`);
  }
});

test('runChecks: the coverage and content rules run only when asked for, other trees keep their old behaviour', () => {
  withSiteTree(REAL, (root) => {
    const {lines, errors} = runChecks(root, {structure: false});
    assert.deepEqual(errors, []);
    assert.ok(lines.every((line) => !RULE_LINE.test(line)), 'no coverage or content line without the option');
    assert.ok(lines.every((line) => !line.startsWith('pending coverage')), 'no pending entries without the option');
  });
});

// A finished setup page and a tiny manifest: the pass path through the checker.
const FINISHED = '---\ntitle: Setup check\nsidebar_position: 2\n---\n\n```bash\nclaude --version\n```\n\nCheck `/status` too.\n';
const TINY = {
  entries: [
    {id: 'SU-01', row: 'WP:50', source: 'plan', section: '3. Setup check', page: 'SU', kind: 'command', snippet: 'claude --version'},
    {id: 'SU-02', row: 'WP:51', source: 'plan', section: '3. Setup check', page: 'SU', kind: 'text', snippet: '/status'},
  ],
  manualRows: [],
};

test('runChecks with content: a finished page that carries its entries passes and the line counts them anchored', () => {
  const {lines, errors} = check({[SETUP]: FINISHED}, {manifest: TINY});
  assert.deepEqual(errors, []);
  assert.ok(lines.includes('ok   coverage (anchored 2/2, 0 manual rows, 0 entries pending)'), lines.join('\n'));
  assert.ok(lines.includes('pending coverage entries: 0 on 0 skeleton pages'), lines.join('\n'));
});

test('runChecks with content: a finished page without a snippet fails with a FAIL line naming the entry', () => {
  const {lines, errors} = check({[SETUP]: FINISHED.replace('claude --version', 'claude -v')}, {manifest: TINY});
  assert.equal(errors.length, 1, errors.join('\n'));
  assert.match(errors[0], /^coverage: SU-01: docs\/00-setup-check\.md: the command "claude --version" is not in a fenced block of the page/);
  assert.ok(lines.includes('FAIL coverage (1 errors)'), lines.join('\n'));
});

test('runChecks with content: an entry whose snippet is absent from the pack fails the coverage line (anchored N-1/N)', () => {
  const broken = {...TINY, entries: [{...TINY.entries[0], snippet: 'claude --versionn'}, TINY.entries[1]]};
  const {lines, errors} = check({[SETUP]: FINISHED}, {manifest: broken});
  assert.ok(errors.some((e) => /^coverage: SU-01: the snippet "claude --versionn" is not in/.test(e)), errors.join('\n'));
  assert.ok(lines.some((line) => line.startsWith('FAIL coverage')), lines.join('\n'));
});

test('runChecks with content: removing the marker from the Level 0 skeleton lists every missing Level 0 entry', () => {
  const l0 = ENTRIES.filter((e) => e.page === 'L0');
  const {errors} = check({[LEVEL0]: REAL[LEVEL0].replace(SKELETON_MARKER, '')});
  const coverage = errors.filter((e) => e.startsWith('coverage:'));
  assert.equal(coverage.length, l0.length, coverage.join('\n'));
  for (const entry of l0) assert.ok(coverage.some((e) => e.startsWith(`coverage: ${entry.id}: ${LEVEL0}:`)), entry.id);
  assert.ok(errors.some((e) => /^content: docs\/01-level-0.*observation card/.test(e)), 'the card rule runs once the marker is gone');
});

test('runChecks with content: the content probes of the card each fail through the whole checker', () => {
  const cases = [
    ['a PowerShell tab with Remove-Item -Recurse foo', SETUP, '<Tabs groupId="os">\n<TabItem value="a" label="macOS/Linux">\n\n```bash\nls\n```\n\n</TabItem>\n<TabItem value="b" label="Windows PowerShell">\n\n```powershell\nRemove-Item -Recurse foo\n```\n\n</TabItem>\n</Tabs>', /PowerShell line "Remove-Item -Recurse foo"/],
    ['export ANTHROPIC_API_KEY=x on the setup page', SETUP, '```bash\nexport ANTHROPIC_API_KEY=x\n```', /ANTHROPIC_API_KEY in an export, set, setx or unset command/],
    ['the word facilitator on a page', LEVEL2, 'The facilitator shows the way.', /"facilitator" must not appear/],
    ['<img src="/x.png"> on a page', LEVEL2, '<img src="/x.png">', /a raw <img> tag/],
    ['an unknown tab label', LEVEL2, '<Tabs groupId="os">\n<TabItem value="a" label="Linux">\n\nx\n\n</TabItem>\n</Tabs>', /unknown label "Linux" in the os group/],
    ['the token on Level 2', LEVEL2, 'Never set `ANTHROPIC_API_KEY`.', /may only be named on the setup and troubleshooting pages/],
    ['feature-briefs.md on a page', LEVEL4, 'See feature-briefs.md.', /"feature-briefs" must not appear/],
    ['spec 10 on a page', LEVEL2, 'As in spec 10.', /"spec NN" must not appear/],
  ];
  for (const [name, file, extra, pattern] of cases) {
    const {lines, errors} = check(page(file, extra));
    assert.ok(errors.some((e) => e.startsWith('content: ') && pattern.test(e)), `${name}\n${errors.join('\n')}`);
    assert.ok(lines.some((line) => line.startsWith('FAIL content')), name);
  }
});

test('the pack states the Windows lines the guide may show, and only whole lines of it count (the real pack)', () => {
  const pack = Object.values(SOURCE_FILES).map((rel) => readFileSync(new URL(`../../docs/spec-pack/${rel}`, import.meta.url), 'utf8'));
  const allowed = packCommandLines(pack);
  for (const line of [
    'Get-Command jdtls',
    '$env:BOOKSTORE_AI_API_KEY="<key on screen>"',
    'irm https://openspec.dev/cli/install.ps1 | iex',
    'which jdtls',
    './mvnw verify',
  ]) {
    assert.ok(allowed.has(line), line);
  }
  for (const line of ['Get-Command', 'Remove-Item -Recurse foo', 'Get-Command jdtls; Remove-Item -Recurse x', 'git clone https://github.com/MaikKingma/AgenticBookStore.git; cd AgenticBookStore']) {
    assert.ok(!allowed.has(line), line);
  }
});

test('runChecks with content: the bare token passes on the setup page, the PowerShell lines of the pack pass, the feature-briefs anchor passes', () => {
  const extra =
    'Do not use `ANTHROPIC_API_KEY`.\n\n<Tabs groupId="os">\n<TabItem value="a" label="macOS/Linux">\n\n```bash\nwhich jdtls\n```\n\n</TabItem>\n<TabItem value="b" label="Windows PowerShell">\n\n```powershell\nGet-Command jdtls\n$env:BOOKSTORE_AI_API_KEY="<key on screen>"\n```\n\n</TabItem>\n</Tabs>';
  const {errors} = check(page(SETUP, extra));
  assert.deepEqual(errors, []);
  assert.ok(REAL[LEVEL4].includes('{/* #feature-briefs */}'), 'the Level 4 skeleton holds the anchor, and the real-guide test above passes with it');
});

test('runChecks with content: removing the marker from the index lists every index entry as missing from the page', () => {
  const {errors} = check({'docs/index.md': replaceOnce(REAL['docs/index.md'], SKELETON_MARKER, '')});
  const ix = ENTRIES.filter((e) => e.page === 'IX');
  assert.equal(errors.filter((e) => e.startsWith('coverage:')).length, ix.length, errors.join('\n'));
});

const run = (args, cwd) => spawnSync(process.execPath, [CHECK_SCRIPT, ...args], {encoding: 'utf8', cwd});

test('cli: --content runs the coverage and content rules on an explicit root, and a failure exits 1', () => {
  withSiteTree(REAL, (root) => {
    const ok = run([root, '--content']);
    assert.equal(ok.status, 0, ok.stdout + ok.stderr);
    assert.match(ok.stdout, new RegExp(`^ok {3}coverage \\(anchored ${ENTRIES.length}/${ENTRIES.length}`, 'm'));
    assert.match(ok.stdout, /^ok {3}content \(11 pages/m);
  });
  withSiteTree({...REAL, ...page(LEVEL2, '<img src="/x.png">')}, (root) => {
    const bad = run([root, '--content']);
    assert.equal(bad.status, 1, bad.stdout + bad.stderr);
    assert.match(bad.stdout, /^FAIL content/m);
    assert.match(bad.stderr, /a raw <img> tag/);
  });
});

test('cli: an explicit root without --content skips the coverage and content rules', () => {
  withSiteTree({...REAL, ...page(LEVEL2, '<img src="/x.png">')}, (root) => {
    const result = run([root]);
    assert.equal(result.status, 0, result.stdout + result.stderr);
    assert.doesNotMatch(result.stdout, /^(ok {2}|FAIL) (coverage|content) /m);
    assert.doesNotMatch(result.stdout, /^pending coverage/m);
  });
});

test('cli: the real guide passes the coverage and content rules from any working directory', () => {
  for (const cwd of [tmpdir(), join(GUIDE_ROOT, '..'), GUIDE_ROOT]) {
    const result = run([], cwd);
    assert.equal(result.status, 0, `cwd ${cwd}\n${result.stdout}${result.stderr}`);
    assert.match(result.stdout, new RegExp(`^ok {3}coverage \\(anchored ${ENTRIES.length}/${ENTRIES.length}`, 'm'));
    assert.match(result.stdout, /^ok {3}content \(11 pages/m);
    assert.match(result.stdout, new RegExp(`^pending coverage entries: ${ENTRIES.length} on 10 skeleton pages`, 'm'));
  }
});
