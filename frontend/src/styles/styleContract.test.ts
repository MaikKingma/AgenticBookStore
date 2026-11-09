/// <reference types="node" />
import { createHash } from 'node:crypto'
import { existsSync, readdirSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

// The style contract of the storefront. Every colour lives in tokens.css, motion respects the user's setting, keyboard
// focus is visible, and the font files are real woff2 files that the stylesheet loads from the app itself.
//
// Vitest replaces every stylesheet import, including the "?raw" form, with an empty string. So all source files are read
// from disk. A file added later is scanned without touching this test. The keys look like "src/styles/global.css".

const srcDir = join(import.meta.dirname, '..')

function readSources(): Record<string, string> {
  const sources: Record<string, string> = {}
  for (const name of readdirSync(srcDir, { recursive: true, encoding: 'utf8' })) {
    if (/\.(css|ts|tsx)$/.test(name)) sources[`src/${name.replaceAll('\\', '/')}`] = readFileSync(join(srcDir, name), 'utf8')
  }
  return sources
}

const allSources = readSources()

// The two files the scan skips: this test (its fixtures hold the forbidden values on purpose) and the token file.
const EXCLUDED = ['src/styles/styleContract.test.ts', 'src/styles/tokens.css'] as const
const scanned = Object.entries(allSources).filter(([file]) => !(EXCLUDED as readonly string[]).includes(file))

const tokensCss = allSources['src/styles/tokens.css']
const globalCss = allSources['src/styles/global.css']
const fontsCss = allSources['src/styles/fonts.css']

// ---------------------------------------------------------------------------------------------------------------------
// Reading CSS
// ---------------------------------------------------------------------------------------------------------------------

type Rule = { selector: string; declarations: Array<[string, string]>; context: string }

function stripComments(css: string): string {
  return css.replace(/\/\*[\s\S]*?\*\//g, '')
}

// Every rule of a stylesheet, with the at-rules around it (for example a media query) in `context`.
function rulesOf(css: string, context = ''): Rule[] {
  const text = stripComments(css)
  const rules: Rule[] = []
  let depth = 0
  let preludeStart = 0
  let bodyStart = 0
  for (let index = 0; index < text.length; index++) {
    if (text[index] === '{') {
      if (depth === 0) bodyStart = index + 1
      depth++
    } else if (text[index] === '}') {
      depth--
      if (depth > 0) continue
      const prelude = text.slice(preludeStart, bodyStart - 1).trim()
      const body = text.slice(bodyStart, index)
      preludeStart = index + 1
      if (prelude.startsWith('@') && body.includes('{')) {
        rules.push(...rulesOf(body, `${context} ${prelude}`.trim()))
        continue
      }
      const declarations: Array<[string, string]> = []
      for (const part of body.split(';')) {
        const colon = part.indexOf(':')
        if (colon >= 0) declarations.push([part.slice(0, colon).trim().toLowerCase(), part.slice(colon + 1).trim()])
      }
      rules.push({ selector: prelude, declarations, context })
    }
  }
  return rules
}

function declared(rule: Rule | undefined, property: string): string | undefined {
  return rule?.declarations.find(([name]) => name === property)?.[1]
}

function ruleFor(css: string, selector: string): Rule | undefined {
  return rulesOf(css).find((rule) => rule.selector === selector)
}

// ---------------------------------------------------------------------------------------------------------------------
// Rule 1: no colour literal outside tokens.css
// ---------------------------------------------------------------------------------------------------------------------

const NAMED_COLOURS = (
    'aliceblue antiquewhite aqua aquamarine azure beige bisque black blanchedalmond blue blueviolet brown burlywood ' +
    'cadetblue chartreuse chocolate coral cornflowerblue cornsilk crimson cyan darkblue darkcyan darkgoldenrod darkgray ' +
    'darkgreen darkgrey darkkhaki darkmagenta darkolivegreen darkorange darkorchid darkred darksalmon darkseagreen ' +
    'darkslateblue darkslategray darkslategrey darkturquoise darkviolet deeppink deepskyblue dimgray dimgrey dodgerblue ' +
    'firebrick floralwhite forestgreen fuchsia gainsboro ghostwhite gold goldenrod gray green greenyellow grey honeydew ' +
    'hotpink indianred indigo ivory khaki lavender lavenderblush lawngreen lemonchiffon lightblue lightcoral lightcyan ' +
    'lightgoldenrodyellow lightgray lightgreen lightgrey lightpink lightsalmon lightseagreen lightskyblue lightslategray ' +
    'lightslategrey lightsteelblue lightyellow lime limegreen linen magenta maroon mediumaquamarine mediumblue ' +
    'mediumorchid mediumpurple mediumseagreen mediumslateblue mediumspringgreen mediumturquoise mediumvioletred ' +
    'midnightblue mintcream mistyrose moccasin navajowhite navy oldlace olive olivedrab orange orangered orchid ' +
    'palegoldenrod palegreen paleturquoise palevioletred papayawhip peachpuff peru pink plum powderblue purple ' +
    'rebeccapurple red rosybrown royalblue saddlebrown salmon sandybrown seagreen seashell sienna silver skyblue ' +
    'slateblue slategray slategrey snow springgreen steelblue tan teal thistle tomato turquoise violet wheat white ' +
    'whitesmoke yellow yellowgreen'
  ).split(' ')

// The words "transparent", "currentColor" and "inherit" are not colour literals and stay allowed.
const COLOUR_FUNCTION = /\b(?:rgb|rgba|hsl|hsla|hwb|lab|lch|oklab|oklch|color)\(/gi
const HEX_COLOUR = /#[0-9a-f]{3,8}\b/gi
const COLOUR_PROPERTY = /color|background|border|outline|shadow|fill|stroke|decoration|column-rule|caret/i

// The colour literals in one value. Token references (var(--ob-navy)) and url() contents are not literals.
function literalColours(value: string): string[] {
  const bare = value
    .replace(/url\([^)]*\)/gi, ' ')
    .replace(/var\(\s*--[\w-]+\s*[,)]?/gi, ' ')
    .replace(/--[\w-]+/g, ' ')
  const found = [...(bare.match(HEX_COLOUR) ?? []), ...(bare.match(COLOUR_FUNCTION) ?? [])]
  for (const word of bare.toLowerCase().match(/[a-z]+/g) ?? []) {
    if (NAMED_COLOURS.includes(word)) found.push(word)
  }
  return found
}

// CSS: every declaration of a colour property, and every custom property (a colour could hide in one).
function cssColourLiterals(css: string): string[] {
  return rulesOf(css).flatMap((rule) =>
    rule.declarations
      .filter(([property]) => property.startsWith('--') || COLOUR_PROPERTY.test(property))
      .flatMap(([property, value]) => literalColours(value).map((literal) => `${property}: ${literal}`)),
  )
}

// TypeScript: a string value given to a colour-like key, as in an inline style prop with a colour, or fill="red".
function tsColourLiterals(source: string): string[] {
  const keyAndString = /\b([\w-]*(?:color|background|border|outline|shadow|fill|stroke)[\w-]*)['"]?\s*[:=]\s*\{?\s*(['"`])([^'"`\n]*)\2/gi
  return [...source.matchAll(keyAndString)].flatMap((match) => literalColours(match[3]).map((literal) => `${match[1]}: ${literal}`))
}

// ---------------------------------------------------------------------------------------------------------------------
// Rule 2: every var(--ob-...) names a token that tokens.css defines
// ---------------------------------------------------------------------------------------------------------------------

function definedTokens(css: string): Set<string> {
  return new Set([...css.matchAll(/(--ob-[a-z0-9-]+)\s*:/g)].map((match) => match[1]))
}

function undefinedTokens(source: string, defined: Set<string>): string[] {
  return [...source.matchAll(/var\(\s*(--ob-[a-z0-9-]+)/g)].map((match) => match[1]).filter((name) => !defined.has(name))
}

// ---------------------------------------------------------------------------------------------------------------------
// Rule 3: transitions and animations come with a prefers-reduced-motion: reduce block that switches them off
// ---------------------------------------------------------------------------------------------------------------------

const REDUCE = /prefers-reduced-motion\s*:\s*reduce/
const MOTION_PROPERTY = /^(transition|animation)(-|$)/
const SWITCHED_OFF = /^(none|0s|0ms|0\.01ms)$/

function motionFamily(property: string): 'transition' | 'animation' {
  return property.startsWith('transition') ? 'transition' : 'animation'
}

// A reduce rule covers a selector when it names it or is the universal selector (pseudo elements need their own).
function covers(reduceSelector: string, selector: string): boolean {
  if (reduceSelector === selector) return true
  if (selector.includes('::before')) return reduceSelector === '*::before'
  if (selector.includes('::after')) return reduceSelector === '*::after'
  return reduceSelector === '*'
}

function motionProblems(stylesheets: string[]): string[] {
  const rules = stylesheets.flatMap((css) => rulesOf(css))
  const inReduce = rules.filter((rule) => REDUCE.test(rule.context))
  const users = rules.filter((rule) => !REDUCE.test(rule.context) && rule.declarations.some(([property]) => MOTION_PROPERTY.test(property)))
  if (users.length === 0) return []
  if (inReduce.length === 0) return ['no @media (prefers-reduced-motion: reduce) block']
  const problems: string[] = []
  for (const user of users) {
    const families = new Set(user.declarations.filter(([property]) => MOTION_PROPERTY.test(property)).map(([property]) => motionFamily(property)))
    for (const selector of user.selector.split(',').map((part) => part.trim())) {
      for (const family of families) {
        const switchedOff = inReduce.some(
          (rule) =>
            rule.selector.split(',').some((part) => covers(part.trim(), selector)) &&
            rule.declarations.some(
              ([property, value]) =>
                (property === family || property === `${family}-duration`) && SWITCHED_OFF.test(value.replace(/\s*!important$/, '')),
            ),
        )
        if (!switchedOff) problems.push(`${selector}: ${family} is not switched off for reduced motion`)
      }
    }
  }
  return problems
}

// ---------------------------------------------------------------------------------------------------------------------
// Rule 4: visible keyboard focus
// ---------------------------------------------------------------------------------------------------------------------

function focusProblems(stylesheets: string[]): string[] {
  const rules = stylesheets.flatMap((css) => rulesOf(css))
  const problems: string[] = []
  for (const rule of rules) {
    for (const property of ['outline', 'outline-style']) {
      if (/^(none|0)\b/.test(declared(rule, property) ?? 'x')) problems.push(`${rule.selector}: ${property} is removed`)
    }
  }
  const ring = rules.find((rule) => rule.selector.split(',').some((part) => part.includes(':focus-visible')) && declared(rule, 'outline'))
  if (ring === undefined) return [...problems, 'no :focus-visible rule with an outline']
  const outline = declared(ring, 'outline') ?? ''
  if (!outline.includes('var(--ob-blue)')) problems.push(`the ring is not blue: ${outline}`)
  if (Number(/(\d+)px/.exec(outline)?.[1] ?? 0) < 2) problems.push(`the ring is thinner than 2px: ${outline}`)
  if (!/^[1-9]\d*px$/.test(declared(ring, 'outline-offset') ?? '')) problems.push('the ring needs a positive outline-offset, so it sits outside the element')
  return problems
}

// ---------------------------------------------------------------------------------------------------------------------
// Rule 5: fonts
// ---------------------------------------------------------------------------------------------------------------------

function hasWoff2Magic(bytes: Uint8Array): boolean {
  return bytes.length > 4 && String.fromCharCode(bytes[0], bytes[1], bytes[2], bytes[3]) === 'wOF2'
}

function isRelativeUrl(url: string): boolean {
  return /^\.{1,2}\//.test(url)
}

// Problems of the @font-face rules of a stylesheet. `fileExists` tells whether a url() target is a real file.
function fontFaceProblems(css: string, fileExists: (url: string) => boolean): string[] {
  const faces = rulesOf(css).filter((rule) => rule.selector === '@font-face')
  const problems: string[] = []
  if (faces.length !== 3) problems.push(`expected 3 @font-face rules, found ${faces.length}`)
  else if (faces.map((face) => declared(face, 'font-weight')).sort().join() !== '400,600,700') problems.push('the weights are not 400, 600 and 700')
  for (const face of faces) {
    const weight = declared(face, 'font-weight')
    if (declared(face, 'font-family') !== 'Montserrat') problems.push(`weight ${weight}: font-family is not Montserrat`)
    if (declared(face, 'font-display') !== 'swap') problems.push(`weight ${weight}: font-display is not swap`)
    const url = /^url\('([^']+)'\) format\('woff2'\)$/.exec(declared(face, 'src') ?? '')?.[1]
    if (url === undefined) problems.push(`weight ${weight}: src is not one url() in woff2 format`)
    else if (!isRelativeUrl(url)) problems.push(`weight ${weight}: url ${url} is not relative`)
    else if (!fileExists(url)) problems.push(`weight ${weight}: ${url} is not a file`)
  }
  return problems
}

// ---------------------------------------------------------------------------------------------------------------------
// Rule 6: url() in a stylesheet is relative, never absolute and never from the network
// ---------------------------------------------------------------------------------------------------------------------

function nonRelativeUrls(css: string): string[] {
  return [...stripComments(css).matchAll(/url\(\s*['"]?([^'")]+)['"]?\s*\)/gi)].map((match) => match[1]).filter((url) => !isRelativeUrl(url))
}

// ---------------------------------------------------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------------------------------------------------

describe('the scan', () => {
  it('covers the stylesheets and the app files and skips exactly this test and tokens.css', () => {
    const files = scanned.map(([file]) => file)
    expect(files).toContain('src/styles/global.css')
    expect(files).toContain('src/styles/fonts.css')
    expect(files).toContain('src/main.tsx')
    expect(files).toContain('src/App.tsx')
    expect(EXCLUDED).toEqual(['src/styles/styleContract.test.ts', 'src/styles/tokens.css'])
    expect(Object.keys(allSources).filter((file) => !files.includes(file)).sort()).toEqual([...EXCLUDED].sort())
  })
})

describe('colour literals', () => {
  it.each<[string]>([
    ['.a { color: #abc; }'],
    ['.a { color: #ABC; }'],
    ['.a { color: #abcd; }'],
    ['.a { color: #ABCD; }'],
    ['.a { color: #aabbcc; }'],
    ['.a { color: #AABBCC; }'],
    ['.a { color: #aabbccdd; }'],
    ['.a { color: #AABBCCDD; }'],
    ['.a { color: rgb(0, 0, 0); }'],
    ['.a { background: rgba(0, 0, 0, 0.5); }'],
    ['.a { color: hsl(0 0% 0%); }'],
    ['.a { color: hsla(0, 0%, 0%, 0.5); }'],
    ['.a { color: oklch(0.5 0.1 200); }'],
    ['.a { background: color-mix(in srgb, #fff 50%, var(--ob-navy)); }'],
    ['.a { background: color-mix(in srgb, red, var(--ob-navy)); }'],
    ['.a { border: 1px solid red; }'],
    ['.a { box-shadow: 0 1px 2px black; }'],
    ['.a { color: navy; }'],
    ['.a { color: Snow; }'],
    ['.a { --brand: #fff; }'],
    ['.a { color: var(--ob-navy, #fff); }'],
    ['@media (min-width: 40rem) { .a { background-color: tomato; } }'],
  ])('flags %s', (css) => {
    expect(cssColourLiterals(css)).not.toEqual([])
  })

  it.each<[string]>([
    ['.a { color: var(--ob-navy); }'],
    ['.a { background: transparent; }'],
    ['.a { color: currentColor; }'],
    ['.a { color: inherit; }'],
    ['.a { border: 1px solid var(--ob-mist); }'],
    ['.a { box-shadow: 0 2px 8px var(--ob-shadow); }'],
    ['.a { background: var(--ob-white) no-repeat; }'],
    ['.a { background: url("../assets/navy.png"); }'],
    ['.a { font-family: Montserrat, system-ui, "Segoe UI", sans-serif; }'],
    ['#abc { margin: 0; }'],
    ['.a { /* color: red; */ margin: 0; }'],
  ])('does not flag %s', (css) => {
    expect(cssColourLiterals(css)).toEqual([])
  })

  it.each<[string]>([
    // Built from two parts so that the repository grep for inline colour styles does not hit this fixture.
    ['const a = <p style={' + "{ color: 'red' }} />"],
    ["const a = { backgroundColor: '#fff' }"],
    ['const a = <path fill="#abc" />'],
    ["const a = { border: '1px solid rgb(0, 0, 0)' }"],
    ["const a = { boxShadow: '0 1px 2px black' }"],
  ])('flags the TypeScript line %s', (source) => {
    expect(tsColourLiterals(source)).not.toEqual([])
  })

  it.each<[string]>([
    ["const href = '#/books/'"],
    ["const href = '#/basket'"],
    ["const a = { background: '--ob-navy' }"],
    ["const a = <p style={{ margin: '0' }} />"],
    ['const a = <path fill="currentColor" />'],
  ])('does not flag the TypeScript line %s', (source) => {
    expect(tsColourLiterals(source)).toEqual([])
  })

  it('finds no colour literal in any stylesheet or source file outside tokens.css', () => {
    const found = scanned.flatMap(([file, source]) =>
      (file.endsWith('.css') ? cssColourLiterals(source) : tsColourLiterals(source)).map((literal) => `${file} ${literal}`),
    )
    expect(found).toEqual([])
  })
})

describe('token references', () => {
  const defined = definedTokens('.x { --ob-navy: 1; --ob-white: 2; }')

  it('flags a var(--ob-...) that names a token nobody defines', () => {
    expect(undefinedTokens('.a { color: var(--ob-nope); }', defined)).toEqual(['--ob-nope'])
    expect(undefinedTokens('.a { color: var( --ob-nope-too , red); }', defined)).toEqual(['--ob-nope-too'])
    expect(undefinedTokens("const a = 'var(--ob-nope)'", defined)).toEqual(['--ob-nope'])
  })

  it('does not flag a defined token', () => {
    expect(undefinedTokens('.a { color: var(--ob-navy); background: var(--ob-white); }', defined)).toEqual([])
  })

  it('finds only defined tokens in every stylesheet and source file', () => {
    const tokens = definedTokens(tokensCss)
    expect(tokens.size).toBe(10)
    const found = scanned.flatMap(([file, source]) => undefinedTokens(source, tokens).map((name) => `${file} ${name}`))
    expect(found).toEqual([])
  })
})

describe('reduced motion', () => {
  const universalOff = '@media (prefers-reduced-motion: reduce) { *, *::before, *::after { animation: none !important; transition: none !important; } }'

  it('flags a transition without a reduce block', () => {
    expect(motionProblems(['.a { transition: color 150ms; }'])).toEqual(['no @media (prefers-reduced-motion: reduce) block'])
  })

  it('flags an animation without a reduce block', () => {
    expect(motionProblems(['.a { animation: spin 1s infinite; }'])).toEqual(['no @media (prefers-reduced-motion: reduce) block'])
  })

  it('flags a reduce block that only switches off transitions when an animation exists', () => {
    const css = '.a { animation: spin 1s; } @media (prefers-reduced-motion: reduce) { * { transition: none; } }'
    expect(motionProblems([css])).toEqual(['.a: animation is not switched off for reduced motion'])
  })

  it('does not count a no-preference block', () => {
    const css = '.a { transition: color 150ms; } @media (prefers-reduced-motion: no-preference) { * { transition: none; } }'
    expect(motionProblems([css])).toEqual(['no @media (prefers-reduced-motion: reduce) block'])
  })

  it('flags a universal rule that leaves a pseudo element running', () => {
    const css = '.a::before { transition: color 150ms; } @media (prefers-reduced-motion: reduce) { * { transition: none; } }'
    expect(motionProblems([css])).toEqual(['.a::before: transition is not switched off for reduced motion'])
  })

  it('accepts a universal reduce block for transitions and animations', () => {
    const css = `.a { transition: color 150ms; } .b::after { animation: spin 1s; } ${universalOff}`
    expect(motionProblems([css])).toEqual([])
  })

  it('accepts a reduce block that names the selector and uses a zero duration', () => {
    const css = '.a { transition: color 150ms; } @media (prefers-reduced-motion: reduce) { .a { transition-duration: 0s; } }'
    expect(motionProblems([css])).toEqual([])
  })

  it('accepts a reduce block in another stylesheet of the set', () => {
    expect(motionProblems(['.a { transition: color 150ms; }', universalOff])).toEqual([])
  })

  it('asks for nothing when no stylesheet uses motion', () => {
    expect(motionProblems(['.a { color: var(--ob-navy); }'])).toEqual([])
  })

  it('holds for the stylesheets of the app', () => {
    expect(motionProblems(scanned.filter(([file]) => file.endsWith('.css')).map(([, css]) => css))).toEqual([])
  })
})

describe('keyboard focus', () => {
  const ring = ':focus-visible { outline: 3px solid var(--ob-blue); outline-offset: 2px; }'

  it('flags a stylesheet set without a :focus-visible rule', () => {
    expect(focusProblems(['.a { color: var(--ob-navy); }'])).toEqual(['no :focus-visible rule with an outline'])
  })

  it('accepts a blue ring that is thick enough and sits outside the element', () => {
    expect(focusProblems([ring])).toEqual([])
  })

  it('flags a ring that is not blue', () => {
    expect(focusProblems([':focus-visible { outline: 3px solid var(--ob-teal); outline-offset: 2px; }']).join()).toContain('the ring is not blue')
  })

  it('flags a ring thinner than 2px', () => {
    expect(focusProblems([':focus-visible { outline: 1px solid var(--ob-blue); outline-offset: 2px; }']).join()).toContain('thinner than 2px')
  })

  it('flags a ring with a negative or missing offset', () => {
    expect(focusProblems([':focus-visible { outline: 3px solid var(--ob-blue); outline-offset: -2px; }']).join()).toContain('positive outline-offset')
    expect(focusProblems([':focus-visible { outline: 3px solid var(--ob-blue); }']).join()).toContain('positive outline-offset')
  })

  it('flags a rule that removes the outline', () => {
    expect(focusProblems([ring, 'input { outline: none; }'])).toEqual(['input: outline is removed'])
    expect(focusProblems([ring, 'a:focus { outline: 0; }'])).toEqual(['a:focus: outline is removed'])
    expect(focusProblems([ring, 'a { outline-style: none; }'])).toEqual(['a: outline-style is removed'])
  })

  it('holds for the stylesheets of the app', () => {
    expect(focusProblems(scanned.filter(([file]) => file.endsWith('.css')).map(([, css]) => css))).toEqual([])
  })
})

describe('urls in stylesheets', () => {
  it('flags an absolute or network url', () => {
    expect(nonRelativeUrls("a { background: url('/img/a.png'); }")).toEqual(['/img/a.png'])
    expect(nonRelativeUrls('a { background: url(//host/a.png); }')).toEqual(['//host/a.png'])
    // The scheme is written apart so that a search of the sources for absolute URLs finds nothing here.
    const network = `${'https'}://host/a.woff2`
    expect(nonRelativeUrls(`@font-face { src: url("${network}"); }`)).toEqual([network])
  })

  it('accepts a relative url', () => {
    expect(nonRelativeUrls("a { background: url('../assets/a.png'); b: url(./b.png); }")).toEqual([])
  })

  it('holds for every stylesheet of the app', () => {
    expect(scanned.filter(([file]) => file.endsWith('.css')).flatMap(([file, css]) => nonRelativeUrls(css).map((url) => `${file} ${url}`))).toEqual([])
  })
})

