// The routes rule on the Pages build (spec 16, T1). It characterizes the live site: the pages a visitor can
// reach and the order in which the "Previous" and "Next" links of the pagination nav lead through them. It reads
// the Pages build (`npm run check:built`, default build dir only) and asserts:
// - the <loc> values of build/sitemap.xml are exactly the list below, each once, compared by value: a missing
//   one, an extra one, one replaced by another, a duplicate and a trailing-slash variant all fail;
// - each route has its build file: index.html for the home page, <route>.html for a top level page and
//   reference/<name>.html for a page of the reference folder;
// - the pagination nav of each of those pages leads to the page before and the page after it in the chain below
//   (href compared as a whole: the base URL path plus the route), the first page has no previous link and the
//   last page has no next link.
// The list is frozen on purpose. A page addition must extend it, and say why in its commit and in this comment:
// - 2026-11-11 (spec 16, T1): the 11 pages live at that date, copied from the sitemap of the Pages site.
import {existsSync, readFileSync} from 'node:fs';
import {join} from 'node:path';
import {elementTree} from '../lib/html.mjs';

// The <loc> values as the live sitemap serves them, in the order of the chain: Welcome, Setup check, Level 0 to
// Level 4, Wrap-up, then the reference pages. Typed out in full on purpose: nothing is derived from the base URL.
export const ROUTES = Object.freeze([
  'https://maikkingma.github.io/AgenticBookStore/',
  'https://maikkingma.github.io/AgenticBookStore/setup-check',
  'https://maikkingma.github.io/AgenticBookStore/level-0-break-the-agent',
  'https://maikkingma.github.io/AgenticBookStore/level-1-context-engineering',
  'https://maikkingma.github.io/AgenticBookStore/level-2-give-your-agent-eyes',
  'https://maikkingma.github.io/AgenticBookStore/level-3-behavioral-control',
  'https://maikkingma.github.io/AgenticBookStore/level-4-spec-driven-development',
  'https://maikkingma.github.io/AgenticBookStore/wrap-up',
  'https://maikkingma.github.io/AgenticBookStore/reference/cheat-sheet',
  'https://maikkingma.github.io/AgenticBookStore/reference/architecture',
  'https://maikkingma.github.io/AgenticBookStore/reference/troubleshooting',
]);

const SITEMAP = 'sitemap.xml';
const NAV_CLASS = 'pagination-nav';
const PREV_CLASS = 'pagination-nav__link--prev';
const NEXT_CLASS = 'pagination-nav__link--next';
const HOW_TO_ADD = 'a page addition must extend the list in rules/routes-built.mjs, with a reason';

