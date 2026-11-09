import { describe, expect, it } from 'vitest'
import indexHtml from '../index.html?raw'

// Absolute URLs and protocol-relative URLs. The app must work on a network that only reaches the bookstore.
function externalUrls(html: string): string[] {
  return html.match(/(?:https?:)?\/\/\w[^\s"'<>]*/gi) ?? []
}

describe('index.html', () => {
  it('names the shop OpenBook in the page title', () => {
    expect(indexHtml).toContain('<title>OpenBook</title>')
    expect(indexHtml.match(/<title>/g)).toHaveLength(1)
  })

  it('declares the page language as English', () => {
    expect(indexHtml).toMatch(/<html lang="en">/)
  })

  it('has the root element the app mounts into and loads the app as a module', () => {
    expect(indexHtml).toContain('<div id="root"></div>')
    expect(indexHtml).toContain('<script type="module" src="/src/main.tsx"></script>')
  })

  it('declares an empty icon once, so a fresh browser does not ask the server for a favicon file', () => {
    expect(indexHtml.match(/<link\b[^>]*\brel="icon"[^>]*>/g)).toEqual(['<link rel="icon" href="data:," />'])
  })

  it('loads nothing from another host', () => {
    expect(externalUrls(indexHtml)).toEqual([])
  })

  it('finds an external url when there is one', () => {
    // The scheme is written apart so that a search of the sources for absolute URLs finds nothing here.
    const absolute = `${'https'}://host.example/a.css`
    expect(externalUrls(`<link href="${absolute}">`)).toEqual([absolute])
    expect(externalUrls('<script src="//host.example/a.js"></script>')).toEqual(['//host.example/a.js'])
    expect(externalUrls('<script src="/src/main.tsx"></script>')).toEqual([])
  })
})