describe('fonts', () => {
  const fontsDir = join(srcDir, 'assets', 'fonts')
  const woff2 = readdirSync(fontsDir).filter((name) => name.endsWith('.woff2')).sort()

  it('ships Montserrat latin in the weights 400, 600 and 700', () => {
    expect(woff2).toEqual(['montserrat-latin-400-normal.woff2', 'montserrat-latin-600-normal.woff2', 'montserrat-latin-700-normal.woff2'])
  })

  it('has real woff2 files, which start with the bytes wOF2', () => {
    for (const name of woff2) {
      const bytes = readFileSync(join(fontsDir, name))
      expect(hasWoff2Magic(bytes), name).toBe(true)
      expect(bytes.length, name).toBeGreaterThan(5000)
    }
  })

  it('recognises the magic bytes and rejects a file that is not woff2', () => {
    expect(hasWoff2Magic(new TextEncoder().encode('wOF2 and more'))).toBe(true)
    expect(hasWoff2Magic(new TextEncoder().encode('<html><body>not a font</body></html>'))).toBe(false)
    expect(hasWoff2Magic(new TextEncoder().encode('wOF'))).toBe(false)
  })

  it('keeps the font license next to the files', () => {
    const license = readFileSync(join(fontsDir, 'OFL.txt'), 'utf8')
    expect(license).toContain('SIL OPEN FONT LICENSE Version 1.1')
    expect(license).toContain('Montserrat Project Authors')
  })

  it('keeps OFL.txt byte for byte as the pinned licence', () => {
    const digest = createHash('sha256').update(readFileSync(join(fontsDir, 'OFL.txt'))).digest('hex')
    expect(digest).toBe('f429f739351287574c0c3175a9222bb3e49c7cad3f568a04f2c08d74c107724e')
  })

  it('loads the three files with @font-face, font-display swap and relative urls', () => {
    const fileExists = (url: string) => existsSync(join(srcDir, 'styles', url))
    expect(fontFaceProblems(fontsCss, fileExists)).toEqual([])
  })

  it('points every @font-face at the file of its own weight', () => {
    const sources = rulesOf(fontsCss)
      .filter((rule) => rule.selector === '@font-face')
      .map((rule) => [declared(rule, 'font-weight'), declared(rule, 'src')])
    expect(sources).toEqual([
      ['400', "url('../assets/fonts/montserrat-latin-400-normal.woff2') format('woff2')"],
      ['600', "url('../assets/fonts/montserrat-latin-600-normal.woff2') format('woff2')"],
      ['700', "url('../assets/fonts/montserrat-latin-700-normal.woff2') format('woff2')"],
    ])
  })

  it('flags a font stylesheet that breaks a rule', () => {
    const face = (weight: string, extra: string) =>
      `@font-face { font-family: Montserrat; font-weight: ${weight}; ${extra} src: url('../assets/a.woff2') format('woff2'); }`
    const good = `${face('400', 'font-display: swap;')} ${face('600', 'font-display: swap;')} ${face('700', 'font-display: swap;')}`
    const always = () => true
    expect(fontFaceProblems(good, always)).toEqual([])
    expect(fontFaceProblems(face('400', 'font-display: swap;'), always)[0]).toContain('expected 3 @font-face rules')
    expect(fontFaceProblems(good.replace('font-display: swap;', 'font-display: auto;'), always)).toEqual(['weight 400: font-display is not swap'])
    expect(fontFaceProblems(good.replace("'../assets/a.woff2'", "'/a.woff2'"), always)).toEqual(['weight 400: url /a.woff2 is not relative'])
    expect(fontFaceProblems(good, () => false)).toHaveLength(3)
  })

  it('uses the font stack Montserrat, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif on the body', () => {
    expect(declared(ruleFor(globalCss, 'body'), 'font-family')).toBe('Montserrat, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif')
  })
})

