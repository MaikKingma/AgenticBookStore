import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {dirname, join} from 'node:path';
import {fileURLToPath} from 'node:url';
import {scanMarkdown} from './lib/mdscan.mjs';
import {BONUS_PLAN_HEADING, SUBAGENT_KEYS, TIME_SLOT_PATTERNS, bonusFragmentErrors, bonusMembershipErrors, bonusPlanSection, codeLiterals, timeSlotErrors} from './rules/content.mjs';
import {normalise, readBullets} from './rules/plan.mjs';

// The guards of the optional bonus level page (spec 16, task T3, decisions D-S4 and D-S6). The rules are pure functions in
// rules/content.mjs; this file runs them on fixture strings (every witness that must fail, and the matching form that
// passes) and on the live page and the live plan. `npm run check` runs it, so a literal that appears only on the page, a
// real value in the frontmatter fragment or a time slot turns the check red.
//
// The MDX comment markers are built from parts: a literal one in this file would be read as a block comment by the
// zero-test guard (FF-T2-1).
const COMMENT_OPEN = '{/' + '*';
const COMMENT_CLOSE = '*' + '/}';

const GUIDE_ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const LIVE_PAGE = readFileSync(join(GUIDE_ROOT, 'docs', '06-bonus-level-orchestration.mdx'), 'utf8');
const LIVE_PLAN = readFileSync(join(GUIDE_ROOT, '..', 'docs', 'spec-pack', 'workshop', 'workshop-plan.md'), 'utf8');

// A bonus page: frontmatter, the marker box, then the body under test.
const HEAD = '---\ntitle: "Bonus level: orchestrate your own team"\nsidebar_position: 7.5\n---\n\n:::info[Optional bonus level]\n\nThis bonus level is optional. [Wrap-up](./06-wrap-up.md)\n\n:::\n\n';
const page = (body) => `${HEAD}${body}\n`;

// A plan: the bonus section sits between the Level 4 section and the Wrap-up section, which hold literals of their own.
const SECTION = [
  `## ${BONUS_PLAN_HEADING}`,
  '',
  'Intro with `claude --version`, `/workflows`, `a | b` and `multi word span`. The word prose-only is only prose here.',
  '',
  '```bash',
  'claude --version',
  '/workflows',
  'deploy # note',
  '  indented-line',
  '```',
  '',
  '### A sub heading',
  '',
  'Under it `under-sub-heading`.',
  '',
  '```text',
  'fence-under-sub-heading',
  '```',
  '',
];
const PLAN = [
  '# Workshop plan',
  '',
  '## 9. Level 4: Spec-driven development (45 min)',
  '',
  'Level 4 has `only-in-nine` and a line below.',
  '',
  '```bash',
  'only-in-nine --flag',
  '```',
  '',
  ...SECTION,
  '## 10. Wrap-up (15 min)',
  '',
  'Wrap `only-in-wrap-up`.',
  '',
  '```bash',
  'only-in-wrap-up --flag',
  '```',
  '',
].join('\n');

const members = (body, plan = PLAN) => bonusMembershipErrors(page(body), plan);
const passes = (errors) => assert.deepEqual(errors, []);
const failsOn = (errors, literal, line) => {
  assert.equal(errors.length, 1, errors.join('\n'));
  assert.ok(errors[0].includes(literal), `${errors[0]} does not name ${literal}`);
  if (line !== undefined) assert.match(errors[0], new RegExp(`^bonus page: line ${line}: `));
};

// 1. Pack membership

// Each case: the markdown that holds one literal. With the literal `page-only` it must fail and name it; with the matching
// form (a literal of the plan section) the same structure must pass. The page body starts on line 12.
const CASES = [
  ['an inline span in prose', (lit) => `Run \`${lit}\` now.`],
  ['an inline span in a heading', (lit) => `## Part A: \`${lit}\``],
  ['an inline span in a list item', (lit) => `- Run \`${lit}\` first`],
  ['an inline span in a numbered list item', (lit) => `1. Run \`${lit}\` first`],
  ['an inline span in a table cell', (lit) => `| Part | Command |\n| --- | --- |\n| One | \`${lit}\` |`],
  ['an inline span inside the marker box', (lit) => `:::info[Optional bonus level]\n\nSee \`${lit}\`. [Wrap-up](./06-wrap-up.md)\n\n:::`],
  ['an inline span in a block quote', (lit) => `> Run \`${lit}\` now.`],
  ['an inline span inside a details element', (lit) => `<details>\n<summary>More</summary>\n\nRun \`${lit}\` here.\n\n</details>`],
  ['an inline span on a summary line', (lit) => `<details>\n<summary>\`${lit}\`</summary>\n</details>`],
  ['an inline span inside a TabItem', (lit) => `<Tabs groupId="os">\n<TabItem value="mac" label="macOS/Linux">\n\nRun \`${lit}\` here.\n\n</TabItem>\n</Tabs>`],
  ['an inline span with a double-backtick run', (lit) => `Run \`\`${lit}\`\` now.`],
  ['a line of a plain fenced block', (lit) => `\`\`\`\n${lit}\n\`\`\``],
  ['a line of a tilde fenced block', (lit) => `~~~\n${lit}\n~~~`],
  ['a line of a four-backtick fenced block', (lit) => `\`\`\`\`\n${lit}\n\`\`\`\``],
  ['a line of a fenced block with a language and meta', (lit) => `\`\`\`bash title="x" {1}\n${lit}\n\`\`\``],
  ['an indented line of a fenced block', (lit) => `\`\`\`bash\n    ${lit}   \n\`\`\``],
  ['a line of a fenced block indented in a list item', (lit) => `1. Step one\n\n   \`\`\`bash\n   ${lit}\n   \`\`\``],
  ['a line of a fenced block in a block quote', (lit) => `> \`\`\`bash\n> ${lit}\n> \`\`\``],
  ['a line of a fenced block inside a TabItem', (lit) => `<Tabs groupId="os">\n<TabItem value="mac" label="macOS/Linux">\n\n\`\`\`bash\n${lit}\n\`\`\`\n\n</TabItem>\n</Tabs>`],
  ['a line of a fenced block inside an admonition', (lit) => `:::tip[Hint]\n\n\`\`\`bash\n${lit}\n\`\`\`\n\n:::`],
  ['a raw code element in prose', (lit) => `Run <code>${lit}</code> now.`],
  ['a raw code element in a table cell', (lit) => `| Part | Command |\n| --- | --- |\n| One | <code>${lit}</code> |`],
  ['a raw code element in capitals with an attribute, over two lines', (lit) => `Run <CODE className="x">\n${lit}\n</CODE> now.`],
  ['a raw pre element on one line', (lit) => `<pre>${lit}</pre>`],
  ['a raw pre element over several lines', (lit) => `<pre>\n${lit}\n</pre>`],
  ['a raw pre element with an attribute and an indented line', (lit) => `<PRE className="x">\n    ${lit}   \n</PRE>`],
  ['a raw pre element inside a TabItem', (lit) => `<Tabs groupId="os">\n<TabItem value="mac" label="macOS/Linux">\n\n<pre>\n${lit}\n</pre>\n\n</TabItem>\n</Tabs>`],
];

test('membership: a literal that only the page has fails, in every place a span or a fenced line can stand, and names it and its line', () => {
  for (const [name, make] of CASES) {
    const errors = members(make('page-only'));
    assert.ok(errors.length >= 1, `${name}: no error`);
    assert.ok(errors.every((error) => error.includes('page-only')), `${name}: ${errors.join('\n')}`);
    assert.match(errors[0], /^bonus page: line \d+: the (inline code span `page-only`|fenced line "page-only") is not a whole inline code span or fenced line of the "Bonus level" section of the plan$/, name);
  }
});

test('membership: the matching literal passes in every one of those places (so the failures above are the literal, not the structure)', () => {
  for (const [name, make] of CASES) {
    passes(members(make('claude --version')));
    assert.ok(codeLiterals(page(make('claude --version'))).spans.length + codeLiterals(page(make('claude --version'))).lines.length >= 1, `${name}: nothing was read`);
  }
});

