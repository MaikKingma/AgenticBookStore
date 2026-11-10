// Small text helpers shared by the rule modules.

// "line:column" (both 1-based) of an offset in a text.
export function position(text, index) {
  const before = text.slice(0, index);
  const line = before.split('\n').length;
  return `${line}:${index - (before.lastIndexOf('\n') + 1) + 1}`;
}

// Replaces every match with spaces of the same length, newlines kept, so offsets
// into the result are offsets into the original.
export function blank(text, regex) {
  return text.replace(regex, (m) => m.replace(/[^\n]/g, ' '));
}

// The text with its /* */ comments blanked out.
export function withoutBlockComments(text) {
  return blank(text, /\/\*[\s\S]*?\*\//g);
}

// The index of the brace that closes the one at `open`, skipping string literals.
// Returns text.length when it never closes.
export function matchingBrace(text, open) {
  let depth = 0;
  for (let i = open; i < text.length; i++) {
    const ch = text[i];
    if (ch === '"' || ch === "'" || ch === '`') {
      for (i++; i < text.length && text[i] !== ch; i++) if (text[i] === '\\') i++;
    } else if (ch === '{') {
      depth++;
    } else if (ch === '}' && --depth === 0) {
      return i;
    }
  }
  return text.length;
}
