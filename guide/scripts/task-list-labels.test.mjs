import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import rehypeTaskListLabels from '../src/plugins/rehype-task-list-labels.mjs';

// Fixture trees in hast, the shape remark-rehype gives a GFM task list: a ul with
// class contains-task-list, li items with class task-list-item, and a bare disabled
// checkbox as the first child of the item (inside the first p when the list is loose).

const text = (value) => ({type: 'text', value});
const el = (tagName, properties = {}, children = []) => ({type: 'element', tagName, properties, children});
const checkbox = (checked = false) => el('input', {type: 'checkbox', checked, disabled: true});
const item = (...children) => el('li', {className: ['task-list-item']}, children);
const list = (...items) => el('ul', {className: ['contains-task-list']}, items);
const root = (...children) => ({type: 'root', children});

function run(tree) {
  const result = rehypeTaskListLabels()(tree);
  assert.equal(result, undefined, 'the transformer edits the tree in place');
  return tree;
}

function labels(tree) {
  const found = [];
  const visit = (node) => {
    if (node.type === 'element' && node.tagName === 'input') found.push(node.properties.ariaLabel);
    for (const child of node.children ?? []) visit(child);
  };
  visit(tree);
  return found;
}

test('task list labels: a plain-text item names its checkbox with the item text', () => {
  const tree = root(list(item(checkbox(), text(' Spec artefacts committed next to the code.'))));
  assert.deepEqual(labels(run(tree)), ['Spec artefacts committed next to the code.']);
});

test('task list labels: inline code counts as plain text, backticks and all spacing as written', () => {
  const tree = root(
    list(
      item(checkbox(), text(' '), el('code', {}, [text('/mcp')]), text(' shows the distributor connected.')),
      item(checkbox(), text(' '), el('code', {}, [text('CLAUDE.md')]), text(' has at most 60 lines.')),
    ),
  );
  assert.deepEqual(labels(run(tree)), ['/mcp shows the distributor connected.', 'CLAUDE.md has at most 60 lines.']);
});

test('task list labels: emphasis and strong keep their words, markers not included', () => {
  const tree = root(
    list(
      item(
        checkbox(),
        text(' Commit made through your '),
        el('strong', {}, [text('/ship'), text(' command')]),
        text(' and '),
        el('em', {}, [text('passed')]),
        text(' the hook.'),
      ),
    ),
  );
  assert.deepEqual(labels(run(tree)), ['Commit made through your /ship command and passed the hook.']);
});

test('task list labels: a link inside the item contributes its text, not its address', () => {
  const tree = root(list(item(checkbox(), text(' Read '), el('a', {href: 'https://example.invalid/x'}, [text('the guide')]), text('.'))));
  assert.deepEqual(labels(run(tree)), ['Read the guide.']);
});

test('task list labels: a loose item (checkbox inside a paragraph) is labelled too', () => {
  // remark-rehype puts a newline text node between block children.
  const tree = root(list(item(text('\n'), el('p', {}, [checkbox(), text(' First, spaced out item.')]), text('\n'), el('p', {}, [text('More detail.')]), text('\n'))));
  assert.deepEqual(labels(run(tree)), ['First, spaced out item. More detail.']);
});

test('task list labels: a checked checkbox is labelled as well', () => {
  const tree = root(list(item(checkbox(true), text(' Already done.'))));
  assert.deepEqual(labels(run(tree)), ['Already done.']);
});

test('task list labels: whitespace runs and line breaks collapse to single spaces and the ends are trimmed', () => {
  const tree = root(list(item(checkbox(), text('\n  Wrapped   across\nlines.  '))));
  assert.deepEqual(labels(run(tree)), ['Wrapped across lines.']);
});

test('task list labels: a nested list is not part of the parent label and its items name themselves', () => {
  const tree = root(
    list(
      item(checkbox(), text(' Parent item'), list(item(checkbox(), text(' Child one')), item(checkbox(), text(' Child two')))),
      item(checkbox(), text(' Sibling')),
    ),
  );
  assert.deepEqual(labels(run(tree)), ['Parent item', 'Child one', 'Child two', 'Sibling']);
});

// T5 (plan.md A6, FF-22): an ordered nested list is a nested list as well
test('task list labels: a nested ordered list is not part of the parent label and its item names itself', () => {
  const tree = root(list(item(checkbox(), text(' Parent'), el('ol', {}, [item(checkbox(), text(' Child'))]))));
  assert.deepEqual(labels(run(tree)), ['Parent', 'Child']);
});

test('task list labels: every item of every list in the tree is labelled, in document order', () => {
  const tree = root(
    el('h2', {}, [text('Cleared when')]),
    list(item(checkbox(), text(' One'))),
    el('p', {}, [text('between')]),
    list(item(checkbox(), text(' Two')), item(checkbox(), text(' Three'))),
  );
  assert.deepEqual(labels(run(tree)), ['One', 'Two', 'Three']);
});

test('task list labels: an aria-label the author already set is kept', () => {
  const box = el('input', {type: 'checkbox', disabled: true, ariaLabel: 'Written by hand'});
  const tree = root(list(item(box, text(' Item text'))));
  assert.deepEqual(labels(run(tree)), ['Written by hand']);
});

test('task list labels: an item with no text gets no label, so check:built can flag it', () => {
  const tree = root(list(item(checkbox()), item(checkbox(), text('   '))));
  assert.deepEqual(labels(run(tree)), [undefined, undefined]);
});

test('task list labels: a checkbox outside a task-list item and other inputs are left alone', () => {
  const stray = checkbox();
  const radio = el('input', {type: 'radio'});
  const tree = root(el('p', {}, [stray, text(' not a task item')]), el('li', {}, [checkbox(), text(' plain li')]), item(radio, text(' radio in a task item')));
  run(tree);
  assert.equal(stray.properties.ariaLabel, undefined);
  assert.equal(radio.properties.ariaLabel, undefined);
  assert.deepEqual(labels(tree), [undefined, undefined, undefined]);
});

test('task list labels: the checkbox keeps its other properties and the text nodes are not edited', () => {
  const box = checkbox(true);
  const words = text(' Keep me.');
  run(root(list(item(box, words))));
  assert.deepEqual(box.properties, {type: 'checkbox', checked: true, disabled: true, ariaLabel: 'Keep me.'});
  assert.equal(words.value, ' Keep me.');
});

test('task list labels: running the plugin twice gives the same label', () => {
  const tree = root(list(item(checkbox(), text(' Same twice.'))));
  run(tree);
  assert.deepEqual(labels(run(tree)), ['Same twice.']);
});

// The config is TypeScript and Node prints a module-type warning when it loads one, so
// the wiring is pinned on the source text. The built pages are the real proof: check:built
// fails on an unnamed checkbox when the registration is missing or broken.
test('task list labels: the site config registers the plugin in the docs options, and the offline config spreads the main config', () => {
  const main = readFileSync(new URL('../docusaurus.config.ts', import.meta.url), 'utf8');
  assert.match(main, /^import rehypeTaskListLabels from '\.\/src\/plugins\/rehype-task-list-labels\.mjs';$/m);
  const docs = main.match(/docs: \{([^}]*)\}/);
  assert.ok(docs, 'the docs options block is found');
  assert.match(docs[1], /^\s*rehypePlugins: \[rehypeTaskListLabels\],$/m);
  const offline = readFileSync(new URL('../docusaurus.offline.config.ts', import.meta.url), 'utf8');
  assert.match(offline, /^\s*\.\.\.config,$/m);
  assert.doesNotMatch(offline, /presets|rehypePlugins/);
});
