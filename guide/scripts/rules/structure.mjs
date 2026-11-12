// The structure rules of the guide docs: which pages exist, the grammar of a level page
// (LevelProgress, the at a glance box, the "Level cleared when" checklist), the
// admonition conventions and the contractual anchors. Pure functions on text; the file
// reads are in lib/structure.mjs. The grammar is the "Page grammar" section of the spec
// folder's strategy.md, and the Goal, minutes and checklist of every level come from the
// workshop plan (parsed by plan.mjs), so the guide cannot drift from it.
import {normalise, parsePlan, parseSddChecklist, readBullets} from './plan.mjs';

export const SKELETON_MARKER = '{/* guide-skeleton */}';

const PROGRESS_IMPORT = "import LevelProgress from '@site/src/components/LevelProgress';";
const BOX_OPENER = ':::note[At a glance]';
const BONUS_TITLE = 'Optional bonus level';
const BONUS_OPENER = `:::info[${BONUS_TITLE}]`;
const SYNC_MOMENT_TITLE = /sync[\s_-]*moment/i;
const CHECKLIST_HEADING = '## Level cleared when';
const CATEGORY_FILE = 'docs/reference/_category_.json';
const WRAP_UP_POSITION = 8;

// The five level pages: exact titles of spec 11, contractual H2 anchors, and the header
// values of the page grammar (Start branch as written on the page, the full catch-up command).
// level-0-start is a tag, not a branch (D17), so Levels 0 and 1 catch up with `git switch -C`,
// which creates the participant's own branch at the tag and resets it on a re-run.
const LEVELS = [
  {
    file: 'docs/01-level-0-break-the-agent.mdx',
    title: 'Level 0: Break the agent',
    start: '`main` (tag `level-0-start`)',
    catchUp: 'git stash -u && git switch -C level-0 level-0-start',
    anchors: [],
  },
  {
    file: 'docs/02-level-1-context-engineering.mdx',
    title: 'Level 1: Context engineering',
    start: '`level-0-start`',
    catchUp: 'git stash -u && git switch -C level-1 level-0-start',
    anchors: [],
  },
  {
    file: 'docs/03-level-2-give-your-agent-eyes.mdx',
    title: 'Level 2: Give your agent eyes (LSP + MCP)',
    start: 'your Level 1 result, or `solution/level-1`',
    catchUp: 'git stash -u && git switch solution/level-1',
    anchors: ['part-a-lsp', 'part-b-mcp', 'stretch'],
  },
  {
    file: 'docs/04-level-3-behavioral-control.mdx',
    title: 'Level 3: Behavioral control',
    start: 'your Level 2 result, or `solution/level-2`',
    catchUp: 'git stash -u && git switch solution/level-2',
    anchors: ['part-a-permissions', 'part-b-hooks', 'optional-stop-hook', 'part-c-skill-and-command', 'stretch'],
  },
  {
    file: 'docs/05-level-4-spec-driven-development.mdx',
    title: 'Level 4: Spec-driven development',
    start: '`start/level-4-<track>`',
    catchUp: 'git stash -u && git switch start/level-4-<track>',
    anchors: ['tracks', 'feature-briefs'],
  },
];

// The other seven pages, with the sidebar position that puts them in the fixed order:
// Welcome, Setup check, Levels 0 to 4 (positions 3 to 7), the optional bonus level (7.5, spec 16),
// Wrap-up, then Reference. bonusMarker: the page follows the marker rule (bonusMarkerErrors).
const OTHER_PAGES = [
  {file: 'docs/index.md', title: 'Welcome', position: 1, slug: '/', anchors: []},
  {file: 'docs/00-setup-check.md', title: 'Setup check', position: 2, anchors: []},
  {file: 'docs/06-bonus-level-orchestration.mdx', title: 'Bonus level: orchestrate your own team', position: 7.5, bonusMarker: true, anchors: []},
  {file: 'docs/06-wrap-up.md', title: 'Wrap-up', position: WRAP_UP_POSITION, anchors: []},
  {file: 'docs/reference/cheat-sheet.md', title: 'Cheat sheet', position: 1, anchors: []},
  {file: 'docs/reference/architecture.md', title: 'Architecture', position: 2, anchors: []},
  {
    file: 'docs/reference/troubleshooting.md',
    title: 'Troubleshooting',
    position: 3,
    anchors: [
      'windows-hooks',
      'jdtls-not-starting',
      'usage-limits',
      'mcp-not-connecting',
      'anthropic-api-key',
      'offline-copy-blank-page',
      'docker-not-running',
      'far-behind',
    ],
  },
];

