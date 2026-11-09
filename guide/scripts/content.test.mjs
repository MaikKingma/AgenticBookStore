import {test} from 'node:test';
import assert from 'node:assert/strict';
import {scanMarkdown} from './lib/mdscan.mjs';
import {
  LEVEL0_PAGE,
  LEVEL4_PAGE,
  TAB_GROUPS,
  apiKeyErrors,
  contentErrors,
  denyListErrors,
  imgErrors,
  observationCardErrors,
  packCommandLines,
  powershellErrors,
  tabErrors,
} from './rules/content.mjs';
import {SKELETON_MARKER} from './rules/structure.mjs';

const OTHER = 'docs/02-level-1-context-engineering.mdx';
const SETUP = 'docs/00-setup-check.md';
const TROUBLE = 'docs/reference/troubleshooting.md';
const INDEX = 'docs/index.md';

const passes = (errors) => assert.deepEqual(errors, []);
const fails = (errors, pattern) => {
  assert.ok(errors.length > 0, `expected an error matching ${pattern}`);
  assert.match(errors.join('\n'), pattern);
};

// (d) the deny-list

test('deny-list: each term fails on a page, in any letter case, and the message names the term and the line', () => {
  for (const term of ['facilitator', 'expected findings', 'fallback kit', 'rehearsal', 'spec-pack', 'workshop-plan', 'sdd-framework-guide', '.claude/comms']) {
    for (const variant of [term, term.toUpperCase(), `${term[0].toUpperCase()}${term.slice(1)}`]) {
      fails(denyListErrors(OTHER, `first line\nsee ${variant} here\n`), new RegExp(`${OTHER}: line 2: "${term.replace(/[.\-/]/g, '\\$&')}" must not appear`));
    }
  }
});

test('deny-list: "spec NN" is the word spec and a number, in any case', () => {
  for (const text of ['spec 10', 'Spec 09', 'SPEC 1', 'spec\n12', 'spec NN']) fails(denyListErrors(OTHER, text), /"spec NN"/);
});

test('deny-list: near misses pass', () => {
  for (const text of ['.claude/rules/domain.md', 'your spec', 'spec artefacts', 'Spec Kit', 'a spec and a plan', 'the workshop plan', 'specs/10', 'facilitat', 'rehearse']) {
    passes(denyListErrors(OTHER, text));
  }
});

test('deny-list FF-16: the anchor feature-briefs passes in its three forms and nowhere else', () => {
  passes(denyListErrors(LEVEL4_PAGE, '## Feature briefs {/* #feature-briefs */}\n'));
  passes(denyListErrors(INDEX, 'See [the briefs](./05-level-4-spec-driven-development.mdx#feature-briefs).\n'));
  passes(denyListErrors(INDEX, 'See [the briefs](./05-level-4-spec-driven-development.md#feature-briefs).\n'));
  passes(denyListErrors(INDEX, 'See [the briefs](/level-4-spec-driven-development#feature-briefs).\n'));
  passes(denyListErrors(LEVEL4_PAGE, 'Jump to [the briefs](#feature-briefs).\n'));
});

test('deny-list FF-16: feature-briefs as a file name, as prose, or on another page fails', () => {
  fails(denyListErrors(OTHER, 'level-4-feature-briefs.md'), /"feature-briefs"/);
  fails(denyListErrors(OTHER, 'feature-briefs.md'), /"feature-briefs"/);
  fails(denyListErrors(OTHER, 'Pick one of the feature-briefs.'), /"feature-briefs"/);
  fails(denyListErrors(LEVEL4_PAGE, 'Pick one of the feature-briefs.'), /"feature-briefs"/);
  fails(denyListErrors(OTHER, 'Jump to #feature-briefs.'), /"feature-briefs"/, 'the id on a page that does not own it is no exemption');
  fails(denyListErrors(INDEX, '[x](./level-4-feature-briefs.md#feature-briefs)'), /"feature-briefs"/);
  fails(denyListErrors(INDEX, '[x](./02-level-1-context-engineering.mdx#feature-briefs)'), /"feature-briefs"/);
});

