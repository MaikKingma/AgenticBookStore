import {test} from 'node:test';
import assert from 'node:assert/strict';
import {structureErrors, SKELETON_MARKER} from './rules/structure.mjs';
import {FIXTURE_LEVELS, fixturePlan, fixtureSdd, goodDocs, levelPage} from './lib/structure-fixtures.mjs';
import {TROUBLESHOOTING_ANCHORS} from './lib/structure-fixtures.mjs';
import {replaceOnce} from './lib/test-fixtures.mjs';

// Runs the pure rule on the good docs plus overrides (a null removes a file).
function run(overrides = {}, input = {}) {
  const files = {...goodDocs(), ...overrides};
  for (const key of Object.keys(files)) if (files[key] === null) delete files[key];
  return structureErrors({files, nulFiles: [], planText: fixturePlan(), sddText: fixtureSdd(), ...input});
}

const L = (n) => FIXTURE_LEVELS[n].file;
const page = (n, options) => levelPage(n, options);

// Asserts that exactly the errors matching the patterns are reported, one per pattern.
function assertErrors(errors, patterns) {
  assert.equal(errors.length, patterns.length, errors.join('\n'));
  patterns.forEach((pattern) => assert.ok(errors.some((e) => pattern.test(e)), `no error matches ${pattern}:\n${errors.join('\n')}`));
}

test('structure: the good docs have no errors and the summary counts the pages', () => {
  const {errors, summary} = run();
  assert.deepEqual(errors, []);
  assert.match(summary, /11 pages/);
});

test('structure: a skeleton level page and a finished one both pass the grammar', () => {
  assert.deepEqual(run({[L(1)]: page(1, {skeleton: false})}).errors, []);
});

// Checklist

test('structure: swapping two checklist items fails', () => {
  const [a, b] = FIXTURE_LEVELS[3].items.slice(0, 2);
  let text = replaceOnce(page(3), `- [ ] ${a}`, '- [ ] SWAP');
  text = replaceOnce(text, `- [ ] ${b}`, `- [ ] ${a}`);
  text = replaceOnce(text, '- [ ] SWAP', `- [ ] ${b}`);
  assertErrors(run({[L(3)]: text}).errors, [/item 1 /, /item 2 /]);
});

test('structure: rewording one word of an item fails and names the page', () => {
  const text = replaceOnce(page(1), 'path-scoped rules', 'path-limited rules');
  const {errors} = run({[L(1)]: text});
  assertErrors(errors, [/item 2 /]);
  assert.match(errors[0], /^structure: docs\/02-level-1-context-engineering\.mdx: /);
});

test('structure: a word on the continuation line of a wrapped plan bullet is compared', () => {
  const text = replaceOnce(page(0), 'or the wrong file', 'or the right file');
  assertErrors(run({[L(0)]: text}).errors, [/item 1 /]);
});

test('structure: dropping the backticks of an inline code item fails', () => {
  const text = replaceOnce(page(1), '- [ ] `CLAUDE.md` has', '- [ ] CLAUDE.md has');
  assertErrors(run({[L(1)]: text}).errors, [/item 1 /]);
});

test('structure: an item may wrap over indented lines, whitespace is normalised', () => {
  const text = replaceOnce(page(1), '- [ ] `CLAUDE.md` has at most', '- [ ] `CLAUDE.md`   has\n  at most');
  assert.deepEqual(run({[L(1)]: text}).errors, []);
});

test('structure: a checked item and a plain bullet fail', () => {
  const checked = replaceOnce(page(2), '- [ ] `/mcp`', '- [x] `/mcp`');
  assertErrors(run({[L(2)]: checked}).errors, [/item 2 .*checked/]);
  const upper = replaceOnce(page(2), '- [ ] `/mcp`', '- [X] `/mcp`');
  assertErrors(run({[L(2)]: upper}).errors, [/item 2 .*checked/]);
  const plain = replaceOnce(page(2), '- [ ] `/mcp`', '- `/mcp`');
  assertErrors(run({[L(2)]: plain}).errors, [/item 2 .*task list item/]);
});

test('structure: an extra item, a missing last item and a duplicated item fail on the count', () => {
  const extra = `${page(1)}- [ ] One more.\n`;
  assertErrors(run({[L(1)]: extra}).errors, [/4 items.*plan has 3/]);
  const last = FIXTURE_LEVELS[1].items[2];
  const dropped = replaceOnce(page(1), `- [ ] ${last}\n`, '');
  assertErrors(run({[L(1)]: dropped}).errors, [/2 items.*plan has 3/]);
  const first = FIXTURE_LEVELS[1].items[0];
  const doubled = replaceOnce(page(1), `- [ ] ${first}\n`, `- [ ] ${first}\n- [ ] ${first}\n`);
  assertErrors(run({[L(1)]: doubled}).errors, [/4 items.*plan has 3/, /item 2 /, /item 3 /]);
});

