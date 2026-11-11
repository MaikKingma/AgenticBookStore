// The table regions rule on the rendered pages (spec 15, T5). The rehype plugin
// src/plugins/rehype-table-regions.mjs wraps every table in a focusable, named region so a keyboard user can
// scroll a wide table (axe: scrollable-region-focusable). This rule reads the server-rendered HTML of any build
// (the Pages build, the offline build, a fixture build) and asserts, for every table inside an <article>:
// - its parent is a div (the region wrapper);
// - the wrapper has tabindex="0", role="region" and a non-blank aria-label;
// - the table is the only element in the wrapper (one region around two tables is an error);
// - on one page no two wrappers have the same name, compared without case and extra spaces (axe:
//   landmark-unique); the same name on two different pages is fine.
// It never reads a class name: attributes and tree position only, which is what a browser and a screen reader see.
import {elementTree, lineOf} from '../lib/html.mjs';

const normalize = (text) => text.replace(/\s+/g, ' ').trim();

const elements = (node) => node.children.filter((child) => !('text' in child));

// Every element below a node, in document order.
function descendants(node, out = []) {
  for (const child of node.children) {
    if ('text' in child) continue;
    out.push(child);
    descendants(child, out);
  }
  return out;
}

const shown = (value) => (value === undefined ? 'none' : `"${value}"`);

// file: the page path for the message; html: its text. Returns {errors, tables}: tables is the number of
// tables inside <article> elements of the page.
export function tableRegions({file, html}) {
  const tree = elementTree(html);
  const errors = [];
  const names = new Map();
  let tables = 0;
  // an article inside an article would list its tables twice
  const found = new Set();
  for (const article of descendants(tree).filter((node) => node.name === 'article')) {
    for (const table of descendants(article).filter((node) => node.name === 'table')) found.add(table);
  }
  for (const table of found) {
    tables++;
    const at = `table regions: ${file}:${lineOf(html, table.offset)}`;
    const wrapper = table.parent;
    // a div that has none of the three attributes is no wrapper attempt: the table is simply not in a region
    const attempt = wrapper && wrapper.name === 'div' && ['tabindex', 'role', 'aria-label'].some((name) => wrapper.attrs.has(name));
    if (!attempt) {
      errors.push(`${at} table is not directly inside a region wrapper (a div with tabindex="0", role="region" and an aria-label); the plugin src/plugins/rehype-table-regions.mjs must wrap every table`);
      continue;
    }
    const tabindex = wrapper.attrs.get('tabindex');
    if (tabindex === undefined || tabindex.trim() !== '0') {
      errors.push(`${at} the region wrapper's tabindex must be "0" (found ${shown(tabindex)}); a scrollable table must take keyboard focus`);
    }
    const role = wrapper.attrs.get('role');
    if (role === undefined || role.trim() !== 'region') {
      errors.push(`${at} the region wrapper's role must be "region" (found ${shown(role)})`);
    }
    const label = wrapper.attrs.get('aria-label');
    if (label === undefined || normalize(label) === '') {
      errors.push(`${at} the region wrapper's aria-label must not be empty; the region needs a name`);
    } else {
      const key = normalize(label).toLowerCase();
      if (names.has(key)) {
        errors.push(`${at} region name "${normalize(label)}" is also used by the table at line ${names.get(key)}; each region on a page needs its own name (axe landmark-unique)`);
      } else {
        names.set(key, lineOf(html, table.offset));
      }
    }
    const inside = elements(wrapper);
    if (inside.length !== 1 || inside[0] !== table) {
      errors.push(`${at} the region must hold exactly one element, the table (found ${inside.length} elements)`);
    }
  }
  return {errors, tables};
}
