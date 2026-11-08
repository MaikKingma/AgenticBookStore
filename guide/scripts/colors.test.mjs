import {test} from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync, mkdirSync, writeFileSync, rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join, dirname} from 'node:path';
import {colorTokens, cssColorErrors, styleColorErrors} from './rules/colors.mjs';
import {runChecks} from './check.mjs';
import {goodPackage, goodSiteFiles} from './lib/test-fixtures.mjs';

const FUNCTIONS = ['rgb', 'rgba', 'hsl', 'hsla', 'hwb', 'lab', 'lch', 'oklab', 'oklch', 'color', 'color-mix'];

test('colorTokens: hex colours of 3, 4, 6 and 8 digits in any case', () => {
  for (const hex of ['#fff', '#FFFF', '#1d252d', '#1D252DFF', '#3d7DC9']) {
    assert.deepEqual(colorTokens(`1px solid ${hex}`), [hex], hex);
  }
});

test('colorTokens: five or seven hex digits and non-hex letters are not colours', () => {
  for (const text of ['#fffff', '#fffffff', '#ggg', '#12', '1px']) {
    assert.deepEqual(colorTokens(text), [], text);
  }
});

test('colorTokens: every colour function is found, in any case', () => {
  for (const fn of FUNCTIONS) {
    assert.deepEqual(colorTokens(`${fn}(0 0 0)`), [`${fn}(`], fn);
  }
  assert.deepEqual(colorTokens('RGB(0, 0, 0)'), ['RGB(']);
  assert.deepEqual(colorTokens('color-mix(in srgb, white 50%, black)').length, 3);
});

test('colorTokens: CSS named colours are found whole-word and in any case', () => {
  assert.deepEqual(colorTokens('red'), ['red']);
  assert.deepEqual(colorTokens('1px solid White'), ['White']);
  assert.deepEqual(colorTokens('rebeccapurple'), ['rebeccapurple']);
  assert.deepEqual(colorTokens('linear-gradient(navy, tan)'), ['navy', 'tan']);
});

test('colorTokens: inherit, currentColor, transparent and other keywords are allowed', () => {
  for (const text of ['inherit', 'currentColor', 'currentcolor', 'transparent', 'none', 'initial', 'unset']) {
    assert.deepEqual(colorTokens(text), [], text);
  }
});

test('colorTokens: var() references, custom property names, url() and lengths are not colours', () => {
  assert.deepEqual(colorTokens('var(--ifm-color-primary)'), []);
  assert.deepEqual(colorTokens('var(--my-red-tint)'), []);
  assert.deepEqual(colorTokens('url(./fonts/red.woff2) format("woff2")'), []);
  assert.deepEqual(colorTokens('calc(1px + 2rem) Montserrat, system-ui, sans-serif'), []);
});

test('colorTokens: a colour name inside a longer word or custom property name is not a colour', () => {
  for (const text of [
    'var(--my-red-tint)',
    'var(--my-red)',
    'var(--red-tint)',
    'red-fade',
    'fade-red',
    'redish',
    'predator',
    'tint_red',
    'red_tint',
    'red2',
  ]) {
    assert.deepEqual(colorTokens(text), [], text);
  }
});

test('colorTokens: the math function tan() is not the colour tan', () => {
  assert.deepEqual(colorTokens('rotate(tan(45deg))'), []);
  assert.deepEqual(colorTokens('calc(1px * tan( 45deg ))'), []);
  assert.deepEqual(colorTokens('linear-gradient(navy, tan)'), ['navy', 'tan']);
  assert.deepEqual(colorTokens('tan'), ['tan']);
});

test('colorTokens: a colour in the fallback of var() is still a colour literal', () => {
  assert.deepEqual(colorTokens('var(--x, #fff)'), ['#fff']);
  assert.deepEqual(colorTokens('var(--x, red)'), ['red']);
});

test('cssColorErrors: a hex colour in a declaration fails with file, line, column and the home of colours', () => {
  const errors = cssColorErrors('src/components/x.module.css', '.a {\n  margin: 0;\n  color: #fff;\n}\n');
  assert.equal(errors.length, 1);
  assert.match(errors[0], /^colors: src\/components\/x\.module\.css:3:10 colour literal "#fff"; /);
  assert.match(errors[0], /src\/css\/custom\.css/);
});

