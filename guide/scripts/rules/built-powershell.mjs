import {elementTree, lineOf, textContent} from '../lib/html.mjs';

// Rule (c) on the built pages: every line of every pre or code element inside a tab panel
// whose tab label contains "PowerShell" (any case), and every line of a PowerShell-language
// code block (pre class language-powershell, -pwsh, -ps1, -ps, -cmd or -bat, any case) inside
// or outside any tab panel, is a pack command line. The source rule (c) in rules/content.mjs
// reads the MDX with a scanner that can lose track of a tab; the built HTML is what the
// participant sees, so this rule reads that, hidden panels included.
// A tabs container is an element with the class tabs-container (the Docusaurus Tabs
// component); its n-th [role=tabpanel] belongs to its n-th [role=tab], counting only the tabs
// and panels whose nearest tabs container it is. Tab labels are compared as they read (no soft
// hyphens, zero-width characters or leftover character references). A line inside any
// PowerShell panel counts, also when a nested panel around it has another label. A line is the
// text of one .token-line (a highlighted code block), one newline-split line of the text of a
// pre outside its token lines, or the text of an inline code element; character references
// decoded, trimmed, empty lines skipped. A panel whose tab cannot be told is an error (it could
// be a PowerShell one).

const classes = (node) => (node.attrs.get('class') ?? '').split(/\s+/);
const role = (node) => node.attrs.get('role');

function elements(node, out = []) {
  for (const child of node.children) {
    if ('text' in child) continue;
    out.push(child);
    elements(child, out);
  }
  return out;
}

function nearest(node, test) {
  for (let up = node.parent; up; up = up.parent) if (test(up)) return up;
  return null;
}

const isContainer = (node) => classes(node).includes('tabs-container');

// A tab label as it reads: without soft hyphens and zero-width characters (U+00AD, U+200B to
// U+200D, U+2060, U+FEFF), and without the character references the tree leaves undecoded
// (such as &shy;), so "Power&shy;Shell" is PowerShell. The characters are built from their
// code points so this file stays ASCII.
const INVISIBLE = new RegExp(`[${[0xad, 0x200b, 0x200c, 0x200d, 0x2060, 0xfeff].map((code) => String.fromCharCode(code)).join('')}]`, 'g');
const visibleLabel = (text) => text.replace(/&#?[A-Za-z0-9]+;/g, '').replace(INVISIBLE, '');

// Returns {labels, problems}: labels maps a panel node to its tab label; problems lists, as
// [node, message], the panels whose tab cannot be told (fail-closed: such a panel could be a
// PowerShell one). Docusaurus writes one tab per item, and one panel per item except with
// <Tabs lazy>, which renders only the selected panel: then the panel's tab cannot be told.
function panelLabels(all) {
  const groups = new Map();
  for (const node of all) {
    const kind = role(node);
    if (kind !== 'tab' && kind !== 'tabpanel') continue;
    const container = nearest(node, isContainer);
    if (!groups.has(container)) groups.set(container, {tabs: [], panels: []});
    groups.get(container)[kind === 'tab' ? 'tabs' : 'panels'].push(node);
  }
  const labels = new Map();
  const problems = [];
  for (const [container, {tabs, panels}] of groups) {
    if (container === null) {
      for (const panel of panels) problems.push([panel, 'a tab panel outside a tabs container; its tab label cannot be read']);
    } else if (tabs.length !== panels.length) {
      problems.push([container, `a tabs container with ${tabs.length} tabs and ${panels.length} panels; a panel without its tab cannot be told from a PowerShell one`]);
    } else {
      panels.forEach((panel, index) => labels.set(panel, visibleLabel(textContent(tabs[index]))));
    }
  }
  return {labels, problems};
}

const isTokenLine = (node) => classes(node).includes('token-line');

// The text of a node outside its .token-line elements, a newline in place of each token line.
function textOutsideTokenLines(node) {
  if ('text' in node) return node.text;
  if (isTokenLine(node)) return '\n';
  return node.children.map(textOutsideTokenLines).join('');
}

// The lines of a page as [node, text]: the text of each .token-line; each newline-split line of
// the text of a pre outside its token lines (all of a raw pre, the rest of a highlighted one);
// the text of each code element outside a pre (inline code) as one line.
function lines(all) {
  const out = [];
  for (const node of all) {
    if (isTokenLine(node)) out.push([node, textContent(node)]);
    else if (node.name === 'pre') {
      for (const line of node.children.map(textOutsideTokenLines).join('').split('\n')) out.push([node, line]);
    } else if (node.name === 'code' && !nearest(node, (up) => up.name === 'pre')) out.push([node, textContent(node)]);
  }
  return out;
}

// The PowerShell-family language classes of a code block, as the source rule (c) lists them.
const POWERSHELL_LANGUAGE = /^language-(powershell|pwsh|ps1|ps|cmd|bat)$/i;

// The PowerShell language class of the pre a line belongs to (the node itself or a pre around
// it), or undefined.
function powershellLanguage(node) {
  for (let up = node; up; up = up.parent) {
    const language = up.name === 'pre' ? classes(up).find((name) => POWERSHELL_LANGUAGE.test(name)) : undefined;
    if (language) return language;
  }
  return undefined;
}

// Returns {errors, checked}: checked is the number of lines read in PowerShell panels and in
// PowerShell-language code blocks anywhere on the page, a line counted once. A line in both is
// named by its tab.
export function powershellLineErrors(file, html, allowed) {
  const all = elements(elementTree(html));
  const {labels, problems} = panelLabels(all);
  const errors = problems.map(([node, message]) => `built: ${file}:${lineOf(html, node.offset)} ${message}`);
  let checked = 0;
  for (const [node, text] of lines(all)) {
    const panel = nearest(node, (up) => labels.has(up) && /powershell/i.test(labels.get(up)));
    const language = panel ? undefined : powershellLanguage(node);
    if (!panel && !language) continue;
    const line = text.trim();
    if (line === '') continue;
    checked++;
    if (!allowed.has(line)) {
      const where = panel ? `in the "${labels.get(panel)}" tab` : `in a PowerShell code block (${language})`;
      errors.push(
        `built: ${file}:${lineOf(html, node.offset)} the line "${line}" ${where} is not a command line or inline code span of the pack, a Windows variant needs a pack source`,
      );
    }
  }
  return {errors, checked};
}
