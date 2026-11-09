import {test} from 'node:test';
import assert from 'node:assert/strict';
import {existsSync, readFileSync, readdirSync} from 'node:fs';
import {dirname, join, relative, sep} from 'node:path';
import {fileURLToPath} from 'node:url';
import {ENTRIES, MANUAL_ROWS, PAGE_FILES, SOURCE_FILES} from './coverage.mjs';
import {coverageErrors, fold, manifestShapeErrors} from './rules/coverage.mjs';
import {SKELETON_DOCS} from './fixtures/skeleton-docs.mjs';

// The real manifest against the real pack and the real pages. The pack lives on the factory
// branch next to guide/, found relative to this file like the checker does. The real pages are
// the skeleton snapshot (guide/docs at 05e58a2, every page a skeleton), so these tests hold while
// the content tasks fill in the live pages; the page-set test reads the live guide/docs.
const GUIDE = fileURLToPath(new URL('..', import.meta.url));
const PACK = fileURLToPath(new URL('../../docs/spec-pack/', import.meta.url));

const readPack = () => Object.fromEntries(Object.entries(SOURCE_FILES).map(([source, rel]) => [source, readFileSync(join(PACK, rel), 'utf8')]));

function readPages(docs = SKELETON_DOCS) {
  return Object.fromEntries(
    readdirSync(docs, {recursive: true, withFileTypes: true})
      .filter((entry) => entry.isFile() && /\.mdx?$/.test(entry.name))
      .map((entry) => [relative(dirname(docs), join(entry.parentPath, entry.name)).split(sep).join('/'), readFileSync(join(entry.parentPath, entry.name), 'utf8')]),
  );
}

const run = (entries = ENTRIES, over = {}) =>
  coverageErrors({entries, manualRows: MANUAL_ROWS, sourceFiles: SOURCE_FILES, pageFiles: PAGE_FILES, pack: readPack(), pages: readPages(), ...over});

// The rows of the research table "source line -> item -> page" (res-3 section 9), written out
// by hand: a second copy, so that dropping the only entry of a row cannot go unnoticed.
const SECTION_9_ROWS = [
  'WP:50', 'WP:51', 'WP:52', 'WP:53', 'WP:54', 'WP:55', 'WP:56', 'WP:61-66', 'WP:68-69', 'WP:74-75', 'WP:78-79',
  'WP:92-93', 'WP:95-98', 'WP:101', 'WP:102-103', 'WP:104', 'WP:105-110', 'WP:111-113', 'WP:114-116', 'WP:117-119',
  'WP:134-135', 'WP:138-139', 'WP:140-147', 'WP:148-151', 'WP:152-155', 'WP:156-159', 'WP:160', 'WP:162-166',
  'WP:182-184', 'WP:190', 'WP:191-194', 'WP:195-196', 'WP:197-199', 'WP:200-202', 'WP:205', 'WP:207-208', 'WP:209-210',
  'WP:211-213', 'WP:214', 'WP:216', 'WP:218-221',
  'WP:232-233', 'WP:236-245', 'WP:246-247', 'WP:250-259', 'WP:260-262', 'WP:263-264', 'WP:265', 'WP:266-268',
  'WP:270-271', 'WP:274-277', 'WP:278', 'WP:279-282', 'WP:284-285', 'WP:287-290',
  'WP:303-307', 'WP:311-312', 'WP:314-315', 'WP:317-321',
  'SDD:93-97', 'SDD:99-102', 'SDD:104-115', 'SDD:117-127', 'SDD:129-137', 'FB:6-12, 14-20, 22-36',
  'WP:326-332', 'WP:329', 'WP:330-331', 'S11:34', 'SDD:146-152', 'WP:25-35', 'S11:38',
  'AS:19-30, 32-41, 45-55, 65-73, 158', 'cheat sheet rows (section 2.9)',
];

test('manifest: every row of the research table is accounted for, by an entry or by a manual row with a reason', () => {
  const entryRows = new Set(ENTRIES.map((e) => e.row));
  const manualRows = new Set(MANUAL_ROWS.map((r) => r.row));
  const missing = SECTION_9_ROWS.filter((row) => !entryRows.has(row) && !manualRows.has(row));
  assert.deepEqual(missing, []);
  assert.equal(SECTION_9_ROWS.length, 74, 'the pinned research table has 74 rows');
  assert.equal(new Set(SECTION_9_ROWS).size, 74);
});