const EXPECTED_FILES = new Set([...LEVELS.map((l) => l.file), ...OTHER_PAGES.map((p) => p.file), CATEGORY_FILE]);

const lowerFirst = (text) => text.charAt(0).toLowerCase() + text.slice(1);

// Frontmatter: {front, body, bodyLine}. front is null when the page has none; bodyLine
// is the 0-based line of the file where the body starts.
function splitFrontmatter(text) {
  const match = /^---\n([\s\S]*?)\n---\n?/.exec(text);
  if (!match) return {front: null, body: text, bodyLine: 0};
  return {front: match[1], body: text.slice(match[0].length), bodyLine: match[0].split('\n').length - 1};
}

function frontValue(front, key) {
  const match = new RegExp(`^${key}:[ \\t]*(.*)$`, 'm').exec(front);
  return match ? match[1].trim() : undefined;
}

// For each line: true when it is a fence line or inside a fenced code block.
function fenceFlags(lines) {
  const flags = [];
  let open = null;
  for (const line of lines) {
    const fence = /^\s*(`{3,}|~{3,})/.exec(line);
    if (open) {
      flags.push(true);
      if (fence && fence[1][0] === open[0] && fence[1].length >= open.length && /^\s*[`~]+\s*$/.test(line)) open = null;
    } else if (fence) {
      open = fence[1];
      flags.push(true);
    } else {
      flags.push(false);
    }
  }
  return flags;
}

// The admonitions of a body: {type, title, start, content}. start is the 0-based line of
// the opener, content the lines inside (nested ones included). Fenced code is skipped
// for the markers. Also returns the openers that never close.
function admonitions(lines, flags) {
  const done = [];
  const stack = [];
  lines.forEach((line, index) => {
    if (!flags[index]) {
      const open = /^(:{3,})([A-Za-z]+)(?:\[(.*)\])?\s*$/.exec(line);
      const close = /^(:{3,})\s*$/.exec(line);
      if (open) {
        stack.push({colons: open[1].length, type: open[2], title: open[3], start: index, content: []});
        return;
      }
      if (close && stack.length > 0 && stack[stack.length - 1].colons === close[1].length) {
        done.push(stack.pop());
        return;
      }
    }
    for (const frame of stack) frame.content.push(line);
  });
  return {done, unclosed: stack};
}

// The at a glance box content: {entries, strays}. An entry is a "**Label:**" line with
// the text after it, the continuation lines and its fenced blocks (every one is kept, so
// a second block cannot hide behind the first).
function parseBox(content) {
  const entries = [];
  const strays = [];
  for (let i = 0; i < content.length; i++) {
    const line = content[i];
    if (line.trim() === '') continue;
    const label = /^\*\*(Goal|Start branch|Catch-up|Time|Compare):\*\*\s*(.*)$/.exec(line);
    const fence = /^\s*(`{3,})(\w*)\s*$/.exec(line);
    const current = entries[entries.length - 1];
    if (label) {
      entries.push({label: label[1], text: label[2], fences: []});
    } else if (fence && current) {
      const lines = [];
      for (i++; i < content.length && !/^\s*`{3,}\s*$/.test(content[i]); i++) lines.push(content[i].trim());
      current.fences.push({lang: fence[2], lines: lines.filter((l) => l !== '')});
    } else if (current) {
      current.text += ` ${line}`;
    } else {
      strays.push(line.trim());
    }
  }
  return {entries, strays};
}

