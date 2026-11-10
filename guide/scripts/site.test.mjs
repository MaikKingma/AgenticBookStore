// runChecks-level tests of the contrast, brand and font rules: they prove that the
// rules are wired into the checker, so a rule that stops reporting makes a test fail.
// Every test works on a temp copy of the real brand files with one edit, never on the
// real stylesheet itself.
import {test} from 'node:test';
import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
import {runChecks} from './check.mjs';
import {CONTRAST_PAIRS} from './rules/contrast-pairs.mjs';
import {goodFontFiles, goodSiteFiles, replaceOnce, withSiteTree} from './lib/test-fixtures.mjs';

const site = goodSiteFiles();
const CSS = site['src/css/custom.css'];
const CONFIG = site['docusaurus.config.ts'];
const README = site['README.md'];

const SWITCH_ON = 'respectPrefersColorScheme: true,';
const SWITCH_OFF = 'disableSwitch: true,\n      respectPrefersColorScheme: false,';
const DARK_SELECTOR = "html[data-theme='dark']";

const startingWith = (list, prefix) => list.filter((line) => line.startsWith(prefix));

test('runChecks: the real brand files pass the contrast, brand and font rules', () => {
  withSiteTree({}, (root) => {
    const {lines, errors} = runChecks(root);
    assert.deepEqual(errors, []);
    for (const rule of ['contrast', 'brand', 'fonts']) {
      assert.equal(startingWith(lines, `ok   ${rule} (`).length, 1, `${rule}: ${lines.join(' | ')}`);
    }
  });
});

test('runChecks: every contrast pair is printed with its ratio, light and dark', () => {
  withSiteTree({}, (root) => {
    const {lines} = runChecks(root);
    assert.ok(
      lines.includes('ok   contrast light body text: --ifm-font-color-base on --ifm-background-color 15.50 (min 4.5)'),
      lines.join('\n'),
    );
    assert.ok(
      lines.includes('ok   contrast dark body text: --ifm-font-color-base on --ifm-background-color 15.50 (min 4.5)'),
      lines.join('\n'),
    );
    assert.equal(startingWith(lines, 'ok   contrast light ').length, CONTRAST_PAIRS.length);
    assert.equal(startingWith(lines, 'ok   contrast dark ').length, CONTRAST_PAIRS.length);
    assert.ok(lines.includes(`ok   contrast (${CONTRAST_PAIRS.length * 2} pair checks, light and dark)`), lines.join('\n'));
  });
});

test('runChecks: a dark body text colour that fails the contrast is an error and a FAIL line', () => {
  const css = replaceOnce(CSS, '--ifm-font-color-base: var(--ov-white);', '--ifm-font-color-base: var(--ov-navy);');
  withSiteTree({'src/css/custom.css': css}, (root) => {
    const {lines, errors} = runChecks(root);
    assert.deepEqual(startingWith(errors, 'contrast: dark body text:'), [
      'contrast: dark body text: --ifm-font-color-base on --ifm-background-color is 1.00:1, below 4.5',
    ]);
    assert.ok(lines.includes('FAIL contrast dark body text: --ifm-font-color-base on --ifm-background-color 1.00 (min 4.5)'));
    assert.equal(startingWith(lines, 'FAIL contrast (').length, 1, lines.join('\n'));
  });
});

test('runChecks: a blue link colour fails the brand rule, which names every pair that uses it', () => {
  const css = replaceOnce(CSS, '--ifm-link-color: var(--ifm-color-primary);', '--ifm-link-color: var(--ov-blue);');
  withSiteTree({'src/css/custom.css': css}, (root) => {
    const {lines, errors} = runChecks(root);
    const tail = 'uses blue #3D7DC9 as text, which is only for large or bold text and non-text accents';
    assert.deepEqual(startingWith(errors, 'brand:'), [
      `brand: light links: text pair --ifm-link-color on --ifm-background-color ${tail}`,
      `brand: light inline code in a link: text pair --ifm-link-color on --ifm-code-background ${tail}`,
    ]);
    assert.equal(startingWith(lines, 'FAIL brand (').length, 1, lines.join('\n'));
  });
});

test('runChecks: a wrong dark primary colour fails the brand rule while dark mode is on', () => {
  const css = replaceOnce(CSS, '--ifm-color-primary: #CBD9DA;', '--ifm-color-primary: #25c2a0;');
  withSiteTree({'src/css/custom.css': css}, (root) => {
    assert.deepEqual(startingWith(runChecks(root).errors, 'brand:'), [
      'brand: dark --ifm-color-primary must be #CBD9DA (mist), found #25c2a0',
    ]);
  });
});

test('runChecks: a font file whose sha256 is not in the README fails the font rule', () => {
  const name = 'montserrat-latin-700.woff2';
  const hash = createHash('sha256').update(goodFontFiles()[name]).digest('hex');
  const readme = replaceOnce(README, hash, 'f'.repeat(64));
  withSiteTree({'README.md': readme}, (root) => {
    const {lines, errors} = runChecks(root);
    assert.deepEqual(startingWith(errors, 'fonts:'), [`fonts: README.md has no sha256 ${hash} for ${name}`]);
    assert.equal(startingWith(lines, 'FAIL fonts (').length, 1, lines.join('\n'));
  });
});

