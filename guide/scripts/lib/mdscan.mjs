// A small scanner for the Markdown and MDX of the guide docs and of the pack files, shared
// by the coverage and content rules. It knows the things the rules need and a regular
// expression over the whole text gets wrong: fenced code blocks (also in a block quote or a
// list item), MDX and HTML comments (a commented-out fence is not a fence, a comment inside a
// fence is code), inline code spans and the paragraphs they live in, table cells, backslash
// escapes, text expressions and the Tabs and TabItem elements. It is no MDX parser: where it is
// unsure it ends a paragraph too early rather than join two blocks. Pure functions on text.

const FENCE_OPEN = /^\s*(`{3,}|~{3,})(.*)$/;
// The two comment forms of MDX pages: a JSX comment (spaces inside the braces are allowed,
// they are the same JavaScript) and an HTML comment.
const COMMENTS = [
  {open: /\{\s*\/\*/, close: /\*\/\s*\}/},
  {open: /<!--/, close: /-->/},
];

// The run of backticks of exactly the same length that closes an inline code span. A closing
// run is never escaped: a backslash inside a code span is text.
const closerOf = (length) => new RegExp(`(?<!\`)\`{${length}}(?!\`)`);

const spaces = (text) => text.replace(/[^\n]/g, ' ');

// Whitespace runs become one space and the ends are trimmed.
export const collapse = (text) => text.replace(/\s+/g, ' ').trim();

// The number of backslashes right before an offset: an odd number escapes the character there.
function backslashesBefore(text, index) {
  let count = 0;
  while (text[index - count - 1] === '\\') count++;
  return count;
}

// The first match of `pattern` in `text` that no odd number of backslashes escapes, or null: an
// escaped comment opener (`\{/*`) is text.
function firstUnescaped(pattern, text) {
  for (const m of text.matchAll(new RegExp(pattern.source, 'g'))) if (backslashesBefore(text, m.index) % 2 === 0) return m;
  return null;
}

// The offset after a JavaScript string literal (quote, double quote or backtick) that opens at
// `index`, or -1 when it does not close on this text. A backslash escapes the next character.
function stringEnd(text, index) {
  let k = index + 1;
  while (k < text.length && text[k] !== text[index]) k += text[k] === '\\' ? 2 : 1;
  return k < text.length ? k + 1 : -1;
}

// The first text expression of a line that closes on that line: an unescaped `{` up to its
// matching `}` (a brace in a string literal does not count). Returns {index, end} or null. MDX
// renders the value of the expression, so a comment opener or a backtick in its strings is text,
// never a comment or a code span. A brace that does not close on its line opens nothing here,
// and a comment opener at the same place is a comment (the caller takes it first).
function lineExpression(text) {
  for (const m of text.matchAll(/\{/g)) {
    if (backslashesBefore(text, m.index) % 2 === 1) continue;
    let depth = 0;
    for (let j = m.index; j < text.length; j++) {
      if (`'"\``.includes(text[j])) {
        const end = stringEnd(text, j);
        if (end === -1) break;
        j = end - 1;
      } else if (text[j] === '{') depth++;
      else if (text[j] === '}' && --depth === 0) return {index: m.index, end: j + 1};
    }
  }
  return null;
}

// The first run of backticks that can open an inline code span: {index, length} or null. A run
// is a whole run of backticks; when an odd number of backslashes escapes its first backtick, the
// rest of the run can still open one.
function openingRun(text) {
  for (const m of text.matchAll(/`+/g)) {
    if (backslashesBefore(text, m.index) % 2 === 0) return {index: m.index, length: m[0].length};
    if (m[0].length > 1) return {index: m.index + 1, length: m[0].length - 1};
  }
  return null;
}

// The opening fence of a fenced block on a line, after the line's container prefix
// (indentation, block quote and list markers), or null. A backtick fence whose info string holds
// a backtick is no fence (inline code on the line). The main loop and the paragraph model below
// use this one test.
function fenceOpener(line) {
  const prefix = CONTAINER.exec(line)[0];
  const open = FENCE_OPEN.exec(line.slice(prefix.length));
  return open && !(open[1][0] === '`' && open[2].includes('`')) ? {char: open[1][0], length: open[1].length, info: open[2], prefix} : null;
}

// A line inside a fenced block that opened after the container prefix `prefix`: the line without
// its container part, or null when the line is outside one of the containers, which ends the
// block. Each block quote marker of the prefix needs a `>` again, each list marker its width as
// indentation (a blank line stays in a list item). A prefix of indentation only has no container.
function containerRest(prefix, line) {
  let open = prefix;
  let rest = line;
  while (open.trim() !== '') {
    const quote = /^\s*>\s?/.exec(open);
    if (quote) {
      const marker = /^\s*>\s?/.exec(rest);
      if (!marker) return null;
      rest = rest.slice(marker[0].length);
      open = open.slice(quote[0].length);
      continue;
    }
    const item = /^\s*(?:[-+*]|\d{1,9}[.)])\s*/.exec(open);
    if (rest.trim() !== '') {
      if (/^\s*/.exec(rest)[0].length < item[0].length) return null;
      rest = rest.slice(item[0].length);
    }
    open = open.slice(item[0].length);
  }
  return rest;
}

// The paragraph model of inline code spans: a span runs over a line break only inside one
// paragraph, as MDX parses it. The next line continues the paragraph unless it starts a block of
// its own, tested after its block quote markers and indentation: a blank line, a fence, an ATX
// heading, a setext underline or thematic break, a deeper block quote, a list marker, a table row
// (a leading pipe, a delimiter row or the header row above one), a JSX or HTML tag, an expression
// or comment, an admonition line. A heading, table row, tag, expression or admonition line holds
// a span of its own line only. Ending a paragraph too early only loses a span; joining two blocks
// into one span must never happen.
const CONTAINER = /^(?:\s*(?:>|(?:[-+*]|\d{1,9}[.)])(?=\s|$)))*\s*/;
const QUOTE_MARKERS = /^(?:\s*>)*/;
const ATX_HEADING = /^#{1,6}(?:\s|$)/;
const SETEXT_OR_BREAK = /^(?:=+|-+|(?:\*\s*){3,}|(?:-\s*){3,}|(?:_\s*){3,})\s*$/;
const LIST_MARKER = /^(?:[-+*]|\d{1,9}[.)])(?:\s|$)/;
const OWN_BLOCK = /^(?:[|<{]|:::)/;

const quoteDepth = (line) => (QUOTE_MARKERS.exec(line)[0].match(/>/g) ?? []).length;
// A line without its container prefix (indentation, block quote and list markers).
const contentOf = (line) => line.slice(CONTAINER.exec(line)[0].length);

// A GFM table delimiter row: cells of dashes with optional colons, at least one pipe.
function isDelimiterRow(line = '') {
  const content = contentOf(line).trim();
  if (!content.includes('|') || !content.includes('-')) return false;
  const cells = content.split('|');
  return cells.every((cell, i) => /^\s*:?-+:?\s*$/.test(cell) || (cell.trim() === '' && (i === 0 || i === cells.length - 1)));
}

// Does line `index` of `all` continue a paragraph whose span opened on a line with `depth`
// block quote markers? A line with fewer markers is a lazy continuation, one with more starts a
// deeper quote.
function continuesParagraph(all, index, depth) {
  const line = all[index];
  // A delimiter row itself is never reached: the line above it is a header row (or the line
  // that opened the span, a table row), and the walk stops there.
  if (fenceOpener(line) || quoteDepth(line) > depth || isDelimiterRow(all[index + 1])) return false;
  const rest = line.slice(QUOTE_MARKERS.exec(line)[0].length).trimStart();
  return !(rest === '' || ATX_HEADING.test(rest) || SETEXT_OR_BREAK.test(rest) || LIST_MARKER.test(rest) || OWN_BLOCK.test(rest));
}

// A line whose block holds that line only, so a span opened on it closes on it or not at all.
function singleLine(line) {
  const content = contentOf(line);
  return ATX_HEADING.test(content) || OWN_BLOCK.test(content);
}

// In a table row a code span closes inside its cell: the text up to the next pipe that no odd
// number of backslashes escapes. Elsewhere the rest of the line.
function cellOf(text, row) {
  if (!row) return text;
  for (const m of text.matchAll(/\|/g)) if (backslashesBefore(text, m.index) % 2 === 0) return text.slice(0, m.index);
  return text;
}

// scanMarkdown(text) returns {lines, blocks, spans}.
//   lines: one {text, fence} per line of the text. fence is null for prose, 'open' or
//     'close' for the fence lines and 'body' for the lines in between. The text of a prose
//     line has its comments blanked with spaces (offsets stay valid); fenced lines are raw.
//     A comment opener inside an inline code span (`{/*`) is code, not a comment, also when
//     the span runs over a line break; an escaped one (`\{/*`) or one in a string of an
//     expression that closes on its line (`{'{/*'}`) is text. On the continuation line of a
//     span the block quote markers are blanked: they are no span content.
//   blocks: one {lang, open, close, body} per fenced block: the 0-based line numbers of the
//     two fence lines (close stays the opening line when the fence never closes, and is the
//     last line of the block when the block quote or list item around the fence ends first)
//     and the body lines, as written but without the block quote and list markers of the
//     fence's container.
//   spans: one [start, end] per inline code span, the offsets in proseOf(scan) of its
//     content (between the backtick runs). This is the one code span recogniser: the
//     comment blanking above, the tag reading of tabGroups and the code of codeOf all
//     use it, so they cannot disagree about what is code.
export function scanMarkdown(text) {
  const lines = [];
  const blocks = [];
  const spans = [];
  const all = text.replace(/\r\n/g, '\n').split('\n');
  let row = false; // the current line is a table row
  let table = false; // the lines since a table delimiter row are table rows, up to a blank line
  // Does a span opened on the current line `at` with `length` backticks close later in the same paragraph?
  const spanContinues = (at, length) => {
    if (row || singleLine(all[at])) return false;
    const depth = quoteDepth(all[at]);
    for (let next = at + 1; next < all.length && continuesParagraph(all, next, depth); next++) {
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
    if (raw.trim() === '') {
      carry = 0;
      table = false;
    }
    if (fence) {
      const body = containerRest(fence.prefix, raw);
      if (body !== null) {
        const closes = new RegExp(`^\\s*${fence.char}{${fence.length},}\\s*$`).test(body);
        lines.push({text: raw, fence: closes ? 'close' : 'body'});
        if (closes) {
          fence.block.close = index;
          fence = null;
        } else {
          fence.block.body.push(body);
        }
        continue;
      }
      // The line is outside the block quote or list item that holds the fence: the block ended
      // on the line before, and this line is read like any other.
      fence.block.close = index - 1;
      fence = null;
    }
    // On the continuation line of a span the block quote markers are no span content.
    let rest = carry > 0 ? raw.replace(QUOTE_MARKERS, (markers) => markers.replace(/>/g, ' ')) : raw;
    let prose = '';
    if (closer === null) {
      const open = fenceOpener(raw);
      if (open) {
        const block = {lang: open.info.trim().split(/\s+/)[0] ?? '', open: index, close: index, body: []};
        blocks.push(block);
        fence = {char: open.char, length: open.length, prefix: open.prefix, block};
        table = false;
        lines.push({text: raw, fence: 'open'});
        continue;
      }
    }
    row = table || contentOf(raw).startsWith('|') || isDelimiterRow(all[index + 1]);
    if (isDelimiterRow(raw)) table = true;
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
        const found = firstUnescaped(comment.open, rest);
        if (found && (first === null || found.index < first.index)) first = {index: found.index, length: found[0].length, close: comment.close};
      }
      const run = openingRun(rest);
      // An expression that closes on this line, before the first comment opener and the first
      // span, is its value: copy it through, its strings may hold anything.
      const expression = lineExpression(rest);
      if (expression && (first === null || expression.index < first.index) && (run === null || expression.index < run.index)) {
        prose += rest.slice(0, expression.end);
        rest = rest.slice(expression.end);
        continue;
      }
      // An inline code span that starts before the first comment opener is code: copy it through.
      if (run && (first === null || run.index < first.index)) {
        const length = run.length;
        const after = run.index + length;
        const end = closerOf(length).exec(cellOf(rest.slice(after), row));
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

// The markup as the tag reader sees it: markupOf, with what MDX renders as text although it
// looks like a tag blanked too: a `<` after an odd number of backslashes (`\</TabItem>`) and the
// content of the string literals of a text expression (`{'</TabItem>'}`). A tag inside an
// expression, outside its strings, stays readable, and nothing inside a tag is touched (an
// attribute expression such as label={name}). Offsets and line numbers still match the file.
function tagMarkupOf(scan) {
  const text = markupOf(scan);
  const chars = text.split('');
  const startsTag = (i) => text[i] === '<' && /[A-Za-z/]/.test(text[i + 1] ?? '');
  // The offset after the tag that starts at i: its first `>` outside quotes and braces.
  const tagEnd = (i) => {
    let quote = null;
    let depth = 0;
    for (let j = i + 1; j < text.length; j++) {
      const char = text[j];
      if (quote !== null) {
        if (char === quote) quote = null;
      } else if (char === '"' || char === "'") quote = char;
      else if (char === '{') depth++;
      else if (char === '}') depth = Math.max(0, depth - 1);
      else if (char === '>' && depth === 0) return j + 1;
    }
    return text.length;
  };
  // The offset after the expression whose `{` is at i, blanking the content of its string literals
  // (quoted or template; a string ends at its closing quote or at the end of its line).
  const expressionEnd = (i) => {
    let depth = 1;
    for (let j = i + 1; j < text.length; j++) {
      const char = text[j];
      if (char === '"' || char === "'" || char === '`') {
        let k = j + 1;
        while (k < text.length && text[k] !== char && text[k] !== '\n') k += text[k] === '\\' ? 2 : 1;
        for (let b = j + 1; b < Math.min(k, text.length); b++) if (chars[b] !== '\n') chars[b] = ' ';
        j = k;
      } else if (startsTag(j)) j = tagEnd(j) - 1;
      else if (char === '{') depth++;
      else if (char === '}' && --depth === 0) return j + 1;
    }
    return text.length;
  };
  for (let i = 0; i < text.length; i++) {
    const escaped = backslashesBefore(text, i) % 2 === 1;
    if (text[i] === '<' && escaped) chars[i] = ' ';
    else if (startsTag(i)) i = tagEnd(i) - 1;
    else if (text[i] === '{' && !escaped) i = expressionEnd(i) - 1;
  }
  return chars.join('');
}

// A raw HTML code element in the markup: <code ...>...</code> or <pre ...>...</pre>, any
// letter case, over line breaks; group 2 is its content. codeOf (rule e) and the PowerShell
// rule (c) read the elements with this one pattern.
export const HTML_CODE = /<(code|pre)(?=[\s>])[^>]*>([\s\S]*?)<\/\1\s*>/gi;

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
// are read in the markup only: never in a fenced block, a comment or an inline code span,
// and never where MDX renders them as text (tagMarkupOf).
export function tabGroups(scan) {
  const prose = tagMarkupOf(scan);
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
