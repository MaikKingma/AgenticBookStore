import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {dirname, join} from 'node:path';
import {fileURLToPath} from 'node:url';
import {scanMarkdown} from './lib/mdscan.mjs';
import {BONUS_PLAN_HEADING, SUBAGENT_KEYS, TIME_SLOT_PATTERNS, bonusFragmentErrors, bonusMembershipErrors, bonusPlanSection, codeLiterals, timeSlotErrors} from './rules/content.mjs';

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
