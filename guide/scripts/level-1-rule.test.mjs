import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import {scanMarkdown} from './lib/mdscan.mjs';

// Spec 15 (decision D19), Level 1: step 3 shows the complete rule file `.claude/rules/domain.md`, step 5 shows an
// illustrative text mock of `/context`, and one "Make it yours" tip invites the participant's own rule. The rule file
// on the page and in the plan must be byte-equal to the staged copy of the level-1 solution branch, so a later edit of
// page and plan together cannot drift away from the branch. The mock must add up, because a mock that does not add up
// teaches the wrong picture of the context window.
const PAGE = fileURLToPath(new URL('../docs/02-level-1-context-engineering.mdx', import.meta.url));
const PLAN = fileURLToPath(new URL('../../docs/spec-pack/workshop/workshop-plan.md', import.meta.url));
const STAGED = fileURLToPath(new URL('../../docs/spec-pack/level-branches/level-1/files/dot-claude/rules/domain.md', import.meta.url));

const read = (file) => readFileSync(file, 'utf8').replace(/\r\n/g, '\n');
const words = (text) => text.split(/\s+/).filter((word) => word !== '');

// The fenced blocks of a text as {lang, open, close, body}: the body without the indent of the list item that holds the
// fence (the plan nests its fences in numbered steps, the page does not).
function blocksOf(text) {
  const lines = text.split('\n');
  return scanMarkdown(text).blocks.map((block) => {
    const indent = /^\s*/.exec(lines[block.open])[0];
    return {...block, body: block.body.map((line) => (line.startsWith(indent) ? line.slice(indent.length) : line))};
  });
}

const DOMAIN_MARKER = '# Domain layer';

// The errors of the rule file on a page and on a plan against the staged file.
export function ruleFileErrors({page, plan, staged}) {
  const errors = [];
  for (const [name, text] of [['page', page], ['plan', plan]]) {
    const block = blocksOf(text).filter((b) => b.body.includes(DOMAIN_MARKER));
    if (block.length !== 1) {
      errors.push(`the ${name} has ${block.length} fenced blocks with "${DOMAIN_MARKER}", expected 1`);
      continue;
    }
    const got = `${block[0].body.join('\n')}\n`;
    if (got !== staged) errors.push(`the ${name} block of .claude/rules/domain.md differs from the staged file`);
  }
  return errors;
}

test('the rule file on the page and in the plan is byte-equal to the staged level-1 rule file', () => {
  assert.deepEqual(ruleFileErrors({page: read(PAGE), plan: read(PLAN), staged: read(STAGED)}), []);
});

test('the staged rule file is the one the page names: path, header and body', () => {
  const staged = read(STAGED);
  assert.ok(staged.startsWith('---\npaths:\n  - "bookstore/src/main/java/**/domain/**"\n---\n# Domain layer\n'));
  assert.ok(read(PAGE).includes('`.claude/rules/domain.md`'));
});

test('a changed glob character on the page, on the plan, or on both together fails the comparison (witnesses)', () => {
  const page = read(PAGE);
  const plan = read(PLAN);
  const staged = read(STAGED);
  const glob = '"bookstore/src/main/java/**/domain/**"';
  const edit = (text) => text.replace(glob, '"bookstore/src/main/java/*/domain/**"');
  assert.ok(page.includes(glob) && plan.includes(glob), 'both carry the glob');
  assert.match(ruleFileErrors({page: edit(page), plan, staged}).join('\n'), /the page block of \.claude\/rules\/domain\.md differs/);
  assert.match(ruleFileErrors({page, plan: edit(plan), staged}).join('\n'), /the plan block of \.claude\/rules\/domain\.md differs/);
  assert.equal(ruleFileErrors({page: edit(page), plan: edit(plan), staged}).length, 2, 'page and plan changed together still differ from the staged file');
  // A dropped line, a trailing blank line and a missing closing marker are differences too.
  assert.equal(ruleFileErrors({page: page.replace('- Value objects are records that validate in the compact constructor.\n', ''), plan, staged}).length, 1);
  assert.equal(ruleFileErrors({page: page.replace('---\n# Domain layer', '# Domain layer'), plan, staged}).length, 1);
  assert.equal(ruleFileErrors({page: page.replace('- Domain tests are plain JUnit and AssertJ, without a Spring context.\n', '- Domain tests are plain JUnit and AssertJ, without a Spring context.\n\n'), plan, staged}).length, 1);
});

test('step 3 names the other two rule files and explains the header in one sentence (YAML, root-relative globs, loaded on read or edit)', () => {
  const page = read(PAGE);
  const step = page.slice(page.indexOf('### 3. Add path-scoped rules'), page.indexOf('### 4.'));
  for (const name of ['`.claude/rules/domain.md`', '`data-and-migrations.md`', '`testing.md`']) assert.ok(step.includes(name), name);
  const sentence = step.split('\n').find((line) => /YAML/.test(line));
  assert.ok(sentence, 'a line explains the header');
  assert.match(sentence, /relative to the project root/);
  assert.match(sentence, /reads or edits/);
  assert.match(sentence, /not at session start/);
  assert.equal(sentence.split(/(?<=\.)\s/).length, 1, 'one sentence');
});

