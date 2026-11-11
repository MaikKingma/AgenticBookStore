import {test} from 'node:test';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {cpSync, mkdirSync, mkdtempSync, readFileSync, realpathSync, rmSync, writeFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {dirname, join} from 'node:path';
import {fileURLToPath} from 'node:url';
import rehypeTableRegions from '../src/plugins/rehype-table-regions.mjs';
import {tableRegions} from './rules/table-regions-built.mjs';
import {scanBuilt} from './rules/built.mjs';

// Spec 15, T5. Part 1: the rehype plugin on hast fixtures. Fixture trees have the shape
// remark-rehype and MDX give a page: headings and tables as elements, an MDX expression such as
// {/* #id */} as an mdxTextExpression node, a JSX element (TabItem, details, kbd) as an
// mdxJsxFlowElement or mdxJsxTextElement node with children.

const text = (value) => ({type: 'text', value});
const el = (tagName, properties = {}, children = []) => ({type: 'element', tagName, properties, children});
const flow = (name, children = []) => ({type: 'mdxJsxFlowElement', name, attributes: [], children});
const inline = (name, children = []) => ({type: 'mdxJsxTextElement', name, attributes: [], children});
const expression = (value) => ({type: 'mdxTextExpression', value});
const heading = (level, ...children) => el(`h${level}`, {}, children);
const table = (cell = 'cell') => el('table', {}, [el('tbody', {}, [el('tr', {}, [el('td', {}, [text(cell)])])])]);
const root = (...children) => ({type: 'root', children});

function run(tree) {
  const result = rehypeTableRegions()(tree);
  assert.equal(result, undefined, 'the transformer edits the tree in place');
  return tree;
}

// Every wrapper of the tree in document order, wherever it sits.
function regions(tree) {
  const found = [];
  const visit = (node) => {
    if (node.type === 'element' && node.tagName === 'div' && node.properties?.role === 'region') found.push(node);
    for (const child of node.children ?? []) visit(child);
  };
  visit(tree);
  return found;
}
const names = (tree) => regions(tree).map((region) => region.properties.ariaLabel);

// W1
test('table regions: two tables under one heading get distinct labels that both start with the heading text', () => {
  const tree = run(root(heading(2, text('Packages')), table(), el('p', {}, [text('between')]), table()));
  const [first, second] = names(tree);
  assert.notEqual(first, second);
  assert.match(first, /^Packages\b/);
  assert.match(second, /^Packages\b/);
  // the tables of one heading are numbered, not told apart by the repeat count
  assert.deepEqual([first, second], ['Packages (table 1 of 2)', 'Packages (table 2 of 2)']);
});

test('table regions: a heading with one table names it with the plain heading text', () => {
  assert.deepEqual(names(run(root(heading(2, text('Roles')), table()))), ['Roles']);
});

// W2
test('table regions: a table before any heading gets a non-empty label', () => {
  const tree = run(root(table(), heading(2, text('Later')), table()));
  const [first, second] = names(tree);
  assert.equal(typeof first, 'string');
  assert.notEqual(first.trim(), '');
  assert.equal(second, 'Later');
});

test('table regions: two tables before any heading do not share a label', () => {
  const [first, second] = names(run(root(table(), table())));
  assert.notEqual(first, second);
  assert.notEqual(first.trim(), '');
  assert.notEqual(second.trim(), '');
});

// W3
test('table regions: an MDX id comment in the heading is not part of the label', () => {
  const tree = run(root(heading(2, text('Tracks '), expression('/* #tracks */')), table()));
  assert.deepEqual(names(tree), ['Tracks']);
});

// W4
test('table regions: inline code, emphasis, strong and a kbd element keep their words in the label', () => {
  const tree = run(
    root(
      heading(3, text('Press '), inline('kbd', [text('Tab')]), text(' in '), el('code', {}, [text('/mcp')]), text(' for '), el('em', {}, [text('each')]), text(' '), el('strong', {}, [text('server')])),
      table(),
    ),
  );
  assert.deepEqual(names(tree), ['Press Tab in /mcp for each server']);
});

test('table regions: a link in the heading contributes its text, whitespace runs collapse and the ends are trimmed', () => {
  const tree = run(root(heading(2, text('\n  Read   '), el('a', {href: '/x'}, [text('the  guide')]), text('  \n')), table()));
  assert.deepEqual(names(tree), ['Read the guide']);
});

test('table regions: a heading with no text falls back to a non-empty label', () => {
  const [label] = names(run(root(heading(2, expression('/* #only-an-id */')), table())));
  assert.notEqual(label.trim(), '');
});

// W5
test('table regions: a table inside a JSX flow element (TabItem, details) is wrapped', () => {
  const tree = run(root(heading(2, text('Inside')), flow('TabItem', [flow('details', [table()])])));
  assert.deepEqual(names(tree), ['Inside']);
});

test('table regions: a table nested in blockquote or list markup is wrapped and counted in document order', () => {
  const tree = run(root(heading(2, text('One')), el('blockquote', {}, [table()]), heading(2, text('Two')), el('ul', {}, [el('li', {}, [table()])])));
  assert.deepEqual(names(tree), ['One', 'Two']);
});

// W6
test('table regions: running the plugin twice keeps one wrapper and the same label', () => {
  const tree = root(heading(2, text('Twice')), table());
  run(tree);
  run(tree);
  assert.equal(regions(tree).length, 1);
  assert.deepEqual(names(tree), ['Twice']);
  assert.equal(regions(tree)[0].children.length, 1);
});

test('table regions: running twice on a page with several tables keeps the labels unique and unchanged', () => {
  const tree = root(heading(2, text('Same')), table(), table(), heading(2, text('Same')), table());
  const once = [...names(run(tree))];
  const twice = names(run(tree));
  assert.deepEqual(twice, once);
  assert.equal(new Set(twice).size, twice.length);
});

// W7
test('table regions: H2 then H3 then a table is named by the H3', () => {
  assert.deepEqual(names(run(root(heading(2, text('Outer')), heading(3, text('Inner')), table()))), ['Inner']);
});

test('table regions: after an H3 the next H2 names the table that follows it', () => {
  assert.deepEqual(names(run(root(heading(2, text('A')), heading(3, text('A1')), heading(2, text('B')), table()))), ['B']);
});

// W8
test('table regions: a heading after the table does not name it', () => {
  const [first] = names(run(root(heading(2, text('Before')), table(), heading(2, text('After')))));
  assert.equal(first, 'Before');
});

// W9
test('table regions: the wrapper is a focusable region with a name and the table keeps its own semantics', () => {
  const tbl = table();
  const tree = run(root(heading(2, text('Semantics')), tbl));
  const [region] = regions(tree);
  assert.equal(region.properties.tabIndex, 0);
  assert.equal(region.properties.role, 'region');
  assert.equal(region.properties.ariaLabel, 'Semantics');
  assert.deepEqual(tbl.properties, {}, 'no role, tabindex or label on the table');
});

// W10
test('table regions: the only child of the wrapper is the table itself and the table order is kept', () => {
  const first = table('first');
  const second = table('second');
  const tree = run(root(heading(2, text('Order')), first, el('p', {}, [text('gap')]), second));
  const found = regions(tree);
  assert.equal(found.length, 2);
  assert.equal(found[0].children.length, 1);
  assert.equal(found[0].children[0], first);
  assert.equal(found[1].children[0], second);
  assert.deepEqual(
    tree.children.map((child) => child.tagName ?? child.type),
    ['h2', 'div', 'p', 'div'],
  );
});

test('table regions: the same heading text twice on a page still gives distinct labels', () => {
  const found = names(run(root(heading(2, text('Notes')), table(), heading(2, text('Notes')), table())));
  assert.equal(new Set(found).size, 2);
  assert.ok(found.every((label) => /^Notes\b/.test(label)));
});

test('table regions: labels differing only in case or spacing are still told apart', () => {
  const found = names(run(root(heading(2, text('Notes')), table(), heading(2, text('NOTES  ')), table())));
  assert.equal(new Set(found.map((label) => label.toLowerCase().replace(/\s+/g, ' ').trim())).size, 2);
});

test('table regions: a page with no table is left unchanged', () => {
  const tree = root(heading(2, text('None')), el('p', {}, [text('text')]));
  const before = JSON.stringify(tree);
  run(tree);
  assert.equal(JSON.stringify(tree), before);
});

test('table regions: a wrapper carries the class the stylesheet styles', () => {
  const [region] = regions(run(root(heading(2, text('Styled')), table())));
  assert.deepEqual(region.properties.className, ['guide-table-region']);
});

// G-8 (qa-strategy): a hand-rolled walk, no node_modules import, so no new dependency
test('table regions: the plugin imports nothing from node_modules', () => {
  const source = readFileSync(new URL('../src/plugins/rehype-table-regions.mjs', import.meta.url), 'utf8');
  const imports = source.split('\n').filter((line) => /^\s*(import\b|export\b.*\bfrom\b)/.test(line) || /\brequire\s*\(/.test(line) || /\bimport\s*\(/.test(line));
  assert.deepEqual(imports, [], 'the plugin has no import line at all, only Node built-ins would be allowed');
});

// --- Part 2: the built rule (rules/table-regions-built.mjs) ------------------------------------------------
// Fixture pages have the shape of the minified Docusaurus output: unquoted attributes, no end tags for
// thead, tbody, tr, th and td. The rule reads the tables inside <article> only.

const TABLE = '<table><thead><tr><th>Package<th>Role<tbody><tr><td><code>domain</code><td>Pure Java</table>';
const region = (label, {tabindex = '0', role = 'region', table = TABLE, label: labelAttr = `aria-label="${label}"`} = {}) => {
  const attrs = ['class=guide-table-region', tabindex === null ? '' : `tabindex=${tabindex}`, role === null ? '' : `role=${role}`, labelAttr].filter(Boolean).join(' ');
  return `<div ${attrs}>${table}</div>`;
};
const page = (body) => `<html><body><nav aria-label=Main></nav><main><article><h1>Title</h1>${body}</article></main></body></html>\n`;
const errorsOf = (body, file = 'p.html') => tableRegions({file, html: page(body)}).errors;

test('table regions rule: a page whose tables all sit in labelled regions passes and counts its tables', () => {
  const result = tableRegions({file: 'p.html', html: page(`<h2>One</h2>${region('One')}<h2>Two</h2>${region('Two')}`)});
  assert.deepEqual(result.errors, []);
  assert.equal(result.tables, 2);
});

test('table regions rule: a page with no table passes with a count of 0', () => {
  assert.deepEqual(tableRegions({file: 'p.html', html: page('<p>text</p>')}), {errors: [], tables: 0});
});

test('table regions rule: a table with no wrapper fails with file and line', () => {
  const errors = errorsOf(`<p>x</p>\n${TABLE}`);
  assert.equal(errors.length, 1);
  assert.match(errors[0], /^table regions: p\.html:\d+ table is not directly inside a region wrapper/);
});

test('table regions rule: a table directly in a span is not in a region wrapper either', () => {
  assert.equal(errorsOf(`<span>${TABLE}</span>`).length, 1);
});

test('table regions rule: a wrapper without tabindex fails and names tabindex', () => {
  const errors = errorsOf(region('A', {tabindex: null}));
  assert.equal(errors.length, 1);
  assert.match(errors[0], /tabindex must be "0" \(found none\)/);
});

test('table regions rule: tabindex -1 and tabindex 1 fail, only 0 passes', () => {
  assert.match(errorsOf(region('A', {tabindex: '-1'}))[0], /tabindex must be "0" \(found "-1"\)/);
  assert.match(errorsOf(region('A', {tabindex: '1'}))[0], /tabindex must be "0" \(found "1"\)/);
  assert.deepEqual(errorsOf(region('A', {tabindex: '"0"'})), []);
});

test('table regions rule: a wrapper without role or with role group fails and names the role', () => {
  assert.match(errorsOf(region('A', {role: null}))[0], /role must be "region" \(found none\)/);
  assert.match(errorsOf(region('A', {role: 'group'}))[0], /role must be "region" \(found "group"\)/);
});

test('table regions rule: a missing, empty or blank aria-label fails', () => {
  for (const label of ['', 'aria-label=""', 'aria-label="   "', 'aria-label=" "']) {
    const errors = errorsOf(region('A', {label}));
    assert.equal(errors.length, 1, `label attr "${label}"`);
    assert.match(errors[0], /aria-label must not be empty/);
  }
});

test('table regions rule: two regions with one name on a page fail once and name the other line', () => {
  const errors = errorsOf(`${region('Same')}\n${region('Same')}`);
  assert.equal(errors.length, 1);
  assert.match(errors[0], /^table regions: p\.html:\d+ region name "Same" is also used by the table at line \d+/);
});

test('table regions rule: names that differ only in case or spacing count as one name', () => {
  assert.equal(errorsOf(`${region('Same name')}${region('SAME   name ')}`).length, 1);
});

test('table regions rule: three regions with one name report each repeat', () => {
  assert.equal(errorsOf(`${region('A')}${region('A')}${region('A')}`).length, 2);
});

test('table regions rule: one region around two tables fails', () => {
  const errors = errorsOf(region('Both', {table: TABLE + TABLE}));
  assert.ok(errors.length >= 1);
  assert.ok(errors.some((error) => /region must hold exactly one element, the table/.test(error)));
});

test('table regions rule: a region that also holds other elements fails', () => {
  const errors = errorsOf(region('Mixed', {table: `<p>note</p>${TABLE}`}));
  assert.ok(errors.some((error) => /region must hold exactly one element, the table/.test(error)));
});

test('table regions rule: a wrapper that is only a grand-ancestor of the table fails', () => {
  const errors = errorsOf(region('Outer', {table: `<div class=inner>${TABLE}</div>`}));
  assert.ok(errors.length >= 1);
  assert.ok(errors.some((error) => /^table regions: p\.html:\d+ table is not directly inside a region wrapper/.test(error)));
});

test('table regions rule: a region must be a div', () => {
  const html = `<section class=guide-table-region tabindex=0 role=region aria-label="A">${TABLE}</section>`;
  assert.match(errorsOf(html)[0], /table is not directly inside a region wrapper/);
});

test('table regions rule: a table outside <article> is not read', () => {
  const outside = `<html><body><footer>${TABLE}</footer><article><p>x</p></article></body></html>`;
  assert.deepEqual(tableRegions({file: 'p.html', html: outside}), {errors: [], tables: 0});
});

test('table regions rule: every problem of one wrapper is reported, each with the line of the table', () => {
  const errors = errorsOf(region('A', {tabindex: '-1', role: 'group', label: ''}));
  assert.equal(errors.length, 3);
  for (const error of errors) assert.match(error, /^table regions: p\.html:\d+ /);
});

// --- scanBuilt and the CLI ----------------------------------------------------------------------------------

function withBuild(files, fn) {
  const root = realpathSync(mkdtempSync(join(tmpdir(), 'guide-table-regions-test-')));
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
const SCRIPTS = dirname(fileURLToPath(import.meta.url));
const CLI = join(SCRIPTS, 'check-built.mjs');
const runScript = (script, args = []) => spawnSync(process.execPath, [script, ...args], {encoding: 'utf8'});
const inBuild = (files) => Object.fromEntries(Object.entries(files).map(([rel, html]) => [`build/${rel}`, html]));

const GOOD = {
  'index.html': page(`<h2>One</h2>${region('One')}`),
  'reference/architecture.html': page(`<h2>A</h2>${region('A')}<h2>B</h2>${region('B')}`),
  'setup-check.html': page('<p>no table</p>'),
};

test('scanBuilt: counts the tables and the pages that hold tables, and the same name on two pages passes', () => {
  const files = {...GOOD, 'other.html': page(`<h2>One</h2>${region('One')}`)};
  withBuild(inBuild(files), (root) => {
    const result = scanBuilt(join(root, 'build'));
    assert.deepEqual(result.errors, []);
    assert.equal(result.tables, 4);
    assert.equal(result.tablePages, 3);
  });
});

test('scanBuilt: a bad table on any page, subfolders included, fails with that page named', () => {
  withBuild(inBuild({'index.html': page('<p>x</p>'), 'reference/architecture.html': page(TABLE)}), (root) => {
    const errors = scanBuilt(join(root, 'build')).errors;
    assert.equal(errors.length, 1);
    assert.match(errors[0], /^table regions: reference\/architecture\.html:\d+ /);
  });
});

test('scanBuilt: a build with no html file is still an error', () => {
  withBuild({'build/assets/x.js': 'x'}, (root) => {
    const result = scanBuilt(join(root, 'build'));
    assert.equal(result.errors.length, 1);
    assert.match(result.errors[0], /no \.html file found/);
  });
});

test('cli: check-built on a build dir prints the table regions line with the counts and exits 0', () => {
  withBuild(inBuild(GOOD), (root) => {
    const result = runScript(CLI, [join(root, 'build')]);
    assert.equal(result.status, 0, result.stdout + result.stderr);
    assert.match(result.stdout, /^ok {3}table regions: 3 tables on 2 pages, each in a focusable region with a name that is unique on its page$/m);
  });
});

test('cli: check-built on a build dir with no table in its pages prints no table regions line', () => {
  withBuild(inBuild({'index.html': page('<p>no table</p>')}), (root) => {
    const result = runScript(CLI, [join(root, 'build')]);
    assert.equal(result.status, 0, result.stdout + result.stderr);
    assert.doesNotMatch(result.stdout, /table regions/);
  });
});

test('cli: check-built on a build dir with an unwrapped table exits 1 and prints the rule message, no ok line', () => {
  withBuild(inBuild({'index.html': page(TABLE)}), (root) => {
    const result = runScript(CLI, [join(root, 'build')]);
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.match(result.stderr, /table regions: index\.html:\d+ table is not directly inside a region wrapper/);
    assert.doesNotMatch(result.stdout, /table regions/);
  });
});

test('cli: with no argument (the default build dir) the table regions rule runs as well', () => {
  const files = {'docusaurus.config.ts': "export default {\n  baseUrl: '/AgenticBookStore/',\n};\n", ...inBuild({'index.html': page(TABLE)})};
  withBuild(files, (root) => {
    cpSync(SCRIPTS, join(root, 'scripts'), {recursive: true, filter: (source) => !source.endsWith('.test.mjs')});
    const result = runScript(join(root, 'scripts', 'check-built.mjs'));
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.match(result.stderr, /table regions: index\.html:\d+ table is not directly inside a region wrapper/);
  });
});
