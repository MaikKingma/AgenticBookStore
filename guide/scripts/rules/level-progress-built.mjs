// The level progress rule on the rendered pages (spec 14, T2). It reads the server-rendered HTML of the
// Pages build (`npm run check:built`, default build dir only) and asserts what a visitor gets from the
// LevelProgress breadcrumb:
// - on each of the five level pages one nav with aria-label "Level progress", holding one ol of five li,
//   Level 0 to Level 4 in order, each li with one link to its level page (the base URL plus the route of
//   the level page found in the build);
// - data-state on each li is done before the current level, current on it, upcoming after it (the level
//   comes from the file name, level-N-*.html), and exactly one element of the page carries
//   aria-current="step": the link of the current crumb (no other crumb link has any aria-current);
// - a done crumb starts with one check mark element (aria-hidden="true", empty: the stylesheet draws the
//   check character, before the label) and its link name says "completed"; a current or upcoming crumb
//   has no check mark and its link name is just "Level N";
// - no other page of the build has a level progress nav.
// The rule never reads a hashed CSS-module class name: data-state, data-part and aria attributes only.
import {elementTree, textContent} from '../lib/html.mjs';

export const NAV_LABEL = 'Level progress';
export const LEVELS = 5;
const CHECK = String.fromCharCode(0x2713);

// A level page of the build: level-N-name.html at the top of the build dir (not in a subfolder).
const LEVEL_FILE = /^level-(\d+)-[^/]*\.html$/;
export const levelOfFile = (file) => {
  const match = LEVEL_FILE.exec(file);
  return match ? Number(match[1]) : null;
};

const normalize = (text) => text.replace(/\s+/g, ' ').trim();

// Every element below a node, in document order.
function descendants(node, out = []) {
  for (const child of node.children) {
    if ('text' in child) continue;
    out.push(child);
    descendants(child, out);
  }
  return out;
}

// The text a screen reader reads: the text content without the subtrees marked aria-hidden="true".
function readText(node) {
  if ('text' in node) return node.text;
  if (node.attrs.get('aria-hidden') === 'true') return '';
  return node.children.map(readText).join('');
}

// The nav elements of a page whose aria-label is "Level progress": exact, or only the same words
// (any case, spaces around) for the pages that must have none.
const levelNavs = (tree, exact) =>
  descendants(tree).filter((node) => {
    if (node.name !== 'nav') return false;
    const label = node.attrs.get('aria-label');
    if (label === undefined) return false;
    return exact ? label === NAV_LABEL : label.trim().toLowerCase() === NAV_LABEL.toLowerCase();
  });

// The crumbs of the first level progress nav of a page: [{name, state, current, href, link}], one per li
// of its ol; name is the link name (what a screen reader reads), state the data-state, current whether
// the link has aria-current="step". Empty when the page has no such nav.
export function levelProgressCrumbs(html) {
  const nav = levelNavs(elementTree(html), true)[0];
  if (!nav) return [];
  const ol = nav.children.find((child) => !('text' in child) && child.name === 'ol');
  const items = ol ? ol.children.filter((child) => !('text' in child) && child.name === 'li') : [];
  return items.map((item) => {
    const link = descendants(item).find((node) => node.name === 'a');
    return {
      name: link ? normalize(readText(link)) : '',
      state: item.attrs.get('data-state'),
      current: link ? link.attrs.get('aria-current') === 'step' : false,
      href: link ? link.attrs.get('href') : undefined,
      link,
    };
  });
}

