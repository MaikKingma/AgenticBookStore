import {test} from 'node:test';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {mkdtempSync, mkdirSync, readFileSync, writeFileSync, rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join, dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {scanBuilt} from './rules/built.mjs';
import {packCommandLines} from './rules/content.mjs';
import {SOURCE_FILES} from './coverage.mjs';

// Rule (c) on the built pages (check:built). The fixtures in fixtures/built-powershell/ are
// captured from a Docusaurus 3.10.2 Pages build (minified, unquoted attributes, no </li> after a
// tab, hidden panels); fixtures/built-powershell/README.md lists the MDX source of each.

const HERE = dirname(fileURLToPath(import.meta.url));
const PACK = join(HERE, '..', '..', 'docs', 'spec-pack');
const CLI = join(HERE, 'check-built.mjs');
const FIXTURES = join(HERE, 'fixtures', 'built-powershell');
const fixture = (name) => readFileSync(join(FIXTURES, `${name}.html`), 'utf8');
const page = (body) => `<!doctype html><html lang=en><body><main><div class=container>${body}</div></main></body></html>\n`;
const INV = 'Remove-Item -Recurse inventedfolder';
const NBSP = String.fromCharCode(0xa0);

// Hand-made pieces in the shape of the captured fixtures: a tabs container with the tab labels
// and panel bodies given, the first panel shown, the others hidden, no </li> after a tab; a
// highlighted code block with one .token-line per line.
const tabs = (items) =>
  `<div class="theme-tabs-container tabs-container tabList_vrMO"><ul role=tablist aria-orientation=horizontal class=tabs>${items
    .map(([label], i) => `<li role=tab tabindex=${i === 0 ? 0 : -1} aria-selected=${i === 0} class="tabs__item tabItem_LRm4${i === 0 ? ' tabs__item--active' : ''}">${label}`)
    .join('')}</ul><div class=margin-top--md>${items
    .map(([, body], i) => `<div role=tabpanel class=tabItem_e6wi${i === 0 ? '' : ' hidden'}>${body}</div>`)
    .join('')}</div></div>`;
const codeBlock = (...tokenLines) =>
  `<div class="language-powershell codeBlockContainer_sQUW theme-code-block"><div class=codeBlockContent_lXeI><pre tabindex=0 class="prism-code language-powershell codeBlock_ItfM thin-scrollbar"><code class=codeBlockLines_H9lz>${tokenLines
    .map((line) => `<div class=token-line><span class="token plain">${line}</span><br/></div>`)
    .join('')}</code></pre></div></div>`;
const bashBlock = (...tokenLines) => codeBlock(...tokenLines).replaceAll('language-powershell', 'language-bash');
const windows = (body) => tabs([['macOS/Linux', '<p>Nothing.'], ['Windows PowerShell', body]]);

function withBuild(files, fn) {
  const root = mkdtempSync(join(tmpdir(), 'guide-built-powershell-test-'));
  const buildDir = join(root, 'build');
  mkdirSync(buildDir);
  for (const [rel, content] of Object.entries(files)) {
    const abs = join(buildDir, rel);
    mkdirSync(dirname(abs), {recursive: true});
    writeFileSync(abs, content);
  }
  try {
    return fn(buildDir, root);
  } finally {
    rmSync(root, {recursive: true, force: true});
  }
}

const scanPage = (body) => withBuild({'probe.html': page(body)}, (dir) => scanBuilt(dir));
// The offending lines named by the PowerShell errors, in order.
const offending = (errors) =>
  errors.map((e) => /the line "(.*)" in the "[^"]*" tab is not a command line/s.exec(e)?.[1]).filter((line) => line !== undefined);

test('built powershell: OA1c as built, the invented line in the Windows PowerShell panel fails with file, tab and line', () => {
  const {errors} = scanPage(fixture('oa1c'));
  assert.ok(
    errors.some((e) => /^built: probe\.html:\d+ /.test(e) && e.includes('"Windows PowerShell"') && e.includes(`"${INV}"`)),
    errors.join('\n'),
  );
});

// The 11 T5 r4 cases the source checker misses (A7), as built. Each fixture is the <article> of a
// probe page whose MDX is the qa-t5 r4 probe source (probes4, probes6) with the probes' Windows
// group: the Windows PowerShell item holds the case text and a bash fence with the invented line.
// OA1b-f (a rule f case) gets that group after its source. The inline code that lands in the
// panel is read too, decoded.
const R4_CASES = {
  OA1c: ['x 2. </TabItem> y', INV],
  OA8c: ['x * more </TabItem> y', INV],
  OA3c: ['x <b>y</b> </TabItem> z', INV],
  OA5c: ['x more </TabItem> y', INV],
  OA4c: ["x {'y'} </TabItem> z", INV],
  OA6c: ['a | </TabItem>', INV],
  OA6b: [INV],
  OA7c: ['x more </TabItem> y', INV],
  OA1b: [INV],
  OA1b2: [INV],
  'OA1b-f': [INV],
};

for (const [id, expected] of Object.entries(R4_CASES)) {
  test(`built powershell: T5 r4 case ${id} as built fails on exactly its Windows lines`, () => {
    const {errors, windowsLines} = scanPage(fixture(id.toLowerCase()));
    assert.deepEqual(offending(errors), expected);
    assert.equal(errors.length, expected.length);
    assert.equal(windowsLines, expected.length);
    for (const error of errors) assert.ok(error.includes('in the "Windows PowerShell" tab'), error);
  });
}

test('built powershell: every token line and every inline code element of the panel is one line, an empty token line is skipped', () => {
  // invented: a fence with Get-Command jdtls, an empty line and Invoke-Invented -Now, then inline `Invented-Inline`.
  const {errors, windowsLines} = scanPage(fixture('invented'));
  assert.deepEqual(offending(errors), ['Invoke-Invented -Now', 'Invented-Inline']);
  assert.equal(errors.length, 2);
  assert.equal(windowsLines, 3);
});

test('built powershell: a pre holding a pack line passes, as a code block, a raw pre and inline code; the macOS panel is not read', () => {
  // plain: macOS/Linux panel with brew install inventedthing; Windows PowerShell panel with
  // Get-Command jdtls as a fence, as <pre> and as inline code.
  const {errors, windowsLines} = scanPage(fixture('plain'));
  assert.deepEqual(errors, []);
  assert.equal(windowsLines, 3);
});

test('built powershell: a raw pre is read line by line, each newline-split line checked, empty lines skipped', () => {
  // rawpre: <pre> over two source lines renders <pre><p>Get-Command jdtls\nRemove-Item ...</pre>.
  const raw = scanPage(fixture('rawpre'));
  assert.deepEqual(offending(raw.errors), [INV]);
  assert.equal(raw.windowsLines, 2);
  // rawpre-expr: <pre>{`...`}</pre> keeps the newlines and the empty line of the template literal.
  const expr = scanPage(fixture('rawpre-expr'));
  assert.deepEqual(offending(expr.errors), [INV]);
  assert.equal(expr.windowsLines, 2);
  // rawpre-blank: a blank line inside <pre> makes MDX paragraphs, <pre><p>a<p>b</pre>; the text
  // content has no newline between them, so the joined line fails (fail-closed).
  const blank = scanPage(fixture('rawpre-blank'));
  assert.deepEqual(offending(blank.errors), [`Get-Command jdtls${INV}`]);
  assert.equal(blank.windowsLines, 1);
});

test('built powershell: a raw pre with a code element inside is read once, as the pre', () => {
  const precode = windows('<pre><code>Get-Command jdtls\nInvented-In-Code</code></pre>');
  const {errors, windowsLines} = scanPage(precode);
  assert.deepEqual(offending(errors), ['Invented-In-Code']);
  assert.equal(windowsLines, 2);
});

test('built powershell: a tab label containing PowerShell is read, other labels are not', () => {
  // labels: a jdtls group (macOS, Windows (WSL)) with invented lines in both, a group with the
  // label "PowerShell" and one with "Windows PowerShell", each holding an invented line.
  const {errors, windowsLines} = scanPage(fixture('labels'));
  assert.deepEqual(offending(errors), ['Invented-Short-Label', 'Invented-Long-Label']);
  assert.match(errors[0], /in the "PowerShell" tab/);
  assert.match(errors[1], /in the "Windows PowerShell" tab/);
  assert.equal(windowsLines, 2);
  const lower = tabs([['macOS/Linux', bashBlock('Invented-Mac')], ['powershell 7', codeBlock('Invented-Lower')]]);
  assert.deepEqual(offending(scanPage(lower).errors), ['Invented-Lower']);
});

test('built powershell: nested tabs, the n-th panel belongs to the n-th tab of its own container, any PowerShell panel around a line counts', () => {
  // nested: outer macOS/Linux panel holds tabs [macOS: brew install innerok, Windows PowerShell:
  // Inner-Invented-One]; outer Windows PowerShell panel holds tabs [Arch Linux: Inner-Invented-Two,
  // Other Linux: Get-Command jdtls].
  const {errors, windowsLines} = scanPage(fixture('nested'));
  assert.deepEqual(offending(errors), ['Inner-Invented-One', 'Inner-Invented-Two']);
  for (const error of errors) assert.match(error, /in the "Windows PowerShell" tab/);
  assert.equal(windowsLines, 3);
});

test('built powershell: tabs inside a list item keep their container (an inner <li> does not close the outer <li>)', () => {
  // inlist: "1. Install the tool:" with the os group indented under it, then "2. Next step."
  const {errors, windowsLines} = scanPage(fixture('inlist'));
  assert.equal(errors.length, 1, errors.join('\n'));
  assert.deepEqual(offending(errors), ['Invented-In-List']);
  assert.equal(windowsLines, 2);
  // inlist-ol: "- Install the tool:" with the os group under it, the Windows PowerShell panel an
  // ordered list; its first <li> does not close the outer bullet item (and the panel with it)
  const ordered = scanPage(fixture('inlist-ol'));
  assert.equal(ordered.errors.length, 1, ordered.errors.join('\n'));
  assert.deepEqual(offending(ordered.errors), ['Invented-In-Ordered-Item']);
  assert.equal(ordered.windowsLines, 2);
});

test('built powershell: a hidden panel is read like a shown one', () => {
  // every captured Windows PowerShell panel is the hidden second panel
  for (const name of ['oa1c', 'plain', 'invented']) {
    assert.match(fixture(name), /<div role=tabpanel class=tabItem_e6wi hidden><(?:p|div)/, name);
  }
  const shownFirst = tabs([['Windows PowerShell', codeBlock('Invented-Shown')], ['macOS/Linux', bashBlock('Invented-Mac')]]);
  assert.deepEqual(offending(scanPage(shownFirst).errors), ['Invented-Shown']);
  assert.deepEqual(offending(scanPage(windows(codeBlock('Invented-Hidden'))).errors), ['Invented-Hidden']);
});

test('built powershell: a panel whose tab cannot be found fails closed', () => {
  // one panel more than tabs: the third panel has no tab
  const extra = tabs([['macOS/Linux', '<p>a'], ['Windows PowerShell', '<p>b']]).replace(
    '</div></div></div>',
    `</div><div role=tabpanel class=tabItem_e6wi hidden>${codeBlock('Get-Command jdtls')}</div></div></div>`,
  );
  const missing = scanPage(extra).errors;
  assert.equal(missing.length, 1, missing.join('\n'));
  assert.match(missing[0], /^built: probe\.html:\d+ a tabs container with 2 tabs and 3 panels; /);
  // a panel outside any tabs container
  const loose = scanPage(`<div role=tabpanel>${codeBlock('Get-Command jdtls')}</div>`).errors;
  assert.equal(loose.length, 1, loose.join('\n'));
  assert.match(loose[0], /^built: probe\.html:\d+ a tab panel outside a tabs container; /);
});

test('built powershell: a stray end tag inside a panel does not end the panel', () => {
  const stray = windows(`<p>Text</section></span>${codeBlock('Invented-After-Stray')}`);
  assert.deepEqual(offending(scanPage(stray).errors), ['Invented-After-Stray']);
});

test('built powershell: entity-encoded text is decoded before the compare, as built', () => {
  // entities: $env:BOOKSTORE_AI_API_KEY="&lt;key on screen>", inline `git clone ... && cd ...` and
  // `/compact focus on &lt;topic>` (pack lines), then the invented Write-Host "a&lt;b" & 'c'.
  const {errors, windowsLines} = scanPage(fixture('entities'));
  assert.deepEqual(offending(errors), [`Write-Host "a<b" & 'c'`]);
  assert.equal(windowsLines, 4);
});

test('built powershell: named and numeric character references are decoded, an unknown one stays as written', () => {
  const pass = codeBlock('$env:BOOKSTORE_AI_API_KEY=&quot;&lt;key on screen&gt;&quot;', '$env:BOOKSTORE_AI_API_KEY=&#34;&#x3C;key on screen&#X3e;&#34;');
  assert.deepEqual(scanPage(windows(pass)).errors, []);
  assert.deepEqual(scanPage(windows('<p><code>git restore . &amp;&amp; git clean -fd</code>')).errors, []);
  // a no-break space is not a space: the line differs from the pack line Get-Command jdtls
  assert.deepEqual(offending(scanPage(windows(codeBlock('Get-Command&nbsp;jdtls'))).errors), [`Get-Command${NBSP}jdtls`]);
  assert.deepEqual(offending(scanPage(windows(codeBlock('Write-Host &#39;x&#x27; &apos;y&apos; &bogus; &#xZZ; &#0; &#x110000;'))).errors), [
    "Write-Host 'x' 'y' &bogus; &#xZZ; &#0; &#x110000;",
  ]);
});

test('built powershell: the allow-list is the pack command lines of all five pack files, compared as whole lines', () => {
  const texts = Object.fromEntries(Object.entries(SOURCE_FILES).map(([key, rel]) => [key, readFileSync(join(PACK, rel), 'utf8')]));
  const own = {};
  for (const key of Object.keys(texts)) {
    const others = packCommandLines(Object.entries(texts).filter(([other]) => other !== key).map(([, text]) => text));
    // a line only this pack file states, without markup characters
    own[key] = [...packCommandLines([texts[key]])].find((line) => !others.has(line) && /^[\w./:@ -]+$/.test(line) && line.length > 3);
    assert.ok(own[key], `no line of its own in the pack file ${key}`);
  }
  const {errors, windowsLines} = scanPage(windows(codeBlock(...Object.values(own))));
  assert.deepEqual(errors, [], Object.values(own).join(' | '));
  assert.equal(windowsLines, 5);
  // a part of a pack line, or a pack line with more after it, is no pack line
  assert.deepEqual(offending(scanPage(windows(codeBlock('Get-Command', 'Get-Command jdtls -All'))).errors), ['Get-Command', 'Get-Command jdtls -All']);
  // whitespace around a line is trimmed
  assert.deepEqual(scanPage(windows(codeBlock('   Get-Command jdtls   '))).errors, []);
});

test('built powershell: a real fragment of guide/build is read the same way: outside a tab nothing is checked, inside a PowerShell panel each line is', () => {
  // real-level-0-at-a-glance: the at a glance box of the real Level 0 page (npm run build of the
  // guide at T5), with inline code main and level-0-start and a code block git stash -u && git switch main.
  const real = fixture('real-level-0-at-a-glance');
  assert.match(real, /<div class=token-line><span class="token function">git<\/span>/);
  const outside = scanPage(real);
  assert.deepEqual(outside.errors, []);
  assert.equal(outside.windowsLines, 0);
  const inside = scanPage(windows(real));
  // main and level-0-start are inline code spans of the pack; the catch-up line is not a pack line
  assert.deepEqual(offending(inside.errors), ['git stash -u && git switch main']);
  assert.equal(inside.windowsLines, 3);
});

test('built powershell: check:built exits 1 on an invented Windows line and 0 on pack lines, and prints the Windows line count', () => {
  withBuild({'index.html': page('<p>Hello.'), 'setup-check.html': page(fixture('oa6b'))}, (dir) => {
    const result = spawnSync(process.execPath, [CLI, dir], {encoding: 'utf8'});
    assert.equal(result.status, 1, result.stdout + result.stderr);
    // oa6b: the paragraph before the tabs ends with a newline, so the Windows line is on line 2
    assert.match(result.stderr, /^built: setup-check\.html:2 the line "Remove-Item -Recurse inventedfolder" in the "Windows PowerShell" tab is not a command line or inline code span of the pack/m);
    assert.match(result.stderr, /check:built failed with 1 error\(s\)/);
  });
  const oneLine = windows(codeBlock('$env:BOOKSTORE_AI_API_KEY="&lt;key on screen>"'));
  withBuild({'index.html': page('<p>Hello.'), 'setup-check.html': page(fixture('plain')), 'reference/x.html': page(oneLine)}, (dir) => {
    const result = spawnSync(process.execPath, [CLI, dir], {encoding: 'utf8'});
    assert.equal(result.status, 0, result.stdout + result.stderr);
    assert.equal(
      result.stdout,
      'ok   built: 3 html files, no literal ":::", 0 checkboxes, all named\nok   built: 4 Windows PowerShell lines in tab panels and PowerShell code blocks, all pack command lines\n',
    );
  });
});

test('built powershell: check:built counts the lines of PowerShell code blocks outside tabs once, with the panel lines', () => {
  // a powershell fence outside tabs (2 lines), a powershell fence in a Windows PowerShell panel
  // (1 line, read once) and a bash fence outside tabs (not read)
  const files = {
    'index.html': page(codeBlock('Get-Command jdtls', '$env:BOOKSTORE_AI_API_KEY="&lt;key on screen>"') + bashBlock('Invented-Bash')),
    'setup-check.html': page(windows(codeBlock('Get-Command jdtls'))),
  };
  withBuild(files, (dir) => {
    const result = spawnSync(process.execPath, [CLI, dir], {encoding: 'utf8'});
    assert.equal(result.status, 0, result.stdout + result.stderr);
    assert.equal(
      result.stdout,
      'ok   built: 2 html files, no literal ":::", 0 checkboxes, all named\nok   built: 3 Windows PowerShell lines in tab panels and PowerShell code blocks, all pack command lines\n',
    );
  });
  withBuild({'index.html': page(codeBlock('Get-Command jdtls', 'Invented-Outside-Tabs'))}, (dir) => {
    const result = spawnSync(process.execPath, [CLI, dir], {encoding: 'utf8'});
    assert.equal(result.status, 1, result.stdout + result.stderr);
    assert.match(result.stderr, /^built: index\.html:1 the line "Invented-Outside-Tabs" in a PowerShell code block \(language-powershell\) is not a command line or inline code span of the pack/m);
    assert.match(result.stderr, /check:built failed with 1 error\(s\)/);
  });
});

test('built powershell: a lazy tabs container (only the selected panel rendered) fails closed', () => {
  // lazy-default: <Tabs groupId="os" lazy>, macOS/Linux "Nothing.", Windows PowerShell marked
  // default with a bash fence Invoke-Invented -Lazy; lazy renders the selected panel only.
  const {errors} = scanPage(fixture('lazy-default'));
  assert.equal(errors.length, 1, errors.join('\n'));
  assert.match(errors[0], /^built: probe\.html:\d+ a tabs container with 2 tabs and 1 panels; /);
});

test('built powershell: a pre holding a token-line plus other text is read whole, the text outside the token lines too', () => {
  // pre-tokenline-extra: <pre><span className="token-line">Get-Command jdtls</span>{'\n'}Invoke-Invented -Extra</pre>
  const {errors, windowsLines} = scanPage(fixture('pre-tokenline-extra'));
  assert.deepEqual(offending(errors), ['Invoke-Invented -Extra']);
  assert.equal(errors.length, 1, errors.join('\n'));
  assert.equal(windowsLines, 2);
  // the text before, between and after token lines: a token line ends a line like a newline, so
  // "Get-Command " and "jdtls" around a token line do not join into the pack line Get-Command jdtls
  const around = windows('<pre>Invented-Before<span class=token-line>Get-Command jdtls</span>Invented-Between\n<span class=token-line>Get-Command jdtls</span>Get-Command <span class=token-line>jdtls</span>jdtls</pre>');
  const mixed = scanPage(around);
  assert.deepEqual(offending(mixed.errors), ['Invented-Before', 'Invented-Between', 'Get-Command']);
  assert.equal(mixed.windowsLines, 7);
});

test('built powershell: a tab label is compared without soft hyphens, zero-width characters and character references', () => {
  // A label="Windows Power&shy;Shell" builds to a raw U+00AD in the tab (Docusaurus 3.10.2); the
  // other forms are the same label written with another invisible character or reference.
  const invisible = [0xad, 0x200b, 0x200c, 0x200d, 0x2060, 0xfeff].map((code) => String.fromCharCode(code));
  const references = ['&shy;', '&#173;', '&#xAD;', '&ZeroWidthSpace;', '&#x200B;', '&zwj;', '&NoBreak;'];
  for (const mark of [...invisible, ...references]) {
    const label = `Windows Power${mark}Shell`;
    const {errors, windowsLines} = scanPage(tabs([['macOS/Linux', '<p>Nothing.'], [label, '<p><code>Invented-Hidden-Mark</code>']]));
    assert.deepEqual(offending(errors), ['Invented-Hidden-Mark'], JSON.stringify(label));
    assert.match(errors[0], /in the "Windows PowerShell" tab/, JSON.stringify(label));
    assert.equal(windowsLines, 1, JSON.stringify(label));
  }
  // every default-ignorable code point goes, not only the six above: bidi marks and isolates, the
  // combining grapheme joiner, the Mongolian vowel separator, the invisible operators, variation
  // selectors and tag characters (built from code points so this file stays ASCII), also as a reference
  const ignorable = [0x200e, 0x200f, 0x034f, 0x061c, 0x180e, 0x2061, 0x2062, 0x2063, 0x2064, 0x202a, 0x202e, 0x2066, 0x2069, 0xfe00, 0xfe0f, 0xe0001, 0xe0041];
  for (const mark of [...ignorable.map((code) => String.fromCodePoint(code)), '&lrm;', '&#x200E;', '&#8207;', '&#847;']) {
    const label = `Windows Power${mark}Shell`;
    const shown = label.replace(/[^ -~]/gu, (c) => `<U+${c.codePointAt(0).toString(16).toUpperCase()}>`);
    const {errors, windowsLines} = scanPage(tabs([['macOS/Linux', '<p>Nothing.'], [label, '<p><code>Invented-Hidden-Mark</code>']]));
    assert.deepEqual(offending(errors), ['Invented-Hidden-Mark'], shown);
    assert.match(errors[0], /in the "Windows PowerShell" tab/, shown);
    assert.equal(windowsLines, 1, shown);
  }
  // every mark in a label goes, not only the first
  const twice = `Win${invisible[0]}dows Po&shy;wer${invisible[1]}Sh&zwj;ell`;
  assert.deepEqual(offending(scanPage(tabs([['macOS/Linux', '<p>Nothing.'], [twice, '<p><code>Invented-Twice</code>']])).errors), ['Invented-Twice']);
  // a visible character between the words still breaks the word
  assert.deepEqual(scanPage(tabs([['macOS/Linux', '<p>Nothing.'], ['Windows Power-Shell', '<p><code>Invented-Dash</code>']])).errors, []);
});

// The PowerShell code block errors as [line, language class], in order.
const offendingBlocks = (errors) =>
  errors.map((e) => /the line "(.*)" in a PowerShell code block \((language-[\w-]+)\) is not a command line/s.exec(e)).filter(Boolean).map((m) => [m[1], m[2]]);

test('built powershell: a PowerShell-language code block outside any tab is read line by line', () => {
  // pslang: fences powershell (Get-Command jdtls, Invented-Powershell), pwsh, ps1, ps, cmd, bat,
  // PowerShell (the build writes language-powershell on the pre), bash and no language (language-text).
  const {errors, windowsLines} = scanPage(fixture('pslang'));
  assert.deepEqual(offendingBlocks(errors), [
    ['Invented-Powershell', 'language-powershell'],
    ['Invented-Pwsh', 'language-pwsh'],
    ['Invented-Ps1', 'language-ps1'],
    ['Invented-Ps', 'language-ps'],
    ['Invented-Cmd', 'language-cmd'],
    ['Invented-Bat', 'language-bat'],
    ['Invented-Mixed-Case', 'language-powershell'],
  ]);
  assert.equal(errors.length, 7, errors.join('\n'));
  for (const error of errors) assert.match(error, /^built: probe\.html:\d+ the line /);
  assert.equal(windowsLines, 8);
  // pslang-opener: an unclosed {/* inside an inline code span before a powershell fence (the
  // combo-lang shape the source rule cannot see past)
  const opener = scanPage(fixture('pslang-opener'));
  assert.deepEqual(offendingBlocks(opener.errors), [['Invoke-Invented -Opener', 'language-powershell']]);
  assert.equal(opener.errors.length, 1, opener.errors.join('\n'));
  // a raw pre and a pre whose language class is upper case count too; a language class on
  // another element than the pre does not make a pre a PowerShell block
  const raw = scanPage('<pre class=language-PWSH>Get-Command jdtls\nInvented-Raw</pre><div class=language-powershell><pre>Invented-Div</pre></div>');
  assert.deepEqual(offendingBlocks(raw.errors), [['Invented-Raw', 'language-PWSH']]);
  assert.equal(raw.errors.length, 1, raw.errors.join('\n'));
  assert.equal(raw.windowsLines, 2);
});

test('built powershell: only the listed language names make a PowerShell code block, as whole class names', () => {
  // the source rule (c) lists powershell, pwsh, ps1, ps, cmd and bat; a class that only starts or
  // ends with one of them is another language
  const near = ['language-batch', 'language-psql', 'language-ps1x', 'xlanguage-ps', 'language-cmdline', 'my-language-bat'];
  for (const name of near) {
    const {errors, windowsLines} = scanPage(`<pre class=${name}>Invented-Near</pre>`);
    assert.deepEqual(errors, [], name);
    assert.equal(windowsLines, 0, name);
  }
});

test('built powershell: a PowerShell-language code block inside a non-PowerShell tab panel is read too, a line counted once', () => {
  // a powershell fence in the macOS/Linux panel: the invented line fails with the code block
  // message, the pack line passes; the bash block next to it is not read
  const mac = scanPage(tabs([['macOS/Linux', codeBlock('Get-Command jdtls', 'Invented-Mac-Ps') + bashBlock('Invented-Mac-Bash')], ['Windows PowerShell', '<p>Nothing.']]));
  assert.deepEqual(offendingBlocks(mac.errors), [['Invented-Mac-Ps', 'language-powershell']]);
  assert.equal(mac.errors.length, 1, mac.errors.join('\n'));
  assert.equal(mac.windowsLines, 2);
  const pass = scanPage(tabs([['macOS/Linux', codeBlock('Get-Command jdtls')], ['Windows PowerShell', '<p>Nothing.']]));
  assert.deepEqual(pass.errors, []);
  assert.equal(pass.windowsLines, 1);
  // a powershell fence inside a nested macOS tab of the outer macOS/Linux panel is read as well
  const nested = scanPage(tabs([['macOS/Linux', tabs([['macOS', codeBlock('Invented-Nested-Ps')], ['Arch Linux', '<p>x']])], ['Windows PowerShell', '<p>Nothing.']]));
  assert.deepEqual(offendingBlocks(nested.errors), [['Invented-Nested-Ps', 'language-powershell']]);
  // a powershell fence in a PowerShell panel is one line, named by its tab
  const both = scanPage(windows(codeBlock('Invented-Both')));
  assert.deepEqual(offending(both.errors), ['Invented-Both']);
  assert.deepEqual(offendingBlocks(both.errors), []);
  assert.equal(both.errors.length, 1, both.errors.join('\n'));
  assert.equal(both.windowsLines, 1);
});

test('built powershell: a PowerShell language class on the code element inside a pre counts as on the pre', () => {
  // pre-code-lang: <pre><code className="language-powershell"> holding Get-Command jdtls and
  // Invented-Code-Lang, then <pre><code className="language-bash"> holding Invented-Code-Bash
  const {errors, windowsLines} = scanPage(fixture('pre-code-lang'));
  assert.deepEqual(offendingBlocks(errors), [['Invented-Code-Lang', 'language-powershell']]);
  assert.equal(errors.length, 1, errors.join('\n'));
  assert.match(errors[0], /^built: probe\.html:\d+ the line /);
  assert.equal(windowsLines, 2);
  // a pack line alone passes
  const pass = scanPage('<pre><code class=language-powershell>Get-Command jdtls</code></pre>');
  assert.deepEqual(pass.errors, []);
  assert.equal(pass.windowsLines, 1);
  // among other classes, in any case, and deeper inside the pre too
  const other = scanPage('<pre><code class="x-one language-PWSH">Invented-Code-Pwsh</code></pre><pre><b><code class=language-cmd>Invented-Code-Deep</code></b></pre>');
  assert.deepEqual(offendingBlocks(other.errors), [['Invented-Code-Pwsh', 'language-PWSH'], ['Invented-Code-Deep', 'language-cmd']]);
  assert.equal(other.errors.length, 2, other.errors.join('\n'));
});

// The PowerShell code block errors as [line, language class], in order, any class text.
const offendingClasses = (errors) =>
  errors.map((e) => /the line "(.*)" in a PowerShell code block \((language-[^)]+)\) is not a command line/s.exec(e)).filter(Boolean).map((m) => [m[1], m[2]]);

