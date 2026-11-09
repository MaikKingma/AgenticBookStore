import { describe, expect, it } from 'vitest'
import { jsonResponse, mockFetch, problemResponse, textResponse } from '../test/fetchMock'

describe('fetchMock', () => {
  it('answers requests whose method and path match a route and records them in the order they were made', async () => {
    const mock = mockFetch([
      { method: 'GET', path: '/books', respond: () => jsonResponse([{ id: 'a' }]) },
      { method: 'POST', path: '/orders/commands/place', respond: () => jsonResponse({ id: 'b' }, 202) },
      { method: 'POST', path: '/inventory/x', respond: () => jsonResponse({ id: 'c' }, 201) },
    ])

    const first = await fetch('/orders/commands/place', { method: 'POST', body: '{"x":1}' })
    const second = await fetch('/books', { headers: { Accept: 'application/json' } })
    const third = await fetch('/inventory/x', { method: 'POST', body: '{"a":2}' })

    expect([first.status, await first.json()]).toEqual([202, { id: 'b' }])
    expect([second.status, await second.json()]).toEqual([200, [{ id: 'a' }]])
    expect([third.status, await third.json()]).toEqual([201, { id: 'c' }])
    expect(mock.calls.map((call) => [call.method, call.path, call.body])).toEqual([
      ['POST', '/orders/commands/place', '{"x":1}'],
      ['GET', '/books', undefined],
      ['POST', '/inventory/x', '{"a":2}'],
    ])
    expect(mock.calls[1].headers.get('Accept')).toBe('application/json')
  })

  it('matches the whole path including the query string, not the path alone', async () => {
    const mock = mockFetch([{ method: 'GET', path: '/books?q=austen&genre=FICTION', respond: () => jsonResponse([]) }])

    await expect(fetch('/books?q=austen')).rejects.toThrow('No mocked route for GET /books?q=austen')

    expect(mock.takeUnmatched()).toEqual(['GET /books?q=austen'])
  })

  it('does not answer a POST on a path that is registered for GET', async () => {
    const mock = mockFetch([{ method: 'GET', path: '/books', respond: () => jsonResponse([]) }])

    await expect(fetch('/books', { method: 'POST' })).rejects.toThrow('No mocked route for POST /books')

    expect(mock.takeUnmatched()).toEqual(['POST /books'])
  })

  it('does not answer a GET on a path that is registered for POST', async () => {
    const mock = mockFetch([{ method: 'POST', path: '/orders/commands/place', respond: () => jsonResponse({}) }])

    await expect(fetch('/orders/commands/place')).rejects.toThrow('No mocked route for GET /orders/commands/place')

    expect(mock.takeUnmatched()).toEqual(['GET /orders/commands/place'])
  })

  it('fails the test through assertAllMatched when an unmatched request was made, even if the caller swallowed it', async () => {
    const mock = mockFetch([])

    await fetch('/books').catch(() => undefined)

    expect(() => mock.assertAllMatched()).toThrow('Unmatched fetch requests: GET /books')
    expect(mock.takeUnmatched()).toEqual(['GET /books'])
    expect(() => mock.assertAllMatched()).not.toThrow()
  })

  it('registers the final check for the end of the test and that check fails on an unmatched request', async () => {
    const registered: Array<() => void> = []
    const mock = mockFetch([], (callback) => {
      registered.push(callback)
    })

    await fetch('/books').catch(() => undefined)

    expect(registered).toHaveLength(1)
    expect(() => registered[0]()).toThrow('Unmatched fetch requests: GET /books')
    expect(mock.takeUnmatched()).toEqual(['GET /books'])
    expect(() => registered[0]()).not.toThrow()
  })

  it('refuses a request that is not a plain string path', async () => {
    const mock = mockFetch([{ method: 'GET', path: '/books', respond: () => jsonResponse([]) }])

    const origin = ['http', '//example.test'].join(':')

    await expect(fetch(new URL('/books', origin))).rejects.toThrow('Mocked fetch expects a string path')

    expect(mock.takeUnmatched()).toEqual(['GET [not a string path]'])
  })

  it('builds a fresh response for every call', async () => {
    mockFetch([{ method: 'GET', path: '/books', respond: () => jsonResponse(['x']) }])

    const first = await fetch('/books')
    const second = await fetch('/books')

    expect([await first.json(), await second.json()]).toEqual([['x'], ['x']])
  })

  it('builds responses with the content type, status and status text the server would send', () => {
    const view = jsonResponse({ a: 1 }, 201)
    const problem = problemResponse(409, { code: 'x' })
    const text = textResponse('<html></html>', 502, 'text/html', 'Bad Gateway')

    expect([view.status, view.headers.get('Content-Type')]).toEqual([201, 'application/json'])
    expect([problem.status, problem.headers.get('Content-Type')]).toEqual([409, 'application/problem+json'])
    expect([text.status, text.statusText, text.headers.get('Content-Type')]).toEqual([502, 'Bad Gateway', 'text/html'])
  })
})