// file: the page path for the message; html: its text; base: the base URL of the build (for example
// /AgenticBookStore/); level: the level of the page (0 to 4) or null for a page that is not a level page;
// routes: the five routes of the level pages without ".html" (level-0-name, ...). Returns the error lines
// of one page.
export function levelProgressErrors({file, html, base, level, routes}) {
  const prefix = `level progress: ${file}:`;
  const tree = elementTree(html);

  if (level === null) {
    const found = levelNavs(tree, false);
    return found.length === 0 ? [] : [`${prefix} a page that is not a level page must have no level progress nav (found ${found.length})`];
  }
  if (!Number.isInteger(level) || level < 0 || level >= LEVELS) {
    return [`${prefix} level must be an integer from 0 to 4 (found ${String(level)})`];
  }

  const navs = levelNavs(tree, true);
  if (navs.length === 0) {
    return [`${prefix} no nav with aria-label "${NAV_LABEL}" (a level page must have the level progress breadcrumb)`];
  }
  const errors = [];
  if (navs.length > 1) errors.push(`${prefix} found ${navs.length} navs with aria-label "${NAV_LABEL}", expected one`);
  const nav = navs[0];

  const lists = nav.children.filter((child) => !('text' in child) && child.name === 'ol');
  if (lists.length !== 1) {
    errors.push(`${prefix} the level progress nav must hold one ol (found ${lists.length})`);
    return errors;
  }
  const items = lists[0].children.filter((child) => !('text' in child) && child.name === 'li');
  if (items.length !== LEVELS) errors.push(`${prefix} expected five crumbs (li) in the list, found ${items.length}`);

  let currentLink = null;
  items.slice(0, LEVELS).forEach((item, index) => {
    const at = `${prefix} crumb ${index + 1} (Level ${index})`;
    const state = index < level ? 'done' : index === level ? 'current' : 'upcoming';
    const found = item.attrs.get('data-state');
    if (found !== state) errors.push(`${at} must have data-state ${state} (found ${found ?? 'none'})`);
    if (item.attrs.has('aria-current')) errors.push(`${at}: aria-current belongs on the link, not on the list item`);

    const links = descendants(item).filter((node) => node.name === 'a');
    if (links.length !== 1) {
      errors.push(`${at} must hold exactly one link, found ${links.length}`);
      return;
    }
    const link = links[0];
    // a level with no page in the build has no route: that is reported once, by levelPagesOf
    if (routes[index] !== undefined) {
      const wanted = `${base}${routes[index]}`;
      const href = link.attrs.get('href');
      if (href !== wanted) errors.push(`${at} link must go to ${wanted} (found ${href ?? 'none'})`);
    }

    // aria-current: only the link of the current crumb has it, with the value step.
    const marker = link.attrs.get('aria-current');
    if (index === level) {
      currentLink = link;
      if (marker !== 'step') errors.push(`${at} link must have aria-current="step" (found ${marker ?? 'none'})`);
    } else if (marker !== undefined) {
      errors.push(`${at} link must not have aria-current (found ${marker})`);
    }

    // the link name: "Level N", and for a done crumb a "completed" after it.
    const label = `Level ${index}`;
    const name = normalize(readText(link));
    if (!new RegExp(`^${label}(?![0-9])`).test(name)) {
      errors.push(`${at} must read ${label} (found "${name}")`);
    } else if (state === 'done') {
      if (!/\bcompleted\b/i.test(name.slice(label.length))) {
        errors.push(`${at} is done and its link name must say completed (found "${name}")`);
      }
    } else if (name !== label) {
      errors.push(`${at} link name must be "${label}" (found "${name}")`);
    }

    // the check mark: first in a done crumb's link, nowhere else.
    const marks = descendants(link).filter((node) => node.attrs.get('data-part') === 'check');
    if (state === 'done') {
      if (marks.length !== 1) {
        errors.push(`${at} is done and must have exactly one check mark, found ${marks.length}`);
      } else {
        const mark = marks[0];
        if (mark.attrs.get('aria-hidden') !== 'true') {
          errors.push(`${at} check mark must have aria-hidden="true" (found ${mark.attrs.get('aria-hidden') ?? 'none'})`);
        }
        if (textContent(mark).trim() !== '') {
          errors.push(`${at} check mark must hold no text (the stylesheet draws it; text in it is a colour pair axe cannot decide)`);
        }
        const first = link.children.find((child) => !('text' in child) || child.text.trim() !== '');
        if (first !== mark) errors.push(`${at} check mark must come before the label (it is the first thing in the link)`);
      }
    } else if (marks.length > 0 || textContent(item).includes(CHECK)) {
      errors.push(`${at} is ${state} and must have no check mark`);
    }
  });

  // aria-current="step" on the whole page: exactly one element, the link of the current crumb.
  const steps = descendants(tree).filter((node) => node.attrs.get('aria-current') === 'step');
  if (steps.length !== 1) {
    errors.push(`${prefix} the page must have exactly one element with aria-current="step", found ${steps.length}`);
  } else if (currentLink && steps[0] !== currentLink) {
    errors.push(`${prefix} the element with aria-current="step" must be the link of the current crumb`);
  }
  return errors;
}

// The level pages of a build: files = the .html paths (relative, with /). Returns {levels, errors}:
// levels maps a level number to its file, errors lists a level with no page, two pages for one level and a
// level-N file outside 0 to 4. The routes of the level pages are the file names without ".html".
export function levelPagesOf(files) {
  const levels = new Map();
  const errors = [];
  for (const file of files) {
    const level = levelOfFile(file);
    if (level === null) continue;
    if (level >= LEVELS) {
      errors.push(`level progress: ${file}: levels run from 0 to 4, this file is for Level ${level}`);
    } else if (levels.has(level)) {
      errors.push(`level progress: two pages for Level ${level} (${levels.get(level)} and ${file})`);
    } else {
      levels.set(level, file);
    }
  }
  for (let level = 0; level < LEVELS; level++) {
    if (!levels.has(level)) errors.push(`level progress: no page for Level ${level} (level-${level}-*.html) in the build`);
  }
  return {levels, errors};
}

export const routeOf = (file) => file.replace(/\.html$/, '');
