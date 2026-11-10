// A rehype plugin that gives every task-list checkbox an accessible name.
//
// Docusaurus renders a GFM task list item ("- [ ] text") as a bare disabled
// <input type="checkbox"> next to the text, with no label. axe reports that as a
// critical "label" violation (WCAG 4.1.2) and a screen reader announces an unnamed
// checkbox. Docusaurus 3.10.2 has no option for it, so this plugin sets aria-label
// on the checkbox to the plain text of its own list item.
//
// Node built-ins only: a small tree walk over hast, no unist-util-visit.

// In hast, className is an array of class names.
const hasClass = (node, name) => Array.isArray(node.properties?.className) && node.properties.className.includes(name);

const isElement = (node, tagName) => node.type === 'element' && node.tagName === tagName;
const isCheckbox = (node) => isElement(node, 'input') && node.properties?.type === 'checkbox';
const isList = (node) => isElement(node, 'ul') || isElement(node, 'ol');

// The checkbox of a task item: a direct child (tight list), or a child of a direct
// paragraph (loose list).
function findCheckbox(item) {
  for (const child of item.children ?? []) {
    if (isCheckbox(child)) return child;
    if (isElement(child, 'p')) {
      const inner = (child.children ?? []).find(isCheckbox);
      if (inner) return inner;
    }
  }
  return undefined;
}

// Plain text of the item: text nodes in document order, inline code and emphasis
// included, the checkbox itself and any nested list left out.
function itemText(node, out) {
  for (const child of node.children ?? []) {
    if (child.type === 'text') out.push(child.value);
    else if (child.type === 'element' && !isCheckbox(child) && !isList(child)) itemText(child, out);
  }
  return out;
}

const collapse = (value) => value.replace(/\s+/g, ' ').trim();

function label(node) {
  if (isElement(node, 'li') && hasClass(node, 'task-list-item')) {
    const checkbox = findCheckbox(node);
    if (checkbox && !checkbox.properties.ariaLabel) {
      const name = collapse(itemText(node, []).join(''));
      if (name !== '') checkbox.properties.ariaLabel = name;
    }
  }
  for (const child of node.children ?? []) label(child);
}

export default function rehypeTaskListLabels() {
  return (tree) => {
    label(tree);
  };
}
