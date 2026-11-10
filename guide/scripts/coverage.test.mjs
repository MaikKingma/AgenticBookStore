import {test} from 'node:test';
import assert from 'node:assert/strict';
import {coverageErrors, isSkeleton, manifestShapeErrors, packSection, pageViews} from './rules/coverage.mjs';
import {SKELETON_MARKER} from './rules/structure.mjs';

// A small pack and two pages, written out by hand: the sections, the headings inside a
// fenced block and the wrapped lines are the cases the rules have to get right.
const PLAN = `# Plan

## 3. Setup check

\`\`\`bash
claude --version            # Claude Code installed
# not a heading
java -version               # 25
\`\`\`

Run \`docker info\` and then
\`/plugin install
jdtls-lsp@claude-plugins-official\`.

> Add discounts to the bookstore. A book can get a discount percentage.
> Show the discounted price.

## 4. Level 0: Break the agent (25 min)

\`git restore . && git clean -fd\`

### Part A: Extra

Only here: \`git status\`
`;

const SDD = `# SDD

## Installing

| Framework | Install |
|---|---|
| OpenSpec | \`curl -fsSL https://x.test/install.sh \\| sh\` |
`;

const PACK = {plan: PLAN, sdd: SDD};
const SOURCE_FILES = {plan: 'workshop/workshop-plan.md', sdd: 'workshop/sdd-framework-guide.md'};
const PAGE_FILES = {SU: 'docs/00-setup-check.md', L0: 'docs/01-level-0.mdx'};

const entry = (over) => ({id: 'E1', row: 'WP:1', source: 'plan', section: '3. Setup check', page: 'SU', kind: 'command', snippet: 'java -version', ...over});

const FINISHED_SU = [
  '---',
  'title: Setup check',
  '---',
  '',
  '```bash',
  'claude --version',
  'java -version',
  '```',
  '',
  'Run `docker info` first.',
  '',
].join('\n');

function run(entries, pages = {}, over = {}) {
  return coverageErrors({
    entries,
    manualRows: [],
    sourceFiles: SOURCE_FILES,
    pageFiles: PAGE_FILES,
    pack: PACK,
    pages: {'docs/00-setup-check.md': FINISHED_SU, 'docs/01-level-0.mdx': `${SKELETON_MARKER}\n`, ...pages},
    ...over,
  });
}

test('coverage: an anchored entry that is on its finished page passes and is counted', () => {
  const result = run([entry({}), entry({id: 'E2', kind: 'text', snippet: 'docker info'})]);
  assert.deepEqual(result.errors, []);
  assert.equal(result.anchored, 2);
  assert.equal(result.total, 2);
  assert.deepEqual(result.pending, {});
});

// Rule (a) on its own: both pages are skeletons, so rule (b) does not run.
const SKELETONS = {'docs/00-setup-check.md': `${SKELETON_MARKER}\n`, 'docs/01-level-0.mdx': `${SKELETON_MARKER}\n`};
const runA = (entries, over = {}) => run(entries, SKELETONS, over);

test('coverage (a): a snippet that is not in the pack fails, names the entry and is not counted as anchored', () => {
  const result = runA([entry({}), entry({id: 'E2', snippet: 'java --version'})]);
  assert.equal(result.errors.length, 1, result.errors.join('\n'));
  assert.match(result.errors[0], /^coverage: E2: the snippet "java --version" is not in workshop\/workshop-plan\.md, section "3\. Setup check"/);
  assert.equal(result.anchored, 1);
  assert.equal(result.total, 2);
});

test('coverage (a): a snippet that exists in the pack but in another section fails', () => {
  const result = runA([entry({id: 'E3', snippet: 'git restore . && git clean -fd'})]);
  assert.match(result.errors.join('\n'), /E3: the snippet "git restore \. && git clean -fd" is not in .*section "3\. Setup check"/);
  const right = runA([entry({id: 'E3', section: '4. Level 0', snippet: 'git restore . && git clean -fd'})]);
  assert.deepEqual(right.errors, []);
});

