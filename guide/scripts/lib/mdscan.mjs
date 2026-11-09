// A small scanner for the Markdown and MDX of the guide docs and of the pack files, shared
// by the coverage and content rules. It knows three things the rules need and a regular
// expression over the whole text gets wrong: fenced code blocks, MDX and HTML comments
// (a commented-out fence is not a fence, a comment inside a fence is code) and the Tabs
// and TabItem elements. Pure functions on text.

const FENCE_OPEN = /^\s*(`{3,}|~{3,})(.*)$/;
// The two comment forms of MDX pages: a JSX comment (spaces inside the braces are allowed,
// they are the same JavaScript) and an HTML comment.
const COMMENTS = [
  {open: /\{\s*\/\*/, close: /\*\/\s*\}/},
  {open: /<!--/, close: /-->/},
];

// A run of backticks that can open an inline code span (not escaped, not part of a longer run),
// and the run of exactly the same length that closes it.
const BACKTICK_RUN = /(?<![\\`])(`+)(?!`)/;
const closerOf = (length) => new RegExp(`(?<!\`)\`{${length}}(?!\`)`);

const spaces = (text) => text.replace(/[^\n]/g, ' ');

// Whitespace runs become one space and the ends are trimmed.
export const collapse = (text) => text.replace(/\s+/g, ' ').trim();

// scanMarkdown(text) returns {lines, blocks, spans}.
//   lines: one {text, fence} per line of the text. fence is null for prose, 'open' or
//     'close' for the fence lines and 'body' for the lines in between. The text of a prose
//     line has its comments blanked with spaces (offsets stay valid); fenced lines are raw.
//     A comment opener inside an inline code span (`{/*`) is code, not a comment, also when
//     the span runs over a line break.
//   blocks: one {lang, open, close, body} per fenced block: the 0-based line numbers of the
//     two fence lines (close is the last line when the fence never closes) and the raw
//     body lines.
//   spans: one [start, end] per inline code span, the offsets in proseOf(scan) of its
//     content (between the backtick runs). This is the one code span recogniser: the
//     comment blanking above, the tag reading of tabGroups and the code of codeOf all
//     use it, so they cannot disagree about what is code.
export function scanMarkdown(text) {
  const lines = [];
  const blocks = [];
  const spans = [];
  const all = text.replace(/\r\n/g, '\n').split('\n');
  // Does a span opened on line `at` with `length` backticks close later in the same paragraph?
  const spanContinues = (at, length) => {
    for (let next = at + 1; next < all.length && all[next].trim() !== '' && !FENCE_OPEN.test(all[next]); next++) {
      if (closerOf(length).test(all[next])) return true;
    }
    return false;
  };
  let fence = null;
  let closer = null;
  let carry = 0; // the backtick count of an inline code span that continues on this line
  let opened = 0; // the proseOf offset where the content of that continuing span starts
  let offset = 0; // the proseOf offset of the current line (a fenced line is empty there)
  for (const raw of all) {
    const index = lines.length;
    if (index > 0) offset += (lines[index - 1].fence === null ? lines[index - 1].text.length : 0) + 1;
    if (raw.trim() === '') carry = 0;
    if (fence) {
      const closes = new RegExp(`^\\s*${fence.char}{${fence.length},}\\s*$`).test(raw);
      lines.push({text: raw, fence: closes ? 'close' : 'body'});
      if (closes) {
        fence.block.close = index;
        fence = null;
      } else {
        fence.block.body.push(raw);
      }
      continue;
    }
    let rest = raw;
    let prose = '';
    if (closer === null) {
      const open = FENCE_OPEN.exec(raw);
      // An info string with a backtick is no opening fence (inline code on the line).
      if (open && !(open[1][0] === '`' && open[2].includes('`'))) {
        const block = {lang: open[2].trim().split(/\s+/)[0] ?? '', open: index, close: index, body: []};
        blocks.push(block);
        fence = {char: open[1][0], length: open[1].length, block};
        lines.push({text: raw, fence: 'open'});
        continue;
      }
    }
    // Blank the comments of this line; a comment that does not close on it carries over.
    while (rest !== '') {
      if (closer !== null) {
        const end = closer.exec(rest);
        if (end === null) {
          prose += spaces(rest);
          rest = '';
        } else {
          const stop = end.index + end[0].length;
          prose += spaces(rest.slice(0, stop));
          rest = rest.slice(stop);
          closer = null;
        }
        continue;
      }
      if (carry > 0) {
        const end = closerOf(carry).exec(rest);
        if (end === null) {
          prose += rest;
          rest = '';
        } else {
          const stop = end.index + end[0].length;
          spans.push([opened, offset + prose.length + end.index]);
          prose += rest.slice(0, stop);
          rest = rest.slice(stop);
          carry = 0;
        }
        continue;
      }
      let first = null;
      for (const comment of COMMENTS) {
        const found = comment.open.exec(rest);
        if (found && (first === null || found.index < first.index)) first = {index: found.index, length: found[0].length, close: comment.close};
      }
      // An inline code span that starts before the first comment opener is code: copy it through.
      const run = BACKTICK_RUN.exec(rest);
      if (run && (first === null || run.index < first.index)) {
        const length = run[1].length;
        const after = run.index + length;
        const end = closerOf(length).exec(rest.slice(after));
        if (end) {
          const stop = after + end.index + end[0].length;
          spans.push([offset + prose.length + after, offset + prose.length + after + end.index]);
          prose += rest.slice(0, stop);
          rest = rest.slice(stop);
        } else if (spanContinues(index, length)) {
          opened = offset + prose.length + after;
          prose += rest;
          rest = '';
          carry = length;
        } else {
          prose += rest.slice(0, after);
          rest = rest.slice(after);
        }
        continue;
      }
      if (first === null) {
        prose += rest;
        rest = '';
      } else {
        prose += rest.slice(0, first.index) + spaces(rest.slice(first.index, first.index + first.length));
        rest = rest.slice(first.index + first.length);
        closer = first.close;
      }
    }
    lines.push({text: prose, fence: null});
  }
  return {lines, blocks, spans};
}

