// The coverage rules of the guide: every manifest entry is anchored in its pack section
// (rule a), and on a finished page the entry's text is on the page, in a fenced block when
// the entry is a command (rule b). Pure functions on text; the file reads are in
// lib/content.mjs and the manifest itself is scripts/coverage.mjs.
//
// A page that still carries the skeleton marker is exempt from rule (b) only: its entries
// are listed as pending. The marker predicate is isSkeleton, one function for every caller.
import {collapse, scanMarkdown} from '../lib/mdscan.mjs';
import {SKELETON_MARKER} from './structure.mjs';

export const isSkeleton = (text) => text.includes(SKELETON_MARKER);

const KINDS = ['command', 'text'];
const BOX_OPENER = ':::note[At a glance]';
const CHECKLIST_HEADING = '## Level cleared when';

// A blockquote marker at the start of a line is not part of the quoted text.
const unquote = (line) => line.replace(/^\s*(?:>\s*)+/, '');

// The form in which snippets, pack sections and pages are compared: whitespace runs are one
// space, and the whitespace next to a bracket or brace goes, so a JSON block matches
// however its lines are broken after "[" or "{". Both sides always go through this function.
export const fold = (text) => collapse(text).replace(/\s*([[\]{}])\s*/g, '$1');

// A snippet as an error message shows it: one line, at most 90 characters.
const shown = (snippet) => {
  const line = collapse(snippet);
  return line.length > 90 ? `${line.slice(0, 87)}...` : line;
};