test('manifest: the pages of the levels are covered by entries, the rows without one are manual rows of the same page', () => {
  const withEntries = new Set(ENTRIES.map((e) => e.page));
  assert.deepEqual([...withEntries].sort(), ['CS', 'IX', 'L0', 'L1', 'L2', 'L3', 'L4', 'SU', 'TS', 'WU']);
  assert.ok(MANUAL_ROWS.some((row) => row.page === 'AR'), 'the architecture page is accounted for by a manual row');
  for (const row of MANUAL_ROWS) assert.ok(row.reason.length >= 40, `${row.row}: a manual row says why in a sentence`);
});

test('manifest: the shape is valid, ids are unique and carry the page code, every field is plain ASCII', () => {
  assert.deepEqual(manifestShapeErrors({entries: ENTRIES, manualRows: MANUAL_ROWS, sourceFiles: SOURCE_FILES, pageFiles: PAGE_FILES}), []);
  for (const entry of ENTRIES) {
    assert.ok(entry.id.startsWith(`${entry.page}-`), `${entry.id} carries its page ${entry.page}`);
    for (const value of Object.values(entry)) assert.doesNotMatch(String(value), /[^\x00-\x7F]/, entry.id);
  }
});

test('manifest: every entry is anchored in its pack section, and the count is the manifest size (anchored N/N)', () => {
  const result = run();
  assert.deepEqual(result.errors, []);
  assert.equal(result.total, ENTRIES.length);
  assert.equal(result.anchored, ENTRIES.length);
  assert.ok(ENTRIES.length >= 290, `the manifest holds ${ENTRIES.length} entries, 290 and more are pinned`);
});

test('manifest: each page has at least its pinned number of entries, so entries cannot be dropped quietly', () => {
  const floor = {SU: 21, L0: 23, L1: 33, L2: 28, L3: 34, L4: 65, WU: 22, IX: 10, CS: 52, TS: 16};
  for (const [page, minimum] of Object.entries(floor)) {
    const count = ENTRIES.filter((e) => e.page === page).length;
    assert.ok(count >= minimum, `${page} has ${count} entries, at least ${minimum} are pinned`);
  }
});

test('manifest: the sources and the pages are the five pack files and the eleven pages, and they exist', () => {
  assert.deepEqual(Object.keys(SOURCE_FILES), ['plan', 'sdd', 'briefs', 'spec10', 'spec09']);
  for (const rel of Object.values(SOURCE_FILES)) assert.ok(existsSync(join(PACK, rel)), rel);
  assert.deepEqual(
    Object.values(PAGE_FILES).sort(),
    Object.keys(readPages(join(GUIDE, 'docs'))).sort(),
    'the pages of the manifest are exactly the md and mdx files of docs/',
  );
});

test('manifest: with every page still a skeleton the entries are pending per page and rule (b) is silent', () => {
  const result = run();
  assert.deepEqual(Object.keys(result.pending).sort(), Object.values(PAGE_FILES).filter((f) => !f.endsWith('architecture.md')).sort());
  const pending = Object.values(result.pending).flat();
  assert.equal(pending.length, ENTRIES.length);
});

test('manifest: an entry whose snippet is absent from the pack fails and is not anchored (probe)', () => {
  const broken = ENTRIES.map((e) => (e.id === 'SU-01' ? {...e, snippet: 'claude --versionn'} : e));
  const result = run(broken);
  assert.equal(result.anchored, ENTRIES.length - 1);
  assert.match(result.errors.join('\n'), /^coverage: SU-01: the snippet "claude --versionn" is not in workshop\/workshop-plan\.md, section "3\. Setup check"/);
});

