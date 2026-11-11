// Spec 14, T2: the built-HTML rule of the level progress breadcrumb (rules/level-progress-built.mjs).
// The fixtures are hand-written slices of a real Pages build (minified: unquoted attributes, no </li>);
// the mutants are made by replacing one piece of a good slice. Nothing here reads guide/build: the rule
// runs in `npm run check:built` on the default build dir only, and the entry-point tests prove that switch
// on fixture builds (a copy of the scripts folder whose ../build is a fixture). The check mark element is
// empty (the stylesheet draws the character); a stray check character in a fixture is the reference &#10003;,
// so this file stays ASCII.
import {test} from 'node:test';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {cpSync, mkdirSync, mkdtempSync, readFileSync, realpathSync, rmSync, writeFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {dirname, join} from 'node:path';
import {fileURLToPath} from 'node:url';
import {levelProgressCrumbs, levelProgressErrors} from './rules/level-progress-built.mjs';
import {scanBuilt} from './rules/built.mjs';
import {cssVariables, resolveColor} from './rules/contrast.mjs';
import {matchingBrace, withoutBlockComments} from './lib/text.mjs';

const SCRIPTS = dirname(fileURLToPath(import.meta.url));
const BASE = '/AgenticBookStore/';
const ROUTES = [
  'level-0-break-the-agent',
  'level-1-context-engineering',
  'level-2-give-your-agent-eyes',
  'level-3-behavioral-control',
  'level-4-spec-driven-development',
];
const NAV_OPEN = '<nav aria-label="Level progress" class=progress_Rjlz><ol class=steps_tDzf>';
const NAV_CLOSE = '</ol></nav>';
const CHECK = '<span aria-hidden=true data-part=check class=check__JZm></span>';
const DONE_TEXT = '<span class=status_qfYd> (completed)</span>';
// The two empty spans that draw the chevron (aria-hidden; the rule does not read them).
const SHAPE = '<span aria-hidden=true data-part=edge class=edge_Qk2a></span><span aria-hidden=true data-part=fill class=fill_x7Lm></span>';

// Level 0 page: the current crumb first, four upcoming ones.
const CURRENT_0 =
  NAV_OPEN +
  '<li class=step_kWIg data-state=current><a class=link_MHCk aria-current=step href=/AgenticBookStore/level-0-break-the-agent><span>Level 0</span></a>' + SHAPE +
  '<li class=step_kWIg data-state=upcoming><a class=link_MHCk href=/AgenticBookStore/level-1-context-engineering><span>Level 1</span></a>' + SHAPE +
  '<li class=step_kWIg data-state=upcoming><a class=link_MHCk href=/AgenticBookStore/level-2-give-your-agent-eyes><span>Level 2</span></a>' + SHAPE +
  '<li class=step_kWIg data-state=upcoming><a class=link_MHCk href=/AgenticBookStore/level-3-behavioral-control><span>Level 3</span></a>' + SHAPE +
  '<li class=step_kWIg data-state=upcoming><a class=link_MHCk href=/AgenticBookStore/level-4-spec-driven-development><span>Level 4</span></a>' + SHAPE +
  NAV_CLOSE;

// Level 2 page: two done crumbs, the current one, two upcoming ones (the slice a real build gives).
const CURRENT_2 =
  NAV_OPEN +
  `<li class=step_kWIg data-state=done><a class=link_MHCk href=/AgenticBookStore/level-0-break-the-agent>${CHECK}<span>Level 0</span>${DONE_TEXT}</a>${SHAPE}` +
  `<li class=step_kWIg data-state=done><a class=link_MHCk href=/AgenticBookStore/level-1-context-engineering>${CHECK}<span>Level 1</span>${DONE_TEXT}</a>${SHAPE}` +
  '<li class=step_kWIg data-state=current><a class=link_MHCk aria-current=step href=/AgenticBookStore/level-2-give-your-agent-eyes><span>Level 2</span></a>' + SHAPE +
  '<li class=step_kWIg data-state=upcoming><a class=link_MHCk href=/AgenticBookStore/level-3-behavioral-control><span>Level 3</span></a>' + SHAPE +
  '<li class=step_kWIg data-state=upcoming><a class=link_MHCk href=/AgenticBookStore/level-4-spec-driven-development><span>Level 4</span></a>' + SHAPE +
  NAV_CLOSE;

// Level 4 page: four done crumbs, the current one last.
// (The two chevron spans of a real build come before the link; here they follow it. The rule does not read
// them, and the mutants below replace pieces of the link, so the order of the fixtures is the simpler one.)
const CURRENT_4 =
  NAV_OPEN +
  `<li class=step_kWIg data-state=done><a class=link_MHCk href=/AgenticBookStore/level-0-break-the-agent>${CHECK}<span>Level 0</span>${DONE_TEXT}</a>${SHAPE}` +
  `<li class=step_kWIg data-state=done><a class=link_MHCk href=/AgenticBookStore/level-1-context-engineering>${CHECK}<span>Level 1</span>${DONE_TEXT}</a>${SHAPE}` +
  `<li class=step_kWIg data-state=done><a class=link_MHCk href=/AgenticBookStore/level-2-give-your-agent-eyes>${CHECK}<span>Level 2</span>${DONE_TEXT}</a>${SHAPE}` +
  `<li class=step_kWIg data-state=done><a class=link_MHCk href=/AgenticBookStore/level-3-behavioral-control>${CHECK}<span>Level 3</span>${DONE_TEXT}</a>${SHAPE}` +
  '<li class=step_kWIg data-state=current><a class=link_MHCk aria-current=step href=/AgenticBookStore/level-4-spec-driven-development><span>Level 4</span></a>' + SHAPE +
  NAV_CLOSE;

const NAVS = {0: CURRENT_0, 2: CURRENT_2, 4: CURRENT_4};

// A docs page around a nav slice: the navbar (with an aria-current of its own kind), the sidebar with its
// aria-current=page link, the article with the slice. Other aria-current values are not the rule's business.
function page(nav, {file = 'level-2-give-your-agent-eyes.html'} = {}) {
  return (
    '<!doctype html><html><head><title>Level</title></head><body><div id=__docusaurus>' +
    '<nav aria-label=Main class="theme-layout-navbar navbar navbar--fixed-top"><a class=navbar__brand href=/AgenticBookStore/>Home</a></nav>' +
    '<div class=main-wrapper><aside><nav aria-label="Docs sidebar" class=menu><ul><li><a aria-current=page class="menu__link menu__link--active" href=/AgenticBookStore/' +
    file.replace(/\.html$/, '') +
    '>Level</a></ul></nav></aside><main><article><div class="theme-doc-markdown markdown"><header><h1>Level</h1></header>' +
    nav +
    '<p>Text.</p></div></article></main></div></div></body></html>\n'
  );
}

const errorsOf = (nav, level, file = 'level-2-give-your-agent-eyes.html') =>
  levelProgressErrors({file, html: page(nav, {file}), base: BASE, level, routes: ROUTES});

// A mutant differs from the good slice; replace() with a missing piece would otherwise pass for nothing.
function mutate(source, from, to) {
  assert.ok(source.includes(from), `the fixture has no ${from}`);
  const result = source.replace(from, to);
  assert.notEqual(result, source);
  return result;
}

const fails = (errors, message) => assert.ok(errors.some((error) => message.test(error)), errors.join('\n') || '(no error)');

// --- good pages: the done / current / upcoming split ------------------------------------------------

for (const [level, split] of [
  [0, ['current', 'upcoming', 'upcoming', 'upcoming', 'upcoming']],
  [2, ['done', 'done', 'current', 'upcoming', 'upcoming']],
  [4, ['done', 'done', 'done', 'done', 'current']],
]) {
  test(`level progress rule: a good Level ${level} page passes and its crumbs split as ${split.join(', ')}`, () => {
    assert.deepEqual(errorsOf(NAVS[level], level), []);
    const crumbs = levelProgressCrumbs(page(NAVS[level]));
    assert.deepEqual(
      crumbs.map((crumb) => crumb.state),
      split,
    );
    assert.deepEqual(
      crumbs.map((crumb) => crumb.name.startsWith(`Level ${crumbs.indexOf(crumb)}`)),
      [true, true, true, true, true],
    );
    assert.deepEqual(
      crumbs.map((crumb) => crumb.current),
      split.map((state) => state === 'current'),
    );
  });
}

test('level progress rule: errors name the file and start with "level progress:"', () => {
  const errors = errorsOf(mutate(CURRENT_2, 'aria-label="Level progress"', 'aria-label=Progress'), 2);
  assert.ok(errors.length >= 1);
  for (const error of errors) assert.match(error, /^level progress: level-2-give-your-agent-eyes\.html: /);
});

test('level progress rule: a check character written into the check mark fails (the stylesheet draws it)', () => {
  const raw = String.fromCharCode(0x2713);
  for (const glyph of ['&#10003;', raw, 'x']) {
    fails(errorsOf(mutate(CURRENT_2, CHECK, CHECK.replace('></span>', `>${glyph}</span>`)), 2), /check mark must hold no text/);
  }
});

test('level progress rule: a double-quoted attribute value reads like an unquoted one', () => {
  const quoted = CURRENT_2.replaceAll('data-state=done', 'data-state="done"').replaceAll('aria-current=step', 'aria-current="step"');
  assert.deepEqual(errorsOf(quoted, 2), []);
});

// --- the nav and its list ---------------------------------------------------------------------------

test('level progress rule: a wrong nav label fails', () => {
  for (const label of ['Progress', 'Levels', 'level progress', 'Level progress ']) {
    const errors = errorsOf(mutate(CURRENT_2, 'aria-label="Level progress"', `aria-label="${label}"`), 2);
    fails(errors, /no nav with aria-label "Level progress"/);
  }
});

test('level progress rule: a nav without a label fails', () => {
  fails(errorsOf(mutate(CURRENT_2, ' aria-label="Level progress"', ''), 2), /no nav with aria-label "Level progress"/);
});

test('level progress rule: a level page with no level progress nav fails', () => {
  fails(errorsOf('', 2), /no nav with aria-label "Level progress"/);
});

test('level progress rule: two level progress navs fail', () => {
  fails(errorsOf(CURRENT_2 + CURRENT_2, 2), /found 2 navs with aria-label "Level progress"/);
});

test('level progress rule: a list that is not an ol fails', () => {
  const ul = mutate(mutate(CURRENT_2, '<ol class=steps_tDzf>', '<ul class=steps_tDzf>'), '</ol>', '</ul>');
  fails(errorsOf(ul, 2), /must hold one ol/);
});

test('level progress rule: four crumbs and six crumbs fail', () => {
  const [head] = CURRENT_2.split('<li class=step_kWIg data-state=upcoming>');
  const four = head + '<li class=step_kWIg data-state=upcoming><a class=link_MHCk href=/AgenticBookStore/level-3-behavioral-control><span>Level 3</span></a>' + NAV_CLOSE;
  fails(errorsOf(four, 2), /five crumbs \(li\) in the list, found 4/);
  const six = mutate(
    CURRENT_2,
    NAV_CLOSE,
    '<li class=step_kWIg data-state=upcoming><a class=link_MHCk href=/AgenticBookStore/level-5-more><span>Level 5</span></a>' + NAV_CLOSE,
  );
  fails(errorsOf(six, 2), /five crumbs \(li\) in the list, found 6/);
});

test('level progress rule: crumbs in the wrong order fail', () => {
  const [open, ...items] = CURRENT_2.slice(0, -NAV_CLOSE.length).split('<li ');
  const swapped = open + ['<li ' + items[1], '<li ' + items[0], ...items.slice(2).map((item) => '<li ' + item)].join('') + NAV_CLOSE;
  assert.notEqual(swapped, CURRENT_2);
  const errors = errorsOf(swapped, 2);
  fails(errors, /crumb 1 .*must read Level 0/);
  fails(errors, /crumb 2 .*must read Level 1/);
});

// --- links ------------------------------------------------------------------------------------------

test('level progress rule: a crumb without a link fails', () => {
  const noLink = mutate(
    CURRENT_2,
    '<a class=link_MHCk href=/AgenticBookStore/level-3-behavioral-control><span>Level 3</span></a>',
    '<span>Level 3</span>',
  );
  fails(errorsOf(noLink, 2), /crumb 4 \(Level 3\) must hold exactly one link, found 0/);
});

test('level progress rule: a crumb with two links fails', () => {
  const two = mutate(CURRENT_2, '<span>Level 3</span></a>', '<span>Level 3</span></a><a href=/AgenticBookStore/>Home</a>');
  fails(errorsOf(two, 2), /crumb 4 \(Level 3\) must hold exactly one link, found 2/);
});

test('level progress rule: a crumb link that goes to another page, to the home page or outside the base fails', () => {
  for (const href of ['/AgenticBookStore/level-4-spec-driven-development', '/AgenticBookStore/', '/level-3-behavioral-control', 'https://example.org/', '#level-3']) {
    const wrong = mutate(CURRENT_2, 'href=/AgenticBookStore/level-3-behavioral-control>', `href=${href}>`);
    fails(errorsOf(wrong, 2), /crumb 4 \(Level 3\) link must go to \/AgenticBookStore\/level-3-behavioral-control/);
  }
  const none = mutate(CURRENT_2, ' href=/AgenticBookStore/level-3-behavioral-control', '');
  fails(errorsOf(none, 2), /crumb 4 \(Level 3\) link must go to \/AgenticBookStore\/level-3-behavioral-control \(found none\)/);
});

test('level progress rule: the crumb links go to the level pages of the build, whatever their names are', () => {
  const routes = ['level-0-a', 'level-1-b', 'level-2-c', 'level-3-d', 'level-4-e'];
  const html = page(CURRENT_2);
  const errors = levelProgressErrors({file: 'level-2-c.html', html, base: BASE, level: 2, routes});
  assert.equal(errors.filter((error) => /link must go to/.test(error)).length, 5, errors.join('\n'));
});

test('level progress rule: the base URL of the build is part of the href', () => {
  const offline = CURRENT_2.replaceAll('/AgenticBookStore/', '/');
  assert.deepEqual(levelProgressErrors({file: 'x.html', html: page(offline), base: '/', level: 2, routes: ROUTES}), []);
  assert.equal(levelProgressErrors({file: 'x.html', html: page(CURRENT_2), base: '/', level: 2, routes: ROUTES}).length, 5);
});

// --- data-state and the aria-current marker ---------------------------------------------------------

test('level progress rule: two current crumbs fail', () => {
  const two = mutate(
    CURRENT_2,
    '<li class=step_kWIg data-state=upcoming><a class=link_MHCk href=/AgenticBookStore/level-3-behavioral-control>',
    '<li class=step_kWIg data-state=current><a class=link_MHCk aria-current=step href=/AgenticBookStore/level-3-behavioral-control>',
  );
  const errors = errorsOf(two, 2);
  fails(errors, /crumb 4 \(Level 3\) must have data-state upcoming \(found current\)/);
  fails(errors, /exactly one element with aria-current="step", found 2/);
});

test('level progress rule: no current crumb fails, with the data-state or with the aria-current marker missing', () => {
  const noMarker = errorsOf(mutate(CURRENT_2, ' aria-current=step', ''), 2);
  fails(noMarker, /exactly one element with aria-current="step", found 0/);
  fails(noMarker, /crumb 3 \(Level 2\) link must have aria-current="step" \(found none\)/);
  fails(errorsOf(mutate(CURRENT_2, 'data-state=current', 'data-state=upcoming'), 2), /crumb 3 \(Level 2\) must have data-state current \(found upcoming\)/);
});

test('level progress rule: a data-state that contradicts the position fails, whatever the value', () => {
  for (const state of ['upcoming', 'current', 'active', '']) {
    const errors = errorsOf(mutate(CURRENT_2, 'data-state=done><a class=link_MHCk href=/AgenticBookStore/level-0', `data-state="${state}"><a class=link_MHCk href=/AgenticBookStore/level-0`), 2);
    fails(errors, /crumb 1 \(Level 0\) must have data-state done/);
  }
  fails(errorsOf(mutate(CURRENT_2, 'class=step_kWIg data-state=done><a class=link_MHCk href=/AgenticBookStore/level-0', 'class=step_kWIg><a class=link_MHCk href=/AgenticBookStore/level-0'), 2), /crumb 1 \(Level 0\) must have data-state done \(found none\)/);
});

test('level progress rule: aria-current on the list item instead of the link fails', () => {
  const onItem = mutate(CURRENT_2, 'data-state=current><a class=link_MHCk aria-current=step', 'data-state=current aria-current=step><a class=link_MHCk');
  fails(errorsOf(onItem, 2), /aria-current belongs on the link, not on the list item/);
  fails(errorsOf(onItem, 2), /the element with aria-current="step" must be the link of the current crumb/);
});

test('level progress rule: aria-current on a done or an upcoming crumb as well fails', () => {
  const onDone = mutate(CURRENT_2, 'class=link_MHCk href=/AgenticBookStore/level-0', 'class=link_MHCk aria-current=step href=/AgenticBookStore/level-0');
  const errors = errorsOf(onDone, 2);
  fails(errors, /exactly one element with aria-current="step", found 2/);
  fails(errors, /crumb 1 \(Level 0\) link must not have aria-current/);
  const onUpcoming = mutate(CURRENT_2, 'class=link_MHCk href=/AgenticBookStore/level-4', 'class=link_MHCk aria-current=step href=/AgenticBookStore/level-4');
  fails(errorsOf(onUpcoming, 2), /crumb 5 \(Level 4\) link must not have aria-current/);
  const page2 = mutate(CURRENT_2, 'class=link_MHCk href=/AgenticBookStore/level-4', 'class=link_MHCk aria-current=page href=/AgenticBookStore/level-4');
  fails(errorsOf(page2, 2), /crumb 5 \(Level 4\) link must not have aria-current/);
});

test('level progress rule: aria-current=step elsewhere on the page counts, and aria-current=page on a menu link does not', () => {
  const extra = page(CURRENT_2).replace('<p>Text.</p>', '<p><a aria-current=step href=/x>x</a></p>');
  const errors = levelProgressErrors({file: 'level-2-give-your-agent-eyes.html', html: extra, base: BASE, level: 2, routes: ROUTES});
  fails(errors, /exactly one element with aria-current="step", found 2/);
  assert.deepEqual(errorsOf(CURRENT_2, 2), [], 'the menu link of the fixture has aria-current=page');
});

test('level progress rule: the level of the page decides which crumb is current (Level 3 page showing current 2 fails, and the other way round)', () => {
  const asThree = errorsOf(CURRENT_2, 3, 'level-3-behavioral-control.html');
  fails(asThree, /crumb 3 \(Level 2\) must have data-state done \(found current\)/);
  fails(asThree, /crumb 4 \(Level 3\) must have data-state current \(found upcoming\)/);
  fails(asThree, /the element with aria-current="step" must be the link of the current crumb/);
  const asOne = errorsOf(CURRENT_2, 1, 'level-1-context-engineering.html');
  fails(asOne, /crumb 2 \(Level 1\) must have data-state current \(found done\)/);
  assert.deepEqual(errorsOf(CURRENT_0, 0, 'level-0-break-the-agent.html'), []);
  assert.deepEqual(errorsOf(CURRENT_4, 4, 'level-4-spec-driven-development.html'), []);
  fails(errorsOf(CURRENT_0, 1, 'level-1-context-engineering.html'), /crumb 1 \(Level 0\) must have data-state done \(found current\)/);
});

test('level progress rule: a level above the last (a Level 5 page) or below the first is not a valid current level', () => {
  for (const level of [5, -1, 2.5]) {
    fails(errorsOf(CURRENT_2, level), /level must be an integer from 0 to 4/);
  }
});

// --- the check mark and the completed text -----------------------------------------------------------

test('level progress rule: a check mark on the current crumb or on an upcoming crumb fails', () => {
  const onCurrent = mutate(CURRENT_2, 'aria-current=step href=/AgenticBookStore/level-2-give-your-agent-eyes>', `aria-current=step href=/AgenticBookStore/level-2-give-your-agent-eyes>${CHECK}`);
  fails(errorsOf(onCurrent, 2), /crumb 3 \(Level 2\) is current and must have no check mark/);
  const onUpcoming = mutate(CURRENT_2, 'href=/AgenticBookStore/level-4-spec-driven-development>', `href=/AgenticBookStore/level-4-spec-driven-development>${CHECK}`);
  fails(errorsOf(onUpcoming, 2), /crumb 5 \(Level 4\) is upcoming and must have no check mark/);
});

test('level progress rule: a check mark character on a crumb that is not done fails even without the data-part marker', () => {
  const bare = mutate(CURRENT_2, '<span>Level 4</span>', '<span>Level 4 &#10003;</span>');
  fails(errorsOf(bare, 2), /crumb 5 \(Level 4\) is upcoming and must have no check mark/);
});

test('level progress rule: a done crumb without its check mark fails', () => {
  fails(errorsOf(mutate(CURRENT_2, CHECK, ''), 2), /crumb 1 \(Level 0\) is done and must have exactly one check mark, found 0/);
  const twice = mutate(CURRENT_2, CHECK, CHECK + CHECK);
  fails(errorsOf(twice, 2), /crumb 1 \(Level 0\) is done and must have exactly one check mark, found 2/);
});

test('level progress rule: a check mark without aria-hidden fails, for every value but true', () => {
  fails(errorsOf(mutate(CURRENT_2, 'aria-hidden=true data-part=check', 'data-part=check'), 2), /check mark must have aria-hidden="true"/);
  fails(errorsOf(mutate(CURRENT_2, 'aria-hidden=true data-part=check', 'aria-hidden=false data-part=check'), 2), /check mark must have aria-hidden="true"/);
});

test('level progress rule: a check mark that is not an element of its own (the data-part marker missing) leaves the done crumb without one', () => {
  fails(errorsOf(mutate(CURRENT_2, CHECK, CHECK.replace(' data-part=check', '')), 2), /crumb 1 \(Level 0\) is done and must have exactly one check mark, found 0/);
});

test('level progress rule: a check mark after the label fails (it comes first)', () => {
  const after = mutate(
    CURRENT_2,
    `${CHECK}<span>Level 0</span>`,
    `<span>Level 0</span>${CHECK}`,
  );
  fails(errorsOf(after, 2), /check mark must come before the label/);
});

test('level progress rule: a done crumb without its "completed" text fails, and a crumb that is not done must not say it', () => {
  fails(errorsOf(mutate(CURRENT_2, DONE_TEXT, ''), 2), /crumb 1 \(Level 0\) is done and its link name must say completed \(found "Level 0"\)/);
  fails(errorsOf(mutate(CURRENT_2, DONE_TEXT, '<span aria-hidden=true> (completed)</span>'), 2), /crumb 1 \(Level 0\) is done and its link name must say completed/);
  const upcoming = mutate(CURRENT_2, '<span>Level 3</span>', `<span>Level 3</span>${DONE_TEXT}`);
  fails(errorsOf(upcoming, 2), /crumb 4 \(Level 3\) link name must be "Level 3" \(found "Level 3 \(completed\)"\)/);
});

test('level progress rule: a done crumb whose name does not start with its level fails (a check mark that is read aloud)', () => {
  const read = mutate(CURRENT_2, CHECK, '<span data-part=check class=check__JZm>&#10003;</span>');
  const errors = errorsOf(read, 2);
  fails(errors, /crumb 1 \(Level 0\) must read Level 0 \(found "\S+ ?Level 0 \(completed\)"\)/);
  fails(errors, /crumb 1 \(Level 0\) check mark must have aria-hidden="true"/);
});

// --- name and list shape pins (spec 14 T2 meta-review: R15, R20, R33, R34) -------------------------------

test('level progress rule: a crumb that reads Level 10 is not the Level 1 crumb (the name must not continue with a digit)', () => {
  const ten = mutate(CURRENT_2, `<span>Level 1</span>${DONE_TEXT}`, `<span>Level 10</span>${DONE_TEXT}`);
  fails(errorsOf(ten, 2), /crumb 2 \(Level 1\) must read Level 1/);
});

test('level progress rule: a second ol in the nav fails', () => {
  const second = mutate(CURRENT_2, NAV_CLOSE, '</ol><ol class=steps_tDzf></ol></nav>');
  fails(errorsOf(second, 2), /must hold one ol \(found 2\)/);
});

test('level progress rule: an ol wrapped in a div is not the ol of the nav', () => {
  const wrapped = mutate(mutate(CURRENT_2, NAV_OPEN, NAV_OPEN.replace('<ol', '<div><ol')), NAV_CLOSE, '</ol></div></nav>');
  fails(errorsOf(wrapped, 2), /must hold one ol \(found 0\)/);
});

test('level progress rule: a done crumb whose text says uncompleted does not say completed', () => {
  const un = mutate(CURRENT_2, '(completed)', '(uncompleted)');
  fails(errorsOf(un, 2), /crumb 1 \(Level 0\) is done and its link name must say completed/);
});

// --- pages that are not level pages ----------------------------------------------------------------------

test('level progress rule: a page that is not a level page must have no level progress nav', () => {
  const home = levelProgressErrors({file: 'index.html', html: page(CURRENT_2, {file: 'index.html'}), base: BASE, level: null, routes: ROUTES});
  fails(home, /index\.html: a page that is not a level page must have no level progress nav/);
  for (const label of ['level progress', 'LEVEL PROGRESS', ' Level progress ']) {
    const odd = levelProgressErrors({file: 'index.html', html: page(mutate(CURRENT_2, 'aria-label="Level progress"', `aria-label="${label}"`)), base: BASE, level: null, routes: ROUTES});
    fails(odd, /must have no level progress nav/);
  }
  assert.deepEqual(levelProgressErrors({file: 'index.html', html: page(''), base: BASE, level: null, routes: ROUTES}), []);
  assert.deepEqual(levelProgressErrors({file: 'index.html', html: page('<nav aria-label="Pagination"></nav>'), base: BASE, level: null, routes: ROUTES}), []);
});

// --- scanBuilt with the level option ------------------------------------------------------------------------

const LEVEL_FILES = ROUTES.map((route) => `${route}.html`);
const goodBuild = () => ({
  'index.html': page(''),
  'setup-check.html': page(''),
  'reference/cheat-sheet.html': page(''),
  ...Object.fromEntries(LEVEL_FILES.map((file, level) => [file, page(NAVS[level] ?? levelNav(level), {file})])),
});

// The slices of levels 1 and 3 (the three hand-written ones above cover 0, 2 and 4), by the same rules.
function levelNav(current) {
  const crumbs = ROUTES.map((route, level) => {
    const state = level < current ? 'done' : level === current ? 'current' : 'upcoming';
    const check = state === 'done' ? CHECK : '';
    const done = state === 'done' ? DONE_TEXT : '';
    const marker = state === 'current' ? ' aria-current=step' : '';
    return `<li class=step_kWIg data-state=${state}><a class=link_MHCk${marker} href=/AgenticBookStore/${route}>${check}<span>Level ${level}</span>${done}</a>${SHAPE}`;
  });
  return NAV_OPEN + crumbs.join('') + NAV_CLOSE;
}

function withBuild(files, fn) {
  const root = realpathSync(mkdtempSync(join(tmpdir(), 'guide-level-progress-test-')));
  try {
    for (const [rel, content] of Object.entries(files)) {
      const abs = join(root, rel);
      mkdirSync(dirname(abs), {recursive: true});
      writeFileSync(abs, content);
    }
    return fn(root);
  } finally {
    rmSync(root, {recursive: true, force: true});
  }
}

const scan = (files, options = {levels: {base: BASE}}) => withBuild(Object.fromEntries(Object.entries(files).map(([rel, html]) => [`build/${rel}`, html])), (root) => scanBuilt(join(root, 'build'), options));

test('scanBuilt: the five level pages and the other pages of a good build pass, and the counts are returned', () => {
  const result = scan(goodBuild());
  assert.deepEqual(result.errors, []);
  assert.equal(result.levelPages, 5);
  assert.equal(result.otherPages, 3);
});

test('scanBuilt: without the levels option no level rule runs (the existing fixture pages stay valid)', () => {
  const files = goodBuild();
  files['level-2-give-your-agent-eyes.html'] = page('');
  const result = scan(files, {});
  assert.deepEqual(result.errors, []);
  assert.equal(result.levelPages, undefined);
});

test('scanBuilt: the current crumb of each level page follows the page (the file name gives the level)', () => {
  // a Level 3 file that shows Level 2 as current, and a Level 1 file that shows Level 0
  const files = goodBuild();
  files['level-3-behavioral-control.html'] = page(CURRENT_2, {file: 'level-3-behavioral-control.html'});
  files['level-1-context-engineering.html'] = page(CURRENT_0, {file: 'level-1-context-engineering.html'});
  const errors = scan(files).errors;
  assert.ok(errors.some((error) => /^level progress: level-3-behavioral-control\.html: crumb 4 \(Level 3\) must have data-state current/.test(error)), errors.join('\n'));
  assert.ok(errors.some((error) => /^level progress: level-1-context-engineering\.html: crumb 2 \(Level 1\) must have data-state current/.test(error)), errors.join('\n'));
  assert.equal(errors.filter((error) => /level-(0|2|4)-/.test(error)).length, 0, errors.join('\n'));
});

test('scanBuilt: a level page missing from the build fails once, naming the level', () => {
  for (const level of [0, 1, 2, 3, 4]) {
    const files = goodBuild();
    delete files[LEVEL_FILES[level]];
    const errors = scan(files).errors.filter((error) => /^level progress:/.test(error));
    assert.ok(errors.some((error) => error.includes(`no page for Level ${level} (level-${level}-*.html) in the build`)), errors.join('\n'));
  }
  const files = goodBuild();
  delete files[LEVEL_FILES[3]];
  assert.equal(scan(files).errors.filter((error) => /no page for Level/.test(error)).length, 1);
});

test('scanBuilt: a sixth level page, a second page of one level and a Level 5 file fail', () => {
  const extra = goodBuild();
  extra['level-5-more.html'] = page(levelNav(4), {file: 'level-5-more.html'});
  assert.ok(scan(extra).errors.some((error) => /level-5-more\.html: levels run from 0 to 4/.test(error)));
  const twice = goodBuild();
  twice['level-2-other.html'] = page(CURRENT_2, {file: 'level-2-other.html'});
  assert.ok(scan(twice).errors.some((error) => /two pages for Level 2 \(level-2-give-your-agent-eyes\.html and level-2-other\.html\)/.test(error)));
});

test('scanBuilt: a level progress nav on a page that is not a level page fails, in a subfolder too', () => {
  for (const rel of ['index.html', 'setup-check.html', 'reference/cheat-sheet.html', 'wrap-up.html']) {
    const files = goodBuild();
    files[rel] = page(CURRENT_2, {file: rel});
    const errors = scan(files).errors;
    assert.ok(errors.some((error) => error.startsWith(`level progress: ${rel}: a page that is not a level page`)), `${rel}: ${errors.join('\n')}`);
  }
});

test('scanBuilt: a file in a subfolder is never a level page, even when its name looks like one', () => {
  const files = goodBuild();
  files['reference/level-1-context-engineering.html'] = page('');
  assert.deepEqual(scan(files).errors, []);
  assert.equal(scan(files).levelPages, 5);
});

test('scanBuilt: the crumb links must go to the level pages found in the build', () => {
  const files = goodBuild();
  files['level-0-break-the-agent.html'] = page(CURRENT_0.replace('level-1-context-engineering', 'level-1-elsewhere'), {file: 'level-0-break-the-agent.html'});
  assert.ok(scan(files).errors.some((error) => /level-0-break-the-agent\.html: crumb 2 \(Level 1\) link must go to \/AgenticBookStore\/level-1-context-engineering/.test(error)));
});

// --- the entry point: the default build dir switches the rule on (G-7) ---------------------------------------
// A copy of the scripts folder sits beside a fixture build and a fixture config, so check-built.mjs
// resolves its default build dir (../build) to the fixture and never to guide/build.

const withNavbar = (html) => html.replace(/<nav aria-label=Main[^]*?<\/nav>/, NAVBAR_FULL);
// The navbar of a real build (see navbar.test.mjs), so the navbar rule that runs beside this one passes.
const NAVBAR_FULL =
  '<nav aria-label=Main class="theme-layout-navbar navbar navbar--fixed-top"><div class=navbar__inner><div class="theme-layout-navbar-left navbar__items">' +
  '<a href=https://openvalue.eu target=_blank rel="noopener noreferrer" class=guide-navbar-logo aria-label="OpenValue (opens in a new tab)">' +
  '<img src=/AgenticBookStore/img/openvalue-logo-dark.png alt="" class="themedComponent_mlkZ themedComponent--light_NVdE" height=32 />' +
  '<img src=/AgenticBookStore/img/openvalue-logo-light.png alt="" class="themedComponent_mlkZ themedComponent--dark_xIcU" height=32 /></a>' +
  '<a class=navbar__brand href=/AgenticBookStore/><b class="navbar__title text--truncate">Crafting your Agentic Harness</b></a></div>' +
  '<div class="theme-layout-navbar-right navbar__items navbar__items--right">' +
  '<a href=https://github.com/MaikKingma/AgenticBookStore target=_blank rel="noopener noreferrer" class="navbar__item navbar__link">GitHub</a>' +
  '<a href=https://www.linkedin.com/in/maik-kingma/ target=_blank rel="noopener noreferrer" class="navbar__item navbar__link">Follow on LinkedIn</a></div></div></nav>';

const fullBuild = () => Object.fromEntries(Object.entries(goodBuild()).map(([rel, html]) => [rel, withNavbar(html)]));

function withCopiedScripts(build, fn) {
  const files = {'docusaurus.config.ts': "export default {\n  baseUrl: '/AgenticBookStore/',\n};\n"};
  for (const [rel, content] of Object.entries(build)) files[`build/${rel}`] = content;
  return withBuild(files, (root) => {
    cpSync(SCRIPTS, join(root, 'scripts'), {recursive: true, filter: (source) => !source.endsWith('.test.mjs')});
    return fn(root, join(root, 'scripts', 'check-built.mjs'));
  });
}

const run = (script, args = []) => spawnSync(process.execPath, [script, ...args], {encoding: 'utf8'});

test('check-built with no argument runs the level progress rule and prints its ok line with the counts', () => {
  withCopiedScripts(fullBuild(), (root, script) => {
    const result = run(script);
    assert.equal(result.status, 0, result.stdout + result.stderr);
    assert.match(result.stdout, /^ok {3}level progress: 5 level pages, each with five crumbs and one current, none on the 3 other pages$/m);
    assert.match(result.stdout, /^ok {3}navbar: 8 pages, /m);
  });
});

test('check-built with no argument fails on a default build whose level page shows the wrong current crumb', () => {
  const build = fullBuild();
  build['level-3-behavioral-control.html'] = withNavbar(page(CURRENT_2, {file: 'level-3-behavioral-control.html'}));
  withCopiedScripts(build, (root, script) => {
    const result = run(script);
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.match(result.stderr, /level progress: level-3-behavioral-control\.html: crumb 4 \(Level 3\) must have data-state current/);
    assert.doesNotMatch(result.stdout, /ok {3}level progress/);
  });
});

test('check-built with no argument fails on a default build with a level page missing', () => {
  const build = fullBuild();
  delete build['level-1-context-engineering.html'];
  withCopiedScripts(build, (root, script) => {
    const result = run(script);
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.match(result.stderr, /level progress: no page for Level 1 \(level-1-\*\.html\) in the build/);
  });
});

test('check-built with no argument fails on a default build with a level progress nav on the home page', () => {
  const build = fullBuild();
  build['index.html'] = withNavbar(page(CURRENT_2, {file: 'index.html'}));
  withCopiedScripts(build, (root, script) => {
    const result = run(script);
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.match(result.stderr, /level progress: index\.html: a page that is not a level page/);
  });
});

test('check-built with no argument and no level pages at all fails (the rule cannot pass on a build without them)', () => {
  withCopiedScripts({'index.html': withNavbar(page('')), 'setup-check.html': withNavbar(page(''))}, (root, script) => {
    const result = run(script);
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.equal((result.stderr.match(/level progress: no page for Level/g) ?? []).length, 5, result.stderr);
  });
});

test('check-built with a build dir argument does not run the level progress rule (fixture pages stay valid)', () => {
  withCopiedScripts(fullBuild(), (root, script) => {
    withBuild({'other/index.html': page(CURRENT_2, {file: 'index.html'}), 'other/level-1-x.html': page('')}, (other) => {
      const result = run(script, [join(other, 'other')]);
      assert.equal(result.status, 0, result.stdout + result.stderr);
      assert.doesNotMatch(result.stdout, /level progress/);
    });
  });
});

test('the real component puts the two chevron layers before the link and holds no check character', () => {
  const source = readFileSync(join(SCRIPTS, '..', 'src', 'components', 'LevelProgress.tsx'), 'utf8');
  assert.ok(source.indexOf('data-part="edge"') > 0 && source.indexOf('data-part="edge"') < source.indexOf('<Link to='));
  assert.ok(source.indexOf('data-part="fill"') > source.indexOf('data-part="edge"') && source.indexOf('data-part="fill"') < source.indexOf('<Link to='));
  assert.ok(!/u2713|10003/i.test(source.replace(/\/\/.*$/gm, '')), 'the check character is drawn by the stylesheet, not written in the component');
});

test('the real components and the rule agree on the names the built page shows (the five routes and the markers)', () => {
  const source = readFileSync(join(SCRIPTS, '..', 'src', 'components', 'LevelProgress.tsx'), 'utf8');
  for (const route of ROUTES) assert.ok(source.includes(`'/${route}'`), route);
  for (const marker of ['aria-label="Level progress"', 'data-state={state}', "aria-current={state === 'current' ? 'step' : undefined}", 'data-part="check"', 'aria-hidden="true"', '(completed)']) {
    assert.ok(source.includes(marker), marker);
  }
});

// --- the stylesheets: what the contrast checker and the look depend on (source-level guards) ---------------
// The rendered proof (computed colours, wrap at 375 px, focus ring, axe) is the browser rig's; these pins make
// the parts of it that live in the source visible to `npm run check`.

const CSS_DIR = join(SCRIPTS, '..', 'src');
const SITE_CSS = readFileSync(join(CSS_DIR, 'css', 'custom.css'), 'utf8');
const MODULE_CSS = readFileSync(join(CSS_DIR, 'components', 'LevelProgress.module.css'), 'utf8');

// Declarations of --guide-level-* outside the two blocks the contrast checker reads (:root and
// html[data-theme='dark'], top level). A variable declared anywhere else would be ignored by the checker
// while the page rendered it (spec 14 G-9).
function levelVariablesElsewhere(css) {
  let text = withoutBlockComments(css);
  for (let i = 0, selectorStart = 0; i < text.length; i++) {
    if (text[i] === '}') {
      selectorStart = i + 1;
    } else if (text[i] === '{') {
      const selector = text.slice(selectorStart, i).trim();
      const end = matchingBrace(text, i);
      if (selector === ':root' || selector === "html[data-theme='dark']") {
        text = text.slice(0, i + 1) + ' '.repeat(end - i - 1) + text.slice(end);
      }
      i = end;
      selectorStart = end + 1;
    }
  }
  return [...text.matchAll(/(?:^|[\s;{])(--guide-level-[\w-]+)\s*:/g)].map((match) => match[1]);
}

test('level progress stylesheet: every --guide-level-* variable is declared in :root or html[data-theme=dark] only', () => {
  assert.deepEqual(levelVariablesElsewhere(SITE_CSS), []);
  const inRoot = [...withoutBlockComments(SITE_CSS).matchAll(/(--guide-level-[\w-]+)\s*:/g)].length;
  assert.ok(inRoot >= 12, `only ${inRoot} declarations found, the pin reads the wrong file`);
});

test('level progress stylesheet: a variable declared in a media query, on a class or on .navbar is found', () => {
  assert.deepEqual(levelVariablesElsewhere(':root { --guide-level-a: #fff; }\n@media (min-width: 997px) { :root { --guide-level-b: #000; } }'), ['--guide-level-b']);
  assert.deepEqual(levelVariablesElsewhere('.navbar { --guide-level-c: #fff; }\nhtml[data-theme="dark"] { --guide-level-d: #fff; }'), ['--guide-level-c', '--guide-level-d']);
  assert.deepEqual(levelVariablesElsewhere(".x { color: var(--guide-level-e); }\nhtml[data-theme='dark'] { --guide-level-f: #fff; }"), []);
});

function resolved(mode, name) {
  const {light, dark} = cssVariables(SITE_CSS);
  const result = resolveColor(name, mode === 'light' ? light : new Map([...light, ...dark]));
  assert.equal(result.error, undefined, result.error);
  return result.hex.toUpperCase();
}

test('level progress stylesheet: the current fill is the AA blue #3371B8 with a white label, never the exact deck blue #3C7EC9', () => {
  for (const mode of ['light', 'dark']) {
    assert.equal(resolved(mode, '--guide-level-current-bg'), '#3371B8', mode);
    assert.equal(resolved(mode, '--guide-level-current-fg'), '#FFFFFF', mode);
    for (const name of ['done', 'current', 'inactive']) {
      for (const part of ['bg', 'fg', 'border']) {
        assert.notEqual(resolved(mode, `--guide-level-${name}-${part}`), '#3C7EC9', `${mode} ${name} ${part}`);
      }
    }
  }
});

test('level progress stylesheet: upcoming crumbs are muted, their fill and label differ from the current and the done ones, in both modes', () => {
  for (const mode of ['light', 'dark']) {
    for (const part of ['bg', 'fg']) {
      const inactive = resolved(mode, `--guide-level-inactive-${part}`);
      assert.notEqual(inactive, resolved(mode, `--guide-level-current-${part}`), `${mode} ${part} inactive vs current`);
      assert.notEqual(inactive, resolved(mode, `--guide-level-done-${part}`), `${mode} ${part} inactive vs done`);
    }
    assert.notEqual(resolved(mode, '--guide-level-done-bg'), resolved(mode, '--guide-level-current-bg'), `${mode} done vs current fill`);
  }
});

// The rules of the module stylesheet as [selector, body]; it has no nested blocks (checked below).
const rulesOf = (css) => [...withoutBlockComments(css).matchAll(/([^{}]+)\{([^{}]*)\}/g)].map((match) => [match[1].replace(/\s+/g, ' ').trim(), match[2]]);
const bodyOf = (rules, selector) => rules.filter(([name]) => name === selector).map(([, body]) => body).join('\n');

// The look the source must keep: a wrapping flex row, the chevron on the two layer spans only (a clip-path on
// the link or the list item would cut the focus ring), a focus ring of its own colour, the state by data-state.
function moduleErrors(css) {
  const errors = [];
  const rules = rulesOf(css);
  if (/@media|@supports/.test(css)) errors.push('an at-rule (@media, @supports) is in the module; the pins read top-level rules only');
  const steps = bodyOf(rules, '.steps');
  if (!/display:\s*flex/.test(steps) || !/flex-wrap:\s*wrap/.test(steps)) errors.push('.steps must be a flex row that wraps');
  for (const [selector, body] of rules) {
    if (/clip-path/.test(body) && !['.edge, .fill', '.status'].includes(selector)) {
      errors.push(`clip-path in ${selector}: only the two chevron layers and the screen reader text may be clipped`);
    }
    if (/::(?:before|after)/.test(selector) && /background/.test(body)) {
      errors.push(`a background on the pseudo element in ${selector}: axe cannot read the colour under a label from a pseudo element`);
    }
    if (/\boverflow\s*:\s*(hidden|clip)/.test(body) && selector !== '.status') errors.push(`overflow in ${selector} would cut the focus ring`);
    if (/\b(?:transform|zoom)\s*:/.test(body)) errors.push(`transform or zoom in ${selector}`);
    // axe reports a label as incomplete (bgOverlap) when the layers behind it sit in a negative z-index or an
    // isolated stacking context; the layers come before the positioned link in the document instead.
    if (/z-index\s*:\s*-/.test(body)) errors.push(`a negative z-index in ${selector} makes axe report the labels as overlapped`);
    if (/isolation\s*:\s*isolate/.test(body)) errors.push(`isolation in ${selector} makes axe report the labels as overlapped`);
  }
  const layers = bodyOf(rules, '.edge, .fill');
  if (!/clip-path:\s*var\(--crumb-clip\)/.test(layers)) errors.push('the two chevron layers must carry the chevron clip-path');
  if (!/background:\s*var\(--crumb-border\)/.test(bodyOf(rules, '.edge')) || !/background:\s*var\(--crumb-bg\)/.test(bodyOf(rules, '.fill'))) {
    errors.push('the chevron layers must paint the outline (.edge) and the fill (.fill) as real backgrounds');
  }
  const focus = bodyOf(rules, '.steps > .step > .link:focus-visible');
  const outline = /outline:\s*(\d+(?:\.\d+)?)px\s+solid\s+var\(--guide-level-focus\)/.exec(focus);
  if (!outline || Number(outline[1]) < 2) errors.push('the focus ring must be an outline of at least 2px in var(--guide-level-focus)');
  if (!/outline-offset:\s*[1-9]/.test(focus)) errors.push('the focus ring needs an outline-offset of at least 1px (outside the crumb)');
  for (const state of ['done', 'current']) {
    if (bodyOf(rules, `.steps > .step[data-state='${state}']`) === '') errors.push(`no rule for data-state ${state}`);
  }
  const link = bodyOf(rules, '.steps > .step > .link');
  if (!/position:\s*relative/.test(link)) errors.push('the link must be positioned so it paints after the chevron layers');
  if (!/content:\s*'\\2713'/.test(bodyOf(rules, '.check::before'))) errors.push("the check mark must draw the check character as the escape '\\2713' in the content of .check::before");
  if (!/color:\s*var\(--crumb-fg\)/.test(link)) errors.push('the link label colour must come from --crumb-fg');
  if (!/--crumb-bg:\s*var\(--guide-level-inactive-bg\)/.test(bodyOf(rules, '.steps > .step'))) errors.push('the upcoming crumb must use the inactive variables by default');
  // A hovered label keeps its own colour: the pair that proves it is the one of the state, not a link hover colour.
  for (const [selector, body] of rules.filter(([name]) => name.includes(':hover'))) {
    for (const declaration of body.matchAll(/(?:^|[;\s])color\s*:\s*([^;]+)/g)) {
      if (declaration[1].trim() !== 'var(--crumb-fg)') errors.push(`${selector} declares color ${declaration[1].trim()}: a hovered crumb label must keep var(--crumb-fg)`);
    }
  }
  // The outline is the inset of the fill layer inside the outline layer.
  if (!/inset:\s*var\(--outline\)/.test(bodyOf(rules, '.fill'))) errors.push('.fill must be inset by var(--outline), so that the .edge layer shows as the outline');
  const width = /--outline:\s*(\d+(?:\.\d+)?)px/.exec(bodyOf(rules, '.steps'));
  if (!width || Number(width[1]) < 1) errors.push('.steps must declare --outline of at least 1px');
  return errors;
}

test('level progress stylesheet: the module keeps the wrapping row, the chevron on the two layer spans and the focus ring', () => {
  assert.deepEqual(moduleErrors(MODULE_CSS), []);
});

for (const [what, from, to, message] of [
  ['no wrap', 'flex-wrap: wrap;', 'flex-wrap: nowrap;', /must be a flex row that wraps/],
  ['a clip-path on the link', '  white-space: nowrap;\n}', '  white-space: nowrap;\n  clip-path: var(--crumb-clip);\n}', /clip-path in \.steps > \.step > \.link/],
  ['a clip-path on the list item', '  position: relative;\n  flex: 1 1 6rem;', '  position: relative;\n  clip-path: var(--crumb-clip);\n  flex: 1 1 6rem;', /clip-path in \.steps > \.step:/],
  ['a negative z-index on the layers', '.fill {\n  inset', '.fill {\n  z-index: -1;\n  inset', /negative z-index in \.fill/],
  ['isolation on the list item', '  position: relative;\n  flex: 1 1 6rem;', '  position: relative;\n  isolation: isolate;\n  flex: 1 1 6rem;', /isolation in \.steps > \.step/],
  ['an unpositioned link', '.steps > .step > .link {\n  position: relative;\n', '.steps > .step > .link {\n', /link must be positioned/],
  ['a check mark with no drawn character', "content: '\\2713';", "content: '';", /check mark must draw the check character/],
  ['overflow hidden on the row', '  list-style: none;\n}', '  list-style: none;\n  overflow: hidden;\n}', /overflow in \.steps/],
  ['a focus ring of 1px', 'outline: 3px solid var(--guide-level-focus);', 'outline: 1px solid var(--guide-level-focus);', /at least 2px/],
  ['a focus ring in another colour', 'outline: 3px solid var(--guide-level-focus);', 'outline: 3px solid var(--guide-level-current-bg);', /at least 2px in var\(--guide-level-focus\)/],
  ['no focus ring offset', 'outline-offset: 2px;', 'outline-offset: 0;', /outline-offset/],
  ['no focus rule', '.link:focus-visible {', '.link:focus {', /focus ring must be an outline/],
  ['a media query', '.check {', '@media (max-width: 996px) {\n  .check {\n    color: red;\n  }\n}\n\n.check {', /an at-rule/],
  ['a background on a pseudo element', '.steps > .step > .link:hover {', '.step::after {\n  background: var(--crumb-bg);\n}\n\n.steps > .step > .link:hover {', /a background on the pseudo element in \.step::after/],
  ['a fill layer without a real background', 'background: var(--crumb-bg);', 'background: transparent;', /as real backgrounds/],
  ['no done rule', "[data-state='done']", "[data-state='finished']", /no rule for data-state done/],
  ['a link colour of its own', 'color: var(--crumb-fg);\n  font-weight', 'color: var(--ifm-link-color);\n  font-weight', /label colour must come from --crumb-fg/],
  ['a hover colour of its own', '.link:hover {\n  color: var(--crumb-fg);', '.link:hover {\n  color: var(--ifm-link-hover-color);', /a hovered crumb label must keep var\(--crumb-fg\)/],
  ['a fill layer with no inset', 'inset: var(--outline);', 'inset: 0;', /\.fill must be inset by var\(--outline\)/],
  ['an outline width of 0', '--outline: 2px;', '--outline: 0px;', /\.steps must declare --outline of at least 1px/],
]) {
  test(`level progress stylesheet: ${what} fails the module pins`, () => {
    assert.ok(MODULE_CSS.includes(from), `the module has no ${from}`);
    const errors = moduleErrors(MODULE_CSS.replace(from, to));
    assert.ok(errors.some((error) => message.test(error)), errors.join('\n') || '(no error)');
  });
}