// The text of a pack section as one normalised line: from the heading that starts with
// `name` to the next heading of the same or a higher level. Headings inside fenced blocks
// are code comments, not headings. Returns {text} or {error}.
export function packSection(packText, name) {
  const scan = scanMarkdown(packText);
  const heads = [];
  scan.lines.forEach((line, index) => {
    const match = line.fence === null ? /^(#{1,6})\s+(.*?)\s*$/.exec(line.text) : null;
    if (match) heads.push({index, level: match[1].length, title: match[2]});
  });
  const found = heads.filter((head) => head.title.startsWith(name));
  if (found.length !== 1) {
    return {error: found.length === 0 ? `no heading starts with "${name}"` : `${found.length} headings start with "${name}"`};
  }
  const [{index, level, title}] = found;
  const next = heads.find((head) => head.index > index && head.level <= level);
  const end = next ? next.index : scan.lines.length;
  // The heading text belongs to its section (a brief is named in its heading).
  const body = [title, ...scan.lines.slice(index + 1, end).map((line) => unquote(line.text))];
  return {text: fold(body.join(' ').replace(/\\\|/g, '|'))};
}

// The views of a page that rule (b) searches: the prose and each fenced block, as
// whitespace-collapsed text. Comments are blanked, and two regions that already have
// their own rules are left out, so that they cannot satisfy an entry: the at a glance box
// and the "Level cleared when" checklist at the end of a level page.
export function pageViews(pageText) {
  const scan = scanMarkdown(pageText);
  const skip = scan.lines.map(() => false);
  const prose = (line) => line.fence === null;
  // The frontmatter is metadata (a title, a description), not text the participant reads.
  if (scan.lines.length > 0 && scan.lines[0].text.trim() === '---') {
    const end = scan.lines.findIndex((line, index) => index > 0 && prose(line) && line.text.trim() === '---');
    if (end !== -1) for (let i = 0; i <= end; i++) skip[i] = true;
  }
  const open = scan.lines.findIndex((line) => prose(line) && line.text.trim() === BOX_OPENER);
  if (open !== -1) {
    const close = scan.lines.findIndex((line, index) => index > open && prose(line) && line.text.trim() === ':::');
    for (let i = open; i < (close === -1 ? skip.length : close + 1); i++) skip[i] = true;
  }
  const checklist = scan.lines.findIndex((line) => prose(line) && line.text.trimEnd() === CHECKLIST_HEADING);
  if (checklist !== -1) for (let i = checklist; i < skip.length; i++) skip[i] = true;

  const text = scan.lines.flatMap((line, index) => (prose(line) && !skip[index] ? [unquote(line.text)] : []));
  const blocks = scan.blocks.filter((block) => !skip[block.open]).map((block) => fold(block.body.join('\n')));
  return {prose: fold(text.join(' ')), blocks};
}

// Problems with the shape of the manifest itself, so that a malformed entry cannot pass
// as "anchored". Returns error lines.
export function manifestShapeErrors({entries, manualRows, sourceFiles, pageFiles}) {
  const errors = [];
  if (entries.length === 0) errors.push('coverage: the manifest has no entries');
  const ids = new Set();
  for (const entry of entries) {
    const where = `coverage: entry ${entry.id ?? JSON.stringify(entry)}`;
    for (const key of ['id', 'row', 'source', 'section', 'page', 'kind', 'snippet']) {
      if (typeof entry[key] !== 'string' || entry[key].trim() === '') errors.push(`${where}: the field ${key} is missing`);
    }
    if (entry.pageSnippet !== undefined && (typeof entry.pageSnippet !== 'string' || entry.pageSnippet.trim() === '')) {
      errors.push(`${where}: pageSnippet must be a non-empty string`);
    }
    if (ids.has(entry.id)) errors.push(`${where}: the id is used twice`);
    ids.add(entry.id);
    if (entry.source && !(entry.source in sourceFiles)) errors.push(`${where}: unknown source "${entry.source}"`);
    if (entry.page && !(entry.page in pageFiles)) errors.push(`${where}: unknown page "${entry.page}"`);
    if (entry.kind && !KINDS.includes(entry.kind)) errors.push(`${where}: unknown kind "${entry.kind}"`);
  }
  for (const row of manualRows) {
    const where = `coverage: manual row ${row.row ?? JSON.stringify(row)}`;
    for (const key of ['row', 'item', 'page', 'reason']) {
      if (typeof row[key] !== 'string' || row[key].trim() === '') errors.push(`${where}: the field ${key} is missing`);
    }
    if (row.page && !(row.page in pageFiles)) errors.push(`${where}: unknown page "${row.page}"`);
  }
  return errors;
}

// entries, manualRows: the manifest. sourceFiles: source -> path (for messages). pageFiles:
// page code -> docs path. pack: source -> text or null (missing). pages: docs path -> text.
// Returns {errors, anchored, total, pending}; pending maps a skeleton page to its entry ids.
export function coverageErrors({entries, manualRows = [], sourceFiles, pageFiles, pack, pages}) {
  const errors = manifestShapeErrors({entries, manualRows, sourceFiles, pageFiles});
  const sections = new Map();
  const section = (entry) => {
    const key = `${entry.source}\0${entry.section}`;
    if (!sections.has(key)) {
      const text = pack[entry.source];
      sections.set(
        key,
        text === null || text === undefined ? {error: `the pack file ${sourceFiles[entry.source]} is missing`} : packSection(text, entry.section),
      );
    }
    return sections.get(key);
  };

  let anchored = 0;
  const pending = {};
  const views = new Map();
  for (const entry of entries) {
    if (!(entry.source in sourceFiles) || !(entry.page in pageFiles) || typeof entry.snippet !== 'string') continue;
    const found = section(entry);
    if (found.error) {
      errors.push(`coverage: ${entry.id}: ${entry.source} section "${entry.section}": ${found.error}`);
    } else if (!found.text.includes(fold(entry.snippet))) {
      errors.push(`coverage: ${entry.id}: the snippet "${shown(entry.snippet)}" is not in ${sourceFiles[entry.source]}, section "${entry.section}"`);
    } else {
      anchored++;
    }

    const file = pageFiles[entry.page];
    const text = pages[file];
    if (text === undefined) {
      errors.push(`coverage: ${entry.id}: the page ${file} is missing`);
      continue;
    }
    if (isSkeleton(text)) {
      (pending[file] ??= []).push(entry.id);
      continue;
    }
    if (!views.has(file)) views.set(file, pageViews(text));
    const view = views.get(file);
    const wanted = fold(entry.pageSnippet ?? entry.snippet);
    if (entry.kind === 'command') {
      if (!view.blocks.some((block) => block.includes(wanted))) {
        errors.push(`coverage: ${entry.id}: ${file}: the command "${shown(entry.pageSnippet ?? entry.snippet)}" is not in a fenced block of the page`);
      }
    } else if (!view.prose.includes(wanted) && !view.blocks.some((block) => block.includes(wanted))) {
      errors.push(`coverage: ${entry.id}: ${file}: the text "${shown(entry.pageSnippet ?? entry.snippet)}" is not on the page`);
    }
  }
  return {errors, anchored, total: entries.length, pending};
}
