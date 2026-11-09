import {existsSync, readdirSync, readFileSync, statSync} from 'node:fs';
import {join} from 'node:path';
import {checkboxes, lineOf} from '../lib/html.mjs';

// The built-output rules behind `npm run check:built`.
// 1. With the Docusaurus 3 title syntax (:::info[Title]) an admonition renders as a box.
// A misspelled one, such as the old space form, renders as plain text with no build
// error, and the colons are the only trace left in the HTML. Any literal ":::" in a
// built page is an error.
// 2. Docusaurus renders a task list item ("- [ ] text") as a bare disabled checkbox. The
// local rehype plugin gives it an aria-label; a checkbox in the built HTML with no
// accessible name is an error, so a lost or broken plugin cannot ship unnoticed.

const FENCE = ':::';

function htmlFiles(dir, relDir, out) {
  for (const entry of readdirSync(join(dir, relDir), {withFileTypes: true})) {
    const rel = relDir === '' ? entry.name : `${relDir}/${entry.name}`;
    if (entry.isDirectory()) htmlFiles(dir, rel, out);
    else if (entry.isFile() && entry.name.endsWith('.html')) out.push(rel);
  }
  return out;
}

// Returns {errors, count, checkboxes}: count is the number of html files scanned,
// checkboxes the number of checkboxes seen in them (named or not).
export function scanBuilt(dir) {
  if (!existsSync(dir) || !statSync(dir).isDirectory()) {
    return {errors: [`built: ${dir} is missing; run npm run build first`], count: 0};
  }
  const files = htmlFiles(dir, '', []).sort();
  if (files.length === 0) {
    return {errors: [`built: no .html file found under ${dir}; run npm run build first`], count: 0};
  }
  const errors = [];
  let checkboxCount = 0;
  for (const file of files) {
    const html = readFileSync(join(dir, file), 'utf8');
    html.split('\n').forEach((line, index) => {
      if (line.includes(FENCE)) {
        errors.push(
          `built: ${file}:${index + 1} literal "${FENCE}" in the rendered page; an admonition was not parsed (use the bracket form, for example ${FENCE}info[Title])`,
        );
      }
    });
    for (const box of checkboxes(html)) {
      checkboxCount++;
      if (!box.named) {
        errors.push(
          `built: ${file}:${lineOf(html, box.offset)} checkbox has no accessible name; the task-list plugin (src/plugins/rehype-task-list-labels.mjs) must give it an aria-label, or a raw checkbox needs a label`,
        );
      }
    }
  }
  return {errors, count: files.length, checkboxes: checkboxCount};
}

export function builtErrors(dir) {
  return scanBuilt(dir).errors;
}
