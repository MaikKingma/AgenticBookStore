import {test} from 'node:test';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {copyFileSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {dirname, join, relative, sep} from 'node:path';
import {fileURLToPath} from 'node:url';
import {runChecks} from './check.mjs';
import {ENTRIES, MANUAL_ROWS, PAGE_FILES, SOURCE_FILES} from './coverage.mjs';
import {packCommandLines} from './rules/content.mjs';
import {SKELETON_MARKER} from './rules/structure.mjs';
import {replaceOnce, withSiteTree} from './lib/test-fixtures.mjs';
import {SKELETON_DOCS, withSkeletonGuideCopy} from './fixtures/skeleton-docs.mjs';

// The coverage and content rules through the whole checker (runChecks and the command line),
// on temp trees that hold the real docs plus the change under test. The pack is the real one.
// The real docs are the skeleton snapshot (guide/docs at 05e58a2, every page a skeleton), so
// these tests hold while the content tasks fill in the live pages. Only the "live guide" test of
// the command line reads guide/docs.
const CHECK_SCRIPT = join(dirname(fileURLToPath(import.meta.url)), 'check.mjs');
const GUIDE_ROOT = join(dirname(CHECK_SCRIPT), '..');
const SETUP = PAGE_FILES.SU;
const LEVEL0 = PAGE_FILES.L0;
const LEVEL2 = PAGE_FILES.L2;
const LEVEL4 = PAGE_FILES.L4;

function realDocs() {
  const docs = SKELETON_DOCS;
  return Object.fromEntries(
    readdirSync(docs, {recursive: true, withFileTypes: true})
      .filter((entry) => entry.isFile())
      .map((entry) => {
        const abs = join(entry.parentPath, entry.name);
        return [relative(dirname(docs), abs).split(sep).join('/'), readFileSync(abs, 'utf8')];
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

test('runChecks with content: a warning that says never to set the token passes on the setup page, the same words as a command fail', () => {
  const warning = check(page(SETUP, 'Never set ANTHROPIC_API_KEY: the workshop uses its own key. Do not export or unset it either.'));
  assert.deepEqual(warning.errors, []);
  const command = check(page(SETUP, '```bash\nexport ANTHROPIC_API_KEY\n```'));
  assert.ok(command.errors.some((e) => /^content: docs\/00-setup-check\.md: line \d+: ANTHROPIC_API_KEY in an export, set, setx or unset command/.test(e)), command.errors.join('\n'));
  const inline = check(page(SETUP, 'Never run `unset ANTHROPIC_API_KEY` here.'));
  assert.ok(inline.errors.some((e) => /in an export, set, setx or unset command/.test(e)), inline.errors.join('\n'));
});

test('runChecks with content: a command in a raw HTML <code> or <pre> element fails on the setup page', () => {
  const html = check(page(SETUP, 'Run <code>export ANTHROPIC_API_KEY</code> first.\n\n<pre>unset ANTHROPIC_API_KEY</pre>'));
  const commands = html.errors.filter((e) => /^content: docs\/00-setup-check\.md: line \d+: ANTHROPIC_API_KEY in an export, set, setx or unset command/.test(e));
  assert.equal(commands.length, 2, html.errors.join('\n'));
  assert.ok(html.lines.some((line) => line.startsWith('FAIL content (2 errors)')), html.lines.join('\n'));
});

test('runChecks with content: an unclosed {/* in inline code does not hide a PowerShell line or a tab group from the rules', () => {
  const group = (id, line) =>
    `Type \`{/*\` with care.\n\n<Tabs groupId="${id}">\n<TabItem value="a" label="macOS/Linux">\n\n\`\`\`bash\nls\n\`\`\`\n\n</TabItem>\n<TabItem value="b" label="Windows PowerShell">\n\n\`\`\`powershell\n${line}\n\`\`\`\n\n</TabItem>\n</Tabs>`;
  const invented = check(page(SETUP, group('os', 'Remove-Item -Recurse inventedfolder')));
  assert.ok(invented.errors.some((e) => /^content: .*the PowerShell line "Remove-Item -Recurse inventedfolder" is not a command line/.test(e)), invented.errors.join('\n'));
  assert.ok(invented.lines.some((line) => line.startsWith('FAIL content')), invented.lines.join('\n'));
  const unknown = check(page(SETUP, group('nope', 'Get-Command jdtls')));
  assert.ok(unknown.errors.some((e) => /^content: .*unknown tab groupId "nope"/.test(e)), unknown.errors.join('\n'));
  assert.deepEqual(check(page(SETUP, group('os', 'Get-Command jdtls'))).errors, [], 'the same page with a pack line is fine');
});

test('runChecks with content: a tag in inline code neither ends the Windows tab early nor counts as a tab group (K1, K2)', () => {
  const k2 = check(
    page(
      SETUP,
      '<Tabs groupId="os">\n<TabItem value="a" label="macOS/Linux">\n\n```bash\nls\n```\n\n</TabItem>\n<TabItem value="b" label="Windows PowerShell">\n\nClose with `</TabItem>` later.\n\n```bash\nRemove-Item -Recurse inventedfolder\n```\n\n</TabItem>\n</Tabs>',
    ),
  );
  assert.ok(k2.errors.some((e) => /^content: docs\/00-setup-check\.md: line \d+: the PowerShell line "Remove-Item -Recurse inventedfolder" is not a command line/.test(e)), k2.errors.join('\n'));
  assert.ok(k2.lines.some((line) => line.startsWith('FAIL content (1 errors)')), k2.lines.join('\n'));
  const k1 = check(page(SETUP, 'Docusaurus writes `<Tabs groupId="os">` for this.'));
  assert.deepEqual(k1.errors, []);
  assert.ok(k1.lines.some((line) => line.startsWith('ok   content')), k1.lines.join('\n'));
});

// The pack folder the rules read is options.workshopDir: a scratch copy of the real pack, with
// or without one of its files.
function withScratchPack(skip, run) {
  const real = fileURLToPath(new URL('../../docs/spec-pack/', import.meta.url));
  const scratch = mkdtempSync(join(tmpdir(), 'guide-pack-'));
  try {
    for (const rel of Object.values(SOURCE_FILES)) {
      if (skip.includes(rel)) continue;
      mkdirSync(dirname(join(scratch, rel)), {recursive: true});
      copyFileSync(join(real, rel), join(scratch, rel));
    }
    mkdirSync(join(scratch, 'workshop'), {recursive: true});
    return run(join(scratch, 'workshop'));
  } finally {
    rmSync(scratch, {recursive: true, force: true});
  }
}

test('runChecks with content: the coverage and content rules read the pack from options.workshopDir', () => {
  const full = withScratchPack([], (workshopDir) => check({}, {workshopDir}));
  assert.deepEqual(full.errors, [], 'a complete copy of the pack behaves like the real one');
  // A pack without the briefs: every entry of the briefs fails the anchoring, and only those.
  const briefs = ENTRIES.filter((e) => e.source === 'briefs');
  const noBriefs = withScratchPack([SOURCE_FILES.briefs], (workshopDir) => check({}, {workshopDir}));
  const coverage = noBriefs.errors.filter((e) => e.startsWith('coverage:'));
  assert.equal(coverage.length, briefs.length, coverage.join('\n'));
  assert.ok(coverage.every((e) => /the pack file workshop\/level-4-feature-briefs\.md is missing/.test(e)), coverage.join('\n'));
  assert.ok(noBriefs.lines.some((line) => line.startsWith('FAIL coverage')), noBriefs.lines.join('\n'));
  // The PowerShell rule reads the same pack: a line that only the SDD guide states passes with the guide and fails without it.
  const install = 'irm https://openspec.dev/cli/install.ps1 | iex';
  const tab = `<Tabs groupId="os">\n<TabItem value="a" label="macOS/Linux">\n\n\`\`\`bash\nls\n\`\`\`\n\n</TabItem>\n<TabItem value="b" label="Windows PowerShell">\n\n\`\`\`powershell\n${install}\n\`\`\`\n\n</TabItem>\n</Tabs>`;
  const withGuide = withScratchPack([], (workshopDir) => check(page(SETUP, tab), {workshopDir}));
  assert.deepEqual(withGuide.errors, []);
  const withoutGuide = withScratchPack([SOURCE_FILES.sdd], (workshopDir) => check(page(SETUP, tab), {workshopDir}));
  assert.ok(withoutGuide.errors.some((e) => e.startsWith('content: ') && e.includes(`the PowerShell line "${install}"`)), withoutGuide.errors.join('\n'));
});

test('runChecks with content: a malformed manifest (a duplicate id) fails the coverage line', () => {
  const {lines, errors} = check({[SETUP]: FINISHED}, {manifest: {...TINY, entries: [TINY.entries[0], TINY.entries[0]]}});
  assert.ok(errors.some((e) => /^coverage: entry SU-01: the id is used twice$/.test(e)), errors.join('\n'));
  assert.ok(lines.some((line) => line.startsWith('FAIL coverage')), lines.join('\n'));
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
  // A copy of the guide with the skeleton snapshot as docs/ and the pack next to it, checked by
  // the copy's own check.mjs without a root (the default root, as `npm run check` runs it).
  withSkeletonGuideCopy((guide) => {
    for (const cwd of [tmpdir(), join(guide, '..'), guide]) {
      const result = spawnSync(process.execPath, [join(guide, 'scripts', 'check.mjs')], {encoding: 'utf8', cwd});
      // T11: the skeleton ban fails the default root with exactly one error per marked page, the 11 skeleton pages
      // here, and no other error. The coverage and content lines below are unchanged.
      assert.equal(result.status, 1, `cwd ${cwd}\n${result.stdout}${result.stderr}`);
      const stderr = result.stderr.split('\n').filter(Boolean);
      const skeletons = Object.keys(REAL).filter((file) => REAL[file].includes(SKELETON_MARKER)).sort();
      assert.equal(skeletons.length, 11);
      assert.deepEqual(stderr.slice(0, -1).map((line) => /^skeleton: (docs\/\S+): /.exec(line)?.[1]), skeletons, `cwd ${cwd}\n${result.stderr}`);
      assert.equal(stderr.at(-1), 'check failed with 11 error(s)', `cwd ${cwd}`);
      assert.match(result.stdout, new RegExp(`^ok {3}coverage \\(anchored ${ENTRIES.length}/${ENTRIES.length}`, 'm'));
      assert.match(result.stdout, /^ok {3}content \(11 pages/m);
      assert.match(result.stdout, new RegExp(`^pending coverage entries: ${ENTRIES.length} on 10 skeleton pages`, 'm'));
    }
  });
});

// The live guide, skeleton or finished page by page: the command line on the default root reports
// exactly what runChecks finds with every rule on, from any working directory, and the pending
// entries are those of the pages that still carry the marker. Whether the live pages pass is
// the second half of `npm run check` (node scripts/check.mjs), not this test.
test('cli: the live guide gets the coverage and content rules from any working directory, its pending entries follow its skeleton markers', () => {
  const live = runChecks(GUIDE_ROOT, {structure: true, content: true});
  const skeletons = Object.entries(PAGE_FILES).filter(([, file]) => readFileSync(join(GUIDE_ROOT, file), 'utf8').includes(SKELETON_MARKER));
  const pending = skeletons
    .map(([code, file]) => [file, ENTRIES.filter((e) => e.page === code).map((e) => e.id).sort()])
    .filter(([, ids]) => ids.length > 0)
    .sort(([a], [b]) => (a < b ? -1 : 1));
  const count = pending.reduce((sum, [, ids]) => sum + ids.length, 0);
  const at = live.lines.indexOf(`pending coverage entries: ${count} on ${pending.length} skeleton pages`);
  assert.ok(at !== -1, live.lines.join('\n'));
  assert.deepEqual(
    live.lines.slice(at + 1).filter((line) => line.startsWith('  pending entries ')),
    pending.map(([file, ids]) => `  pending entries ${file}: ${ids.length} (${ids.join(', ')})`),
  );
  for (const [, file] of skeletons) {
    assert.deepEqual(live.errors.filter((e) => e.startsWith('coverage: ') && e.includes(`: ${file}: `)), [], `${file} still carries the marker`);
  }
  for (const rule of ['coverage', 'content', 'structure']) {
    assert.ok(live.lines.some((line) => line.startsWith(`ok   ${rule} (`) || line.startsWith(`FAIL ${rule} (`)), rule);
  }
  const stderr = live.errors.length === 0 ? [] : [...live.errors, `check failed with ${live.errors.length} error(s)`];
  for (const cwd of [tmpdir(), join(GUIDE_ROOT, '..'), GUIDE_ROOT]) {
    const result = run([], cwd);
    assert.equal(result.status, live.errors.length === 0 ? 0 : 1, `cwd ${cwd}\n${result.stdout}${result.stderr}`);
    assert.deepEqual(result.stdout.trimEnd().split('\n'), live.lines, `cwd ${cwd}`);
    assert.deepEqual(result.stderr.split('\n').filter(Boolean), stderr, `cwd ${cwd}`);
  }
});

// Escalated round, through the whole checker: the paragraph model of the span recogniser, the
// tag reader, container fences, <pre> in a Windows tab, the Env:\ form and the allow-list pins.
const SU13 = 'Use WSL and install as on Linux there.';
const su13Errors = (body) => check({[SETUP]: replaceOnce(REAL[SETUP], SKELETON_MARKER, body)}).errors.filter((e) => e.startsWith('coverage: SU-13: '));
const windowsTab = (windows) =>
  `<Tabs groupId="os">\n<TabItem value="a" label="macOS/Linux">\n\n\`\`\`bash\nls\n\`\`\`\n\n</TabItem>\n<TabItem value="b" label="Windows PowerShell">\n\n${windows}\n\n</TabItem>\n</Tabs>`;
const INVENTED_FENCE = '```bash\nRemove-Item -Recurse inventedfolder\n```';
const oneContentError = (result, pattern) => {
  assert.equal(result.errors.length, 1, result.errors.join('\n'));
  assert.match(result.errors[0], pattern);
  assert.ok(result.lines.includes('FAIL content (1 errors)'), result.lines.join('\n'));
};
const SETUP_INVENTED = /^content: docs\/00-setup-check\.md: line \d+: the PowerShell line "Remove-Item -Recurse inventedfolder" is not a command line/;

test('runChecks with content: a comment between two lone backticks across a list item, a heading or a table cell is hidden text (B10, B11, B12)', () => {
  const missing = [`coverage: SU-13: ${SETUP}: the text "${SU13}" is not on the page`];
  assert.deepEqual(su13Errors(`- Press the \` key {/* ${SU13} */}\n- then \` done`), missing, 'B10');
  assert.deepEqual(su13Errors(`Press the \` key {/* ${SU13} */}\n## Then \` done`), missing, 'B11');
  assert.deepEqual(su13Errors(`| Press \` | {/* ${SU13} */} | then \` |`), missing, 'B12');
  assert.deepEqual(su13Errors(`| Key | Note |\n| - | - |\n| Press \` | {/* ${SU13} */} | then \` |`), missing, 'B12 in a table');
  // The same sentence as visible text is on the page: the comment alone decides.
  assert.deepEqual(su13Errors(`- Press the \` key ${SU13}\n- then \` done`), []);
});

test('runChecks with content: lone backticks, a ``` line in a span and an escaped backslash end no Windows tab early (K5, K7, K8)', () => {
  const k5 = '<Tabs groupId="os">\n<TabItem value="a" label="macOS/Linux">\nUse the ` key\n</TabItem>\n<TabItem value="b" label="Windows PowerShell">\nand the ` key.\n```bash\nRemove-Item -Recurse inventedfolder\n```\n</TabItem>\n</Tabs>';
  oneContentError(check(page(SETUP, k5)), SETUP_INVENTED);
  oneContentError(check(page(SETUP, windowsTab(`Close with \`</TabItem>\n\`\`\` no fence \` later.\n\n${INVENTED_FENCE}`))), SETUP_INVENTED);
  oneContentError(check(page(SETUP, windowsTab(`Close with \\\\\`</TabItem>\` later.\n\n${INVENTED_FENCE}`))), SETUP_INVENTED);
});

test('runChecks with content: a closing tag MDX renders as text, escaped or in a string literal, ends no Windows tab (K3, K4, K4b)', () => {
  for (const text of ['Close with \\</TabItem> later.', "Close with {'</TabItem>'} later.", 'Close with {"</TabItem>"} later.']) {
    oneContentError(check(page(SETUP, windowsTab(`${text}\n\n${INVENTED_FENCE}`))), SETUP_INVENTED);
  }
});

test('runChecks with content: a comment opener MDX renders as text, escaped or in a string of an expression, hides no Windows line', () => {
  for (const intro of ['Type \\{/* to start a comment.', "Type {'{/*'} to start a comment."]) {
    oneContentError(check(page(SETUP, `${intro}\n\n${windowsTab(INVENTED_FENCE)}`)), SETUP_INVENTED);
  }
});

test('runChecks with content: a fence after a block quote or a list marker in the Windows tab is checked (RA-D)', () => {
  for (const fence of ['> ```powershell\n> Remove-Item -Recurse inventedfolder\n> ```', '- ```powershell\n  Remove-Item -Recurse inventedfolder\n  ```']) {
    oneContentError(check(page(SETUP, windowsTab(fence))), SETUP_INVENTED);
  }
});

test('runChecks with content: a raw <pre> element in the Windows tab is checked, a pack line in it passes (RA-E)', () => {
  oneContentError(check(page(SETUP, windowsTab('<pre>Remove-Item -Recurse inventedfolder</pre>'))), SETUP_INVENTED);
  assert.deepEqual(check(page(SETUP, windowsTab('<pre>Get-Command jdtls</pre>'))).errors, []);
});

test('the allow-list of the real pack holds the collapsed form of its one span over a line break (PK-3)', () => {
  const pack = Object.values(SOURCE_FILES).map((rel) => readFileSync(new URL(`../../docs/spec-pack/${rel}`, import.meta.url), 'utf8'));
  assert.ok(packCommandLines(pack).has('/plugin install jdtls-lsp@claude-plugins-official'));
});

test('runChecks with content: Set-Item -Path Env:\\ANTHROPIC_API_KEY fails on the setup page as the Env: form (RA-F)', () => {
  oneContentError(check(page(SETUP, '```bash\nSet-Item -Path Env:\\ANTHROPIC_API_KEY -Value x\n```')), /^content: docs\/00-setup-check\.md: line \d+: ANTHROPIC_API_KEY in a \$env: or Env: form/);
});

test('runChecks with content: a list item ends a span and an escaped backslash does not escape the backtick after it (E8, E9)', () => {
  assert.deepEqual(check(page(SETUP, '- Press the ` key\n- Never set ANTHROPIC_API_KEY, really `')).errors, []);
  oneContentError(check(page(SETUP, 'Run \\\\`export ANTHROPIC_API_KEY` now.')), /^content: docs\/00-setup-check\.md: line \d+: ANTHROPIC_API_KEY in an export, set, setx or unset command/);
});

test('runChecks with content: a real <img> between two lone backticks across a heading, a list item or table rows fails (FI4 to FI7)', () => {
  const img = '<img src="https://example.com/x.png" alt="x" />';
  for (const extra of [
    `Press the \` key.\n## Keys ${img} and \``,
    `- Press the \` key\n- ${img} then \``,
    `| a | b |\n| - | - |\n| Press \` | key |\n| ${img} | then \` |`,
    `| \` | ${img} | \` |`,
  ]) {
    const result = check(page(LEVEL2, extra));
    assert.equal(result.errors.length, 1, `${extra}\n${result.errors.join('\n')}`);
    assert.ok(result.errors[0].startsWith(`content: ${LEVEL2}: line `) && result.errors[0].endsWith('a raw <img> tag, use Markdown image syntax'), result.errors[0]);
    assert.ok(result.lines.includes('FAIL content (1 errors)'), result.lines.join('\n'));
  }
});