test('coverage (a): a section runs to the next heading of the same or a higher level, so a part is scoped and its parent holds it', () => {
  assert.deepEqual(runA([entry({section: 'Part A: Extra', snippet: 'git status'})]).errors, []);
  assert.deepEqual(runA([entry({section: '4. Level 0', snippet: 'git status'})]).errors, [], 'a parent section holds its parts');
  assert.match(runA([entry({section: 'Part A: Extra', snippet: 'git clean -fd'})]).errors.join('\n'), /is not in/, 'a part does not hold its parent');
});

test('coverage (a): a part ends at the next heading of a higher level too, so it holds nothing from the section after it', () => {
  const pack = '## 4. Level 0\n\n### Part A: Extra\n\nOnly here: `git status`\n\n## 5. Level 1\n\nAfter: `git log`\n\n### Part B: More\n\nLater: `git tag`\n';
  const part = packSection(pack, 'Part A: Extra').text;
  assert.equal(part.includes('git status'), true);
  assert.equal(part.includes('git log'), false, 'the next ## ends the ### part');
  assert.equal(part.includes('git tag'), false);
  const sections = {pack: {plan: pack}, sourceFiles: SOURCE_FILES, pageFiles: PAGE_FILES, pages: SKELETONS};
  const probe = (name, snippet) => coverageErrors({entries: [entry({section: name, snippet})], ...sections}).errors.join('\n');
  assert.match(probe('Part A: Extra', 'git log'), /E1: the snippet "git log" is not in workshop\/workshop-plan\.md, section "Part A: Extra"/);
  assert.equal(probe('Part A: Extra', 'git status'), '');
  assert.equal(probe('5. Level 1', 'git tag'), '', 'a parent holds its own parts');
});

test('coverage (a): a heading-like line inside a fenced block does not end or start a section', () => {
  assert.equal(packSection(PLAN, '3. Setup check').text.includes('java -version'), true);
  assert.deepEqual(packSection(PLAN, 'not a heading'), {error: 'no heading starts with "not a heading"'});
});

test('coverage (a): a section name that matches no heading or two headings is an error', () => {
  assert.match(run([entry({section: '9. Nowhere'})]).errors.join('\n'), /E1: plan section "9\. Nowhere": no heading starts with "9\. Nowhere"/);
  const twice = coverageErrors({
    entries: [entry({section: 'Dup'})],
    sourceFiles: SOURCE_FILES,
    pageFiles: PAGE_FILES,
    pack: {plan: '## Dup one\n\nx\n\n## Dup two\n\ny\n'},
    pages: {'docs/00-setup-check.md': FINISHED_SU},
  });
  assert.match(twice.errors.join('\n'), /2 headings start with "Dup"/);
});

test('coverage (a): a missing pack file fails every entry of that source and names the file', () => {
  const result = run([entry({})], {}, {pack: {plan: null, sdd: SDD}});
  assert.match(result.errors.join('\n'), /E1: plan section .*: the pack file workshop\/workshop-plan\.md is missing/);
  assert.equal(result.anchored, 0);
});

test('coverage (a): wrapped pack lines, blockquote markers and escaped table pipes are normalised', () => {
  const wrapped = entry({kind: 'text', snippet: '/plugin install jdtls-lsp@claude-plugins-official'});
  const quote = entry({id: 'E2', kind: 'text', snippet: 'Add discounts to the bookstore. A book can get a discount percentage. Show the discounted price.'});
  const pipe = entry({id: 'E3', source: 'sdd', section: 'Installing', kind: 'command', snippet: 'curl -fsSL https://x.test/install.sh | sh'});
  const page = `${FINISHED_SU}\n\`/plugin install jdtls-lsp@claude-plugins-official\`\n\nAdd discounts to the bookstore. A book can get a discount percentage. Show the discounted price.\n\n\`\`\`bash\ncurl -fsSL https://x.test/install.sh | sh\n\`\`\`\n`;
  const result = run([wrapped, quote, pipe], {'docs/00-setup-check.md': page});
  assert.deepEqual(result.errors, []);
  assert.equal(result.anchored, 3);
});