test('structure: nothing may follow the checklist', () => {
  const after = (extra) => `${page(2)}\n${extra}\n`;
  const follow = /follows the checklist/;
  assertErrors(run({[L(2)]: after('A closing paragraph.')}).errors, [follow]);
  assertErrors(run({[L(2)]: after('## Debrief')}).errors, [follow]);
  assertErrors(run({[L(2)]: after(':::tip\nx\n:::')}).errors, [follow]);
  assertErrors(run({[L(2)]: after('{/* a comment */}')}).errors, [follow]);
  assertErrors(run({[L(2)]: after('- [ ] A second list.')}).errors, [follow]);
});

test('structure: a paragraph between the heading and the list fails, trailing blank lines pass', () => {
  const text = replaceOnce(page(2), '## Level cleared when\n\n', '## Level cleared when\n\nRead this first.\n\n');
  assertErrors(run({[L(2)]: text}).errors, [/must be followed by a task list/]);
  assert.deepEqual(run({[L(2)]: `${page(2)}\n\n\n`}).errors, []);
});

test('structure: a missing or repeated "Level cleared when" heading fails', () => {
  const none = replaceOnce(page(0), '## Level cleared when', '## Cleared');
  assertErrors(run({[L(0)]: none}).errors, [/no "## Level cleared when" heading/]);
  const twice = `${page(0)}\n## Level cleared when\n`;
  assertErrors(run({[L(0)]: twice}).errors, [/2 "## Level cleared when" headings/]);
});

test('structure: the marker never exempts the checklist', () => {
  const text = replaceOnce(page(3), '`git commit --no-verify` is denied.', 'changed');
  assert.ok(text.includes(SKELETON_MARKER));
  assertErrors(run({[L(3)]: text}).errors, [/item 2 /]);
});

// At a glance box

test('structure: the Goal matches the plan, first letter case-insensitive, whole sentence', () => {
  const goal = FIXTURE_LEVELS[2].goal;
  assert.deepEqual(run({[L(2)]: replaceOnce(page(2), `**Goal:** ${goal.charAt(0).toUpperCase()}${goal.slice(1)}`, `**Goal:** ${goal}`)}).errors, []);
  const noPeriod = replaceOnce(page(2), 'instead of guessing.', 'instead of guessing');
  assertErrors(run({[L(2)]: noPeriod}).errors, [/Goal/]);
  const firstLine = replaceOnce(page(2), ', and real external data through our own MCP server, instead of guessing.', '.');
  assertErrors(run({[L(2)]: firstLine}).errors, [/Goal/]);
});

test('structure: a Goal that wraps over lines is joined', () => {
  const text = replaceOnce(page(2), ' through a language server,', '\nthrough a language server,');
  assert.deepEqual(run({[L(2)]: text}).errors, []);
});

test('structure: the Time equals the plan minutes of the heading, not a part', () => {
  assertErrors(run({[L(2)]: replaceOnce(page(2), '**Time:** 20 minutes', '**Time:** 8 minutes')}).errors, [/Time is 8 minutes.*20/]);
  assertErrors(run({[L(0)]: replaceOnce(page(0), '**Time:** 25 minutes', '**Time:** 30 minutes')}).errors, [/Time is 30 minutes.*25/]);
  assertErrors(run({[L(0)]: replaceOnce(page(0), '**Time:** 25 minutes', '**Time:** 25')}).errors, [/Time/]);
});

test('structure: the Start branch equals the header table value', () => {
  assertErrors(run({[L(1)]: replaceOnce(page(1), '**Start branch:** `level-0-start`', '**Start branch:** `develop`')}).errors, [/Start branch/]);
  assertErrors(run({[L(0)]: replaceOnce(page(0), ' (tag `level-0-start`)', '')}).errors, [/Start branch/]);
  assertErrors(run({[L(2)]: replaceOnce(page(2), 'your Level 1 result, or `solution/level-1`', 'your Level 1 result')}).errors, [/Start branch/]);
});