test('cssColorErrors: colour functions and named colours in any property fail', () => {
  const text = '.a { background: rgb(0 0 0); border: 1px solid red; box-shadow: 0 0 1px oklch(50% 0 0) }\n';
  assert.deepEqual(
    cssColorErrors('a.css', text).map((e) => /colour literal "([^"]+)"/.exec(e)[1]),
    ['rgb(', 'red', 'oklch('],
  );
});

test('cssColorErrors: custom properties are values too', () => {
  assert.equal(cssColorErrors('a.css', ':root { --brand: #014046; }\n').length, 1);
});

test('cssColorErrors: a last declaration without a semicolon and nested rules are read', () => {
  assert.equal(cssColorErrors('a.css', '.a { color: red }\n').length, 1);
  assert.equal(cssColorErrors('a.css', '.a {\n  &:hover { color: red; }\n}\n').length, 1);
  assert.equal(cssColorErrors('a.css', '@media (min-width: 996px) {\n  .a { background: blue; }\n}\n').length, 1);
  assert.equal(cssColorErrors('a.css', '.a { color: red').length, 1);
});

test('cssColorErrors: selectors, comments, strings and urls that mention colours do not fail', () => {
  const text = [
    '/* color: #fff; and red */',
    '.red:hover, a:not(.blue) { margin: 0; }',
    '.a::after { content: "red #fff"; }',
    ".b { background: url('./red.png'); }",
    '',
  ].join('\n');
  assert.deepEqual(cssColorErrors('a.css', text), []);
});

test('cssColorErrors: a data URI with semicolons does not split a declaration or hide the next one', () => {
  const text = '.a { background: url(data:image/svg+xml;utf8,<svg fill="red"/>); color: blue; }\n';
  assert.deepEqual(
    cssColorErrors('a.css', text).map((e) => /colour literal "([^"]+)"/.exec(e)[1]),
    ['blue'],
  );
});

test('cssColorErrors: inherit, currentColor, transparent and var() pass', () => {
  const text = '.a { color: inherit; border-color: currentColor; background: transparent; fill: var(--ifm-color-primary); }\n';
  assert.deepEqual(cssColorErrors('a.css', text), []);
});

test('styleColorErrors: a named, hex or function colour in a JSX style object fails', () => {
  const text = [
    "export const A = () => <p style={{color: 'white'}}>x</p>;",
    'export const B = () => <p style={{background: "#fff", margin: 0}}>x</p>;',
    "export const C = () => <p style={{border: '1px solid red'}}>x</p>;",
    'export const D = () => <p style={{color: `rgb(0 0 0)`}}>x</p>;',
    '',
  ].join('\n');
  const errors = styleColorErrors('src/x.tsx', text);
  assert.deepEqual(
    errors.map((e) => /^colors: src\/x\.tsx:(\d+):\d+ colour literal "([^"]+)" in a style prop/.exec(e)?.slice(1, 3).join(' ')),
    ['1 white', '2 #fff', '3 red', '4 rgb('],
  );
});

test('styleColorErrors: both branches of a ternary inside a style prop are read', () => {
  const text = "<p style={{color: dark ? 'white' : 'black'}}>x</p>\n";
  assert.equal(styleColorErrors('docs/a.mdx', text).length, 2);
});

test('styleColorErrors: an HTML style attribute in Markdown or MDX fails', () => {
  assert.equal(styleColorErrors('docs/a.md', '<span style="color: red">x</span>\n').length, 1);
  assert.equal(styleColorErrors('docs/a.mdx', "<span style='margin: 0; background: #fff'>x</span>\n").length, 1);
});

test('styleColorErrors: style props without a colour pass, and style object keys are not values', () => {
  const text = [
    "<p style={{display: 'none', margin: 0, color: 'var(--ifm-color-primary)'}}>x</p>",
    "<p style={{'color': 'inherit', 'background-color': 'transparent'}}>x</p>",
    '<span style="margin: 0; color: currentColor">x</span>',
    'The style of this page is plain.',
    '',
  ].join('\n');
  assert.deepEqual(styleColorErrors('docs/a.mdx', text), []);
});