// A5 (F1, F3): no documentation page shows a path-scoped rule in /memory or /context, so the checkpoint is a behavioural
// check: ask for a change in `domain` and see that it follows a real line of the rule file. The Expected line is pinned in
// parts, each part a check of its own, and the rule line it quotes is read from the staged rule file.
const CHECKPOINT_PROMPT = "Let a book's title be corrected after it was added to the catalogue.";
export function checkpointErrors(page, staged) {
  const errors = [];
  const start = page.indexOf(':::tip[Checkpoint]');
  const box = page.slice(start, page.indexOf('\n:::', start + 5));
  const [block] = scanMarkdown(box).blocks;
  if (!block) return ['the checkpoint has no fenced block'];
  if (block.body.join('\n') !== CHECKPOINT_PROMPT) errors.push(`the checkpoint block is not the prompt "${CHECKPOINT_PROMPT}"`);
  const expected = box.split('\n').find((line) => line.startsWith('Expected:'));
  if (!expected) return [...errors, 'the checkpoint has no Expected line'];
  const ruleLine = staged.split('\n').find((line) => line.startsWith('- Change state through behaviour methods'))?.slice(2);
  const parts = [
    ['the answer Claude should give', 'Claude adds a method named after the business action (for example `correctTitle`), not a `setTitle`'],
    ['the quoted rule line', `"${ruleLine}"`],
    ['the documented trigger', 'A path-scoped rule loads when Claude reads or edits a file that matches its `paths`'],
    ['the file that triggers it', 'when it opens `Book.java`, the rule is loaded.'],
  ];
  for (const [what, text] of parts) if (!expected.includes(text)) errors.push(`the Expected line lacks ${what}: ${text}`);
  if (/\/(memory|context)\b/.test(box)) errors.push('the checkpoint claims a command shows the rule (the docs do not say /memory or /context list path-scoped rules)');
  return errors;
}