test('coverage: a JSON snippet matches however the page breaks its lines after brackets, and still fails on a changed value', () => {
  const plan = '## 3. Setup check\n\n```json\n{\n  "deny":  ["Read(./.env)", "Bash(git push)"]\n}\n```\n';
  const e = entry({snippet: '{\n  "deny":  ["Read(./.env)", "Bash(git push)"]\n}'});
  const page = (body) => `---\ntitle: X\n---\n\n\`\`\`json\n${body}\n\`\`\`\n`;
  const check = (body) => run([e], {'docs/00-setup-check.md': page(body)}, {pack: {plan, sdd: SDD}});
  assert.deepEqual(check('{\n  "deny": [\n    "Read(./.env)",\n    "Bash(git push)"\n  ]\n}').errors, []);
  assert.deepEqual(check('{"deny": ["Read(./.env)", "Bash(git push)"]}').errors, []);
  assert.equal(check('{"deny": ["Read(./.env)"]}').errors.length, 1);
  assert.equal(check('{"deny": ["Read(./.env)", "Bash(git push)", "Bash(rm *)"]}').errors.length, 1);
});

test('coverage: a long snippet is shortened in the message', () => {
  const long = 'x'.repeat(200);
  const [message] = runA([entry({snippet: long})]).errors;
  assert.ok(message.length < 300 && message.includes(`${'x'.repeat(87)}...`), message);
});

test('coverage (a): the match is case-sensitive and exact, a word inside a longer snippet is not enough', () => {
  // The rule (a) message itself, not just "not in": the rule (b) message of a snippet that is
  // missing from a finished page ("is not in a fenced block of the page") reads alike. The
  // pages here are finished and carry the lower-case command, so only rule (a) can speak.
  for (const [snippet, shownAs] of [['JAVA -version', 'JAVA -version'], ['java -version 26', 'java -version 26']]) {
    const result = run([entry({snippet, pageSnippet: 'java -version'}), entry({id: 'E2', snippet: 'java -version'})]);
    assert.equal(result.errors.length, 1, result.errors.join('\n'));
    assert.match(result.errors[0], new RegExp(`^coverage: E1: the snippet "${shownAs}" is not in workshop/workshop-plan\\.md, section "3\\. Setup check"$`));
    assert.equal(result.anchored, result.total - 1);
  }
});

test('coverage (b): a finished page without the page snippet fails and names page and entry', () => {
  const result = run([entry({snippet: 'claude --version', pageSnippet: 'claude --versions'})]);
  assert.equal(result.errors.length, 1, result.errors.join('\n'));
  assert.match(result.errors[0], /^coverage: E1: docs\/00-setup-check\.md: the command "claude --versions" is not in a fenced block of the page/);
});

test('coverage (b): the page snippet replaces the snippet, so a placeholder path in the pack needs the real path on the page', () => {
  const plan = `${PLAN}\n## 5. Level 1\n\nSee \`distributor/src/.../Tools.java\`.\n`;
  const e = entry({section: '5. Level 1', kind: 'text', snippet: 'distributor/src/.../Tools.java', pageSnippet: 'distributor/src/main/Tools.java'});
  const page = `${FINISHED_SU}\nSee distributor/src/main/Tools.java.\n`;
  assert.deepEqual(run([e], {'docs/00-setup-check.md': page}, {pack: {plan, sdd: SDD}}).errors, []);
  const dots = `${FINISHED_SU}\nSee distributor/src/.../Tools.java.\n`;
  assert.match(run([e], {'docs/00-setup-check.md': dots}, {pack: {plan, sdd: SDD}}).errors.join('\n'), /the text "distributor\/src\/main\/Tools\.java" is not on the page/);
});

test('coverage (b): the snippet must be on the page of the entry, not on another page', () => {
  const result = run([entry({page: 'L0'})], {'docs/01-level-0.mdx': '---\ntitle: X\n---\n\n```bash\nnothing\n```\n', 'docs/00-setup-check.md': FINISHED_SU});
  assert.match(result.errors.join('\n'), /docs\/01-level-0\.mdx: the command "java -version" is not in a fenced block/);
});

test('coverage (b): a command only in prose inline code fails, in a fenced block it passes', () => {
  const prose = '---\ntitle: X\n---\n\nRun `java -version` now.\n';
  assert.match(run([entry({})], {'docs/00-setup-check.md': prose}).errors.join('\n'), /the command "java -version" is not in a fenced block/);
  assert.deepEqual(run([entry({kind: 'text'})], {'docs/00-setup-check.md': prose}).errors, [], 'a text entry accepts prose');
});