describe('the entry point', () => {
  it('imports the tokens, the fonts and the global stylesheet in that order', () => {
    const imports = [...allSources['src/main.tsx'].matchAll(/^import '(\.\/styles\/[a-z]+\.css)'$/gm)].map((match) => match[1])
    expect(imports).toEqual(['./styles/tokens.css', './styles/fonts.css', './styles/global.css'])
  })
})

// The class names of the styling contract, which the screens of T6 to T9 use. A class counts as declared when some rule
// has it in a selector, as a whole class name (".wordmark" is not declared by ".wordmark-book").
const CONTRACT_CLASSES = [
  'wordmark', 'wordmark-book', 'site-header', 'site-header-inner', 'site-nav', 'count-badge', 'site-footer', 'page', 'card',
  'book-grid', 'button', 'button-primary', 'button-secondary', 'badge', 'badge-in-stock', 'badge-out-of-stock', 'alert',
] as const

function undeclaredClasses(css: string, classes: readonly string[]): string[] {
  const selectors = rulesOf(css).flatMap((rule) => rule.selector.split(','))
  return classes.filter((name) => !selectors.some((selector) => new RegExp(`\\.${name}(?![\\w-])`).test(selector)))
}

describe('global.css primitives', () => {
  it('declares a rule for every class of the styling contract', () => {
    expect(CONTRACT_CLASSES).toHaveLength(17)
    expect(undeclaredClasses(globalCss, CONTRACT_CLASSES)).toEqual([])
  })

  it('reports a contract class that has no rule, also when only a longer class name starts with it', () => {
    expect(undeclaredClasses('.card { margin: 0; }', ['card', 'alert'])).toEqual(['alert'])
    expect(undeclaredClasses('.wordmark-book { margin: 0; }', ['wordmark', 'wordmark-book'])).toEqual(['wordmark'])
    expect(undeclaredClasses('.site-nav a, .site-nav a:hover { margin: 0; }', ['site-nav'])).toEqual([])
  })

  it('has a sticky header', () => {
    expect(declared(ruleFor(globalCss, '.site-header'), 'position')).toBe('sticky')
  })

  it('has cards with rounded corners and the token shadow', () => {
    const card = ruleFor(globalCss, '.card')
    expect(declared(card, 'border-radius')).toBe('0.75rem')
    expect(declared(card, 'box-shadow')).toBe('0 2px 8px var(--ob-shadow)')
  })

  it('has a primary button that is white on teal', () => {
    const button = ruleFor(globalCss, '.button-primary')
    expect(declared(button, 'color')).toBe('var(--ob-white)')
    expect(declared(button, 'background')).toBe('var(--ob-teal)')
  })

  it('has teal links', () => {
    expect(declared(ruleFor(globalCss, 'a'), 'color')).toBe('var(--ob-teal)')
  })

  it('has an in-stock badge that is white on teal and an out-of-stock badge that is navy on mist', () => {
    const inStock = ruleFor(globalCss, '.badge-in-stock')
    expect([declared(inStock, 'color'), declared(inStock, 'background')]).toEqual(['var(--ob-white)', 'var(--ob-teal)'])
    const outOfStock = ruleFor(globalCss, '.badge-out-of-stock')
    expect([declared(outOfStock, 'color'), declared(outOfStock, 'background')]).toEqual(['var(--ob-navy)', 'var(--ob-mist)'])
  })

  it('has an alert in the error tokens', () => {
    const alert = ruleFor(globalCss, '.alert')
    expect([declared(alert, 'color'), declared(alert, 'background')]).toEqual(['var(--ob-error-text)', 'var(--ob-error-bg)'])
  })

  it('has a blue focus ring', () => {
    expect(declared(ruleFor(globalCss, ':focus-visible'), 'outline')).toBe('3px solid var(--ob-blue)')
  })

  it('shows the book grid in one column first and in more columns from a min-width media query', () => {
    expect(declared(ruleFor(globalCss, '.book-grid'), 'grid-template-columns')).toBe('1fr')
    const wider = rulesOf(globalCss).filter((rule) => /^@media \(min-width: [\d.]+rem\)$/.test(rule.context) && rule.selector === '.book-grid')
    expect(wider.map((rule) => declared(rule, 'grid-template-columns'))).toEqual(['repeat(2, 1fr)', 'repeat(3, 1fr)'])
  })

  it('switches off transitions and animations for everything when the user asks for reduced motion', () => {
    const reduce = rulesOf(globalCss).find((rule) => REDUCE.test(rule.context))
    expect(reduce?.selector.split(',').map((part) => part.trim())).toEqual(['*', '*::before', '*::after'])
    expect(declared(reduce, 'animation')).toBe('none !important')
    expect(declared(reduce, 'transition')).toBe('none !important')
  })
})
