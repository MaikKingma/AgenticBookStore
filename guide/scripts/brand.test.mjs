import {test} from 'node:test';
import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
import {readFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import {BRAND, FONT_STACK, brandErrors, restrictedTextErrors} from './rules/brand.mjs';
import {fontErrors, bannedFontSourceErrors} from './rules/fonts.mjs';
import {evaluatePairs} from './rules/contrast.mjs';
import {CONTRAST_PAIRS} from './rules/contrast-pairs.mjs';
import {goodSiteFiles, goodFontFiles} from './lib/test-fixtures.mjs';

const site = goodSiteFiles();
const CSS = site['src/css/custom.css'];
const same = (a, b) => a.toUpperCase() === b.toUpperCase();

const swap = (text, from, to) => {
  assert.ok(text.includes(from), `fixture lacks ${from}`);
  return text.replace(from, to);
};

test('brandErrors: the real custom.css has no error in light and dark mode', () => {
  assert.deepEqual(brandErrors({css: CSS, darkMode: true}), []);
  assert.deepEqual(brandErrors({css: CSS, darkMode: false}), []);
});

test('brandErrors: the brand values are the ones of decision D18 (deck blue and slate, with the D7 neutrals sage, snow and white)', () => {
  assert.deepEqual(BRAND, {
    blue: '#3C7EC9',
    slate: '#1D242C',
    sage: '#809FA2',
    snow: '#F1F5F6',
    white: '#FFFFFF',
  });
});

test('brandErrors: a missing brand value fails and names the colour', () => {
  const css = swap(CSS, '--ov-sage: #809FA2;', '--ov-sage: #809FA3;');
  assert.deepEqual(brandErrors({css, darkMode: true}), ['brand: brand value #809FA2 (sage) is missing from custom.css']);
});

test('brandErrors: brand values are matched in any letter case', () => {
  const css = swap(CSS, '--ov-sage: #809FA2;', '--ov-sage: #809fa2;');
  assert.deepEqual(brandErrors({css, darkMode: true}), []);
});

test('brandErrors: the light primary colour must be the AA shade #3371B8 of the deck blue', () => {
  const css = swap(CSS, '--ifm-color-primary: #3371B8;', '--ifm-color-primary: #2e8555;');
  assert.deepEqual(brandErrors({css, darkMode: true}), [
    'brand: light --ifm-color-primary must be #3371B8 (the AA shade of the deck blue), found #2e8555',
  ]);
});

test('brandErrors: the dark primary colour must be the lighter AA shade #79A7DA, and only matters while dark mode is on', () => {
  const css = swap(CSS, '--ifm-color-primary: #79A7DA;', '--ifm-color-primary: #25c2a0;');
  assert.deepEqual(brandErrors({css, darkMode: true}), [
    'brand: dark --ifm-color-primary must be #79A7DA (the lighter AA shade of the deck blue), found #25c2a0',
  ]);
  assert.deepEqual(brandErrors({css, darkMode: false}), []);
});

test('brandErrors: a primary that is not declared is an error', () => {
  const css = swap(CSS, '--ifm-color-primary: #3371B8;', '--ifm-not-primary: #3371B8;');
  assert.match(brandErrors({css, darkMode: false}).join('\n'), /light --ifm-color-primary is not defined/);
});

// Spec 14 T3 (A1, FF-T1-6): the four primaries that must fail, each with its exact message. The exact deck blue
// #3C7EC9 is only 4.18:1 on white and 3.74:1 on slate, so it may never be the primary text colour.
test('brandErrors: the light primary restored to the old teal #014046 fails', () => {
  const css = swap(CSS, '--ifm-color-primary: #3371B8;', '--ifm-color-primary: #014046;');
  assert.deepEqual(brandErrors({css, darkMode: true}), [
    'brand: light --ifm-color-primary must be #3371B8 (the AA shade of the deck blue), found #014046',
  ]);
});

test('brandErrors: the light primary set to the exact deck blue #3C7EC9 fails (any blue is not enough)', () => {
  const css = swap(CSS, '--ifm-color-primary: #3371B8;', '--ifm-color-primary: #3C7EC9;');
  assert.deepEqual(brandErrors({css, darkMode: true}), [
    'brand: light --ifm-color-primary must be #3371B8 (the AA shade of the deck blue), found #3C7EC9',
  ]);
});

test('brandErrors: the dark primary set to the exact deck blue #3C7EC9 fails', () => {
  const css = swap(CSS, '--ifm-color-primary: #79A7DA;', '--ifm-color-primary: #3C7EC9;');
  assert.deepEqual(brandErrors({css, darkMode: true}), [
    'brand: dark --ifm-color-primary must be #79A7DA (the lighter AA shade of the deck blue), found #3C7EC9',
  ]);
});

test('brandErrors: the dark primary swapped with the light value fails, and so does the light one swapped with the dark value', () => {
  const dark = swap(CSS, '--ifm-color-primary: #79A7DA;', '--ifm-color-primary: #3371B8;');
  assert.deepEqual(brandErrors({css: dark, darkMode: true}), [
    'brand: dark --ifm-color-primary must be #79A7DA (the lighter AA shade of the deck blue), found #3371B8',
  ]);
  const light = swap(CSS, '--ifm-color-primary: #3371B8;', '--ifm-color-primary: #79A7DA;');
  assert.deepEqual(brandErrors({css: light, darkMode: true}), [
    'brand: light --ifm-color-primary must be #3371B8 (the AA shade of the deck blue), found #79A7DA',
  ]);
});

// The six shade variables of each mode are the Docusaurus ratios of the primary and stay ordered by luminance.
test('brandErrors: the six primary shades stay ordered, darkest to lightest, in each mode', () => {
  const tail = 'must run darkest, darker, dark, primary, light, lighter, lightest from dark to light';
  const lightDark = swap(CSS, '--ifm-color-primary-dark: #2E66A6;', '--ifm-color-primary-dark: #4383CB;');
  assert.deepEqual(brandErrors({css: lightDark, darkMode: true}), [
    `brand: light primary shades ${tail} (--ifm-color-primary-dark is not darker than --ifm-color-primary)`,
  ]);
  const darkLightest = swap(CSS, '--ifm-color-primary-lightest: #C9DBF0;', '--ifm-color-primary-lightest: #79A7DA;');
  assert.deepEqual(brandErrors({css: darkLightest, darkMode: true}), [
    `brand: dark primary shades ${tail} (--ifm-color-primary-lighter is not darker than --ifm-color-primary-lightest)`,
  ]);
  assert.deepEqual(brandErrors({css: darkLightest, darkMode: false}), []);
});

// A second dark block after the real one: its declaration is the last one, so it wins like in the cascade.
const withDark = (name, value) => `${CSS}\nhtml[data-theme='dark'] {\n  ${name}: ${value};\n}\n`;

// Dark mode (G-4): the page, the navbar and the sidebar are the deck slate; every other surface is a named
// --ov-slate-<step> token, so a surface cannot drift to a colour of its own.
test('brandErrors: the dark page, navbar and sidebar backgrounds must resolve to the deck slate #1D242C', () => {
  for (const name of ['--ifm-background-color', '--ifm-background-surface-color', '--ifm-navbar-background-color']) {
    const css = withDark(name, 'var(--ov-snow)');
    assert.deepEqual(brandErrors({css, darkMode: true}), [`brand: dark ${name} must resolve to the deck slate #1D242C, found #F1F5F6`]);
  }
});

test('brandErrors: every dark surface variable is a named --ov-slate-<step> token', () => {
  for (const name of [
    '--ifm-code-background',
    '--prism-background-color',
    '--docusaurus-highlighted-code-line-bg',
    '--ifm-color-secondary-contrast-background',
    '--ifm-color-success-contrast-background',
    '--ifm-color-info-contrast-background',
    '--ifm-color-warning-contrast-background',
    '--ifm-color-danger-contrast-background',
    '--ifm-table-stripe-background',
    '--ifm-hover-overlay',
  ]) {
    const css = withDark(name, 'var(--ov-snow)');
    assert.deepEqual(brandErrors({css, darkMode: true}), [
      `brand: dark ${name} must be a named slate token var(--ov-slate-<step>), found "var(--ov-snow)"`,
    ]);
    assert.deepEqual(brandErrors({css, darkMode: false}), []);
  }
});

test('brandErrors: a dark surface that is not declared in the dark block is an error', () => {
  const css = swap(CSS, '--ifm-table-stripe-background: var(--ov-slate-800);', '--ifm-table-stripe-colour: var(--ov-slate-800);');
  assert.deepEqual(brandErrors({css, darkMode: true}), ['brand: dark --ifm-table-stripe-background is not declared in the dark block']);
});

test('brandErrors: the light heading colour must resolve to the deck slate #1D242C', () => {
  const css = swap(CSS, '--ifm-heading-color: var(--ov-slate);', '--ifm-heading-color: var(--ov-sage);');
  assert.deepEqual(brandErrors({css, darkMode: true}), ['brand: light --ifm-heading-color must resolve to the deck slate #1D242C, found #809FA2']);
});

// Admonition borders (G-4): the -dark variable of each bordered type is the exact deck blue, one of the six shades
// of the primary of its mode, or a named neutral (slate, white, snow, sage), never another colour.
test('brandErrors: an admonition border that is neither the deck blue, a primary shade nor a named neutral fails, in each mode', () => {
  const light = swap(CSS, '--ifm-color-info-dark: var(--ov-blue);', '--ifm-color-info-dark: #2E8555;');
  assert.deepEqual(brandErrors({css: light, darkMode: true}), [
    'brand: light --ifm-color-info-dark (admonition border) must be the deck blue #3C7EC9, a shade of the primary or a named neutral, found #2E8555',
  ]);
  const dark = swap(CSS, '--ifm-color-info-dark: var(--ifm-color-primary);', '--ifm-color-info-dark: #2E8555;');
  assert.deepEqual(brandErrors({css: dark, darkMode: true}), [
    'brand: dark --ifm-color-info-dark (admonition border) must be the deck blue #3C7EC9, a shade of the primary or a named neutral, found #2E8555',
  ]);
  assert.deepEqual(brandErrors({css: dark, darkMode: false}), []);
});

// The slate tokens and every colour of the Brand section of the README are declared in custom.css, so the README
// cannot name a colour the stylesheet does not hold (and the stylesheet cannot grow a slate token unlisted).
test('brand README: every --ov-slate-<step> token of custom.css is listed in the Brand section, and every hex there is in custom.css', () => {
  const readme = site['README.md'];
  const brand = readme.slice(readme.indexOf('\n## Brand'), readme.indexOf('\n### Fonts and provenance'));
  assert.ok(brand.length > 500, 'the Brand section was not found');
  const tokens = [...new Set(CSS.match(/--ov-slate-\d+(?=:)/g))];
  assert.ok(tokens.length >= 5, tokens.join(' '));
  for (const token of tokens) assert.ok(brand.includes(`\`${token}\``), `${token} is not listed in the README Brand section`);
  const declared = CSS.toUpperCase();
  for (const hex of new Set(brand.match(/#[0-9A-Fa-f]{6}\b/g))) {
    assert.ok(declared.includes(hex.toUpperCase()), `${hex} is in the README Brand section but not in custom.css`);
  }
});

test('brandErrors: the font stack must be exactly the one of decision D7', () => {
  assert.equal(FONT_STACK, 'Montserrat, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif');
  const css = swap(CSS, 'Montserrat, system-ui,', 'Montserrat, serif,');
  const errors = brandErrors({css, darkMode: true});
  assert.equal(errors.length, 1);
  assert.match(errors[0], /^brand: --ifm-font-family-base must be exactly "Montserrat, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif", found "Montserrat, serif,/);
});

test('brandErrors: a missing font stack variable is an error', () => {
  const css = swap(CSS, '--ifm-font-family-base:', '--ifm-font-family-other:');
  assert.match(brandErrors({css, darkMode: true}).join('\n'), /--ifm-font-family-base is not defined/);
});

const result = (extra) => ({mode: 'light', name: 'p', fg: '--a', bg: '--b', kind: 'text', fgHex: '#000000', bgHex: '#ffffff', ...extra});

test('restrictedTextErrors: blue and sage never carry normal text, in any case, as text or as its background', () => {
  const errors = restrictedTextErrors([
    result({fgHex: '#3c7ec9'}),
    result({fgHex: '#809FA2', mode: 'dark'}),
    result({bgHex: '#3C7EC9', name: 'q'}),
  ]);
  assert.deepEqual(errors, [
    'brand: light p: text pair --a on --b uses blue #3C7EC9 as text, which is only for non-text accents',
    'brand: dark p: text pair --a on --b uses sage #809FA2 as text, which is decorative only',
    'brand: light q: text pair --a on --b uses blue #3C7EC9 as a background for text',
  ]);
});

test('restrictedTextErrors: sage as the background of a text pair is an error of its own', () => {
  assert.deepEqual(restrictedTextErrors([result({bgHex: '#809fa2', name: 's'})]), [
    'brand: light s: text pair --a on --b uses sage #809FA2 as a background for text',
  ]);
});

test('restrictedTextErrors: non-text pairs may use blue and sage', () => {
  assert.deepEqual(restrictedTextErrors([result({kind: 'nontext', fgHex: '#3C7EC9'}), result({kind: 'nontext', bgHex: '#809FA2'})]), []);
});

test('restrictedTextErrors: other colours pass', () => {
  assert.deepEqual(restrictedTextErrors([result({})]), []);
});

// Spec 14 T3 (A1): the exact deck blue is for borders, bars, outlines and non-text pairs only. In the real stylesheet
// no text pair resolves to it, as text or as background, in light or in dark mode; where it is used it is a non-text pair.
test('restrictedTextErrors: no real text pair uses the exact deck blue, and the non-text pairs that use it are named', () => {
  const {results, errors} = evaluatePairs({css: CSS, pairs: CONTRAST_PAIRS, darkMode: true});
  assert.deepEqual(errors, []);
  assert.deepEqual(restrictedTextErrors(results), []);
  const exact = results.filter((pair) => same(pair.fgHex, BRAND.blue) || same(pair.bgHex, BRAND.blue));
  assert.ok(exact.length > 0, 'the deck blue is used by no pair at all: the guard below proves nothing');
  assert.deepEqual([...new Set(exact.map((pair) => pair.kind))], ['nontext']);
  assert.deepEqual([...new Set(exact.map((pair) => pair.name))].toSorted(), ['admonition danger border', 'admonition info border', 'navbar accent line']);
  const asText = exact.map((pair) => ({...pair, kind: 'text'}));
  assert.equal(restrictedTextErrors(asText).length, asText.length);
});

const fontFiles = () => ({
  'montserrat-latin-400.woff2': Buffer.from('four'),
  'montserrat-latin-600.woff2': Buffer.from('six'),
  'montserrat-latin-700.woff2': Buffer.from('seven'),
});
const sha = (buffer) => createHash('sha256').update(buffer).digest('hex');
const readmeFor = (files) =>
  Object.entries(files)
    .map(([name, buffer]) => `| ${name} | https://example.invalid/${name} | ${sha(buffer)} |`)
    .join('\n');

test('fontErrors: the real stylesheet, files, provenance and licence are consistent', () => {
  assert.deepEqual(
    fontErrors({css: CSS, files: goodFontFiles(), readme: site['README.md'], hasLicense: Boolean(site['static/fonts/OFL.txt'])}),
    [],
  );
});

test('fontErrors: three Montserrat faces, weights 400, 600 and 700, swap, relative woff2 urls', () => {
  const css = CSS;
  const errors = fontErrors({css, files: fontFiles(), readme: readmeFor(fontFiles()), hasLicense: true});
  assert.deepEqual(errors, []);
});

test('fontErrors: a missing weight, a missing swap and a CDN url each fail', () => {
  const files = fontFiles();
  const readme = readmeFor(files);
  const noWeight = swap(CSS, 'font-weight: 600;', 'font-weight: 500;');
  assert.match(fontErrors({css: noWeight, files, readme, hasLicense: true}).join('\n'), /weights 400, 600 and 700/);
  const noSwap = swap(CSS, 'font-display: swap;', 'font-display: block;');
  assert.match(fontErrors({css: noSwap, files, readme, hasLicense: true}).join('\n'), /font-display: swap/);
  const cdn = swap(CSS, "url('./fonts/montserrat-latin-400.woff2')", "url('https://example.invalid/a.woff2')");
  assert.match(fontErrors({css: cdn, files, readme, hasLicense: true}).join('\n'), /url\('\.\/fonts\/<file>\.woff2'\) format\('woff2'\)/);
});

test('fontErrors: a referenced font file that does not exist fails', () => {
  const files = fontFiles();
  delete files['montserrat-latin-600.woff2'];
  const errors = fontErrors({css: CSS, files, readme: readmeFor(files), hasLicense: true});
  assert.ok(errors.includes('fonts: custom.css references ./fonts/montserrat-latin-600.woff2 but the file is missing'), errors.join(' | '));
});

test('fontErrors: a font file whose sha256 is not in the README provenance fails', () => {
  const files = fontFiles();
  const readme = readmeFor(files).replace(sha(files['montserrat-latin-700.woff2']), 'f'.repeat(64));
  assert.deepEqual(fontErrors({css: CSS, files, readme, hasLicense: true}), [
    `fonts: README.md has no sha256 ${sha(files['montserrat-latin-700.woff2'])} for montserrat-latin-700.woff2`,
  ]);
});

test('fontErrors: a missing README or licence fails', () => {
  const files = fontFiles();
  assert.match(fontErrors({css: CSS, files, readme: undefined, hasLicense: true}).join('\n'), /README\.md is missing/);
  assert.deepEqual(fontErrors({css: CSS, files, readme: readmeFor(files), hasLicense: false}), [
    'fonts: static/fonts/OFL.txt is missing',
  ]);
});

// The banned names are built from pieces, so that no file in guide/ holds them whole
// (the rule is "these strings appear nowhere in guide/", the checker included).
const CDN_API = 'fonts.' + 'googleapis';
const CDN_FILES = 'g' + 'static';
const FONT_PACKAGE = '@font' + 'source';

test('bannedFontSourceErrors: font CDNs and the font package are errors in any file', () => {
  const files = [
    {file: 'src/css/x.css', text: `@import url(https://${CDN_API}.com/css2?family=Montserrat);\n`},
    {file: 'docs/a.md', text: `from fonts.${CDN_FILES}.com\n`},
    {file: 'docusaurus.config.ts', text: `stylesheets: ['${FONT_PACKAGE}/montserrat']\n`},
    {file: 'docs/clean.md', text: 'plain\n'},
  ];
  const errors = bannedFontSourceErrors(files);
  const tail = 'fonts are self-hosted, no CDN and no font package';
  assert.deepEqual(errors, [
    `fonts: src/css/x.css:1 mentions "${CDN_API}"; ${tail}`,
    `fonts: docs/a.md:1 mentions "${CDN_FILES}"; ${tail}`,
    `fonts: docusaurus.config.ts:1 mentions "${FONT_PACKAGE}"; ${tail}`,
  ]);
});

test('bannedFontSourceErrors: this self-test file does not hold a banned name whole', () => {
  const own = readFileSync(fileURLToPath(import.meta.url), 'utf8');
  for (const name of [CDN_API, CDN_FILES, FONT_PACKAGE]) assert.equal(own.includes(name), false, name);
});
