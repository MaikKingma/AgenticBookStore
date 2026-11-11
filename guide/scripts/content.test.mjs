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
  for (const term of ['facilitator', 'expected findings', 'fallback kit', 'rehearsal', 'spec-pack', 'workshop-plan', 'sdd-framework-guide', '.claude/comms', 'sync moment', 'maik demos', 'live demo']) {
    for (const variant of [term, term.toUpperCase(), `${term[0].toUpperCase()}${term.slice(1)}`]) {
      fails(denyListErrors(OTHER, `first line\nsee ${variant} here\n`), new RegExp(`${OTHER}: line 2: "${term.replace(/[.\-/]/g, '\\$&')}" must not appear`));
    }
  }
});

// Spec 15 (decision D19): the workshop has no sync moments and no live demo. Each term allows any run of whitespace
// between its words, a line break included, and each row is pinned on its own.
test('deny-list: the demo terms fail across a line break, a double space and a tab, and name the term and the line', () => {
  for (const [term, words] of [['sync moment', ['sync', 'moment']], ['maik demos', ['maik', 'demos']], ['live demo', ['live', 'demo']]]) {
    for (const gap of [' ', '  ', '\n', ' \n', '\t']) {
      fails(denyListErrors(OTHER, `first line\n${words[0]}${gap}${words[1]} here\n`), new RegExp(`${OTHER}: line 2: "${term}" must not appear`));
    }
  }
  fails(denyListErrors(OTHER, 'Look up, Maik demos this now.'), /"maik demos"/);
  fails(denyListErrors(OTHER, 'Each level opens with a short live demo and closes with a debrief.'), /"live demo"/);
  fails(denyListErrors(OTHER, 'Sync moments are the live demos.'), /"sync moment"[\s\S]*"live demo"/);
});

test('deny-list: "spec NN" is the word spec and a number, in any case', () => {
  for (const text of ['spec 10', 'Spec 09', 'SPEC 1', 'spec\n12', 'spec NN']) fails(denyListErrors(OTHER, text), /"spec NN"/);
});

