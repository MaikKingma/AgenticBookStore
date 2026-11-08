// The brand rule (decision D7): the seven OpenValue 2026 values are in custom.css,
// the primary colour is teal in light mode and mist in dark mode, the font stack is
// the agreed one, and blue and sage never carry text. The colour facts of the brand
// live here and in custom.css only; the contrast rule itself holds no colour.
import {cssVariables, resolveColor} from './contrast.mjs';

export const BRAND = {
  navy: '#1D252D',
  blue: '#3D7DC9',
  teal: '#014046',
  sage: '#809FA2',
  mist: '#CBD9DA',
  snow: '#F1F5F6',
  white: '#FFFFFF',
};

export const FONT_STACK = 'Montserrat, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif';

const PRIMARY = {light: 'teal', dark: 'mist'};
const same = (a, b) => a.toUpperCase() === b.toUpperCase();

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
  for (const mode of darkMode ? ['light', 'dark'] : ['light']) {
    const primary = resolveColor('--ifm-color-primary', modes[mode]);
    const expected = BRAND[PRIMARY[mode]];
    if (primary.error) {
      errors.push(`brand: ${mode} ${primary.error}`);
    } else if (!same(primary.hex, expected)) {
      errors.push(`brand: ${mode} --ifm-color-primary must be ${expected} (${PRIMARY[mode]}), found ${primary.hex}`);
    }
  }

  const font = light.get('--ifm-font-family-base');
  if (font === undefined) {
    errors.push('brand: --ifm-font-family-base is not defined');
  } else if (font !== FONT_STACK) {
    errors.push(`brand: --ifm-font-family-base must be exactly "${FONT_STACK}", found "${font}"`);
  }
  return errors;
}

// results: the evaluated pairs of contrast.mjs. Blue is for large or bold text and
// non-text accents only, sage is decorative only, so no text pair may use either.
export function restrictedTextErrors(results) {
  const errors = [];
  for (const r of results.filter((pair) => pair.kind === 'text')) {
    const where = `brand: ${r.mode} ${r.name}: text pair ${r.fg} on ${r.bg} uses`;
    if (same(r.fgHex, BRAND.blue)) {
      errors.push(`${where} blue ${BRAND.blue} as text, which is only for large or bold text and non-text accents`);
    }
    if (same(r.fgHex, BRAND.sage)) errors.push(`${where} sage ${BRAND.sage} as text, which is decorative only`);
    if (same(r.bgHex, BRAND.blue)) errors.push(`${where} blue ${BRAND.blue} as a background for text`);
    if (same(r.bgHex, BRAND.sage)) errors.push(`${where} sage ${BRAND.sage} as a background for text`);
  }
  return errors;
}
