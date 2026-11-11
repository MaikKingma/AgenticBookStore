// The content rules of the guide pages: the deny-list of internal paths and words (d), the
// ANTHROPIC_API_KEY forms (e), raw <img> (f), the tab group and label sets (g), the
// PowerShell lines (c), the Level 0 observation card (h) and the ban on workflow script code
// (i). Pure functions on text; the file reads are in lib/content.mjs. The rules (c) to (i)
// read every page, a skeleton included, except (h), which waits for the Level 0 page to lose
// its marker.
import {HTML_CODE, codeOf, collapse, markupOf, proseOf, scanMarkdown, tabGroups} from '../lib/mdscan.mjs';
import {isSkeleton} from './coverage.mjs';

export const LEVEL0_PAGE = 'docs/01-level-0-break-the-agent.mdx';
export const LEVEL4_PAGE = 'docs/05-level-4-spec-driven-development.mdx';
const KEY_PAGES = ['docs/00-setup-check.md', 'docs/reference/troubleshooting.md'];

// The tab groups of the page grammar: group id -> the labels it has, all of them.
export const TAB_GROUPS = {
  os: ['macOS/Linux', 'Windows PowerShell'],
  jdtls: ['macOS', 'Arch Linux', 'Other Linux', 'Windows (WSL)'],
  track: ['Track A: OpenSpec', 'Track B: Spec Kit', 'Track C: Agent OS'],
};

const POWERSHELL_LANGUAGES = new Set(['powershell', 'pwsh', 'ps1', 'ps', 'cmd', 'bat']);

// 1-based line of an offset in a text.
const lineOf = (text, offset) => text.slice(0, offset).split('\n').length;

// (d) Words and paths that never belong on a participant page. The match is case-insensitive.
// The one exemption is the contractual anchor id `feature-briefs`: only as `#feature-briefs`,
// not followed by more id characters, and only where the anchor lives: anywhere on the
// Level 4 page (its heading comment, in-page links) or as the target of a link to that page.
const DENY = [
  ['facilitator', /facilitator/gi],
  ['expected findings', /expected\s+findings/gi],
  ['fallback kit', /fallback\s+kit/gi],
  ['rehearsal', /rehearsal/gi],
  ['spec-pack', /spec-pack/gi],
  ['workshop-plan', /workshop-plan/gi],
  ['sdd-framework-guide', /sdd-framework-guide/gi],
  ['spec NN', /\bspec\s+(?:\d+|NN)\b/gi],
  ['.claude/comms', /\.claude\/comms/gi],
  // Spec 15 (decision D19): no sync moments and no live demo, Maik and Tom walk around instead.
  // The words of "sync moment" may also be joined by a hyphen or an underscore, or by nothing at all.
  ['sync moment', /sync[\s_-]*moment/gi],
  ['maik demos', /maik\s+demos/gi],
  ['live demo', /\blive\s+demo/gi],
  // Spec 15 task T3 (decision D19): the Level 2 fallback is a debugging section, never a demo.
  ['watch the demo', /watch\s+the\s+demo/gi],
];
const FEATURE_BRIEFS = /feature-briefs/gi;
const LEVEL4_LINK_TARGET = /(?:05-level-4-spec-driven-development\.mdx?|\blevel-4-spec-driven-development)$/;

export function denyListErrors(file, text) {
  const errors = [];
  const add = (term, offset) => errors.push(`${file}: line ${lineOf(text, offset)}: "${term}" must not appear on a participant page (internal path or word)`);
  for (const [term, pattern] of DENY) for (const m of text.matchAll(pattern)) add(term, m.index);
  for (const m of text.matchAll(FEATURE_BRIEFS)) {
    const anchor =
      m[0] === 'feature-briefs' &&
      text[m.index - 1] === '#' &&
      !/[\w-]/.test(text[m.index + m[0].length] ?? '') &&
      (file === LEVEL4_PAGE || LEVEL4_LINK_TARGET.test(text.slice(0, m.index - 1)));
    if (!anchor) add('feature-briefs', m.index);
  }
  return errors;
}

