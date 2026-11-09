import {test} from 'node:test';
import assert from 'node:assert/strict';
import {parsePlan, parseSddChecklist} from './rules/plan.mjs';
import {FIXTURE_LEVELS, fixturePlan, fixtureSdd} from './lib/structure-fixtures.mjs';

const normalise = (text) => text.replace(/\s+/g, ' ').trim();

test('parsePlan: reads the five levels with minutes, goal and checklist', () => {
  const {levels, errors} = parsePlan(fixturePlan());
  assert.deepEqual(errors, []);
  assert.equal(levels.length, 5);
  levels.forEach((level, n) => {
    const want = FIXTURE_LEVELS[n];
    assert.equal(level.level, n);
    assert.equal(level.minutes, want.minutes);
    assert.equal(level.goal, normalise(want.goal));
    assert.deepEqual(level.items, want.items.map(normalise));
  });
});

test('parsePlan: a goal and a bullet that wrap over continuation lines are joined, markdown kept', () => {
  const {levels} = parsePlan(fixturePlan());
  assert.equal(levels[2].goal, FIXTURE_LEVELS[2].goal);
  assert.ok(levels[1].items[2].includes('`./mvnw verify`'));
  assert.ok(levels[0].items[0].endsWith('the wrong file).'));
});

test('parsePlan: the minutes are the last (N min) of the heading, not the one in the name', () => {
  const plan = fixturePlan().replace('Level 2: Give your agent eyes (LSP + MCP) (20 min)', 'Level 2: Give your agent eyes (LSP + MCP (8 min)) (20 min)');
  assert.equal(parsePlan(plan).levels[2].minutes, 20);
});

test('parsePlan: the quote of "Level cleared when" inside a table is not a list', () => {
  const {errors} = parsePlan(fixturePlan());
  assert.deepEqual(errors, []);
});

test('parsePlan: a sixth "Level cleared when" block fails the count guard', () => {
  const plan = `${fixturePlan()}\n**Level cleared when**\n- Something extra.\n`;
  const {errors} = parsePlan(plan);
  assert.ok(errors.some((e) => /6 .*Level cleared when/.test(e) && /expected 5/.test(e)), errors.join(' | '));
});

test('parsePlan: a missing level section is an error naming the level', () => {
  const plan = fixturePlan().replace('## 6. Level 2:', '## 6. Chapter 2:');
  const {errors} = parsePlan(plan);
  assert.ok(errors.some((e) => /Level 2/.test(e) && /no section/.test(e)), errors.join(' | '));
});

test('parsePlan: an empty checklist is an error (anti-vacuity)', () => {
  const l = FIXTURE_LEVELS[0].items[0];
  const plan = fixturePlan().replace(`- ${l.slice(0, 20)}`, 'Not a bullet: ');
  const {errors} = parsePlan(plan);
  assert.ok(errors.some((e) => /Level 0/.test(e) && /no checklist items/.test(e)), errors.join(' | '));
});

test('parsePlan: a level without a Goal is an error', () => {
  const plan = fixturePlan().replace('**Goal:** let the agent', 'Aim: let the agent');
  const {errors} = parsePlan(plan);
  assert.ok(errors.some((e) => /Level 2/.test(e) && /no Goal/.test(e)), errors.join(' | '));
});

test('parseSddChecklist: reads the all-tracks list', () => {
  const {items, errors} = parseSddChecklist(fixtureSdd());
  assert.deepEqual(errors, []);
  assert.deepEqual(items, FIXTURE_LEVELS[4].items.map(normalise));
});

test('parseSddChecklist: a missing or duplicated heading is an error', () => {
  assert.ok(parseSddChecklist('# Guide\n').errors.some((e) => /no .*Level cleared when/.test(e)));
  const twice = `${fixtureSdd()}\n### Level cleared when\n- x\n`;
  assert.ok(parseSddChecklist(twice).errors.some((e) => /2 .*Level cleared when/.test(e)));
});

test('parseSddChecklist: a heading followed by prose instead of a list is an error, with no items', () => {
  const {items, errors} = parseSddChecklist('# Guide\n\n### Level cleared when (all tracks)\nProse, not a list.\n');
  assert.deepEqual(items, []);
  assert.deepEqual(errors, ['SDD framework guide: the "Level cleared when" heading has no list']);
});
