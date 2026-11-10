import {test} from 'node:test';
import assert from 'node:assert/strict';
import {
  parseHex,
  contrastRatio,
  cssVariables,
  resolveColor,
  evaluatePairs,
  colorModeErrors,
} from './rules/contrast.mjs';

const two = (n) => n.toFixed(2);

test('contrastRatio: white on black is 21.00 in either order', () => {
  assert.equal(two(contrastRatio('#ffffff', '#000000')), '21.00');
  assert.equal(two(contrastRatio('#000000', '#ffffff')), '21.00');
});

test('contrastRatio: 4.5 witnesses, #767676 passes on white and #777777 does not', () => {
  assert.equal(two(contrastRatio('#767676', '#ffffff')), '4.54');
  assert.equal(two(contrastRatio('#777777', '#ffffff')), '4.48');
});

test('contrastRatio: the brand blue is 4.22 on white, so it is no normal text colour', () => {
  assert.equal(two(contrastRatio('#3D7DC9', '#FFFFFF')), '4.22');
});

test('contrastRatio: navy on white is 15.50 and white on navy is the same', () => {
  assert.equal(two(contrastRatio('#1D252D', '#FFFFFF')), '15.50');
  assert.equal(two(contrastRatio('#FFFFFF', '#1D252D')), '15.50');
});

test('contrastRatio: 3.0 witnesses on both sides of the non-text threshold', () => {
  assert.equal(two(contrastRatio('#949494', '#ffffff')), '3.03');
  assert.equal(two(contrastRatio('#959595', '#ffffff')), '3.00');
  assert.ok(contrastRatio('#949494', '#ffffff') >= 3);
  assert.ok(contrastRatio('#959595', '#ffffff') < 3);
});

test('parseHex: 3, 4, 6 and 8 digit forms, any case', () => {
  assert.deepEqual(parseHex('#fff'), {r: 255, g: 255, b: 255});
  assert.deepEqual(parseHex('#1d252D'), {r: 29, g: 37, b: 45});
  assert.deepEqual(parseHex('#f0a'), {r: 255, g: 0, b: 170});
});

test('parseHex: anything that is not an opaque hex colour is null', () => {
  for (const value of ['#fffa', '#11223344', 'red', 'rgb(0 0 0)', '#12', '', 'var(--x)']) {
    assert.equal(parseHex(value), null, value);
  }
});

test('contrastRatio: a 3-digit and a mixed-case hex give the same ratio as the long lower-case form', () => {
  assert.equal(contrastRatio('#fff', '#000'), contrastRatio('#ffffff', '#000000'));
  assert.equal(contrastRatio('#1D252d', '#fFfFfF'), contrastRatio('#1d252d', '#ffffff'));
});

const CSS = `
/* a comment with :root { --ghost: #123456; } inside */
:root {
  --a: #111111;
  --b: var(--a);
  --c: var(--b);
  --light-only: #eeeeee;
}
.alert--info { --nope: #abcdef; }
html[data-theme='dark'] {
  --a: #fafafa;
  --dark-only: #010101;
}
:root { --late: #222222; --a: #333333; }
`;

test('cssVariables: reads :root and the dark block, ignores comments and other selectors', () => {
  const {light, dark, hasDark} = cssVariables(CSS);
  assert.equal(hasDark, true);
  assert.equal(light.get('--light-only'), '#eeeeee');
  assert.equal(light.has('--ghost'), false);
  assert.equal(light.has('--nope'), false);
  assert.equal(dark.get('--dark-only'), '#010101');
  assert.equal(light.has('--dark-only'), false);
});

test('cssVariables: a later :root block adds variables and overrides earlier ones', () => {
  const {light} = cssVariables(CSS);
  assert.equal(light.get('--late'), '#222222');
  assert.equal(light.get('--a'), '#333333');
});

test('cssVariables: accepts [data-theme="dark"] with double quotes and reports no dark block when absent', () => {
  assert.equal(cssVariables('[data-theme="dark"] { --a: #000000; }').hasDark, true);
  assert.equal(cssVariables(':root { --a: #000000; }').hasDark, false);
});

test('resolveColor: follows var() chains two levels deep', () => {
  const {light} = cssVariables(':root { --a: #111111; --b: var(--a); --c: var(--b); }');
  assert.deepEqual(resolveColor('--c', light), {hex: '#111111'});
});

