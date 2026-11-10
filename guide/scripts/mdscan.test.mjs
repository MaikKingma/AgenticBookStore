import {test} from 'node:test';
import assert from 'node:assert/strict';
import {collapse, proseOf, scanMarkdown, tabGroups} from './lib/mdscan.mjs';
import {markupOf} from './lib/mdscan.mjs';

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

test('mdscan: a comment opener inside an inline code span does not start a comment (what follows stays visible)', () => {
  for (const opener of ['{/*', '<!--', '{ /*']) {
    const prose = proseOf(scanMarkdown(`Type \`${opener}\` with care.\nNext line.\n<Tabs groupId="os">\n`));
    assert.equal(collapse(prose), `Type \`${opener}\` with care. Next line. <Tabs groupId="os">`, opener);
  }
  // A longer backtick run holds a single backtick, and the span may have more than one opener.
  assert.equal(collapse(proseOf(scanMarkdown('``a ` {/* <!-- b`` c'))), '``a ` {/* <!-- b`` c');
  // A comment opener after the span, outside any code, is a comment.
  assert.equal(collapse(proseOf(scanMarkdown('`x` <!-- hidden'))), '`x`');
  // The comment that follows the span is still a comment.
  assert.equal(collapse(proseOf(scanMarkdown('`{/*` {/* hidden */} shown'))), '`{/*` shown');
});

test('mdscan: a code span that runs over a line break keeps its comment opener inert too', () => {
  const prose = proseOf(scanMarkdown('Type `a\nb {/*` c\nafter\n\n{/* gone */} end'));
  assert.equal(collapse(prose), 'Type `a b {/*` c after end');
});

test('mdscan: a code span ends where it closes, and an opening backtick finds its closer only in its own paragraph', () => {
  // The line after the closing backtick is prose again: a comment there is a comment.
  assert.equal(collapse(proseOf(scanMarkdown('Type `a\nb {/*` c\nnext {/* hidden */} line'))), 'Type `a b {/*` c next line');
  // A backtick on the other side of a blank line is no closer: the comment after the lone backtick is a comment.
  assert.equal(collapse(proseOf(scanMarkdown('one ` two {/* hidden */} three\n\nlater ` four'))), 'one ` two three later ` four');
  // A fenced block ends the paragraph: a backtick inside it is no closer either.
  assert.equal(collapse(proseOf(scanMarkdown('one ` two {/* hidden */}\n```bash\nx `\n```\nafter'))), 'one ` two after');
});

test('mdscan: a code span over three lines keeps its middle line, a double-backtick span over a line break closes on two backticks', () => {
  // The middle line has no closer of its own: it is code, copied through, not blanked.
  assert.equal(collapse(proseOf(scanMarkdown('Type `a\nb\nc {/*` d\n{/* gone */} end'))), 'Type `a b c {/*` d end');
  // A single backtick does not close a span opened with two: the comment after the double closer is a comment.
  assert.equal(collapse(proseOf(scanMarkdown('Type ``a\nb`` c {/* hidden */} d'))), 'Type ``a b`` c d');
});

test('mdscan: spans are the proseOf offsets of the span contents, one line or many, after a fence too', () => {
  const text = 'a `b` c ``d`` e\n```bash\nx\n```\nf `g\nh` i';
  const scan = scanMarkdown(text);
  const prose = proseOf(scan);
  assert.deepEqual(
    scan.spans.map(([start, end]) => prose.slice(start, end)),
    ['b', 'd', 'g\nh'],
  );
});

test('mdscan: a backtick without a closing one is plain text, and an escaped backtick opens no span', () => {
  assert.equal(collapse(proseOf(scanMarkdown('one ` two {/* hidden */} three'))), 'one ` two three');
  assert.equal(collapse(proseOf(scanMarkdown('one \\`{/* hidden */} two` three'))), 'one \\` two` three');
  // A lone backtick inside a comment does not matter, the comment still closes.
  assert.equal(collapse(proseOf(scanMarkdown('{/* a ` b */} visible'))), 'visible');
});

