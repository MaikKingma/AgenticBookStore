import { describe, expect, it } from 'vitest'
import { jsonResponse, mockFetch, problemResponse, textResponse } from '../test/fetchMock'
import { ApiError, getJson, postJson } from './client'
import type { Problem } from './types'

// The promise must reject with an ApiError, never with a SyntaxError or a TypeError.
async function rejection(promise: Promise<unknown>): Promise<ApiError> {
  const error = await promise.then(
    () => undefined,
    (reason: unknown) => reason,
  )
  expect(error).toBeInstanceOf(ApiError)
  return error as ApiError
}

// Answers GET /probe with the given response and returns the normalised problem of the failed call.
async function problemOf(respond: () => Response): Promise<Problem> {
  mockFetch([{ method: 'GET', path: '/probe', respond }])
  return (await rejection(getJson('/probe'))).problem
}

describe('request shape', () => {
  it('sends a GET to the exact same-origin path with an Accept header and no body or content type', async () => {
    const mock = mockFetch([{ method: 'GET', path: '/books', respond: () => jsonResponse([]) }])

    await getJson('/books')

    expect(mock.calls).toHaveLength(1)
    expect(mock.calls[0].method).toBe('GET')
    expect(mock.calls[0].path).toBe('/books')
    expect(mock.calls[0].headers.get('Accept')).toBe('application/json, application/problem+json')
    expect(mock.calls[0].headers.get('Content-Type')).toBeNull()
    expect(mock.calls[0].body).toBeUndefined()
  })

  it('sends a POST with a JSON content type, an Accept header and the payload byte for byte', async () => {
    const mock = mockFetch([
      { method: 'POST', path: '/orders/commands/place', respond: () => jsonResponse({ id: '1' }, 202) },
    ])

    await postJson('/orders/commands/place', {
      customerEmail: '  Reader@Example.COM ',
      lines: [
        { bookId: '5a0d3c71-8e24-4b19-a6f3-c2d9e1b47a58', quantity: 2 },
        { bookId: '1c7e92f4-3b86-4d05-8a1e-7f4b0d6e2c93', quantity: 1 },
        { bookId: '7f2e4b06-9d13-4c58-b0a7-3e6c1d8f5a29', quantity: 3 },
      ],
    })

    expect(mock.calls).toHaveLength(1)
    expect(mock.calls[0].method).toBe('POST')
    expect(mock.calls[0].path).toBe('/orders/commands/place')
    expect(mock.calls[0].headers.get('Content-Type')).toBe('application/json')
    expect(mock.calls[0].headers.get('Accept')).toBe('application/json, application/problem+json')
    expect(mock.calls[0].body).toBe(
      '{"customerEmail":"  Reader@Example.COM ","lines":[' +
        '{"bookId":"5a0d3c71-8e24-4b19-a6f3-c2d9e1b47a58","quantity":2},' +
        '{"bookId":"1c7e92f4-3b86-4d05-8a1e-7f4b0d6e2c93","quantity":1},' +
        '{"bookId":"7f2e4b06-9d13-4c58-b0a7-3e6c1d8f5a29","quantity":3}]}',
    )
  })

  it('does not trim or re-case a chat message that starts with a space and holds a newline', async () => {
    const mock = mockFetch([{ method: 'POST', path: '/assistant/chat', respond: () => jsonResponse({}) }])

    await postJson('/assistant/chat', {
      conversationId: '0b6d2f8e-7a10-4c7b-9a1e-3f2b8c1e5d4a',
      message: ' Hello\nWorld ',
    })

    expect(mock.calls[0].body).toBe('{"conversationId":"0b6d2f8e-7a10-4c7b-9a1e-3f2b8c1e5d4a","message":" Hello\\nWorld "}')
  })
})