test('resolveColor: names a missing variable, a cycle and a value that is not an opaque hex colour', () => {
  const {light} = cssVariables(':root { --a: var(--missing); --x: var(--y); --y: var(--x); --n: red; --al: #11223344; }');
  assert.match(resolveColor('--a', light).error, /--missing is not defined/);
  assert.match(resolveColor('--nothing', light).error, /--nothing is not defined/);
  assert.match(resolveColor('--x', light).error, /cycle/);
  assert.match(resolveColor('--n', light).error, /--n.*"red".*opaque hex/);
  assert.match(resolveColor('--al', light).error, /--al.*"#11223344".*opaque hex/);
});

const PAIRS = [
  {name: 'body text', fg: '--fg', bg: '--bg', kind: 'text'},
  {name: 'accent', fg: '--accent', bg: '--bg', kind: 'nontext'},
];

test('evaluatePairs: prints every pair with its ratio, light and dark, dark values overriding light', () => {
  const css =
    ":root { --fg: #000000; --bg: #ffffff; --accent: #949494; } html[data-theme='dark'] { --fg: #ffffff; --bg: #000000; }";
  const {lines, errors, results} = evaluatePairs({css, pairs: PAIRS, darkMode: true});
  assert.deepEqual(errors, []);
  assert.equal(results.length, 4);
  assert.deepEqual(lines, [
    'ok   contrast light body text: --fg on --bg 21.00 (min 4.5)',
    'ok   contrast light accent: --accent on --bg 3.03 (min 3.0)',
    'ok   contrast dark body text: --fg on --bg 21.00 (min 4.5)',
    'ok   contrast dark accent: --accent on --bg 6.92 (min 3.0)',
  ]);
});

test('evaluatePairs: a text pair below 4.5 fails, a non-text pair below 3.0 fails', () => {
  const css = ':root { --fg: #777777; --bg: #ffffff; --accent: #959595; }';
  const {errors, lines} = evaluatePairs({css, pairs: PAIRS, darkMode: false});
  assert.deepEqual(errors, [
    'contrast: light body text: --fg on --bg is 4.48:1, below 4.5',
    'contrast: light accent: --accent on --bg is 3.00:1, below 3.0',
  ]);
  assert.ok(lines.every((line) => line.startsWith('FAIL contrast')));
});

test('evaluatePairs: a ratio equal to the minimum passes (the boundary is inclusive)', () => {
  const css = ':root { --fg: #000000; --bg: #ffffff; --accent: #000000; }';
  const {errors} = evaluatePairs({css, pairs: PAIRS, darkMode: false, minimums: {text: 21, nontext: 21}});
  assert.deepEqual(errors, []);
});

test('evaluatePairs: with dark mode off only the light mode is evaluated', () => {
  const css = ":root { --fg: #000000; --bg: #ffffff; --accent: #000000; } html[data-theme='dark'] { --fg: #111111; --bg: #000000; }";
  const {results} = evaluatePairs({css, pairs: PAIRS, darkMode: false});
  assert.deepEqual(results.map((r) => r.mode), ['light', 'light']);
});

test('evaluatePairs: a dark value that fails is caught, and the light value cannot hide it', () => {
  const css =
    ":root { --fg: #000000; --bg: #ffffff; --accent: #000000; } html[data-theme='dark'] { --fg: #111111; --bg: #000000; --accent: #ffffff; }";
  const {errors} = evaluatePairs({css, pairs: PAIRS, darkMode: true});
  assert.equal(errors.length, 1);
  assert.match(errors[0], /^contrast: dark body text: --fg on --bg is 1\.\d\d:1, below 4\.5/);
});

test('evaluatePairs: a variable that is missing in a mode fails the pair and names the variable', () => {
  const css = ':root { --fg: #000000; --bg: #ffffff; }';
  const {errors} = evaluatePairs({css, pairs: PAIRS, darkMode: false});
  assert.deepEqual(errors, ['contrast: light accent: --accent is not defined']);
});

test('evaluatePairs: a background variable that is missing fails the pair and names that variable', () => {
  const css = ':root { --fg: #000000; --accent: #000000; }';
  const {errors, lines, results} = evaluatePairs({css, pairs: PAIRS, darkMode: false});
  assert.deepEqual(errors, ['contrast: light body text: --bg is not defined', 'contrast: light accent: --bg is not defined']);
  assert.deepEqual(lines, [
    'FAIL contrast light body text: --fg on --bg (--bg is not defined)',
    'FAIL contrast light accent: --accent on --bg (--bg is not defined)',
  ]);
  assert.deepEqual(results, []);
});

test('evaluatePairs: a foreground that is missing is reported before a missing background', () => {
  const {errors} = evaluatePairs({css: ':root { }', pairs: PAIRS.slice(0, 1), darkMode: false});
  assert.deepEqual(errors, ['contrast: light body text: --fg is not defined']);
});