test('mdscan: the comment opener inside inline code does not hide a Tabs group from tabGroups', () => {
  const text = 'Type `{/*` with care.\n\n<Tabs groupId="os">\n<TabItem value="a" label="Windows PowerShell">\n```powershell\nRemove-Item -Recurse x\n```\n</TabItem>\n</Tabs>\n';
  const scan = scanMarkdown(text);
  assert.equal(tabGroups(scan).groups.length, 1);
  assert.equal(scan.blocks.length, 1);
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

test('tabGroups: a tag written in an inline code span is text, so the item ends at the real closing tag (K2)', () => {
  const text =
    '<Tabs groupId="os">\n<TabItem value="a" label="macOS/Linux">\nx\n</TabItem>\n<TabItem value="b" label="Windows PowerShell">\nClose with `</TabItem>` later.\n```bash\nRemove-Item -Recurse inventedfolder\n```\n</TabItem>\n</Tabs>\n';
  const {groups, errors} = tabGroups(scanMarkdown(text));
  assert.deepEqual(errors, []);
  assert.deepEqual(
    groups.map((g) => [g.groupId, g.items.map((i) => [i.label, i.line, i.end])]),
    [
      [
        'os',
        [
          ['macOS/Linux', 1, 3],
          ['Windows PowerShell', 4, 9],
        ],
      ],
    ],
  );
});

test('tabGroups: an opening or closing tag in an inline code span is no tag, a tag next to a span still is (K1)', () => {
  for (const prose of [
    'Docusaurus writes `<Tabs groupId="os">` for this.',
    'Write ``<TabItem value="a" label="x">`` here.',
    'Write `<Tabs\ngroupId="os">` here, the span runs over a line break.',
    'A `</Tabs>` closes it and a `</TabItem>` ends an item.',
  ]) {
    assert.deepEqual(tabGroups(scanMarkdown(prose)), {groups: [], errors: []}, prose);
  }
  // A span over a line break before a real tag keeps the line numbers of the file.
  const after = tabGroups(scanMarkdown('Write `<Tabs\ngroupId="os">` here.\n<Tabs groupId="os">\n</Tabs>\n'));
  assert.deepEqual([after.groups.map((g) => g.line), after.errors], [[2], []]);
  const next = tabGroups(scanMarkdown('Use `x` <Tabs groupId="os">\n<TabItem value="a" label="macOS/Linux">\n`y` </TabItem>\n</Tabs>\n'));
  assert.deepEqual(next.errors, []);
  assert.deepEqual(
    next.groups.map((g) => [g.groupId, g.items.map((i) => [i.label, i.line, i.end])]),
    [['os', [['macOS/Linux', 1, 2]]]],
  );
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

// The paragraph model of the span recogniser (escalated round, RA-A): an inline code span runs
// over a line break only inside one paragraph, as MDX parses it.
const spanTexts = (text) => {
  const scan = scanMarkdown(text);
  const prose = proseOf(scan);
  return scan.spans.map(([start, end]) => prose.slice(start, end));
};

test('mdscan: a code span does not continue onto a line that starts a block of its own (B10, B11 and the other block starts)', () => {
  for (const next of [
    '- then ` done',
    '+ then ` done',
    '* then ` done',
    '1. then ` done',
    '2) then ` done',
    '123456789. then ` done',
    '  - then ` done',
    '# Then ` done',
    '## Then ` done',
    '###### Then ` done',
    '> then ` done',
    '| then ` |',
    '<b>then ` done</b>',
    '</TabItem> then ` done',
    '{name} then ` done',
    ':::note then ` done',
  ]) {
    const text = `Press the \` key {/* hidden */}\n${next}`;
    assert.deepEqual(spanTexts(text), [], next);
    assert.equal(collapse(proseOf(scanMarkdown(text))), collapse(`Press the \` key\n${next}`), next);
  }
});

test('mdscan: a setext underline, a thematic break, an empty list item or a tag line between two backticks ends the paragraph', () => {
  for (const middle of ['===', '=', '---', '--', '***', '* * *', '___', '- - -', '-', '1.', '<TabItem value="win" label="Windows PowerShell">', '</TabItem>', '{/* note */}', ':::']) {
    const text = `Press the \` key {/* hidden */}\n${middle}\nthen \` done`;
    assert.deepEqual(spanTexts(text), [], middle);
    assert.ok(!proseOf(scanMarkdown(text)).includes('hidden'), middle);
  }
  // A tilde fence may hold a backtick in its info string: it is a fence, so the span ends before it.
  assert.deepEqual(spanTexts('Press the ` key\n~~~ a ` b\nthen ` done\n~~~'), []);
});

test('mdscan: a line that only looks like a block start continues the paragraph, and so does a plain or indented line', () => {
  for (const next of ['then ` done', '    then ` done', '#then ` done', '1.5 then ` done', '-then ` done', '1234567890. then ` done', '**then** ` done', '=then ` done', 'a <b>then</b> ` done']) {
    assert.equal(spanTexts(`Press the \` key\n${next}`).length, 1, next);
  }
  // The controls of the round: three plain lines, the indented and the lazy continuation of a
  // list item, and the one span of the pack that runs over a line break.
  assert.deepEqual(spanTexts('Run `a\nb\nc` here'), ['a\nb\nc']);
  assert.deepEqual(spanTexts('- Run `a\n  b` here'), ['a\n  b']);
  assert.deepEqual(spanTexts('- Run `a\nb` here'), ['a\nb']);
  assert.deepEqual(spanTexts('Sync (`which jdtls`, `/plugin install\njdtls-lsp@claude-plugins-official`, x)'), ['which jdtls', '/plugin install\njdtls-lsp@claude-plugins-official']);
});

test('mdscan: in a block quote a span continues on the next quoted or lazy line, its quote markers are no span content, a deeper quote ends it', () => {
  assert.deepEqual(spanTexts('> Type `{/*\n> ` to start a comment.'), ['{/*\n  ']);
  assert.equal(collapse(proseOf(scanMarkdown('> Type `{/*\n> ` to start a comment.\n\nVisible {/* hidden */} end'))), '> Type `{/* ` to start a comment. Visible end');
  assert.deepEqual(spanTexts('> Run `a\nb` here'), ['a\nb']);
  assert.deepEqual(spanTexts('> > Run `a\n> b` here'), ['a\n  b']);
  assert.deepEqual(spanTexts('> - Run `a\n>   b` here'), ['a\n    b']);
  assert.deepEqual(spanTexts('> Run `a {/* x */}\n> > b` here'), []);
  assert.deepEqual(spanTexts('> Run `a {/* x */}\n>\n> b` here'), [], 'a quote line with nothing after the marker is a blank line');
  assert.deepEqual(spanTexts('> Run `a {/* x */}\n> - b` here'), [], 'a list marker after the quote marker');
});

test('mdscan: a span opened in a heading, a table row, a tag or expression line or an admonition line ends on that line', () => {
  for (const first of [
    '## Press the ` key {/* hidden */}',
    '- ## Press the ` key {/* hidden */}',
    '> ## Press the ` key {/* hidden */}',
    '| Press the ` key {/* hidden */} |',
    '<b>Press the ` key</b> {/* hidden */}',
    '{name} Press the ` key {/* hidden */}',
    ':::note[Press the ` key] {/* hidden */}',
  ]) {
    const text = `${first}\nthen \` done`;
    assert.deepEqual(spanTexts(text), [], first);
    assert.ok(!proseOf(scanMarkdown(text)).includes('hidden'), first);
  }
});

test('mdscan: in a table row a span ends inside its cell, an escaped pipe stays in the span (B12, FI7)', () => {
  assert.deepEqual(spanTexts('| a | `b | c` |'), []);
  assert.deepEqual(spanTexts('| a | `b \\| c` |'), ['b \\| c']);
  assert.deepEqual(spanTexts('| a | `b \\\\| c` |'), [], 'two backslashes: the pipe is a cell border');
  assert.deepEqual(spanTexts('| `a` | `b` |'), ['a', 'b']);
  assert.deepEqual(spanTexts('> | `a | b` |'), []);
  assert.deepEqual(spanTexts('a | `b | c`'), ['b | c'], 'one line without a leading pipe and no delimiter row is a paragraph');
  assert.equal(collapse(proseOf(scanMarkdown('| Press ` | {/* Use WSL and install as on Linux there. */} | then ` |'))), '| Press ` | | then ` |');
  assert.equal(collapse(markupOf(scanMarkdown('| ` | <img src="https://example.com/x.png" alt="x" /> | ` |'))), '| ` | <img src="https://example.com/x.png" alt="x" /> | ` |');
  // A table without leading pipes: the header row before the delimiter row and the rows after it.
  assert.deepEqual(spanTexts('x `a | b` y\n--|--\nc `d | e` f'), []);
  assert.deepEqual(spanTexts('a | b\n- | -\nPress ` | key\nx {/* hidden */} | then `'), []);
  assert.deepEqual(spanTexts('Text `a\nx | y`\n--|--'), [], 'the header row of a table ends the paragraph before it');
  assert.deepEqual(spanTexts('x `a | b` y\n|--|--|\nz'), [], 'a delimiter row with outer pipes');
  // A table ends at a blank line or a fence: the paragraph after it has its spans again.
  assert.deepEqual(spanTexts('a | b\n--|--\nc | d\n\nx `e | f` g'), ['e | f']);
  assert.deepEqual(spanTexts('a | b\n--|--\nc | d\n```\ncode\n```\nx `e | f` g'), ['e | f']);
  // No delimiter row below: a line of pipes without a dash, dashes without a pipe (a setext
  // underline) and a row with a cell that is not dashes.
  assert.deepEqual(spanTexts('Run `a | b` x\n| |'), ['a | b']);
  assert.deepEqual(spanTexts('Run `a | b` x\n|'), ['a | b']);
  assert.deepEqual(spanTexts('Run `a | b` x\n---'), ['a | b']);
  assert.deepEqual(spanTexts('Run `a | b` x\n| a | - |'), ['a | b']);
});

test('mdscan: a backtick after an odd number of backslashes is escaped, after an even number it opens a span (K8, E9)', () => {
  assert.deepEqual(spanTexts('Close with \\\\`</TabItem>` later.'), ['</TabItem>']);
  assert.deepEqual(spanTexts('Run \\\\`export ANTHROPIC_API_KEY` now.'), ['export ANTHROPIC_API_KEY']);
  assert.deepEqual(spanTexts('Not \\`code` here.'), []);
  assert.deepEqual(spanTexts('Not \\\\\\`code` here.'), []);
  assert.deepEqual(spanTexts('a \\``b` c'), ['b'], 'the escaped backtick is text, the backtick after it opens');
  assert.equal(collapse(markupOf(scanMarkdown('Close with \\\\`</TabItem>` later.'))), 'Close with \\\\` ` later.');
});

test('mdscan: a fence after a block quote or list marker is a fenced block, its body without the container markers (RA-D)', () => {
  const quoted = '> ```powershell\n> Remove-Item -Recurse inventedfolder\n> ```\nafter';
  assert.deepEqual(scanMarkdown(quoted).blocks, [{lang: 'powershell', open: 0, close: 2, body: ['Remove-Item -Recurse inventedfolder']}]);
  assert.deepEqual(fences(quoted), ['open', 'body', 'close', null]);
  const listed = '- ```powershell\n  Remove-Item -Recurse inventedfolder\n  ```\nafter';
  assert.deepEqual(scanMarkdown(listed).blocks, [{lang: 'powershell', open: 0, close: 2, body: ['Remove-Item -Recurse inventedfolder']}]);
  assert.deepEqual(fences(listed), ['open', 'body', 'close', null]);
  for (const [opener, body, closer] of [
    ['1. ```powershell', '   x', '   ```'],
    ['* ~~~ps1', '  x', '  ~~~'],
    ['> > ```bash', '> > x', '> > ```'],
    ['> - ```bash', '>   x', '>   ```'],
    ['- > ```bash', '  > x', '  > ```'],
    ['>```bash', '>x', '>```'],
    ['  - ```bash', '    x', '    ```'],
  ]) {
    const scan = scanMarkdown(`${opener}\n${body}\n${closer}\nafter`);
    assert.deepEqual(scan.blocks.map((b) => [b.open, b.close, b.body]), [[0, 2, ['x']]], opener);
  }
});

test('mdscan: a fence in a block quote or a list item ends with its container, the line outside is prose again (RA-D)', () => {
  const quote = scanMarkdown('> ```text\n> x\n<img src="/x.png">\n> more\n```');
  assert.deepEqual(quote.blocks[0], {lang: 'text', open: 0, close: 1, body: ['x']});
  assert.equal(proseOf(quote).split('\n')[2], '<img src="/x.png">');
  assert.deepEqual(scanMarkdown('> ```text\n> x\n\n> y').blocks, [{lang: 'text', open: 0, close: 1, body: ['x']}]);
  const item = scanMarkdown('- ```text\n  x\n\n  y\nz\n  ```');
  assert.deepEqual(item.blocks, [
    {lang: 'text', open: 0, close: 3, body: ['x', '', 'y']},
    {lang: '', open: 5, close: 5, body: []},
  ]);
  assert.equal(proseOf(item).split('\n')[4], 'z');
});

test('tabGroups: a tag MDX renders as text, after an odd number of backslashes or in a string literal of an expression, is no tag (K3, K4)', () => {
  const items = (windows) => {
    const text = `<Tabs groupId="os">\n<TabItem value="a" label="macOS/Linux">\nx\n</TabItem>\n<TabItem value="b" label="Windows PowerShell">\n${windows}\n\`\`\`bash\nRemove-Item -Recurse inventedfolder\n\`\`\`\n</TabItem>\n</Tabs>\n`;
    const {groups, errors} = tabGroups(scanMarkdown(text));
    return [errors, groups.map((g) => g.items.map((i) => [i.label, i.line, i.end]))];
  };
  const whole = [[], [[['macOS/Linux', 1, 3], ['Windows PowerShell', 4, 9]]]];
  for (const windows of [
    'Close with \\</TabItem> later.',
    'Close with \\\\\\</TabItem> later.',
    "Close with {'</TabItem>'} later.",
    'Close with {"</TabItem>"} later.',
    "Close with {'a' + \"</TabItem>\" + 'b'} later.",
    "Close with {'it\\'s </TabItem>'} later.",
    'Close with {`</TabItem>`} later.',
  ]) {
    assert.deepEqual(items(windows), whole, windows);
  }
  // Two backslashes are one escaped backslash, and an escaped brace opens no expression: the tag after them is a tag.
  const early = [[], [[['macOS/Linux', 1, 3], ['Windows PowerShell', 4, 5]]]];
  assert.deepEqual(items('Close with \\\\</TabItem> later.'), early);
  assert.deepEqual(items("Close with \\{'</TabItem>'} later."), early);
  // A tag inside an expression but outside its strings is still read, its attributes untouched.
  const inside = tabGroups(scanMarkdown('Use {<Tabs groupId="os"><TabItem value="a" label="macOS/Linux">x</TabItem></Tabs>} here.\n'));
  assert.deepEqual(inside.groups.map((g) => [g.groupId, g.items.map((i) => i.label)]), [['os', ['macOS/Linux']]]);
  // An expression ends at its closing brace: a tag after it is a tag.
  assert.deepEqual(items("Close {'x'} with </TabItem> later."), early);
  // A brace or quote inside an attribute string of a tag opens nothing: the tag stays whole, and
  // an escaped tag after it is still text.
  const quoted = tabGroups(scanMarkdown('<Tabs groupId="os">\n<TabItem value="{" label="macOS/Linux">\nx\n</TabItem>\n</Tabs>\n'));
  assert.deepEqual(quoted.groups.map((g) => g.items.map((i) => i.label)), [['macOS/Linux']]);
  const titled = `<Tabs groupId="os">\n<TabItem value="a" label="macOS/Linux">\nx\n</TabItem>\n<TabItem value="b" label="Windows PowerShell" title="{">\nClose with \\</TabItem> later.\n\`\`\`bash\nRemove-Item -Recurse inventedfolder\n\`\`\`\n</TabItem>\n</Tabs>\n`;
  assert.deepEqual(tabGroups(scanMarkdown(titled)).groups.map((g) => g.items.map((i) => [i.label, i.line, i.end])), [[['macOS/Linux', 1, 3], ['Windows PowerShell', 4, 9]]]);
});

test('mdscan: a comment opener after an odd number of backslashes is text, as MDX renders it, after an even number it opens a comment', () => {
  assert.equal(collapse(proseOf(scanMarkdown('Type \\{/* to start a comment.\n<Tabs groupId="os">'))), 'Type \\{/* to start a comment. <Tabs groupId="os">');
  assert.equal(collapse(proseOf(scanMarkdown('Type \\<!-- as text.\nnext'))), 'Type \\<!-- as text. next');
  assert.equal(collapse(proseOf(scanMarkdown('Type \\\\{/* hidden */} shown'))), 'Type \\\\ shown');
});

test('mdscan: an expression that closes on its line is its value: a comment opener or backticks in its strings are text', () => {
  assert.equal(collapse(proseOf(scanMarkdown("Type {'{/*'} to start a comment.\n<Tabs groupId=\"os\">"))), "Type {'{/*'} to start a comment. <Tabs groupId=\"os\">");
  assert.equal(collapse(proseOf(scanMarkdown('Type {"{/*"} and {/* hidden */} shown'))), 'Type {"{/*"} and shown');
  assert.deepEqual(spanTexts('Run {`export ANTHROPIC_API_KEY`} now and `x` too.'), ['x']);
  assert.deepEqual(spanTexts("Use {'}'} and `x`."), ['x'], 'a brace in a string does not close the expression');
  assert.equal(collapse(proseOf(scanMarkdown("Type {'}{/*'} shown"))), "Type {'}{/*'} shown");
  // What comes first on the line comes first: a comment before an expression is a comment, a span before one a span.
  assert.equal(collapse(proseOf(scanMarkdown('{/* hidden */} {name} shown'))), '{name} shown');
  assert.deepEqual(spanTexts('`{` and {name}'), ['{']);
  // A brace that does not close on its line, or an escaped one, opens nothing: the comment after it is a comment.
  assert.equal(collapse(proseOf(scanMarkdown('Open { here {/* hidden */} shown'))), 'Open { here shown');
  assert.equal(collapse(proseOf(scanMarkdown("Type \\{'{/* hidden */}'} shown"))), "Type \\{' '} shown");
});

test('mdscan: inside a span a ``` line with a backtick in its info string is no fence, the span runs over it (K7)', () => {
  const scan = scanMarkdown('Close with `</TabItem>\n``` no fence ` later.');
  assert.deepEqual(scan.blocks, []);
  assert.deepEqual(spanTexts('Close with `</TabItem>\n``` no fence ` later.'), ['</TabItem>\n``` no fence ']);
  assert.ok(!markupOf(scan).includes('</TabItem>'));
});
