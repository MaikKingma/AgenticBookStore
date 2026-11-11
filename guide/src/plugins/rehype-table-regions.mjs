// A rehype plugin that puts every table of a page into a focusable, named scroll region.
//
// Docusaurus renders a markdown table as a bare <table> that scrolls when it is wider than
// the page (Infima gives it display: block and overflow: auto). A scrolling box that cannot
// take focus is an axe "scrollable-region-focusable" violation (WCAG 2.1.1): a keyboard
// user cannot scroll it. This plugin wraps each table in a div with tabindex="0",
// role="region" and an aria-label, so the keyboard reaches it and a screen reader names it.
//
// The label is the text of the nearest preceding heading. A heading with several tables
// numbers them ("Packages (table 1 of 2)"), and any label already used on the page gets a
// count ("Notes, 2"), because two regions with one name are an axe "landmark-unique"
// violation. The role goes on the wrapper, never on the table: role="region" on a table
// would replace its table semantics.
//
// The tree walk covers MDX nodes as well as plain elements: a table inside a JSX element
// (TabItem, details) is found, and an MDX expression such as the id comment of
// "## Title {/* #id */}" is no part of a heading text.
//
// Node built-ins only, and none needed: a small tree walk over hast, no unist-util-visit.

export const REGION_CLASS = 'guide-table-region';
const FALLBACK = 'Table';

const isElement = (node, tagName) => node.type === 'element' && node.tagName === tagName;
const isHeading = (node) =>
  (node.type === 'element' && /^h[1-6]$/.test(node.tagName)) ||
  ((node.type === 'mdxJsxFlowElement' || node.type === 'mdxJsxTextElement') && /^h[1-6]$/.test(node.name ?? ''));
const isRegion = (node) =>
  isElement(node, 'div') && Array.isArray(node.properties?.className) && node.properties.className.includes(REGION_CLASS);

// Plain text of a heading in document order: text nodes, also inside inline code, emphasis,
// links and JSX elements such as <kbd>. MDX expressions and comments hold no text.
function headingText(node, out) {
  for (const child of node.children ?? []) {
    if (child.type === 'text') out.push(child.value);
    else if (child.type === 'element' || child.type === 'mdxJsxTextElement' || child.type === 'mdxJsxFlowElement') headingText(child, out);
  }
  return out;
}

const collapse = (value) => value.replace(/\s+/g, ' ').trim();
// How axe compares names: the same words in any case and spacing are one name.
const nameKey = (label) => collapse(label).toLowerCase();

// One walk in document order. state.heading is the nearest heading seen so far, an object
// {text} (a fresh one per heading, so the tables of one heading group by identity).
function collect(node, state) {
  for (const child of node.children ?? []) {
    if (isHeading(child)) {
      state.heading = {text: collapse(headingText(child, []).join(''))};
    } else if (isRegion(child)) {
      // already wrapped (the plugin ran before): keep it and remember its name
      const label = child.properties.ariaLabel;
      if (typeof label === 'string') state.used.add(nameKey(label));
    } else if (isElement(child, 'table')) {
      state.tables.push({parent: node, table: child, heading: state.heading});
    } else {
      collect(child, state);
    }
  }
}

export default function rehypeTableRegions() {
  return (tree) => {
    const state = {heading: null, tables: [], used: new Set()};
    collect(tree, state);

    const perHeading = new Map();
    for (const entry of state.tables) perHeading.set(entry.heading, (perHeading.get(entry.heading) ?? 0) + 1);
    const seen = new Map();

    for (const {parent, table, heading} of state.tables) {
      const base = heading && heading.text !== '' ? heading.text : FALLBACK;
      const total = perHeading.get(heading);
      const position = (seen.get(heading) ?? 0) + 1;
      seen.set(heading, position);
      let label = total > 1 ? `${base} (table ${position} of ${total})` : base;
      for (let count = 2; state.used.has(nameKey(label)); count++) {
        label = `${total > 1 ? `${base} (table ${position} of ${total})` : base}, ${count}`;
      }
      state.used.add(nameKey(label));
      const wrapper = {
        type: 'element',
        tagName: 'div',
        properties: {className: [REGION_CLASS], tabIndex: 0, role: 'region', ariaLabel: label},
        children: [table],
      };
      parent.children[parent.children.indexOf(table)] = wrapper;
    }
  };
}
