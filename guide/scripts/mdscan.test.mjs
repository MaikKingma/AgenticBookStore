import {test} from 'node:test';
import assert from 'node:assert/strict';
import {collapse, proseOf, scanMarkdown, tabGroups} from './lib/mdscan.mjs';

const fences = (text) => scanMarkdown(text).lines.map((line) => line.fence);

test('mdscan: fence lines, bodies and the language of a fenced block', () => {
  const scan = scanMarkdown('Text\n```bash\ngit status\n# a comment\n```\nAfter\n');
  assert.deepEqual(fences('Text\n```bash\ngit status\n# a comment\n```\nAfter\n'), [null, 'open', 'body', 'body', 'close', null, null]);
  assert.deepEqual(scan.blocks, [{lang: 'bash', open: 1, close: 4, body: ['git status', '# a comment']}]);
});

test('mdscan: an indented fence inside a TabItem is a fence, a longer fence is closed only by an equally long one', () => {
  const scan = scanMarkdown('<TabItem value="a">\n  ```bash\n  ls\n  ```\n</TabItem>\n````md\n```\ninner\n```\n````\n');
  assert.deepEqual(
    scan.blocks.map((b) => [b.lang, b.body]),
    [
      ['bash', ['  ls']],
      ['md', ['```', 'inner', '```']],
    ],
  );
});

test('mdscan: tilde fences work, a backtick on the info line means no fence, an unclosed fence runs to the end', () => {
  assert.equal(scanMarkdown('~~~sh\nx\n~~~\n').blocks.length, 1);
  assert.equal(scanMarkdown('```not a fence ` here\nx\n').blocks.length, 0);
  const open = scanMarkdown('```bash\nx\ny');
  assert.deepEqual(open.blocks, [{lang: 'bash', open: 0, close: 0, body: ['x', 'y']}]);
});

test('mdscan: MDX and HTML comments are blanked in prose, one line or many, offsets kept', () => {
  const text = 'a {/* one */} b\n{/*\nmulti\n*/} c\n<!-- x --> d\n<!--\nmore -->\ne';
  const prose = proseOf(scanMarkdown(text)).split('\n');
  assert.deepEqual(prose.map((line) => collapse(line)).filter(Boolean), ['a b', 'c', 'd', 'e']);
  assert.equal(prose[0].length, 'a {/* one */} b'.length);
  assert.equal(prose.length, text.split('\n').length);
});

test('mdscan: a JSX comment with spaces inside the braces is a comment too', () => {
  const prose = proseOf(scanMarkdown('a { /* one */ } b {/* two */ } c { /*\nthree\n*/} d'));
  assert.equal(collapse(prose), 'a b c d');
});

test('mdscan: a fence inside a comment is not a fence, a comment inside a fence is code', () => {
  const hidden = scanMarkdown('{/*\n```bash\nrm -rf x\n```\n*/}\nvisible');
  assert.equal(hidden.blocks.length, 0);
  assert.equal(proseOf(hidden).replace(/\s+/g, ''), 'visible');
  const code = scanMarkdown('```js\n{/* kept */}\n```\n');
  assert.deepEqual(code.blocks[0].body, ['{/* kept */}']);
});

test('mdscan: proseOf drops fenced lines but keeps the line count', () => {
  const text = 'one\n```\ncode\n```\ntwo';
  assert.deepEqual(proseOf(scanMarkdown(text)).split('\n'), ['one', '', '', '', 'two']);
});

test('mdscan: windows line endings are read like unix ones', () => {
  assert.equal(scanMarkdown('```bash\r\nls\r\n```\r\n').blocks[0].body[0], 'ls');
});

test('collapse: whitespace runs become one space and the ends are trimmed', () => {
  assert.equal(collapse('  a \n\t b  '), 'a b');
});

const TABS = `
<Tabs groupId="os">
  <TabItem value="mac" label="macOS/Linux">
    \`\`\`bash
    ls
    \`\`\`
  </TabItem>
  <TabItem value="win" label='Windows PowerShell'>
    text
  </TabItem>
</Tabs>
`;

test('tabGroups: reads the group id, the labels and the line span of each item', () => {
  const {groups, errors} = tabGroups(scanMarkdown(TABS));
  assert.deepEqual(errors, []);
  assert.equal(groups.length, 1);
  assert.equal(groups[0].groupId, 'os');
  assert.deepEqual(
    groups[0].items.map((i) => [i.label, i.line, i.end]),
    [
      ['macOS/Linux', 2, 6],
      ['Windows PowerShell', 7, 9],
    ],
  );
});

test('tabGroups: a missing groupId, a label that is not a string literal and a multi-line tag are read', () => {
  const text = '<Tabs>\n<TabItem\n  value="a"\n  label={name}>\nx\n</TabItem>\n</Tabs>\n';
  const {groups} = tabGroups(scanMarkdown(text));
  assert.equal(groups[0].groupIdPresent, false);
  assert.equal(groups[0].groupId, undefined);
  assert.equal(groups[0].items[0].labelPresent, true);
  assert.equal(groups[0].items[0].label, undefined);
  assert.equal(groups[0].items[0].line, 1);
});

test('tabGroups: tags inside a fenced block or a comment are not tabs', () => {
  const text = '```mdx\n<Tabs groupId="os">\n```\n{/* <Tabs groupId="x"> */}\n';
  const {groups, errors} = tabGroups(scanMarkdown(text));
  assert.deepEqual([groups, errors], [[], []]);
});

test('tabGroups: unbalanced and stray tags are errors', () => {
  assert.match(tabGroups(scanMarkdown('<Tabs groupId="os">\n<TabItem value="a" label="x">\n')).errors.join('|'), /never closed/);
  assert.match(tabGroups(scanMarkdown('</Tabs>\n')).errors.join('|'), /closing <\/Tabs>/);
  assert.match(tabGroups(scanMarkdown('<TabItem value="a" label="x">\n</TabItem>\n')).errors.join('|'), /outside <Tabs>/);
});

test('tabGroups: nested groups keep the item spans apart', () => {
  const text = '<Tabs groupId="track">\n<TabItem label="Track A: OpenSpec">\n<Tabs groupId="os">\n<TabItem label="macOS/Linux">\nx\n</TabItem>\n</Tabs>\ny\n</TabItem>\n</Tabs>\n';
  const {groups, errors} = tabGroups(scanMarkdown(text));
  assert.deepEqual(errors, []);
  assert.deepEqual(
    groups.map((g) => [g.groupId, g.items.map((i) => [i.label, i.line, i.end])]),
    [
      ['track', [['Track A: OpenSpec', 1, 8]]],
      ['os', [['macOS/Linux', 3, 5]]],
    ],
  );
});
