// A small HTML scanner for the built pages, Node built-ins only. It reads the
// minified output of the Docusaurus build (unquoted attributes, void tags written
// with a trailing slash) and finds checkboxes that have no accessible name.
// It is a scanner, not a parser: the checkbox check needs no tree and no decoded
// entities, which is enough to tell "has a name" from "has none". elementTree builds
// a light tree on top of the scanner (with decoded text) for the PowerShell rule.

const RAW_TEXT = new Set(['script', 'style']);

// Reads the attributes of a start tag from index i (just after the tag name).
// Returns {attrs, end} with end just past the closing ">". Attribute names are
// lower-cased, a value-less attribute has the value "", quotes are honoured so a
// ">" inside a quoted value does not end the tag.
function readAttributes(html, start) {
  const attrs = new Map();
  let i = start;
  const n = html.length;
  while (i < n) {
    while (i < n && /[\s/]/.test(html[i])) i++;
    if (i >= n) break;
    if (html[i] === '>') return {attrs, end: i + 1};
    const nameStart = i;
    while (i < n && !/[\s=/>]/.test(html[i])) i++;
    if (i === nameStart) {
      i++;
      continue;
    }
    const name = html.slice(nameStart, i).toLowerCase();
    let j = i;
    while (j < n && /\s/.test(html[j])) j++;
    let value = '';
    if (html[j] === '=') {
      j++;
      while (j < n && /\s/.test(html[j])) j++;
      if (html[j] === '"' || html[j] === "'") {
        const close = html.indexOf(html[j], j + 1);
        const stop = close === -1 ? n : close;
        value = html.slice(j + 1, stop);
        j = close === -1 ? n : close + 1;
      } else {
        const valueStart = j;
        while (j < n && !/[\s>]/.test(html[j])) j++;
        value = html.slice(valueStart, j);
        // "type=checkbox/>": the slash belongs to the tag, not to the value.
        if (html[j] === '>' && value.endsWith('/')) value = value.slice(0, -1);
      }
      i = j;
    }
    if (!attrs.has(name)) attrs.set(name, value);
  }
  return {attrs, end: n};
}

// Events in document order: {kind: 'tag', name, closing, attrs, offset} and
// {kind: 'text', value}. Comments are skipped, and so is the content of script and
// style elements.
export function scanHtml(html) {
  const events = [];
  const n = html.length;
  let i = 0;
  while (i < n) {
    const lt = html.indexOf('<', i);
    if (lt === -1) {
      events.push({kind: 'text', value: html.slice(i)});
      break;
    }
    if (lt > i) events.push({kind: 'text', value: html.slice(i, lt)});
    if (html.startsWith('<!--', lt)) {
      const end = html.indexOf('-->', lt + 4);
      i = end === -1 ? n : end + 3;
      continue;
    }
    const closing = html[lt + 1] === '/';
    const nameStart = lt + (closing ? 2 : 1);
    const nameMatch = /^[A-Za-z][^\s/>]*/.exec(html.slice(nameStart, nameStart + 64));
    if (!nameMatch) {
      events.push({kind: 'text', value: '<'});
      i = lt + 1;
      continue;
    }
    const name = nameMatch[0].toLowerCase();
    const {attrs, end} = readAttributes(html, nameStart + nameMatch[0].length);
    events.push({kind: 'tag', name, closing, attrs, offset: lt});
    i = end;
    if (!closing && RAW_TEXT.has(name)) {
      const close = html.toLowerCase().indexOf(`</${name}`, i);
      i = close === -1 ? n : close;
    }
  }
  return events;
}

const isCheckbox = (event) =>
  event.kind === 'tag' && !event.closing && event.name === 'input' && (event.attrs.get('type') ?? '').trim().toLowerCase() === 'checkbox';

