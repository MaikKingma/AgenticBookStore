import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {dirname, join} from 'node:path';
import {fileURLToPath} from 'node:url';
import {scanMarkdown} from './lib/mdscan.mjs';

// Where a participant starts and how they catch up (D17: level-0-start is a tag, not a branch). This reads the
// live guide/docs on purpose, like live-guide.test.mjs, and uses the shared Markdown scanner: it asserts what
// the pages tell a participant to type, so a rename of the start cannot leave a page or the far-behind help behind.
const DOCS = join(dirname(fileURLToPath(import.meta.url)), '..', 'docs');

const LEVEL_PAGES = [
  '01-level-0-break-the-agent.mdx',
  '02-level-1-context-engineering.mdx',
  '03-level-2-give-your-agent-eyes.mdx',
  '04-level-3-behavioral-control.mdx',
  '05-level-4-spec-driven-development.mdx',
];
const LEVEL_0_CATCH_UP = 'git stash -u && git switch -C level-0 level-0-start';
const LEVEL_1_CATCH_UP = 'git stash -u && git switch -C level-1 level-0-start';
const START_COMMAND = './mvnw -pl bookstore spring-boot:run';

const scan = (file) => scanMarkdown(readFileSync(join(DOCS, file), 'utf8'));

// The first prose line that matches, as a line number, or -1. Fenced lines never match.
const proseLine = (scanned, pattern, from = 0) => scanned.lines.findIndex((line, i) => i >= from && line.fence === null && pattern.test(line.text));

// The fenced blocks of a line range: {lang, text} with the body lines joined by a newline.
const blocksBetween = (scanned, from, to) =>
  scanned.blocks.filter((block) => block.open > from && block.open < to).map((block) => ({lang: block.lang, text: block.body.join('\n')}));

// The prose and blocks from a H2 heading line to the next H2 (or the end of the page).
function section(scanned, headingPattern) {
  const from = proseLine(scanned, headingPattern);
  if (from === -1) return null;
  const next = proseLine(scanned, /^## /, from + 1);
  const to = next === -1 ? scanned.lines.length : next;
  return {
    from,
    to,
    text: scanned.lines.slice(from, to).map((line) => line.text).join('\n'),
    blocks: blocksBetween(scanned, from, to),
  };
}

// The at a glance box of a level page: its Start branch line and the one block under Catch-up.
function glance(file) {
  const scanned = scan(file);
  const start = proseLine(scanned, /^\*\*Start branch:\*\*/);
  const catchUp = proseLine(scanned, /^\*\*Catch-up:\*\*/);
  const time = proseLine(scanned, /^\*\*Time:\*\*/);
  assert.notEqual(catchUp, -1, `${file} has no Catch-up line`);
  return {
    start: start === -1 ? '' : scanned.lines[start].text.replace(/^\*\*Start branch:\*\*\s*/, '').trim(),
    catchUp: blocksBetween(scanned, catchUp, time).map((block) => block.text),
  };
}

test('Levels 0 and 1 catch up on an own branch at the tag level-0-start', () => {
  const level0 = glance(LEVEL_PAGES[0]);
  assert.deepEqual(level0.catchUp, [LEVEL_0_CATCH_UP]);
  assert.equal(level0.start, '`main` (tag `level-0-start`)');

  const level1 = glance(LEVEL_PAGES[1]);
  assert.deepEqual(level1.catchUp, [LEVEL_1_CATCH_UP]);
  assert.equal(level1.start, '`level-0-start`');

  // The Level 1 prose starts from the tag too, not from main.
  const prose = scan(LEVEL_PAGES[1]).lines.filter((line) => line.fence === null).map((line) => line.text).join('\n');
  assert.doesNotMatch(prose, /Start on `main`/);
});

test('the far-behind help gives the catch-up of every level', () => {
  // The literal commands of the five levels: Levels 0 and 1 from the tag, 2 and 3 from their solution branch,
  // Level 4 from its track start branch.
  const expected = [
    LEVEL_0_CATCH_UP,
    LEVEL_1_CATCH_UP,
    'git stash -u && git switch solution/level-1',
    'git stash -u && git switch solution/level-2',
    'git stash -u && git switch start/level-4-<track>',
  ];
  // The scanner blanks the heading id comment ({/* #far-behind */}), the structure rule pins that anchor.
  const help = section(scan('reference/troubleshooting.md'), /^## I am far behind\s*$/);
  assert.notEqual(help, null, 'troubleshooting has no #far-behind section');
  const bash = help.blocks.filter((block) => block.lang === 'bash').map((block) => block.text);
  for (const [level, command] of expected.entries()) {
    assert.ok(bash.includes(command), `the far-behind section has no fenced bash block of exactly "${command}" (Level ${level}), found ${JSON.stringify(bash)}`);
  }
  // And each block is the command the level page itself shows, so help and pages cannot drift apart.
  for (const [level, page] of LEVEL_PAGES.entries()) {
    assert.deepEqual(glance(page).catchUp, [expected[level]], `${page} at a glance`);
  }
  // No generic form is left.
  for (const block of help.blocks) {
    assert.doesNotMatch(block.text, /solution\/level-N|<branch>/, 'the far-behind section keeps a generic catch-up form');
  }

  const index = scan('index.md');
  const generic = index.lines.filter((line) => /git switch <branch>/.test(line.text));
  assert.deepEqual(generic.map((line) => line.text), []);
  const indexText = index.lines.map((line) => line.text).join('\n');
  assert.ok(indexText.includes('(./reference/troubleshooting.md#far-behind)'), 'index.md does not link to ./reference/troubleshooting.md#far-behind');
});

test('the setup page starts the app in the terminal that holds the key', () => {
  const setup = scan('00-setup-check.md');
  const key = proseLine(setup, /^## Set the workshop key\s*$/);
  const start = proseLine(setup, /^## Start the app\s*$/);
  const model = proseLine(setup, /^## Choose the model\s*$/);
  assert.notEqual(start, -1, 'the setup page has no "## Start the app" section');
  assert.ok(key !== -1 && key < start && start < model, `the sections must run Set the workshop key (${key}), Start the app (${start}), Choose the model (${model})`);

  const run = section(setup, /^## Start the app\s*$/);
  assert.deepEqual(run.blocks, [{lang: 'bash', text: START_COMMAND}]);
  assert.ok(run.text.includes('http://localhost:8080/shop/'), 'the section does not name http://localhost:8080/shop/');

  const level0 = scan(LEVEL_PAGES[0]).lines.map((line) => line.text).join('\n');
  assert.ok(level0.includes('(./00-setup-check.md#start-the-app)'), 'Level 0 does not link to ./00-setup-check.md#start-the-app');
});