describe('successful answers', () => {
  it('parses a 200 JSON body and keeps the order of a list', async () => {
    const rows = [
      { id: '3f2b8c1e-5d4a-4c7b-9a1e-0b6d2f8e7a10', price: '7.99' },
      { id: '1c7e92f4-3b86-4d05-8a1e-7f4b0d6e2c93', price: '14.25' },
      { id: '5a0d3c71-8e24-4b19-a6f3-c2d9e1b47a58', price: '9.50' },
    ]
    mockFetch([{ method: 'GET', path: '/probe', respond: () => jsonResponse(rows) }])

    expect(await getJson('/probe')).toEqual([
      { id: '3f2b8c1e-5d4a-4c7b-9a1e-0b6d2f8e7a10', price: '7.99' },
      { id: '1c7e92f4-3b86-4d05-8a1e-7f4b0d6e2c93', price: '14.25' },
      { id: '5a0d3c71-8e24-4b19-a6f3-c2d9e1b47a58', price: '9.50' },
    ])
  })

  it('parses a 202 body without raising an error', async () => {
    mockFetch([
      {
        method: 'POST',
        path: '/probe',
        respond: () => jsonResponse({ id: '2d8c4f61-7e3a-4c05-b9d2-5a1f0e6c8b73' }, 202, 'Accepted'),
      },
    ])

    expect(await postJson('/probe', {})).toEqual({ id: '2d8c4f61-7e3a-4c05-b9d2-5a1f0e6c8b73' })
  })

  it('treats a successful answer that is not JSON as a failure with the status text as title', async () => {
    mockFetch([{ method: 'GET', path: '/probe', respond: () => textResponse('<html>', 200, 'text/html', 'OK') }])

    const error = await rejection(getJson('/probe'))

    expect(error.problem).toStrictEqual({ status: 200, title: 'OK', details: [] })
  })
})