// (e) ANTHROPIC_API_KEY never in an assignment, export or $env: form (the workshop uses
// BOOKSTORE_AI_API_KEY), and the bare token only on the setup and troubleshooting pages.
// The words export, set, setx and unset are ordinary English in a warning ("Never set
// ANTHROPIC_API_KEY: ..."), so that form (with its flags: set -gx, setx /M) and the YAML
// key form (the warning's colon) are read in code only: a fenced block line, an inline
// code span or a raw <code> or <pre> element. The other forms (an assignment, $env: or the
// Env: drive, also as the path Env:\, a SetEnvironmentVariable call, a quoted settings key)
// are no English and fail anywhere.
const KEY = 'ANTHROPIC_API_KEY';
const KEY_FORMS = [
  ['an export, set, setx or unset command', new RegExp(`\\b(?:export|set|setx|unset)(?:\\s+(?:-{1,2}|/)[A-Za-z]*)*\\s+${KEY}`, 'gi'), true],
  ['a $env: or Env: form', new RegExp(`(?:\\$env:|\\bEnv:)[\\\\/]?\\s*${KEY}`, 'gi'), false],
  ['a SetEnvironmentVariable call', new RegExp(`SetEnvironmentVariable\\s*\\(\\s*["']${KEY}["']`, 'gi'), false],
  ['an assignment', new RegExp(`${KEY}\\s*=`, 'gi'), false],
  ['a settings or JSON key', new RegExp(`["']${KEY}["']\\s*:`, 'gi'), false],
  ['a YAML key', new RegExp(`\\b${KEY}\\s*:`, 'gi'), true],
];

export function apiKeyErrors(file, text) {
  const errors = [];
  const code = codeOf(scanMarkdown(text.replace(/\r\n/g, '\n')));
  for (const [form, pattern, codeOnly] of KEY_FORMS) {
    const searched = codeOnly ? code : text;
    for (const m of searched.matchAll(pattern)) {
      errors.push(`${file}: line ${lineOf(searched, m.index)}: ${KEY} in ${form}, the workshop only ever uses BOOKSTORE_AI_API_KEY`);
    }
  }
  if (!KEY_PAGES.includes(file)) {
    for (const m of text.matchAll(new RegExp(KEY, 'gi'))) {
      errors.push(`${file}: line ${lineOf(text, m.index)}: ${KEY} may only be named on the setup and troubleshooting pages`);
    }
  }
  return errors;
}

// (f) No raw <img>: Markdown image syntax only. The tag is read in the markup: an <img>
// written in inline code, a fenced block or a comment is text, not a tag. An escaped \<img or
// one in a string of an expression is still reported: a literal tag belongs in inline code.
export function imgErrors(file, text) {
  const markup = markupOf(scanMarkdown(text.replace(/\r\n/g, '\n')));
  return [...markup.matchAll(/<img\b/gi)].map((m) => `${file}: line ${lineOf(markup, m.index)}: a raw <img> tag, use Markdown image syntax`);
}

// (g) Every Tabs element has a known groupId and exactly the labels of that group.
export function tabErrors(file, scan) {
  const {groups, errors: scanErrors} = tabGroups(scan);
  const errors = scanErrors.map((message) => `${file}: ${message}`);
  for (const group of groups) {
    const where = `${file}: line ${group.line + 1}`;
    if (!group.groupIdPresent) {
      errors.push(`${where}: <Tabs> without a groupId, the groups are ${Object.keys(TAB_GROUPS).join(', ')}`);
      continue;
    }
    const labels = TAB_GROUPS[group.groupId];
    if (group.groupId === undefined || !labels) {
      errors.push(`${where}: unknown tab groupId ${JSON.stringify(group.groupId)}, the groups are ${Object.keys(TAB_GROUPS).join(', ')}`);
      continue;
    }
    const seen = [];
    for (const item of group.items) {
      const at = `${file}: line ${item.line + 1}`;
      if (item.label === undefined) errors.push(`${at}: the <TabItem> label must be a string literal`);
      else if (!labels.includes(item.label)) errors.push(`${at}: unknown label "${item.label}" in the ${group.groupId} group, the labels are ${labels.join(', ')}`);
      else if (seen.includes(item.label)) errors.push(`${at}: the label "${item.label}" appears twice in the ${group.groupId} group`);
      else seen.push(item.label);
    }
    for (const label of labels) {
      if (!seen.includes(label)) errors.push(`${where}: the ${group.groupId} group has no "${label}" tab`);
    }
  }
  return errors;
}