test('deny-list FF-16: the exemption is exactly the id: a longer id, another case or another term gets none', () => {
  fails(denyListErrors(LEVEL4_PAGE, '[x](#feature-briefs-extra)'), /"feature-briefs"/);
  fails(denyListErrors(LEVEL4_PAGE, '[x](#Feature-Briefs)'), /"feature-briefs"/);
  fails(denyListErrors(LEVEL4_PAGE, '[x](#workshop-plan)'), /"workshop-plan"/);
  fails(denyListErrors(LEVEL4_PAGE, '[x](#spec-pack)'), /"spec-pack"/);
  fails(denyListErrors(INDEX, '[x](./05-level-4-spec-driven-development.mdx#workshop-plan)'), /"workshop-plan"/);
});

// (e) the API key

test('api key: an assignment, export, $env:, setx and settings form each fail on the setup page', () => {
  for (const text of [
    'export ANTHROPIC_API_KEY=x',
    'ANTHROPIC_API_KEY=x claude',
    'ANTHROPIC_API_KEY = x',
    '$env:ANTHROPIC_API_KEY="x"',
    '$env: ANTHROPIC_API_KEY',
    'Remove-Item Env:ANTHROPIC_API_KEY',
    'setx ANTHROPIC_API_KEY x',
    'set ANTHROPIC_API_KEY=x',
    'unset ANTHROPIC_API_KEY',
    '{"env": {"ANTHROPIC_API_KEY": "x"}}',
    "'ANTHROPIC_API_KEY' : 'x'",
  ]) {
    fails(apiKeyErrors(SETUP, text), /ANTHROPIC_API_KEY in /);
    fails(apiKeyErrors(TROUBLE, text), /ANTHROPIC_API_KEY in /);
  }
});

test('api key: the bare token passes on the setup and troubleshooting pages and fails on every other page', () => {
  const warning = 'Never export your own key. If `ANTHROPIC_API_KEY` is set, Claude Code uses it.';
  passes(apiKeyErrors(SETUP, warning));
  passes(apiKeyErrors(TROUBLE, warning));
  for (const file of [OTHER, 'docs/03-level-2-give-your-agent-eyes.mdx', INDEX, 'docs/06-wrap-up.md', 'docs/reference/cheat-sheet.md']) {
    fails(apiKeyErrors(file, warning), /may only be named on the setup and troubleshooting pages/);
  }
});

test('api key: BOOKSTORE_AI_API_KEY, the anchor id and the lower-case id are not the token', () => {
  passes(apiKeyErrors(OTHER, 'export BOOKSTORE_AI_API_KEY=<key on screen>'));
  passes(apiKeyErrors(OTHER, '[the pitfall](../reference/troubleshooting.md#anthropic-api-key)'));
  passes(apiKeyErrors(TROUBLE, '## The pitfall {/* #anthropic-api-key */}'));
});

// (f) img

test('img: <img and <IMG fail, Markdown image syntax and other tags pass', () => {
  fails(imgErrors(OTHER, 'a\n<img src="/x.png">'), /line 2: a raw <img> tag/);
  fails(imgErrors(OTHER, '<IMG src="/x.png" />'), /a raw <img> tag/);
  fails(imgErrors(OTHER, '<img\nsrc="/x.png">'), /a raw <img> tag/);
  passes(imgErrors(OTHER, '![alt text](/img/x.png)'));
  passes(imgErrors(OTHER, '<imgfoo> and <Tabs groupId="os">'));
});

// (g) tabs

const tabs = (groupId, labels) =>
  `<Tabs${groupId === null ? '' : ` groupId="${groupId}"`}>\n${labels.map((label) => `  <TabItem value="v" label="${label}">\n  x\n  </TabItem>\n`).join('')}</Tabs>\n`;
const tabCheck = (text) => tabErrors(OTHER, scanMarkdown(text));

test('tabs: the three groups with all their labels pass, in any order', () => {
  for (const [groupId, labels] of Object.entries(TAB_GROUPS)) {
    passes(tabCheck(tabs(groupId, labels)));
    passes(tabCheck(tabs(groupId, [...labels].reverse())));
  }
});