describe('ProblemDetail answers', () => {
  it('keeps title, detail, code and details of a 400 validation-failed with an indexed and a repeated field in server order', async () => {
    const problem = await problemOf(() =>
      problemResponse(400, {
        detail: 'The request is invalid',
        instance: '/orders/commands/place',
        status: 400,
        title: 'Validation failed',
        code: 'validation-failed',
        details: [
          { field: 'customerEmail', message: 'must contain one at sign' },
          { field: 'lines[1].quantity', message: 'must not be null' },
          { field: 'customerEmail', message: 'is required' },
        ],
      }),
    )

    expect(problem).toStrictEqual({
      status: 400,
      title: 'Validation failed',
      detail: 'The request is invalid',
      code: 'validation-failed',
      details: [
        { field: 'customerEmail', message: 'must contain one at sign' },
        { field: 'lines[1].quantity', message: 'must not be null' },
        { field: 'customerEmail', message: 'is required' },
      ],
    })
  })

  it('keeps a 404 book-not-found with empty details', async () => {
    const problem = await problemOf(() =>
      problemResponse(404, {
        detail: 'No book with id 46b1d7e3-2c58-4f9a-8e07-b3d5a1c9f620',
        status: 404,
        title: 'Book not found',
        code: 'book-not-found',
        details: [],
      }),
    )

    expect(problem).toStrictEqual({
      status: 404,
      title: 'Book not found',
      detail: 'No book with id 46b1d7e3-2c58-4f9a-8e07-b3d5a1c9f620',
      code: 'book-not-found',
      details: [],
    })
  })

  it('keeps a 404 order-not-found, a second code on the same status', async () => {
    const problem = await problemOf(() =>
      problemResponse(404, {
        detail: 'No order with id 15c9e2a7-8b34-4d6f-a0e1-7c2b9d4f3e58',
        status: 404,
        title: 'Order not found',
        code: 'order-not-found',
        details: [],
      }),
    )

    expect(problem).toStrictEqual({
      status: 404,
      title: 'Order not found',
      detail: 'No order with id 15c9e2a7-8b34-4d6f-a0e1-7c2b9d4f3e58',
      code: 'order-not-found',
      details: [],
    })
  })

  it('keeps a 409 insufficient-stock with three shortages of different string values in request order', async () => {
    const problem = await problemOf(() =>
      problemResponse(409, {
        detail: 'Not enough stock for 3 of the requested books',
        status: 409,
        title: 'Insufficient stock',
        code: 'insufficient-stock',
        details: [
          { bookId: '5a0d3c71-8e24-4b19-a6f3-c2d9e1b47a58', requested: '5', available: '2' },
          { bookId: '1c7e92f4-3b86-4d05-8a1e-7f4b0d6e2c93', requested: '3', available: '0' },
          { bookId: '7f2e4b06-9d13-4c58-b0a7-3e6c1d8f5a29', requested: '8', available: '4' },
        ],
      }),
    )

    expect(problem).toStrictEqual({
      status: 409,
      title: 'Insufficient stock',
      detail: 'Not enough stock for 3 of the requested books',
      code: 'insufficient-stock',
      details: [
        { bookId: '5a0d3c71-8e24-4b19-a6f3-c2d9e1b47a58', requested: '5', available: '2' },
        { bookId: '1c7e92f4-3b86-4d05-8a1e-7f4b0d6e2c93', requested: '3', available: '0' },
        { bookId: '7f2e4b06-9d13-4c58-b0a7-3e6c1d8f5a29', requested: '8', available: '4' },
      ],
    })
  })

  it('keeps a 409 book-not-orderable, a second code on the same status', async () => {
    const problem = await problemOf(() =>
      problemResponse(409, {
        detail: 'The book with id 20000000-0000-4000-8000-00000000000b cannot be ordered',
        status: 409,
        title: 'Book not orderable',
        code: 'book-not-orderable',
        details: [],
      }),
    )

    expect(problem).toStrictEqual({
      status: 409,
      title: 'Book not orderable',
      detail: 'The book with id 20000000-0000-4000-8000-00000000000b cannot be ordered',
      code: 'book-not-orderable',
      details: [],
    })
  })

  it('keeps a 422 unknown-genre with the ten genre objects in server order', async () => {
    const genres = [
      'FICTION',
      'SCIENCE_FICTION',
      'FANTASY',
      'MYSTERY',
      'ROMANCE',
      'HISTORY',
      'SCIENCE',
      'TECHNOLOGY',
      'CHILDREN',
      'POETRY',
    ]
    const problem = await problemOf(() =>
      problemResponse(422, {
        detail: "Unknown genre 'cookbooks'",
        status: 422,
        title: 'Unknown genre',
        code: 'unknown-genre',
        details: genres.map((genre) => ({ genre })),
      }),
    )

    expect(problem.status).toBe(422)
    expect(problem.title).toBe('Unknown genre')
    expect(problem.detail).toBe("Unknown genre 'cookbooks'")
    expect(problem.code).toBe('unknown-genre')
    expect(problem.details).toEqual([
      { genre: 'FICTION' },
      { genre: 'SCIENCE_FICTION' },
      { genre: 'FANTASY' },
      { genre: 'MYSTERY' },
      { genre: 'ROMANCE' },
      { genre: 'HISTORY' },
      { genre: 'SCIENCE' },
      { genre: 'TECHNOLOGY' },
      { genre: 'CHILDREN' },
      { genre: 'POETRY' },
    ])
  })

  it('keeps a 422 invalid-email, a second code on the same status', async () => {
    const problem = await problemOf(() =>
      problemResponse(422, {
        detail: "'x' is not a valid e-mail address",
        status: 422,
        title: 'Invalid e-mail address',
        code: 'invalid-email',
        details: [{ field: 'customerEmail', message: 'must contain one at sign' }],
      }),
    )

    expect(problem).toStrictEqual({
      status: 422,
      title: 'Invalid e-mail address',
      detail: "'x' is not a valid e-mail address",
      code: 'invalid-email',
      details: [{ field: 'customerEmail', message: 'must contain one at sign' }],
    })
  })

  it('keeps a 503 assistant-offline', async () => {
    const problem = await problemOf(() =>
      problemResponse(503, {
        detail: 'The assistant is switched off',
        status: 503,
        title: 'Assistant offline',
        code: 'assistant-offline',
        details: [],
      }),
    )

    expect(problem).toStrictEqual({
      status: 503,
      title: 'Assistant offline',
      detail: 'The assistant is switched off',
      code: 'assistant-offline',
      details: [],
    })
  })

  it('turns missing, null and non-array details into an empty list', async () => {
    const missing = await problemOf(() => problemResponse(404, { title: 'Book not found', code: 'book-not-found' }))
    const nothing = await problemOf(() =>
      problemResponse(404, { title: 'Book not found', code: 'book-not-found', details: null }),
    )
    const object = await problemOf(() =>
      problemResponse(404, { title: 'Book not found', code: 'book-not-found', details: { field: 'x' } }),
    )

    expect([missing.details, nothing.details, object.details]).toEqual([[], [], []])
  })

  it('takes the HTTP status, not the status in the body', async () => {
    const problem = await problemOf(() =>
      problemResponse(409, { title: 'Conflict', code: 'x', status: 200, details: [] }),
    )

    expect(problem.status).toBe(409)
  })

  it('reads problem+json without a code as a ProblemDetail and leaves the code out', async () => {
    const problem = await problemOf(() =>
      problemResponse(403, { title: 'Forbidden thing', detail: 'Not today', status: 403 }),
    )

    expect(problem).toStrictEqual({ status: 403, title: 'Forbidden thing', detail: 'Not today', details: [] })
  })

  it('reads application/json that has a code as a ProblemDetail', async () => {
    const problem = await problemOf(() =>
      jsonResponse({ title: 'Order not found', detail: 'None', code: 'order-not-found', details: [] }, 404),
    )

    expect(problem).toStrictEqual({
      status: 404,
      title: 'Order not found',
      detail: 'None',
      code: 'order-not-found',
      details: [],
    })
  })

  it('handles content types that carry a parameter', async () => {
    const plain = await problemOf(() =>
      textResponse(
        JSON.stringify({ title: 'Book not found', code: 'book-not-found', details: [] }),
        404,
        'application/json;charset=UTF-8',
      ),
    )
    const problemJson = await problemOf(() =>
      textResponse(JSON.stringify({ title: 'Order not found', details: [] }), 404, 'application/problem+json; charset=UTF-8'),
    )

    expect([plain.code, plain.title]).toEqual(['book-not-found', 'Book not found'])
    expect([problemJson.code, problemJson.title]).toEqual([undefined, 'Order not found'])
  })

  it('reads any media type with a +json suffix as JSON', async () => {
    const problem = await problemOf(() =>
      textResponse(JSON.stringify({ title: 'Odd', code: 'odd-code', details: [] }), 400, 'application/vnd.shop+json'),
    )

    expect(problem).toStrictEqual({ status: 400, title: 'Odd', code: 'odd-code', details: [] })
  })

  it('ignores a body whose content type is not JSON even when the text looks like a ProblemDetail', async () => {
    const problem = await problemOf(() =>
      textResponse(JSON.stringify({ title: 'Hidden', code: 'hidden-code', details: [] }), 500, 'text/plain', 'Internal Server Error'),
    )

    expect(problem).toStrictEqual({ status: 500, title: 'Internal Server Error', details: [] })
  })

  it('falls back to the status text when a ProblemDetail has no title', async () => {
    const problem = await problemOf(() => problemResponse(409, { code: 'x', details: [] }, 'Conflict'))

    expect(problem).toStrictEqual({ status: 409, title: 'Conflict', code: 'x', details: [] })
  })
})

