// Pins the contrast pair table of the guide (decision G-6): the pairs that the card
// requires are in it, by name, with their variables, and the table does not shrink.
// Without this a pair could be deleted from rules/contrast-pairs.mjs and the checker
// would stay green, because a pair that is not there is never evaluated.
import {test} from 'node:test';
import assert from 'node:assert/strict';
import {CONTRAST_PAIRS} from './rules/contrast-pairs.mjs';
import {readFileSync} from 'node:fs';
import {dirname, join} from 'node:path';
import {fileURLToPath} from 'node:url';
import {evaluatePairs} from './rules/contrast.mjs';

const byName = (name) => CONTRAST_PAIRS.filter((pair) => pair.name === name);

// The number of pairs when this test was written. It may only grow: raise it together
// with a new pair, never lower it to make a removal pass.
const MINIMUM_PAIR_COUNT = 60;

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

// Spec 14, T1: the navbar logo is a graphic of one colour per mode (slate on the light bar, white on the
// slate bar), so it is a non-text pair like any other accent; logo.test.mjs ties the variable to the PNG.
test('contrast pairs: the navbar logo colour is a non-text pair on the navbar background', () => {
  assert.deepEqual(byName('navbar logo'), [
    {name: 'navbar logo', fg: '--guide-navbar-logo', bg: '--ifm-navbar-background-color', kind: 'nontext'},
  ]);
});

test('contrast pairs: the admonition types are exactly the six of Docusaurus', () => {
  const types = CONTRAST_PAIRS.map((pair) => /^admonition (\w+) text$/.exec(pair.name)?.[1]).filter(Boolean);
  assert.deepEqual(types, ['note', 'tip', 'info', 'warning', 'danger', 'caution']);
});

// Spec 14, T2: the level progress crumbs. Every label is a text pair (4.5:1), including the current one
// (white on the AA blue #3371B8, 5.01:1; the exact deck blue #3C7EC9 is 4.18:1 and may not carry text). The
// outlines that carry a state, the check mark and the focus ring are non-text pairs (3:1).
const CRUMB_PAIRS = [
  ['level progress done label', '--guide-level-done-fg', '--guide-level-done-bg', 'text'],
  ['level progress current label', '--guide-level-current-fg', '--guide-level-current-bg', 'text'],
  ['level progress inactive label', '--guide-level-inactive-fg', '--guide-level-inactive-bg', 'text'],
  ['level progress done fill', '--guide-level-done-bg', '--ifm-background-color', 'nontext'],
  ['level progress done outline', '--guide-level-done-border', '--ifm-background-color', 'nontext'],
  ['level progress done check mark', '--guide-level-done-check', '--guide-level-done-bg', 'nontext'],
  ['level progress current fill', '--guide-level-current-bg', '--ifm-background-color', 'nontext'],
  ['level progress current outline', '--guide-level-current-border', '--ifm-background-color', 'nontext'],
  ['level progress inactive outline', '--guide-level-inactive-border', '--ifm-background-color', 'nontext'],
  ['level progress focus ring', '--guide-level-focus', '--ifm-background-color', 'nontext'],
];

for (const [name, fg, bg, kind] of CRUMB_PAIRS) {
  test(`contrast pairs: the crumb pair "${name}" is declared once, ${kind}, on ${fg} and ${bg}`, () => {
    assert.deepEqual(byName(name), [{name, fg, bg, kind}]);
  });
}

test('contrast pairs: every level progress pair is one of the ten crumb pairs, none is dropped to a lower kind', () => {
  const names = CONTRAST_PAIRS.filter((pair) => pair.name.startsWith('level progress')).map((pair) => pair.name);
  assert.deepEqual(names.toSorted(), CRUMB_PAIRS.map(([name]) => name).toSorted());
  assert.deepEqual(
    CONTRAST_PAIRS.filter((pair) => pair.name.startsWith('level progress') && pair.name.endsWith('label')).map((pair) => pair.kind),
    ['text', 'text', 'text'],
  );
});

const CSS = readFileSync(join(dirname(fileURLToPath(import.meta.url)), '..', 'src', 'css', 'custom.css'), 'utf8');

// A fill of #3C7EC9 under the white current label is 4.18:1, below the 4.5:1 of a text pair: the pair is
// really evaluated as text, in both modes, with the real stylesheet.
test('contrast pairs: a current fill of #3C7EC9 fails the current label pair at 4.18 in light and dark mode', () => {
  const swapped = CSS.replace('--guide-level-current-bg: #3371B8;', '--guide-level-current-bg: #3C7EC9;');
  assert.notEqual(swapped, CSS);
  const {errors} = evaluatePairs({css: swapped, pairs: CONTRAST_PAIRS, darkMode: true});
  const label = errors.filter((error) => error.includes('level progress current label'));
  assert.equal(label.length, 2, errors.join('\n'));
  assert.ok(label.every((error) => /is 4\.18:1, below 4\.5/.test(error)), label.join('\n'));
  const real = evaluatePairs({css: CSS, pairs: CONTRAST_PAIRS, darkMode: true});
  assert.deepEqual(real.errors, []);
  const current = real.results.filter((result) => result.name === 'level progress current label');
  assert.deepEqual(current.map((result) => [result.mode, result.fgHex.toUpperCase(), result.bgHex.toUpperCase(), result.min]), [
    ['light', '#FFFFFF', '#3371B8', 4.5],
    ['dark', '#FFFFFF', '#3371B8', 4.5],
  ]);
});
