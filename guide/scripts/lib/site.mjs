// The file reads behind the brand, contrast and font rules. The rules themselves are
// pure functions in scripts/rules/; this module finds the files and hands them over.
import {existsSync, readFileSync, readdirSync} from 'node:fs';
import {join} from 'node:path';
import {CONTRAST_PAIRS} from '../rules/contrast-pairs.mjs';
import {colorModeErrors, evaluatePairs} from '../rules/contrast.mjs';
import {brandErrors, restrictedTextErrors} from '../rules/brand.mjs';
import {bannedFontSourceErrors, fontErrors} from '../rules/fonts.mjs';

const CSS = 'src/css/custom.css';
const CONFIG = 'docusaurus.config.ts';
const FONT_DIR = 'src/css/fonts';

function readText(root, rel) {
  const path = join(root, rel);
  return existsSync(path) ? readFileSync(path, 'utf8') : undefined;
}

function readFonts(root) {
  const dir = join(root, FONT_DIR);
  if (!existsSync(dir)) return {};
  return Object.fromEntries(
    readdirSync(dir)
      .filter((name) => name.endsWith('.woff2'))
      .sort()
      .map((name) => [name, readFileSync(join(dir, name))]),
  );
}

// Runs the contrast, brand and font rules. files: the text files of guide/ as
// {file, text}. Returns, per rule, the error lines, plus the printed contrast pairs
// and a one-line summary per rule.
export function siteRules(root, files) {
  const css = readText(root, CSS);
  const config = readText(root, CONFIG);
  const missing = (rule, rel) => ({errors: [`${rule}: ${rel} is missing`], lines: [], summary: ''});

  let contrast;
  let brand;
  if (css === undefined) {
    contrast = missing('contrast', CSS);
    brand = missing('brand', CSS);
  } else if (config === undefined) {
    contrast = missing('contrast', CONFIG);
    brand = missing('brand', CONFIG);
  } else {
    const mode = colorModeErrors({css, config});
    const evaluated = evaluatePairs({css, pairs: CONTRAST_PAIRS, darkMode: mode.darkMode});
    const modes = mode.darkMode ? 'light and dark' : 'light only, dark mode is off';
    contrast = {
      errors: [...mode.errors, ...evaluated.errors],
      lines: evaluated.lines,
      summary: `${evaluated.results.length} pair checks, ${modes}`,
    };
    brand = {
      errors: [...brandErrors({css, darkMode: mode.darkMode}), ...restrictedTextErrors(evaluated.results)],
      lines: [],
      summary: 'brand values, primary colours, font stack',
    };
  }

  const fonts = {
    errors: [
      ...(css === undefined
        ? [`fonts: ${CSS} is missing`]
        : fontErrors({
            css,
            files: readFonts(root),
            readme: readText(root, 'README.md'),
            hasLicense: existsSync(join(root, 'static/fonts/OFL.txt')),
          })),
      ...bannedFontSourceErrors(files),
    ],
    lines: [],
    summary: `${Object.keys(readFonts(root)).length} font files, provenance, licence`,
  };
  return {contrast, brand, fonts};
}