// The build file of a route: the home page is index.html; every other route is <route>.html, so a route in the
// reference folder is reference/<name>.html.
export function routeFile(loc) {
  const path = new URL(loc).pathname;
  const base = new URL(ROUTES[0]).pathname;
  const route = path.startsWith(base) ? path.slice(base.length) : path.replace(/^\//, '');
  return route === '' ? 'index.html' : `${route}.html`;
}

// The href a page of the build uses for a route: the path of its <loc> (the base URL path plus the route).
const hrefOfRoute = (loc) => new URL(loc).pathname;

// The <loc> values of a sitemap, as written (no trimming, no normalising: the value must equal the live one).
const locsOf = (xml) => [...xml.matchAll(/<loc>([^<]*)<\/loc>/g)].map((match) => match[1]);

function sitemapErrors(dir) {
  const path = join(dir, SITEMAP);
  if (!existsSync(path)) return [`routes: ${SITEMAP} is missing from the build; run npm run build first`];
  const found = locsOf(readFileSync(path, 'utf8'));
  const counts = new Map();
  for (const loc of found) counts.set(loc, (counts.get(loc) ?? 0) + 1);
  const errors = [];
  for (const loc of ROUTES) {
    if (!counts.has(loc)) errors.push(`routes: ${SITEMAP} has no <loc> for ${loc}`);
  }
  for (const [loc, count] of counts) {
    if (!ROUTES.includes(loc)) errors.push(`routes: ${SITEMAP} has an unexpected <loc> ${loc} (${HOW_TO_ADD})`);
    else if (count > 1) errors.push(`routes: ${SITEMAP} lists ${loc} ${count} times, expected once`);
  }
  return errors;
}

const classesOf = (node) => (node.attrs.get('class') ?? '').split(/\s+/).filter(Boolean);
const hasClass = (node, name) => classesOf(node).includes(name);

// Every element below a node, in document order.
function descendants(node, out = []) {
  for (const child of node.children) {
    if ('text' in child) continue;
    out.push(child);
    descendants(child, out);
  }
  return out;
}

// The href of a link as written in the HTML. The shared scanner (lib/html.mjs) drops the slash that ends an
// unquoted value written last in the tag (href=/AgenticBookStore/>), because it must read type=checkbox/> too, and
// so cannot tell /AgenticBookStore/ from /AgenticBookStore. This one reads the tag itself; the first href wins.
function hrefOf(html, node) {
  const tag = /^<a\b((?:[^>"']|"[^"]*"|'[^']*')*)>/.exec(html.slice(node.offset));
  const match = tag && /\shref\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'>]+))/.exec(tag[1]);
  return match ? (match[1] ?? match[2] ?? match[3]) : undefined;
}

// What one of the two links must be: expected is the href it must have, or null when the page must have none.
function linkErrors({prefix, label, nounPlace, links, html, expected}) {
  if (expected === null) {
    return links.map((link) => `${prefix} the ${nounPlace} page must have no ${label} link (found ${hrefOf(html, link) ?? 'none'})`);
  }
  if (links.length === 0) return [`${prefix} no ${label} link, it must go to ${expected}`];
  if (links.length > 1) return [`${prefix} found ${links.length} ${label} links, expected one`];
  const href = hrefOf(html, links[0]);
  return href === expected ? [] : [`${prefix} ${label} link must go to ${expected} (found ${href ?? 'none'})`];
}

// The chain errors of one page: file is its path under the build dir, html its text, index its position in ROUTES.
export function chainErrors({file, html, index}) {
  const prefix = `routes: ${file}:`;
  const navs = descendants(elementTree(html)).filter((node) => node.name === 'nav' && hasClass(node, NAV_CLASS));
  if (navs.length === 0) return [`${prefix} no pagination nav (a nav element with the class ${NAV_CLASS})`];
  if (navs.length > 1) return [`${prefix} found ${navs.length} pagination navs, expected one`];
  const links = descendants(navs[0]).filter((node) => node.name === 'a');
  const prevs = links.filter((link) => hasClass(link, PREV_CLASS));
  const nexts = links.filter((link) => hasClass(link, NEXT_CLASS) && !hasClass(link, PREV_CLASS));
  const errors = [];
  for (const link of links) {
    if (!hasClass(link, PREV_CLASS) && !hasClass(link, NEXT_CLASS)) {
      errors.push(`${prefix} the pagination nav holds a link that is neither previous nor next (href ${hrefOf(html, link) ?? 'none'})`);
    }
  }
  errors.push(
    ...linkErrors({prefix, label: 'previous', nounPlace: 'first', links: prevs, html, expected: index > 0 ? hrefOfRoute(ROUTES[index - 1]) : null}),
    ...linkErrors({prefix, label: 'next', nounPlace: 'last', links: nexts, html, expected: index < ROUTES.length - 1 ? hrefOfRoute(ROUTES[index + 1]) : null}),
  );
  return errors;
}

// dir: the build dir. Returns {errors, routes, chainPages}: the number of routes of the frozen list and of the pages
// whose chain was read (both are the full list when errors is empty).
export function routesErrors(dir) {
  const errors = sitemapErrors(dir);
  let chainPages = 0;
  ROUTES.forEach((loc, index) => {
    const file = routeFile(loc);
    const path = join(dir, file);
    if (!existsSync(path)) {
      errors.push(`routes: ${file} is missing from the build (route ${loc})`);
      return;
    }
    errors.push(...chainErrors({file, html: readFileSync(path, 'utf8'), index}));
    chainPages++;
  });
  return {errors, routes: ROUTES.length, chainPages};
}