// Every checkbox in the page as {offset, named}. A checkbox is named by a non-blank
// aria-label, by an aria-labelledby whose ids are all on the page, by a label with
// text that points at its id (for) or wraps it. Whether the text behind a
// labelledby id is non-empty is not checked.
export function checkboxes(html) {
  const events = scanHtml(html);
  const ids = new Set();
  for (const event of events) {
    if (event.kind === 'tag' && !event.closing && event.attrs.has('id')) ids.add(event.attrs.get('id'));
  }
  const labelledIds = new Set();
  const found = [];
  let label = null;
  const closeLabel = () => {
    if (label && label.text.trim() !== '') {
      if (label.attrs.has('for')) labelledIds.add(label.attrs.get('for'));
      for (const box of label.boxes) box.wrapped = true;
    }
    label = null;
  };
  for (const event of events) {
    if (event.kind === 'text') {
      if (label) label.text += event.value;
    } else if (event.name === 'label') {
      if (event.closing) closeLabel();
      else {
        closeLabel();
        label = {attrs: event.attrs, text: '', boxes: []};
      }
    } else if (isCheckbox(event)) {
      const box = {offset: event.offset, attrs: event.attrs, wrapped: false};
      found.push(box);
      if (label) label.boxes.push(box);
    }
  }
  closeLabel();
  return found.map(({offset, attrs, wrapped}) => {
    const labelledBy = (attrs.get('aria-labelledby') ?? '').trim().split(/\s+/).filter(Boolean);
    const named =
      (attrs.get('aria-label') ?? '').trim() !== '' ||
      (labelledBy.length > 0 && labelledBy.every((id) => ids.has(id))) ||
      wrapped ||
      (attrs.has('id') && labelledIds.has(attrs.get('id')));
    return {offset, named};
  });
}

// The named character references the build writes, and numeric ones in decimal or hex. Any
// other reference stays as written, so text holding it cannot equal a plain line by accident.
const NAMED = {amp: '&', lt: '<', gt: '>', quot: '"', apos: "'", nbsp: String.fromCharCode(0xa0)};

export function decodeEntities(text) {
  return text.replace(/&(?:#[xX]([0-9a-fA-F]+)|#([0-9]+)|([A-Za-z][A-Za-z0-9]*));/g, (whole, hex, dec, name) => {
    if (name !== undefined) return Object.hasOwn(NAMED, name) ? NAMED[name] : whole;
    const code = hex !== undefined ? parseInt(hex, 16) : parseInt(dec, 10);
    return code > 0 && code <= 0x10ffff ? String.fromCodePoint(code) : whole;
  });
}

// The element tree of a page (for the rules that need to know what is inside what):
// {name, attrs, offset, parent, children}, the root has the name "#root"; a text child is
// {text}, with its character references decoded. An end tag closes the nearest open element
// of its name and everything opened inside it (the minifier drops optional end tags such as
// </p>); an end tag with no open element of its name is ignored. A new <li> closes an open
// <li> of the same list, so the tabs of a tab list (<li role=tab>, written without </li>)
// are siblings. A void element (<br/>, <img>) stays open until its parent closes: it holds
// the text that follows it, which changes no text content and no ancestor of interest.
export function elementTree(html) {
  const root = {name: '#root', attrs: new Map(), offset: 0, parent: null, children: []};
  const open = [root];
  const top = () => open[open.length - 1];
  for (const event of scanHtml(html)) {
    if (event.kind === 'text') {
      top().children.push({text: decodeEntities(event.value)});
      continue;
    }
    if (event.closing) {
      const at = open.findLastIndex((node) => node.name === event.name);
      if (at > 0) open.length = at;
      continue;
    }
    if (event.name === 'li') {
      const list = open.findLastIndex((node) => node.name === 'ul' || node.name === 'ol');
      const item = open.findLastIndex((node) => node.name === 'li');
      if (item > list) open.length = item;
    }
    const node = {name: event.name, attrs: event.attrs, offset: event.offset, parent: top(), children: []};
    top().children.push(node);
    open.push(node);
  }
  return root;
}

// The text content of a node as the DOM gives it: the text of every descendant.
export function textContent(node) {
  if ('text' in node) return node.text;
  return node.children.map(textContent).join('');
}

export function lineOf(html, offset) {
  let line = 1;
  for (let i = html.indexOf('\n'); i !== -1 && i < offset; i = html.indexOf('\n', i + 1)) line++;
  return line;
}
