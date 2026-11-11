// Spec 14, T1: the built-HTML navbar rule (rules/navbar-built.mjs). The fixtures are the navbar of a real
// Pages build (minified, unquoted attributes) with mutants made by replacing one tag. Nothing here reads
// guide/build: the rule runs in `npm run check:built` on the default build dir only, and the entry-point
// tests prove that switch on fixture builds (a copy of the scripts folder whose ../build is a fixture).
import {test} from 'node:test';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {cpSync, mkdirSync, mkdtempSync, readFileSync, realpathSync, rmSync, writeFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {dirname, join} from 'node:path';
import {fileURLToPath} from 'node:url';
import {baseUrlOf, navbarErrors} from './rules/navbar-built.mjs';
import {scanBuilt} from './rules/built.mjs';
import {ROUTES, routeFile} from './rules/routes-built.mjs';

const SCRIPTS = dirname(fileURLToPath(import.meta.url));
const BASE = '/AgenticBookStore/';
const LOGO_OPEN =
  '<a href=https://openvalue.eu target=_blank rel="noopener noreferrer" class=guide-navbar-logo aria-label="OpenValue (opens in a new tab)">';
const LOGO_IMAGES =
  '<img src=/AgenticBookStore/img/openvalue-logo-dark.png alt="" class="themedComponent_mlkZ themedComponent--light_NVdE" height=32 />' +
  '<img src=/AgenticBookStore/img/openvalue-logo-light.png alt="" class="themedComponent_mlkZ themedComponent--dark_xIcU" height=32 />';
const BRAND_OPEN = '<a class=navbar__brand href=/AgenticBookStore/>';
const TITLE = '<b class="navbar__title text--truncate">Crafting your Agentic Harness</b>';
const GITHUB_OPEN =
  '<a href=https://github.com/MaikKingma/AgenticBookStore target=_blank rel="noopener noreferrer" class="navbar__item navbar__link">';
const LINKEDIN_OPEN =
  '<a href=https://www.linkedin.com/in/maik-kingma/ target=_blank rel="noopener noreferrer" class="navbar__item navbar__link">';
const ICON = '<svg width=13.5 height=13.5 aria-label="(opens in new tab)" class=iconExternalLink_nPIU><use href=#theme-svg-external-link /></svg>';
const TOGGLE = '<button aria-label="Toggle navigation bar" aria-expanded=false class="navbar__toggle clean-btn" type=button><svg width=30 height=30 aria-hidden=true><path d="M4 7h22"/></svg></button>';

// parts: pieces of the navbar to swap; everything else is the real structure.
function page({logo = LOGO_OPEN + LOGO_IMAGES + '</a>', brand = BRAND_OPEN + TITLE + '</a>', github, linkedin, nav = true} = {}) {
  const gh = github ?? `${GITHUB_OPEN}GitHub${ICON}</a>`;
  const li = linkedin ?? `${LINKEDIN_OPEN}Follow on LinkedIn${ICON}</a>`;
  const bar = `<nav aria-label=Main class="theme-layout-navbar navbar navbar--fixed-top"><div class=navbar__inner><div class="theme-layout-navbar-left navbar__items">${TOGGLE}${logo}${brand}</div><div class="theme-layout-navbar-right navbar__items navbar__items--right">${gh}${li}</div></div></nav>`;
  return `<!doctype html><html><head><title>Welcome</title></head><body><div id=__docusaurus>${nav ? bar : ''}<main><h1>Welcome</h1><p><a href=https://example.org target=_blank>Elsewhere</a></p></main></div></body></html>\n`;
}

const errorsOf = (html, file = 'index.html') => navbarErrors({file, html, base: BASE});

test('navbar rule: the navbar of a real build passes', () => {
  assert.deepEqual(errorsOf(page()), []);
});

test('navbar rule: errors name the file and start with "navbar:"', () => {
  const errors = errorsOf(page({nav: false}), 'reference/cheat-sheet.html');
  assert.equal(errors.length, 1);
  assert.match(errors[0], /^navbar: reference\/cheat-sheet\.html: /);
});

// --- the logo link ----------------------------------------------------------------------------

const logoWith = (open) => open + LOGO_IMAGES + '</a>';
const MUTANT_LOGO_OPENS = [
  ['a wrong href', LOGO_OPEN.replace('https://openvalue.eu', 'https://openvalue.com'), /logo link href must be exactly https:\/\/openvalue\.eu/],
  ['a trailing slash on the href', LOGO_OPEN.replace('openvalue.eu', 'openvalue.eu/'), /logo link href must be exactly/],
  ['http instead of https', LOGO_OPEN.replace('https://', 'http://'), /logo link href must be exactly/],
  ['no target', LOGO_OPEN.replace(' target=_blank', ''), /logo link target must be _blank/],
  ['target _self', LOGO_OPEN.replace('target=_blank', 'target=_self'), /logo link target must be _blank/],
  ['rel noopener only', LOGO_OPEN.replace('"noopener noreferrer"', 'noopener'), /logo link rel must hold noopener and noreferrer/],
  ['rel noreferrer only', LOGO_OPEN.replace('"noopener noreferrer"', 'noreferrer'), /logo link rel must hold noopener and noreferrer/],
  ['no rel', LOGO_OPEN.replace(' rel="noopener noreferrer"', ''), /logo link rel must hold noopener and noreferrer/],
  ['an empty aria-label', LOGO_OPEN.replace('aria-label="OpenValue (opens in a new tab)"', 'aria-label=""'), /logo link has no accessible name/],
  ['no aria-label', LOGO_OPEN.replace(' aria-label="OpenValue (opens in a new tab)"', ''), /logo link has no accessible name/],
];
for (const [what, open, message] of MUTANT_LOGO_OPENS) {
  test(`navbar rule: ${what} on the logo link fails`, () => {
    assert.ok(open !== LOGO_OPEN, 'the mutant differs from the good tag');
    const errors = errorsOf(page({logo: logoWith(open)}));
    assert.ok(errors.length >= 1 && errors.some((error) => message.test(error)), errors.join('\n'));
  });
}

// The href is the last attribute here, so the value ends at ">" and a trailing slash belongs to it
// (the shared scanner would drop it, which is why the rule reads the href itself).
test('navbar rule: a trailing slash on an unquoted logo href written last in the tag fails', () => {
  const tail = ' target=_blank rel="noopener noreferrer" aria-label=OpenValue href=https://openvalue.eu';
  assert.deepEqual(errorsOf(page({logo: `<a class=guide-navbar-logo${tail}>${LOGO_IMAGES}</a>`})), []);
  const errors = errorsOf(page({logo: `<a class=guide-navbar-logo${tail}/>${LOGO_IMAGES}</a>`}));
  assert.ok(errors.some((error) => /logo link href must be exactly https:\/\/openvalue\.eu \(found https:\/\/openvalue\.eu\/\)/.test(error)), errors.join('\n'));
});

test('navbar rule: the logo link may be named by the alt text of its image instead of an aria-label', () => {
  const open = LOGO_OPEN.replace(' aria-label="OpenValue (opens in a new tab)"', '');
  const images = LOGO_IMAGES.replace('alt=""', 'alt=OpenValue');
  assert.deepEqual(errorsOf(page({logo: open + images + '</a>'})), []);
});

test('navbar rule: the logo link rel is a token list, so extra tokens and any order pass but one joined token fails', () => {
  assert.deepEqual(errorsOf(page({logo: logoWith(LOGO_OPEN.replace('"noopener noreferrer"', '"noreferrer external noopener"'))})), []);
  const joined = errorsOf(page({logo: logoWith(LOGO_OPEN.replace('"noopener noreferrer"', 'noopener-noreferrer'))}));
  assert.ok(joined.some((error) => /logo link rel must hold noopener and noreferrer/.test(error)), joined.join('\n'));
});

test('navbar rule: a logo inside the title link fails', () => {
  const errors = errorsOf(page({logo: '', brand: BRAND_OPEN + logoWith(LOGO_OPEN) + TITLE + '</a>'}));
  assert.ok(errors.some((error) => /logo is inside the title link/.test(error)), errors.join('\n'));
});

test('navbar rule: a logo as an image inside the title link, with no logo link of its own, fails', () => {
  const errors = errorsOf(page({logo: '', brand: BRAND_OPEN + LOGO_IMAGES + TITLE + '</a>'}));
  assert.ok(errors.length >= 1, 'an error');
  assert.ok(errors.some((error) => /logo is inside the title link/.test(error)), errors.join('\n'));
});

test('navbar rule: a navbar without a logo link fails', () => {
  const errors = errorsOf(page({logo: ''}));
  assert.ok(errors.some((error) => /no logo link/.test(error)), errors.join('\n'));
});

test('navbar rule: the logo must come before the title', () => {
  const errors = errorsOf(page({logo: '', brand: BRAND_OPEN + TITLE + '</a>' + logoWith(LOGO_OPEN)}));
  assert.ok(errors.some((error) => /logo link must come before the title link/.test(error)), errors.join('\n'));
});

test('navbar rule: the logo link must be on the left side of the navbar', () => {
  const right = page({logo: ''}).replace(GITHUB_OPEN, logoWith(LOGO_OPEN) + GITHUB_OPEN);
  const errors = errorsOf(right);
  assert.ok(errors.some((error) => /logo link must be in the left part of the navbar/.test(error)), errors.join('\n'));
});

// Spec 14, T2 (FF-T1-1): two logo links are one too many; the rule names the count and nothing else.
test('navbar rule: two logo links in the left part of the navbar fail with the count, and only that', () => {
  const two = errorsOf(page({logo: logoWith(LOGO_OPEN) + logoWith(LOGO_OPEN)}));
  assert.deepEqual(two, ['navbar: index.html: more than one logo link in the navbar (found 2)']);
});

// --- the logo images: the right file per mode ------------------------------------------------------

test('navbar rule: swapped light and dark logo files fail (the slate logo belongs to light mode)', () => {
  const swapped = LOGO_IMAGES.replace('openvalue-logo-dark.png', 'TMP').replace('openvalue-logo-light.png', 'openvalue-logo-dark.png').replace('TMP', 'openvalue-logo-light.png');
  const errors = errorsOf(page({logo: LOGO_OPEN + swapped + '</a>'}));
  assert.equal(errors.filter((error) => /logo image/.test(error)).length, 2, errors.join('\n'));
});

test('navbar rule: a logo with only one mode image fails', () => {
  const errors = errorsOf(page({logo: LOGO_OPEN + LOGO_IMAGES.split('<img').slice(0, 2).join('<img') + '</a>'}));
  assert.ok(errors.some((error) => /dark mode logo image/.test(error)), errors.join('\n'));
});

test('navbar rule: a logo image that is not 32 high fails', () => {
  const errors = errorsOf(page({logo: LOGO_OPEN + LOGO_IMAGES.replaceAll('height=32', 'height=40') + '</a>'}));
  assert.equal(errors.filter((error) => /height 32/.test(error)).length, 2, errors.join('\n'));
});

test('navbar rule: a logo image path that is not under the base URL fails', () => {
  const errors = errorsOf(page({logo: LOGO_OPEN + LOGO_IMAGES.replaceAll('/AgenticBookStore/img', '/img') + '</a>'}));
  assert.equal(errors.filter((error) => /logo image/.test(error)).length, 2, errors.join('\n'));
});

// --- the title link --------------------------------------------------------------------------------

test('navbar rule: a title link with a target fails', () => {
  const errors = errorsOf(page({brand: BRAND_OPEN.replace('<a ', '<a target=_blank ') + TITLE + '</a>'}));
  assert.ok(errors.some((error) => /title link must not have a target/.test(error)), errors.join('\n'));
});

test('navbar rule: a title link that does not go to the base URL fails', () => {
  for (const href of ['/', '/AgenticBookStore', 'https://openvalue.eu', '/AgenticBookStore/setup-check']) {
    const errors = errorsOf(page({brand: `<a class=navbar__brand href=${href}>${TITLE}</a>`}));
    assert.ok(errors.some((error) => /title link href must be the base URL \/AgenticBookStore\//.test(error)), `${href}: ${errors.join('\n')}`);
  }
});

test('navbar rule: a missing title link or a changed title text fails', () => {
  assert.ok(errorsOf(page({brand: ''})).some((error) => /no title link/.test(error)));
  const errors = errorsOf(page({brand: BRAND_OPEN + TITLE.replace('Crafting', 'Building') + '</a>'}));
  assert.ok(errors.some((error) => /title must be "Crafting your Agentic Harness"/.test(error)), errors.join('\n'));
});

// --- GitHub and LinkedIn -----------------------------------------------------------------------------

const LINKS = [
  ['GitHub', GITHUB_OPEN, 'GitHub', 'https://github.com/MaikKingma/AgenticBookStore'],
  ['LinkedIn', LINKEDIN_OPEN, 'Follow on LinkedIn', 'https://www.linkedin.com/in/maik-kingma/'],
];

for (const [name, open, label, href] of LINKS) {
  const render = (tag, text = label) => page(name === 'GitHub' ? {github: `${tag}${text}${ICON}</a>`} : {linkedin: `${tag}${text}${ICON}</a>`});
  const failing = (what, html, message) =>
    test(`navbar rule: the ${name} link with ${what} fails`, () => {
      const errors = errorsOf(html);
      assert.ok(errors.some((error) => message.test(error)), errors.join('\n'));
    });
  failing('no target', render(open.replace(' target=_blank', '')), new RegExp(`${name} link target must be _blank`));
  failing('target _self', render(open.replace('target=_blank', 'target=_self')), new RegExp(`${name} link target must be _blank`));
  failing('rel noopener only', render(open.replace('"noopener noreferrer"', 'noopener')), new RegExp(`${name} link rel must hold noopener and noreferrer`));
  failing('rel noreferrer only', render(open.replace('"noopener noreferrer"', 'noreferrer')), new RegExp(`${name} link rel must hold noopener and noreferrer`));
  failing('one joined rel token', render(open.replace('"noopener noreferrer"', 'noopener-noreferrer')), new RegExp(`${name} link rel must hold noopener and noreferrer`));
  failing('no rel', render(open.replace(' rel="noopener noreferrer"', '')), new RegExp(`${name} link rel must hold noopener and noreferrer`));
  failing('no accessible name', render(open, ''), new RegExp(`${name} link has no accessible name`));
  failing('a wrong href', render(open.replace(href, `${href}x`)), new RegExp(`no ${name} link`));
  failing('another label', render(open, 'Elsewhere'), new RegExp(`${name} link has no accessible name`));
  test(`navbar rule: the ${name} link missing fails`, () => {
    const errors = errorsOf(page(name === 'GitHub' ? {github: ''} : {linkedin: ''}));
    assert.ok(errors.some((error) => new RegExp(`no ${name} link`).test(error)), errors.join('\n'));
  });
  test(`navbar rule: the ${name} link on the left side of the navbar fails`, () => {
    const moved = page(name === 'GitHub' ? {github: ''} : {linkedin: ''}).replace(BRAND_OPEN, `${open}${label}${ICON}</a>${BRAND_OPEN}`);
    const errors = errorsOf(moved);
    assert.ok(errors.some((error) => new RegExp(`${name} link must be in the right part of the navbar`).test(error)), errors.join('\n'));
  });
}

test('navbar rule: an aria-label names the link instead of its text', () => {
  const labelled = GITHUB_OPEN.replace('<a ', '<a aria-label="GitHub repository" ');
  assert.deepEqual(errorsOf(page({github: `${labelled}${ICON}</a>`})), []);
});

// --- page level: no navbar, scanBuilt ----------------------------------------------------------------

test('navbar rule: a page with no navbar fails', () => {
  const errors = errorsOf(page({nav: false}));
  assert.deepEqual(errors, ['navbar: index.html: no navbar (a nav element with the class navbar) found']);
});

function withBuild(files, fn) {
  const root = realpathSync(mkdtempSync(join(tmpdir(), 'guide-navbar-test-')));
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

test('scanBuilt: without the navbar option a page with no navbar passes (the existing fixture pages stay valid)', () => {
  withBuild({'build/index.html': page({nav: false})}, (root) => {
    assert.deepEqual(scanBuilt(join(root, 'build')).errors, []);
  });
});

test('scanBuilt: with the navbar option every page is checked and the count is returned', () => {
  withBuild({'build/index.html': page(), 'build/reference/cheat-sheet.html': page(), 'build/404.html': page({github: ''})}, (root) => {
    const result = scanBuilt(join(root, 'build'), {navbar: {base: BASE}});
    assert.equal(result.navbarPages, 3);
    assert.equal(result.errors.length, 1, result.errors.join('\n'));
    assert.match(result.errors[0], /^navbar: 404\.html: no GitHub link/);
  });
});

test('scanBuilt: with the navbar option a build with no html file still fails once (anti-vacuity)', () => {
  withBuild({'build/assets/main.js': 'x'}, (root) => {
    const result = scanBuilt(join(root, 'build'), {navbar: {base: BASE}});
    assert.equal(result.errors.length, 1);
    assert.match(result.errors[0], /no \.html file found/);
  });
});

test('baseUrlOf: reads the baseUrl of the real config', () => {
  assert.equal(baseUrlOf(readFileSync(join(SCRIPTS, '..', 'docusaurus.config.ts'), 'utf8')), BASE);
  assert.equal(baseUrlOf("export default {\n  baseUrl: '/x/',\n};\n"), '/x/');
  assert.equal(baseUrlOf('export default {};'), undefined);
});

// --- the entry point: the default build dir switches the rule on --------------------------------------
// A copy of the scripts folder sits beside a fixture build and a fixture config, so check-built.mjs
// resolves its default build dir (../build) to the fixture and never to guide/build.

function withCopiedScripts(build, fn) {
  const files = {'docusaurus.config.ts': "export default {\n  baseUrl: '/AgenticBookStore/',\n};\n"};
  for (const [rel, content] of Object.entries(build)) files[`build/${rel}`] = content;
  return withBuild(files, (root) => {
    cpSync(SCRIPTS, join(root, 'scripts'), {recursive: true, filter: (source) => !source.endsWith('.test.mjs')});
    return fn(root, join(root, 'scripts', 'check-built.mjs'));
  });
}

const run = (script, args = []) => spawnSync(process.execPath, [script, ...args], {encoding: 'utf8'});

// Spec 14, T2 (FF-T1-2): the default build dir also runs the level progress rule (rules/level-progress-built.mjs),
// which needs the five level pages with their breadcrumb. These fixture builds gain them (page() plus the nav).
function levelProgressNav(current) {
  const routes = ['level-0-break-the-agent', 'level-1-context-engineering', 'level-2-give-your-agent-eyes', 'level-3-behavioral-control', 'level-4-spec-driven-development'];
  const crumbs = routes.map((route, level) => {
    const state = level < current ? 'done' : level === current ? 'current' : 'upcoming';
    const check = state === 'done' ? '<span aria-hidden=true data-part=check></span>' : '';
    const done = state === 'done' ? '<span> (completed)</span>' : '';
    const marker = state === 'current' ? ' aria-current=step' : '';
    return `<li data-state=${state}><a${marker} href=/AgenticBookStore/${route}>${check}<span>Level ${level}</span>${done}</a>`;
  });
  return `<nav aria-label="Level progress"><ol>${crumbs.join('')}</ol></nav>`;
}
const LEVEL_PAGES = Object.fromEntries(
  [0, 1, 2, 3, 4].map((level) => [
    routeFile(ROUTES[level + 2]),
    page().replace('<main><h1>Welcome</h1>', `<main><h1>Welcome</h1>${levelProgressNav(level)}`),
  ]),
);

// Spec 16, T1 (G-1, option A): the default build dir also runs the routes rule (rules/routes-built.mjs), which needs
// the sitemap and the 11 route files with their pagination nav. These fixture builds gain them: ROUTE_PAGES is the
// whole set of route files (the level pages keep their breadcrumb, every page gets its previous and next link in
// the chain order) and the sitemap. The level pages, index.html and setup-check.html are built from page() as before.
const chainHref = (index) => new URL(ROUTES[index]).pathname;
function withPagination(html, index) {
  const link = (kind, target) =>
    `<a class="pagination-nav__link pagination-nav__link--${kind}" href=${chainHref(target)}><div class=pagination-nav__sublabel>${kind}</div><div class=pagination-nav__label>Label</div></a>`;
  const links = (index > 0 ? link('prev', index - 1) : '') + (index < ROUTES.length - 1 ? link('next', index + 1) : '');
  return html.replace('</main>', `<nav class="docusaurus-mt-lg pagination-nav" aria-label="Docs pages">${links}</nav></main>`);
}
const SITEMAP =
  '<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">' +
  ROUTES.map((loc) => `<url><loc>${loc}</loc><changefreq>weekly</changefreq><priority>0.5</priority></url>`).join('') +
  '</urlset>';
const ROUTE_PAGES = {
  'sitemap.xml': SITEMAP,
  ...Object.fromEntries(
    ROUTES.map((loc, index) => [routeFile(loc), withPagination(LEVEL_PAGES[routeFile(loc)] ?? page(), index)]),
  ),
};

test('check-built with no argument checks the default build dir and runs the navbar rule', () => {
  withCopiedScripts(ROUTE_PAGES, (root, script) => {
    const result = run(script);
    assert.equal(result.status, 0, result.stdout + result.stderr);
    assert.match(result.stdout, /^ok {3}navbar: 11 pages, /m);
    assert.match(result.stdout, /^ok {3}built: 11 html files/m);
    assert.match(result.stdout, /^ok {3}routes: 11 routes in the sitemap and the build, prev\/next chain of 11 pages$/m);
  });
});

test('check-built with no argument fails on a default build whose navbar is wrong', () => {
  withCopiedScripts({...ROUTE_PAGES, 'setup-check.html': withPagination(page({github: ''}), 1)}, (root, script) => {
    const result = run(script);
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.match(result.stderr, /navbar: setup-check\.html: no GitHub link/);
    assert.match(result.stderr, /check:built failed with 1 error/);
  });
});

test('check-built with no argument fails on a default build with no navbar at all', () => {
  withCopiedScripts({'index.html': page({nav: false})}, (root, script) => {
    const result = run(script);
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.match(result.stderr, /navbar: index\.html: no navbar/);
  });
});

test('check-built with a build dir argument does not run the navbar rule (fixture pages stay valid)', () => {
  withCopiedScripts({'index.html': page()}, (root, script) => {
    withBuild({'other/index.html': page({nav: false})}, (other) => {
      const result = run(script, [join(other, 'other')]);
      assert.equal(result.status, 0, result.stdout + result.stderr);
      assert.doesNotMatch(result.stdout, /navbar/);
    });
  });
});

test('check-built with no argument and no readable baseUrl fails instead of skipping the rule', () => {
  withCopiedScripts({'index.html': page()}, (root, script) => {
    writeFileSync(join(root, 'docusaurus.config.ts'), 'export default {};\n');
    const result = run(script);
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.match(result.stderr, /navbar: .*baseUrl/);
  });
});

test('check-built with no argument and no default build dir fails (nothing to skip to)', () => {
  withBuild({'docusaurus.config.ts': "export default {\n  baseUrl: '/AgenticBookStore/',\n};\n"}, (root) => {
    cpSync(SCRIPTS, join(root, 'scripts'), {recursive: true, filter: (source) => !source.endsWith('.test.mjs')});
    const result = run(join(root, 'scripts', 'check-built.mjs'));
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.match(result.stderr, /built: .* is missing; run npm run build first/);
  });
});

test('the real config holds the title and the two external links the navbar rule asserts', () => {
  const config = readFileSync(join(SCRIPTS, '..', 'docusaurus.config.ts'), 'utf8');
  assert.ok(config.includes("title: 'Crafting your Agentic Harness'"));
  assert.ok(config.includes("href: 'https://github.com/MaikKingma/AgenticBookStore'"));
  assert.ok(config.includes("href: 'https://www.linkedin.com/in/maik-kingma/'"));
});