test('manifest: SU-21 pins the sentence "Node.js is not needed": a setup page that says Node.js is required fails it', () => {
  const su = ENTRIES.filter((e) => e.id === 'SU-21');
  const page = (body) => ({pages: {...readPages(), [PAGE_FILES.SU]: `---\ntitle: Setup check\n---\n\n${body}\n`}});
  assert.deepEqual(run(su, page('Node.js is not needed: the build installs its own copy.')).errors, []);
  assert.match(run(su, page('You need Node.js 22 installed.')).errors.join('\n'), /^coverage: SU-21: docs\/00-setup-check\.md: the text "Node\.js is not needed" is not on the page/);
});

test('manifest: an entry attributed to the wrong pack section fails (an L1 command in the Level 3 section)', () => {
  const moved = ENTRIES.map((e) => (e.id === 'L1-04' ? {...e, section: LEVEL3} : e));
  assert.match(run(moved).errors.join('\n'), /L1-04: the snippet "\.\/mvnw -pl bookstore test -Dtest=BookTest" is not in .*section "8\. Level 3"/);
});

// The independent literal sweep (the guard against silent exclusion): every inline code span
// and every fenced-block line of the participant-useful sections of the workshop plan, the SDD
// guide and the briefs is contained in a snippet of a manifest entry that has the same source,
// whose section holds that very line of the pack, and whose page is the page that section
// belongs to (PAGES below). A snippet of another section or of another page does not count, or
// is listed here with its reason.
const LEVEL3 = '8. Level 3';