test('styleColorErrors: Markdown anchors and heading ids that look like hex colours are not colour values', () => {
  const text = [
    '## Add book reviews {#deadbeef}',
    '',
    'See [reviews](#add-book-reviews), [bad](#bad), [cafe](#cafe) and [fade](#facade).',
    '',
  ].join('\n');
  assert.deepEqual(styleColorErrors('docs/a.md', text), []);
});

function withTree(files, fn) {
  const root = mkdtempSync(join(tmpdir(), 'guide-colors-test-'));
  try {
    const all = {
      'package.json': JSON.stringify(goodPackage()),
      'docs/index.md': '---\ntitle: Welcome\nslug: /\n---\n\nHello.\n',
      'scripts/a.test.mjs': "import {test} from 'node:test';\ntest('real case', () => {});\n",
      ...goodSiteFiles(),
      ...files,
    };
    for (const [rel, content] of Object.entries(all)) {
      const abs = join(root, rel);
      mkdirSync(dirname(abs), {recursive: true});
      writeFileSync(abs, content);
    }
    return fn(root);
  } finally {
    rmSync(root, {recursive: true, force: true});
  }
}

const colorLines = (errors) => errors.filter((error) => error.startsWith('colors:'));

test('runChecks: a clean tree has an ok colors line', () => {
  withTree({}, (root) => {
    const {lines, errors} = runChecks(root);
    assert.deepEqual(errors, []);
    assert.ok(lines.some((line) => line.startsWith('ok   colors')), lines.join(' | '));
  });
});

test('runChecks: custom.css may hold colour values, any other stylesheet may not', () => {
  withTree({'src/css/custom.css': ':root { --a: #014046; }\n'}, (root) => {
    assert.deepEqual(colorLines(runChecks(root).errors), []);
  });
  withTree({'src/css/custom.css': ':root { --a: #014046; }\n', 'src/css/extra.css': '.a { color: #fff; }\n'}, (root) => {
    const errors = colorLines(runChecks(root).errors);
    assert.equal(errors.length, 1);
    assert.match(errors[0], /^colors: src\/css\/extra\.css:1:13 /);
  });
  withTree({'src/pages/custom.css': '.a { color: #fff; }\n'}, (root) => {
    assert.equal(colorLines(runChecks(root).errors).length, 1);
  });
});

test('runChecks: a new CSS module with color: #fff fails the colors rule', () => {
  withTree({'src/components/Box/styles.module.css': '.box {\n  color: #fff;\n}\n'}, (root) => {
    const {lines, errors} = runChecks(root);
    assert.equal(colorLines(errors).length, 1);
    assert.ok(lines.some((line) => line.startsWith('FAIL colors')));
  });
});

test('runChecks: a style prop with a colour in an MDX page fails the colors rule', () => {
  withTree({'docs/page.mdx': "---\ntitle: P\n---\n\n<p style={{color: 'white'}}>x</p>\n"}, (root) => {
    const errors = colorLines(runChecks(root).errors);
    assert.equal(errors.length, 1);
    assert.match(errors[0], /^colors: docs\/page\.mdx:5:/);
  });
});

test('runChecks: a page full of anchors that look like hex colours passes', () => {
  const page = '---\ntitle: P\n---\n\n## Reviews {#deadbeef}\n\n[a](#add-book-reviews) [b](#bad) [c](#cafe)\n';
  withTree({'docs/page.md': page}, (root) => {
    assert.deepEqual(colorLines(runChecks(root).errors), []);
  });
});

test('runChecks: generated folders and scripts are not scanned for colours', () => {
  withTree(
    {
      'build/assets/main.css': '.a { color: #fff; }\n',
      'node_modules/x/x.css': '.a { color: red; }\n',
      'scripts/lib/fixture.mjs': "export const x = 'color: #fff';\n",
    },
    (root) => {
      assert.deepEqual(colorLines(runChecks(root).errors), []);
    },
  );
});