test('coverage (b): a snippet only inside an MDX or HTML comment does not count, for commands and for text', () => {
  const comments = [
    '{/* ```bash\njava -version\n``` */}',
    '{/*\n```bash\njava -version\n```\n*/}',
    '<!--\n```bash\njava -version\n```\n-->',
    '{/* java -version */}',
  ];
  for (const comment of comments) {
    const page = `---\ntitle: X\n---\n\n${comment}\n`;
    for (const kind of ['command', 'text']) {
      const expected = kind === 'command' ? /the command "java -version" is not in a fenced block of the page/ : /the text "java -version" is not on the page/;
      assert.match(run([entry({kind})], {'docs/00-setup-check.md': page}).errors.join('\n'), expected, `${kind} in ${comment}`);
    }
  }
});

test('coverage (b): a command inside a fenced block inside a TabItem passes', () => {
  const page = '---\ntitle: X\n---\n\n<Tabs groupId="os">\n  <TabItem value="a" label="macOS/Linux">\n    ```bash\n    java -version\n    ```\n  </TabItem>\n</Tabs>\n';
  assert.deepEqual(run([entry({})], {'docs/00-setup-check.md': page}).errors, []);
});

test('coverage (b): a wrapped command and a text snippet across a blockquote are matched on whitespace-collapsed text', () => {
  const page = '---\ntitle: X\n---\n\n> Add discounts to the bookstore.\n> A book can get a\n> discount percentage.\n\n```bash\njava    -version\n```\n';
  const quote = entry({id: 'E2', kind: 'text', snippet: 'Add discounts to the bookstore. A book can get a discount percentage.'});
  const result = run([entry({}), quote], {'docs/00-setup-check.md': page});
  assert.deepEqual(result.errors, []);
});

test('coverage (b): the at a glance box and the checklist cannot satisfy an entry', () => {
  const level = [
    '---',
    'title: X',
    '---',
    '',
    ':::note[At a glance]',
    '',
    '**Catch-up:**',
    '',
    '```bash',
    'java -version',
    '```',
    '',
    ':::',
    '',
    '## Level cleared when',
    '',
    '- [ ] `docker info` shows it.',
    '',
  ].join('\n');
  const result = run([entry({}), entry({id: 'E2', kind: 'text', snippet: 'docker info'})], {'docs/00-setup-check.md': level});
  assert.equal(result.errors.length, 2, result.errors.join('\n'));
  const body = level.replace('## Level cleared when', 'Body: java -version docker info\n\n## Level cleared when');
  assert.equal(run([entry({kind: 'text'})], {'docs/00-setup-check.md': body}).errors.length, 0);
});

test('coverage (b): the frontmatter is metadata, a snippet only there does not count', () => {
  const only = '---\ntitle: X\ndescription: docker info\n---\n\nBody.\n';
  assert.match(run([entry({kind: 'text', snippet: 'docker info'})], {'docs/00-setup-check.md': only}).errors.join('\n'), /the text "docker info" is not on the page/);
  const body = '---\ntitle: X\n---\n\nRun docker info.\n';
  assert.deepEqual(run([entry({kind: 'text', snippet: 'docker info'})], {'docs/00-setup-check.md': body}).errors, []);
  assert.equal(pageViews(only).prose, 'Body.');
});

test('coverage (b): a JSX comment with spaces inside the braces hides its text as well', () => {
  const page = '---\ntitle: X\n---\n\n{ /* docker info */ }\n';
  assert.match(run([entry({kind: 'text', snippet: 'docker info'})], {'docs/00-setup-check.md': page}).errors.join('\n'), /not on the page/);
});

test('coverage (b): a text snippet may sit in a fenced block', () => {
  const page = '---\ntitle: X\n---\n\n```bash\ndocker info\n```\n';
  assert.deepEqual(run([entry({id: 'E2', kind: 'text', snippet: 'docker info'})], {'docs/00-setup-check.md': page}).errors, []);
});

