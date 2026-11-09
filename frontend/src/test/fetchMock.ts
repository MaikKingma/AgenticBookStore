import { onTestFinished, vi } from 'vitest'

// A route answers one method and one exact path including its query string. The answer is built per call, because a
// Response body can be read only once. A function that throws or rejects makes fetch reject, which is how a test
// simulates a network failure.
export interface MockRoute {
  method: 'GET' | 'POST'
  path: string
  respond: () => Response | Promise<Response>
}

export interface RecordedCall {
  method: string
  path: string
  headers: Headers
  body: string | undefined
}

export interface FetchMock {
  calls: RecordedCall[]
  // Throws when a request matched no route. Called automatically when the test ends.
  assertAllMatched: () => void
  // Returns the unmatched requests as "METHOD path" and forgets them. A test that provokes an unmatched request on
  // purpose must take them, or the test fails at its end.
  takeUnmatched: () => string[]
}

// Replaces the global fetch for the current test. Every request that matches no route rejects with an error naming the
// request and is remembered, so the test fails at its end even when the code under test swallowed the rejection (the
// API client turns a rejected fetch into an ApiError). The global is restored by setupTests (unstubAllGlobals).
// The second parameter says when the final check runs; the default is the end of the current test.
export function mockFetch(
  routes: MockRoute[],
  whenTestFinishes: (callback: () => void) => void = onTestFinished,
): FetchMock {
  const calls: RecordedCall[] = []
  let unmatched: string[] = []

  const mock: FetchMock = {
    calls,
    assertAllMatched: () => {
      if (unmatched.length > 0) {
        throw new Error(`Unmatched fetch requests: ${unmatched.join(', ')}`)
      }
    },
    takeUnmatched: () => {
      const taken = unmatched
      unmatched = []
      return taken
    },
  }

  vi.stubGlobal('fetch', async (input: unknown, init?: RequestInit) => {
    const method = (init?.method ?? 'GET').toUpperCase()
    if (typeof input !== 'string') {
      unmatched.push(`${method} [not a string path]`)
      throw new Error('Mocked fetch expects a string path')
    }
    calls.push({
      method,
      path: input,
      headers: new Headers(init?.headers),
      body: typeof init?.body === 'string' ? init.body : undefined,
    })
    const route = routes.find((candidate) => candidate.method === method && candidate.path === input)
    if (route === undefined) {
      unmatched.push(`${method} ${input}`)
      throw new Error(`No mocked route for ${method} ${input}`)
    }
    return route.respond()
  })

  whenTestFinishes(() => mock.assertAllMatched())
  return mock
}

export function textResponse(text: string, status: number, contentType: string, statusText = ''): Response {
  return new Response(text, { status, statusText, headers: { 'Content-Type': contentType } })
}

// A view, a list or a Boot error JSON body, as the bookstore sends it.
export function jsonResponse(body: unknown, status = 200, statusText = ''): Response {
  return textResponse(JSON.stringify(body), status, 'application/json', statusText)
}

// A ProblemDetail body, as the bookstore sends it.
export function problemResponse(status: number, body: unknown, statusText = ''): Response {
  return textResponse(JSON.stringify(body), status, 'application/problem+json', statusText)
}