// Where a literal sits: {literal, line}, line being the 0-based line of the pack file.
function literalsOf(text, {from, to, skipSections = [], skipParagraphs = [], skipLists = []}) {
  const lines = text.split('\n');
  const start = lines.findIndex((line) => line.startsWith(from));
  const end = to === undefined ? lines.length : lines.findIndex((line, index) => index > start && line.startsWith(to));
  assert.ok(start !== -1 && end !== -1 && start < end, `the sweep range ${from} .. ${to} exists`);
  const found = [];
  let fenced = false;
  let skipping = false;
  let skippingList = false;
  let paragraph = [];
  const flush = () => {
    const rows = paragraph;
    paragraph = [];
    const joined = rows.map((row) => row.text).join(' ');
    if (skipParagraphs.some((prefix) => joined.startsWith(prefix))) return;
    // The line of a span is the line of the paragraph row that holds its first character.
    const starts = [];
    rows.reduce((offset, row) => (starts.push(offset), offset + row.text.length + 1), 0);
    for (const m of joined.matchAll(/(?<!`)`([^`]+)`(?!`)/g)) {
      const row = starts.filter((s) => s <= m.index).length - 1;
      found.push({literal: m[1].replace(/\\\|/g, '|'), line: rows[row].index});
    }
  };
  lines.slice(start, end).forEach((line, offset) => {
    const index = start + offset;
    if (/^#{2,3}\s/.test(line)) skipping = skipSections.some((name) => line.includes(name));
    if (skipping) return;
    if (skippingList) {
      // A "Level cleared when" list is checked by the structure rule: it ends at the blank line.
      if (line.trim() !== '') return;
      skippingList = false;
    }
    if (skipLists.some((prefix) => line.startsWith(prefix))) {
      flush();
      skippingList = true;
      return;
    }
    if (/^\s*(```|~~~)/.test(line)) {
      flush();
      fenced = !fenced;
      return;
    }
    if (fenced) {
      const literal = line.trim().replace(/\s+#.*$/, '').trim();
      if (literal) found.push({literal, line: index});
    } else if (line.trim() === '') {
      flush();
    } else {
      paragraph.push({text: line.trim(), index});
    }
  });
  flush();
  return found;
}

// The 0-based lines [from, to) of the pack section that starts with `name`: to the next heading
// of the same or a higher level. Own scanner (fence-aware), not the checker's packSection.
function sectionLines(text, name) {
  const lines = text.split('\n');
  const heads = [];
  let fenced = false;
  lines.forEach((line, index) => {
    if (/^\s*(```|~~~)/.test(line)) fenced = !fenced;
    const m = !fenced && /^(#{1,6})\s+(.*?)\s*$/.exec(line);
    if (m) heads.push({index, level: m[1].length, title: m[2]});
  });
  const [head] = heads.filter((h) => h.title.startsWith(name));
  assert.ok(head, `the pack has a section "${name}"`);
  const next = heads.find((h) => h.index > head.index && h.level <= head.level);
  return [head.index, next ? next.index : lines.length];
}

// The page that carries each part of the pack: the pages of the manifest for the sections of
// the workshop plan, and Level 4 for the SDD tracks and the briefs. A part may list a second
// page when the pack states the same literal for two pages.
const SWEEPS = [
  [
    'plan',
    {
      from: '## 3. Setup check',
      to: '## 10. Wrap-up',
      skipSections: ['6. Break'],
      skipParagraphs: ['Facilitator fallback kit', '**Expected findings'],
      skipLists: ['**Level cleared when**'],
      pages: [['3. Setup check', ['SU']], ['4. Level 0', ['L0']], ['5. Level 1', ['L1']], ['7. Level 2', ['L2']], ['8. Level 3', ['L3']], ['9. Level 4', ['L4', 'CS']]],
    },
  ],
  ['sdd', {from: '## Part 2', to: '---', skipSections: ['Level cleared when'], pages: [['Part 2', ['L4']]]}],
  ['briefs', {from: '## Brief A', to: '---', pages: [['Brief A', ['L4']], ['Brief B', ['L4']], ['Brief C', ['L4']]]}],
];

// What no entry of its own section and page covers, keyed "source:line literal" (the 1-based
// line of the pack file; the pack is frozen on this branch, so a pack edit that moves a line
// fails this test and the new place gets reviewed), with the reason. Everything else of the
// sweep is in the manifest. A literal that another section or page covers (CS-08 and L1-27 hold
// "/clear") is no reason: that is the case the sweep exists to catch.
const SWEEP_EXCLUDED = {
  'plan:46 PATH': 'a word of the prerequisites sentence: SU-06 and SU-07 carry the jdtls binary check, SU-19 to SU-21 the other prerequisites',
  'plan:65 PATH': 'the wrapper-script half of the Other Linux row, which SU-12 carries; the word PATH alone says nothing to check',
  'plan:68 jdtls-lsp': 'the plugin is installed in Level 2 Part A, which SU-15 carries and L2-04 shows as a command; the plugin id is the sentence subject here',
  'plan:89 main': 'Start branch line of the Level 0 box, checked by the structure rule',
  'plan:89 level-0-start': 'Start branch line of the Level 0 box, checked by the structure rule',
  'plan:89 CLAUDE.md': 'the Level 0 premise that the repo has none, nothing to do; the structure rule checks the Start line of the box',
  'plan:89 .claude/': 'the Level 0 premise that the repo has none, nothing to do; the structure rule checks the Start line of the box',
  'plan:108 double': 'a hint inside the money question, which L0-11 (for money) carries',
  'plan:132 solution/level-1': 'Catch-up and Compare line of the Level 1 box, checked by the structure rule',
  'plan:143 Money': 'the type name inside the CLAUDE.md rule about money, which L1-08 (never double) carries',
  'plan:180 solution/level-1': 'Start line of the Level 2 box, checked by the structure rule',
  'plan:180 solution/level-2': 'Catch-up and Compare line of the Level 2 box, checked by the structure rule',
  'plan:182 which jdtls': 'the facilitator demo sentence, where the participant steps carry it: L2-02 (Part A) and SU-06',
  'plan:182 /plugin install jdtls-lsp@claude-plugins-official': 'the facilitator demo sentence, where the participant steps carry it: L2-04 (Part A)',
  'plan:183 /plugin': 'the facilitator demo sentence about the Installed tab, which L2-09 carries',
  'plan:183 claude mcp add': 'the facilitator demo sentence, where the participant steps carry it: L2-20 (Part B)',
  'plan:193 solution/level-2': 'the plugin declaration claim of the project scope sentence (follow-up F4); the file itself is L2-08',
  'plan:198 Found N new diagnostic issues ...': 'the same text as L2-12, which stops before the ellipsis',
  'plan:230 solution/level-2': 'Start line of the Level 3 box, checked by the structure rule',
  'plan:230 solution/level-3': 'Catch-up and Compare line of the Level 3 box, checked by the structure rule',
  'plan:237 solution/level-2': 'the plugin declaration claim of the settings.json paragraph (follow-up F4); the file itself is L3-32',
  'plan:237 jdtls-lsp': 'the plugin declaration claim of the settings.json paragraph (follow-up F4); the file itself is L3-32',
  'plan:250 .claude/settings.json': 'the same file as L3-32 and L2-08, which the Permissions part names; the hooks step only says where the block goes',
  'plan:260 ./mvnw -q verify': 'the command of the hook script that spec 10 prints in full, which L3-16 carries',
  'plan:267 --no-verify': 'the escape hatch discussion, carried by L3-14 (escape hatch) and L3-29 (the spec 10 scenario command)',
  'plan:267 -n': 'the short form of --no-verify in the same discussion, carried by L3-14 and the deny rule in L3-02',
  'plan:285 git diff': 'a hint of the stretch subagent sentence, which L3-26 and L3-27 carry',
  'plan:300 solution/level-3': 'the Level 4 box, whose start branches are L4-01 to L4-03 and L4-63 (built from solution/level-3)',
  'plan:307 /plan': 'the Track C row of the table, which repeats Part 2 of the SDD guide (L4-37 carries /plan there)',
  'plan:309 sdd-framework-guide.md': 'an internal path: the deny-list bans it on participant pages',
  'plan:311 level-4-feature-briefs.md': 'an internal path: the deny-list bans it on participant pages',
  'sdd:99 .mcp.json': 'a description of what the start branches contain, which depends on the plugin declaration claim (follow-up F4)',
  'sdd:99 jdtls-lsp': 'a description of what the start branches contain, which depends on the plugin declaration claim (follow-up F4)',
  'sdd:100 /ship': 'a description of what the start branches contain; /ship is a step of every track (L4-19, L4-32, L4-42)',
  'sdd:101 level-4-feature-briefs.md': 'an internal path: the deny-list bans it on participant pages',
  'sdd:114 openspec ...': 'notation of the Track A gotcha, covered by L4-22',
  'sdd:114 /opsx:...': 'notation of the Track A gotcha, covered by L4-22',
};

// The literals no entry covers, as [{literal, source, line, pages}] (line is 1-based).
function uncovered(entries) {
  const pack = readPack();
  const left = [];
  for (const [source, {pages, ...options}] of SWEEPS) {
    const text = pack[source];
    const ranges = pages.map(([name, codes]) => ({range: sectionLines(text, name), codes}));
    const own = entries.filter((e) => e.source === source).map((e) => ({e, snippet: fold(e.snippet), range: sectionLines(text, e.section)}));
    for (const {literal, line} of literalsOf(text, options)) {
      const home = ranges.find(({range}) => range[0] <= line && line < range[1]);
      assert.ok(home, `${source}:${line + 1} ${literal} is in a part that has a page`);
      const covered = own.some(({e, snippet, range}) => home.codes.includes(e.page) && range[0] <= line && line < range[1] && snippet.includes(fold(literal)));
      if (!covered) left.push({literal, source, line: line + 1, pages: home.codes});
    }
  }
  return left;
}

const sweep = (entries) => uncovered(entries).map((u) => `${u.source}:${u.line} ${u.literal}`).sort();

test('manifest: the literal sweep of the pack leaves exactly the listed exclusions (no entry silently dropped)', () => {
  assert.deepEqual(sweep(ENTRIES), Object.keys(SWEEP_EXCLUDED).sort());
});

test('manifest: the literal sweep notices a dropped entry (witness)', () => {
  const dropped = ENTRIES.filter((e) => e.id !== 'L1-04');
  assert.ok(sweep(dropped).some((u) => u.endsWith(' ./mvnw -pl bookstore test -Dtest=BookTest')));
  const droppedFence = ENTRIES.filter((e) => e.id !== 'SU-08');
  assert.ok(sweep(droppedFence).some((u) => u.endsWith(' git clone https://github.com/MaikKingma/AgenticBookStore.git && cd AgenticBookStore')));
});

test('manifest: the sweep counts coverage per section and per page, a snippet of another section or page does not cover a literal', () => {
  // "/clear" is also in L1-27 (Level 1 section, page L1) and CS-08 (cheat sheet), "./mvnw verify"
  // in SU-09, L0-06 and L1-03 (three sections of the plan, three pages).
  assert.ok(sweep(ENTRIES.filter((e) => e.id !== 'L4-64')).includes('sdd:110 /clear'), 'the Track A step is covered by its own entry only');
  assert.ok(sweep(ENTRIES.filter((e) => e.id !== 'SU-09')).includes('plan:56 ./mvnw verify'), 'the setup check command is covered by its own entry only');
  assert.ok(sweep(ENTRIES.filter((e) => e.id !== 'L4-65')).includes('sdd:97 claude'), 'the common start line is covered by its own entry only');
  assert.ok(sweep(ENTRIES.filter((e) => e.id !== 'L3-34')).includes('plan:276 name'), 'the skill frontmatter field is covered by its own entry only');
  // The right snippet on the wrong page, and on the right page but attributed to another section.
  const wrongPage = ENTRIES.map((e) => (e.id === 'L4-11' ? {...e, page: 'WU'} : e));
  assert.ok(sweep(wrongPage).includes('sdd:105 /opsx:explore'));
  const wrongSection = ENTRIES.map((e) => (e.id === 'L4-18' ? {...e, section: 'Track B: Spec Kit'} : e));
  assert.ok(sweep(wrongSection).includes('sdd:110 /opsx:apply'));
});

test('manifest: every excluded literal of the sweep is still found uncovered (no stale exclusion)', () => {
  const found = new Set(sweep(ENTRIES));
  for (const key of Object.keys(SWEEP_EXCLUDED)) assert.ok(found.has(key), `${key} is excluded but the sweep no longer finds it uncovered`);
});

test('manifest: the three gaps the first review found each have an entry on their page (Track A /clear, the common start claude, the skill name)', () => {
  const byId = Object.fromEntries(ENTRIES.map((e) => [e.id, e]));
  assert.deepEqual([byId['L4-64'].page, byId['L4-64'].source, byId['L4-64'].section, byId['L4-64'].snippet], ['L4', 'sdd', 'Track A: OpenSpec', '/clear']);
  assert.deepEqual([byId['L4-65'].page, byId['L4-65'].source, byId['L4-65'].kind, byId['L4-65'].snippet], ['L4', 'sdd', 'command', 'claude # new session']);
  assert.deepEqual([byId['L3-34'].page, byId['L3-34'].source, byId['L3-34'].section, byId['L3-34'].snippet], ['L3', 'plan', 'Part C: A skill and a command', 'Frontmatter: `name`']);
});

test('manifest: the exclusions of the sweep each have a reason in a sentence', () => {
  for (const [literal, reason] of Object.entries(SWEEP_EXCLUDED)) assert.ok(reason.length >= 20, literal);
});

test('manifest: snippets shorter than 8 characters are the pinned short ones, each a real token of the pack', () => {
  const short = ENTRIES.filter((e) => e.snippet.length < 8).map((e) => `${e.id} ${e.snippet}`);
  // Pinned on purpose: a short snippet grades little (a slash command or one word that can
  // sit anywhere on its page), so a new one needs a reason from the next implementer. These
  // are the slash commands and tokens of the pack, each the only literal of its row.
  assert.deepEqual(short.sort(), [
    'CS-02 /status',
    'CS-04 /usage',
    'CS-05 /init',
    'CS-07 /memory',
    'CS-08 /clear',
    'CS-10 /rewind',
    'CS-12 /plugin',
    'CS-15 /mcp',
    'CS-19 /plan',
    'CS-20 /ship',
    'L0-03 claude',
    'L0-20 /rewind',
    'L1-02 /init',
    'L1-24 /memory',
    'L1-27 /clear',
    'L2-14 Errors',
    'L2-22 /mcp',
    'L3-05 /plan',
    'L3-25 /ship',
    'L4-37 /plan',
    'L4-64 /clear',
    'SU-02 /status',
    'TS-05 /usage',
    'TS-10 /status',
    'TS-16 /mcp',
  ]);
});
