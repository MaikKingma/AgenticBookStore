import {elementTree, lineOf, textContent} from '../lib/html.mjs';

// Rule (c) on the built pages: every line of every pre or code element inside a tab panel
// whose tab label contains "PowerShell" (any case) is a pack command line. The source rule (c)
// in rules/content.mjs reads the MDX with a scanner that can lose track of a tab; the built
// HTML is what the participant sees, so this rule reads that, hidden panels included.
// A tabs container is an element with the class tabs-container (the Docusaurus Tabs
// component); its n-th [role=tabpanel] belongs to its n-th [role=tab], counting only the tabs
// and panels whose nearest tabs container it is. A line inside any PowerShell panel counts,
// also when a nested panel around it has another label. A line is the text of one
// .token-line (a highlighted code block), one newline-split line of a raw pre, or the text of
// an inline code element; character references decoded, trimmed, empty lines skipped. A panel
// whose tab cannot be told is an error (it could be a PowerShell one).

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

// Returns {labels, problems}: labels maps a panel node to its tab label; problems lists, as
// [node, message], the panels whose tab cannot be told (fail-closed: such a panel could be a
// PowerShell one). Docusaurus writes as many tabs as panels into every container.
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
      panels.forEach((panel, index) => labels.set(panel, textContent(tabs[index])));
    }
  }
  return {labels, problems};
}

const isTokenLine = (node) => classes(node).includes('token-line');

// The lines of a page as [node, text]: the text of each .token-line; each newline-split line of
// the text of a raw pre (one with no .token-line inside); the text of each code element outside
// a pre (inline code) as one line.
function lines(all) {
  const out = [];
  for (const node of all) {
    if (isTokenLine(node)) out.push([node, textContent(node)]);
    else if (node.name === 'pre' && !elements(node).some(isTokenLine)) {
      for (const line of textContent(node).split('\n')) out.push([node, line]);
    } else if (node.name === 'code' && !nearest(node, (up) => up.name === 'pre')) out.push([node, textContent(node)]);
  }
  return out;
}

// Returns {errors, checked}: checked is the number of lines read in PowerShell panels.
export function powershellLineErrors(file, html, allowed) {
  const all = elements(elementTree(html));
  const {labels, problems} = panelLabels(all);
  const errors = problems.map(([node, message]) => `built: ${file}:${lineOf(html, node.offset)} ${message}`);
  let checked = 0;
  for (const [node, text] of lines(all)) {
    const panel = nearest(node, (up) => labels.has(up) && /powershell/i.test(labels.get(up)));
    if (!panel) continue;
    const line = text.trim();
    if (line === '') continue;
    checked++;
    if (!allowed.has(line)) {
      errors.push(
        `built: ${file}:${lineOf(html, node.offset)} the line "${line}" in the "${labels.get(panel)}" tab is not a command line or inline code span of the pack, a Windows variant needs a pack source`,
      );
    }
  }
  return {errors, checked};
}
