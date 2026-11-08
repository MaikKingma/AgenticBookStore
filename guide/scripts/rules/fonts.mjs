// The font rule: Montserrat is self-hosted (three woff2 faces, 400, 600 and 700,
// loaded with font-display: swap from ./fonts/), its provenance and licence are in the
// tree, and no font CDN or font package is mentioned anywhere in guide/.
import {withoutBlockComments} from '../lib/text.mjs';
import {createHash} from 'node:crypto';

const WEIGHTS = [400, 600, 700];
const SRC_FORM = /^url\('\.\/fonts\/([\w.-]+\.woff2)'\)\s*format\('woff2'\)$/;

// The banned names are built from pieces, so that this file does not hold them whole.
const BANNED = ['fonts.' + 'googleapis', 'g' + 'static', '@font' + 'source'];

const sha256 = (buffer) => createHash('sha256').update(buffer).digest('hex');

// css: custom.css text; files: {woff2 file name: Buffer} of src/css/fonts; readme: the
// README.md text or undefined; hasLicense: whether static/fonts/OFL.txt exists.
export function fontErrors({css, files, readme, hasLicense}) {
  const errors = [];
  const faces = [...withoutBlockComments(css).matchAll(/@font-face\s*\{([^}]*)\}/g)]
    .map((match) => match[1])
    .filter((body) => /font-family:\s*['"]?Montserrat['"]?\s*;/.test(body))
    .map((body) => ({
      weight: Number(/font-weight:\s*(\d+)\s*;/.exec(body)?.[1]),
      display: /font-display:\s*([\w-]+)\s*;/.exec(body)?.[1],
      src: /src:\s*([^;]+);/.exec(body)?.[1]?.replace(/\s+/g, ' ').trim() ?? '',
    }));

  const found = faces.map((face) => face.weight).sort((a, b) => a - b);
  if (found.join(',') !== WEIGHTS.join(',')) {
    errors.push(
      `fonts: custom.css must declare three Montserrat @font-face rules, weights 400, 600 and 700 (found ${found.join(', ') || 'none'})`,
    );
  }
  for (const face of faces) {
    if (face.display !== 'swap') errors.push(`fonts: the Montserrat ${face.weight} @font-face needs font-display: swap`);
    const form = SRC_FORM.exec(face.src);
    if (!form) {
      errors.push(
        `fonts: the Montserrat ${face.weight} @font-face src must be url('./fonts/<file>.woff2') format('woff2') (found ${face.src || 'none'})`,
      );
    } else if (!(form[1] in files)) {
      errors.push(`fonts: custom.css references ./fonts/${form[1]} but the file is missing`);
    }
  }

  if (readme === undefined) {
    errors.push('fonts: README.md is missing, so the font provenance cannot be checked');
  } else {
    for (const [name, buffer] of Object.entries(files)) {
      const hash = sha256(buffer);
      if (!readme.split('\n').some((line) => line.includes(name) && line.includes(hash))) {
        errors.push(`fonts: README.md has no sha256 ${hash} for ${name}`);
      }
    }
  }
  if (!hasLicense) errors.push('fonts: static/fonts/OFL.txt is missing');
  return errors;
}

// files: [{file, text}] of every text file of guide/. Fonts are self-hosted, so no
// file may mention a font CDN or the font package.
export function bannedFontSourceErrors(files) {
  const errors = [];
  for (const {file, text} of files) {
    text.split('\n').forEach((line, index) => {
      for (const name of BANNED.filter((banned) => line.includes(banned))) {
        errors.push(`fonts: ${file}:${index + 1} mentions "${name}"; fonts are self-hosted, no CDN and no font package`);
      }
    });
  }
  return errors;
}