test("structure: the catch-up block is exactly the level's git stash -u && git switch command", () => {
  const wrongBranch = replaceOnce(page(2), 'git switch solution/level-1', 'git switch solution/level-2');
  assertErrors(run({[L(2)]: wrongBranch}).errors, [/Catch-up/]);
  const extraCommand = replaceOnce(page(1), 'git switch -C level-1 level-0-start\n', 'git switch -C level-1 level-0-start\ngit pull\n');
  assertErrors(run({[L(1)]: extraCommand}).errors, [/Catch-up/]);
  const noStash = replaceOnce(page(1), 'git stash -u && git switch -C level-1 level-0-start', 'git switch -C level-1 level-0-start');
  assertErrors(run({[L(1)]: noStash}).errors, [/Catch-up/]);
  const noFence = replaceOnce(page(1), '```bash\ngit stash -u && git switch -C level-1 level-0-start\n```', '`git stash -u && git switch -C level-1 level-0-start`');
  assertErrors(run({[L(1)]: noFence}).errors, [/Catch-up/]);
  const wrongLang = replaceOnce(page(1), '```bash', '```text');
  assertErrors(run({[L(1)]: wrongLang}).errors, [/Catch-up/]);
});

test('structure: Levels 0 and 1 catch up at the tag on their own branch, not on main', () => {
  const mainLevel0 = replaceOnce(page(0), 'git switch -C level-0 level-0-start', 'git switch main');
  assertErrors(run({[L(0)]: mainLevel0}).errors, [/Catch-up/]);
  const mainLevel1 = replaceOnce(page(1), 'git switch -C level-1 level-0-start', 'git switch main');
  assertErrors(run({[L(1)]: mainLevel1}).errors, [/Catch-up/]);
  const lowerC = replaceOnce(page(1), 'git switch -C level-1', 'git switch -c level-1');
  assertErrors(run({[L(1)]: lowerC}).errors, [/Catch-up/]);
  const swapped = replaceOnce(page(1), 'git switch -C level-1', 'git switch -C level-0');
  assertErrors(run({[L(1)]: swapped}).errors, [/Catch-up/]);
  assertErrors(run({[L(1)]: replaceOnce(page(1), '**Start branch:** `level-0-start`', '**Start branch:** `main`')}).errors, [/Start branch/]);
});

test('structure: Compare appears on Levels 1 to 3 only, with the right branch', () => {
  assertErrors(run({[L(0)]: replaceOnce(page(0), '\n\n:::\n', '\n\n**Compare:** `solution/level-1`\n\n:::\n')}).errors, [/Compare/]);
  assertErrors(run({[L(4)]: replaceOnce(page(4), '\n\n:::\n', '\n\n**Compare:** `solution/level-4`\n\n:::\n')}).errors, [/Compare/]);
  assertErrors(run({[L(3)]: replaceOnce(page(3), '\n\n**Compare:** `solution/level-3`', '')}).errors, [/Compare/]);
  assertErrors(run({[L(3)]: replaceOnce(page(3), '`solution/level-3`', '`solution/level-2`')}).errors, [/Compare/]);
});

test('structure: the box lines must come in order, each once, with no stray content', () => {
  const swapped = replaceOnce(
    page(1),
    '**Time:** 35 minutes\n\n**Compare:** `solution/level-1`',
    '**Compare:** `solution/level-1`\n\n**Time:** 35 minutes',
  );
  assertErrors(run({[L(1)]: swapped}).errors, [/out of order/]);
  const twice = replaceOnce(page(1), '**Time:** 35 minutes', '**Time:** 35 minutes\n\n**Time:** 35 minutes');
  assertErrors(run({[L(1)]: twice}).errors, [/Time.* twice/]);
  const stray = replaceOnce(page(1), '**Goal:**', 'A stray line.\n\n**Goal:**');
  assertErrors(run({[L(1)]: stray}).errors, [/unexpected/]);
  const noGoal = replaceOnce(page(1), /\*\*Goal:\*\*[^\n]*\n\n/.exec(page(1))[0], '');
  assertErrors(run({[L(1)]: noGoal}).errors, [/missing the Goal line/]);
});

test('structure: a page without the at a glance box fails', () => {
  const text = replaceOnce(page(1), ':::note[At a glance]', ':::note[Summary]');
  assert.ok(run({[L(1)]: text}).errors.some((e) => /At a glance/.test(e)));
});

// LevelProgress

test('structure: LevelProgress must be the first element after the imports, with the right level', () => {
  const below = page(1)
    .replace(/<LevelProgress current=\{1\} \/>\n\n/, '')
    .replace('\n:::\n', '\n:::\n\n<LevelProgress current={1} />\n');
  assertErrors(run({[L(1)]: below}).errors, [/first element/]);
  const inside = page(1)
    .replace(/<LevelProgress current=\{1\} \/>\n\n/, '')
    .replace(':::note[At a glance]\n', ':::note[At a glance]\n<LevelProgress current={1} />\n');
  assert.ok(run({[L(1)]: inside}).errors.some((e) => /first element/.test(e)));
  const paragraph = replaceOnce(page(1), '<LevelProgress current={1} />', 'Welcome.\n\n<LevelProgress current={1} />');
  assertErrors(run({[L(1)]: paragraph}).errors, [/first element/]);
  assertErrors(run({[L(3)]: replaceOnce(page(3), 'current={3}', 'current={2}')}).errors, [/current=\{2\}.*Level 3/]);
  assertErrors(run({[L(3)]: replaceOnce(page(3), 'current={3}', 'current={7}')}).errors, [/current=\{7\}/]);
});

