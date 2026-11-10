// Pins the contrast pair table of the guide (decision G-6): the pairs that the card
// requires are in it, by name, with their variables, and the table does not shrink.
// Without this a pair could be deleted from rules/contrast-pairs.mjs and the checker
// would stay green, because a pair that is not there is never evaluated.
import {test} from 'node:test';
import assert from 'node:assert/strict';
import {CONTRAST_PAIRS} from './rules/contrast-pairs.mjs';

const byName = (name) => CONTRAST_PAIRS.filter((pair) => pair.name === name);

// The number of pairs when this test was written. It may only grow: raise it together
// with a new pair, never lower it to make a removal pass.
const MINIMUM_PAIR_COUNT = 53;

test('contrast pairs: the table holds at least the pinned number of pairs', () => {
  assert.ok(CONTRAST_PAIRS.length >= MINIMUM_PAIR_COUNT, `only ${CONTRAST_PAIRS.length} pairs`);
});

test('contrast pairs: names are unique and every pair is a text or non-text pair of two variables', () => {
  const names = CONTRAST_PAIRS.map((pair) => pair.name);
  assert.deepEqual(names.filter((name, i) => names.indexOf(name) !== i), []);
  for (const pair of CONTRAST_PAIRS) {
    assert.match(pair.fg, /^--[\w-]+$/, pair.name);
    assert.match(pair.bg, /^--[\w-]+$/, pair.name);
    assert.ok(['text', 'nontext'].includes(pair.kind), `${pair.name}: kind ${pair.kind}`);
  }
});

const REQUIRED = [
  ['body text', '--ifm-font-color-base', '--ifm-background-color'],
  ['headings', '--ifm-heading-color', '--ifm-background-color'],
  ['links', '--ifm-link-color', '--ifm-background-color'],
  ['link hover', '--ifm-link-hover-color', '--ifm-background-color'],
  ['inline code', '--ifm-font-color-base', '--ifm-code-background'],
  ['code block text', '--prism-color', '--prism-background-color'],
  ['navbar text', '--ifm-navbar-link-color', '--ifm-navbar-background-color'],
  ['navbar hover', '--ifm-navbar-link-hover-color', '--ifm-navbar-background-color'],
  ['navbar active', '--ifm-navbar-link-active-color', '--ifm-navbar-background-color'],
  ['level progress current label', '--guide-level-current-fg', '--guide-level-current-bg'],
  ['level progress inactive label', '--guide-level-inactive-fg', '--guide-level-inactive-bg'],
];

for (const [name, fg, bg] of REQUIRED) {
  test(`contrast pairs: the required text pair "${name}" is declared once, on ${fg} and ${bg}`, () => {
    assert.deepEqual(byName(name), [{name, fg, bg, kind: 'text'}]);
  });
}

// [admonition type, the Infima colour its variables use]. caution is an alias of
// warning in Docusaurus, so its pairs name the warning variables on purpose.
const ADMONITIONS = [
  ['note', 'secondary'],
  ['tip', 'success'],
  ['info', 'info'],
  ['warning', 'warning'],
  ['danger', 'danger'],
  ['caution', 'warning'],
];

for (const [type, colour] of ADMONITIONS) {
  test(`contrast pairs: the admonition type ${type} has its text pair on the ${colour} variables`, () => {
    assert.deepEqual(byName(`admonition ${type} text`), [
      {
        name: `admonition ${type} text`,
        fg: `--ifm-color-${colour}-contrast-foreground`,
        bg: `--ifm-color-${colour}-contrast-background`,
        kind: 'text',
      },
    ]);
  });
}

test('contrast pairs: the admonition types are exactly the six of Docusaurus', () => {
  const types = CONTRAST_PAIRS.map((pair) => /^admonition (\w+) text$/.exec(pair.name)?.[1]).filter(Boolean);
  assert.deepEqual(types, ['note', 'tip', 'info', 'warning', 'danger', 'caution']);
});
