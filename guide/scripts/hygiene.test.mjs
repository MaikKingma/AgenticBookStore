import {test} from 'node:test';
import assert from 'node:assert/strict';
import {asciiErrors, labErrors, dependencyErrors} from './rules/hygiene.mjs';
import {goodPackage} from './lib/test-fixtures.mjs';

// Forbidden characters are built from code points so that this file stays ASCII.
const EM_DASH = String.fromCodePoint(0x2014);
const NBSP = String.fromCodePoint(0x00a0);
const RIGHT_QUOTE = String.fromCodePoint(0x2019);

test('ascii: plain ASCII text passes', () => {
  assert.deepEqual(asciiErrors('docs/index.md', 'Plain text, with `code` and a tab\there.\n'), []);
});

test('ascii: an em dash fails with the dedicated message, file, line and column', () => {
  const errors = asciiErrors('docs/index.md', `first line\nsecond ${EM_DASH} line\n`);
  assert.equal(errors.length, 1);
  assert.match(errors[0], /^ascii: docs\/index\.md:2:8 /);
  assert.match(errors[0], /em dash/);
  assert.doesNotMatch(errors[0], /non-ASCII character/);
});

test('ascii: a no-break space and a curly quote fail with the generic message', () => {
  const nbsp = asciiErrors('docs/index.md', `a${NBSP}b\n`);
  assert.equal(nbsp.length, 1);
  assert.match(nbsp[0], /non-ASCII character U\+00A0/);
  const quote = asciiErrors('docs/index.md', `it${RIGHT_QUOTE}s\n`);
  assert.equal(quote.length, 1);
  assert.match(quote[0], /non-ASCII character U\+2019/);
});

test('ascii: an em dash as the last byte of a file without a final newline fails', () => {
  const errors = asciiErrors('README.md', `text ${EM_DASH}`);
  assert.equal(errors.length, 1);
  assert.match(errors[0], /^ascii: README\.md:1:6 /);
});

test('ascii: every offending character on a line is reported', () => {
  const errors = asciiErrors('a.md', `${EM_DASH} and ${NBSP}\n`);
  assert.equal(errors.length, 2);
});

test('ascii: a non-BMP character is reported once with its full code point', () => {
  const errors = asciiErrors('a.md', `x${String.fromCodePoint(0x1f600)}y\n`);
  assert.equal(errors.length, 1);
  assert.match(errors[0], /U\+1F600/);
});

test('lab: the word in any casing and punctuation context fails', () => {
  for (const text of ['Lab 1', 'LABS', 'the lab.', 'see lab-1', 'a Labs page', '(lab)']) {
    assert.equal(labErrors('docs/index.md', text).length, 1, text);
  }
});

test('lab: look-alike words pass', () => {
  for (const text of ['label', 'collaborate', 'syllabus', 'laboratory', 'Elaborate', 'labelled']) {
    assert.deepEqual(labErrors('docs/index.md', text), [], text);
  }
});

test('lab: the message names the file and the line', () => {
  const errors = labErrors('docs/index.md', 'fine\nGo to Lab 2 now\n');
  assert.equal(errors.length, 1);
  assert.match(errors[0], /^lab: docs\/index\.md:2:7 /);
});

test('dependencies: the template set with exact Docusaurus pins passes', () => {
  assert.deepEqual(dependencyErrors(goodPackage()), []);
});

test('dependencies: an extra dependency key fails', () => {
  const pkg = goodPackage();
  pkg.dependencies['left-pad'] = '1.0.0';
  const errors = dependencyErrors(pkg);
  assert.equal(errors.length, 1);
  assert.match(errors[0], /dependencies: unexpected key "left-pad"/);
});

test('dependencies: an extra devDependency key fails', () => {
  const pkg = goodPackage();
  pkg.devDependencies['left-pad'] = '1.0.0';
  const errors = dependencyErrors(pkg);
  assert.equal(errors.length, 1);
  assert.match(errors[0], /devDependencies: unexpected key "left-pad"/);
});

test('dependencies: a removed template key fails', () => {
  const pkg = goodPackage();
  delete pkg.dependencies.clsx;
  const errors = dependencyErrors(pkg);
  assert.equal(errors.length, 1);
  assert.match(errors[0], /dependencies: missing key "clsx"/);
});

test('dependencies: a key moved between sections fails twice', () => {
  const pkg = goodPackage();
  pkg.devDependencies.clsx = pkg.dependencies.clsx;
  delete pkg.dependencies.clsx;
  assert.equal(dependencyErrors(pkg).length, 2);
});

test('dependencies: a range on a Docusaurus package fails (exact pin)', () => {
  const pkg = goodPackage();
  pkg.devDependencies['@docusaurus/types'] = '^3.10.2';
  const errors = dependencyErrors(pkg);
  assert.equal(errors.length, 1);
  assert.match(errors[0], /"@docusaurus\/types" must be exactly 3\.10\.2/);
});

test('dependencies: a different exact Docusaurus version fails', () => {
  const pkg = goodPackage();
  pkg.dependencies['@docusaurus/core'] = '3.10.1';
  assert.equal(dependencyErrors(pkg).length, 1);
});

test('dependencies: dependency sections other than the two allowed ones fail', () => {
  for (const section of ['optionalDependencies', 'peerDependencies', 'overrides', 'resolutions']) {
    const pkg = goodPackage();
    pkg[section] = {'left-pad': '1.0.0'};
    const errors = dependencyErrors(pkg);
    assert.equal(errors.length, 1, section);
    assert.match(errors[0], new RegExp(section));
  }
});

test('dependencies: a missing devDependencies section reports every key as missing', () => {
  const pkg = goodPackage();
  delete pkg.devDependencies;
  assert.equal(dependencyErrors(pkg).length, 5);
});