test('structure: blank lines and several imports before LevelProgress are fine', () => {
  const imports = "import LevelProgress from '@site/src/components/LevelProgress';\nimport Tabs from '@theme/Tabs';\nimport TabItem from '@theme/TabItem';";
  const text = replaceOnce(page(2), "import LevelProgress from '@site/src/components/LevelProgress';", `\n${imports}\n`);
  assert.deepEqual(run({[L(2)]: text}).errors, []);
});

test('structure: the LevelProgress import is required and the component appears once', () => {
  const noImport = replaceOnce(page(1), "import LevelProgress from '@site/src/components/LevelProgress';\n\n", '');
  assertErrors(run({[L(1)]: noImport}).errors, [/import/]);
  const twice = `${page(1).replace(/\n## Level cleared when/, '\n<LevelProgress current={1} />\n\n## Level cleared when')}`;
  assertErrors(run({[L(1)]: twice}).errors, [/2 times/]);
});

// Title, H1, frontmatter

test('structure: a level page title must be the exact spec name, quoted when it holds a colon', () => {
  assertErrors(run({[L(2)]: replaceOnce(page(2), 'Level 2: Give your agent eyes (LSP + MCP)', 'Level 2: Give your agent eyes')}).errors, [/title/]);
  assertErrors(run({[L(4)]: replaceOnce(page(4), 'title: "Level 4', 'title: Level 4').replace('development"', 'development')}).errors, [/quoted/]);
  const noFrontmatter = page(0).replace(/^---\n[\s\S]*?\n---\n/, '');
  assert.ok(run({[L(0)]: noFrontmatter}).errors.some((e) => /frontmatter/.test(e)));
});

test('structure: a body H1 fails, a comment line inside a fenced block does not', () => {
  assertErrors(run({[L(1)]: replaceOnce(page(1), SKELETON_MARKER, `${SKELETON_MARKER}\n\n# Context`)}).errors, [/H1/]);
  const fenced = replaceOnce(page(1), SKELETON_MARKER, `${SKELETON_MARKER}\n\n\`\`\`bash\n# a comment\n~/x\n\`\`\`\n\n~~~\n# also a comment\n~~~`);
  assert.deepEqual(run({[L(1)]: fenced}).errors, []);
  assertErrors(run({'docs/06-wrap-up.md': '---\ntitle: Wrap-up\nsidebar_position: 8\n---\n\n# Heading\n'}).errors, [/H1/]);
});

// Admonitions

test('structure: space-form admonitions fail on every page', () => {
  const level = replaceOnce(page(1), SKELETON_MARKER, `${SKELETON_MARKER}\n\n:::info Heads up\nbody\n:::`);
  assertErrors(run({[L(1)]: level}).errors, [/space form/]);
  const wrapUp = '---\ntitle: Wrap-up\nsidebar_position: 8\n---\n\n:::tip Checkpoint\nbody\n:::\n';
  assertErrors(run({'docs/06-wrap-up.md': wrapUp}).errors, [/space form/]);
});

// Spec 15 (decision D19): the workshop has no sync moments. The old rule demanded exactly one on a finished
// level page; it is now a ban on any admonition whose title contains "sync moment", on every page.
const WRAP_UP_HEAD = '---\ntitle: Wrap-up\nsidebar_position: 8\n---\n\n';
const INDEX_HEAD = '---\ntitle: Welcome\nslug: /\nsidebar_position: 1\n---\n\n';
const SYNC_BAN = /an admonition titled ".*" is a sync moment, the workshop has none/;

// The page text with an admonition added: level pages after the marker (or the finished page's body), others at the end.
function withBox(file, opener, body = 'Look up, Maik demos this now.', {skeleton = true} = {}) {
  const box = `${opener}\n${body}\n:::`;
  if (file === 'docs/06-wrap-up.md') return `${WRAP_UP_HEAD}${box}\n`;
  if (file === 'docs/index.md') return `${INDEX_HEAD}${box}\n`;
  const n = FIXTURE_LEVELS.findIndex((level) => level.file === file);
  const text = page(n, {skeleton});
  return skeleton ? replaceOnce(text, SKELETON_MARKER, `${SKELETON_MARKER}\n\n${box}`) : replaceOnce(text, '## Level cleared when', `${box}\n\n## Level cleared when`);
}

