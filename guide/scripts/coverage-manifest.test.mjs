import {test} from 'node:test';
import assert from 'node:assert/strict';
import {existsSync, readFileSync, readdirSync} from 'node:fs';
import {join, relative, sep} from 'node:path';
import {fileURLToPath} from 'node:url';
import {ENTRIES, MANUAL_ROWS, PAGE_FILES, SOURCE_FILES} from './coverage.mjs';
import {coverageErrors, fold, manifestShapeErrors} from './rules/coverage.mjs';

// The real manifest against the real pack and the real pages. The pack lives on the factory
// branch next to guide/, found relative to this file like the checker does.
const GUIDE = fileURLToPath(new URL('..', import.meta.url));
const PACK = fileURLToPath(new URL('../../docs/spec-pack/', import.meta.url));

const readPack = () => Object.fromEntries(Object.entries(SOURCE_FILES).map(([source, rel]) => [source, readFileSync(join(PACK, rel), 'utf8')]));

function readPages() {
  const docs = join(GUIDE, 'docs');
  return Object.fromEntries(
    readdirSync(docs, {recursive: true, withFileTypes: true})
      .filter((entry) => entry.isFile() && /\.mdx?$/.test(entry.name))
      .map((entry) => [relative(GUIDE, join(entry.parentPath, entry.name)).split(sep).join('/'), readFileSync(join(entry.parentPath, entry.name), 'utf8')]),
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
  const floor = {SU: 21, L0: 23, L1: 33, L2: 28, L3: 33, L4: 63, WU: 22, IX: 10, CS: 52, TS: 16};
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
    Object.keys(readPages()).sort(),
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

test('manifest: an entry attributed to the wrong pack section fails (an L1 command in the Level 3 section)', () => {
  const moved = ENTRIES.map((e) => (e.id === 'L1-04' ? {...e, section: LEVEL3} : e));
  assert.match(run(moved).errors.join('\n'), /L1-04: the snippet "\.\/mvnw -pl bookstore test -Dtest=BookTest" is not in .*section "8\. Level 3"/);
});

// The independent literal sweep (the guard against silent exclusion): every inline code span
// and every fenced-block line of the participant-useful sections of the workshop plan, the SDD
// guide and the briefs is contained in a manifest snippet, or is listed here with its reason.
const LEVEL3 = '8. Level 3';

function literalsOf(text, {from, to, skipSections = [], skipParagraphs = []}) {
  const lines = text.split('\n');
  const start = lines.findIndex((line) => line.startsWith(from));
  const end = to === undefined ? lines.length : lines.findIndex((line, index) => index > start && line.startsWith(to));
  assert.ok(start !== -1 && end !== -1 && start < end, `the sweep range ${from} .. ${to} exists`);
  const found = [];
  let fenced = false;
  let skipping = false;
  let paragraph = [];
  const flush = () => {
    const joined = paragraph.join(' ');
    paragraph = [];
    if (skipParagraphs.some((prefix) => joined.startsWith(prefix))) return;
    for (const m of joined.matchAll(/(?<!`)`([^`]+)`(?!`)/g)) found.push(m[1].replace(/\\\|/g, '|'));
  };
  for (const line of lines.slice(start, end)) {
    if (/^#{2,3}\s/.test(line)) skipping = skipSections.some((name) => line.includes(name));
    if (skipping) continue;
    if (/^\s*(```|~~~)/.test(line)) {
      flush();
      fenced = !fenced;
      continue;
    }
    if (fenced) {
      const literal = line.trim().replace(/\s+#.*$/, '').trim();
      if (literal) found.push(literal);
    } else if (line.trim() === '') {
      flush();
    } else {
      paragraph.push(line.trim());
    }
  }
  flush();
  return found;
}

const SWEEPS = [
  ['plan', {from: '## 3. Setup check', to: '## 10. Wrap-up', skipSections: ['6. Break'], skipParagraphs: ['Facilitator fallback kit', '**Expected findings']}],
  ['sdd', {from: '## Part 2', to: '---'}],
  ['briefs', {from: '## Brief A', to: '---'}],
];

// What no snippet contains, with the reason. Everything else of the sweep is in the manifest.
const SWEEP_EXCLUDED = {
  'level-0-start': 'Start branch line of the Level 0 box, checked by the structure rule',
  'solution/level-1': 'Start, Catch-up and Compare lines of the boxes (structure rule) and the plugin declaration claim (follow-up F4)',
  'solution/level-2': 'Start, Catch-up and Compare lines of the boxes (structure rule) and the plugin declaration claim (follow-up F4)',
  'Found N new diagnostic issues ...': 'the same text as L2-12, which stops before the ellipsis',
  'sdd-framework-guide.md': 'an internal path: the deny-list bans it on participant pages',
  'level-4-feature-briefs.md': 'an internal path: the deny-list bans it on participant pages',
  'openspec ...': 'notation of the Track A gotcha, covered by L4-22',
  '/opsx:...': 'notation of the Track A gotcha, covered by L4-22',
};

function sweep(entries) {
  const pack = readPack();
  const snippets = entries.map((e) => fold(e.snippet));
  const left = [];
  for (const [source, options] of SWEEPS) {
    for (const literal of literalsOf(pack[source], options)) {
      if (!snippets.some((s) => s.includes(fold(literal)))) left.push(literal);
    }
  }
  return [...new Set(left)].sort();
}

test('manifest: the literal sweep of the pack leaves exactly the listed exclusions (no entry silently dropped)', () => {
  assert.deepEqual(sweep(ENTRIES), Object.keys(SWEEP_EXCLUDED).sort());
});

test('manifest: the literal sweep notices a dropped entry (witness)', () => {
  const dropped = ENTRIES.filter((e) => e.id !== 'L1-04');
  assert.ok(sweep(dropped).includes('./mvnw -pl bookstore test -Dtest=BookTest'));
  const droppedFence = ENTRIES.filter((e) => e.id !== 'SU-08');
  assert.ok(sweep(droppedFence).includes('git clone https://github.com/MaikKingma/AgenticBookStore.git && cd AgenticBookStore'));
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
    'SU-02 /status',
    'SU-21 Node.js',
    'TS-05 /usage',
    'TS-10 /status',
    'TS-16 /mcp',
  ]);
});
