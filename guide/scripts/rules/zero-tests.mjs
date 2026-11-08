// The zero-test guard. `node --test` exits 0 when it runs nothing: a glob that matches
// no file prints "tests 0", and an emptied test file counts as one passing "test"
// named after the file. The `check` script of package.json chains the checker after
// node:test with `&&`, so the checker is the place that can notice: it reads the
// patterns of the `node --test` command, finds the files they match and requires at
// least one registered test case. Pure functions only; check.mjs does the file reads.

// The patterns that follow `node --test` in a package.json script, up to the first &&.
export function testGlobs(script) {
  if (typeof script !== 'string') return [];
  const command = /\bnode\s+--test\b([^&]*)/.exec(script);
  if (!command) return [];
  const patterns = [];
  for (const token of command[1].matchAll(/"([^"]*)"|'([^']*)'|(\S+)/g)) {
    const value = token[1] ?? token[2] ?? token[3];
    if (!value.startsWith('--')) patterns.push(value);
  }
  return patterns;
}

// How many test cases a test file registers: test( and it( at the start of a line.
// test.skip, test.todo, comments and mid-line mentions do not count.
export function countTestCases(text) {
  return (text.match(/^[ \t]*(?:test|it)\s*\(/gm) ?? []).length;
}

const plural = (count, word) => `${count} ${word}${count === 1 ? '' : 's'}`;

// script: the `check` script text; matched: pattern -> [{file, text}] of the files
// that pattern matches. Returns the error lines.
export function zeroTestErrors({script, matched}) {
  const patterns = testGlobs(script);
  if (patterns.length === 0) {
    return ['zero-test guard: package.json has no check script with a node --test command'];
  }
  const errors = [];
  const files = new Map();
  for (const pattern of patterns) {
    const found = matched[pattern] ?? [];
    if (found.length === 0) {
      errors.push(`zero-test guard: the node --test pattern "${pattern}" matches no test file`);
    }
    for (const entry of found) files.set(entry.file, entry.text);
  }
  if (errors.length > 0) return errors;
  const cases = [...files.values()].reduce((sum, text) => sum + countTestCases(text), 0);
  if (cases === 0) {
    return [
      `zero-test guard: ${plural(files.size, 'test file')} ${files.size === 1 ? 'matches' : 'match'} but none registers a test case, so node:test would execute nothing`,
    ];
  }
  return [];
}