test('structure: a sync moment admonition is banned on every page, skeleton or finished', () => {
  const targets = [
    ['a skeleton level page', L(1), {}],
    ['a finished level page', L(1), {skeleton: false}],
    ['the wrap-up page', 'docs/06-wrap-up.md', {}],
    ['the welcome page', 'docs/index.md', {}],
  ];
  for (const [name, file, options] of targets) {
    for (const body of ['Look up, Maik demos this now.', 'Something else entirely.']) {
      const errors = run({[file]: withBox(file, ':::info[Sync moment]', body, options)}).errors;
      assert.equal(errors.length, 1, `${name}, body "${body}": ${errors.join('\n')}`);
      assert.match(errors[0], SYNC_BAN, name);
      assert.match(errors[0], /^structure: .*: line \d+: /, name);
    }
  }
});

test('structure: the sync moment ban holds for any admonition type, case, spacing and title suffix', () => {
  const titles = [
    ':::note[Sync moment]',
    ':::tip[sync moment]',
    ':::warning[SYNC MOMENT]',
    ':::info[Sync  Moment]',
    ':::info[Sync\tmoment]',
    ':::info[Syncmoment]',
    ':::info[Sync moment: hooks]',
    ':::info[The Sync moment]',
    ':::danger[Sync moment (3 minutes)]',
  ];
  for (const opener of titles) {
    const errors = run({[L(1)]: withBox(L(1), opener)}).errors;
    assert.equal(errors.length, 1, `${opener}: ${errors.join('\n')}`);
    assert.match(errors[0], SYNC_BAN, opener);
  }
});

test('structure: a sync moment nested in another admonition, or never closed, is still banned', () => {
  const nested = replaceOnce(page(1), SKELETON_MARKER, `${SKELETON_MARKER}\n\n::::tip[Outer]\n:::info[Sync moment]\nbody\n:::\n::::`);
  assertErrors(run({[L(1)]: nested}).errors, [/is a sync moment/]);
  const unclosed = `${WRAP_UP_HEAD}:::info[Sync moment]\nLook up, Maik demos this now.\n`;
  assertErrors(run({'docs/06-wrap-up.md': unclosed}).errors, [/is a sync moment/, /the info admonition is never closed/]);
});

test('structure: titles without "sync moment" pass, also near misses and the words in the body', () => {
  for (const opener of [':::info[Heads up]', ':::tip[Make it yours]', ':::note[In sync]', ':::info[A moment]', ':::info[Sync]', ':::info']) {
    assert.deepEqual(run({[L(1)]: withBox(L(1), opener)}).errors, [], opener);
  }
  // The ban reads titles only: the deny-list in the content rules handles prose.
  assert.deepEqual(run({[L(1)]: withBox(L(1), ':::info[Heads up]', 'Maik demos this in a sync moment.')}).errors, []);
  // Fenced text is not an admonition.
  const fenced = replaceOnce(page(1), SKELETON_MARKER, `${SKELETON_MARKER}\n\n\`\`\`text\n:::info[Sync moment]\n:::\n\`\`\``);
  assert.deepEqual(run({[L(1)]: fenced}).errors, []);
});

test('structure: a checkpoint needs a fenced block and a line starting with Expected', () => {
  const tip = (body) => replaceOnce(page(1), SKELETON_MARKER, `${SKELETON_MARKER}\n\n:::tip[Checkpoint]\n${body}\n:::`);
  assert.deepEqual(run({[L(1)]: tip('Run:\n\n```bash\n./mvnw verify\n```\n\nExpected: BUILD SUCCESS.')}).errors, []);
  assertErrors(run({[L(1)]: tip('Run it.\n\nExpected: BUILD SUCCESS.')}).errors, [/Checkpoint.*fenced/]);
  assertErrors(run({[L(1)]: tip('Run:\n\n```bash\n./mvnw verify\n```\n\nYou should see BUILD SUCCESS.')}).errors, [/Checkpoint.*Expected/]);
  assertErrors(run({[L(1)]: tip('Run:\n\n```bash\n./mvnw verify\n```\n\nThe Expected output is BUILD SUCCESS.')}).errors, [/Checkpoint.*Expected/]);
});

test('structure: a finished level page holds no sync moment and needs none, two of them give one error each', () => {
  assert.deepEqual(run({[L(1)]: page(1, {skeleton: false})}).errors, []);
  assert.ok(!/Sync moment/i.test(page(1, {skeleton: false})), 'the finished fixture page must not carry a sync moment');
  const box = ':::info[Sync moment]\nbody\n:::\n';
  const two = replaceOnce(page(1, {skeleton: false}), '## Level cleared when', `${box}\n${box}\n## Level cleared when`);
  assertErrors(run({[L(1)]: two}).errors, [/is a sync moment/, /is a sync moment/]);
});