describe('answers that are not a ProblemDetail', () => {
  it('maps a Boot error JSON 400 to the title from its error field, not from the status text', async () => {
    const problem = await problemOf(() =>
      jsonResponse(
        { timestamp: '2026-11-08T20:06:58.789Z', status: 400, error: 'Bad Request', path: '/books/not-a-uuid' },
        400,
        'Client Error',
      ),
    )

    expect(problem).toStrictEqual({ status: 400, title: 'Bad Request', details: [] })
  })

  it('maps a Boot error JSON 429 and keeps 429 as the status', async () => {
    const problem = await problemOf(() =>
      jsonResponse(
        { timestamp: '2026-11-08T20:07:00.001Z', status: 429, error: 'Too Many Requests', path: '/assistant/chat' },
        429,
        'Slow Down',
      ),
    )

    expect(problem).toStrictEqual({ status: 429, title: 'Too Many Requests', details: [] })
  })

  it('uses the status text when a JSON body has no error field', async () => {
    const problem = await problemOf(() => jsonResponse({ path: '/x' }, 500, 'Internal Server Error'))

    expect(problem).toStrictEqual({ status: 500, title: 'Internal Server Error', details: [] })
  })

  it('does not read a code that is not a string as a ProblemDetail', async () => {
    const problem = await problemOf(() => jsonResponse({ code: 7, title: 'Nope', error: 'Teapot' }, 418, 'I am a teapot'))

    expect(problem).toStrictEqual({ status: 418, title: 'Teapot', details: [] })
  })

  it('uses the status text for a non-JSON 502 body', async () => {
    const problem = await problemOf(() =>
      textResponse('<html><body>Bad gateway</body></html>', 502, 'text/html', 'Bad Gateway'),
    )

    expect(problem).toStrictEqual({ status: 502, title: 'Bad Gateway', details: [] })
  })

  it('uses "Request failed (502)" for an empty body and an empty status text', async () => {
    const problem = await problemOf(() => textResponse('', 502, 'application/json'))

    expect(problem).toStrictEqual({ status: 502, title: 'Request failed (502)', details: [] })
  })

  it('uses "Request failed" with the status for an empty body without a content type', async () => {
    const problem = await problemOf(() => new Response(null, { status: 504 }))

    expect(problem).toStrictEqual({ status: 504, title: 'Request failed (504)', details: [] })
  })

  it('turns malformed JSON under application/json into an ApiError with the status text', async () => {
    const problem = await problemOf(() => textResponse('{"title": ', 500, 'application/json', 'Internal Server Error'))

    expect(problem).toStrictEqual({ status: 500, title: 'Internal Server Error', details: [] })
  })

  it('turns malformed JSON under application/problem+json into an ApiError with the status text', async () => {
    const problem = await problemOf(() => textResponse('not json', 503, 'application/problem+json', 'Service Unavailable'))

    expect(problem).toStrictEqual({ status: 503, title: 'Service Unavailable', details: [] })
  })

  it('does not read a JSON value that is not an object as a ProblemDetail', async () => {
    const list = await problemOf(() => jsonResponse([{ code: 'x' }], 500, 'Internal Server Error'))
    const nothing = await problemOf(() => textResponse('null', 500, 'application/json', 'Internal Server Error'))

    expect([list.title, list.code, nothing.title, nothing.code]).toEqual([
      'Internal Server Error',
      undefined,
      'Internal Server Error',
      undefined,
    ])
  })

  it('still raises an ApiError when the body stream breaks while it is read', async () => {
    const problem = await problemOf(
      () =>
        new Response(
          new ReadableStream({
            start(controller) {
              controller.error(new TypeError('connection reset'))
            },
          }),
          { status: 502, statusText: 'Bad Gateway', headers: { 'Content-Type': 'application/json' } },
        ),
    )

    expect(problem).toStrictEqual({ status: 502, title: 'Bad Gateway', details: [] })
  })
})

