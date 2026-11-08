import {existsSync, readdirSync, readFileSync, statSync} from 'node:fs';
import {join} from 'node:path';

// The built-output rule behind `npm run check:built`. With the Docusaurus 3 title
// syntax (:::info[Title]) an admonition renders as a box. A misspelled one, such as
// the old space form, renders as plain text with no build error, and the colons are
// the only trace left in the HTML. Any literal ":::" in a built page is an error.

const FENCE = ':::';

function htmlFiles(dir, relDir, out) {
  for (const entry of readdirSync(join(dir, relDir), {withFileTypes: true})) {
    const rel = relDir === '' ? entry.name : `${relDir}/${entry.name}`;
    if (entry.isDirectory()) htmlFiles(dir, rel, out);
    else if (entry.isFile() && entry.name.endsWith('.html')) out.push(rel);
  }
  return out;
}

// Returns {errors, count}: count is the number of html files scanned.
export function scanBuilt(dir) {
  if (!existsSync(dir) || !statSync(dir).isDirectory()) {
    return {errors: [`built: ${dir} is missing; run npm run build first`], count: 0};
  }
  const files = htmlFiles(dir, '', []).sort();
  if (files.length === 0) {
    return {errors: [`built: no .html file found under ${dir}; run npm run build first`], count: 0};
  }
  const errors = [];
  for (const file of files) {
    readFileSync(join(dir, file), 'utf8')
      .split('\n')
      .forEach((line, index) => {
        if (line.includes(FENCE)) {
          errors.push(
            `built: ${file}:${index + 1} literal "${FENCE}" in the rendered page; an admonition was not parsed (use the bracket form, for example ${FENCE}info[Title])`,
          );
        }
      });
  }
  return {errors, count: files.length};
}

export function builtErrors(dir) {
  return scanBuilt(dir).errors;
}