test('runChecks: a font file that is missing fails the font rule', () => {
  withSiteTree({'src/css/fonts/montserrat-latin-600.woff2': null}, (root) => {
    assert.deepEqual(startingWith(runChecks(root).errors, 'fonts:'), [
      'fonts: custom.css references ./fonts/montserrat-latin-600.woff2 but the file is missing',
    ]);
  });
});

// Built from pieces: no file under guide/ may hold a banned name whole.
const CDN_API = 'fonts.' + 'googleapis';

test('runChecks: a font CDN named in a nested doc fails the font rule with file and line', () => {
  const page = `---\ntitle: Deep\n---\n\nLoad it from https://${CDN_API}.com/css2 please.\n`;
  withSiteTree({'docs/deep/er/page.md': page}, (root) => {
    assert.deepEqual(startingWith(runChecks(root).errors, 'fonts:'), [
      `fonts: docs/deep/er/page.md:5 mentions "${CDN_API}"; fonts are self-hosted, no CDN and no font package`,
    ]);
  });
});

test('runChecks: the colour mode switch off while a dark block exists is an error', () => {
  const config = replaceOnce(CONFIG, SWITCH_ON, SWITCH_OFF);
  withSiteTree({'docusaurus.config.ts': config}, (root) => {
    const {lines, errors} = runChecks(root);
    assert.deepEqual(errors, ['contrast: colorMode.disableSwitch is true but custom.css still has a dark block']);
    assert.equal(startingWith(lines, 'FAIL contrast (').length, 1, lines.join('\n'));
  });
});

test('runChecks: the switch off without respectPrefersColorScheme: false is an error', () => {
  const config = replaceOnce(CONFIG, SWITCH_ON, `disableSwitch: true,\n      ${SWITCH_ON}`);
  const css = replaceOnce(CSS, DARK_SELECTOR, 'html.no-dark-theme');
  withSiteTree({'docusaurus.config.ts': config, 'src/css/custom.css': css}, (root) => {
    assert.deepEqual(runChecks(root).errors, [
      'contrast: colorMode.disableSwitch is true, so colorMode.respectPrefersColorScheme must be false',
    ]);
  });
});

test('runChecks: the switch on without a dark block is an error', () => {
  const css = replaceOnce(CSS, DARK_SELECTOR, 'html.no-dark-theme');
  withSiteTree({'src/css/custom.css': css}, (root) => {
    assert.deepEqual(startingWith(runChecks(root).errors, 'contrast: dark mode is on'), [
      'contrast: dark mode is on (colorMode.disableSwitch is not true) but custom.css has no dark block',
    ]);
  });
});

test('runChecks: with the switch off and no dark block only the light pairs are checked and printed', () => {
  const config = replaceOnce(CONFIG, SWITCH_ON, SWITCH_OFF);
  const css = replaceOnce(CSS, DARK_SELECTOR, 'html.no-dark-theme');
  withSiteTree({'docusaurus.config.ts': config, 'src/css/custom.css': css}, (root) => {
    const {lines, errors} = runChecks(root);
    assert.deepEqual(errors, []);
    assert.equal(startingWith(lines, 'ok   contrast light ').length, CONTRAST_PAIRS.length);
    assert.deepEqual(startingWith(lines, 'ok   contrast dark '), []);
    assert.ok(
      lines.includes(`ok   contrast (${CONTRAST_PAIRS.length} pair checks, light only, dark mode is off)`),
      lines.join('\n'),
    );
  });
});

test('runChecks: with the switch off a wrong dark primary colour is not checked', () => {
  const config = replaceOnce(CONFIG, SWITCH_ON, SWITCH_OFF);
  const css = replaceOnce(replaceOnce(CSS, DARK_SELECTOR, 'html.no-dark-theme'), '--ifm-color-primary: #CBD9DA;', '--ifm-color-primary: #25c2a0;');
  withSiteTree({'docusaurus.config.ts': config, 'src/css/custom.css': css}, (root) => {
    assert.deepEqual(runChecks(root).errors, []);
  });
});

test('runChecks: a missing stylesheet or config fails the contrast, brand and font rules and names the file', () => {
  withSiteTree({'src/css/custom.css': null}, (root) => {
    const {errors} = runChecks(root);
    for (const rule of ['contrast', 'brand', 'fonts']) {
      assert.ok(errors.includes(`${rule}: src/css/custom.css is missing`), errors.join(' | '));
    }
  });
  withSiteTree({'docusaurus.config.ts': null}, (root) => {
    const {errors} = runChecks(root);
    for (const rule of ['contrast', 'brand']) {
      assert.ok(errors.includes(`${rule}: docusaurus.config.ts is missing`), errors.join(' | '));
    }
  });
});