function boxErrors({box, level, spec, planLevel}) {
  const errors = [];
  const {entries, strays} = parseBox(box.content);
  const expected = ['Goal', 'Start branch', 'Catch-up', 'Time', ...(level >= 1 && level <= 3 ? ['Compare'] : [])];
  const labels = entries.map((e) => e.label);
  for (const label of new Set(labels)) {
    if (labels.filter((l) => l === label).length > 1) errors.push(`the at a glance box has ${label} twice`);
    if (!expected.includes(label)) {
      errors.push(
        label === 'Compare'
          ? 'the at a glance box has a Compare line, which only Levels 1 to 3 have'
          : `the at a glance box has an unexpected ${label} line`,
      );
    }
  }
  for (const label of expected) if (!labels.includes(label)) errors.push(`the at a glance box is missing the ${label} line`);
  if (errors.length === 0 && labels.join('|') !== expected.join('|')) {
    errors.push(`the at a glance box lines are out of order, expected ${expected.join(', ')}`);
  }
  for (const stray of strays) errors.push(`the at a glance box has unexpected content: "${stray}"`);

  const first = (label) => entries.find((e) => e.label === label);
  const goal = first('Goal');
  if (goal && planLevel && lowerFirst(normalise(goal.text)) !== lowerFirst(planLevel.goal)) {
    errors.push(`the Goal differs from the plan: expected "${planLevel.goal}", found "${normalise(goal.text)}"`);
  }
  const start = first('Start branch');
  if (start && normalise(start.text) !== spec.start) {
    errors.push(`the Start branch must be "${spec.start}", found "${normalise(start.text)}"`);
  }
  // Only the Catch-up line holds a fenced block: exactly one, the command, nothing else
  // on that line.
  for (const entry of entries) {
    if (entry.label !== 'Catch-up' && entry.fences.length > 0) {
      errors.push(`the at a glance box has a fenced block under the ${entry.label} line, only the Catch-up line has one`);
    }
  }
  const catchUp = first('Catch-up');
  const command = spec.catchUp;
  if (catchUp) {
    const [block] = catchUp.fences;
    const ok =
      normalise(catchUp.text) === '' && catchUp.fences.length === 1 && block.lang === 'bash' && block.lines.join('\n') === command;
    if (!ok) errors.push(`the Catch-up must be a fenced bash block of exactly "${command}", once, with nothing else on the Catch-up line`);
  }
  const time = first('Time');
  if (time && planLevel) {
    const match = /^(\d+) minutes$/.exec(normalise(time.text));
    if (!match) errors.push(`the Time must read "${planLevel.minutes} minutes", found "${normalise(time.text)}"`);
    else if (Number(match[1]) !== planLevel.minutes) {
      errors.push(`the Time is ${match[1]} minutes, the plan heading says ${planLevel.minutes} minutes`);
    }
  }
  const compare = first('Compare');
  if (compare && expected.includes('Compare') && normalise(compare.text) !== `\`solution/level-${level}\``) {
    errors.push(`the Compare must be "\`solution/level-${level}\`", found "${normalise(compare.text)}"`);
  }
  return errors;
}

