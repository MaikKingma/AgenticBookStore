import {existsSync, readdirSync, readFileSync, statSync} from 'node:fs';
import {join} from 'node:path';
import {fileURLToPath} from 'node:url';
import {checkboxes, lineOf} from '../lib/html.mjs';
import {SOURCE_FILES} from '../coverage.mjs';
import {packCommandLines} from './content.mjs';
import {powershellLineErrors} from './built-powershell.mjs';
import {navbarErrors} from './navbar-built.mjs';
import {levelOfFile, levelPagesOf, levelProgressErrors, routeOf} from './level-progress-built.mjs';
import {tableRegions} from './table-regions-built.mjs';

// The built-output rules behind `npm run check:built`.
// 1. With the Docusaurus 3 title syntax (:::info[Title]) an admonition renders as a box.
// A misspelled one, such as the old space form, renders as plain text with no build
// error, and the colons are the only trace left in the HTML. Any literal ":::" in a
// built page is an error.
// 2. Docusaurus renders a task list item ("- [ ] text") as a bare disabled checkbox. The
// local rehype plugin gives it an aria-label; a checkbox in the built HTML with no
// accessible name is an error, so a lost or broken plugin cannot ship unnoticed.
// 3. Rule (c) on the rendered pages: every line of a pre or code element in a tab panel
// labelled PowerShell is a pack command line (rules/built-powershell.mjs). The allow-list
// is packCommandLines of the five pack files of the manifest, the list the source rule uses.

const FENCE = ':::';
// The pack root (docs/spec-pack/), found relative to this file like check.mjs finds it.
const PACK_ROOT = fileURLToPath(new URL('../../../docs/spec-pack/', import.meta.url));

// The allow-list of rule (c): packCommandLines of the five pack files; a missing file adds
// nothing, so its lines fail.
function packAllowList() {
  return packCommandLines(
    Object.values(SOURCE_FILES).map((rel) => {
      const path = join(PACK_ROOT, rel);
      return existsSync(path) ? readFileSync(path, 'utf8') : null;
    }),
  );
}

function htmlFiles(dir, relDir, out) {
  for (const entry of readdirSync(join(dir, relDir), {withFileTypes: true})) {
    const rel = relDir === '' ? entry.name : `${relDir}/${entry.name}`;
    if (entry.isDirectory()) htmlFiles(dir, rel, out);
    else if (entry.isFile() && entry.name.endsWith('.html')) out.push(rel);
  }
  return out;
}

// Returns {errors, count, checkboxes, windowsLines, tables, tablePages, navbarPages, levelPages, otherPages}: count is the number of html
// files scanned, checkboxes the number of checkboxes seen in them (named or not), windowsLines the
// number of lines read in PowerShell tab panels and PowerShell-language code blocks, a line
// counted once (in the list or not). options.navbar ({base}) switches on rule 4 below, the navbar
// of every page (rules/navbar-built.mjs); it describes the real site, so check-built.mjs sets it
// for the default build dir only and the fixture builds of the self-tests leave it off. The same
// holds for options.levels ({base}), rule 5: levelPages and otherPages are returned only then.
// 4. The navbar of the Pages build: logo link, title link, GitHub and LinkedIn links.
// 5. The level progress breadcrumb: on the five level pages (level-N-*.html), none on any other page
// (rules/level-progress-built.mjs).
// 6. The table regions: every table inside an article sits in a focusable, named region with a name that
// is unique on its page (rules/table-regions-built.mjs). It runs on every build dir, the Pages build and
// the offline build alike; tables is the number of such tables, tablePages the pages that hold one.
export function scanBuilt(dir, options = {}) {
  if (!existsSync(dir) || !statSync(dir).isDirectory()) {
    return {errors: [`built: ${dir} is missing; run npm run build first`], count: 0};
  }
  const files = htmlFiles(dir, '', []).sort();
  if (files.length === 0) {
    return {errors: [`built: no .html file found under ${dir}; run npm run build first`], count: 0};
  }
  const errors = [];
  let checkboxCount = 0;
  let windowsLines = 0;
  let navbarPages = 0;
  let tableCount = 0;
  let tablePages = 0;
  const allowed = packAllowList();
  const pages = options.levels ? levelPagesOf(files) : null;
  if (pages) errors.push(...pages.errors);
  const routes = pages ? [0, 1, 2, 3, 4].map((level) => (pages.levels.has(level) ? routeOf(pages.levels.get(level)) : undefined)) : [];
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
    const windows = powershellLineErrors(file, html, allowed);
    errors.push(...windows.errors);
    windowsLines += windows.checked;
    const regions = tableRegions({file, html});
    errors.push(...regions.errors);
    tableCount += regions.tables;
    if (regions.tables > 0) tablePages++;
    if (options.navbar) {
      errors.push(...navbarErrors({file, html, base: options.navbar.base}));
      navbarPages++;
    }
    if (pages) {
      const level = levelOfFile(file);
      // a level-N file beyond Level 4 is reported by levelPagesOf; its page is no level page
      const pageLevel = level !== null && level < 5 ? level : null;
      errors.push(...levelProgressErrors({file, html, base: options.levels.base, level: pageLevel, routes}));
    }
  }
  const levelPages = pages ? files.filter((file) => levelOfFile(file) !== null).length : undefined;
  return {
    errors,
    count: files.length,
    checkboxes: checkboxCount,
    windowsLines,
    tables: tableCount,
    tablePages,
    navbarPages,
    levelPages,
    otherPages: pages ? files.length - levelPages : undefined,
  };
}

export function builtErrors(dir) {
  return scanBuilt(dir).errors;
}
