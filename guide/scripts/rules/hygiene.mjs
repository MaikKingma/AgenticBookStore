// Hygiene rules. Each rule is a pure function from text (or parsed JSON) to a list
// of error strings. The forbidden characters are built from code points on purpose:
// the Write and Edit tools decode a typed backslash-u escape into the literal
// character, which would put a forbidden character into this file.

const EM_DASH = 0x2014;

// Every character outside the 7-bit ASCII range is an error. The em dash gets its
// own message because it is the one that sneaks in most often.
export function asciiErrors(file, text) {
  const errors = [];
  const lines = text.split('\n');
  lines.forEach((line, lineIndex) => {
    for (let i = 0; i < line.length; ) {
      const codePoint = line.codePointAt(i);
      if (codePoint > 0x7f) {
        const where = `${file}:${lineIndex + 1}:${i + 1}`;
        const hex = codePoint.toString(16).toUpperCase().padStart(4, '0');
        errors.push(
          codePoint === EM_DASH
            ? `ascii: ${where} em dash (U+${hex}); use a comma, a colon or a hyphen instead`
            : `ascii: ${where} non-ASCII character U+${hex}`,
        );
      }
      i += codePoint > 0xffff ? 2 : 1;
    }
  });
  return errors;
}

// The workshop guide never uses the word that the workshop levels replaced.
// Word boundaries keep label, collaborate, syllabus and laboratory legal.
const FORBIDDEN_WORD = /\blabs?\b/gi;

export function labErrors(file, text) {
  const errors = [];
  text.split('\n').forEach((line, lineIndex) => {
    for (const match of line.matchAll(FORBIDDEN_WORD)) {
      errors.push(
        `lab: ${file}:${lineIndex + 1}:${match.index + 1} forbidden word "${match[0]}"; the guide says Level, never the old term`,
      );
    }
  });
  return errors;
}

// The dependency allowlist: exactly the keys of the official create-docusaurus
// classic TypeScript template, and the six @docusaurus packages pinned exactly.
const DOCUSAURUS_PIN = '3.10.2';

const ALLOWED = {
  dependencies: [
    '@docusaurus/core',
    '@docusaurus/faster',
    '@docusaurus/preset-classic',
    '@mdx-js/react',
    'clsx',
    'prism-react-renderer',
    'react',
    'react-dom',
  ],
  devDependencies: [
    '@docusaurus/module-type-aliases',
    '@docusaurus/tsconfig',
    '@docusaurus/types',
    '@types/react',
    'typescript',
  ],
};

const FORBIDDEN_SECTIONS = ['optionalDependencies', 'peerDependencies', 'overrides', 'resolutions'];

export function dependencyErrors(pkg) {
  const errors = [];
  for (const [section, allowed] of Object.entries(ALLOWED)) {
    const actual = Object.keys(pkg[section] ?? {});
    for (const key of actual.filter((k) => !allowed.includes(k))) {
      errors.push(`dependencies: ${section}: unexpected key "${key}"; new dependencies are not allowed`);
    }
    for (const key of allowed.filter((k) => !actual.includes(k))) {
      errors.push(`dependencies: ${section}: missing key "${key}"`);
    }
  }
  for (const section of Object.keys(ALLOWED)) {
    for (const [key, version] of Object.entries(pkg[section] ?? {})) {
      if (key.startsWith('@docusaurus/') && version !== DOCUSAURUS_PIN) {
        errors.push(`dependencies: "${key}" must be exactly ${DOCUSAURUS_PIN} (found ${version})`);
      }
    }
  }
  for (const section of FORBIDDEN_SECTIONS.filter((s) => s in pkg)) {
    errors.push(`dependencies: section "${section}" is not allowed in package.json`);
  }
  return errors;
}
