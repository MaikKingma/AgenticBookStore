// Spec 16, T1: the built-output routes rule (rules/routes-built.mjs). It characterizes the live site before the
// bonus page is added: the 11 routes in the sitemap and in the build, and the prev/next chain of the pagination
// nav on each of them. The fixtures have the markup of a real Pages build (minified, unquoted attributes, the
// pagination nav of Docusaurus 3). Nothing here reads guide/build: `npm run check:built` runs the rule on the
// default build dir only, and the entry-point tests prove that switch on fixture builds (a copy of the scripts
// folder whose ../build is a fixture).
import {test} from 'node:test';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {cpSync, mkdirSync, mkdtempSync, realpathSync, rmSync, writeFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {dirname, join} from 'node:path';
import {fileURLToPath} from 'node:url';
import {ROUTES, routesErrors} from './rules/routes-built.mjs';
import {scanBuilt} from './rules/built.mjs';

const SCRIPTS = dirname(fileURLToPath(import.meta.url));
const SITE = 'https://maikkingma.github.io/AgenticBookStore/';

// The live <loc> values, in the order of the chain (a second copy of the list, typed here on purpose: the rule's
// list must equal what the live sitemap served on 2026-11-11).
const LIVE = [
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
];
// The build writes the sitemap in this order (alphabetical by file, the home page last), not in chain order.
const BUILD_ORDER = [2, 3, 4, 5, 6, 9, 8, 10, 1, 7, 0].map((index) => LIVE[index]);

const FILES = [
  'index.html',
  'setup-check.html',
  'level-0-break-the-agent.html',
  'level-1-context-engineering.html',
  'level-2-give-your-agent-eyes.html',
  'level-3-behavioral-control.html',
  'level-4-spec-driven-development.html',
  'wrap-up.html',
  'reference/cheat-sheet.html',
  'reference/architecture.html',
  'reference/troubleshooting.html',
];
const LAST = LIVE.length - 1;
const hrefAt = (index) => new URL(LIVE[index]).pathname;

// --- fixtures: the markup of a real build ---------------------------------------------------------------------

const sitemapOf = (locs) =>
  '<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">' +
  locs.map((loc) => `<url><loc>${loc}</loc><changefreq>weekly</changefreq><priority>0.5</priority></url>`).join('') +
  '</urlset>';

const paginationLink = (kind, href) =>
  `<a class="pagination-nav__link pagination-nav__link--${kind}" href=${href}><div class=pagination-nav__sublabel>${kind === 'prev' ? 'Previous' : 'Next'}</div><div class=pagination-nav__label>Label</div></a>`;

// The page at chain position index. prev and next default to the right hrefs; null leaves the link out; extra is
// markup added at the end of the nav; nav: false leaves the pagination nav out altogether.
function pageAt(index, {prev = index > 0 ? hrefAt(index - 1) : null, next = index < LAST ? hrefAt(index + 1) : null, nav = true, extra = ''} = {}) {
  const links = (prev === null ? '' : paginationLink('prev', prev)) + (next === null ? '' : paginationLink('next', next));
  const bar = nav ? `<nav class="docusaurus-mt-lg pagination-nav" aria-label="Docs pages">${links}${extra}</nav>` : '';
  return `<!doctype html><html><head><title>Page</title></head><body><div id=__docusaurus><main><article><h1>Page</h1></article>${bar}</main></div></body></html>\n`;
}

// The files of a good build, by path under the build dir; a test deletes or replaces entries.
const goodFiles = () => ({
  'sitemap.xml': sitemapOf(BUILD_ORDER),
  ...Object.fromEntries(FILES.map((file, index) => [file, pageAt(index)])),
});

function withBuild(files, fn) {
  const root = realpathSync(mkdtempSync(join(tmpdir(), 'guide-routes-test-')));
  try {
    for (const [rel, content] of Object.entries(files)) {
      const abs = join(root, rel);
      mkdirSync(dirname(abs), {recursive: true});
      writeFileSync(abs, content);
    }
    return fn(root);
  } finally {
    rmSync(root, {recursive: true, force: true});
  }
}

// The rule on a build made of the given files (path under the build dir -> content).
const scan = (files) => withBuild(Object.fromEntries(Object.entries(files).map(([rel, html]) => [`build/${rel}`, html])), (root) => routesErrors(join(root, 'build')));
const errorsOf = (files) => scan(files).errors;

const without = (files, rel) => {
  assert.ok(rel in files, `the fixture has no ${rel}`);
  const {[rel]: dropped, ...rest} = files;
  return rest;
};
const withSitemap = (locs) => ({...goodFiles(), 'sitemap.xml': sitemapOf(locs)});
const withPage = (index, options) => ({...goodFiles(), [FILES[index]]: pageAt(index, options)});
const only = (errors, pattern) => {
  assert.equal(errors.length, 1, errors.join('\n') || '(no error)');
  assert.match(errors[0], pattern);
};
const includesError = (errors, pattern) => assert.ok(errors.some((error) => pattern.test(error)), errors.join('\n') || '(no error)');

// --- the frozen list ------------------------------------------------------------------------------------------

test('routes list: the 11 live <loc> values, verbatim and in chain order, frozen', () => {
  assert.deepEqual([...ROUTES], LIVE);
  assert.ok(Object.isFrozen(ROUTES));
  assert.equal(new Set(ROUTES).size, 11);
  assert.ok(ROUTES.every((loc) => loc.startsWith(SITE)));
});

// --- a good build ---------------------------------------------------------------------------------------------

test('routes rule: a build shaped like the real one passes, with 11 routes and 11 pages in the chain', () => {
  assert.deepEqual(scan(goodFiles()), {errors: [], routes: 11, chainPages: 11});
});

test('routes rule: the order of the sitemap does not matter', () => {
  assert.deepEqual(errorsOf(withSitemap([...LIVE])), []);
  assert.deepEqual(errorsOf(withSitemap([...LIVE].reverse())), []);
});

test('routes rule: the pagination links may be quoted in the html', () => {
  const quoted = pageAt(2).replace(/href=(\S+?)>/g, 'href="$1">');
  assert.notEqual(quoted, pageAt(2));
  assert.deepEqual(errorsOf({...goodFiles(), [FILES[2]]: quoted}), []);
});

// --- a route's build file -------------------------------------------------------------------------------------

for (const [index, file] of FILES.entries()) {
  test(`routes rule: the build file ${file} deleted fails once, naming the file and the route`, () => {
    only(errorsOf(without(goodFiles(), file)), new RegExp(`^routes: ${file.replace(/\./g, '\\.')} is missing from the build \\(route ${LIVE[index].replace(/[./:]/g, '\\$&')}\\)`));
  });
}

test('routes rule: a nested route is read from its folder (reference/troubleshooting.html, not a flat file)', () => {
  const files = without(goodFiles(), 'reference/troubleshooting.html');
  files['reference-troubleshooting.html'] = pageAt(LAST);
  only(errorsOf(files), /^routes: reference\/troubleshooting\.html is missing from the build/);
});

test('routes rule: the home page is index.html (deleting it fails, a stray home.html does not help)', () => {
  const files = without(goodFiles(), 'index.html');
  files['home.html'] = pageAt(0);
  only(errorsOf(files), /^routes: index\.html is missing from the build \(route https:\/\/maikkingma\.github\.io\/AgenticBookStore\/\)/);
});

// --- the sitemap ----------------------------------------------------------------------------------------------

test('routes rule: a build with no sitemap fails once', () => {
  only(errorsOf(without(goodFiles(), 'sitemap.xml')), /^routes: sitemap\.xml is missing from the build; run npm run build first/);
});

test('routes rule: a sitemap with no <loc> at all fails for every route', () => {
  const errors = errorsOf(withSitemap([]));
  assert.equal(errors.length, 11, errors.join('\n'));
  for (const loc of LIVE) includesError(errors, new RegExp(`^routes: sitemap\\.xml has no <loc> for ${loc.replace(/[./:]/g, '\\$&')}$`));
});

test('routes rule: an extra <loc> in the sitemap fails (a page addition must extend the frozen list)', () => {
  const extra = `${SITE}bonus-level-orchestration`;
  const errors = errorsOf(withSitemap([...BUILD_ORDER, extra]));
  only(errors, new RegExp(`^routes: sitemap\\.xml has an unexpected <loc> ${extra.replace(/[./:]/g, '\\$&')} .*extend the list in rules/routes-built\\.mjs`));
});

test('routes rule: a missing <loc> fails, for every route in turn', () => {
  for (const loc of LIVE) {
    const errors = errorsOf(withSitemap(BUILD_ORDER.filter((other) => other !== loc)));
    only(errors, new RegExp(`^routes: sitemap\\.xml has no <loc> for ${loc.replace(/[./:]/g, '\\$&')}$`));
  }
});

test('routes rule: one <loc> replaced by another at the same count fails twice (the missing one and the unknown one)', () => {
  const replacement = `${SITE}level-5-something`;
  const locs = BUILD_ORDER.map((loc) => (loc === LIVE[7] ? replacement : loc));
  assert.equal(locs.length, 11);
  const errors = errorsOf(withSitemap(locs));
  assert.equal(errors.length, 2, errors.join('\n'));
  includesError(errors, /sitemap\.xml has no <loc> for https:\/\/maikkingma\.github\.io\/AgenticBookStore\/wrap-up$/);
  includesError(errors, /sitemap\.xml has an unexpected <loc> https:\/\/maikkingma\.github\.io\/AgenticBookStore\/level-5-something /);
});

test('routes rule: a <loc> listed twice fails even when the set of locs is right', () => {
  const locs = [...BUILD_ORDER, LIVE[4]];
  assert.equal(new Set(locs).size, 11);
  only(errorsOf(withSitemap(locs)), /^routes: sitemap\.xml lists https:\/\/maikkingma\.github\.io\/AgenticBookStore\/level-2-give-your-agent-eyes 2 times, expected once$/);
});

test('routes rule: a <loc> with a trailing slash fails (it is not the live one, and is not normalised away)', () => {
  const locs = BUILD_ORDER.map((loc) => (loc === LIVE[7] ? `${LIVE[7]}/` : loc));
  const errors = errorsOf(withSitemap(locs));
  assert.equal(errors.length, 2, errors.join('\n'));
  includesError(errors, /has no <loc> for https:\/\/maikkingma\.github\.io\/AgenticBookStore\/wrap-up$/);
  includesError(errors, /has an unexpected <loc> https:\/\/maikkingma\.github\.io\/AgenticBookStore\/wrap-up\/ /);
});

test('routes rule: the home <loc> without its trailing slash fails (the slash belongs to the live value)', () => {
  const locs = BUILD_ORDER.map((loc) => (loc === LIVE[0] ? SITE.slice(0, -1) : loc));
  const errors = errorsOf(withSitemap(locs));
  assert.equal(errors.length, 2, errors.join('\n'));
  includesError(errors, /has no <loc> for https:\/\/maikkingma\.github\.io\/AgenticBookStore\/$/);
  includesError(errors, /has an unexpected <loc> https:\/\/maikkingma\.github\.io\/AgenticBookStore /);
});

test('routes rule: a <loc> with another host, scheme or spaces around it fails', () => {
  for (const wrong of ['http://maikkingma.github.io/AgenticBookStore/wrap-up', 'https://example.org/AgenticBookStore/wrap-up', ` ${LIVE[7]} `]) {
    const errors = errorsOf(withSitemap(BUILD_ORDER.map((loc) => (loc === LIVE[7] ? wrong : loc))));
    assert.equal(errors.length, 2, `${wrong}: ${errors.join('\n')}`);
  }
});

// --- the prev/next chain --------------------------------------------------------------------------------------

test('routes rule: a swapped next href fails, for every page that has a next', () => {
  for (let index = 0; index < LAST; index++) {
    const wrong = hrefAt((index + 2) % LIVE.length);
    only(errorsOf(withPage(index, {next: wrong})), new RegExp(`^routes: ${FILES[index].replace(/\./g, '\\.')}: next link must go to ${hrefAt(index + 1).replace(/[./]/g, '\\$&')} \\(found ${wrong.replace(/[./]/g, '\\$&')}\\)$`));
  }
});

test('routes rule: a swapped prev href fails, for every page that has a prev', () => {
  for (let index = 1; index <= LAST; index++) {
    const wrong = hrefAt((index + 2) % LIVE.length);
    only(errorsOf(withPage(index, {prev: wrong})), new RegExp(`^routes: ${FILES[index].replace(/\./g, '\\.')}: previous link must go to ${hrefAt(index - 1).replace(/[./]/g, '\\$&')} \\(found ${wrong.replace(/[./]/g, '\\$&')}\\)$`));
  }
});

test('routes rule: a page with no pagination nav fails, for every page', () => {
  for (const [index, file] of FILES.entries()) {
    only(errorsOf(withPage(index, {nav: false})), new RegExp(`^routes: ${file.replace(/\./g, '\\.')}: no pagination nav \\(a nav element with the class pagination-nav\\)`));
  }
});

test('routes rule: a missing next link fails, and so does a missing prev link', () => {
  only(errorsOf(withPage(3, {next: null})), /^routes: level-1-context-engineering\.html: no next link, it must go to \/AgenticBookStore\/level-2-give-your-agent-eyes$/);
  only(errorsOf(withPage(3, {prev: null})), /^routes: level-1-context-engineering\.html: no previous link, it must go to \/AgenticBookStore\/level-0-break-the-agent$/);
});

test('routes rule: Troubleshooting is the last page, so a next link on it fails', () => {
  only(errorsOf(withPage(LAST, {next: hrefAt(0)})), /^routes: reference\/troubleshooting\.html: the last page must have no next link \(found \/AgenticBookStore\/\)$/);
  only(errorsOf(withPage(LAST, {next: '/AgenticBookStore/reference/extra'})), /^routes: reference\/troubleshooting\.html: the last page must have no next link \(found \/AgenticBookStore\/reference\/extra\)$/);
});

test('routes rule: Welcome is the first page, so a prev link on it fails', () => {
  only(errorsOf(withPage(0, {prev: hrefAt(LAST)})), /^routes: index\.html: the first page must have no previous link \(found \/AgenticBookStore\/reference\/troubleshooting\)$/);
});

test('routes rule: a next href with an extra path segment fails (the whole href is compared, not its end)', () => {
  const extraSegment = '/AgenticBookStore/x/wrap-up';
  assert.ok(extraSegment.endsWith('/wrap-up') && extraSegment.includes('wrap-up'));
  only(errorsOf(withPage(6, {next: extraSegment})), /^routes: level-4-spec-driven-development\.html: next link must go to \/AgenticBookStore\/wrap-up \(found \/AgenticBookStore\/x\/wrap-up\)$/);
  // an href that starts with the right one (a trailing slash, a longer name) fails as well
  for (const longer of ['/AgenticBookStore/wrap-up/', '/AgenticBookStore/wrap-up-old']) {
    only(errorsOf(withPage(6, {next: longer})), /^routes: level-4-spec-driven-development\.html: next link must go to \/AgenticBookStore\/wrap-up \(found /);
  }
});

test('routes rule: a prev href that only starts with, ends with or contains the right one fails', () => {
  for (const wrong of ['/AgenticBookStore/sub/', '/x/AgenticBookStore/', '/AgenticBookStore']) {
    only(errorsOf(withPage(1, {prev: wrong})), new RegExp(`^routes: setup-check\\.html: previous link must go to /AgenticBookStore/ \\(found ${wrong.replace(/[./]/g, '\\$&')}\\)$`));
  }
  only(errorsOf(withPage(7, {prev: '/AgenticBookStore/old/level-4-spec-driven-development'})), /^routes: wrap-up\.html: previous link must go to \/AgenticBookStore\/level-4-spec-driven-development /);
});

test('routes rule: two pagination navs, or a link in the nav that is neither previous nor next, fail', () => {
  const doubled = pageAt(3).replace('</main>', pageAt(3).match(/<nav class="docusaurus-mt-lg[^]*?<\/nav>/)[0] + '</main>');
  assert.notEqual(doubled, pageAt(3));
  only(errorsOf({...goodFiles(), [FILES[3]]: doubled}), /^routes: level-1-context-engineering\.html: found 2 pagination navs, expected one$/);
  only(errorsOf(withPage(3, {extra: '<a class=pagination-nav__link href=/AgenticBookStore/wrap-up>Stray</a>'})), /^routes: level-1-context-engineering\.html: the pagination nav holds a link that is neither previous nor next \(href \/AgenticBookStore\/wrap-up\)$/);
});

test('routes rule: a second previous link in the nav fails', () => {
  only(errorsOf(withPage(3, {extra: paginationLink('prev', hrefAt(2))})), /^routes: level-1-context-engineering\.html: found 2 previous links, expected one$/);
});

test('routes rule: a nav that only looks like a pagination nav (other class) is not one', () => {
  const lookalike = pageAt(3).replace('class="docusaurus-mt-lg pagination-nav"', 'class="docusaurus-mt-lg pagination-navigation"');
  assert.notEqual(lookalike, pageAt(3));
  only(errorsOf({...goodFiles(), [FILES[3]]: lookalike}), /^routes: level-1-context-engineering\.html: no pagination nav/);
});

test('routes rule: a missing file is reported once and its chain is not read (no second error for the same page)', () => {
  const result = scan(without(goodFiles(), 'wrap-up.html'));
  assert.equal(result.errors.length, 1, result.errors.join('\n'));
  assert.equal(result.routes, 11);
  assert.equal(result.chainPages, 10);
});

test('routes rule: errors of the sitemap, the files and the chain come together', () => {
  const files = withSitemap(BUILD_ORDER.slice(1));
  delete files['reference/architecture.html'];
  files['level-2-give-your-agent-eyes.html'] = pageAt(4, {next: hrefAt(0)});
  const errors = errorsOf(files);
  assert.equal(errors.length, 3, errors.join('\n'));
  includesError(errors, /sitemap\.xml has no <loc>/);
  includesError(errors, /^routes: reference\/architecture\.html is missing/);
  includesError(errors, /^routes: level-2-give-your-agent-eyes\.html: next link must go to/);
});

// --- scanBuilt: the option ------------------------------------------------------------------------------------

const scanBuiltOf = (files, options) =>
  withBuild(Object.fromEntries(Object.entries(files).map(([rel, html]) => [`build/${rel}`, html])), (root) => scanBuilt(join(root, 'build'), options));

test('scanBuilt: with the routes option the rule runs and its counts are returned', () => {
  const result = scanBuiltOf(goodFiles(), {routes: true});
  assert.deepEqual(result.errors, []);
  assert.equal(result.routes, 11);
  assert.equal(result.chainPages, 11);
  const broken = scanBuiltOf(without(goodFiles(), 'sitemap.xml'), {routes: true});
  only(broken.errors, /^routes: sitemap\.xml is missing from the build/);
});

test('scanBuilt: without the routes option no routes rule runs (the fixture builds of the other self-tests stay valid)', () => {
  const result = scanBuiltOf(without(goodFiles(), 'sitemap.xml'), {});
  assert.deepEqual(result.errors, []);
  assert.equal(result.routes, undefined);
  assert.equal(result.chainPages, undefined);
});

// --- the entry point: the default build dir switches the rule on ----------------------------------------------
// A copy of the scripts folder sits beside a fixture build and a fixture config, so check-built.mjs resolves its
// default build dir (../build) to the fixture and never to guide/build. The fixture has what the other rules of the
// default build dir need too: the navbar on every page and the level progress breadcrumb on the five level pages.

const NAVBAR =
  '<nav aria-label=Main class="theme-layout-navbar navbar navbar--fixed-top"><div class=navbar__inner><div class="theme-layout-navbar-left navbar__items">' +
  '<a href=https://openvalue.eu target=_blank rel="noopener noreferrer" class=guide-navbar-logo aria-label="OpenValue (opens in a new tab)">' +
  '<img src=/AgenticBookStore/img/openvalue-logo-dark.png alt="" class="themedComponent_mlkZ themedComponent--light_NVdE" height=32 />' +
  '<img src=/AgenticBookStore/img/openvalue-logo-light.png alt="" class="themedComponent_mlkZ themedComponent--dark_xIcU" height=32 /></a>' +
  '<a class=navbar__brand href=/AgenticBookStore/><b class="navbar__title text--truncate">Crafting your Agentic Harness</b></a></div>' +
  '<div class="theme-layout-navbar-right navbar__items navbar__items--right">' +
  '<a href=https://github.com/MaikKingma/AgenticBookStore target=_blank rel="noopener noreferrer" class="navbar__item navbar__link">GitHub</a>' +
  '<a href=https://www.linkedin.com/in/maik-kingma/ target=_blank rel="noopener noreferrer" class="navbar__item navbar__link">Follow on LinkedIn</a></div></div></nav>';

function levelProgress(current) {
  const crumbs = [2, 3, 4, 5, 6].map((chain, level) => {
    const state = level < current ? 'done' : level === current ? 'current' : 'upcoming';
    const check = state === 'done' ? '<span aria-hidden=true data-part=check></span>' : '';
    const done = state === 'done' ? '<span> (completed)</span>' : '';
    const marker = state === 'current' ? ' aria-current=step' : '';
    return `<li data-state=${state}><a${marker} href=${hrefAt(chain)}>${check}<span>Level ${level}</span>${done}</a>`;
  });
  return `<nav aria-label="Level progress"><ol>${crumbs.join('')}</ol></nav>`;
}

// A page of the full default build: the page of the chain with the navbar and, for a level page, the breadcrumb.
function fullPage(index) {
  const level = index >= 2 && index <= 6 ? index - 2 : null;
  return pageAt(index).replace('<main>', `${NAVBAR}<main>${level === null ? '' : levelProgress(level)}`);
}
const fullFiles = () => ({
  'sitemap.xml': sitemapOf(BUILD_ORDER),
  ...Object.fromEntries(FILES.map((file, index) => [file, fullPage(index)])),
});

function withCopiedScripts(build, fn) {
  const files = {'docusaurus.config.ts': "export default {\n  baseUrl: '/AgenticBookStore/',\n};\n"};
  for (const [rel, content] of Object.entries(build)) files[`build/${rel}`] = content;
  return withBuild(files, (root) => {
    cpSync(SCRIPTS, join(root, 'scripts'), {recursive: true, filter: (source) => !source.endsWith('.test.mjs')});
    return fn(root, join(root, 'scripts', 'check-built.mjs'));
  });
}

const run = (script, args = []) => spawnSync(process.execPath, [script, ...args], {encoding: 'utf8'});

test('check-built with no argument on a build shaped like the real one prints the routes ok line and exits 0', () => {
  withCopiedScripts(fullFiles(), (root, script) => {
    const result = run(script);
    assert.equal(result.status, 0, result.stdout + result.stderr);
    assert.match(result.stdout, /^ok {3}routes: 11 routes in the sitemap and the build, prev\/next chain of 11 pages$/m);
    assert.match(result.stdout, /^ok {3}navbar: 11 pages, /m);
    assert.match(result.stdout, /^ok {3}level progress: 5 level pages, each with five crumbs and one current, none on the 6 other pages$/m);
  });
});

test('check-built with no argument and the sitemap removed exits 1 with the routes message and no routes ok line', () => {
  withCopiedScripts(without(fullFiles(), 'sitemap.xml'), (root, script) => {
    const result = run(script);
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.match(result.stderr, /^routes: sitemap\.xml is missing from the build; run npm run build first$/m);
    assert.match(result.stderr, /check:built failed with 1 error/);
    assert.doesNotMatch(result.stdout, /routes/);
  });
});

test('check-built with no argument and a broken chain exits 1 with the page named', () => {
  const build = fullFiles();
  build['wrap-up.html'] = fullPage(7).replace(`href=${hrefAt(8)}`, 'href=/AgenticBookStore/x/cheat-sheet');
  withCopiedScripts(build, (root, script) => {
    const result = run(script);
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.match(result.stderr, /^routes: wrap-up\.html: next link must go to \/AgenticBookStore\/reference\/cheat-sheet \(found \/AgenticBookStore\/x\/cheat-sheet\)$/m);
    assert.doesNotMatch(result.stdout, /ok {3}routes/);
  });
});

test('check-built with no argument runs the routes rule even when the baseUrl cannot be read', () => {
  withCopiedScripts(without(fullFiles(), 'sitemap.xml'), (root, script) => {
    writeFileSync(join(root, 'docusaurus.config.ts'), 'export default {};\n');
    const result = run(script);
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.match(result.stderr, /^navbar: docusaurus\.config\.ts has no readable baseUrl/m);
    assert.match(result.stderr, /^routes: sitemap\.xml is missing from the build/m);
  });
});

test('check-built with a build dir argument prints no routes line, and checks no routes (fixture builds and build-offline stay valid)', () => {
  withCopiedScripts(fullFiles(), (root, script) => {
    withBuild({'other/index.html': fullPage(0), 'other/stray.html': pageAt(4, {nav: false})}, (other) => {
      const result = run(script, [join(other, 'other')]);
      assert.equal(result.status, 0, result.stdout + result.stderr);
      assert.doesNotMatch(result.stdout, /routes/);
      assert.doesNotMatch(result.stderr, /routes/);
    });
  });
});