// The prose of a scan as one text: fenced lines are empty, comments are blank, so the
// line numbers still match the file.
export function proseOf(scan) {
  return scan.lines.map((line) => (line.fence === null ? line.text : '')).join('\n');
}

// The markup of a scan: proseOf with the content of every inline code span blanked too, so
// that a tag written in a span (`</TabItem>`) is text, as MDX renders it, and never a tag.
// Offsets and line numbers still match the file.
export function markupOf(scan) {
  const chars = proseOf(scan).split('');
  for (const [start, end] of scan.spans) {
    for (let i = start; i < end; i++) if (chars[i] !== '\n') chars[i] = ' ';
  }
  return chars.join('');
}

// A raw HTML code element in the markup: <code ...>...</code> or <pre ...>...</pre>, any
// letter case, over line breaks; group 2 is its content.
const HTML_CODE = /<(code|pre)(?=[\s>])[^>]*>([\s\S]*?)<\/\1\s*>/gi;

// The code of a scan as one text with the line structure of the file: the fenced lines as
// written, the inside of every inline code span (scan.spans) and the inside of every raw
// <code> and <pre> element of the markup; everything else (the prose, comments, the
// backticks and tags themselves) replaced by a filler character, so that a word of a
// sentence is never read as part of a command.
export function codeOf(scan) {
  const prose = proseOf(scan);
  const keep = prose.split('').map((char) => char === '\n');
  for (const [start, end] of scan.spans) keep.fill(true, start, end);
  for (const m of markupOf(scan).matchAll(HTML_CODE)) {
    const start = m.index + m[0].indexOf('>') + 1;
    keep.fill(true, start, start + m[2].length);
  }
  const proseLines = prose.split('').map((char, i) => (keep[i] ? char : '\u0001')).join('').split('\n');
  return scan.lines.map((line, index) => (line.fence === null ? proseLines[index] : line.text)).join('\n');
}

// The attribute `name` of a tag text: {present, value}. value is undefined when the
// attribute is not a plain string literal (for example label={x}).
function attribute(tag, name) {
  const present = new RegExp(`\\b${name}\\s*=`).test(tag);
  const match = new RegExp(`\\b${name}\\s*=\\s*(?:"([^"]*)"|'([^']*)')`).exec(tag);
  return {present, value: match ? (match[1] ?? match[2]) : undefined};
}

// The Docusaurus Tabs groups of a scan: {groups, errors}. A group is {line, groupId,
// items}; an item is {line, end, label} with 0-based line numbers (end is the line of the
// closing tag, or the last line when it never closes). errors name unbalanced tags. Tags
// are read in the markup only: never in a fenced block, a comment or an inline code span.
export function tabGroups(scan) {
  const prose = markupOf(scan);
  const lineOf = (offset) => prose.slice(0, offset).split('\n').length - 1;
  const groups = [];
  const errors = [];
  const stack = [];
  const open = [];
  const lastLine = scan.lines.length - 1;
  for (const m of prose.matchAll(/<(\/?)(Tabs|TabItem)\b([^>]*)>/g)) {
    const [, closing, name, tag] = m;
    const line = lineOf(m.index);
    if (tag.trimEnd().endsWith('/')) continue;
    if (name === 'Tabs') {
      if (closing) {
        const group = stack.pop();
        if (!group) errors.push(`line ${line + 1}: a closing </Tabs> without an opening <Tabs>`);
      } else {
        const id = attribute(tag, 'groupId');
        const group = {line, groupId: id.value, groupIdPresent: id.present, items: []};
        groups.push(group);
        stack.push(group);
      }
    } else if (closing) {
      const item = open.pop();
      if (item) item.end = line;
    } else {
      const label = attribute(tag, 'label');
      const item = {line, end: lastLine, label: label.value, labelPresent: label.present};
      open.push(item);
      const group = stack[stack.length - 1];
      if (group) group.items.push(item);
      else errors.push(`line ${line + 1}: a <TabItem> outside <Tabs>`);
    }
  }
  for (const group of stack) errors.push(`line ${group.line + 1}: this <Tabs> is never closed`);
  return {groups, errors};
}
