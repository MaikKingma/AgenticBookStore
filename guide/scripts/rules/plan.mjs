// Parsers for the pack files the guide is checked against: the workshop plan (Goal,
// minutes and "Level cleared when" list of each level) and the SDD framework guide
// (its "Level cleared when" list for Level 4). Pure functions on text; the file reads
// are in lib/structure.mjs.

const LEVEL_COUNT = 5;
const LIST_MARKER = '**Level cleared when**';

// Whitespace runs become one space and the ends are trimmed.
export const normalise = (text) => text.replace(/\s+/g, ' ').trim();

const lines = (text) => text.replace(/\r\n/g, '\n').split('\n');

// The bullet items that start at `from` and run to the first blank line. An item is
// a line starting with "- "; an indented line after it continues it. Returns the raw
// item texts (after "- ", continuation lines joined with a space) and the index of
// the first line after the list.
export function readBullets(allLines, from) {
  const items = [];
  let i = from;
  for (; i < allLines.length && allLines[i].trim() !== ''; i++) {
    const line = allLines[i];
    if (line.startsWith('- ')) items.push(line.slice(2).trim());
    else if (/^\s+\S/.test(line) && items.length > 0) items[items.length - 1] += ` ${line.trim()}`;
    else break;
  }
  return {items, next: i};
}

// The text of a labelled paragraph: the line starting with `label` and the lines that
// follow it up to the first blank line, normalised. Undefined when no line starts with it.
function labelled(allLines, label) {
  const start = allLines.findIndex((line) => line.startsWith(label));
  if (start === -1) return undefined;
  const parts = [allLines[start].slice(label.length)];
  for (let i = start + 1; i < allLines.length && allLines[i].trim() !== ''; i++) parts.push(allLines[i]);
  return normalise(parts.join(' '));
}

// Returns {levels, errors}. levels[n] is {level, name, minutes, goal, items} for
// Level n, or undefined when the plan has no such section. Anti-vacuity: the plan
// must hold exactly five "Level cleared when" blocks, each with at least one item.
export function parsePlan(text) {
  const all = lines(text);
  const errors = [];
  const levels = [];

  const heading = /^## \d+\. Level (\d): (.+) \((\d+) min\)\s*$/;
  const starts = [];
  all.forEach((line, index) => {
    const m = heading.exec(line);
    if (m) starts.push({index, level: Number(m[1]), name: m[2], minutes: Number(m[3])});
  });
  const headingIndexes = all.flatMap((line, index) => (line.startsWith('## ') ? [index] : []));

  for (let n = 0; n < LEVEL_COUNT; n++) {
    const found = starts.filter((s) => s.level === n);
    if (found.length !== 1) {
      errors.push(`workshop plan: ${found.length === 0 ? 'no section' : `${found.length} sections`} for Level ${n}`);
      continue;
    }
    const [{index, name, minutes}] = found;
    const end = headingIndexes.find((h) => h > index) ?? all.length;
    const section = all.slice(index + 1, end);
    const goal = labelled(section, '**Goal:**');
    if (goal === undefined) errors.push(`workshop plan: Level ${n} has no Goal line`);
    const marker = section.findIndex((line) => line.trim() === LIST_MARKER);
    const items = marker === -1 ? [] : readBullets(section, marker + 1).items.map(normalise);
    if (items.length === 0) errors.push(`workshop plan: Level ${n} has no checklist items after "Level cleared when"`);
    levels[n] = {level: n, name, minutes, goal: goal ?? '', items};
  }

  const blocks = all.filter((line) => line.trim() === LIST_MARKER).length;
  if (blocks !== LEVEL_COUNT) {
    errors.push(`workshop plan: found ${blocks} "Level cleared when" blocks, expected ${LEVEL_COUNT}`);
  }
  return {levels, errors};
}

// Returns {items, errors}: the list under the heading "Level cleared when" of the SDD
// framework guide.
export function parseSddChecklist(text) {
  const all = lines(text);
  const headings = all.flatMap((line, index) => (/^#{2,4}\s+Level cleared when\b/.test(line) ? [index] : []));
  if (headings.length !== 1) {
    return {
      items: [],
      errors: [
        headings.length === 0
          ? 'SDD framework guide: no "Level cleared when" heading'
          : `SDD framework guide: ${headings.length} "Level cleared when" headings, expected 1`,
      ],
    };
  }
  const items = readBullets(all, headings[0] + 1).items.map(normalise);
  return {
    items,
    errors: items.length === 0 ? ['SDD framework guide: the "Level cleared when" heading has no list'] : [],
  };
}