test('membership: a raw code element inside an inline span, a fence or a comment is no element, and a plan element is a literal like a span', () => {
  failsOn(members('Run `<code>page-only</code>` now.'), '<code>page-only</code>');
  failsOn(members('```html\n<code>page-only</code>\n```'), '<code>page-only</code>');
  passes(members(`${COMMENT_OPEN} <pre>page-only</pre> ${COMMENT_CLOSE}`));
  const plan = PLAN.replace('Intro with', 'Intro with <code>raw-plan-code</code> and <pre>\nraw plan line\n</pre> and');
  passes(members('Run `raw-plan-code` now.\n\n```bash\nraw plan line\n```', plan));
  passes(members('Run <code>raw-plan-code</code> now.\n\n<pre>raw plan line</pre>', plan));
  failsOn(members('Run `raw-plan-code` now.', PLAN), 'raw-plan-code');
});

test('membership: a blank line inside a page fence is no literal, and a span in the heading of the next plan section is not in the bonus section', () => {
  passes(members('```bash\n\nclaude --version\n\n```'));
  passes(members('<pre>\n\nclaude --version\n\n</pre>'));
  const plan = PLAN.replace('## 10. Wrap-up (15 min)', '## 10. Wrap-up (15 min) `heading-span`');
  failsOn(members('Run `heading-span` now.', plan), 'heading-span');
  // raw elements of a neighbouring plan section are not in the bonus section either
  const raw = PLAN.replace('Level 4 has', 'Level 4 has <code>raw-in-nine</code> and <pre>raw nine line</pre> and').replace('Wrap `only-in-wrap-up`.', 'Wrap <code>raw-in-wrap-up</code>.');
  for (const lit of ['raw-in-nine', 'raw nine line', 'raw-in-wrap-up']) {
    failsOn(members(`Run <code>${lit}</code> now.`, raw), lit);
    failsOn(members(`<pre>${lit}</pre>`, raw), lit);
  }
  passes(members('Run `claude --version` now.', plan));
});

test('membership: the error line is the line of the literal in the page (a span on line 12, a fenced line on line 13, a list span on line 15)', () => {
  failsOn(members('Run `page-only` now.'), 'page-only', 12);
  failsOn(members('```bash\npage-only\n```'), 'page-only', 13);
  failsOn(members('One.\n\n- a list\n- Run `page-only`'), 'page-only', 15);
  failsOn(members('One.\n\n<pre>\npage-only\n</pre>'), 'page-only', 15);
  failsOn(members('<pre>page-only</pre>'), 'page-only', 12);
  failsOn(members('One.\n\nRun <code>page-only</code> now.'), 'page-only', 14);
});

test('membership: a span with an escaped pipe in a table cell is compared with the pipe, so `a \\| b` matches the plan span `a | b`', () => {
  passes(members('| Part | Command |\n| --- | --- |\n| One | `a \\| b` |'));
  passes(members('Run `a | b` now.'));
  failsOn(members('| Part | Command |\n| --- | --- |\n| One | `x \\| page-only` |'), 'page-only');
});

test('membership: a span wrapped over two lines, or with extra spaces, is compared with white space collapsed', () => {
  passes(members('Run `multi\nword span` now.'));
  passes(members('Run `multi   word  span` now.'));
  passes(members('Run ` multi word span ` now.'));
});

test('membership: a substring or a superstring of a plan literal fails, only a whole literal passes', () => {
  failsOn(members('Run `/work` now.'), '/work');
  passes(members('Run `/workflows` now.'));
  failsOn(members('Run `claude` now.'), 'claude');
  failsOn(members('Run `--version` now.'), '--version');
  failsOn(members('Run `claude --version && echo x` now.'), 'claude --version && echo x');
  failsOn(members('```bash\nclaude --version && echo x\n```'), 'claude --version && echo x');
  failsOn(members('```bash\n/work\n```'), '/work');
  failsOn(members('```bash\nclaude --version extra\n```'), 'claude --version extra');
});

test('membership: a comment added to a fenced line fails, and so does one taken away (both sides compared as written)', () => {
  failsOn(members('```bash\nclaude --version # note\n```'), 'claude --version # note');
  passes(members('```bash\ndeploy # note\n```'));
  failsOn(members('```bash\ndeploy\n```'), 'deploy');
});

test('membership: leading and trailing spaces of a fenced line do not count, the line inside is compared as written', () => {
  passes(members('```bash\n   claude --version   \n```'));
  passes(members('```bash\nindented-line\n```'));
  passes(members('```bash\n\tindented-line\n```'));
  failsOn(members('```bash\nclaude  --version\n```'), 'claude  --version');
});

test('membership: a span of the plan counts for a fenced line and the other way round, as the rule says "a span or a fenced line"', () => {
  passes(members('```bash\n/workflows\n```'));
  passes(members('Run `claude --version` or `deploy # note`.'));
  passes(members('```bash\nmulti word span\n```'));
});

test('membership: a span of the Level 4 section or of the Wrap-up section, the two neighbours, is not in the bonus section', () => {
  for (const lit of ['only-in-nine', 'only-in-nine --flag', 'only-in-wrap-up', 'only-in-wrap-up --flag']) {
    failsOn(members(`Run \`${lit}\` now.`), lit);
    failsOn(members(`\`\`\`bash\n${lit}\n\`\`\``), lit);
  }
});

test('membership: a span or a fenced line under a ### heading inside the section is in the section (it ends at the next ## heading)', () => {
  passes(members('Run `under-sub-heading` now.'));
  passes(members('```text\nfence-under-sub-heading\n```'));
});

test('membership: the words of the plan section that are no code are no literal (prose-only is prose in the plan, a span on the page)', () => {
  failsOn(members('Run `prose-only` now.'), 'prose-only');
  failsOn(members('```bash\nprose-only\n```'), 'prose-only');
  passes(members('The words claude --version and /workflows are prose on the page and need no pack source.'));
});

test('membership: several literals are several errors in page order, and a page without any literal passes', () => {
  const errors = members('Run `one-only`, then `two-only`.\n\n```bash\nthree-only\n```');
  assert.equal(errors.length, 3, errors.join('\n'));
  ['one-only', 'two-only', 'three-only'].forEach((lit, i) => assert.ok(errors[i].includes(lit), errors[i]));
  passes(members('Plain prose only.'));
});

test('membership: a comment is not code, so a span inside an MDX comment is no literal', () => {
  passes(members(`${COMMENT_OPEN} Run \`commented-out\` ${COMMENT_CLOSE}`));
  failsOn(members(`${COMMENT_OPEN} note ${COMMENT_CLOSE} Run \`page-only\` now.`), 'page-only');
});

test('membership: a plan without the bonus heading is an error, never a pass on zero literals', () => {
  const removed = PLAN.replace(`## ${BONUS_PLAN_HEADING}`, '## Another heading');
  for (const body of ['Plain prose only.', 'Run `claude --version` now.', 'Run `page-only` now.']) {
    const errors = members(body, removed);
    assert.equal(errors.length, 1, errors.join('\n'));
    assert.match(errors[0], /^bonus page: pack membership: the plan has no "## Bonus level: orchestrate your own team \(optional\)" heading$/);
  }
  assert.deepEqual(bonusMembershipErrors(page('Plain prose only.'), ''), [`bonus page: pack membership: the plan has no "## ${BONUS_PLAN_HEADING}" heading`]);
});

