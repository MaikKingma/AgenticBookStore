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
  assert.deepEqual([...withEntries].sort(), ['BO', 'CS', 'IX', 'L0', 'L1', 'L2', 'L3', 'L4', 'SU', 'TS', 'WU']);
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
  // L1 rose from 33 to 41 in task T2 (L1-34 to L1-41: the rule file, its header sentence, the /context mock, two annotations, the tip).
  // L2 rose from 28 to 34 in spec 15 task T3 (L2-29 to L2-34: the diagnosis prompt, the Java 21 check, the new session
  // check, the first Java edit, the Level 2 tip and the debug log sentence of amendment A4); L2-17 was retargeted in place.
  // L3 rose from 34 to 35 in task T6 (L3-35, the Level 3 "Make it yours" tip).
  // SU rose from 21 to 26 and L4 from 65 to 79 in spec 15 task T4 (Node.js qualification and the pinned OpenSpec install
  // on the setup page, the start-branch sentences, the brownfield notes, the 42-minute sentence, the Level 4 tip).
  // BO is the bonus page (spec 16): BO-01 pins the sentence of its marker box (T2), BO-02 to BO-15 the sentences, the table and the
  // fragment that hold its code spans and fenced lines (T4). The BO floor equals the live count.
  const floor = {BO: 15, SU: 26, L0: 23, L1: 41, L2: 34, L3: 35, L4: 79, WU: 22, IX: 16, CS: 52, TS: 16};
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

// Spec 15 task T6: L3-35 pins the Level 3 "Make it yours" tip on the live page and in the Level 3 section of the plan.
test('manifest: L3-35 pins the Level 3 tip, a page or a plan section without its sentence fails', () => {
  const entry = ENTRIES.filter((e) => e.id === 'L3-35');
  assert.equal(entry.length, 1);
  const sentence = 'invent one more guardrail of your own';
  const live = readPages(join(GUIDE, 'docs'));
  const page = live[PAGE_FILES.L3];
  assert.equal(page.split(sentence).length - 1, 1, 'the sentence is on the live page exactly once');
  assert.deepEqual(run(entry, {pages: live}).errors, []);
  const noPage = run(entry, {pages: {...live, [PAGE_FILES.L3]: page.replace(sentence, 'do something else')}});
  assert.match(noPage.errors.join('\n'), new RegExp(`^coverage: L3-35: docs/04-level-3-behavioral-control\\.mdx: the text "${sentence}" is not on the page`));
  const pack = readPack();
  assert.equal(pack.plan.replace(/\s+/g, ' ').split(sentence).length - 1, 1, 'the sentence is in the plan exactly once');
  const noPlan = run(entry, {pages: live, pack: {...pack, plan: pack.plan.replace('invent one more guardrail of your own', 'do something else')}});
  assert.match(noPlan.errors.join('\n'), new RegExp(`^coverage: L3-35: the snippet "${sentence}" is not in workshop/workshop-plan\\.md, section "8\\. Level 3"`));
});

test('manifest: the Level 3 page holds one "Make it yours" tip of at most 60 words, outside the at a glance box and the checklist', () => {
  const page = readPages(join(GUIDE, 'docs'))[PAGE_FILES.L3];
  const tips = [...page.matchAll(/^:::tip\[Make it yours\]\n([\s\S]*?)\n:::$/gm)];
  assert.equal(tips.length, 1, 'one tip titled "Make it yours"');
  const [tip] = tips;
  const words = tip[1].trim().split(/\s+/).length;
  assert.ok(words >= 20 && words <= 60, `the tip has ${words} words, 20 to 60 are allowed`);
  const glance = page.indexOf(':::note[At a glance]');
  const afterGlance = page.indexOf('\n:::\n', glance) + 1;
  assert.ok(glance !== -1 && tip.index > afterGlance, 'the tip is after the at a glance box');
  assert.ok(tip.index < page.indexOf('\n## Level cleared when'), 'the tip is before the "Level cleared when" checklist');
  assert.match(tip[1], /\b(rule|skill|hook|guardrail|command)\b/, 'the tip names a rule, skill, hook, guardrail or command');
  assert.match(tip[1], /\b(permission|deny|hook|skill|command)\b/, 'the tip ties to the Level 3 content (permissions, hooks, skill, command)');
});