test('built powershell: a language class with a non-alphanumeric suffix counts as that language', () => {
  // fence-suffix: fences powershell{1} (Get-Command jdtls, Invented-Suffix), bat{2}
  // (Invented-Bat-Suffix), ps1x{1} (Invented-Ps1x-Suffix) and psql (Invented-Psql); the build keeps
  // the info string whole in the class (language-powershell{1})
  const {errors, windowsLines} = scanPage(fixture('fence-suffix'));
  assert.deepEqual(offendingClasses(errors), [
    ['Invented-Suffix', 'language-powershell{1}'],
    ['Invented-Bat-Suffix', 'language-bat{2}'],
  ]);
  assert.equal(errors.length, 2, errors.join('\n'));
  assert.equal(windowsLines, 3);
  // other suffixes after a listed name count; a letter or digit after it makes another language
  for (const name of ['language-pwsh-x', 'language-ps1:x', 'language-cmd.x', 'language-ps_x']) {
    assert.deepEqual(offendingClasses(scanPage(`<pre class=${name}>Invented-Suffixed</pre>`).errors), [['Invented-Suffixed', name]], name);
  }
  for (const name of ['language-ps1x', 'language-psql', 'language-ps1x{1}', 'language-pwsh2', 'language-batch-x']) {
    const near = scanPage(`<pre class=${name}>Invented-Near</pre>`);
    assert.deepEqual(near.errors, [], name);
    assert.equal(near.windowsLines, 0, name);
  }
});