describe('network failure', () => {
  it('turns a rejected fetch into status 0 with the message "Cannot reach the bookstore"', async () => {
    mockFetch([
      {
        method: 'GET',
        path: '/probe',
        respond: () => {
          throw new TypeError('Failed to fetch')
        },
      },
    ])

    const error = await rejection(getJson('/probe'))

    expect(error.problem).toStrictEqual({ status: 0, title: 'Cannot reach the bookstore', details: [] })
    expect(error.message).toBe('Cannot reach the bookstore')
    expect(error.name).toBe('ApiError')
    expect(error).toBeInstanceOf(Error)
  })

  it('does the same for a POST', async () => {
    mockFetch([
      {
        method: 'POST',
        path: '/probe',
        respond: () => {
          throw new TypeError('Failed to fetch')
        },
      },
    ])

    const error = await rejection(postJson('/probe', { a: 1 }))

    expect(error.problem.status).toBe(0)
  })
})

describe('no retry', () => {
  it('calls fetch exactly once when the answer is a 503', async () => {
    const mock = mockFetch([
      {
        method: 'GET',
        path: '/probe',
        respond: () => problemResponse(503, { title: 'Assistant offline', code: 'assistant-offline', details: [] }),
      },
    ])

    await rejection(getJson('/probe'))

    expect(mock.calls).toHaveLength(1)
  })

  it('calls fetch exactly once when the network fails', async () => {
    const mock = mockFetch([
      {
        method: 'GET',
        path: '/probe',
        respond: () => {
          throw new TypeError('Failed to fetch')
        },
      },
    ])

    await rejection(getJson('/probe'))

    expect(mock.calls).toHaveLength(1)
  })
})