// The position of the first element after the imports: {index, imports}.
function firstElement(lines) {
  const imports = [];
  let i = 0;
  for (; i < lines.length; i++) {
    if (lines[i].trim() === '') continue;
    if (!lines[i].startsWith('import ')) break;
    for (; i < lines.length; i++) {
      imports.push(lines[i].trim());
      if (/['"]\s*;?\s*$/.test(lines[i])) break;
    }
  }
  return {index: i < lines.length ? i : -1, imports};
}

function checklistErrors({lines, flags, bodyLine, planItems}) {
  const headings = lines.flatMap((line, index) => (!flags[index] && line.trimEnd() === CHECKLIST_HEADING ? [index] : []));
  if (headings.length !== 1) {
    return [
      headings.length === 0
        ? `no "${CHECKLIST_HEADING}" heading`
        : `${headings.length} "${CHECKLIST_HEADING}" headings, expected 1`,
    ];
  }
  let from = headings[0] + 1;
  while (from < lines.length && lines[from].trim() === '') from++;
  const {items, next} = readBullets(lines, from);
  if (items.length === 0) return [`"${CHECKLIST_HEADING}" must be followed by a task list of "- [ ] " items`];

  const errors = [];
  const found = items.map((item, index) => {
    const match = /^\[([ xX])\]\s+(.*)$/.exec(item);
    if (!match) {
      errors.push(`checklist item ${index + 1} is not a task list item ("- [ ] " followed by the text)`);
      return undefined;
    }
    if (match[1] !== ' ') errors.push(`checklist item ${index + 1} is checked, a task list item must be "- [ ]"`);
    return normalise(match[2]);
  });
  if (items.length !== planItems.length) {
    errors.push(`the checklist has ${items.length} items, the plan has ${planItems.length}`);
  }
  for (let i = 0; i < Math.min(found.length, planItems.length); i++) {
    if (found[i] !== undefined && found[i] !== planItems[i]) {
      errors.push(`checklist item ${i + 1} differs from the plan: expected "${planItems[i]}", found "${found[i]}"`);
    }
  }
  const after = lines.findIndex((line, index) => index >= next && line.trim() !== '');
  if (after !== -1) {
    errors.push(`something follows the checklist (line ${bodyLine + after + 1}: "${lines[after].trim()}"), the checklist ends the page`);
  }
  return errors;
}

// Rules every page follows: no body H1, no space-form admonitions, no sync moment, the
// checkpoint convention, and the contractual anchors of the page.
function commonErrors({lines, flags, bodyLine, anchors}) {
  const errors = [];
  lines.forEach((line, index) => {
    // Every H1 that Markdown renders: ATX with up to three spaces of indent (four is code),
    // and setext, a text line underlined with "=".
    if (!flags[index] && /^ {0,3}#(?!#)/.test(line)) errors.push(`line ${bodyLine + index + 1}: a body H1 ("${line.trim()}"), the title comes from the frontmatter`);
    if (!flags[index] && index > 0 && !flags[index - 1] && lines[index - 1].trim() !== '' && /^ {0,3}=+\s*$/.test(line)) {
      errors.push(`line ${bodyLine + index}: a setext H1 ("${lines[index - 1].trim()}" underlined with ===), the title comes from the frontmatter`);
    }
    if (/^\s*:::[A-Za-z]+[ \t]+\S/.test(line)) {
      errors.push(`line ${bodyLine + index + 1}: admonition in the space form ("${line.trim()}"), use the bracket form, for example :::tip[Checkpoint]`);
    }
  });
  const {done, unclosed} = admonitions(lines, flags);
  for (const frame of unclosed) errors.push(`line ${bodyLine + frame.start + 1}: the ${frame.type} admonition is never closed`);
  // The workshop has no sync moments (spec 15, decision D19): no admonition of any type may carry one in its
  // title, whatever the case or spacing, on any page. A box that is never closed is banned too.
  for (const box of [...done, ...unclosed]) {
    if (SYNC_MOMENT_TITLE.test(box.title ?? '')) {
      errors.push(`line ${bodyLine + box.start + 1}: an admonition titled "${box.title}" is a sync moment, the workshop has none, Maik and Tom walk around instead`);
    }
  }
  for (const box of done) {
    const where = `line ${bodyLine + box.start + 1}`;
    if (box.type === 'tip' && box.title === 'Checkpoint') {
      if (!box.content.some((line) => /^\s*(`{3,}|~{3,})/.test(line))) errors.push(`${where}: the Checkpoint has no fenced block with the command`);
      if (!box.content.some((line) => line.trim().startsWith('Expected'))) errors.push(`${where}: the Checkpoint has no line starting with "Expected"`);
    }
  }
  // The heading id is the comment form of Docusaurus, `## Title {/* #id */}`: with
  // future.v4 (docusaurus.config.ts) the `{#id}` form does not compile in MDX.
  for (const id of anchors) {
    const found = lines.some((line, index) => !flags[index] && new RegExp(`^## .+ \\{/\\* #${id} \\*/\\}\\s*$`).test(line));
    if (!found) errors.push(`the contractual anchor #${id} is missing: an H2 heading must end with {/* #${id} */}`);
  }
  return {errors, done};
}

function frontmatterErrors({front, title, position, slug}) {
  if (front === null) return ['no frontmatter, every page starts with a --- block holding its title'];
  const errors = [];
  const raw = frontValue(front, 'title');
  if (raw === undefined) errors.push('the frontmatter has no title');
  else {
    const quoted = /^(["']).*\1$/.test(raw);
    const value = quoted ? raw.slice(1, -1) : raw;
    if (value.includes(':') && !quoted) errors.push(`the title "${value}" holds a colon and must be quoted`);
    if (value !== title) errors.push(`the title must be "${title}", found "${value}"`);
  }
  const found = frontValue(front, 'sidebar_position');
  if (found !== String(position)) errors.push(`sidebar_position must be ${position}, found ${found ?? 'none'}`);
  if (slug !== undefined && frontValue(front, 'slug') !== slug) {
    errors.push(`the slug must be ${slug}, found ${frontValue(front, 'slug') ?? 'none'}`);
  }
  return errors;
}

function levelPageErrors({text, level, spec, planLevel}) {
  const {front, body, bodyLine} = splitFrontmatter(text);
  const lines = body.split('\n');
  const flags = fenceFlags(lines);
  const errors = frontmatterErrors({front, title: spec.title, position: level + 3});

  const common = commonErrors({lines, flags, bodyLine, anchors: spec.anchors});
  errors.push(...common.errors);

  // LevelProgress: imported, first after the imports, with this page's level, once.
  const {index, imports} = firstElement(lines);
  if (!imports.includes(PROGRESS_IMPORT)) errors.push(`the page must import LevelProgress: ${PROGRESS_IMPORT}`);
  const wanted = `<LevelProgress current={${level}} />`;
  const progress = index === -1 ? null : /^<LevelProgress current=\{([^}]*)\} \/>$/.exec(lines[index].trim());
  let progressFirst = false;
  if (!progress) {
    errors.push(`the first element after the imports must be ${wanted}, found ${index === -1 ? 'nothing' : `"${lines[index].trim()}"`}`);
  } else if (progress[1] !== String(level)) {
    errors.push(`LevelProgress has current={${progress[1]}} but this is Level ${level}, expected ${wanted}`);
  } else {
    progressFirst = true;
  }
  const occurrences = lines.filter((line, i) => !flags[i] && line.includes('<LevelProgress')).length;
  if (occurrences > 1) errors.push(`LevelProgress appears ${occurrences} times, expected once`);

  // The at a glance box directly after it.
  const boxes = common.done.filter((a) => a.type === 'note' && a.title === 'At a glance');
  if (boxes.length !== 1) {
    errors.push(boxes.length === 0 ? `the page has no ${BOX_OPENER} box` : `the page has ${boxes.length} ${BOX_OPENER} boxes, expected 1`);
  } else {
    if (progressFirst) {
      const next = lines.findIndex((line, i) => i > index && line.trim() !== '');
      if (next !== boxes[0].start) errors.push(`the ${BOX_OPENER} box must follow LevelProgress directly`);
    }
    errors.push(...boxErrors({box: boxes[0], level, spec, planLevel}));
  }

  // The checklist ends the page and equals the plan. The marker exempts neither.
  if (planLevel) errors.push(...checklistErrors({lines, flags, bodyLine, planItems: planLevel.items}));

  return errors;
}

// The marker rule of the bonus page (spec 16): the page opens with the optional-bonus box, so a participant who is behind
// sees at once that the page may be skipped and where to go. The box is the first element of the body: only blank lines
// and whole-line MDX comments (the skeleton marker is one) may come before it, because they render nothing. A paragraph,
// a heading, an import line, another admonition or a comment that shares its line with text may not. The box links to
// the Wrap-up from inside the box, and the page has no level bar (it is no level).
const COMMENT_LINE = /^\s*(?:\{\/\*(?:(?!\*\/).)*\*\/\}\s*)+$/;
const WRAP_UP_LINK = '](./06-wrap-up.md)';

function bonusMarkerErrors({lines, flags, bodyLine, done}) {
  const errors = [];
  const boxes = done.filter((box) => box.type === 'info' && box.title === BONUS_TITLE);
  if (boxes.length !== 1) {
    errors.push(boxes.length === 0 ? `the page has no ${BONUS_OPENER} box` : `the page has ${boxes.length} ${BONUS_OPENER} boxes, expected 1`);
  }
  if (boxes.length > 0) {
    const [box] = boxes;
    const stray = lines.findIndex((line, index) => index < box.start && line.trim() !== '' && !COMMENT_LINE.test(line));
    if (stray !== -1) {
      errors.push(
        `line ${bodyLine + stray + 1}: the ${BONUS_OPENER} box must be the first element of the page, found "${lines[stray].trim()}" before it (only blank lines and one-line MDX comments may come first)`,
      );
    }
    // The link is searched inside the box, with inline code removed (a link written as code is no link).
    if (!box.content.some((line) => line.replace(/`[^`]*`/g, '').includes(WRAP_UP_LINK))) {
      errors.push(`the ${BONUS_OPENER} box must link to the Wrap-up with (./06-wrap-up.md), inside the box`);
    }
  }
  lines.forEach((line, index) => {
    if (!flags[index] && line.includes('LevelProgress')) {
      errors.push(`line ${bodyLine + index + 1}: the bonus page has no level bar, it must not import or render LevelProgress`);
    }
  });
  return errors;
}

function otherPageErrors({text, spec}) {
  const {front, body, bodyLine} = splitFrontmatter(text);
  const lines = body.split('\n');
  const flags = fenceFlags(lines);
  const common = commonErrors({lines, flags, bodyLine, anchors: spec.anchors});
  return [
    ...frontmatterErrors({front, title: spec.title, position: spec.position, slug: spec.slug}),
    ...common.errors,
    ...(spec.bonusMarker ? bonusMarkerErrors({lines, flags, bodyLine, done: common.done}) : []),
  ];
}

function categoryErrors(text) {
  let category;
  try {
    category = JSON.parse(text);
  } catch {
    return ['the file is not valid JSON'];
  }
  const errors = [];
  if (category.label !== 'Reference') errors.push(`the label must be "Reference", found ${JSON.stringify(category.label)}`);
  if (typeof category.position !== 'number' || category.position <= WRAP_UP_POSITION) {
    errors.push(`the position must be a number after Wrap-up (${WRAP_UP_POSITION}), found ${JSON.stringify(category.position)}`);
  }
  return errors;
}

// files: {path relative to guide/: text} of every file under docs/. nulFiles: the docs
// paths that hold a NUL byte. planText and sddText: the pack files, or null when they
// are missing (planPath and sddPath name them in that error). Returns {errors, summary}.
export function structureErrors({files, nulFiles = [], planText, planPath, sddText, sddPath}) {
  const errors = [];
  const add = (file, message) => errors.push(`structure: ${file}: ${message}`);

  for (const file of Object.keys(files).sort()) {
    if (!EXPECTED_FILES.has(file)) {
      errors.push(`structure: unexpected file ${file}, the guide has exactly the twelve pages and reference/_category_.json`);
    }
  }
  for (const file of [...EXPECTED_FILES].sort()) {
    if (!(file in files)) errors.push(`structure: missing ${file}, the guide has exactly the twelve pages and reference/_category_.json`);
  }
  for (const file of nulFiles) add(file, 'the file contains a NUL byte, docs files must be plain text');

  let plan = {levels: []};
  if (planText === null || planText === undefined) {
    errors.push(`structure: workshop plan: ${planPath} is missing; the guide is checked against it, and guide/ lives on the factory branch where the pack exists`);
  } else {
    plan = parsePlan(planText);
    for (const error of plan.errors) errors.push(`structure: ${error}`);
  }
  if (sddText === null || sddText === undefined) {
    errors.push(`structure: SDD framework guide: ${sddPath} is missing; it is checked against the Level 4 list`);
  } else {
    const sdd = parseSddChecklist(sddText);
    for (const error of sdd.errors) errors.push(`structure: ${error}`);
    const level4 = plan.levels[4]?.items;
    if (level4 && sdd.errors.length === 0) {
      const count = Math.max(level4.length, sdd.items.length);
      for (let i = 0; i < count; i++) {
        if (level4[i] !== sdd.items[i]) {
          errors.push(`structure: SDD framework guide: the "Level cleared when" list differs from the workshop plan's Level 4 list at item ${i + 1}: plan "${level4[i] ?? ''}", guide "${sdd.items[i] ?? ''}"`);
        }
      }
    }
  }

  let pages = 0;
  LEVELS.forEach((spec, level) => {
    if (!(spec.file in files)) return;
    pages++;
    for (const message of levelPageErrors({text: files[spec.file].replace(/\r\n/g, '\n'), level, spec, planLevel: plan.levels[level]})) {
      add(spec.file, message);
    }
  });
  for (const spec of OTHER_PAGES) {
    if (!(spec.file in files)) continue;
    pages++;
    for (const message of otherPageErrors({text: files[spec.file].replace(/\r\n/g, '\n'), spec})) add(spec.file, message);
  }
  if (CATEGORY_FILE in files) for (const message of categoryErrors(files[CATEGORY_FILE])) add(CATEGORY_FILE, message);

  return {errors, summary: `${pages} pages, Level 0 to 4 against the workshop plan`};
}
