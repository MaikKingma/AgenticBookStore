// The contrast rule. It reads src/css/custom.css by variable name, resolves var()
// chains per colour mode and computes the WCAG 2.x contrast ratio of every declared
// pair (see contrast-pairs.mjs for the pairs). This module holds no colour values:
// the colours come from the stylesheet, the names from the pair table.
import {blank, matchingBrace, withoutBlockComments} from '../lib/text.mjs';

const MINIMUMS = {text: 4.5, nontext: 3};

// {r, g, b} of an opaque #rgb or #rrggbb colour, any case; null for anything else
// (a four or eight digit form carries alpha, which a contrast ratio cannot take).
export function parseHex(value) {
  const match = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i.exec(String(value).trim());
  if (!match) return null;
  const digits = match[1].length === 3 ? [...match[1]].map((d) => d + d).join('') : match[1];
  const n = Number.parseInt(digits, 16);
  return {r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255};
}

function channel(value) {
  const s = value / 255;
  return s <= 0.04045 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4;
}

function luminance({r, g, b}) {
  return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b);
}

// WCAG 2.x contrast ratio of two hex colours, from 1 to 21, in either order.
export function contrastRatio(a, b) {
  const [light, dark] = [luminance(parseHex(a)), luminance(parseHex(b))].sort((x, y) => y - x);
  return (light + 0.05) / (dark + 0.05);
}

const DARK_SELECTOR = /^(?:html)?\[data-theme\s*=\s*['"]dark['"]\]$/;

// The custom properties of the top-level :root blocks (light) and the dark theme
// blocks (dark) of a stylesheet. Rules inside @media and other at-rules are ignored.
export function cssVariables(css) {
  const text = withoutBlockComments(css);
  const light = new Map();
  const dark = new Map();
  let hasDark = false;
  let selectorStart = 0;
  for (let i = 0; i < text.length; i++) {
    if (text[i] === '}') {
      selectorStart = i + 1;
    } else if (text[i] === '{') {
      const selector = text.slice(selectorStart, i).trim();
      const end = matchingBrace(text, i);
      const target = selector === ':root' ? light : DARK_SELECTOR.test(selector) ? dark : null;
      if (target) {
        if (target === dark) hasDark = true;
        for (const declaration of text.slice(i + 1, end).matchAll(/(--[\w-]+)\s*:\s*([^;]+?)\s*(?:;|$)/g)) {
          target.set(declaration[1], declaration[2]);
        }
      }
      i = end;
      selectorStart = end + 1;
    }
  }
  return {light, dark, hasDark};
}

// The hex colour a variable resolves to, following var() references; {hex} or {error}.
export function resolveColor(name, variables, seen = []) {
  if (seen.includes(name)) return {error: `${name} is part of a var() cycle`};
  const value = variables.get(name);
  if (value === undefined) return {error: `${name} is not defined`};
  const reference = /^var\(\s*(--[\w-]+)\s*\)$/.exec(value);
  if (reference) return resolveColor(reference[1], variables, [...seen, name]);
  if (parseHex(value)) return {hex: value};
  return {error: `${name} is "${value}", not an opaque hex colour`};
}

// Evaluates every pair in light mode and, when darkMode is true, in dark mode (dark
// values override the light ones, everything else is inherited like in the cascade).
// pairs: [{name, fg, bg, kind: 'text' | 'nontext'}] with variable names.
export function evaluatePairs({css, pairs, darkMode, minimums = MINIMUMS}) {
  if (pairs.length === 0) {
    return {lines: [], errors: ['contrast: no contrast pair is declared'], results: []};
  }
  const {light, dark} = cssVariables(css);
  const byMode = {light, dark: new Map([...light, ...dark])};
  const lines = [];
  const errors = [];
  const results = [];
  for (const mode of darkMode ? ['light', 'dark'] : ['light']) {
    for (const pair of pairs) {
      const min = minimums[pair.kind];
      const label = `${mode} ${pair.name}: ${pair.fg} on ${pair.bg}`;
      const fg = resolveColor(pair.fg, byMode[mode]);
      const bg = resolveColor(pair.bg, byMode[mode]);
      const problem = fg.error ?? bg.error;
      if (problem) {
        errors.push(`contrast: ${mode} ${pair.name}: ${problem}`);
        lines.push(`FAIL contrast ${label} (${problem})`);
        continue;
      }
      const ratio = contrastRatio(fg.hex, bg.hex);
      const ok = ratio >= min;
      results.push({mode, ...pair, fgHex: fg.hex, bgHex: bg.hex, ratio, min, ok});
      if (ok) {
        lines.push(`ok   contrast ${label} ${ratio.toFixed(2)} (min ${min.toFixed(1)})`);
      } else {
        errors.push(`contrast: ${label} is ${ratio.toFixed(2)}:1, below ${min.toFixed(1)}`);
        lines.push(`FAIL contrast ${label} ${ratio.toFixed(2)} (min ${min.toFixed(1)})`);
      }
    }
  }
  return {lines, errors, results};
}

// Config source without comments. A // comment must follow whitespace, so the // of a
// URL inside a string stays.
function withoutComments(source) {
  return withoutBlockComments(source).replace(/(^|\s)\/\/[^\n]*/g, (m, lead) => lead);
}

// Cross-checks the colour mode switch of docusaurus.config.ts against the dark block
// of the stylesheet. Returns {errors, darkMode}: darkMode is false only when the
// switch is disabled.
export function colorModeErrors({css, config}) {
  const source = withoutComments(config);
  const open = /\bcolorMode\s*:\s*\{/.exec(source);
  const body = open ? source.slice(open.index + open[0].length - 1, matchingBrace(source, open.index + open[0].length - 1)) : '';
  const switchOff = /\bdisableSwitch\s*:\s*true\b/.test(body);
  const respect = /\brespectPrefersColorScheme\s*:\s*(true|false)\b/.exec(body)?.[1];
  const {hasDark} = cssVariables(css);
  const errors = [];
  if (switchOff) {
    if (hasDark) errors.push('contrast: colorMode.disableSwitch is true but custom.css still has a dark block');
    if (respect !== 'false') {
      errors.push('contrast: colorMode.disableSwitch is true, so colorMode.respectPrefersColorScheme must be false');
    }
  } else if (!hasDark) {
    errors.push('contrast: dark mode is on (colorMode.disableSwitch is not true) but custom.css has no dark block');
  }
  return {errors, darkMode: !switchOff};
}
