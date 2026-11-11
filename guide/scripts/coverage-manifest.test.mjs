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
  // IX rose from 10 to 16 in spec 15 (the five Format cells and the walk-around sentence); the other floors equal the
  // live counts, the five retargeted sync moment entries (L0-01, L1-01, L2-01, L3-01, L4-05) keep their ids and rows.
  // L2 rose from 28 to 34 in spec 15 task T3 (L2-29 to L2-34: the diagnosis prompt, the Java 21 check, the new session
  // check, the first Java edit, the Level 2 tip and the debug log sentence of amendment A4); L2-17 was retargeted in place.
  const floor = {SU: 21, L0: 23, L1: 33, L2: 34, L3: 34, L4: 65, WU: 22, IX: 16, CS: 52, TS: 16};
  for (const [page, minimum] of Object.entries(floor)) {
    const count = ENTRIES.filter((e) => e.page === page).length;
    assert.ok(count >= minimum, `${page} has ${count} entries, at least ${minimum} are pinned`);
  }
});

// Spec 15 (decision D19): the sync minutes went to solo time. IX-11 to IX-15 pin the five Format cells and IX-16 the
// sentence that Maik and Tom walk around; the old "Sync N, solo N" cell or a missing sentence on the welcome page fails.
test('manifest: the Format cells and the walk-around sentence of the welcome page are pinned (IX-11 to IX-16)', () => {
  const ix = ENTRIES.filter((e) => ['IX-11', 'IX-12', 'IX-13', 'IX-14', 'IX-15', 'IX-16'].includes(e.id));
  assert.equal(ix.length, 6);
  // The live welcome page (the snapshot pages are skeletons, where rule (b) is silent).
  const live = readPages(join(GUIDE, 'docs'));
  const welcome = live[PAGE_FILES.IX];
  assert.deepEqual(run(ix, {pages: live}).errors, []);
  const cells = ['Solo 22, debrief 3', 'Solo 32, debrief 3', 'Solo 18 (LSP 10, MCP 8), debrief 2', 'Solo 28 (permissions 4, hooks 14, skill and command 10), debrief 2', 'Solo 42, debrief 3'];
  cells.forEach((cell, index) => {
    const id = `IX-${11 + index}`;
    assert.ok(welcome.includes(cell), `the welcome page carries "${cell}"`);
    const old = run(ix.filter((e) => e.id === id), {pages: {...live, [PAGE_FILES.IX]: welcome.replace(cell, 'Sync 3, solo 19, debrief 3')}});
    assert.match(old.errors.join('\n'), new RegExp(`^coverage: ${id}: docs/index\\.md: the text "`));
  });
  const without = run(ix.filter((e) => e.id === 'IX-16'), {pages: {...live, [PAGE_FILES.IX]: welcome.replace('During the levels Maik and Tom walk around and help.', '')}});
  assert.match(without.errors.join('\n'), /^coverage: IX-16: docs\/index\.md: the text "During the levels Maik and Tom walk around and help\." is not on the page/);
});

