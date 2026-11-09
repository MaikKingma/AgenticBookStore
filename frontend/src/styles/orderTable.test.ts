/// <reference types="node" />
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

// jsdom computes no layout, so the fix for the order table is pinned in the text of the stylesheet. The cells of the
// order lines would otherwise inherit `overflow-wrap: anywhere` from the card they sit in. That lets the table shrink
// into its wrapper and break words in the middle, and the wrapper never scrolls.

interface Rule {
  selectors: string
  declarations: Record<string, string>
}

// The rules of a stylesheet that are not inside an at-rule, in source order. The selector list is normalised to one
// line with ", " between the selectors. A property that is declared twice in one rule keeps its last value.
function topLevelRules(css: string): Rule[] {
  const text = css.replace(/\/\*[\s\S]*?\*\//g, '')
  const rules: Rule[] = []
  let position = 0
  while (position < text.length) {
    const open = text.indexOf('{', position)
    if (open === -1) break
    let depth = 1
    let close = open + 1
    while (close < text.length && depth > 0) {
      if (text[close] === '{') depth += 1
      if (text[close] === '}') depth -= 1
      close += 1
    }
    const prelude = text.slice(position, open).trim()
    if (!prelude.startsWith('@')) {
      const declarations: Record<string, string> = {}
      for (const part of text.slice(open + 1, close - 1).split(';')) {
        const colon = part.indexOf(':')
        if (colon > -1) declarations[part.slice(0, colon).trim()] = part.slice(colon + 1).trim()
      }
      const selectors = prelude
        .split(',')
        .map((selector) => selector.trim().replace(/\s+/g, ' '))
        .join(', ')
      rules.push({ selectors, declarations })
    }
    position = close
  }
  return rules
}

function rulesFor(css: string, selectors: string): Rule[] {
  return topLevelRules(css).filter((rule) => rule.selectors === selectors)
}

function lastValue(rules: Rule[], property: string): string | undefined {
  return rules.map((rule) => rule.declarations[property]).filter((value) => value !== undefined).pop()
}

const cells = '.order-lines th, .order-lines td'

describe('order lines table at a narrow width', () => {
  const css = readFileSync(join(import.meta.dirname, 'global.css'), 'utf8')

  it('keeps whole words in the header and body cells, whatever the card around them allows', () => {
    expect(lastValue(rulesFor(css, cells), 'overflow-wrap')).toBe('normal')
  })

  it('keeps a header and the short value cells on one line, so only the title column wraps at spaces', () => {
    expect(lastValue(rulesFor(css, '.order-lines th, .order-lines td:not(:first-child)'), 'white-space')).toBe('nowrap')
  })

  it('lets the wrapper scroll sideways instead of widening the page', () => {
    expect(rulesFor(css, '.table-scroll')).toHaveLength(1)
    expect(lastValue(rulesFor(css, '.table-scroll'), 'overflow-x')).toBe('auto')
  })

  it('leaves the card rule that lets long titles and e-mail addresses break as it was', () => {
    expect(lastValue(rulesFor(css, '.card'), 'overflow-wrap')).toBe('anywhere')
  })
})

describe('the stylesheet reader', () => {
  it('normalises a selector list that is spread over lines', () => {
    const rules = topLevelRules('.a th,\n.a   td {\n  margin: 0;\n  overflow-wrap: normal;\n}')
    expect(rules).toEqual([{ selectors: '.a th, .a td', declarations: { margin: '0', 'overflow-wrap': 'normal' } }])
  })

  it('does not match a narrower selector list or a longer one', () => {
    const css = '.a th { overflow-wrap: normal; } .a th, .a td, .a caption { overflow-wrap: normal; }'
    expect(rulesFor(css, '.a th, .a td')).toEqual([])
  })

  it('skips rules inside a media query and ignores comments', () => {
    const css = '/* .a th, .a td { overflow-wrap: normal; } */ @media (min-width: 40rem) { .a th, .a td { overflow-wrap: normal; } }'
    expect(topLevelRules(css)).toEqual([])
  })

  it('takes the last value of a property and reports none when it is absent', () => {
    const rules = topLevelRules('.a { overflow-wrap: anywhere; } .a { overflow-wrap: normal; } .a { margin: 0; }')
    expect(lastValue(rules, 'overflow-wrap')).toBe('normal')
    expect(lastValue(rules, 'overflow-x')).toBeUndefined()
  })
})