test('tabs: the label sets are exactly the page grammar', () => {
  assert.deepEqual(TAB_GROUPS, {
    os: ['macOS/Linux', 'Windows PowerShell'],
    jdtls: ['macOS', 'Arch Linux', 'Other Linux', 'Windows (WSL)'],
    track: ['Track A: OpenSpec', 'Track B: Spec Kit', 'Track C: Agent OS'],
  });
});

test('tabs: an unknown label, an unknown group id, no group id and a missing label each fail', () => {
  fails(tabCheck(tabs('os', ['macOS/Linux', 'PowerShell'])), /unknown label "PowerShell" in the os group/);
  fails(tabCheck(tabs('os', ['macOS/Linux', 'PowerShell'])), /the os group has no "Windows PowerShell" tab/);
  fails(tabCheck(tabs('shell', ['macOS/Linux', 'Windows PowerShell'])), /unknown tab groupId "shell"/);
  fails(tabCheck(tabs(null, ['macOS/Linux', 'Windows PowerShell'])), /<Tabs> without a groupId/);
  fails(tabCheck(tabs('os', ['macOS/Linux'])), /the os group has no "Windows PowerShell" tab/);
  fails(tabCheck(tabs('track', ['Track A: OpenSpec', 'Track B: Spec Kit'])), /the track group has no "Track C: Agent OS" tab/);
  fails(tabCheck(tabs('jdtls', ['macOS', 'Arch Linux', 'Other Linux'])), /the jdtls group has no "Windows \(WSL\)" tab/);
});

test('tabs: a label of another group, a repeated label, a non-literal label and a non-literal group id fail', () => {
  fails(tabCheck(tabs('os', ['macOS/Linux', 'Windows PowerShell', 'Arch Linux'])), /unknown label "Arch Linux" in the os group/);
  fails(tabCheck(tabs('os', ['macOS/Linux', 'Windows PowerShell', 'macOS/Linux'])), /"macOS\/Linux" appears twice/);
  fails(tabCheck('<Tabs groupId="os">\n<TabItem value="a" label={name}>\nx\n</TabItem>\n</Tabs>\n'), /label must be a string literal/);
  fails(tabCheck('<Tabs groupId={id}>\n</Tabs>\n'), /unknown tab groupId undefined/);
});

test('tabs: unbalanced tags are reported with the file', () => {
  fails(tabCheck('<Tabs groupId="os">\n'), new RegExp(`${OTHER}: line 1: this <Tabs> is never closed`));
});

// (c) PowerShell lines

const PACK = [
  '## Setup\n\n```bash\nclaude --version            # Claude Code installed\nexport BOOKSTORE_AI_API_KEY=<key on screen>     # macOS / Linux / WSL\n$env:BOOKSTORE_AI_API_KEY="<key on screen>"     # Windows PowerShell\n```\n\nCheck with `which jdtls` (PowerShell: `Get-Command jdtls`). Install: `irm https://x.test/install.ps1 \\| iex`.\n\nWrapped `Get-Command\n  jdtls`.\n',
];
const allowed = packCommandLines(PACK);
const psTab = (lines) => `<Tabs groupId="os">\n  <TabItem value="mac" label="macOS/Linux">\n  \`\`\`bash\n  which jdtls\n  \`\`\`\n  </TabItem>\n  <TabItem value="win" label="Windows PowerShell">\n  \`\`\`powershell\n${lines.map((l) => `  ${l}`).join('\n')}\n  \`\`\`\n  </TabItem>\n</Tabs>\n`;
const psCheck = (text) => powershellErrors(OTHER, scanMarkdown(text), allowed);

test('powershell: a pack fenced line with its comment stripped and a pack inline code span pass', () => {
  passes(psCheck(psTab(['$env:BOOKSTORE_AI_API_KEY="<key on screen>"'])));
  passes(psCheck(psTab(['Get-Command jdtls'])));
  passes(psCheck(psTab(['irm https://x.test/install.ps1 | iex'])));
  passes(psCheck(psTab(['  Get-Command jdtls  ', '', 'Get-Command jdtls'])), 'indent and blank lines');
});

test('powershell: a command that is in no pack line fails (card witness Remove-Item -Recurse foo)', () => {
  fails(psCheck(psTab(['Remove-Item -Recurse foo'])), new RegExp(`${OTHER}: line \\d+: the PowerShell line "Remove-Item -Recurse foo" is not a command line`));
});