// The command lines the pack states, for rule (c): every trimmed line of a fenced block
// (once as written, once with its trailing "# comment" stripped) and every inline code
// span (the scanner's spans), whitespace-collapsed and with the table escape \| undone.
// Returns a Set.
export function packCommandLines(packTexts) {
  const allowed = new Set();
  for (const text of packTexts) {
    if (typeof text !== 'string') continue;
    const scan = scanMarkdown(text);
    for (const block of scan.blocks) {
      for (const line of block.body) {
        const trimmed = line.trim();
        if (trimmed === '') continue;
        allowed.add(trimmed);
        const stripped = trimmed.replace(/\s+#.*$/, '').trim();
        if (stripped !== '') allowed.add(stripped);
      }
    }
    const prose = proseOf(scan);
    for (const [start, end] of scan.spans) {
      const span = prose.slice(start, end).replace(/\\\|/g, '|');
      allowed.add(span.trim());
      allowed.add(collapse(span));
    }
  }
  allowed.delete('');
  return allowed;
}

// (c) Every line of a Windows PowerShell code block (a fenced block inside a tab labelled
// PowerShell, or a fenced block in a PowerShell language, or a raw <pre> element inside a tab
// labelled PowerShell) equals, as a whole, a pack command line or inline code span. A
// substring of one does not count.
export function powershellErrors(file, scan, allowed) {
  const errors = [];
  const {groups} = tabGroups(scan);
  const items = groups.flatMap((group) => group.items);
  for (const block of scan.blocks) {
    const enclosing = items.filter((item) => item.line < block.open && block.close <= item.end).sort((a, b) => b.line - a.line)[0];
    const inTab = enclosing !== undefined && /powershell/i.test(enclosing.label ?? '');
    if (!inTab && !POWERSHELL_LANGUAGES.has(block.lang.toLowerCase())) continue;
    block.body.forEach((line, index) => {
      const trimmed = line.trim();
      if (trimmed !== '' && !allowed.has(trimmed)) {
        errors.push(`${file}: line ${block.open + index + 2}: the PowerShell line "${trimmed}" is not a command line or inline code span of the pack, a Windows variant needs a pack source`);
      }
    });
  }
  // A raw <pre> element (read in the markup: not in inline code, a fenced block or a comment)
  // whose innermost tab is labelled PowerShell: each line of its content, as the page has it.
  const markup = markupOf(scan);
  const prose = proseOf(scan);
  for (const m of markup.matchAll(HTML_CODE)) {
    if (m[1].toLowerCase() !== 'pre') continue;
    const first = lineOf(markup, m.index) - 1;
    const last = lineOf(markup, m.index + m[0].length - 1) - 1;
    const enclosing = items.filter((item) => item.line <= first && last <= item.end).sort((a, b) => b.line - a.line)[0];
    if (enclosing === undefined || !/powershell/i.test(enclosing.label ?? '')) continue;
    const start = m.index + m[0].indexOf('>') + 1;
    prose.slice(start, start + m[2].length).split('\n').forEach((line, index) => {
      const trimmed = line.trim();
      if (trimmed !== '' && !allowed.has(trimmed)) {
        errors.push(`${file}: line ${lineOf(prose, start) + index}: the PowerShell line "${trimmed}" is not a command line or inline code span of the pack, a Windows variant needs a pack source`);
      }
    });
  }
  return errors;
}

// (h) The Level 0 observation card: a fenced markdown block (it gets a copy button) with the
// table header | Failure mode | Evidence | Column |, at least three rows of three cells and a
// line "What went right:"; the hint (didn't know, didn't care, didn't check) and the
// instruction to save it as notes/level-0.md are on the page.
const cells = (line) => line.trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map((cell) => cell.trim());

export function observationCardErrors(file, scan) {
  const errors = [];
  const markdown = scan.blocks.filter((block) => ['markdown', 'md'].includes(block.lang.toLowerCase()));
  if (markdown.length === 0) {
    return [`${file}: the observation card must be a fenced markdown block (a rendered table has no copy button)`];
  }
  const card = markdown.find((block) => block.body.some((line) => /^\s*\|\s*Failure mode\b/i.test(line)));
  if (!card) {
    errors.push(`${file}: no fenced markdown block holds the observation card table with the header | Failure mode | Evidence | Column |`);
  } else {
    const head = card.body.findIndex((line) => /^\s*\|\s*Failure mode\b/i.test(line));
    const header = cells(card.body[head]);
    if (header.join('|') !== 'Failure mode|Evidence|Column') {
      errors.push(`${file}: the observation card header must be | Failure mode | Evidence | Column |, found "${card.body[head].trim()}"`);
    }
    const separator = card.body[head + 1];
    if (separator === undefined || !/^\s*\|(?:\s*:?-+:?\s*\|)+\s*$/.test(separator)) {
      errors.push(`${file}: the observation card needs a table separator row under the header`);
    }
    const rows = [];
    for (let i = head + 2; i < card.body.length && card.body[i].trim().startsWith('|'); i++) rows.push(card.body[i]);
    if (rows.length < 3) errors.push(`${file}: the observation card has ${rows.length} rows, it needs at least three`);
    rows.forEach((row, index) => {
      if (cells(row).length !== 3) errors.push(`${file}: observation card row ${index + 1} must have three cells (Failure mode, Evidence, Column)`);
    });
    if (!card.body.some((line) => line.trim().startsWith('What went right:'))) {
      errors.push(`${file}: the observation card has no line starting with "What went right:"`);
    }
  }
  const page = collapse(scan.lines.map((line) => line.text).join('\n'));
  if (!page.includes("(didn't know, didn't care, didn't check)")) {
    errors.push(`${file}: the observation card hint "(didn't know, didn't care, didn't check)" is missing`);
  }
  if (!page.includes('notes/level-0.md')) errors.push(`${file}: the page must say to save the card as notes/level-0.md`);
  return errors;
}

// (i) No workflow script code in the guide (spec 16, decision D-S4). The guide explains the fields and the questions of an
// agent flow and never writes the script. Two forms fail on every page: the line `export const meta` of a workflow
// script, anywhere in the page text (prose, code, a comment), and a call of agent, pipeline, parallel or phase inside
// code: an inline code span, a fenced line or a raw <code> or <pre> element (codeOf). The words in English prose, with
// a parenthesis after them ("the agents (two of them)"), are not code and pass. A call may have white space (a line
// break too) between the name and the parenthesis. The name is a whole word and lower case: `subagent(x)` and
// `Agent(agent_type)` are no calls.
const SCRIPT_META = /\bexport\s+const\s+meta/g;
const SCRIPT_CALL = /\b(agent|pipeline|parallel|phase)\s*\(/g;
const NO_SCRIPT_CODE = 'no workflow script code in the guide, describe the flow in words';

export function scriptCodeErrors(file, text) {
  const errors = [];
  const page = text.replace(/\r\n/g, '\n');
  const code = codeOf(scanMarkdown(page));
  const found = [
    ...[...page.matchAll(SCRIPT_META)].map((m) => ({line: lineOf(page, m.index), message: `"export const meta" in the page text: ${NO_SCRIPT_CODE}`})),
    ...[...code.matchAll(SCRIPT_CALL)].map((m) => ({line: lineOf(code, m.index), message: `"${m[1]}(" in code: ${NO_SCRIPT_CODE}`})),
  ];
  for (const {line, message} of found.sort((a, b) => a.line - b.line)) errors.push(`${file}: line ${line}: ${message}`);
  return errors;
}

// The guards of the optional bonus level page (spec 16, decisions D-S4 and D-S6). They compare one page with one plan
// section, so they are not part of contentErrors: bonus-page.test.mjs runs them on the live page and the live plan and
// on fixture strings, and `npm run check` runs that test. Every function reads text with the scanner of the checker
// (scanMarkdown), never a second one.

// The heading of the plan section that holds the bonus level, and the frontmatter keys of a subagent file as the field
// table of the sub-agents page of the Claude Code docs lists them (read 2026-11-11).
export const BONUS_PLAN_HEADING = 'Bonus level: orchestrate your own team (optional)';
export const SUBAGENT_KEYS = [
  'name',
  'description',
  'tools',
  'disallowedTools',
  'model',
  'permissionMode',
  'maxTurns',
  'autoCompactWindow',
  'skills',
  'mcpServers',
  'hooks',
  'memory',
  'background',
  'omitClaudeMd',
  'effort',
  'isolation',
  'color',
  'initialPrompt',
  'experimental',
];
const KEY_LINE = new RegExp(`^(${SUBAGENT_KEYS.join('|')})\\s*:(.*)$`, 'i');
const PLACEHOLDER = /^<[^>]+>$/;

// The inline code spans and the trimmed, non-empty fenced lines of a text, each with its 1-based line. A span is read
// like packCommandLines reads it: the table escape \| undone, white space collapsed. A raw <code> element of the markup
// (never one in a span, a fence or a comment) is a span, a raw <pre> element is a fenced block: its lines count as fenced
// lines, like codeOf reads both (HTML_CODE); a line break right after the opening tag and the line of the closing tag are
// no content. `blocks` holds every fenced block and every <pre> element as its trimmed lines, blank ones included, each
// with its 1-based line (the fragment rule reads them). range {from, to} keeps the 0-based lines from <= line < to (a
// section of the plan).
export function codeLiterals(text, range) {
  const scan = scanMarkdown(text);
  const prose = proseOf(scan);
  const starts = [];
  let at = 0;
  for (const line of scan.lines) {
    starts.push(at);
    at += (line.fence === null ? line.text.length : 0) + 1;
  }
  const within = (index) => range === undefined || (index >= range.from && index < range.to);
  const spans = [];
  for (const [start, end] of scan.spans) {
    const index = starts.findLastIndex((value) => value <= start);
    const span = collapse(prose.slice(start, end).replace(/\\\|/g, '|'));
    if (within(index) && span !== '') spans.push({text: span, line: index + 1});
  }
  const lines = [];
  const blocks = [];
  for (const block of scan.blocks) {
    if (!within(block.open)) continue;
    blocks.push(block.body.map((raw, i) => ({text: raw.trim(), line: block.open + i + 2})));
  }
  const markup = markupOf(scan);
  for (const m of markup.matchAll(HTML_CODE)) {
    const start = m.index + m[0].indexOf('>') + 1;
    const first = starts.findLastIndex((value) => value <= start);
    if (!within(first)) continue;
    const content = prose.slice(start, start + m[2].length);
    if (m[1].toLowerCase() === 'code') {
      const span = collapse(content.replace(/\\\|/g, '|'));
      if (span !== '') spans.push({text: span, line: first + 1});
      continue;
    }
    const rows = content.split('\n');
    let line = first + 1;
    if (rows.length > 1 && rows[0].trim() === '') {
      rows.shift();
      line++;
    }
    if (rows.length > 1 && rows[rows.length - 1].trim() === '') rows.pop();
    blocks.push(rows.map((raw, i) => ({text: raw.trim(), line: line + i})));
  }
  for (const block of blocks) for (const entry of block) if (entry.text !== '') lines.push(entry);
  return {spans, lines, blocks};
}

// The section of the plan that holds the bonus level: from its `## ` heading (outside any fenced block) to the next
// heading of level 1 or 2. Returns {heading, from, to, text} (0-based lines; from the line after the heading up to, but
// not including, to; text is the heading and the section as written) or {error}. No heading is an error, never an empty
// pass.
export function bonusPlanSection(planText) {
  const scan = scanMarkdown(planText);
  const heads = [];
  scan.lines.forEach((line, index) => {
    const match = line.fence === null ? /^(#{1,6})\s+(.*?)\s*$/.exec(line.text) : null;
    if (match) heads.push({index, level: match[1].length, title: match[2]});
  });
  const found = heads.filter((head) => head.level === 2 && head.title === BONUS_PLAN_HEADING);
  if (found.length !== 1) {
    return {error: found.length === 0 ? `the plan has no "## ${BONUS_PLAN_HEADING}" heading` : `the plan has ${found.length} "## ${BONUS_PLAN_HEADING}" headings, expected 1`};
  }
  const [{index}] = found;
  const next = heads.find((head) => head.index > index && head.level <= 2);
  const to = next ? next.index : scan.lines.length;
  return {heading: index, from: index + 1, to, text: planText.replace(/\r\n/g, '\n').split('\n').slice(index, to).join('\n')};
}

// Pack membership (D-S6): every inline code span and every trimmed fenced line of the page equals, as a whole, a span or a
// fenced line of the "Bonus level" section of the plan (not of another section, not a substring, not with a comment added
// or taken away). Returns error lines.
export function bonusMembershipErrors(pageText, planText) {
  const section = bonusPlanSection(planText);
  if (section.error) return [`bonus page: pack membership: ${section.error}`];
  const plan = codeLiterals(planText, {from: section.from, to: section.to});
  const allowed = new Set([...plan.spans, ...plan.lines].map((literal) => literal.text));
  const page = codeLiterals(pageText);
  const where = 'is not a whole inline code span or fenced line of the "Bonus level" section of the plan';
  return [
    ...page.spans.filter((literal) => !allowed.has(literal.text)).map((literal) => ({line: literal.line, message: `the inline code span \`${literal.text}\` ${where}`})),
    ...page.lines.filter((literal) => !allowed.has(literal.text)).map((literal) => ({line: literal.line, message: `the fenced line "${literal.text}" ${where}`})),
  ]
    .sort((a, b) => a.line - b.line)
    .map(({line, message}) => `bonus page: line ${line}: ${message}`);
}

// The frontmatter fragment (D-S4): at most one fenced block holds subagent frontmatter key lines; every key line of it has
// one angle-bracket placeholder as its value, and the block holds no other line than `---` and key lines (a blank line is
// another line). And an inline code span of the form `key: value` with a subagent key has a placeholder value too
// (`tools: <tools>` and the key alone, `tools`, pass). Keys are read in any letter case. Returns error lines.
export function bonusFragmentErrors(pageText) {
  const {spans, blocks} = codeLiterals(pageText);
  const errors = [];
  let fragments = 0;
  for (const body of blocks) {
    if (!body.some(({text}) => KEY_LINE.test(text))) continue;
    fragments++;
    for (const {text, line} of body) {
      const key = KEY_LINE.exec(text);
      if (key) {
        if (!PLACEHOLDER.test(key[2].trim())) {
          errors.push(`bonus page: line ${line}: the value of "${key[1]}" in the frontmatter fragment must be one placeholder in angle brackets such as <tools>, found "${key[2].trim()}"`);
        }
      } else if (text !== '---') {
        errors.push(`bonus page: line ${line}: the frontmatter fragment holds ${text === '' ? 'an empty line' : `"${text}"`}, it may hold only --- and key lines with placeholder values`);
      }
    }
  }
  if (fragments > 1) errors.push(`bonus page: ${fragments} fenced blocks hold subagent frontmatter key lines, at most 1 fragment is allowed`);
  for (const {text, line} of spans) {
    const key = KEY_LINE.exec(text);
    if (key && !PLACEHOLDER.test(key[2].trim())) {
      errors.push(`bonus page: line ${line}: the inline code span \`${text}\` gives "${key[1]}" a real value, it must be one placeholder in angle brackets such as <tools>`);
    }
  }
  return errors;
}

// No time slot (G-4): the bonus level has no fixed time. A number with a unit of time ("10 min", "10min", "30-minute") and
// a number word or an article with one ("an hour", "ten minutes", "half an hour") fail. A version number or a flag does not.
export const TIME_SLOT_PATTERNS = [
  /\b\d+\s*-?\s*(min|mins|minute|minutes|hour|hours)\b/gi,
  /\b(an?|half an?|one|two|three|ten|fifteen|twenty|thirty|forty|forty-five|sixty|ninety)\s*-?\s*(minute|minutes|hour|hours)\b/gi,
];

// label names the text in the message; firstLine is the 1-based line of its first line. Returns error lines.
export function timeSlotErrors(label, text, firstLine = 1) {
  const errors = [];
  text.replace(/\r\n/g, '\n').split('\n').forEach((line, index) => {
    for (const pattern of TIME_SLOT_PATTERNS) {
      for (const match of line.matchAll(pattern)) {
        errors.push(`${label}: line ${firstLine + index}: "${match[0]}" is a time slot, the bonus level has no fixed time`);
      }
    }
  });
  return errors;
}

// pages: docs path -> text of every .md and .mdx page. packTexts: the pack files for rule
// (c). Returns {errors, summary}; errors are "content: ..." lines.
export function contentErrors({pages, packTexts}) {
  const errors = [];
  const allowed = packCommandLines(packTexts);
  const files = Object.keys(pages).sort();
  for (const file of files) {
    const text = pages[file].replace(/\r\n/g, '\n');
    const scan = scanMarkdown(text);
    const found = [
      ...denyListErrors(file, text),
      ...apiKeyErrors(file, text),
      ...imgErrors(file, text),
      ...scriptCodeErrors(file, text),
      ...tabErrors(file, scan),
      ...powershellErrors(file, scan, allowed),
      ...(file === LEVEL0_PAGE && !isSkeleton(text) ? observationCardErrors(file, scan) : []),
    ];
    errors.push(...found.map((message) => `content: ${message}`));
  }
  return {errors, summary: `${files.length} pages, deny-list, API key forms, img, tab sets, PowerShell lines, observation card, script code ban`};
}