test('deny-list: near misses pass', () => {
  for (const text of ['.claude/rules/domain.md', 'your spec', 'spec artefacts', 'Spec Kit', 'a spec and a plan', 'the workshop plan', 'specs/10', 'facilitat', 'rehearse', 'demo data', 'the demo data', 'in sync', 'a moment', 'sync the moment', 'Maik and Tom walk around', 'olive demo']) {
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

test('deny-list FF-16: the exemption is the link target right before the id, a link to Level 4 earlier in the text is none', () => {
  const l4 = '[L4](./05-level-4-spec-driven-development.mdx)';
  fails(denyListErrors(SETUP, `${l4} and #feature-briefs`), /"feature-briefs"/);
  fails(denyListErrors(SETUP, `${l4}\n#feature-briefs`), /"feature-briefs"/);
  fails(denyListErrors(SETUP, '[L4](/level-4-spec-driven-development) then #feature-briefs'), /"feature-briefs"/);
  fails(denyListErrors(SETUP, `${l4} and [x](./02-level-1-context-engineering.mdx#feature-briefs)`), /"feature-briefs"/);
  passes(denyListErrors(SETUP, `${l4} and [x](./05-level-4-spec-driven-development.mdx#feature-briefs)`));
});

test('deny-list FF-16: the exemption is exactly the id: a longer id, another case or another term gets none', () => {
  fails(denyListErrors(LEVEL4_PAGE, '[x](#feature-briefs-extra)'), /"feature-briefs"/);
  fails(denyListErrors(LEVEL4_PAGE, '[x](#Feature-Briefs)'), /"feature-briefs"/);
  fails(denyListErrors(LEVEL4_PAGE, '[x](#workshop-plan)'), /"workshop-plan"/);
  fails(denyListErrors(LEVEL4_PAGE, '[x](#spec-pack)'), /"spec-pack"/);
  fails(denyListErrors(INDEX, '[x](./05-level-4-spec-driven-development.mdx#workshop-plan)'), /"workshop-plan"/);
});

// (e) the API key

// The forms that are a command or a setting: they fail wherever they stand, in a fenced block,
// in an inline code span or in a sentence.
const KEY_FORMS_EVERYWHERE = [
  'export ANTHROPIC_API_KEY=x',
  'ANTHROPIC_API_KEY=x claude',
  'ANTHROPIC_API_KEY = x',
  '$env:ANTHROPIC_API_KEY="x"',
  '$env: ANTHROPIC_API_KEY',
  'Remove-Item Env:ANTHROPIC_API_KEY',
  'set ANTHROPIC_API_KEY=x',
  '{"env": {"ANTHROPIC_API_KEY": "x"}}',
  "'ANTHROPIC_API_KEY' : 'x'",
];
// The export, set, setx and unset words are ordinary English in a warning ("Never set
// ANTHROPIC_API_KEY: the workshop uses its own key"), so they fail only as a command: in a
// fenced block line or an inline code span.
const KEY_COMMANDS = ['export ANTHROPIC_API_KEY', 'setx ANTHROPIC_API_KEY x', 'set ANTHROPIC_API_KEY', 'unset ANTHROPIC_API_KEY', 'export  ANTHROPIC_API_KEY'];

test('api key: an assignment, export, $env:, setx and settings form each fail on the setup page', () => {
  for (const text of KEY_FORMS_EVERYWHERE) {
    for (const wrapped of [text, `\`\`\`bash\n${text}\n\`\`\``, `Run \`${text}\` first.`, `Then ${text} in a sentence.`]) {
      fails(apiKeyErrors(SETUP, wrapped), /ANTHROPIC_API_KEY in /);
      fails(apiKeyErrors(TROUBLE, wrapped), /ANTHROPIC_API_KEY in /);
    }
  }
});

test('api key: export, set, setx and unset fail as a command (fenced block line, inline code span), also in any letter case', () => {
  for (const text of KEY_COMMANDS) {
    for (const wrapped of [`\`\`\`bash\n${text}\n\`\`\``, `Run \`${text}\` first.`, `\`\`\`powershell\n  ${text.toUpperCase()}\n\`\`\``, `Never \`${text.toLowerCase()}\`.`]) {
      for (const file of [SETUP, TROUBLE, OTHER]) fails(apiKeyErrors(file, wrapped), /ANTHROPIC_API_KEY in an export, set, setx or unset command/);
    }
  }
});

test('api key: a warning that says never to set, export or unset the token passes on the setup and troubleshooting pages', () => {
  for (const sentence of [
    'Never set ANTHROPIC_API_KEY: the workshop uses its own key.',
    'Do not export ANTHROPIC_API_KEY, and do not unset ANTHROPIC_API_KEY either.',
    'If you setx ANTHROPIC_API_KEY once, remove it again.',
    'Never\nset\nANTHROPIC_API_KEY on this machine.',
  ]) {
    passes(apiKeyErrors(SETUP, sentence));
    passes(apiKeyErrors(TROUBLE, sentence));
    fails(apiKeyErrors(OTHER, sentence), /may only be named on the setup and troubleshooting pages/);
  }
  // The same words in a sentence with code around them: the span or the line is what counts.
  passes(apiKeyErrors(SETUP, 'Never set `BOOKSTORE_AI_API_KEY` or `ANTHROPIC_API_KEY`: use the key on screen.'));
  fails(apiKeyErrors(SETUP, 'Never set this: `export ANTHROPIC_API_KEY`.'), /in an export, set, setx or unset command/);
});

test('api key: a command word in one code span and the token in the next span is no command', () => {
  passes(apiKeyErrors(SETUP, 'Never use `set` or `ANTHROPIC_API_KEY` for this.'));
  passes(apiKeyErrors(SETUP, 'Never `export` `ANTHROPIC_API_KEY`.'));
});

test('api key: a command on any line of an inline code span over three lines fails, prose after a double-backtick span stays prose', () => {
  fails(apiKeyErrors(SETUP, 'Type `a\nexport ANTHROPIC_API_KEY\nc` d.'), /line 2: ANTHROPIC_API_KEY in an export, set, setx or unset command/);
  fails(apiKeyErrors(SETUP, 'Type `a\nb\nunset ANTHROPIC_API_KEY` d.'), /line 3: ANTHROPIC_API_KEY in an export, set, setx or unset command/);
  passes(apiKeyErrors(SETUP, 'Type ``a\nb`` Never set ANTHROPIC_API_KEY: use the key on screen.'));
});

test('api key: fish set -gx, setx /M, a SetEnvironmentVariable call and a YAML key fail in code, the call in prose too', () => {
  const inCode = [
    ['set -gx ANTHROPIC_API_KEY x', /in an export, set, setx or unset command/],
    ['set -Ux ANTHROPIC_API_KEY x', /in an export, set, setx or unset command/],
    ['setx /M ANTHROPIC_API_KEY x', /in an export, set, setx or unset command/],
    ['unset -v ANTHROPIC_API_KEY', /in an export, set, setx or unset command/],
    ['export --  ANTHROPIC_API_KEY', /in an export, set, setx or unset command/],
    ['[Environment]::SetEnvironmentVariable("ANTHROPIC_API_KEY", "x", "User")', /in a SetEnvironmentVariable call/],
    ["[System.Environment]::SetEnvironmentVariable( 'ANTHROPIC_API_KEY' , $null)", /in a SetEnvironmentVariable call/],
    ['ANTHROPIC_API_KEY: x', /in a YAML key/],
    ['env:\n  ANTHROPIC_API_KEY : "x"', /in a YAML key/],
  ];
  for (const [text, form] of inCode) {
    for (const wrapped of [`\`\`\`bash\n${text}\n\`\`\``, `Run \`${text.replace('\n', ' ')}\` first.`]) {
      for (const file of [SETUP, TROUBLE]) fails(apiKeyErrors(file, wrapped), form);
    }
  }
  fails(apiKeyErrors(SETUP, 'Never call [Environment]::SetEnvironmentVariable("ANTHROPIC_API_KEY", "x") here.'), /in a SetEnvironmentVariable call/);
  // The warning keeps passing: its colon is prose, and set with a flag-like word is no command in prose.
  passes(apiKeyErrors(SETUP, 'Never set ANTHROPIC_API_KEY: use the key on screen. Do not set -gx ANTHROPIC_API_KEY either.'));
  // $env: and Env: are the PowerShell form, not a YAML key; the JSON key is the settings form.
  const env = apiKeyErrors(SETUP, '```powershell\n$env:ANTHROPIC_API_KEY\nRemove-Item Env:ANTHROPIC_API_KEY\n{"ANTHROPIC_API_KEY": "x"}\n```');
  assert.equal(env.filter((e) => /in a YAML key/.test(e)).length, 0, env.join('\n'));
  assert.equal(env.length, 3, env.join('\n'));
});

test('api key: lone backticks in other paragraphs and escaped backticks make no code of the warning between them', () => {
  passes(apiKeyErrors(SETUP, 'One ` lone backtick.\n\nNever set ANTHROPIC_API_KEY: use the key on screen.\n\nAnother ` lone backtick.'));
  passes(apiKeyErrors(SETUP, 'Never \\`set ANTHROPIC_API_KEY\\` on this machine.'));
});

test('api key: a command in a raw HTML <code> or <pre> element is code and fails, the warning in prose still passes', () => {
  for (const element of [
    '<code>export ANTHROPIC_API_KEY</code>',
    '<code>export ANTHROPIC_API_KEY=...</code>',
    '<pre>unset ANTHROPIC_API_KEY</pre>',
    '<pre>\nsetx ANTHROPIC_API_KEY x\n</pre>',
    '<code className="language-bash">set ANTHROPIC_API_KEY</code>',
    '<CODE>unset ANTHROPIC_API_KEY</CODE>',
  ]) {
    for (const file of [SETUP, TROUBLE]) fails(apiKeyErrors(file, `Run ${element} first.`), /ANTHROPIC_API_KEY in an export, set, setx or unset command/);
  }
  passes(apiKeyErrors(SETUP, 'Never set ANTHROPIC_API_KEY: <code>BOOKSTORE_AI_API_KEY</code> is the one to use.'));
  // A <code> tag written in an inline code span or in a comment opens no element, and <codex> is another tag.
  passes(apiKeyErrors(SETUP, 'The tag `<code>` starts code. Never set ANTHROPIC_API_KEY: use the key on screen. The tag `</code>` ends it.'));
  passes(apiKeyErrors(SETUP, '{/* <code> */} Never set ANTHROPIC_API_KEY: use the key on screen. {/* </code> */}'));
  passes(apiKeyErrors(SETUP, '<codex>Never set ANTHROPIC_API_KEY: use the key on screen.</codex> Use <code>BOOKSTORE_AI_API_KEY</code>.'));
  // Each element ends at its own closing tag: the warning between two elements is prose.
  passes(apiKeyErrors(SETUP, 'Use <code>BOOKSTORE_AI_API_KEY</code>. Never set ANTHROPIC_API_KEY: use <code>the key on screen</code>.'));
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

test('img: a tag written in inline code, a fenced block or a comment is text, a tag next to them or in a <pre> element still fails', () => {
  passes(imgErrors(OTHER, 'Write `<img src="/x.png">` and ``<img alt="x">`` as text.'));
  passes(imgErrors(OTHER, '```html\n<img src="/x.png">\n```'));
  passes(imgErrors(OTHER, '{/* <img src="/x.png"> */}\n<!-- <img src="/y.png"> -->'));
  fails(imgErrors(OTHER, 'Write `x`\n```html\n<b>\n```\n<img src="/x.png">'), /line 5: a raw <img> tag/);
  fails(imgErrors(OTHER, 'Write `x` <img src="/x.png">'), /line 1: a raw <img> tag/);
  fails(imgErrors(OTHER, '<pre><img src="/x.png"></pre>'), /a raw <img> tag/);
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

test('powershell: the innermost tab decides, so a bash fence in a PowerShell tab nested in a track tab is checked', () => {
  const text = '<Tabs groupId="track">\n<TabItem label="Track A: OpenSpec">\n<Tabs groupId="os">\n<TabItem label="macOS/Linux">\n```bash\nrm x\n```\n</TabItem>\n<TabItem label="Windows PowerShell">\n```bash\ndel x\n```\n</TabItem>\n</Tabs>\n```bash\nrm y\n```\n</TabItem>\n</Tabs>\n';
  const errors = psCheck(text);
  assert.equal(errors.length, 1, errors.join('\n'));
  assert.match(errors[0], /line 11: the PowerShell line "del x"/);
  // The same fence in the macOS tab, or in the track tab outside the os group, is not checked.
  passes(psCheck(text.replace('label="Windows PowerShell"', 'label="macOS/Linux"')));
});

test('powershell and tabs: a comment opener in inline code hides neither a Windows line nor a tab group (the unclosed {/* witness)', () => {
  for (const opener of ['{/*', '<!--']) {
    const intro = `Type \`${opener}\` with care.\n\n`;
    fails(contentErrors({pages: {[OTHER]: `${intro}${psTab(['Remove-Item -Recurse inventedfolder'])}`}, packTexts: PACK}).errors, /the PowerShell line "Remove-Item -Recurse inventedfolder" is not a command line/);
    fails(contentErrors({pages: {[OTHER]: `${intro}${tabs('nope', ['macOS/Linux', 'Windows PowerShell'])}`}, packTexts: PACK}).errors, /unknown tab groupId "nope"/);
  }
  // The span may run over a line break.
  const wrapped = 'Type `a\nb {/*` c.\n\n';
  fails(contentErrors({pages: {[OTHER]: `${wrapped}${psTab(['Remove-Item -Recurse inventedfolder'])}`}, packTexts: PACK}).errors, /the PowerShell line/);
  // A real comment still hides what it covers.
  passes(contentErrors({pages: {[OTHER]: `{/*\n${psTab(['Remove-Item -Recurse inventedfolder'])}*/}\n`}, packTexts: PACK}).errors);
});

test('powershell and tabs: a tag in inline code neither ends a Windows tab early nor counts as a tab group (K1, K2)', () => {
  const k2 =
    '<Tabs groupId="os">\n<TabItem value="mac" label="macOS/Linux">\n\n```bash\nls\n```\n\n</TabItem>\n<TabItem value="win" label="Windows PowerShell">\n\nClose with `</TabItem>` later.\n\n```bash\nRemove-Item -Recurse inventedfolder\n```\n\n</TabItem>\n</Tabs>\n';
  const errors = contentErrors({pages: {[OTHER]: k2}, packTexts: PACK}).errors;
  assert.equal(errors.length, 1, errors.join('\n'));
  assert.match(errors[0], /line 14: the PowerShell line "Remove-Item -Recurse inventedfolder" is not a command line/);
  // K1: a tag in a span in prose is text: no group, no unclosed tag, no missing label.
  passes(contentErrors({pages: {[OTHER]: 'Docusaurus writes `<Tabs groupId="os">` for this.\n'}, packTexts: PACK}).errors);
  // A span that opens a PowerShell tab does not make the bash fence after it a Windows block.
  passes(contentErrors({pages: {[OTHER]: 'Wrap it in `<TabItem value="w" label="Windows PowerShell">` yourself.\n\n```bash\nrm -rf x\n```\n'}, packTexts: PACK}).errors);
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

test('powershell: packCommandLines reads the spans of the shared scanner (no span across a blank line, none from an escaped backtick)', () => {
  const lines = packCommandLines(['One ` lone.\n\nTwo ` lone.\n', 'Not \\`Remove-Item x\\` code, but `Get-Command jdtls` is.\n']);
  assert.deepEqual([...lines].sort(), ['Get-Command jdtls']);
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

// Escalated round: what MDX keeps apart, or renders as text, the rules keep apart too.
const osTab = (windows) =>
  `<Tabs groupId="os">\n<TabItem value="mac" label="macOS/Linux">\n\n\`\`\`bash\nls\n\`\`\`\n\n</TabItem>\n<TabItem value="win" label="Windows PowerShell">\n\n${windows}\n\n</TabItem>\n</Tabs>\n`;
const INVENTED = '```bash\nRemove-Item -Recurse inventedfolder\n```';
const pageErrors = (text, file = OTHER) => contentErrors({pages: {[file]: text}, packTexts: PACK}).errors;
const onlyInventedAt = (errors, line) => {
  assert.equal(errors.length, 1, errors.join('\n'));
  assert.match(errors[0], new RegExp(`^content: ${OTHER}: line ${line}: the PowerShell line "Remove-Item -Recurse inventedfolder" is not a command line`));
};
const exactly = (errors, count, pattern) => {
  assert.equal(errors.length, count, errors.join('\n'));
  for (const error of errors) assert.match(error, pattern);
};

test('powershell and tabs: lone backticks in two tab items make no span over the tags between them (K5)', () => {
  const k5 = '<Tabs groupId="os">\n<TabItem value="mac" label="macOS/Linux">\nUse the ` key\n</TabItem>\n<TabItem value="win" label="Windows PowerShell">\nand the ` key.\n```bash\nRemove-Item -Recurse inventedfolder\n```\n</TabItem>\n</Tabs>\n';
  onlyInventedAt(pageErrors(k5), 8);
});

test('powershell: a span over a ``` line with a backtick in its info string, or after an escaped backslash, keeps its tag text (K7, K8)', () => {
  onlyInventedAt(pageErrors(osTab(`Close with \`</TabItem>\n\`\`\` no fence \` later.\n\n${INVENTED}`)), 15);
  onlyInventedAt(pageErrors(osTab(`Close with \\\\\`</TabItem>\` later.\n\n${INVENTED}`)), 14);
});

test('powershell: a closing tag MDX renders as text, escaped or in a string literal of an expression, ends no Windows tab (K3, K4, K4b)', () => {
  for (const text of ['Close with \\</TabItem> later.', "Close with {'</TabItem>'} later.", 'Close with {"</TabItem>"} later.']) {
    onlyInventedAt(pageErrors(osTab(`${text}\n\n${INVENTED}`)), 14);
  }
  // Two backslashes: the tag is real, the item ends there, and the fence after it is in no tab.
  passes(pageErrors(osTab(`Close with \\\\</TabItem> later.\n\n${INVENTED}`)));
});

test('powershell and tabs: a comment opener MDX renders as text, escaped or in a string of an expression, hides no Windows line', () => {
  for (const intro of ['Type \\{/* to start a comment.', "Type {'{/*'} to start a comment.", 'Type {"{/*"} to start a comment.']) {
    onlyInventedAt(pageErrors(`${intro}\n\n${osTab(INVENTED)}`), 14);
  }
});

test('powershell: a fence after a block quote or a list marker in the Windows tab is checked like any fence (RA-D)', () => {
  for (const fence of [
    '> ```powershell\n> Remove-Item -Recurse inventedfolder\n> ```',
    '- ```powershell\n  Remove-Item -Recurse inventedfolder\n  ```',
    '> ```bash\n> Remove-Item -Recurse inventedfolder\n> ```',
  ]) {
    onlyInventedAt(pageErrors(osTab(fence)), 12);
    passes(pageErrors(osTab(fence.replace('Remove-Item -Recurse inventedfolder', 'Get-Command jdtls'))));
  }
});

test('powershell: each line of a raw <pre> element in the Windows tab is checked like a fenced line (RA-E)', () => {
  onlyInventedAt(pageErrors(osTab('<pre>Remove-Item -Recurse inventedfolder</pre>')), 11);
  passes(pageErrors(osTab('<pre>Get-Command jdtls</pre>')));
  onlyInventedAt(pageErrors(osTab('<PRE className="x">\nGet-Command jdtls\nRemove-Item -Recurse inventedfolder\n</PRE>')), 13);
  // A <pre> in the macOS tab, outside any tab, in inline code or in a comment is no Windows block.
  passes(pageErrors(osTab('Get-Command jdtls').replace('```bash\nls\n```', '<pre>Remove-Item -Recurse inventedfolder</pre>')));
  passes(pageErrors('<pre>Remove-Item -Recurse inventedfolder</pre>\n'));
  passes(pageErrors(osTab('Write `<pre>Remove-Item -Recurse inventedfolder</pre>` as text.')));
  passes(pageErrors(osTab('{/* <pre>Remove-Item -Recurse inventedfolder</pre> */}')));
  // An inline <code> element is no code block, like an inline code span in prose.
  passes(pageErrors(osTab('Run <code>Remove-Item -Recurse inventedfolder</code> by hand.')));
  // A <pre> on the line of its TabItem tags belongs to that tab, and the innermost tab decides.
  const oneLine = '<Tabs groupId="os">\n<TabItem value="mac" label="macOS/Linux">x</TabItem>\n<TabItem value="win" label="Windows PowerShell"><pre>Remove-Item -Recurse inventedfolder</pre></TabItem>\n</Tabs>\n';
  onlyInventedAt(pageErrors(oneLine), 3);
  onlyInventedAt(pageErrors(`<Tabs groupId="track">\n<TabItem label="Track A: OpenSpec">\n${osTab('<pre>Remove-Item -Recurse inventedfolder</pre>')}</TabItem>\n<TabItem label="Track B: Spec Kit">\nx\n</TabItem>\n<TabItem label="Track C: Agent OS">\nx\n</TabItem>\n</Tabs>\n`), 13);
});

test('powershell: packCommandLines adds the collapsed form of a span wrapped over two lines, and a span with an inner run of spaces in both forms (PK-3, PK-4)', () => {
  const lines = packCommandLines(['Run `a\nb` and `c  d`.']);
  assert.ok(lines.has('a b'), 'the wrapped span, collapsed');
  assert.ok(lines.has('c  d'), 'the span as written, trimmed');
  assert.ok(lines.has('c d'), 'the span collapsed');
});

test('api key: the PowerShell drive path Env:\\ANTHROPIC_API_KEY is the Env: form and fails anywhere (RA-F)', () => {
  const env = /in a \$env: or Env: form/;
  exactly(apiKeyErrors(SETUP, '```bash\nSet-Item -Path Env:\\ANTHROPIC_API_KEY -Value x\n```'), 1, env);
  exactly(apiKeyErrors(TROUBLE, 'Run `Remove-Item Env:\\ANTHROPIC_API_KEY` once.'), 1, env);
  exactly(apiKeyErrors(SETUP, 'Never run Get-Item env:/ANTHROPIC_API_KEY here.'), 1, env);
});

test('api key: a list item ends a span, an escaped backslash does not escape the backtick after it, quote markers are no code (E8, E9)', () => {
  passes(apiKeyErrors(SETUP, '- Press the ` key\n- Never set ANTHROPIC_API_KEY, really `'));
  exactly(apiKeyErrors(SETUP, 'Run \\\\`export ANTHROPIC_API_KEY` now.'), 1, /line 1: ANTHROPIC_API_KEY in an export, set, setx or unset command/);
  exactly(apiKeyErrors(SETUP, '> Run `export\n> ANTHROPIC_API_KEY` now.'), 1, /line 1: ANTHROPIC_API_KEY in an export, set, setx or unset command/);
});

test('img: a real <img> between two lone backticks across a heading, a list item or table rows fails, and in one table row too (FI4 to FI7)', () => {
  const img = '<img src="https://example.com/x.png" alt="x" />';
  exactly(imgErrors(OTHER, `Press the \` key.\n## Keys ${img} and \``), 1, /line 2: a raw <img> tag/);
  exactly(imgErrors(OTHER, `- Press the \` key\n- ${img} then \``), 1, /line 2: a raw <img> tag/);
  exactly(imgErrors(OTHER, `| a | b |\n| - | - |\n| Press \` | key |\n| ${img} | then \` |`), 1, /line 4: a raw <img> tag/);
  exactly(imgErrors(OTHER, `| \` | ${img} | \` |`), 1, /line 1: a raw <img> tag/);
  // FI2 stays: an <img> in inline code, a fenced block or a comment is text.
  passes(imgErrors(OTHER, `Write \`${img}\`.\n\n\`\`\`html\n${img}\n\`\`\`\n\n{/* ${img} */}`));
  // An escaped tag or one in a string of an expression is still reported: a literal tag belongs in inline code.
  exactly(imgErrors(OTHER, `Write \\${img} and {'${img}'} as text.`), 2, /line 1: a raw <img> tag/);
});