// Anchors

test('structure: every contractual anchor must sit on an H2 of its page', () => {
  assertErrors(run({[L(2)]: replaceOnce(page(2), ' {/* #part-b-mcp */}', '')}).errors, [/part-b-mcp/]);
  const h3 = replaceOnce(page(3), '## Optional: Stop hook {/* #optional-stop-hook */}', '### Optional: Stop hook {/* #optional-stop-hook */}');
  assertErrors(run({[L(3)]: h3}).errors, [/optional-stop-hook/]);
  assertErrors(run({[L(4)]: replaceOnce(page(4), '{/* #feature-briefs */}', '{/* #briefs */}')}).errors, [/feature-briefs/]);
  const trouble = goodDocs()['docs/reference/troubleshooting.md'];
  assertErrors(run({'docs/reference/troubleshooting.md': replaceOnce(trouble, ' {/* #anthropic-api-key */}', '')}).errors, [/anthropic-api-key/]);
});

test('structure: the {#id} heading form fails, it does not compile in MDX with future.v4', () => {
  const text = replaceOnce(page(2), '{/* #part-a-lsp */}', '{#part-a-lsp}');
  assertErrors(run({[L(2)]: text}).errors, [/part-a-lsp.*\{\/\* #part-a-lsp \*\/\}/]);
});

test('structure: an anchor inside a fenced block does not count', () => {
  const text = replaceOnce(page(2), '## Part A: LSP {/* #part-a-lsp */}', '```md\n## Part A: LSP {/* #part-a-lsp */}\n```');
  assertErrors(run({[L(2)]: text}).errors, [/part-a-lsp/]);
});

// Files and sidebar

test('structure: exactly the eleven pages and the category file exist', () => {
  assertErrors(run({'docs/extra.md': '---\ntitle: X\n---\n'}).errors, [/unexpected file docs\/extra\.md/]);
  assertErrors(run({'docs/06-wrap-up.md': null}).errors, [/missing .*06-wrap-up\.md/]);
  assertErrors(run({'docs/reference/_category_.json': null}).errors, [/missing .*_category_\.json/]);
  assertErrors(run({'docs/img/a.png': 'x'}).errors, [/unexpected file docs\/img\/a\.png/]);
});

test('structure: index is the home page and the sidebar positions are fixed', () => {
  const index = goodDocs()['docs/index.md'];
  assertErrors(run({'docs/index.md': replaceOnce(index, 'slug: /\n', '')}).errors, [/slug/]);
  assertErrors(run({'docs/index.md': replaceOnce(index, 'sidebar_position: 1', 'sidebar_position: 5')}).errors, [/sidebar_position/]);
  assertErrors(run({'docs/00-setup-check.md': replaceOnce(goodDocs()['docs/00-setup-check.md'], 'sidebar_position: 2\n', '')}).errors, [/sidebar_position/]);
  assertErrors(run({[L(2)]: replaceOnce(page(2), 'sidebar_position: 5', 'sidebar_position: 4')}).errors, [/sidebar_position/]);
});

test('structure: the Reference category is labelled and comes after Wrap-up', () => {
  const category = 'docs/reference/_category_.json';
  assertErrors(run({[category]: '{"label": "Docs", "position": 9}'}).errors, [/label/]);
  assertErrors(run({[category]: '{"label": "Reference", "position": 8}'}).errors, [/position/]);
  assertErrors(run({[category]: '{"label": "Reference"}'}).errors, [/position/]);
  assertErrors(run({[category]: '{nope'}).errors, [/not valid JSON/]);
});

test('structure: other page titles are fixed', () => {
  const text = replaceOnce(goodDocs()['docs/reference/cheat-sheet.md'], 'title: Cheat sheet', 'title: Cheatsheet');
  assertErrors(run({'docs/reference/cheat-sheet.md': text}).errors, [/title/]);
});

test('structure: a docs file with a NUL byte is an error, not skipped', () => {
  const {errors} = run({}, {nulFiles: ['docs/06-wrap-up.md']});
  assertErrors(errors, [/docs\/06-wrap-up\.md.*NUL byte/]);
});

// Pack files

test('structure: the plan guards come through, with the plan named', () => {
  const {errors} = run({}, {planText: `${fixturePlan()}\n**Level cleared when**\n- x\n`});
  assertErrors(errors, [/workshop plan: found 6/]);
});

test('structure: the L4 list of the SDD guide must equal the plan, and the message names the SDD guide', () => {
  const items = [...FIXTURE_LEVELS[4].items];
  items[1] = items[1].replace('green', 'red');
  const {errors} = run({}, {sddText: fixtureSdd(items)});
  assertErrors(errors, [/SDD framework guide.*item 2/]);
});

test('structure: the L4 page list must equal the plan list', () => {
  const text = replaceOnce(page(4), 'not claimed', 'claimed');
  assertErrors(run({[L(4)]: text}).errors, [/item 2 /]);
});

// Rework round 1: the at a glance box holds one fenced block, under Catch-up only

test('structure: a second fenced block in the Catch-up entry fails, whichever side the right one is on', () => {
  const bogus = '```bash\ngit reset --hard origin/main\n```\n\n';
  const above = replaceOnce(page(1), '**Catch-up:**\n\n```bash\n', `**Catch-up:**\n\n${bogus}\`\`\`bash\n`);
  assertErrors(run({[L(1)]: above}).errors, [/Catch-up/]);
  const below = replaceOnce(page(1), 'git switch -C level-1 level-0-start\n```\n', 'git switch -C level-1 level-0-start\n```\n\n```bash\ngit pull\n```\n');
  assertErrors(run({[L(1)]: below}).errors, [/Catch-up/]);
});

test('structure: a fenced block under any other box line fails', () => {
  const goal = replaceOnce(page(1), 'the obvious thing.\n', 'the obvious thing.\n\n```bash\nrm -rf .claude\n```\n');
  assertErrors(run({[L(1)]: goal}).errors, [/fenced block under the Goal line/]);
  const time = replaceOnce(page(1), '**Time:** 35 minutes\n', '**Time:** 35 minutes\n\n```bash\ngit pull\n```\n');
  assertErrors(run({[L(1)]: time}).errors, [/fenced block under the Time line/]);
  const compare = replaceOnce(page(1), '**Compare:** `solution/level-1`\n', '**Compare:** `solution/level-1`\n\n```bash\ngit pull\n```\n');
  assertErrors(run({[L(1)]: compare}).errors, [/fenced block under the Compare line/]);
});

test('structure: text on the Catch-up line itself fails', () => {
  const inline = replaceOnce(page(1), '**Catch-up:**\n', '**Catch-up:** run this first\n');
  assertErrors(run({[L(1)]: inline}).errors, [/Catch-up/]);
  const wrapped = replaceOnce(page(1), '**Catch-up:**\n\n```bash\n', '**Catch-up:**\nrun this first\n\n```bash\n');
  assertErrors(run({[L(1)]: wrapped}).errors, [/Catch-up/]);
});

// Rework round 1: every form of H1 that Markdown renders

test('structure: an indented ATX H1 of up to three spaces fails, four spaces is code and passes', () => {
  const h1 = (indent) => run({[L(1)]: replaceOnce(page(1), SKELETON_MARKER, `${SKELETON_MARKER}\n\n${indent}# Foo`)}).errors;
  assertErrors(h1(' '), [/H1/]);
  assertErrors(h1('   '), [/H1/]);
  assert.deepEqual(h1('    '), []);
});

test('structure: a setext H1 fails, a setext H2 and a === line inside a fence do not', () => {
  const body = (text) => run({[L(1)]: replaceOnce(page(1), SKELETON_MARKER, `${SKELETON_MARKER}\n\n${text}`)}).errors;
  assertErrors(body('Foo\n==='), [/setext H1/]);
  assertErrors(body('Foo\n   ======  '), [/setext H1/]);
  assert.deepEqual(body('Foo\n---'), []);
  assert.deepEqual(body('```text\nFoo\n===\n```'), []);
  assert.deepEqual(body('A paragraph.\n\n==='), []);
});

// T5 (plan.md A5, FF-15): the Start branch line and the shortest setext underlines

test('structure: a fenced block under the Start branch line fails', () => {
  const start = replaceOnce(page(1), '**Start branch:** `level-0-start`\n', '**Start branch:** `level-0-start`\n\n```bash\ngit reset --hard origin/main\n```\n');
  assertErrors(run({[L(1)]: start}).errors, [/fenced block under the Start branch line/]);
});

test('structure: a setext H1 underlined with = or == fails, as ===', () => {
  const body = (text) => run({[L(1)]: replaceOnce(page(1), SKELETON_MARKER, `${SKELETON_MARKER}\n\n${text}`)}).errors;
  assertErrors(body('Foo\n='), [/setext H1/]);
  assertErrors(body('Foo\n=='), [/setext H1/]);
});

// Rework round 1: every contractual anchor is pinned by page and exact id

const ANCHORS = {
  2: ['part-a-lsp', 'part-b-mcp', 'stretch'],
  3: ['part-a-permissions', 'part-b-hooks', 'optional-stop-hook', 'part-c-skill-and-command', 'stretch'],
  4: ['tracks', 'feature-briefs'],
};
const TROUBLESHOOTING = [
  'windows-hooks',
  'jdtls-not-starting',
  'usage-limits',
  'mcp-not-connecting',
  'anthropic-api-key',
  'offline-copy-blank-page',
  'docker-not-running',
  'far-behind',
];

test('structure: each of the 18 contractual anchors fails when its heading id is removed, and the other pages stay ok', () => {
  const cases = [];
  for (const [level, ids] of Object.entries(ANCHORS)) {
    for (const id of ids) cases.push({file: L(Number(level)), text: page(Number(level)), id});
  }
  const trouble = goodDocs()['docs/reference/troubleshooting.md'];
  for (const id of TROUBLESHOOTING) cases.push({file: 'docs/reference/troubleshooting.md', text: trouble, id});
  assert.equal(cases.length, 18);
  for (const {file, text, id} of cases) {
    const errors = run({[file]: replaceOnce(text, ` {/* #${id} */}`, '')}).errors;
    assert.equal(errors.length, 1, `${file} without #${id}:\n${errors.join('\n')}`);
    assert.ok(errors[0].startsWith(`structure: ${file}: `) && errors[0].includes(`#${id} `), `${file} without #${id}:\n${errors[0]}`);
  }
});

test('structure: the fixture and the pinned anchor table agree, so a page needs no anchor beyond them', () => {
  for (const n of [0, 1, 2, 3, 4]) assert.deepEqual(FIXTURE_LEVELS[n].anchors.map(([, id]) => id), ANCHORS[n] ?? [], `Level ${n}`);
  assert.deepEqual(TROUBLESHOOTING_ANCHORS.map(([, id]) => id), TROUBLESHOOTING);
});

// Rework round 1: rules that had no self-test

test('structure: the at a glance box must follow LevelProgress directly', () => {
  const text = replaceOnce(page(1), '<LevelProgress current={1} />\n', '<LevelProgress current={1} />\n\nSome text first.\n');
  assertErrors(run({[L(1)]: text}).errors, [/box must follow LevelProgress directly/]);
  const comment = replaceOnce(page(1), '<LevelProgress current={1} />\n', '<LevelProgress current={1} />\n\n{/* a comment */}\n');
  assertErrors(run({[L(1)]: comment}).errors, [/box must follow LevelProgress directly/]);
});

test('structure: a level page has exactly one at a glance box', () => {
  const second = replaceOnce(page(1), SKELETON_MARKER, `${SKELETON_MARKER}\n\n:::note[At a glance]\n\n**Goal:** Again.\n\n:::`);
  assertErrors(run({[L(1)]: second}).errors, [/2 :::note\[At a glance\] boxes, expected 1/]);
});

test('structure: an admonition that is never closed fails and names its line', () => {
  const text = replaceOnce(page(1), SKELETON_MARKER, `${SKELETON_MARKER}\n\n:::tip[Later]\nbody without a closing line`);
  const {errors} = run({[L(1)]: text});
  assertErrors(errors, [/line \d+: the tip admonition is never closed/]);
  const wrapUp = '---\ntitle: Wrap-up\nsidebar_position: 8\n---\n\n:::info[Heads up]\nSome text.\n';
  assertErrors(run({'docs/06-wrap-up.md': wrapUp}).errors, [/the info admonition is never closed/]);
});

test('structure: errors from parsing the SDD guide are reported, they do not pass silently', () => {
  const renamed = fixtureSdd().replace('### Level cleared when (all tracks)', '### Cleared (all tracks)');
  assertErrors(run({}, {sddText: renamed}).errors, [/^structure: SDD framework guide: no "Level cleared when" heading$/]);
  const twice = `${fixtureSdd()}\n### Level cleared when\n- x\n`;
  assertErrors(run({}, {sddText: twice}).errors, [/^structure: SDD framework guide: 2 "Level cleared when" headings, expected 1$/]);
});

test('structure: an SDD guide heading with no list is an error', () => {
  const noList = '# Guide\n\n### Level cleared when (all tracks)\nSome prose instead of a list.\n';
  assertErrors(run({}, {sddText: noList}).errors, [/^structure: SDD framework guide: the "Level cleared when" heading has no list$/]);
});

test('structure: a plan with no parsable levels reports the plan, not the pages', () => {
  const {errors} = run({}, {planText: '# Empty plan\n'});
  assert.ok(errors.some((e) => /workshop plan: no section for Level 0/.test(e)), errors.join('\n'));
  assert.ok(errors.every((e) => !/item \d/.test(e)), errors.join('\n'));
});
