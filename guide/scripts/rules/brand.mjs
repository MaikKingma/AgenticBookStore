// The brand rule (decision D18, which replaced the guide's D7 palette): the deck blue and slate and the
// neutrals kept are in custom.css, the primary colour is the AA shade of the deck blue in each mode, the dark
// surfaces are named slate tokens, the font stack is the agreed one, and the exact deck blue and sage never carry
// text. The colour facts of the brand live here and in custom.css only; the contrast rule itself holds no colour.
import {contrastRatio, cssVariables, resolveColor} from './contrast.mjs';

// Deck blue #3C7EC9 is only 4.18:1 on white and 3.74:1 on slate: a non-text accent (borders, bars, outlines).
// Slate is the deck's dark background. Sage is a decorative neutral of decision D7, snow and white the light ones.
export const BRAND = {
  blue: '#3C7EC9',
  slate: '#1D242C',
  sage: '#809FA2',
  snow: '#F1F5F6',
  white: '#FFFFFF',
};

export const FONT_STACK = 'Montserrat, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif';

// The primary colour of each mode: a shade of the deck blue that is text at 4.5:1 or more on the page (light mode
// #3371B8, the "dark" shade of the deck blue, 5.01:1 on white; dark mode #79A7DA, its "lightest" shade, 6.23:1 on slate).
const PRIMARY = {
  light: {hex: '#3371B8', what: 'the AA shade of the deck blue'},
  dark: {hex: '#79A7DA', what: 'the lighter AA shade of the deck blue'},
};
const same = (a, b) => a.toUpperCase() === b.toUpperCase();

// The six shade variables of the primary, darkest to lightest (the primary itself sits between dark and light).
const SHADE_ORDER = ['-darkest', '-darker', '-dark', '', '-light', '-lighter', '-lightest'].map((suffix) => `--ifm-color-primary${suffix}`);

// Dark mode: these backgrounds are the deck slate itself (page, surface and navbar; the sidebar has no background
// of its own and sits on the page) ...
const DARK_PAGE = ['--ifm-background-color', '--ifm-background-surface-color', '--ifm-navbar-background-color'];
// ... and every other surface variable is declared in the dark block as a named slate step.
const DARK_SURFACES = [
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
  '--ifm-color-emphasis-100',
  '--ifm-color-emphasis-200',
  '--guide-alert-note-code',
  '--guide-alert-tip-code',
  '--guide-alert-info-code',
  '--guide-alert-warning-code',
  '--guide-alert-danger-code',
  '--guide-navbar-hover-bg',
  '--guide-navbar-menu-bg',
  '--guide-level-inactive-bg',
];
const SLATE_STEP = /^var\(\s*--ov-slate-\d+\s*\)$/;

// The bordered admonition types (note has no border pair): the border colour is the -dark variable of the type.
const ADMONITION_BORDERS = ['success', 'info', 'warning', 'danger'].map((type) => `--ifm-color-${type}-dark`);

// darkMode: whether the dark block is in use (false when the colour mode switch is off).
export function brandErrors({css, darkMode}) {
  const {light, dark} = cssVariables(css);
  const errors = [];

  const declared = [...light.values()].map((value) => value.toUpperCase());
  for (const [name, hex] of Object.entries(BRAND)) {
    if (!declared.includes(hex.toUpperCase())) {
      errors.push(`brand: brand value ${hex} (${name}) is missing from custom.css`);
    }
  }

  const modes = {light, dark: new Map([...light, ...dark])};
  const names = darkMode ? ['light', 'dark'] : ['light'];
  for (const mode of names) {
    const primary = resolveColor('--ifm-color-primary', modes[mode]);
    const expected = PRIMARY[mode];
    let primaryOk = false;
    if (primary.error) {
      errors.push(`brand: ${mode} ${primary.error}`);
    } else if (!same(primary.hex, expected.hex)) {
      errors.push(`brand: ${mode} --ifm-color-primary must be ${expected.hex} (${expected.what}), found ${primary.hex}`);
    } else {
      primaryOk = true;
    }
    // The shades are the Docusaurus ratios of the primary and run from dark to light. A wrong primary is reported
    // above and moves every shade, so the order is only read once the primary is the pinned one.
    if (primaryOk) errors.push(...shadeOrderErrors(mode, modes[mode]));
  }

  // Light headings are the deck slate; dark headings are light text (a pair checks their contrast).
  const heading = resolveColor('--ifm-heading-color', modes.light);
  if (!heading.error && !same(heading.hex, BRAND.slate)) {
    errors.push(`brand: light --ifm-heading-color must resolve to the deck slate ${BRAND.slate}, found ${heading.hex}`);
  }

  if (darkMode) errors.push(...darkSurfaceErrors(dark, modes.dark));
  errors.push(...admonitionBorderErrors(modes, names));

  const font = light.get('--ifm-font-family-base');
  if (font === undefined) {
    errors.push('brand: --ifm-font-family-base is not defined');
  } else if (font !== FONT_STACK) {
    errors.push(`brand: --ifm-font-family-base must be exactly "${FONT_STACK}", found "${font}"`);
  }
  return errors;
}

