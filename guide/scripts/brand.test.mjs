import {test} from 'node:test';
import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
import {readFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import {BRAND, FONT_STACK, brandErrors, restrictedTextErrors} from './rules/brand.mjs';
import {fontErrors, bannedFontSourceErrors} from './rules/fonts.mjs';
import {goodSiteFiles, goodFontFiles} from './lib/test-fixtures.mjs';

const site = goodSiteFiles();
const CSS = site['src/css/custom.css'];

const swap = (text, from, to) => {
  assert.ok(text.includes(from), `fixture lacks ${from}`);
  return text.replace(from, to);
};

test('brandErrors: the real custom.css has no error in light and dark mode', () => {
  assert.deepEqual(brandErrors({css: CSS, darkMode: true}), []);
  assert.deepEqual(brandErrors({css: CSS, darkMode: false}), []);
});

test('brandErrors: the seven brand values are the ones of decision D7', () => {
  assert.deepEqual(BRAND, {
    navy: '#1D252D',
    blue: '#3D7DC9',
    teal: '#014046',
    sage: '#809FA2',
    mist: '#CBD9DA',
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

test('brandErrors: the light primary colour must be teal', () => {
  const css = swap(CSS, '--ifm-color-primary: #014046;', '--ifm-color-primary: #2e8555;');
  assert.deepEqual(brandErrors({css, darkMode: true}), [
    'brand: light --ifm-color-primary must be #014046 (teal), found #2e8555',
  ]);
});

test('brandErrors: the dark primary colour must be mist, and only matters while dark mode is on', () => {
  const css = swap(CSS, '--ifm-color-primary: #CBD9DA;', '--ifm-color-primary: #25c2a0;');
  assert.deepEqual(brandErrors({css, darkMode: true}), [
    'brand: dark --ifm-color-primary must be #CBD9DA (mist), found #25c2a0',
  ]);
  assert.deepEqual(brandErrors({css, darkMode: false}), []);
});

test('brandErrors: a primary that is not declared is an error', () => {
  const css = swap(CSS, '--ifm-color-primary: #014046;', '--ifm-not-primary: #014046;');
  assert.match(brandErrors({css, darkMode: false}).join('\n'), /light --ifm-color-primary is not defined/);
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
    result({fgHex: '#3d7dc9'}),
    result({fgHex: '#809FA2', mode: 'dark'}),
    result({bgHex: '#3D7DC9', name: 'q'}),
  ]);
  assert.deepEqual(errors, [
    'brand: light p: text pair --a on --b uses blue #3D7DC9 as text, which is only for large or bold text and non-text accents',
    'brand: dark p: text pair --a on --b uses sage #809FA2 as text, which is decorative only',
    'brand: light q: text pair --a on --b uses blue #3D7DC9 as a background for text',
  ]);
});

test('restrictedTextErrors: non-text pairs may use blue and sage', () => {
  assert.deepEqual(restrictedTextErrors([result({kind: 'nontext', fgHex: '#3D7DC9'}), result({kind: 'nontext', bgHex: '#809FA2'})]), []);
});

test('restrictedTextErrors: other colours pass', () => {
  assert.deepEqual(restrictedTextErrors([result({})]), []);
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
