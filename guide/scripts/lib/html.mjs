// A small HTML scanner for the built pages, Node built-ins only. It reads the
// minified output of the Docusaurus build (unquoted attributes, void tags written
// with a trailing slash) and finds checkboxes that have no accessible name.
// It is a scanner, not a parser: it does not build a tree and it does not decode
// entities, which is enough to tell "has a name" from "has none".

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

export function lineOf(html, offset) {
  let line = 1;
  for (let i = html.indexOf('\n'); i !== -1 && i < offset; i = html.indexOf('\n', i + 1)) line++;
  return line;
}