// Neighbours in SHADE_ORDER must get lighter by luminance (the contrast against black rises with it).
function shadeOrderErrors(mode, variables) {
  const hexes = SHADE_ORDER.map((name) => resolveColor(name, variables));
  if (hexes.some((shade) => shade.error)) return hexes.filter((shade) => shade.error).map((shade) => `brand: ${mode} ${shade.error}`);
  const level = (shade) => contrastRatio(shade.hex, '#000000');
  const errors = [];
  for (let i = 0; i + 1 < SHADE_ORDER.length; i++) {
    if (level(hexes[i]) >= level(hexes[i + 1])) {
      errors.push(
        `brand: ${mode} primary shades must run darkest, darker, dark, primary, light, lighter, lightest from dark to light (${SHADE_ORDER[i]} is not darker than ${SHADE_ORDER[i + 1]})`,
      );
    }
  }
  return errors;
}

function darkSurfaceErrors(dark, variables) {
  const errors = [];
  for (const name of DARK_PAGE) {
    const color = resolveColor(name, variables);
    if (color.error) errors.push(`brand: dark ${color.error}`);
    else if (!same(color.hex, BRAND.slate)) {
      errors.push(`brand: dark ${name} must resolve to the deck slate ${BRAND.slate}, found ${color.hex}`);
    }
  }
  for (const name of DARK_SURFACES) {
    const value = dark.get(name);
    if (value === undefined) errors.push(`brand: dark ${name} is not declared in the dark block`);
    else if (!SLATE_STEP.test(value)) {
      errors.push(`brand: dark ${name} must be a named slate token var(--ov-slate-<step>), found "${value}"`);
    }
  }
  return errors;
}

// The border of a bordered admonition: the exact deck blue, a shade of the primary of its mode, or a named neutral.
function admonitionBorderErrors(modes, names) {
  const errors = [];
  for (const mode of names) {
    const allowed = [BRAND.blue, BRAND.slate, BRAND.white, BRAND.snow, BRAND.sage];
    for (const shade of SHADE_ORDER) {
      const color = resolveColor(shade, modes[mode]);
      if (!color.error) allowed.push(color.hex);
    }
    for (const name of ADMONITION_BORDERS) {
      const color = resolveColor(name, modes[mode]);
      if (color.error) errors.push(`brand: ${mode} ${color.error}`);
      else if (!allowed.some((hex) => same(hex, color.hex))) {
        errors.push(
          `brand: ${mode} ${name} (admonition border) must be the deck blue ${BRAND.blue}, a shade of the primary or a named neutral, found ${color.hex}`,
        );
      }
    }
  }
  return errors;
}

// results: the evaluated pairs of contrast.mjs. The exact deck blue is for non-text accents only (A1: it is 4.18:1
// on white and 3.74:1 on slate, so the primary text colour is an AA shade of it), sage is decorative only, so no
// text pair may use either as its colour or its background.
export function restrictedTextErrors(results) {
  const errors = [];
  for (const r of results.filter((pair) => pair.kind === 'text')) {
    const where = `brand: ${r.mode} ${r.name}: text pair ${r.fg} on ${r.bg} uses`;
    if (same(r.fgHex, BRAND.blue)) errors.push(`${where} blue ${BRAND.blue} as text, which is only for non-text accents`);
    if (same(r.fgHex, BRAND.sage)) errors.push(`${where} sage ${BRAND.sage} as text, which is decorative only`);
    if (same(r.bgHex, BRAND.blue)) errors.push(`${where} blue ${BRAND.blue} as a background for text`);
    if (same(r.bgHex, BRAND.sage)) errors.push(`${where} sage ${BRAND.sage} as a background for text`);
  }
  return errors;
}