test('manifest: the sources and the pages are the five pack files and the twelve pages, and they exist', () => {
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

// Spec 15 task T4 (decision D19, items 6 and 7, owner review of the live guide): the Node.js qualification, the pinned OpenSpec
// install route, what each start branch holds, the brownfield notes, the 42-minute sentence and the Level 4 tip. The tests read the
// live pages (the snapshot pages are skeletons, where rule (b) is silent).
const T4_NODE_SENTENCE =
  'Node.js is not needed for the build: it installs its own copy. You only need Node.js 22.12 or later if you pick Track A (OpenSpec) and install it with npm; the install script needs no Node.js.';
const T4_HOLDS = {
  openspec:
    'The start branch holds an initialised OpenSpec project: `openspec/config.yaml` and one spec in `openspec/specs/ordering/spec.md`, plus the `/opsx` commands in `.claude/commands/opsx/`. It does not hold the `openspec` program.',
  speckit:
    'The start branch holds a Spec Kit project: the `.specify/` folder with a constitution already written in `.specify/memory/constitution.md`, plus the `/speckit-*` skills in `.claude/skills/`.',
  agentos:
    'The start branch holds Agent OS product docs in `agent-os/product/` and standards written for this repo and indexed in `agent-os/standards/index.yml`, plus the commands in `.claude/commands/agent-os/`.',
};
const T4_NOTES = {
  openspec: {
    docs: 'https://github.com/Fission-AI/OpenSpec/blob/v1.16.0/docs/existing-projects.md',
    commands: ['openspec init', 'context:', 'openspec/config.yaml', '/opsx:explore', '/opsx:propose', '/opsx:apply', '/opsx:archive'],
  },
  speckit: {
    docs: 'https://github.com/github/spec-kit/blob/v1.3.1/docs/how-to/existing-projects.md',
    commands: [
      'specify init --here --force --integration claude',
      '/speckit-constitution',
      '/speckit-specify',
      '/speckit-clarify',
      '/speckit-plan',
      '/speckit-tasks',
      '/speckit-implement',
    ],
  },
  agentos: {
    docs: 'https://buildermethods.com/agent-os/workflow',
    commands: ['/discover-standards', '/inject-standards', '/plan-product', '/shape-spec'],
  },
};
const T4_NOTE_INTRO = '**For your own brownfield project**';
const T4_LEVEL4_SENTENCE =
  'A track takes about 35 minutes; the rest of the 42 solo minutes is for choosing a track and a brief at the start and for the final checks at the end.';

const t4Live = () => ({pages: readPages(join(GUIDE, 'docs')), pack: readPack()});
// The text of one track tab of the Level 4 page: up to the next track tab, or the end of the track group (the OpenSpec tab holds
// a nested os group with its own TabItems, so the first closing tag is not the end).
const t4Tab = (level4, value) => {
  const start = level4.indexOf(`<TabItem value="${value}"`);
  assert.notEqual(start, -1, `the Level 4 page has the ${value} tab`);
  const ends = Object.keys(T4_NOTES)
    .filter((other) => other !== value)
    .map((other) => level4.indexOf(`<TabItem value="${other}"`, start))
    .filter((index) => index !== -1);
  const end = ends.length > 0 ? Math.min(...ends) : level4.indexOf('\n</Tabs>', start);
  return level4.slice(start, end);
};
// The brownfield note of a tab: the intro line and the numbered list under it (lines, as written).
const t4Note = (tab) => {
  const lines = tab.split('\n');
  const from = lines.findIndex((line) => line.startsWith(T4_NOTE_INTRO));
  assert.notEqual(from, -1, 'the tab has a brownfield note');
  const note = [lines[from]];
  let seenItem = false;
  for (const line of lines.slice(from + 1)) {
    if (/^\d+\. /.test(line)) {
      seenItem = true;
      note.push(line);
    } else if (line.trim() === '' && !seenItem) note.push(line);
    else if (line.trim() === '' && seenItem) break;
    else break;
  }
  return note;
};

test('manifest: SU-22 pins the Node.js qualification and SU-21 stays; the setup page and the plan carry one identical sentence (A12)', () => {
  const byId = (id) => ENTRIES.filter((e) => e.id === id);
  const setup = (body) => ({pages: {...readPages(), [PAGE_FILES.SU]: `---\ntitle: Setup check\n---\n\n${body}\n`}});
  const base = 'Node.js is not needed: the build installs its own copy.';
  // SU-21 cannot tell the base sentence from the corrected one, which is why SU-22 exists.
  assert.deepEqual(run(byId('SU-21'), setup(base)).errors, []);
  assert.deepEqual(run(byId('SU-21'), setup(T4_NODE_SENTENCE)).errors, []);
  assert.match(run(byId('SU-22'), setup(base)).errors.join('\n'), /^coverage: SU-22: docs\/00-setup-check\.md: the text "You only need Node\.js 22\.12 or later/);
  assert.deepEqual(run(byId('SU-22'), setup(T4_NODE_SENTENCE)).errors, []);
  const {pages, pack} = t4Live();
  for (const [name, text] of [['setup page', pages[PAGE_FILES.SU]], ['workshop plan', pack.plan]]) {
    assert.equal(fold(text).split(fold(T4_NODE_SENTENCE)).length - 1, 1, `the ${name} carries the Node.js sentence once`);
  }
});

test('manifest: the OpenSpec install route is pinned to the version of tech-stack.md on the setup page, the Level 4 page and the plan, the variable sits on sh (U10)', () => {
  const stack = readFileSync(join(PACK, 'product/tech-stack.md'), 'utf8');
  const version = /\| OpenSpec \| (\d+\.\d+\.\d+) \|/.exec(stack)?.[1];
  assert.equal(version, '1.16.0', 'tech-stack.md pins OpenSpec 1.16.0');
  const {pages, pack} = t4Live();
  const route = [
    `curl -fsSL https://openspec.dev/cli/install.sh | OPENSPEC_VERSION=v${version} sh`,
    `$env:OPENSPEC_VERSION = "v${version}"`,
    `npm install -g @fission-ai/openspec@${version}`,
    'openspec --version',
  ];
  for (const [name, text] of [['setup page', pages[PAGE_FILES.SU]], ['Level 4 page', pages[PAGE_FILES.L4]], ['workshop plan', pack.plan]]) {
    for (const line of route) assert.ok(text.split('\n').some((l) => l.trim().startsWith(line)), `the ${name} has the line ${line}`);
  }
  for (const name of [PAGE_FILES.SU, PAGE_FILES.L4]) {
    assert.ok(pages[name].split('\n').some((l) => l.trim() === 'irm https://openspec.dev/cli/install.ps1 | iex'), `${name} has the PowerShell installer line`);
  }
  // The variable on curl pins nothing (it reaches curl, not the script); every pin everywhere names the tech-stack version.
  for (const [name, text] of [...Object.entries(pages), ['workshop plan', pack.plan]]) {
    assert.doesNotMatch(text, /OPENSPEC_VERSION=\S+\s+curl/, `${name}: the variable must sit on sh, not on curl`);
    for (const m of text.matchAll(/OPENSPEC_VERSION(?:=|\s*=\s*)"?v?(\d+\.\d+\.\d+)/g)) assert.equal(m[1], version, `${name}: ${m[0]}`);
  }
});

test('manifest: each track says what its start branch holds, the page and the plan agree, and no page or plan claims an installed framework or discovered standards (U9, A7)', () => {
  const {pages, pack} = t4Live();
  const level4 = pages[PAGE_FILES.L4];
  for (const [value, sentence] of Object.entries(T4_HOLDS)) {
    assert.ok(fold(t4Tab(level4, value)).includes(fold(sentence)), `the ${value} tab says what its branch holds`);
    assert.ok(fold(pack.plan).includes(fold(sentence)), `the plan says what the ${value} branch holds`);
  }
  // The "SDD in Level 4" row of the plan (line 19, A7 and G-4): no longer says that the install is removed from the level.
  const [row] = pack.plan.split('\n').filter((line) => line.startsWith('| SDD in Level 4 |'));
  assert.ok(row?.includes('Only Track A needs one install, the OpenSpec program'), 'the SDD row names the one install of Track A');
  assert.ok(!row.includes('remove the install'), 'the SDD row does not claim the install is removed');
  for (const [name, text] of [['Level 4 page', level4], ['workshop plan', pack.plan]]) {
    assert.doesNotMatch(text, /discovered/i, `${name}: nothing was discovered, the standards were written for this repo`);
    assert.doesNotMatch(fold(text), /framework installed|already installed and configured/i, `${name}: no framework is installed on a start branch`);
  }
});

test('manifest: each track has a short brownfield note with the documented first steps in order, the page and the plan list them alike (U8, D-S6)', () => {
  const {pages, pack} = t4Live();
  const level4 = pages[PAGE_FILES.L4];
  const plan = fold(pack.plan);
  for (const [value, {docs, commands}] of Object.entries(T4_NOTES)) {
    const note = t4Note(t4Tab(level4, value));
    const items = note.filter((line) => /^\d+\. /.test(line));
    const spans = items.flatMap((line) => [...line.matchAll(/`([^`]+)`/g)].map((m) => m[1]));
    assert.deepEqual(spans, commands, `${value}: the commands of the note, in the documented order`);
    assert.ok(note.filter((line) => line.trim() !== '').length <= 8, `${value}: at most 8 lines`);
    assert.ok(note.join(' ').split(/\s+/).filter(Boolean).length <= 120, `${value}: at most 120 words`);
    assert.ok(note[0].includes(`](${docs})`), `${value}: the note cites ${docs}`);
    assert.ok(plan.includes(fold(items.join('\n'))), `${value}: the plan lists the same steps`);
  }
});

test('manifest: the Level 4 page says what the rest of the 42 solo minutes is for, the plan agrees, and the shared-spec sentence is no part of Brief C (FF-T1-3)', () => {
  const {pages, pack} = t4Live();
  const level4 = pages[PAGE_FILES.L4];
  assert.equal(fold(level4).split(fold(T4_LEVEL4_SENTENCE)).length - 1, 1, 'the page carries the 42-minute sentence once');
  assert.ok(fold(pack.plan).includes(fold(T4_LEVEL4_SENTENCE)), 'the plan carries it');
  assert.equal((level4.match(/About 35 minutes/g) ?? []).length, 3, 'the three track times stay');
  const paragraph = level4.split('\n').find((line) => line.includes('hold the same idea in three shapes'));
  assert.ok(paragraph?.startsWith('Whichever brief you pick, once your spec artefact is committed'), 'the sentence names every brief, so it does not read as part of Brief C');
});

test('manifest: the Level 4 page holds one Make it yours tip of at most 60 words, outside the box and the checklist, mirrored in the plan (T6 1 and 3)', () => {
  const {pages, pack} = t4Live();
  const level4 = pages[PAGE_FILES.L4];
  const tips = [...level4.matchAll(/^:::tip\[Make it yours\]\n\n([\s\S]*?)\n\n:::$/gm)];
  assert.equal(tips.length, 1, 'one Make it yours tip');
  const [{0: whole, 1: body, index}] = tips;
  assert.ok(body.split(/\s+/).filter(Boolean).length <= 60, 'at most 60 words');
  assert.doesNotMatch(body, /^\s*(?:\d+\.|-)\s/m, 'a tip is no step and no checklist item');
  assert.ok(index > level4.indexOf('## Tracks') && index + whole.length < level4.indexOf('## Level cleared when'), 'inside the page, before the checklist');
  assert.ok(index > level4.indexOf('\n:::\n'), 'after the at a glance box');
  assert.ok(fold(pack.plan).includes(fold(`**Make it yours:** ${body}`)), 'the plan mirrors the tip');
  assert.doesNotMatch(level4.replace(whole, ''), /Make it yours/, 'nothing refers to the tip');
});

// Amendment A3 of spec 15: the wrap-up page must not say that Level 4 needed no install (Track A needs the OpenSpec program).
// The sentence is on no pack line (the plan does not carry it), so no coverage entry can anchor it: this test pins it.
test('manifest: the wrap-up page says that only Track A needed an install in Level 4 (A3)', () => {
  const wrapUp = readPages(join(GUIDE, 'docs'))[PAGE_FILES.WU];
  assert.ok(wrapUp.includes('Level 4 used prepared branches, so only Track A needed one install, the OpenSpec program.'));
  assert.doesNotMatch(wrapUp, /did not install anything/);
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
      pages: [['3. Setup check', ['SU']], ['4. Level 0', ['L0']], ['5. Level 1', ['L1']], ['7. Level 2', ['L2']], ['8. Level 3', ['L3']], ['9. Level 4', ['L4', 'CS']], ['Bonus level', ['BO']]],
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
  'plan:68 PATH': 'the wrapper-script half of the Other Linux row, which SU-12 carries; the word PATH alone says nothing to check',
  'plan:71 jdtls-lsp': 'the plugin is installed in Level 2 Part A, which SU-15 carries and L2-04 shows as a command; the plugin id is the sentence subject here',
  'plan:118 main': 'Start branch line of the Level 0 box, checked by the structure rule',
  'plan:118 level-0-start': 'Start branch line of the Level 0 box, checked by the structure rule',
  'plan:118 CLAUDE.md': 'the Level 0 premise that the repo has none, nothing to do; the structure rule checks the Start line of the box',
  'plan:118 .claude/': 'the Level 0 premise that the repo has none, nothing to do; the structure rule checks the Start line of the box',
  'plan:137 double': 'a hint inside the money question, which L0-11 (for money) carries',
  'plan:161 solution/level-1': 'Catch-up and Compare line of the Level 1 box, checked by the structure rule',
  'plan:161 git stash -u && git switch -C level-1 level-0-start': 'Start line of the Level 1 box, checked by the structure rule and by level-start.test.mjs',
  'plan:161 level-0-start': 'Start line of the Level 1 box, checked by the structure rule and by level-start.test.mjs',
  'plan:170 Money': 'the type name inside the CLAUDE.md rule about money, which L1-08 (never double) carries',
  'plan:252 solution/level-1': 'Start line of the Level 2 box, checked by the structure rule',
  'plan:252 solution/level-2': 'Catch-up and Compare line of the Level 2 box, checked by the structure rule',
  'plan:263 solution/level-2': 'the plugin declaration claim of the project scope sentence (follow-up F4); the file itself is L2-08',
  'plan:268 Found N new diagnostic issues ...': 'the same text as L2-12, which stops before the ellipsis',
  'plan:323 solution/level-2': 'Start line of the Level 3 box, checked by the structure rule',
  'plan:323 solution/level-3': 'Catch-up and Compare line of the Level 3 box, checked by the structure rule',
  'plan:327 solution/level-2': 'the plugin declaration claim of the settings.json paragraph (follow-up F4); the file itself is L3-32',
  'plan:327 jdtls-lsp': 'the plugin declaration claim of the settings.json paragraph (follow-up F4); the file itself is L3-32',
  'plan:341 .claude/settings.json': 'the same file as L3-32 and L2-08, which the Permissions part names; the hooks step only says where the block goes',
  'plan:351 ./mvnw -q verify': 'the command of the hook script that spec 10 prints in full, which L3-16 carries',
  'plan:358 --no-verify': 'the escape hatch discussion, carried by L3-14 (escape hatch) and L3-29 (the spec 10 scenario command)',
  'plan:358 -n': 'the short form of --no-verify in the same discussion, carried by L3-14 and the deny rule in L3-02',
  'plan:376 git diff': 'a hint of the stretch subagent sentence, which L3-26 and L3-27 carry',
  'plan:395 solution/level-3': 'the Level 4 box, whose start branches are L4-01 to L4-03 and L4-63 (built from solution/level-3)',
  // Spec 15 task T4: the key `plan:375 /plan` (the Track C row of the table) is gone. The literal `/plan` is now covered by L4-79,
  // whose snippet is the whole Track C cell of that row on the page and in the plan (R2 of the T4 QA round; deleting the cell
  // from the plan or the page fails L4-79), and by substring in L4-72 (the Agent OS brownfield note names `/plan-product`).
  // The step itself stays pinned by L4-37 (`/plan`, SDD Part 2).
  'plan:404 sdd-framework-guide.md': 'an internal path: the deny-list bans it on participant pages',
  'plan:437 level-4-feature-briefs.md': 'an internal path: the deny-list bans it on participant pages',
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
  assert.ok(sweep(ENTRIES.filter((e) => e.id !== 'SU-09')).includes('plan:59 ./mvnw verify'), 'the setup check command is covered by its own entry only');
  assert.ok(sweep(ENTRIES.filter((e) => e.id !== 'L4-65')).includes('sdd:97 claude'), 'the common start line is covered by its own entry only');
  assert.ok(sweep(ENTRIES.filter((e) => e.id !== 'L3-34')).includes('plan:367 name'), 'the skill frontmatter field is covered by its own entry only');
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

// Spec 16 (T2): BO-01 pins the sentence of the marker box of the bonus page. The page is the optional page between Level 4
// and the Wrap-up; its box is pinned by the structure rule (first element, the Wrap-up link inside it), the sentence here.
const BO_SENTENCE = 'This bonus level is optional: it is for you if you finish Level 4 early or want to carry on at home.';

test('manifest: BO-01 pins the marker sentence on the bonus page and in the plan section "Bonus level" (probes: page loses it, plan rewords it)', () => {
  // T4 retarget: the page has 15 entries now (BO-01 to BO-15); this test is about BO-01, so it reads that one entry.
  const bo = ENTRIES.filter((e) => e.page === 'BO' && e.id === 'BO-01');
  assert.deepEqual(
    bo.map((e) => [e.id, e.source, e.section, e.kind, e.snippet, e.pageSnippet]),
    [['BO-01', 'plan', 'Bonus level', 'text', BO_SENTENCE, undefined]],
  );
  assert.equal(PAGE_FILES.BO, 'docs/06-bonus-level-orchestration.mdx');
  const pack = readPack();
  const pages = readPages(join(GUIDE, 'docs'));
  assert.deepEqual(run(bo, {pages, pack}).errors, [], 'the live page and the plan carry the sentence');
  assert.equal(fold(pack.plan).split(fold(BO_SENTENCE)).length - 1, 1, 'the plan carries the sentence once');
  assert.equal(fold(pages[PAGE_FILES.BO]).split(fold(BO_SENTENCE)).length - 1, 1, 'the page carries the sentence once');
  const stub = (body) => ({pages: {...pages, [PAGE_FILES.BO]: `---\ntitle: Bonus\n---\n\n${body}\n`}, pack});
  // the page without the sentence, or with it reworded: the text is not on the page
  for (const body of ['Nothing here.', BO_SENTENCE.replace('optional', 'voluntary')]) {
    assert.match(
      run(bo, stub(body)).errors.join('\n'),
      /^coverage: BO-01: docs\/06-bonus-level-orchestration\.mdx: the text "This bonus level is optional: it is for you if you finish Level 4 early or want to carr\.\.\." is not on the page/,
    );
  }
  assert.deepEqual(run(bo, stub(BO_SENTENCE)).errors, []);
  // the plan reworded: the snippet is no longer anchored in the section
  const noPlan = run(bo, {pages, pack: {...pack, plan: pack.plan.replace('it is for you if you finish', 'you may do it if you finish')}});
  assert.match(
    noPlan.errors.join('\n'),
    /^coverage: BO-01: the snippet "This bonus level is optional: it is for you if you finish Level 4 early or want to carr\.\.\." is not in workshop\/workshop-plan\.md, section "Bonus level"/,
  );
  assert.equal(noPlan.anchored, 0);
  // the sentence in another plan section does not anchor it (the entry is searched in its own section only)
  const elsewhere = pack.plan.replace(BO_SENTENCE, 'Placeholder.').replace('## 10. Wrap-up (15 min)', `## 10. Wrap-up (15 min)\n\n${BO_SENTENCE}`);
  assert.match(run(bo, {pages, pack: {...pack, plan: elsewhere}}).errors.join('\n'), /BO-01: the snippet .* is not in workshop\/workshop-plan\.md, section "Bonus level"/);
});

// Spec 16 (T4): the content of the bonus page. BO-02 to BO-15 hold every code span and fenced line of the plan section "Bonus level"
// (the literal sweep above reads that range for the page BO), and the live page carries each snippet word for word. The count is typed
// here a second time, so that an entry dropped from the manifest (and from the floor above) cannot go unnoticed.
const BO_COUNT = 15;
const BO_IDS = Array.from({length: BO_COUNT}, (_, i) => `BO-${String(i + 1).padStart(2, '0')}`);

test('manifest: the bonus page has exactly BO-01 to BO-15, and dropping any one of them falls below the floor of the page', () => {
  const bo = ENTRIES.filter((e) => e.page === 'BO');
  assert.deepEqual(bo.map((e) => e.id), BO_IDS);
  for (const e of bo) assert.deepEqual([e.source, e.section], ['plan', 'Bonus level'], e.id);
  for (const id of BO_IDS) assert.equal(ENTRIES.filter((e) => e.page === 'BO' && e.id !== id).length, BO_COUNT - 1, `without ${id} the page has ${BO_COUNT - 1} entries, below the floor`);
});

test('manifest: the live bonus page and the plan section carry every BO snippet, a snippet missing on the page fails by its id (BO-02 to BO-15)', () => {
  const bo = ENTRIES.filter((e) => e.page === 'BO');
  const pages = readPages(join(GUIDE, 'docs'));
  const pack = readPack();
  const live = run(bo, {pages, pack});
  assert.deepEqual(live.errors, []);
  assert.equal(live.anchored, BO_COUNT);
  assert.deepEqual(live.pending, {}, 'the bonus page is no skeleton, so no entry is pending');
  for (const entry of bo.slice(1)) {
    const page = pages[PAGE_FILES.BO];
    const folded = fold(page);
    assert.equal(folded.split(fold(entry.snippet)).length - 1, 1, `${entry.id}: the page carries the snippet exactly once`);
    assert.equal(fold(pack.plan).split(fold(entry.snippet)).length - 1, 1, `${entry.id}: the plan carries the snippet exactly once`);
    // the page without the snippet: the entry fails, and only that entry
    const lines = page.split('\n');
    const without = entry.snippet.includes('\n') ? lines.filter((line) => !entry.snippet.split('\n').includes(line)) : lines.map((line) => line.replace(entry.snippet, 'Nothing here.'));
    const errors = run(bo, {pages: {...pages, [PAGE_FILES.BO]: without.join('\n')}, pack}).errors;
    assert.ok(errors.some((error) => error.startsWith(`coverage: ${entry.id}: ${PAGE_FILES.BO}: the `)), `${entry.id}: ${errors.join('\n')}`);
  }
});

test('manifest: the literal sweep reads the plan section "Bonus level" for the page BO, a bonus literal without its entry fails, and no exclusion is added for it', () => {
  assert.deepEqual(SWEEPS[0][1].pages.at(-1), ['Bonus level', ['BO']]);
  assert.deepEqual(sweep(ENTRIES).filter((key) => /^plan:(4[5-9]\d|5\d\d) /.test(key)), [], 'no sweep exclusion lies in the bonus section (lines 453 to the Wrap-up)');
  const witnesses = [
    ['BO-10', 'claude --version'],
    ['BO-11', '/config'],
    ['BO-13', '/<name>'],
    ['BO-07', '.claude/agents/architecture-reviewer.md'],
    ['BO-07', 'start/level-4-<track>'],
    ['BO-06', 'name: <name>'],
    ['BO-06', 'description: <when to use it>'],
    ['BO-04', 'Read'],
    ['BO-09', '/ship'],
  ];
  for (const [id, literal] of witnesses) {
    const left = sweep(ENTRIES.filter((e) => e.id !== id));
    assert.ok(left.some((key) => key.endsWith(` ${literal}`)), `without ${id} the sweep finds ${literal} uncovered`);
  }
  // the right snippet on the wrong page or attributed to the wrong section does not cover a bonus literal
  assert.ok(sweep(ENTRIES.map((e) => (e.id === 'BO-10' ? {...e, page: 'L4'} : e))).some((key) => key.endsWith(' claude --version')));
  assert.ok(sweep(ENTRIES.map((e) => (e.id === 'BO-10' ? {...e, section: '9. Level 4'} : e))).some((key) => key.endsWith(' claude --version')));
});

test('manifest: the literals of the bonus section shorter than 4 characters are the fence lines --- of the fragment, pinned by BO-06 in sentence context', () => {
  const plan = readPack().plan;
  const found = literalsOf(plan, {from: '## Bonus level', to: '## 10. Wrap-up'}).map((u) => u.literal);
  assert.ok(found.length >= 20, `the bonus section holds ${found.length} literals`);
  assert.deepEqual([...new Set(found.filter((literal) => literal.length < 4))], ['---']);
  const [fragment] = ENTRIES.filter((e) => e.id === 'BO-06');
  assert.equal(fragment.kind, 'command', 'the fragment must stand in a fenced block of the page');
  assert.ok(fold(fragment.snippet).startsWith('--- name: <name>') && fold(fragment.snippet).endsWith('model: <model> ---'), 'the --- lines are the first and the last line of the fragment');
  // the page must hold the fragment as one fenced block: without its fence the command entry fails
  const pages = readPages(join(GUIDE, 'docs'));
  const asProse = pages[PAGE_FILES.BO].replace('```yaml\n---\nname: <name>', 'Fragment:\n\n---\nname: <name>').replace('model: <model>\n---\n```', 'model: <model>\n---');
  assert.match(run([fragment], {pages: {...pages, [PAGE_FILES.BO]: asProse}, pack: readPack()}).errors.join('\n'), /^coverage: BO-06: docs\/06-bonus-level-orchestration\.mdx: the command ".*" is not in a fenced block of the page/);
});