test('membership: only an exact level-2 heading outside a fenced block is the bonus heading', () => {
  for (const heading of [`### ${BONUS_PLAN_HEADING}`, '## Bonus level: orchestrate your own team', `# ${BONUS_PLAN_HEADING}`, `## ${BONUS_PLAN_HEADING} extra`, '## bonus level: orchestrate your own team (optional)']) {
    const errors = members('Plain prose only.', PLAN.replace(`## ${BONUS_PLAN_HEADING}`, heading));
    assert.equal(errors.length, 1, `${heading}: ${errors.join('\n')}`);
    assert.match(errors[0], /pack membership: the plan has no "## Bonus level/);
  }
  const fenced = PLAN.replace(`## ${BONUS_PLAN_HEADING}`, '## Another heading').replace('## 10. Wrap-up (15 min)', `\`\`\`md\n## ${BONUS_PLAN_HEADING}\n\`\`\`\n\n## 10. Wrap-up (15 min)`);
  assert.match(members('Plain prose only.', fenced)[0], /pack membership: the plan has no/);
});

test('membership: two bonus headings in the plan are an error too', () => {
  const doubled = `${PLAN}\n## ${BONUS_PLAN_HEADING}\n\nAgain.\n`;
  const errors = members('Plain prose only.', doubled);
  assert.equal(errors.length, 1);
  assert.match(errors[0], /the plan has 2 "## Bonus level: orchestrate your own team \(optional\)" headings, expected 1/);
});

test('membership: the section runs to the end of the plan when no ## heading follows it', () => {
  const last = PLAN.slice(0, PLAN.indexOf('## 10. Wrap-up'));
  passes(members('Run `under-sub-heading` now.', last));
  failsOn(members('Run `only-in-wrap-up` now.', last), 'only-in-wrap-up');
});

test('membership: the section bounds are the heading and the next ## heading (a # title ends it too), as bonusPlanSection reads them', () => {
  const lines = PLAN.split('\n');
  const section = bonusPlanSection(PLAN);
  assert.equal(lines[section.heading], `## ${BONUS_PLAN_HEADING}`);
  assert.equal(lines[section.from], '');
  assert.equal(lines[section.to], '## 10. Wrap-up (15 min)');
  assert.ok(section.text.startsWith(`## ${BONUS_PLAN_HEADING}\n`) && section.text.endsWith('```\n'), section.text);
  assert.ok(section.text.includes('### A sub heading'));
  assert.ok(!section.text.includes('only-in-nine') && !section.text.includes('only-in-wrap-up'));
  const title = bonusPlanSection(PLAN.replace('## 10. Wrap-up (15 min)', '# A new part'));
  assert.equal(PLAN.replace('## 10. Wrap-up (15 min)', '# A new part').split('\n')[title.to], '# A new part');
});

test('membership: the extraction is the scanner of the checker, span for span (no second scanner)', () => {
  const text = page('Run `one`, ``two``, and `x \\| y` here.\n\n| a |\n| - |\n| `cell` |\n\n```bash\nline\n```\n');
  const scan = scanMarkdown(text);
  assert.equal(codeLiterals(text).spans.length, scan.spans.length);
  assert.equal(codeLiterals(text).lines.length, scan.blocks.reduce((sum, block) => sum + block.body.filter((line) => line.trim() !== '').length, 0));
});

test('membership: the live bonus page holds no literal that the plan section lacks', () => {
  assert.deepEqual(bonusMembershipErrors(LIVE_PAGE, LIVE_PLAN), []);
});

test('membership: the live plan has the bonus section between Level 4 and the Wrap-up, with no ## heading inside it', () => {
  const section = bonusPlanSection(LIVE_PLAN);
  assert.equal(section.error, undefined);
  const lines = LIVE_PLAN.split('\n');
  assert.equal(lines[section.heading], `## ${BONUS_PLAN_HEADING}`);
  assert.match(lines[section.to], /^## 10\. Wrap-up/);
  const before = lines.slice(0, section.heading).filter((line) => /^## /.test(line));
  assert.match(before[before.length - 1], /^## 9\. Level 4/);
  assert.ok(!lines.slice(section.from, section.to).some((line) => /^## /.test(line)));
});

// 2. The frontmatter fragment

const fragment = (...lines) => ['```yaml', ...lines, '```'].join('\n');
const fragments = (body) => bonusFragmentErrors(page(body));

test('fragment: no fragment passes, and so does one fragment with only --- and placeholder key lines', () => {
  passes(fragments('Plain prose only.'));
  passes(fragments(fragment('---', 'name: <name>', 'description: <what it does>', 'tools: <tools>', 'model: <model>', '---')));
  passes(fragments(fragment('name: <name>')));
  passes(fragments(fragment('---', 'name: <name>')));
  passes(bonusFragmentErrors(LIVE_PAGE));
});

test('fragment: a real value fails, with the key, the line and the value in the message', () => {
  const errors = fragments(fragment('---', 'name: <name>', 'tools: Read, Grep', '---'));
  assert.equal(errors.length, 1, errors.join('\n'));
  assert.equal(errors[0], 'bonus page: line 15: the value of "tools" in the frontmatter fragment must be one placeholder in angle brackets such as <tools>, found "Read, Grep"');
});

test('fragment: every kind of value that is not exactly one angle-bracket placeholder fails', () => {
  for (const value of ['', 'Read', 'Read, Grep', '"<tools>"', "'<tools>'", '<tools> <more>', '<tools>, <more>', '<tools', 'tools>', '<>', '[<tools>]', '<tools> # note', 'sonnet', 'true', '|']) {
    const errors = fragments(fragment('---', `tools: ${value}`, '---'));
    assert.equal(errors.length, 1, `value "${value}": ${errors.join('\n')}`);
    assert.match(errors[0], /^bonus page: line 14: the value of "tools" in the frontmatter fragment must be one placeholder/);
  }
});

// The 19 fields of the field table of the sub-agents page of the Claude Code docs (https://code.claude.com/docs/en/sub-agents.md,
// read 2026-11-11), typed here a second time so that the table of the rule cannot grade itself.
const DOC_KEYS = ['name', 'description', 'tools', 'disallowedTools', 'model', 'permissionMode', 'maxTurns', 'autoCompactWindow', 'skills', 'mcpServers', 'hooks', 'memory', 'background', 'omitClaudeMd', 'effort', 'isolation', 'color', 'initialPrompt', 'experimental'];

test('fragment: the key table of the rule is exactly the 19 fields of the sub-agents docs (a dropped or added key fails by name)', () => {
  assert.deepEqual(SUBAGENT_KEYS, DOC_KEYS);
  assert.equal(new Set(DOC_KEYS).size, 19);
});

test('fragment: every subagent key counts, in any letter case, with a space before the colon, and indented', () => {
  for (const key of DOC_KEYS) {
    failsOn(fragments(fragment('---', `${key}: real`, '---')), key);
    failsOn(fragments(fragment(`${key.toUpperCase()}: real`)), key.toUpperCase());
    failsOn(fragments(`Write \`${key}: real\` in the file.`), `${key}: real`);
    passes(fragments(`Write \`${key}: <${key}>\` and \`${key}\` in the file.`));
    failsOn(fragments(fragment('---', `${key}: real`, '---')), key);
    passes(fragments(fragment('---', `${key}: <${key}>`, '---')));
  }
  failsOn(fragments(fragment('Tools: Read')), 'Read');
  failsOn(fragments(fragment('TOOLS : Read')), 'Read');
  failsOn(fragments(fragment('  tools: Read')), 'Read');
});

test('fragment: a raw pre element is a fenced block and a raw code element is a span for the fragment rule', () => {
  const pre = (...lines) => ['<pre>', ...lines, '</pre>'].join('\n');
  failsOn(fragments(pre('---', 'name: <name>', 'tools: Read, Grep', '---')), 'Read, Grep', 15);
  failsOn(fragments('<pre>tools: Read, Grep</pre>'), 'Read, Grep', 12);
  failsOn(fragments(pre('---', 'name: <name>', 'You are a reviewer.', '---')), 'You are a reviewer.');
  failsOn(fragments(pre('---', 'name: <name>', '', '---')), 'an empty line');
  passes(fragments(pre('---', 'name: <name>', 'tools: <tools>', '---')));
  passes(fragments(pre('echo hi')));
  failsOn(fragments('Write <code>tools: Read, Grep</code> in the file.'), 'tools: Read, Grep', 12);
  failsOn(fragments('Write <CODE className="x">model: sonnet</CODE> in the file.'), 'model: sonnet');
  passes(fragments('Write <code>tools: <tools></code> and <code>tools</code> in the file.'));
  // a pre element and a fence together are two fragments, two pre elements too
  const one = fragment('---', 'name: <name>', '---');
  assert.deepEqual(fragments(`${one}\n\n${pre('tools: <tools>')}`), ['bonus page: 2 fenced blocks hold subagent frontmatter key lines, at most 1 fragment is allowed']);
  assert.deepEqual(fragments(`${pre('name: <name>')}\n\n${pre('tools: <tools>')}`), ['bonus page: 2 fenced blocks hold subagent frontmatter key lines, at most 1 fragment is allowed']);
  passes(fragments(`${one}\n\n${pre('echo hi')}`));
});

test('fragment: a line that is not a key of a subagent is no key line, so a block of such lines is no fragment', () => {
  passes(fragments(fragment('toolbox: Read', 'names: x', 'title: Real', 'color-scheme: red')));
  passes(fragments('```bash\necho tools: Read\n```'));
});

test('fragment: a body line, a list item, a comment, a blank line or text after the key lines fails (only --- and key lines may stand there)', () => {
  const cases = [
    ['You are a code reviewer.', 'You are a code reviewer.'],
    ['- Read', '- Read'],
    ['# a comment', '# a comment'],
    ['', 'an empty line'],
    ['<tools>', '<tools>'],
    ['----', '----'],
    ['...', '...'],
  ];
  for (const [line, shown] of cases) {
    const errors = fragments(fragment('---', 'name: <name>', line, '---'));
    assert.equal(errors.length, 1, `"${line}": ${errors.join('\n')}`);
    assert.equal(errors[0], `bonus page: line 15: the frontmatter fragment holds ${line === '' ? shown : `"${shown}"`}, it may hold only --- and key lines with placeholder values`);
  }
  const body = fragments(fragment('---', 'name: <name>', 'description: <description>', '---', '', 'You are a reviewer. Read the code and report.'));
  assert.equal(body.length, 2, body.join('\n'));
});

test('fragment: a list under a key fails, whether or not the key itself has a placeholder', () => {
  const errors = fragments(fragment('---', 'tools:', '  - Read', '  - Grep', '---'));
  assert.equal(errors.length, 3, errors.join('\n'));
  assert.match(errors[0], /the value of "tools" .* found ""/);
});

test('fragment: two fragments fail (in the same language, in another one, in a tilde fence, in an admonition), one is fine', () => {
  const one = fragment('---', 'name: <name>', '---');
  const two = `${one}\n\nText between.\n\n${one}`;
  const errors = fragments(two);
  assert.deepEqual(errors, ['bonus page: 2 fenced blocks hold subagent frontmatter key lines, at most 1 fragment is allowed']);
  for (const second of ['```text\n---\ntools: <tools>\n---\n```', '~~~\ntools: <tools>\n~~~', ':::tip[Hint]\n\n```yaml\nmodel: <model>\n```\n\n:::', '1. Step\n\n   ```yaml\n   name: <name>\n   ```', '> ```yaml\n> name: <name>\n> ```']) {
    assert.deepEqual(fragments(`${one}\n\n${second}`), ['bonus page: 2 fenced blocks hold subagent frontmatter key lines, at most 1 fragment is allowed'], second);
  }
  assert.equal(fragments(`${one}\n\n${one}\n\n${one}`)[0], 'bonus page: 3 fenced blocks hold subagent frontmatter key lines, at most 1 fragment is allowed');
  passes(fragments(`${one}\n\n\`\`\`bash\nclaude --version\n\`\`\``));
});

test('fragment: a fragment in an admonition, a list item or a tilde fence is judged like any other', () => {
  failsOn(fragments(':::tip[Hint]\n\n```yaml\n---\ntools: Read\n---\n```\n\n:::'), 'Read');
  failsOn(fragments('1. Step\n\n   ```yaml\n   tools: Read\n   ```'), 'Read');
  failsOn(fragments('~~~yaml\ntools: Read\n~~~'), 'Read');
  passes(fragments(':::tip[Hint]\n\n```yaml\n---\ntools: <tools>\n---\n```\n\n:::'));
});

test('fragment: an inline span `key: value` with a subagent key needs a placeholder value, the key alone and a placeholder pass', () => {
  failsOn(fragments('Write `tools: Read, Grep` in the file.'), 'tools: Read, Grep', 12);
  failsOn(fragments('Write `model: sonnet` in the file.'), 'model: sonnet');
  failsOn(fragments('Write `tools:` in the file.'), 'tools:');
  failsOn(fragments('Write `tools: <a>, <b>` in the file.'), 'tools: <a>, <b>');
  failsOn(fragments('Write `Tools: Read` in the file.'), 'Tools: Read');
  failsOn(fragments('- Write `description: Reviews code` in the file.'), 'description: Reviews code');
  failsOn(fragments('| Key | Value |\n| --- | --- |\n| One | `tools: Read` |'), 'tools: Read');
  failsOn(fragments('Write ``tools: Read`` in the file.'), 'tools: Read');
  passes(fragments('Write `tools: <tools>` in the file.'));
  passes(fragments('Write `description: <what it does>` in the file.'));
  passes(fragments('Write `tools` and `name` and `model` in the file.'));
  passes(fragments('Write `Read, Grep` and `toolbox: Read` and `tools-list: x` in the file.'));
  const message = fragments('Write `tools: Read, Grep` in the file.')[0];
  assert.equal(message, 'bonus page: line 12: the inline code span `tools: Read, Grep` gives "tools" a real value, it must be one placeholder in angle brackets such as <tools>');
});

// 3. No time slot

const SLOT_WITNESSES = ['10 min', '10min', '10 mins', '1 minute', '30-minute', 'an hour', 'ten minutes', 'half an hour', 'a minute', '2 hours', 'ninety minutes', 'forty-five minutes', 'ten-minute', 'one-hour', 'one hour', 'two hours', 'three hours', 'fifteen minutes', 'twenty minutes', 'thirty minutes', 'forty minutes', 'sixty minutes', 'An hour', 'Ten minutes', 'HALF AN HOUR', 'Forty-Five Minutes', '15 Minutes', '1  hour', '5 - min'];
const NO_SLOT = ['2.1.154', 'claude --version', '2.1.154 or later', 'the minimum', 'an hourly rate', 'a minuteman', 'Phase 1 of 2', 'version 10', 'min', 'hours of fun', 'extra minutes', 'Man hours', 'plan hours'];

test('time slot: every way to write a number of minutes or hours fails on the bonus page, with the text and the line in the message', () => {
  for (const witness of SLOT_WITNESSES) {
    for (const text of [`It takes ${witness} to finish.`, `Run \`x\` (${witness}).`, `\`\`\`bash\n# ${witness}\n\`\`\``, `## Part A (${witness})`]) {
      const errors = timeSlotErrors('bonus page', page(text));
      assert.equal(errors.length, 1, `${witness} in ${text}: ${errors.join('\n')}`);
      assert.match(errors[0], new RegExp(`^bonus page: line (12|13): "${witness.replace(/[-.*+?^${}()|[\]\\]/g, '\\$&')}" is a time slot, the bonus level has no fixed time$`, 'i'));
    }
  }
});

test('time slot: the two patterns are the text of the rule of the plan (G-4), typed here a second time', () => {
  assert.deepEqual(
    TIME_SLOT_PATTERNS.map((pattern) => [pattern.source, pattern.flags]),
    [
      ['\\b\\d+\\s*-?\\s*(min|mins|minute|minutes|hour|hours)\\b', 'gi'],
      ['\\b(an?|half an?|one|two|three|ten|fifteen|twenty|thirty|forty|forty-five|sixty|ninety)\\s*-?\\s*(minute|minutes|hour|hours)\\b', 'gi'],
    ],
  );
});

test('time slot: every number word of the list fails with minutes and with hours, in lower, capital and upper case', () => {
  for (const word of ['a', 'an', 'half a', 'half an', 'one', 'two', 'three', 'ten', 'fifteen', 'twenty', 'thirty', 'forty', 'forty-five', 'sixty', 'ninety']) {
    for (const unit of ['minute', 'minutes', 'hour', 'hours']) {
      for (const text of [`${word} ${unit}`, `${word[0].toUpperCase()}${word.slice(1)} ${unit}`, `${word} ${unit}`.toUpperCase()]) {
        const errors = timeSlotErrors('bonus page', `It takes ${text} to finish.`);
        assert.equal(errors.length, 1, `${text}: ${errors.join('\n')}`);
      }
    }
  }
});

test('time slot: a version number, a flag and the words around a unit that is none pass', () => {
  for (const text of NO_SLOT) passes(timeSlotErrors('bonus page', page(`Check ${text} and go on.`)));
  passes(timeSlotErrors('bonus page', page('Run `claude --version`. You need 2.1.154 or later.')));
  passes(timeSlotErrors('bonus page', page('It has no time slot of its own and no solution branch.')));
  passes(timeSlotErrors('bonus page', LIVE_PAGE));
});

test('time slot: two time words in a line are two errors, on two lines two lines', () => {
  const errors = timeSlotErrors('bonus page', 'Take 10 min or an hour.\nThen 30-minute more.\n');
  assert.deepEqual(errors, [
    'bonus page: line 1: "10 min" is a time slot, the bonus level has no fixed time',
    'bonus page: line 1: "an hour" is a time slot, the bonus level has no fixed time',
    'bonus page: line 2: "30-minute" is a time slot, the bonus level has no fixed time',
  ]);
  assert.equal(timeSlotErrors('x', 'Take 10 min.', 40)[0], 'x: line 40: "10 min" is a time slot, the bonus level has no fixed time');
});

test('time slot: the plan section of the bonus level fails on a time slot, with the line of the plan', () => {
  for (const witness of SLOT_WITNESSES) {
    const plan = PLAN.replace('Intro with', `It takes ${witness} to finish. Intro with`);
    const section = bonusPlanSection(plan);
    const errors = timeSlotErrors('plan "Bonus level" section', section.text, section.heading + 1);
    assert.equal(errors.length, 1, `${witness}: ${errors.join('\n')}`);
    assert.match(errors[0], /^plan "Bonus level" section: line 13: ".*" is a time slot, the bonus level has no fixed time$/, witness);
  }
});

test('time slot: minutes in the neighbouring plan sections are no fault of the bonus section (the section bound matters)', () => {
  const section = bonusPlanSection(PLAN);
  passes(timeSlotErrors('plan "Bonus level" section', section.text, section.heading + 1));
  assert.ok(timeSlotErrors('plan', PLAN).length >= 2, 'the fixture plan has minutes outside the section: (45 min) and (15 min)');
  const live = bonusPlanSection(LIVE_PLAN);
  passes(timeSlotErrors('plan "Bonus level" section', live.text, live.heading + 1));
  assert.ok(timeSlotErrors('plan', LIVE_PLAN).length >= 5, 'the live plan has minutes in its level headings, outside the bonus section');
});

// 4. The outline of the page (spec 16, task T4, criterion 1b)

// The H2 headings of the page in order, exact strings: the four parts, the question and the checklist. "What you bring" is a bold
// line and no heading, so the sequence has six entries.
const OUTLINE = ['Part A: Think first', 'Part B: Define two subagents', 'Part C: Coordinate them from the main session', 'Part D: Let Claude write it as a workflow', 'Before you move on', 'Bonus cleared when'];
const MARKER_BOX = ':::info[Optional bonus level]';
const GLANCE_BOX = ':::note[At a glance]';
const TIP_BOX = ':::tip[Make it yours]';
const headingsMessage = (titles) => `bonus page: the H2 headings are ${JSON.stringify(titles)}, expected exactly ${JSON.stringify(OUTLINE)}`;

// The outline rule: the marker box first, one at a glance box before the first H2, the six H2 headings in order, one Make it yours
// tip between Part D and the question, the checklist last and nothing after its list. Headings and boxes in a fenced block are text.
function outlineErrors(pageText) {
  const text = pageText.replace(/\r\n/g, '\n');
  const raw = text.split('\n');
  const prose = scanMarkdown(text).lines.map((line) => (line.fence === null ? line.text : null));
  const at = (wanted) => prose.flatMap((line, index) => (line !== null && line.trim() === wanted ? [index] : []));
  const errors = [];
  const end = raw.indexOf('---', 1);
  if (raw[0] !== '---' || end === -1) errors.push('bonus page: the page has no frontmatter');
  const first = raw.findIndex((line, index) => index > end && line.trim() !== '');
  if (first === -1 || raw[first].trim() !== MARKER_BOX) {
    errors.push(`bonus page: the ${MARKER_BOX} box must be the first element of the page, found ${first === -1 ? 'nothing' : `"${raw[first].trim()}"`}`);
  }
  const heads = prose.flatMap((line, index) => {
    const match = line === null ? null : /^## (.+)$/.exec(line.trimEnd());
    return match ? [{title: match[1], index}] : [];
  });
  const titles = heads.map((head) => head.title);
  if (JSON.stringify(titles) !== JSON.stringify(OUTLINE)) errors.push(headingsMessage(titles));
  const head = (title) => heads.find((candidate) => candidate.title === title)?.index;
  const glance = at(GLANCE_BOX);
  if (glance.length !== 1) errors.push(`bonus page: ${glance.length} ${GLANCE_BOX} boxes, expected 1`);
  else if (!(glance[0] > first && (heads.length === 0 || glance[0] < heads[0].index))) {
    errors.push(`bonus page: the ${GLANCE_BOX} box must come after the marker box and before the first H2 heading`);
  }
  const tips = at(TIP_BOX);
  if (tips.length !== 1) errors.push(`bonus page: ${tips.length} ${TIP_BOX} tips, expected 1`);
  else if (head(OUTLINE[3]) !== undefined && head(OUTLINE[4]) !== undefined && !(tips[0] > head(OUTLINE[3]) && tips[0] < head(OUTLINE[4]))) {
    errors.push(`bonus page: the ${TIP_BOX} tip must stand after the Part D heading and before "## ${OUTLINE[4]}"`);
  }
  const checklist = head(OUTLINE[5]);
  if (checklist !== undefined) {
    let from = checklist + 1;
    while (from < raw.length && raw[from].trim() === '') from++;
    const {items, next} = readBullets(raw, from);
    if (items.length === 0) errors.push(`bonus page: "## ${OUTLINE[5]}" must be followed by a task list`);
    const after = raw.findIndex((line, index) => index >= next && line.trim() !== '');
    if (after !== -1) errors.push(`bonus page: something follows the cleared-when list (line ${after + 1}: "${raw[after].trim()}"), the list ends the page`);
  }
  return errors;
}

const swapText = (text, a, b) => text.replace(a, '@@swap@@').replace(b, a).replace('@@swap@@', b);

test('outline: the live page has the marker box first, one at a glance box, the six H2 headings in order, one Make it yours tip and the list last', () => {
  passes(outlineErrors(LIVE_PAGE));
  const lines = LIVE_PAGE.split('\n');
  assert.equal(scanMarkdown(LIVE_PAGE).lines.length, lines.length, 'the scanner reads one line per line of the page');
  assert.deepEqual(lines.filter((line) => line.startsWith('## ')), OUTLINE.map((title) => `## ${title}`));
  assert.equal(lines.filter((line) => line.trim() === TIP_BOX).length, 1);
  assert.equal(lines.filter((line) => line.trim() === GLANCE_BOX).length, 1);
  assert.ok(lines.indexOf(MARKER_BOX) < lines.indexOf(GLANCE_BOX) && lines.indexOf(GLANCE_BOX) < lines.indexOf(`## ${OUTLINE[0]}`));
  assert.ok(lines.indexOf(`## ${OUTLINE[3]}`) < lines.indexOf(TIP_BOX) && lines.indexOf(TIP_BOX) < lines.indexOf(`## ${OUTLINE[4]}`));
});

test('outline: a Part D heading removed, two Parts swapped, a heading reworded or an extra heading fails by the sequence it finds', () => {
  const without = LIVE_PAGE.replace(`## ${OUTLINE[3]}\n`, '');
  assert.deepEqual(outlineErrors(without), [headingsMessage([OUTLINE[0], OUTLINE[1], OUTLINE[2], OUTLINE[4], OUTLINE[5]])]);
  const swapped = swapText(LIVE_PAGE, `## ${OUTLINE[1]}`, `## ${OUTLINE[2]}`);
  assert.deepEqual(outlineErrors(swapped), [headingsMessage([OUTLINE[0], OUTLINE[2], OUTLINE[1], OUTLINE[3], OUTLINE[4], OUTLINE[5]])]);
  const swappedLast = swapText(LIVE_PAGE, `## ${OUTLINE[4]}`, `## ${OUTLINE[5]}`);
  assert.deepEqual(outlineErrors(swappedLast).slice(0, 1), [headingsMessage([OUTLINE[0], OUTLINE[1], OUTLINE[2], OUTLINE[3], OUTLINE[5], OUTLINE[4]])]);
  const reworded = LIVE_PAGE.replace(`## ${OUTLINE[0]}`, '## Part A: Think First');
  assert.deepEqual(outlineErrors(reworded), [headingsMessage(['Part A: Think First', ...OUTLINE.slice(1)])]);
  const noPart = LIVE_PAGE.replace(`## ${OUTLINE[1]}`, 'Part B: Define two subagents');
  assert.deepEqual(outlineErrors(noPart), [headingsMessage(OUTLINE.filter((title) => title !== OUTLINE[1]))]);
  const level3 = LIVE_PAGE.replace(`## ${OUTLINE[1]}`, `### ${OUTLINE[1]}`);
  assert.deepEqual(outlineErrors(level3), [headingsMessage(OUTLINE.filter((title) => title !== OUTLINE[1]))]);
  const bring = LIVE_PAGE.replace('**What you bring from Levels 3 and 4**', '## What you bring from Levels 3 and 4');
  assert.deepEqual(outlineErrors(bring), [headingsMessage(['What you bring from Levels 3 and 4', ...OUTLINE])]);
  const extra = `${LIVE_PAGE.trimEnd()}\n`.replace(`## ${OUTLINE[4]}`, `## An extra heading\n\nText.\n\n## ${OUTLINE[4]}`);
  assert.match(outlineErrors(extra)[0], /^bonus page: the H2 headings are \[.*"An extra heading".*\], expected exactly/);
});

test('outline: a heading or a box inside a fenced block is text, a level 3 heading is no H2, and a page with carriage returns reads alike', () => {
  const fenced = LIVE_PAGE.replace(`## ${OUTLINE[4]}`, `\`\`\`md\n## Part E: Not a heading\n${TIP_BOX}\n\`\`\`\n\n## ${OUTLINE[4]}`);
  passes(outlineErrors(fenced));
  passes(outlineErrors(LIVE_PAGE.replace(`## ${OUTLINE[4]}`, `### Not an H2\n\n## ${OUTLINE[4]}`)));
  passes(outlineErrors(LIVE_PAGE.replace(/\n/g, '\r\n')));
});

test('outline: the marker box must be the first element, with the right type and title, and a page without it fails', () => {
  failsOn(outlineErrors(LIVE_PAGE.replace(MARKER_BOX, `A paragraph first.\n\n${MARKER_BOX}`)), 'A paragraph first.');
  failsOn(outlineErrors(LIVE_PAGE.replace(MARKER_BOX, ':::note[Optional bonus level]')), ':::note[Optional bonus level]');
  failsOn(outlineErrors(LIVE_PAGE.replace(MARKER_BOX, ':::info[Optional Bonus Level]')), ':::info[Optional Bonus Level]');
  failsOn(outlineErrors(LIVE_PAGE.replace(MARKER_BOX, `import X from 'x';\n\n${MARKER_BOX}`)), "import X from 'x';");
  failsOn(outlineErrors(LIVE_PAGE.replace(MARKER_BOX, `${GLANCE_BOX}\n\nA.\n\n:::\n\n${MARKER_BOX}`)).filter((error) => error.includes('first element')), GLANCE_BOX);
  const bare = outlineErrors('---\ntitle: x\n---\n');
  assert.ok(bare.some((error) => error.endsWith('found nothing')), bare.join('\n'));
});

test('outline: exactly one Make it yours tip, after Part D and before the question; exactly one at a glance box, before the first H2', () => {
  const tipBlock = `${TIP_BOX}\n\n${LIVE_PAGE.split(`${TIP_BOX}\n\n`)[1].split('\n')[0]}\n\n:::\n\n`;
  assert.ok(LIVE_PAGE.includes(tipBlock), 'the tip block is read from the page');
  assert.deepEqual(outlineErrors(LIVE_PAGE.replace(tipBlock, `${tipBlock}${tipBlock}`)), [`bonus page: 2 ${TIP_BOX} tips, expected 1`]);
  assert.deepEqual(outlineErrors(LIVE_PAGE.replace(tipBlock, '')), [`bonus page: 0 ${TIP_BOX} tips, expected 1`]);
  assert.deepEqual(outlineErrors(LIVE_PAGE.replace(TIP_BOX, ':::tip[Hint]')), [`bonus page: 0 ${TIP_BOX} tips, expected 1`]);
  const late = LIVE_PAGE.replace(tipBlock, '').replace('Which gate did you keep human, and why?\n\n', `Which gate did you keep human, and why?\n\n${tipBlock}`);
  assert.match(outlineErrors(late)[0], /tip must stand after the Part D heading and before "## Before you move on"$/);
  const early = LIVE_PAGE.replace(tipBlock, '').replace(`## ${OUTLINE[3]}\n`, `${tipBlock}## ${OUTLINE[3]}\n`);
  assert.match(outlineErrors(early)[0], /tip must stand after the Part D heading/);
  assert.deepEqual(outlineErrors(LIVE_PAGE.replace(GLANCE_BOX, ':::note[Glance]')), [`bonus page: 0 ${GLANCE_BOX} boxes, expected 1`]);
  const glanceBlock = LIVE_PAGE.slice(LIVE_PAGE.indexOf(GLANCE_BOX), LIVE_PAGE.indexOf('\n:::\n', LIVE_PAGE.indexOf(GLANCE_BOX)) + 5);
  assert.deepEqual(outlineErrors(LIVE_PAGE.replace(glanceBlock, `${glanceBlock}\n${glanceBlock}`)), [`bonus page: 2 ${GLANCE_BOX} boxes, expected 1`]);
  const lateGlance = LIVE_PAGE.replace(glanceBlock, '').replace(`## ${OUTLINE[1]}`, `${glanceBlock}\n## ${OUTLINE[1]}`);
  assert.deepEqual(outlineErrors(lateGlance), [`bonus page: the ${GLANCE_BOX} box must come after the marker box and before the first H2 heading`]);
});

test('outline: nothing follows the cleared-when list (a paragraph, a heading, a second list, an HTML comment) and the list may not be empty', () => {
  for (const tail of ['A paragraph after the list.', '## Another heading', '- [ ] one more item after a blank line', '<div></div>', ':::note\nx\n:::']) {
    const errors = outlineErrors(`${LIVE_PAGE.trimEnd()}\n\n${tail}\n`);
    assert.ok(errors.some((error) => error.startsWith('bonus page: something follows the cleared-when list (line ')), `${tail}: ${errors.join('\n')}`);
  }
  failsOn(outlineErrors(`${LIVE_PAGE.trimEnd()}\n\nA paragraph after the list.\n`), 'A paragraph after the list.');
  assert.match(outlineErrors(`${LIVE_PAGE.trimEnd()}\n\nA paragraph after the list.\n`)[0], /^bonus page: something follows the cleared-when list \(line \d+: "A paragraph after the list\."\), the list ends the page$/);
  passes(outlineErrors(`${LIVE_PAGE.trimEnd()}\n\n\n`));
  const empty = LIVE_PAGE.slice(0, LIVE_PAGE.indexOf(`## ${OUTLINE[5]}`) + `## ${OUTLINE[5]}`.length);
  assert.deepEqual(outlineErrors(`${empty}\n`), [`bonus page: "## ${OUTLINE[5]}" must be followed by a task list`]);
});

// 5. The cleared-when list against the plan (task T4, criterion 3)

const CLEARED_MARKER = '**Bonus cleared when**';
const CLEARED_HEADING = '## Bonus cleared when';

// The items of the list under the marker line of the plan section "Bonus level": the marker line stands exactly once in the whole
// plan, inside that section; an item runs over its indented continuation lines (readBullets of the checker), white space folded.
function planClearedWhen(planText) {
  const lines = planText.replace(/\r\n/g, '\n').split('\n');
  const marks = lines.flatMap((line, index) => (line.trim() === CLEARED_MARKER ? [index] : []));
  if (marks.length !== 1) return {error: `the plan has ${marks.length} "${CLEARED_MARKER}" lines, expected exactly 1`};
  const section = bonusPlanSection(planText);
  if (section.error) return {error: section.error};
  if (!(marks[0] > section.heading && marks[0] < section.to)) return {error: `"${CLEARED_MARKER}" must stand in the plan section "Bonus level"`};
  const {items} = readBullets(lines, marks[0] + 1);
  if (items.length === 0) return {error: `"${CLEARED_MARKER}" has no list items in the plan`};
  return {items: items.map(normalise)};
}

function pageClearedWhen(pageText) {
  const text = pageText.replace(/\r\n/g, '\n');
  const lines = text.split('\n');
  const prose = scanMarkdown(text).lines;
  const heads = lines.flatMap((line, index) => (prose[index].fence === null && line.trimEnd() === CLEARED_HEADING ? [index] : []));
  if (heads.length !== 1) return {error: `the page has ${heads.length} "${CLEARED_HEADING}" headings, expected exactly 1`};
  let from = heads[0] + 1;
  while (from < lines.length && lines[from].trim() === '') from++;
  const {items} = readBullets(lines, from);
  if (items.length === 0) return {error: `"${CLEARED_HEADING}" must be followed by a task list of "- [ ] " items`};
  return {items};
}

// The page list equals the plan list in order and count, item by item with the task box taken off (white space folded).
function clearedWhenErrors(pageText, planText) {
  const wanted = planClearedWhen(planText);
  if (wanted.error) return [`bonus page: ${wanted.error}`];
  const found = pageClearedWhen(pageText);
  if (found.error) return [`bonus page: ${found.error}`];
  const errors = [];
  const items = found.items.map((item, index) => {
    const match = /^\[([ xX])\]\s+(.*)$/.exec(item);
    if (!match) {
      errors.push(`bonus page: cleared-when item ${index + 1} is not a task list item ("- [ ] " followed by the text)`);
      return undefined;
    }
    if (match[1] !== ' ') errors.push(`bonus page: cleared-when item ${index + 1} is checked, a task list item must be "- [ ]"`);
    return normalise(match[2]);
  });
  if (items.length !== wanted.items.length) errors.push(`bonus page: the cleared-when list has ${items.length} items, the plan has ${wanted.items.length}`);
  for (let i = 0; i < Math.min(items.length, wanted.items.length); i++) {
    if (items[i] !== undefined && items[i] !== wanted.items[i]) {
      errors.push(`bonus page: cleared-when item ${i + 1} differs from the plan: expected "${wanted.items[i]}", found "${items[i]}"`);
    }
  }
  return errors;
}

// The lines of a text, the index after its marker line, and the index of the first blank line from there on.
const listLines = (text, marker) => {
  const lines = text.split('\n');
  const start = lines.findIndex((line) => line.trim() === marker) + 1;
  let end = start;
  while (end < lines.length && lines[end].trim() !== '') end++;
  return {lines, start, end};
};

test('cleared when: the live page items equal the live plan items in order and count, and the plan marker stands once, in the bonus section', () => {
  passes(clearedWhenErrors(LIVE_PAGE, LIVE_PLAN));
  const wanted = planClearedWhen(LIVE_PLAN);
  assert.equal(wanted.error, undefined);
  assert.equal(wanted.items.length, 4);
  assert.equal(pageClearedWhen(LIVE_PAGE).items.length, 4);
  assert.equal(LIVE_PLAN.split('\n').filter((line) => line.trim() === CLEARED_MARKER).length, 1);
  assert.ok(wanted.items.every((item) => !/\s{2}/.test(item)), 'the plan items are white space folded');
  // one plan item is wrapped over two lines, so that the continuation witness below reads a real continuation line
  const {lines, start, end} = listLines(LIVE_PLAN, CLEARED_MARKER);
  const item = lines.slice(start, end);
  assert.equal(item.length, 5, 'four items, one of them wrapped over two lines');
  assert.ok(item.some((line) => /^\s+\S/.test(line)), 'a continuation line is indented');
});

test('cleared when: two items swapped, one dropped, a page-only extra item or a plan-only extra item each fail with the message and the item', () => {
  const {lines: pageLines, start} = listLines(LIVE_PAGE, CLEARED_HEADING);
  const first = pageLines.findIndex((line, index) => index > start && line.startsWith('- [ ] '));
  const items = pageLines.slice(first, first + 4);
  assert.equal(items.length, 4);
  const withItems = (list) => [...pageLines.slice(0, first), ...list, ...pageLines.slice(first + 4)].join('\n');
  const swapped = clearedWhenErrors(withItems([items[1], items[0], items[2], items[3]]), LIVE_PLAN);
  assert.equal(swapped.length, 2, swapped.join('\n'));
  assert.match(swapped[0], /^bonus page: cleared-when item 1 differs from the plan: expected ".*", found ".*"$/);
  assert.match(swapped[1], /^bonus page: cleared-when item 2 differs from the plan/);
  for (let drop = 0; drop < 4; drop++) {
    const rest = items.filter((_, index) => index !== drop);
    const errors = clearedWhenErrors(withItems(rest), LIVE_PLAN);
    assert.ok(errors.includes('bonus page: the cleared-when list has 3 items, the plan has 4'), `drop ${drop}: ${errors.join('\n')}`);
  }
  assert.deepEqual(clearedWhenErrors(withItems([...items, '- [ ] An item only the page has.']), LIVE_PLAN), ['bonus page: the cleared-when list has 5 items, the plan has 4']);
  passes(clearedWhenErrors(withItems(items), LIVE_PLAN));
  const planExtra = LIVE_PLAN.replace(`${CLEARED_MARKER}\n`, `${CLEARED_MARKER}\n- An item only the plan has.\n`);
  assert.deepEqual(clearedWhenErrors(LIVE_PAGE, planExtra).slice(0, 1), ['bonus page: the cleared-when list has 4 items, the plan has 5']);
});

test('cleared when: a reworded continuation line of a wrapped plan item fails, a continuation line dropped fails, white space alone passes', () => {
  const continuation = '  the tools you chose and what it hands back.';
  assert.ok(LIVE_PLAN.includes(`\n${continuation}\n`), 'the plan item wraps before "the tools you chose"');
  const reworded = LIVE_PLAN.replace(continuation, '  the tools you picked and what it hands back.');
  const errors = clearedWhenErrors(LIVE_PAGE, reworded);
  assert.equal(errors.length, 1, errors.join('\n'));
  assert.match(errors[0], /^bonus page: cleared-when item 2 differs from the plan: expected ".* the tools you picked and what it hands back\.", found ".* the tools you chose and what it hands back\."$/);
  const dropped = clearedWhenErrors(LIVE_PAGE, LIVE_PLAN.replace(`\n${continuation}\n`, '\n'));
  assert.equal(dropped.length, 1, dropped.join('\n'));
  assert.match(dropped[0], /^bonus page: cleared-when item 2 differs from the plan/);
  passes(clearedWhenErrors(LIVE_PAGE, LIVE_PLAN.replace(continuation, '      the  tools you chose\tand what it hands back.')));
  passes(clearedWhenErrors(LIVE_PAGE.replace('- [ ] Two subagent files', '- [ ]   Two  subagent  files'), LIVE_PLAN));
});

test('cleared when: backticks dropped from one item, a reworded item, a checked item and a plain bullet each fail', () => {
  const ticks = '`.claude/agents/`';
  assert.ok(LIVE_PAGE.includes(`are in ${ticks},`));
  const noTicks = clearedWhenErrors(LIVE_PAGE.replace(`are in ${ticks},`, 'are in .claude/agents/,'), LIVE_PLAN);
  assert.equal(noTicks.length, 1, noTicks.join('\n'));
  assert.match(noTicks[0], /^bonus page: cleared-when item 2 differs from the plan: expected ".*`\.claude\/agents\/`.*", found ".*are in \.claude\/agents\/, .*"$/);
  const noTicksVerify = clearedWhenErrors(LIVE_PAGE.replace('and `./mvnw verify` is green', 'and ./mvnw verify is green'), LIVE_PLAN);
  assert.match(noTicksVerify[0], /cleared-when item 3 differs/);
  const reworded = clearedWhenErrors(LIVE_PAGE.replace('written down', 'noted down'), LIVE_PLAN);
  assert.match(reworded[0], /cleared-when item 1 differs/);
  assert.deepEqual(clearedWhenErrors(LIVE_PAGE.replace('- [ ] Your flow', '- [x] Your flow'), LIVE_PLAN), ['bonus page: cleared-when item 1 is checked, a task list item must be "- [ ]"']);
  assert.deepEqual(clearedWhenErrors(LIVE_PAGE.replace('- [ ] Your flow', '- Your flow'), LIVE_PLAN), ['bonus page: cleared-when item 1 is not a task list item ("- [ ] " followed by the text)']);
});

test('cleared when: the plan marker must stand exactly once, inside the bonus section, with a list; the page needs exactly one heading with a list', () => {
  const none = LIVE_PLAN.replace(`${CLEARED_MARKER}\n`, '');
  assert.deepEqual(clearedWhenErrors(LIVE_PAGE, none), [`bonus page: the plan has 0 "${CLEARED_MARKER}" lines, expected exactly 1`]);
  const twice = LIVE_PLAN.replace('## 10. Wrap-up (15 min)', `## 10. Wrap-up (15 min)\n${CLEARED_MARKER}\n- A second list.\n`);
  assert.deepEqual(clearedWhenErrors(LIVE_PAGE, twice), [`bonus page: the plan has 2 "${CLEARED_MARKER}" lines, expected exactly 1`]);
  const moved = LIVE_PLAN.replace(`${CLEARED_MARKER}\n`, '').replace('## 10. Wrap-up (15 min)', `## 10. Wrap-up (15 min)\n${CLEARED_MARKER}\n- A list in the wrong section.\n`);
  assert.deepEqual(clearedWhenErrors(LIVE_PAGE, moved), [`bonus page: "${CLEARED_MARKER}" must stand in the plan section "Bonus level"`]);
  const empty = LIVE_PLAN.replace(`${CLEARED_MARKER}\n`, `${CLEARED_MARKER}\n\n`);
  assert.match(clearedWhenErrors(LIVE_PAGE, empty)[0], /has no list items in the plan$/);
  const noSection = LIVE_PLAN.replace(`## ${BONUS_PLAN_HEADING}`, '## Another heading');
  assert.match(clearedWhenErrors(LIVE_PAGE, noSection)[0], /^bonus page: the plan has no "## Bonus level/);
  assert.deepEqual(clearedWhenErrors(LIVE_PAGE.replace(CLEARED_HEADING, '## Bonus finished when'), LIVE_PLAN), [`bonus page: the page has 0 "${CLEARED_HEADING}" headings, expected exactly 1`]);
  assert.deepEqual(clearedWhenErrors(`${LIVE_PAGE}\n${CLEARED_HEADING}\n\n- [ ] x\n`, LIVE_PLAN), [`bonus page: the page has 2 "${CLEARED_HEADING}" headings, expected exactly 1`]);
  assert.match(clearedWhenErrors(LIVE_PAGE.slice(0, LIVE_PAGE.indexOf(CLEARED_HEADING) + CLEARED_HEADING.length), LIVE_PLAN)[0], /must be followed by a task list/);
  passes(clearedWhenErrors(LIVE_PAGE.replace(/\n/g, '\r\n'), LIVE_PLAN.replace(/\n/g, '\r\n')));
});

// 6. The anti-vacuity floor (task T4, criterion 3)

// The membership check above passes when the page holds no literal at all, so an emptied page would pass it. The floor pins the
// number of inline code spans and fenced lines of the live page at this tip, each exactly: adding or removing one literal on the
// page (and in the plan section, so that membership stays green) turns this test red until the pin is changed on purpose.
const PAGE_SPAN_FLOOR = 34;
const PAGE_LINE_FLOOR = 6;

function floorErrors(pageText) {
  const {spans, lines} = codeLiterals(pageText);
  const errors = [];
  if (spans.length !== PAGE_SPAN_FLOOR) errors.push(`bonus page: ${spans.length} inline code spans, the floor pins exactly ${PAGE_SPAN_FLOOR}`);
  if (lines.length !== PAGE_LINE_FLOOR) errors.push(`bonus page: ${lines.length} fenced lines, the floor pins exactly ${PAGE_LINE_FLOOR}`);
  return errors;
}

test('floor: the live page holds exactly the pinned number of inline code spans and fenced lines', () => {
  passes(floorErrors(LIVE_PAGE));
  const {spans, lines} = codeLiterals(LIVE_PAGE);
  assert.equal(spans.length, 34);
  assert.equal(lines.length, 6);
});

test('floor: an emptied page passes the membership check and fails the floor, so the floor is what stops a vacuous pass', () => {
  const emptied = page('Plain prose only.');
  passes(bonusMembershipErrors(emptied, LIVE_PLAN));
  assert.deepEqual(floorErrors(emptied), [
    `bonus page: 0 inline code spans, the floor pins exactly ${PAGE_SPAN_FLOOR}`,
    `bonus page: 0 fenced lines, the floor pins exactly ${PAGE_LINE_FLOOR}`,
  ]);
  passes(bonusMembershipErrors(LIVE_PAGE.slice(0, LIVE_PAGE.indexOf('In Level 4 you held')), LIVE_PLAN));
  assert.equal(floorErrors(LIVE_PAGE.slice(0, LIVE_PAGE.indexOf('In Level 4 you held'))).length, 2);
});

test('floor: one span or one fenced line added or removed on the page, with the plan following so that membership stays green, turns it red', () => {
  const added = LIVE_PAGE.replace('Watch the run with `/workflows`.', 'Watch the run with `/workflows` and `claude`.');
  passes(bonusMembershipErrors(added, LIVE_PLAN));
  assert.deepEqual(floorErrors(added), [`bonus page: 35 inline code spans, the floor pins exactly ${PAGE_SPAN_FLOOR}`]);
  const removed = LIVE_PAGE.replace('Watch the run with `/workflows`.', 'Watch the run with the workflows view.');
  passes(bonusMembershipErrors(removed, LIVE_PLAN));
  assert.deepEqual(floorErrors(removed), [`bonus page: 33 inline code spans, the floor pins exactly ${PAGE_SPAN_FLOOR}`]);
  const plan = LIVE_PLAN.replace('Watch the run with `/workflows`.', 'Watch the run with `/workflows` and `extra-literal`.');
  const both = LIVE_PAGE.replace('Watch the run with `/workflows`.', 'Watch the run with `/workflows` and `extra-literal`.');
  passes(bonusMembershipErrors(both, plan));
  assert.deepEqual(floorErrors(both), [`bonus page: 35 inline code spans, the floor pins exactly ${PAGE_SPAN_FLOOR}`]);
  const line = LIVE_PAGE.replace('model: <model>\n---', 'model: <model>\n\ntools: <tools>\n---');
  assert.deepEqual(floorErrors(line), [`bonus page: 7 fenced lines, the floor pins exactly ${PAGE_LINE_FLOOR}`]);
  const fewer = LIVE_PAGE.replace('model: <model>\n', '');
  assert.deepEqual(floorErrors(fewer), [`bonus page: 5 fenced lines, the floor pins exactly ${PAGE_LINE_FLOOR}`]);
});
