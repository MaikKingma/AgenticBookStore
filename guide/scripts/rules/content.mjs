// The content rules of the guide pages: the deny-list of internal paths and words (d), the
// ANTHROPIC_API_KEY forms (e), raw <img> (f), the tab group and label sets (g), the
// PowerShell lines (c) and the Level 0 observation card (h). Pure functions on text; the
// file reads are in lib/content.mjs. The rules (c) to (h) read every page, a skeleton
// included, except (h), which waits for the Level 0 page to lose its marker.
import {codeOf, collapse, scanMarkdown, tabGroups} from '../lib/mdscan.mjs';
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
// ANTHROPIC_API_KEY: ..."), so that form is read in code only (a fenced block line or an
// inline code span); the other forms (an assignment, $env:, a settings key) fail anywhere.
const KEY = 'ANTHROPIC_API_KEY';
const KEY_FORMS = [
  ['an export, set, setx or unset command', new RegExp(`\\b(?:export|set|setx|unset)\\s+${KEY}`, 'gi'), true],
  ['a $env: or Env: form', new RegExp(`(?:\\$env:|\\bEnv:)\\s*${KEY}`, 'gi'), false],
  ['an assignment', new RegExp(`${KEY}\\s*=`, 'gi'), false],
  ['a settings or JSON key', new RegExp(`["']${KEY}["']\\s*:`, 'gi'), false],
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

// (f) No raw <img>: Markdown image syntax only.
export function imgErrors(file, text) {
  return [...text.matchAll(/<img\b/gi)].map((m) => `${file}: line ${lineOf(text, m.index)}: a raw <img> tag, use Markdown image syntax`);
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
// span, whitespace-collapsed and with the table escape \| undone. Returns a Set.
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
    const prose = scan.lines.map((line) => (line.fence === null ? line.text : '')).join('\n');
    for (const m of prose.matchAll(/(?<!`)(`+)(?!`)([\s\S]+?)(?<!`)\1(?!`)/g)) {
      const span = m[2].replace(/\\\|/g, '|');
      allowed.add(span.trim());
      allowed.add(collapse(span));
    }
  }
  allowed.delete('');
  return allowed;
}

// (c) Every line of a Windows PowerShell code block (a fenced block inside a tab labelled
// PowerShell, or a fenced block in a PowerShell language) equals, as a whole, a pack
// command line or inline code span. A substring of one does not count.
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
      ...tabErrors(file, scan),
      ...powershellErrors(file, scan, allowed),
      ...(file === LEVEL0_PAGE && !isSkeleton(text) ? observationCardErrors(file, scan) : []),
    ];
    errors.push(...found.map((message) => `content: ${message}`));
  }
  return {errors, summary: `${files.length} pages, deny-list, API key forms, img, tab sets, PowerShell lines, observation card`};
}
