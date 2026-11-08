/// <reference types="node" />
import { readdirSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

// Vitest replaces every stylesheet import, including the "?raw" form, with an empty string. So the stylesheets are read
// from disk instead. Every stylesheet in src is read, which means a stylesheet added later is checked without touching
// this file. The keys look like "src/styles/global.css".
function readStylesheets(): Record<string, string> {
  const srcDir = join(import.meta.dirname, '..')
  const sheets: Record<string, string> = {}
  for (const name of readdirSync(srcDir, { recursive: true, encoding: 'utf8' })) {
    if (name.endsWith('.css')) sheets[`src/${name.replaceAll('\\', '/')}`] = readFileSync(join(srcDir, name), 'utf8')
  }
  return sheets
}

const stylesheets = readStylesheets()
const tokensCss = stylesheets['src/styles/tokens.css']

// WCAG 2.2 SC 1.4.3 asks for 4.5:1 for normal text and SC 1.4.11 asks for 3:1 for the focus ring and logotypes.
const MIN_TEXT_RATIO = 4.5
const MIN_NON_TEXT_RATIO = 3

// ---------------------------------------------------------------------------------------------------------------------
// WCAG relative luminance and contrast ratio
// ---------------------------------------------------------------------------------------------------------------------

function linearise(channel: number): number {
  const c = channel / 255
  return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4
}

function relativeLuminance(hex: string): number {
  const red = linearise(Number.parseInt(hex.slice(1, 3), 16))
  const green = linearise(Number.parseInt(hex.slice(3, 5), 16))
  const blue = linearise(Number.parseInt(hex.slice(5, 7), 16))
  return 0.2126 * red + 0.7152 * green + 0.0722 * blue
}

function contrastRatio(first: string, second: string): number {
  const one = relativeLuminance(first)
  const two = relativeLuminance(second)
  return (Math.max(one, two) + 0.05) / (Math.min(one, two) + 0.05)
}

// The one predicate every text pair assertion uses.
function meetsTextContrast(foreground: string, background: string): boolean {
  return contrastRatio(foreground, background) >= MIN_TEXT_RATIO
}

// ---------------------------------------------------------------------------------------------------------------------
// Reading CSS: tokens, rules and the text pairs a rule declares
// ---------------------------------------------------------------------------------------------------------------------

type Rule = { selector: string; declarations: Array<[string, string]>; surfaces: string[] }
type Pair = { selector: string; foreground: string; background: string }

// A rule can name the surface its text sits on with a comment inside its braces: /* ob-on: --ob-snow, --ob-white */.
// The comment becomes a pseudo declaration first, then every other comment is dropped, so a commented-out declaration
// never counts. Nested at-rules (media queries) work because only the innermost blocks are matched.
function parseRules(css: string): Rule[] {
  const annotated = css.replace(/\/\*\s*ob-on:\s*([^*]*?)\s*\*\//g, (_match, surfaces: string) => `--ob-on: ${surfaces};`)
  const plain = annotated.replace(/\/\*[\s\S]*?\*\//g, '')
  const rules: Rule[] = []
  for (const match of plain.matchAll(/([^{}]+)\{([^{}]*)\}/g)) {
    const declarations: Array<[string, string]> = []
    let surfaces: string[] = []
    for (const part of match[2].split(';')) {
      const colon = part.indexOf(':')
      if (colon < 0) continue
      const property = part.slice(0, colon).trim().toLowerCase()
      const value = part.slice(colon + 1).trim()
      if (property === '--ob-on') {
        surfaces = value.split(',').map((surface) => surface.trim())
      } else {
        declarations.push([property, value])
      }
    }
    rules.push({ selector: match[1].trim(), declarations, surfaces })
  }
  return rules
}

function tokenIn(value: string): string | undefined {
  return /var\(\s*(--ob-[a-z0-9-]+)\s*\)/.exec(value)?.[1]
}

function parseTokens(css: string): Map<string, string> {
  const tokens = new Map<string, string>()
  for (const rule of parseRules(css)) {
    if (rule.selector !== ':root') continue
    for (const [property, value] of rule.declarations) {
      if (property.startsWith('--ob-')) tokens.set(property, value)
    }
  }
  return tokens
}

const tokens = parseTokens(tokensCss)

function hexOf(token: string): string {
  const value = tokens.get(token)
  if (value === undefined) throw new Error(`Token ${token} is not defined in tokens.css`)
  return value
}

function ratioOfTokens(foreground: string, background: string): number {
  return contrastRatio(hexOf(foreground), hexOf(background))
}

// Every rule that declares `color` needs a known surface: a token background in the same rule or an ob-on comment.
// A rule with neither is reported in `uncovered`, because its text pair cannot be checked.
function collectPairs(css: string): { pairs: Pair[]; uncovered: string[] } {
  const pairs: Pair[] = []
  const uncovered: string[] = []
  for (const rule of parseRules(css)) {
    for (const [property, value] of rule.declarations) {
      if (property !== 'color') continue
      const backgroundDeclaration = rule.declarations.find(([name]) => name === 'background' || name === 'background-color')
      const ownSurface = backgroundDeclaration === undefined ? undefined : tokenIn(backgroundDeclaration[1])
      const surfaces = [...new Set([...(ownSurface === undefined ? [] : [ownSurface]), ...rule.surfaces])]
      if (surfaces.length === 0) {
        uncovered.push(rule.selector)
        continue
      }
      const foreground = tokenIn(value)
      if (foreground === undefined) continue
      for (const selector of rule.selector.split(',').map((part) => part.trim())) {
        for (const background of surfaces) pairs.push({ selector, foreground, background })
      }
    }
  }
  return { pairs, uncovered }
}

// ---------------------------------------------------------------------------------------------------------------------
// The logotype exemption (WCAG 2.2 SC 1.4.3 exempts logotypes): exactly one entry, blue on the header surface.
// ---------------------------------------------------------------------------------------------------------------------

const LOGOTYPE_EXEMPTIONS = [{ selector: '.wordmark-book', foreground: '--ob-blue', surface: '--ob-white' }] as const

function isLogotypeSelector(selector: string): boolean {
  return LOGOTYPE_EXEMPTIONS.some((entry) => entry.selector === selector)
}

function isLogotypePair(pair: Pair): boolean {
  return LOGOTYPE_EXEMPTIONS.some(
    (entry) => entry.selector === pair.selector && entry.foreground === pair.foreground && entry.surface === pair.background,
  )
}

// Selectors of every rule that sets the text colour to blue.
function blueTextSelectors(css: string): string[] {
  return parseRules(css).flatMap((rule) =>
    rule.declarations.some(([property, value]) => property === 'color' && tokenIn(value) === '--ob-blue')
      ? rule.selector.split(',').map((part) => part.trim())
      : [],
  )
}

// ---------------------------------------------------------------------------------------------------------------------
// Production data
// ---------------------------------------------------------------------------------------------------------------------

const productionSheets = Object.entries(stylesheets)
const globalCss = stylesheets['src/styles/global.css']
const productionPairs = productionSheets.flatMap(([file, css]) => collectPairs(css).pairs.map((pair) => ({ file, ...pair })))

// Combinations that are inherited (body default, text in cards, headings, text on a hovered button) and so are not
// declared together in a rule.
const EXPLICIT_PAIRS = [
  { foreground: '--ob-teal', background: '--ob-mist' },
  { foreground: '--ob-navy', background: '--ob-snow' },
  { foreground: '--ob-navy', background: '--ob-white' },
  { foreground: '--ob-white', background: '--ob-navy' },
  { foreground: '--ob-white', background: '--ob-teal' },
  { foreground: '--ob-teal', background: '--ob-white' },
  { foreground: '--ob-teal', background: '--ob-snow' },
  { foreground: '--ob-navy', background: '--ob-mist' },
  { foreground: '--ob-error-text', background: '--ob-error-bg' },
] as const

// The focus ring is blue. Blue is 2.91:1 on mist and 2.73:1 on teal, so the ring must never sit on those surfaces.
const FOCUS_RING = '--ob-blue'
const FOCUS_SURFACES = ['--ob-snow', '--ob-white', '--ob-navy'] as const

// ---------------------------------------------------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------------------------------------------------

describe('tokens.css', () => {
  it('defines the seven brand tokens with the brand values on :root', () => {
    const brand = new Map([...tokens].filter(([name]) => !['--ob-error-bg', '--ob-error-text', '--ob-shadow'].includes(name)))
    expect(Object.fromEntries(brand)).toEqual({
      '--ob-navy': '#1D252D',
      '--ob-blue': '#3D7DC9',
      '--ob-teal': '#014046',
      '--ob-sage': '#809FA2',
      '--ob-mist': '#CBD9DA',
      '--ob-snow': '#F1F5F6',
      '--ob-white': '#FFFFFF',
    })
  })

  it('defines exactly three extra tokens: error text, error background and shadow', () => {
    expect([...tokens.keys()].sort()).toEqual([
      '--ob-blue',
      '--ob-error-bg',
      '--ob-error-text',
      '--ob-mist',
      '--ob-navy',
      '--ob-sage',
      '--ob-shadow',
      '--ob-snow',
      '--ob-teal',
      '--ob-white',
    ])
  })

  it('writes every colour token as a hex value except the shadow', () => {
    for (const [name, value] of tokens) {
      if (name === '--ob-shadow') continue
      expect(value, name).toMatch(/^#[0-9A-F]{6}$/)
    }
    expect(tokens.get('--ob-shadow')).toMatch(/^rgba\(\d+, \d+, \d+, 0\.\d+\)$/)
  })
})

describe('the contrast checker', () => {
  it('matches the WCAG reference values for black and white', () => {
    expect(contrastRatio('#FFFFFF', '#000000')).toBeCloseTo(21, 2)
    expect(contrastRatio('#000000', '#000000')).toBeCloseTo(1, 2)
  })

  it('computes the calibration ratios of the brand pairs', () => {
    expect(ratioOfTokens('--ob-white', '--ob-navy')).toBeCloseTo(15.5, 2)
    expect(ratioOfTokens('--ob-navy', '--ob-mist')).toBeCloseTo(10.69, 2)
    expect(ratioOfTokens('--ob-teal', '--ob-white')).toBeCloseTo(11.52, 2)
    expect(ratioOfTokens('--ob-blue', '--ob-white')).toBeCloseTo(4.22, 2)
    expect(ratioOfTokens('--ob-sage', '--ob-white')).toBeCloseTo(2.84, 2)
  })

  it('gives the same ratio whichever colour comes first', () => {
    expect(ratioOfTokens('--ob-navy', '--ob-white')).toBeCloseTo(ratioOfTokens('--ob-white', '--ob-navy'), 6)
    expect(ratioOfTokens('--ob-blue', '--ob-mist')).toBeCloseTo(ratioOfTokens('--ob-mist', '--ob-blue'), 6)
  })

  it('uses the same predicate for the calibration that it uses for the pairs, and the predicate can fail', () => {
    expect(meetsTextContrast(hexOf('--ob-blue'), hexOf('--ob-white'))).toBe(false)
    expect(meetsTextContrast(hexOf('--ob-sage'), hexOf('--ob-white'))).toBe(false)
    expect(meetsTextContrast(hexOf('--ob-navy'), hexOf('--ob-snow'))).toBe(true)
  })

  it('refuses a token that tokens.css does not define', () => {
    expect(() => hexOf('--ob-nope')).toThrow('Token --ob-nope is not defined in tokens.css')
  })
})

describe('the pair extractor', () => {
  it('reads a background-color declaration in the same rule', () => {
    const css = '.a { color: var(--ob-white); background-color: var(--ob-teal); }'
    expect(collectPairs(css)).toEqual({
      pairs: [{ selector: '.a', foreground: '--ob-white', background: '--ob-teal' }],
      uncovered: [],
    })
  })

  it('reads the background shorthand with the token first and with extra values', () => {
    const css = '.a { background: var(--ob-white) no-repeat; color: var(--ob-navy); }'
    expect(collectPairs(css).pairs).toEqual([{ selector: '.a', foreground: '--ob-navy', background: '--ob-white' }])
  })

  it('reads a rule inside a media query', () => {
    const css = '@media (min-width: 40rem) { .a { color: var(--ob-navy); background: var(--ob-mist); } }'
    expect(collectPairs(css).pairs).toEqual([{ selector: '.a', foreground: '--ob-navy', background: '--ob-mist' }])
  })

  it('makes one pair per selector of a multi-selector rule', () => {
    const css = '.a,\n.b > c { color: var(--ob-teal); background: var(--ob-snow); }'
    expect(collectPairs(css).pairs).toEqual([
      { selector: '.a', foreground: '--ob-teal', background: '--ob-snow' },
      { selector: '.b > c', foreground: '--ob-teal', background: '--ob-snow' },
    ])
  })

  it('ignores a commented-out declaration', () => {
    const css = '.a { /* color: var(--ob-blue); */ background: var(--ob-white); }'
    expect(collectPairs(css)).toEqual({ pairs: [], uncovered: [] })
  })

  it('does not take border-color or outline-color for a text colour', () => {
    const css = '.a { border-color: var(--ob-mist); outline-color: var(--ob-blue); }'
    expect(collectPairs(css)).toEqual({ pairs: [], uncovered: [] })
  })

  it('reads an ob-on comment with two surfaces', () => {
    const css = 'a { color: var(--ob-teal); /* ob-on: --ob-snow, --ob-white */ }'
    expect(collectPairs(css)).toEqual({
      pairs: [
        { selector: 'a', foreground: '--ob-teal', background: '--ob-snow' },
        { selector: 'a', foreground: '--ob-teal', background: '--ob-white' },
      ],
      uncovered: [],
    })
  })

  it('reports a color declaration with neither a background nor an ob-on comment', () => {
    const css = '.fine { color: var(--ob-navy); background: var(--ob-snow); }\n.lost { color: var(--ob-navy); }'
    expect(collectPairs(css).uncovered).toEqual(['.lost'])
  })

  it('reports a color declaration whose background is not a token', () => {
    expect(collectPairs('.a { color: var(--ob-navy); background: transparent; }').uncovered).toEqual(['.a'])
  })
})

describe('the logotype exemption', () => {
  it('holds exactly one entry: blue on the header surface', () => {
    expect(LOGOTYPE_EXEMPTIONS).toEqual([{ selector: '.wordmark-book', foreground: '--ob-blue', surface: '--ob-white' }])
  })

  it('names the surface the header really has', () => {
    const header = parseRules(globalCss).find((rule) => rule.selector === '.site-header')
    expect(header, 'global.css has a .site-header rule').toBeDefined()
    const background = header?.declarations.find(([name]) => name === 'background' || name === 'background-color')
    expect(tokenIn(background?.[1] ?? '')).toBe(LOGOTYPE_EXEMPTIONS[0].surface)
  })

  it('is at least 3:1, the non-text threshold', () => {
    const [entry] = LOGOTYPE_EXEMPTIONS
    expect(ratioOfTokens(entry.foreground, entry.surface)).toBeGreaterThanOrEqual(MIN_NON_TEXT_RATIO)
  })

  it('lets only the listed selector use blue text', () => {
    expect(blueTextSelectors('.wordmark-book { color: var(--ob-blue); }').every(isLogotypeSelector)).toBe(true)
    expect(blueTextSelectors('.card h2 { color: var(--ob-blue); background: var(--ob-white); }')).toEqual(['.card h2'])
    expect(blueTextSelectors('.card h2 { color: var(--ob-blue); }').every(isLogotypeSelector)).toBe(false)
    expect(blueTextSelectors('.a, .wordmark-book { color: var(--ob-blue); }').every(isLogotypeSelector)).toBe(false)
  })

  it('exempts a pair only for the listed selector, the listed colour and the listed surface', () => {
    const listed = { selector: '.wordmark-book', foreground: '--ob-blue', background: '--ob-white' }
    expect(isLogotypePair(listed)).toBe(true)
    expect(isLogotypePair({ ...listed, selector: '.other' })).toBe(false)
    expect(isLogotypePair({ ...listed, background: '--ob-snow' })).toBe(false)
    expect(isLogotypePair({ ...listed, foreground: '--ob-teal' })).toBe(false)
  })

  it('is the only way blue is used as a text colour in the stylesheets', () => {
    const selectors = productionSheets.flatMap(([, css]) => blueTextSelectors(css))
    expect(selectors.filter((selector) => !isLogotypeSelector(selector))).toEqual([])
    expect(selectors.length).toBeLessThanOrEqual(1)
  })
})

describe('text contrast of the stylesheets', () => {
  it('checks at least eight explicit pairs and at least one pair read from the stylesheets', () => {
    expect(EXPLICIT_PAIRS.length).toBeGreaterThanOrEqual(8)
    expect(productionPairs.length).toBeGreaterThanOrEqual(1)
  })

  it('gives every explicit pair at least 4.5:1', () => {
    for (const pair of EXPLICIT_PAIRS) {
      expect(meetsTextContrast(hexOf(pair.foreground), hexOf(pair.background)), `${pair.foreground} on ${pair.background}`).toBe(true)
    }
  })

  it('gives every pair read from the stylesheets at least 4.5:1, the logotype aside', () => {
    for (const pair of productionPairs) {
      if (isLogotypePair(pair)) continue
      expect(
        meetsTextContrast(hexOf(pair.foreground), hexOf(pair.background)),
        `${pair.file} ${pair.selector}: ${pair.foreground} on ${pair.background}`,
      ).toBe(true)
    }
  })

  it('leaves no color declaration without a surface in any stylesheet', () => {
    for (const [file, css] of productionSheets) {
      expect(collectPairs(css).uncovered, file).toEqual([])
    }
  })

  it('reads the pairs the primitives are meant to have', () => {
    const has = (selector: string, foreground: string, background: string) =>
      productionPairs.some((pair) => pair.selector === selector && pair.foreground === foreground && pair.background === background)
    expect(has('.button-primary', '--ob-white', '--ob-teal'), 'primary button').toBe(true)
    expect(has('.badge-in-stock', '--ob-white', '--ob-teal'), 'in-stock badge').toBe(true)
    expect(has('.badge-out-of-stock', '--ob-navy', '--ob-mist'), 'out-of-stock badge').toBe(true)
    expect(has('.alert', '--ob-error-text', '--ob-error-bg'), 'alert').toBe(true)
  })
})

describe('the focus ring', () => {
  it('is blue in the stylesheets', () => {
    const ring = parseRules(globalCss).find((rule) => rule.selector.includes(':focus-visible'))
    expect(ring, 'global.css has a :focus-visible rule').toBeDefined()
    const outline = ring?.declarations.find(([name]) => name === 'outline')
    expect(tokenIn(outline?.[1] ?? '')).toBe(FOCUS_RING)
  })

  it('is at least 3:1 against every surface it appears on', () => {
    expect(FOCUS_SURFACES.length).toBeGreaterThanOrEqual(3)
    for (const surface of FOCUS_SURFACES) {
      expect(ratioOfTokens(FOCUS_RING, surface), `${FOCUS_RING} on ${surface}`).toBeGreaterThanOrEqual(MIN_NON_TEXT_RATIO)
    }
  })

  it('would fail on mist and on teal, which the ring must stay away from', () => {
    expect(ratioOfTokens(FOCUS_RING, '--ob-mist')).toBeLessThan(MIN_NON_TEXT_RATIO)
    expect(ratioOfTokens(FOCUS_RING, '--ob-teal')).toBeLessThan(MIN_NON_TEXT_RATIO)
  })
})