test('evaluatePairs: no pair declared at all is an error, never a silent pass', () => {
  const {errors} = evaluatePairs({css: ':root { --fg: #000000; }', pairs: [], darkMode: true});
  assert.deepEqual(errors, ['contrast: no contrast pair is declared']);
});

test('evaluatePairs: resolved colours are returned for the brand rules', () => {
  const css = ':root { --fg: #000000; --bg: var(--paper); --paper: #FFFFFF; --accent: #000; }';
  const {results} = evaluatePairs({css, pairs: PAIRS, darkMode: false});
  assert.equal(results[0].fgHex, '#000000');
  assert.equal(results[0].bgHex, '#FFFFFF');
  assert.equal(results[0].mode, 'light');
  assert.equal(results[0].kind, 'text');
});

const OFF = 'themeConfig = { colorMode: { disableSwitch: true, respectPrefersColorScheme: false } };';
const ON = 'themeConfig = { colorMode: { respectPrefersColorScheme: true } };';
const LIGHT_ONLY = ':root { --a: #000000; }';
const WITH_DARK = ":root { --a: #000000; } html[data-theme='dark'] { --a: #ffffff; }";

test('colorModeErrors: switch off with no dark block, and switch on with a dark block, are both fine', () => {
  assert.deepEqual(colorModeErrors({css: LIGHT_ONLY, config: OFF}), {errors: [], darkMode: false});
  assert.deepEqual(colorModeErrors({css: WITH_DARK, config: ON}), {errors: [], darkMode: true});
});

test('colorModeErrors: switch off while a dark block exists fails', () => {
  const {errors, darkMode} = colorModeErrors({css: WITH_DARK, config: OFF});
  assert.deepEqual(errors, ['contrast: colorMode.disableSwitch is true but custom.css still has a dark block']);
  assert.equal(darkMode, false);
});

test('colorModeErrors: switch on without a dark block fails', () => {
  const {errors, darkMode} = colorModeErrors({css: LIGHT_ONLY, config: ON});
  assert.deepEqual(errors, ['contrast: dark mode is on (colorMode.disableSwitch is not true) but custom.css has no dark block']);
  assert.equal(darkMode, true);
});

test('colorModeErrors: switch off must also stop following the system colour scheme', () => {
  const config = 'themeConfig = { colorMode: { disableSwitch: true, respectPrefersColorScheme: true } };';
  assert.deepEqual(colorModeErrors({css: LIGHT_ONLY, config}).errors, [
    'contrast: colorMode.disableSwitch is true, so colorMode.respectPrefersColorScheme must be false',
  ]);
});

test('colorModeErrors: switch off without any respectPrefersColorScheme key fails too', () => {
  const config = 'themeConfig = { colorMode: { disableSwitch: true } };';
  const {errors, darkMode} = colorModeErrors({css: LIGHT_ONLY, config});
  assert.deepEqual(errors, [
    'contrast: colorMode.disableSwitch is true, so colorMode.respectPrefersColorScheme must be false',
  ]);
  assert.equal(darkMode, false);
});

test('colorModeErrors: switch off with respectPrefersColorScheme: false in any position is fine', () => {
  const config = 'themeConfig = { colorMode: { respectPrefersColorScheme: false, disableSwitch: true } };';
  assert.deepEqual(colorModeErrors({css: LIGHT_ONLY, config}), {errors: [], darkMode: false});
});

test('colorModeErrors: disableSwitch inside a comment does not count as set', () => {
  const config = 'themeConfig = { colorMode: { /* disableSwitch: true, */ respectPrefersColorScheme: true } };';
  assert.deepEqual(colorModeErrors({css: WITH_DARK, config}), {errors: [], darkMode: true});
  const lineComment = 'themeConfig = { colorMode: {\n // disableSwitch: true,\n respectPrefersColorScheme: true } };';
  assert.deepEqual(colorModeErrors({css: WITH_DARK, config: lineComment}), {errors: [], darkMode: true});
});

test('colorModeErrors: a config without a colorMode block counts as dark mode on', () => {
  assert.equal(colorModeErrors({css: WITH_DARK, config: 'themeConfig = {};'}).darkMode, true);
});

test('colorModeErrors: disableSwitch outside the colorMode block does not count', () => {
  const config = 'themeConfig = { navbar: { disableSwitch: true }, colorMode: { respectPrefersColorScheme: true } };';
  assert.deepEqual(colorModeErrors({css: WITH_DARK, config}), {errors: [], darkMode: true});
});