// Spec 15 task T3: Part A step 5 of Level 2 holds the debugging options in exactly one details block that is closed by
// default, with the five checks and the diagnosis prompt inside, and the Part B fallback outside it (A9). No other rule
// reads the details element, so this test is its pin.
test('manifest: the Level 2 debugging options are one closed details block in Part A step 5 and the Part B fallback stays outside it (T3)', () => {
  const page = readPages(join(GUIDE, 'docs'))[PAGE_FILES.L2];
  const lines = page.split('\n');
  const at = (pattern) => lines.flatMap((line, index) => (pattern.test(line) ? [index] : []));
  const [partA, step5, partB] = [at(/^## Part A: LSP/), at(/^### 5\. Nothing appears\?$/), at(/^## Part B: MCP/)].map((found) => found[0]);
  const [open, close] = [at(/<details\b/i), at(/<\/details>/i)];
  assert.equal(open.length, 1, 'exactly one details block on the page');
  assert.equal(close.length, 1);
  assert.equal(lines[open[0]], '<details>', 'closed by default: the tag has no open attribute');
  assert.match(lines[open[0] + 1], /^<summary>[^<]+<\/summary>$/, 'the block has a summary title');
  assert.equal(at(/<summary/i).length, 1);
  assert.ok(partA < step5 && step5 < open[0] && open[0] < close[0] && close[0] < partB, 'the block sits in step 5 of Part A, before Part B');
  // The fallback is visible while the block is closed: it follows the closing tag and sits before Part B.
  const fallback = at(/^Still stuck after two minutes: continue with Part B\.$/);
  assert.equal(fallback.length, 1);
  assert.ok(close[0] < fallback[0] && fallback[0] < partB, 'the Part B fallback is outside the details block');
  const body = lines.slice(open[0], close[0] + 1).join('\n');
  // The five checks, each inside the block.
  for (const text of [
    'run `which jdtls` in the same terminal that you start `claude` from',
    'Java 21 or later',
    '`jdtls-lsp` must be on the **Installed** tab and enabled, and the **Errors** tab must not list it',
    'start a new session from a shell where `which jdtls` prints a path',
    'the language server starts the first time Claude edits a `.java` file',
  ]) {
    assert.ok(body.includes(text), `the details block holds: ${text}`);
  }
  // The diagnosis prompt: one fenced block inside the block, plain ASCII, no placeholder to edit, read-only, names the checks.
  assert.equal((body.match(/^```/gm) ?? []).length, 2, 'one fenced block inside the details block');
  const [prompt] = /^```text\n(.+)\n```$/m.exec(body)?.slice(1) ?? [];
  assert.ok(prompt, 'the fenced block is a text block with the prompt on one line');
  assert.doesNotMatch(prompt, /[^\x20-\x7E]/, 'the prompt is plain ASCII');
  assert.doesNotMatch(prompt, /[<>{}[\]]/, 'the prompt has no placeholder to edit');
  for (const text of ['without changing anything', 'Do not edit, install or delete anything', 'which jdtls', 'java -version', '.claude/settings.json', '~/.claude/settings.json', 'jdtls-lsp@claude-plugins-official', 'Errors tab', 'most likely cause']) {
    assert.ok(prompt.includes(text), `the prompt says: ${text}`);
  }
  // The prompt is the L2-29 entry of the manifest, word for word.
  assert.equal(ENTRIES.find((e) => e.id === 'L2-29').snippet, prompt);
});

test('manifest: the Level 2 tip is one "Make it yours" tip of at most 60 words outside the box, the steps and the checklist (T3, U12)', () => {
  const page = readPages(join(GUIDE, 'docs'))[PAGE_FILES.L2];
  const tips = [...page.matchAll(/^:::tip\[Make it yours\]\n\n([^\n]+)\n\n:::$/gm)];
  assert.equal(tips.length, 1, 'exactly one tip titled Make it yours');
  assert.ok(tips[0][1].split(/\s+/).length <= 60, 'at most 60 words');
  assert.equal(ENTRIES.find((e) => e.id === 'L2-33').snippet, tips[0][1], 'L2-33 pins the tip word for word');
  const boxEnd = page.indexOf('\n:::\n', page.indexOf(':::note[At a glance]'));
  assert.ok(boxEnd !== -1 && tips[0].index > boxEnd, 'not in the at a glance box');
  assert.ok(tips[0].index < page.indexOf('## Level cleared when'), 'before the checklist');
  const checklist = page.slice(page.indexOf('## Level cleared when'));
  assert.doesNotMatch(checklist, /Make it yours|fifth tool/, 'the checklist does not refer to the tip');
  assert.doesNotMatch(page.replace(tips[0][0], ''), /Make it yours|fifth tool/, 'no step or checkpoint refers to the tip');
});

// Spec 15 task T3 (decision D-S5): the language server needs Java 21 or later, in the plan and on every page, and the
// plan holds no demo step for it any more.
test('manifest: no page and no plan line names Java 10 to 19 for jdtls, and the plan has no demo step (T3)', () => {
  const plan = readFileSync(join(PACK, SOURCE_FILES.plan), 'utf8');
  assert.ok(plan.includes('it needs JDK 21 or later, participants have 25'), 'the plan says JDK 21 for jdtls');
  for (const [name, text] of [...Object.entries(readPages(join(GUIDE, 'docs'))), [SOURCE_FILES.plan, plan]]) {
    assert.doesNotMatch(text, /\b(?:JDK|Java) 1\d\b/, `${name} names no JDK 10 to 19`);
    assert.doesNotMatch(text, /watch\s+the\s+(?:facilitator\s+)?demo|facilitator\s+demo/i, `${name} has no demo step`);
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
  'plan:48 PATH': 'a word of the prerequisites sentence: SU-06 and SU-07 carry the jdtls binary check, SU-19 to SU-21 the other prerequisites',
  'plan:67 PATH': 'the wrapper-script half of the Other Linux row, which SU-12 carries; the word PATH alone says nothing to check',
  'plan:70 jdtls-lsp': 'the plugin is installed in Level 2 Part A, which SU-15 carries and L2-04 shows as a command; the plugin id is the sentence subject here',
  'plan:91 main': 'Start branch line of the Level 0 box, checked by the structure rule',
  'plan:91 level-0-start': 'Start branch line of the Level 0 box, checked by the structure rule',
  'plan:91 CLAUDE.md': 'the Level 0 premise that the repo has none, nothing to do; the structure rule checks the Start line of the box',
  'plan:91 .claude/': 'the Level 0 premise that the repo has none, nothing to do; the structure rule checks the Start line of the box',
  'plan:110 double': 'a hint inside the money question, which L0-11 (for money) carries',
  'plan:134 solution/level-1': 'Catch-up and Compare line of the Level 1 box, checked by the structure rule',
  'plan:134 git stash -u && git switch -C level-1 level-0-start': 'Start line of the Level 1 box, checked by the structure rule and by level-start.test.mjs',
  'plan:134 level-0-start': 'Start line of the Level 1 box, checked by the structure rule and by level-start.test.mjs',
  'plan:143 Money': 'the type name inside the CLAUDE.md rule about money, which L1-08 (never double) carries',
  'plan:180 solution/level-1': 'Start line of the Level 2 box, checked by the structure rule',
  'plan:180 solution/level-2': 'Catch-up and Compare line of the Level 2 box, checked by the structure rule',
  'plan:191 solution/level-2': 'the plugin declaration claim of the project scope sentence (follow-up F4); the file itself is L2-08',
  'plan:196 Found N new diagnostic issues ...': 'the same text as L2-12, which stops before the ellipsis',
  'plan:251 solution/level-2': 'Start line of the Level 3 box, checked by the structure rule',
  'plan:251 solution/level-3': 'Catch-up and Compare line of the Level 3 box, checked by the structure rule',
  'plan:255 solution/level-2': 'the plugin declaration claim of the settings.json paragraph (follow-up F4); the file itself is L3-32',
  'plan:255 jdtls-lsp': 'the plugin declaration claim of the settings.json paragraph (follow-up F4); the file itself is L3-32',
  'plan:269 .claude/settings.json': 'the same file as L3-32 and L2-08, which the Permissions part names; the hooks step only says where the block goes',
  'plan:279 ./mvnw -q verify': 'the command of the hook script that spec 10 prints in full, which L3-16 carries',
  'plan:286 --no-verify': 'the escape hatch discussion, carried by L3-14 (escape hatch) and L3-29 (the spec 10 scenario command)',
  'plan:286 -n': 'the short form of --no-verify in the same discussion, carried by L3-14 and the deny rule in L3-02',
  'plan:304 git diff': 'a hint of the stretch subagent sentence, which L3-26 and L3-27 carry',
  'plan:319 solution/level-3': 'the Level 4 box, whose start branches are L4-01 to L4-03 and L4-63 (built from solution/level-3)',
  'plan:326 /plan': 'the Track C row of the table, which repeats Part 2 of the SDD guide (L4-37 carries /plan there)',
  'plan:328 sdd-framework-guide.md': 'an internal path: the deny-list bans it on participant pages',
  'plan:330 level-4-feature-briefs.md': 'an internal path: the deny-list bans it on participant pages',
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
  assert.ok(sweep(ENTRIES.filter((e) => e.id !== 'SU-09')).includes('plan:58 ./mvnw verify'), 'the setup check command is covered by its own entry only');
  assert.ok(sweep(ENTRIES.filter((e) => e.id !== 'L4-65')).includes('sdd:97 claude'), 'the common start line is covered by its own entry only');
  assert.ok(sweep(ENTRIES.filter((e) => e.id !== 'L3-34')).includes('plan:295 name'), 'the skill frontmatter field is covered by its own entry only');
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