test('the checkpoint after step 3 asks for a change in domain and pins every part of its Expected line', () => {
  const page = read(PAGE);
  const staged = read(STAGED);
  assert.ok(staged.includes('- Change state through behaviour methods named after the business action. No setters.'), 'the quoted line is in the rule file');
  assert.deepEqual(checkpointErrors(page, staged), []);
  assert.match(checkpointErrors(page.replace(/\nExpected:.*\n/, '\n'), staged).join('\n'), /no Expected line/);
  assert.match(checkpointErrors(page.replace(', not a `setTitle`', ''), staged).join('\n'), /lacks the answer Claude should give/);
  assert.match(checkpointErrors(page.replace(' No setters."', '"'), staged).join('\n'), /lacks the quoted rule line/);
  assert.match(checkpointErrors(page.replace(/ A path-scoped rule loads[^\n]*Book\.java`, the rule is loaded\./, ''), staged).join('\n'), /lacks the documented trigger[\s\S]*lacks the file that triggers it/);
  assert.match(checkpointErrors(page.replace("Let a book's title be corrected", "Let a book's price be corrected"), staged).join('\n'), /is not the prompt/);
  assert.match(checkpointErrors(page.replace('You do not have to keep the change.', 'Run `/memory` to see the rule.'), staged).join('\n'), /claims a command shows the rule/);
});

test('the Level 1 checklist item on the rule claims only that Claude follows it, not that a command shows it', () => {
  const page = read(PAGE);
  const item = page.slice(page.indexOf('## Level cleared when')).split('\n').find((line) => /domain/.test(line));
  assert.equal(item, '- [ ] Asked to change a class in `domain`, Claude follows `.claude/rules/domain.md`: a behaviour method named after the business action, no setters.');
  assert.doesNotMatch(page.slice(page.indexOf('## Level cleared when')), /\/(memory|context)\b/);
  assert.ok(read(PLAN).includes(item.replace('- [ ] ', '- ')), 'the plan carries the same item');
});

// The mock: a header, then the grid beside its legend, then the footer. The grid has 10 rows of 10 symbols.
const CELL_K = 2;
export function mockErrors(lines) {
  const errors = [];
  const header = /^Context usage: (\d+)k of (\d+)k tokens \((\d+)%\)$/.exec(lines[0] ?? '');
  if (!header) return ['the first line is not "Context usage: Nk of Nk tokens (N%)"'];
  const [used, total, usedPercent] = header.slice(1).map(Number);
  const legend = [];
  const grid = [];
  for (const line of lines.slice(1)) {
    const row = /^((?:\S ){9}\S)(?:\s{2,}(\S)\s{2,}(.+?)\s+(\d+)k\s+\((\d+)%\))?$/.exec(line);
    if (row) {
      grid.push(row[1].split(' '));
      if (row[2]) legend.push({symbol: row[2], label: row[3], tokens: Number(row[4]), percent: Number(row[5])});
    }
  }
  if (grid.length !== 10) errors.push(`the grid has ${grid.length} rows, expected 10`);
  const cells = grid.flat();
  if (cells.length !== 100) errors.push(`the grid has ${cells.length} cells, expected 100`);
  if (new Set(legend.map((entry) => entry.symbol)).size !== legend.length || legend.length < 4) errors.push('the legend has fewer than 4 categories or repeats a symbol');
  for (const entry of legend) {
    const count = cells.filter((cell) => cell === entry.symbol).length;
    if (count * CELL_K !== entry.tokens) errors.push(`${entry.label}: ${count} cells of ${CELL_K}k are ${count * CELL_K}k, the legend says ${entry.tokens}k`);
    if (Math.round((entry.tokens / total) * 100) !== entry.percent) errors.push(`${entry.label}: ${entry.tokens}k of ${total}k is not ${entry.percent}%`);
  }
  for (const symbol of new Set(cells)) if (!legend.some((entry) => entry.symbol === symbol)) errors.push(`the symbol "${symbol}" is in the grid but not in the legend`);
  const free = legend.find((entry) => /^Free space$/.test(entry.label));
  if (!free) errors.push('no Free space category');
  const taken = legend.filter((entry) => entry !== free).reduce((sum, entry) => sum + entry.tokens, 0);
  if (taken !== used) errors.push(`the categories without free space add up to ${taken}k, the header says ${used}k used`);
  if (free && used + free.tokens !== total) errors.push(`used ${used}k plus free ${free?.tokens}k is not the window of ${total}k`);
  if (Math.round((used / total) * 100) !== usedPercent) errors.push(`${used}k of ${total}k is not ${usedPercent}%`);
  if (legend.reduce((sum, entry) => sum + entry.percent, 0) !== 100) errors.push('the percentages do not add up to 100');
  return errors;
}

const MOCK_MARKER = /^Context usage: /;
const mockOf = (text, where) => {
  const found = blocksOf(text).filter((block) => MOCK_MARKER.test(block.body[0] ?? ''));
  assert.equal(found.length, 1, `${where} has exactly one /context mock`);
  return found[0];
};

test('the /context mock is ASCII, in a text fence, and adds up (the grid matches the legend, the legend matches the header)', () => {
  const block = mockOf(read(PAGE), 'the page');
  assert.equal(block.lang, 'text');
  for (const line of block.body) assert.doesNotMatch(line, /[^\x20-\x7E]/, line);
  assert.deepEqual(mockErrors(block.body), []);
});

test('the mock is the same on the page and in the plan', () => {
  assert.deepEqual(mockOf(read(PLAN), 'the plan').body, mockOf(read(PAGE), 'the page').body);
});

test('the mock checker kills each way a mock can stop adding up (witnesses)', () => {
  const lines = mockOf(read(PAGE), 'the page').body;
  assert.deepEqual(mockErrors(lines), []);
  const gridRows = lines.flatMap((line, index) => (/^(?:\S ){9}\S(?:\s|$)/.test(line) ? [index] : []));
  assert.equal(gridRows.length, 10);
  // Edit one line (by index) or every line (index -1) of the mock and return its errors.
  const edit = (index, from, to) => mockErrors(lines.map((line, at) => (index === -1 || at === index ? line.replace(from, to) : line))).join('\n');
  assert.match(mockErrors(lines.filter((line, at) => at !== gridRows[9])).join('\n'), /the grid has 9 rows/);
  assert.match(edit(gridRows[1], /^M M M M M M M M M M/, 'M M M M M M M M M .'), /Messages and tool results: 40 cells of 2k are 80k, the legend says 82k/);
  assert.match(edit(-1, '100k', '98k'), /Free space: 50 cells of 2k are 100k, the legend says 98k/);
  assert.match(edit(-1, '(7%)', '(8%)'), /is not 8%/);
  assert.match(edit(0, '100k of', '90k of'), /add up to 100k, the header says 90k used/);
  assert.match(edit(0, 'of 200k tokens', 'of 190k tokens'), /used 100k plus free 100k is not the window of 190k/);
  assert.match(edit(gridRows[0], 'C K M', 'C K X'), /the symbol "X" is in the grid but not in the legend/);
});

test('step 5 says in one pinned sentence that the grid is an illustration, not a screenshot', () => {
  const sentence = 'The grid below is an illustration, not a screenshot: your symbols, names and numbers differ.';
  for (const [name, text] of [['page', read(PAGE)], ['plan', read(PLAN)]]) assert.ok(text.replace(/\s+/g, ' ').includes(sentence), `the ${name} says: ${sentence}`);
});

// A5 (F2): the sentence "Each symbol stands for Nk tokens of a Nk window" must say what the mock draws: the size of one
// cell is CELL_K and the window is the total of the header line, on the page and in the plan.
const WINDOW_SENTENCE = /(\d+)k tokens of a (\d+)k window/;
export function windowSentenceErrors(text, mockLines) {
  const found = WINDOW_SENTENCE.exec(text.replace(/\s+/g, ' '));
  if (!found) return ['no sentence "Each symbol stands for Nk tokens of a Nk window"'];
  const [cell, windowK] = found.slice(1).map(Number);
  const total = Number(/of (\d+)k tokens/.exec(mockLines[0])?.[1]);
  const errors = [];
  if (cell !== CELL_K) errors.push(`the sentence says a symbol is ${cell}k, the mock is drawn with ${CELL_K}k cells`);
  if (windowK !== total) errors.push(`the sentence says a ${windowK}k window, the mock header says ${total}k`);
  return errors;
}

test('the sentence on the size of a symbol and of the window matches the mock, on the page and in the plan', () => {
  const page = read(PAGE);
  const plan = read(PLAN);
  const lines = mockOf(page, 'the page').body;
  assert.deepEqual(windowSentenceErrors(page, lines), []);
  assert.deepEqual(windowSentenceErrors(plan, lines), []);
  for (const [name, text] of [['page', page], ['plan', plan]]) {
    assert.match(windowSentenceErrors(text.replace('of a 200k window', 'of a 100k window'), lines).join('\n'), /says a 100k window, the mock header says 200k/, `${name}: window`);
    assert.match(windowSentenceErrors(text.replace('2k tokens of a', '3k tokens of a'), lines).join('\n'), /says a symbol is 3k, the mock is drawn with 2k cells/, `${name}: cell`);
    assert.match(windowSentenceErrors(text.replace('Each symbol stands for 2k tokens of a 200k window.', ''), lines).join('\n'), /no sentence/, `${name}: deleted`);
  }
});

// The annotations are the bullets between the mock and the three moves.
function annotationsOf(page) {
  const {close} = blocksOf(page).find((block) => MOCK_MARKER.test(block.body[0] ?? ''));
  const rest = page.split('\n').slice(close + 1);
  const part = rest.slice(0, rest.findIndex((line) => line.startsWith('Learn the three moves')));
  return part.filter((line) => /^- /.test(line)).map((line) => line.slice(2).trim());
}

test('step 5 has 3 to 5 annotations of at most 30 words that cover growth, free space, /compact focus and /clear', () => {
  const notes = annotationsOf(read(PAGE));
  assert.ok(notes.length >= 3 && notes.length <= 5, `${notes.length} annotations`);
  for (const note of notes) assert.ok(words(note).length <= 30, `${words(note).length} words: ${note}`);
  const all = notes.join(' ');
  assert.match(all, /grows/);
  assert.match(all, /Free space shrinks/);
  assert.match(all, /`\/compact focus on <topic>`/);
  assert.match(all, /`\/clear`/);
});

test('step 5 keeps its three moves and the 1M-token sentence', () => {
  const page = read(PAGE);
  const step = page.slice(page.indexOf('### 5. Watch for context rot'), page.indexOf('### 6.'));
  for (const text of ['- `/clear` between unrelated tasks.', '- `/compact focus on <topic>` when a long task drifts.', 'A 1M-token window is not an attention guarantee.']) assert.ok(step.includes(text), text);
});

test('the Level 1 page holds exactly one "Make it yours" tip of at most 60 words, outside the box, the checklist and any step list', () => {
  const page = read(PAGE);
  assert.equal(page.split(':::tip[Make it yours]').length - 1, 1, 'one tip');
  const start = page.indexOf(':::tip[Make it yours]');
  const body = page.slice(start + ':::tip[Make it yours]'.length, page.indexOf('\n:::', start + 5));
  assert.ok(words(body).length <= 60, `${words(body).length} words`);
  assert.match(body, /\brules?\b/);
  assert.ok(start > page.indexOf('### 6.'), 'after the last step heading text');
  assert.ok(start < page.indexOf('## Before you move on'), 'before the closing sections');
  assert.ok(start > page.indexOf('\n:::\n', page.indexOf(':::note[At a glance]')), 'not in the at a glance box');
  assert.ok(start < page.indexOf('## Level cleared when'), 'not in the checklist');
  assert.doesNotMatch(page.slice(0, start) + page.slice(page.indexOf('\n:::', start + 5)), /Make it yours/, 'nothing refers to the tip');
  assert.doesNotMatch(body, /^\s*(?:[-*]|\d+\.)\s/m, 'not a list of steps');
});