test('powershell: a pack line with something appended fails, a substring of a pack line fails (no substring matching)', () => {
  fails(psCheck(psTab(['Get-Command jdtls; Remove-Item -Recurse x'])), /not a command line/);
  fails(psCheck(psTab(['Get-Command jdtls | Out-File x'])), /not a command line/);
  fails(psCheck(psTab(['$env:BOOKSTORE_AI_API_KEY="<key on screen>"; Remove-Item -Recurse x'])), /not a command line/);
  fails(psCheck(psTab(['Get-Command'])), /not a command line/);
  fails(psCheck(psTab(['jdtls'])), /not a command line/);
  fails(psCheck(psTab(['claude --version --force'])), /not a command line/);
});

test('powershell: only the failing lines are reported, with their line number in the page', () => {
  const errors = psCheck(psTab(['Get-Command jdtls', 'Remove-Item x', 'Get-Command jdtls', 'del y']));
  assert.equal(errors.length, 2, errors.join('\n'));
  assert.match(errors[0], /line 10: the PowerShell line "Remove-Item x"/);
  assert.match(errors[1], /line 12: the PowerShell line "del y"/);
});

test('powershell: the tab is found by its label in any case and spelling with PowerShell, and a bash tab is not checked', () => {
  const tab = (label) => `<Tabs groupId="os">\n<TabItem value="w" label="${label}">\n\`\`\`text\nRemove-Item x\n\`\`\`\n</TabItem>\n</Tabs>\n`;
  fails(psCheck(tab('PowerShell')), /not a command line/);
  fails(psCheck(tab('Windows PowerShell')), /not a command line/);
  fails(psCheck(tab('windows powershell')), /not a command line/);
  passes(psCheck(tab('macOS/Linux')));
});

test('powershell: a fenced block in a PowerShell language is checked outside a tab as well, other languages are not', () => {
  fails(psCheck('```powershell\nRemove-Item x\n```\n'), /not a command line/);
  fails(psCheck('```pwsh\nRemove-Item x\n```\n'), /not a command line/);
  fails(psCheck('```ps1\nRemove-Item x\n```\n'), /not a command line/);
  passes(psCheck('```bash\nrm -rf x\n```\n'));
  passes(psCheck('Run `Remove-Item x` in prose.\n'));
});

test('powershell: a block of a PowerShell tab nested in another group is attributed to the right tab', () => {
  const text = '<Tabs groupId="track">\n<TabItem label="Track A: OpenSpec">\n<Tabs groupId="os">\n<TabItem label="macOS/Linux">\n```bash\nrm x\n```\n</TabItem>\n<TabItem label="Windows PowerShell">\n```powershell\ndel x\n```\n</TabItem>\n</Tabs>\n```bash\nrm y\n```\n</TabItem>\n</Tabs>\n';
  const errors = psCheck(text);
  assert.equal(errors.length, 1, errors.join('\n'));
  assert.match(errors[0], /"del x"/);
});

test('powershell: packCommandLines reads fenced lines, stripped comments, inline spans, escaped pipes and wrapped spans', () => {
  assert.ok(allowed.has('claude --version'));
  assert.ok(allowed.has('claude --version            # Claude Code installed'));
  assert.ok(allowed.has('Get-Command jdtls'));
  assert.ok(allowed.has('irm https://x.test/install.ps1 | iex'));
  assert.ok(!allowed.has('Remove-Item x'));
  assert.ok(!allowed.has(''));
  assert.ok(!allowed.has('Get-Command'), 'a word of a line is not a line');
});

// (h) the Level 0 observation card

const CARD = [
  '| Failure mode | Evidence | Column |',
  '|---|---|---|',
  '| | | |',
  '| | | |',
  '| | | |',
  '',
  'What went right:',
].join('\n');
const page0 = (card = CARD, extra = "Pick a column (didn't know, didn't care, didn't check). Save it as `notes/level-0.md`.") =>
  `Intro.\n\n${extra}\n\n\`\`\`markdown\n${card}\n\`\`\`\n`;
const cardCheck = (text) => observationCardErrors(LEVEL0_PAGE, scanMarkdown(text));