test('coverage: a page that still has the marker is exempt from rule (b), lists its entries as pending, and rule (a) still runs', () => {
  const skeleton = `---\ntitle: X\n---\n\n${SKELETON_MARKER}\n`;
  const result = run([entry({}), entry({id: 'E2', snippet: 'not in the pack'})], {'docs/00-setup-check.md': skeleton});
  assert.deepEqual(result.pending, {'docs/00-setup-check.md': ['E1', 'E2']});
  assert.equal(result.errors.length, 1, result.errors.join('\n'));
  assert.match(result.errors[0], /E2: the snippet "not in the pack" is not in/);
  assert.equal(result.anchored, 1);
});

test('coverage: removing the marker lists every entry of the page that is not on it', () => {
  const bare = '---\ntitle: Level 0\n---\n\nnothing yet\n';
  const result = run([entry({id: 'L0-1', page: 'L0', section: '4. Level 0', snippet: 'git restore . && git clean -fd'})], {'docs/01-level-0.mdx': bare});
  assert.match(result.errors.join('\n'), /L0-1: docs\/01-level-0\.mdx: the command "git restore \. && git clean -fd"/);
});

test('coverage: a page file that is missing is an error', () => {
  const result = coverageErrors({entries: [entry({})], sourceFiles: SOURCE_FILES, pageFiles: PAGE_FILES, pack: PACK, pages: {}});
  assert.match(result.errors.join('\n'), /E1: the page docs\/00-setup-check\.md is missing/);
});

test('coverage: isSkeleton reads the one marker of the structure rules, exactly', () => {
  assert.equal(isSkeleton(`x\n${SKELETON_MARKER}\ny`), true);
  assert.equal(isSkeleton('x\ny'), false);
});

test('pageViews: comments are blanked, blockquote markers dropped, blocks kept apart', () => {
  const view = pageViews('a {/* hidden */} b\n> c\n\n```bash\nx   y\n```\n```bash\nz\n```\n');
  assert.equal(view.prose, 'a b c');
  assert.deepEqual(view.blocks, ['x y', 'z']);
});

test('coverageErrors: a malformed manifest entry is an error of the rule itself, not only of a direct manifestShapeErrors call', () => {
  const messages = (entries) => run(entries, SKELETONS).errors.join('\n');
  assert.match(messages([entry({}), entry({})]), /^coverage: entry E1: the id is used twice$/m);
  assert.match(messages([entry({kind: 'prose'})]), /^coverage: entry E1: unknown kind "prose"$/m);
  assert.match(messages([entry({source: 'nope'})]), /unknown source "nope"/);
  assert.match(messages([entry({row: ''})]), /the field row is missing/);
  assert.match(coverageErrors({entries: [entry({})], manualRows: [{row: 'WP:2', item: 'x', page: 'ZZ', reason: ''}], sourceFiles: SOURCE_FILES, pageFiles: PAGE_FILES, pack: PACK, pages: SKELETONS}).errors.join('\n'), /manual row WP:2: the field reason is missing/);
});

test('manifest shape: an empty manifest, missing fields, duplicate ids and unknown values are errors', () => {
  const shape = (entries, manualRows = []) => manifestShapeErrors({entries, manualRows, sourceFiles: SOURCE_FILES, pageFiles: PAGE_FILES}).join('\n');
  assert.match(shape([]), /the manifest has no entries/);
  assert.match(shape([entry({snippet: ' '})]), /E1: the field snippet is missing/);
  assert.match(shape([entry({row: undefined})]), /E1: the field row is missing/);
  assert.match(shape([entry({}), entry({})]), /the id is used twice/);
  assert.match(shape([entry({source: 'nope'})]), /unknown source "nope"/);
  assert.match(shape([entry({page: 'ZZ'})]), /unknown page "ZZ"/);
  assert.match(shape([entry({kind: 'prose'})]), /unknown kind "prose"/);
  assert.match(shape([entry({pageSnippet: ''})]), /pageSnippet must be a non-empty string/);
  assert.match(shape([entry({})], [{row: 'WP:1', item: 'x', page: 'ZZ', reason: ''}]), /manual row WP:1: the field reason is missing[\s\S]*unknown page "ZZ"/);
  assert.equal(shape([entry({})], [{row: 'WP:2', item: 'x', page: 'SU', reason: 'prose'}]), '');
});