test('card: the fenced markdown block with the header, three rows, the right line, the hint and the notes file passes', () => {
  passes(cardCheck(page0()));
  passes(cardCheck(page0(CARD.replace('| | | |\n', '| a | b | c |\n'))));
});

test('card: two rows fail, four rows pass', () => {
  fails(cardCheck(page0(CARD.replace('| | | |\n', ''))), /has 2 rows, it needs at least three/);
  passes(cardCheck(page0(CARD.replace('|---|---|---|\n', '|---|---|---|\n| | | |\n'))));
});

test('card: a rendered table without the fenced markdown block fails', () => {
  const rendered = `${CARD.replace(/\n\nWhat went right:/, '')}\n\n(didn't know, didn't care, didn't check) notes/level-0.md\n\nWhat went right:\n`;
  fails(cardCheck(rendered), /must be a fenced markdown block/);
  fails(cardCheck(`\`\`\`bash\n${CARD}\n\`\`\`\n(didn't know, didn't care, didn't check) notes/level-0.md`), /must be a fenced markdown block/);
});

test('card: a wrong header, a missing separator, a row with the wrong cell count and a missing last line each fail', () => {
  fails(cardCheck(page0(CARD.replace('Evidence', 'Proof'))), /header must be \| Failure mode \| Evidence \| Column \|/);
  fails(cardCheck(page0(CARD.replace('|---|---|---|\n', ''))), /separator row/);
  fails(cardCheck(page0(CARD.replace('| | | |\n', '| | |\n'))), /row 1 must have three cells/);
  fails(cardCheck(page0(CARD.replace('What went right:', 'What worked:'))), /"What went right:"/);
});

test('card: a missing hint or a missing notes/level-0.md instruction fails', () => {
  fails(cardCheck(page0(CARD, 'Save it as `notes/level-0.md`.')), /hint/);
  fails(cardCheck(page0(CARD, "(didn't know, didn't care, didn't check)")), /notes\/level-0\.md/);
  fails(cardCheck(page0(CARD, "{/* (didn't know, didn't care, didn't check) notes/level-0.md */}")), /hint/);
});

test('card: a fenced markdown block without the table says so', () => {
  fails(cardCheck('```markdown\nhello\n```\n'), /no fenced markdown block holds the observation card table/);
});

// the aggregate

test('contentErrors: one error list over all pages with the content prefix, a good page has none', () => {
  const good = {[OTHER]: '---\ntitle: x\n---\n\nBody.\n', [INDEX]: '---\ntitle: y\n---\n\nBody.\n'};
  const clean = contentErrors({pages: good, packTexts: PACK});
  assert.deepEqual(clean.errors, []);
  assert.match(clean.summary, /^2 pages/);
  const bad = contentErrors({pages: {...good, [OTHER]: 'The facilitator says so.\n<img src="x">\n'}, packTexts: PACK});
  assert.equal(bad.errors.length, 2);
  assert.ok(bad.errors.every((e) => e.startsWith(`content: ${OTHER}: line `)), bad.errors.join('\n'));
});

test('contentErrors: the card rule waits for the marker, every other rule applies to a skeleton too', () => {
  const skeleton = `---\ntitle: Level 0\n---\n\n${SKELETON_MARKER}\n`;
  passes(contentErrors({pages: {[LEVEL0_PAGE]: skeleton}, packTexts: PACK}).errors);
  fails(contentErrors({pages: {[LEVEL0_PAGE]: skeleton.replace(SKELETON_MARKER, `${SKELETON_MARKER}\nfacilitator`)}, packTexts: PACK}).errors, /"facilitator"/);
  fails(contentErrors({pages: {[LEVEL0_PAGE]: skeleton.replace(SKELETON_MARKER, '')}, packTexts: PACK}).errors, /observation card/);
  fails(contentErrors({pages: {[OTHER]: `${skeleton}${psTab(['Remove-Item x'])}`}, packTexts: PACK}).errors, /PowerShell line/);
  fails(contentErrors({pages: {[OTHER]: `${skeleton}${tabs('os', ['macOS/Linux'])}`}, packTexts: PACK}).errors, /the os group has no/);
});
