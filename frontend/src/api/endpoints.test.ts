import { describe, expect, it } from 'vitest'
import { jsonResponse, mockFetch, problemResponse } from '../test/fetchMock'
import { chat } from './assistant'
import { getBook, searchBooks } from './books'
import { getStock } from './inventory'
import { findOrders, getOrder, placeOrder } from './orders'
import { ApiError } from './client'
import type { AssistantReplyView, BookDetailView, BookView, OrderSummaryView, OrderView, StockView } from './types'

// Resolves a request path against a placeholder origin so that the query parameters can be read back.
function queryOf(path: string): URLSearchParams {
  return new URL(path, ['http', '//example.test'].join(':')).searchParams
}

describe('searchBooks', () => {
  it('sends the four criteria in the order q, title, author, genre whatever order the caller lists them in', async () => {
    const mock = mockFetch([
      {
        method: 'GET',
        path: '/books?q=pride&title=Emma&author=Austen&genre=FICTION',
        respond: () => jsonResponse([]),
      },
    ])

    await searchBooks({ genre: 'FICTION', author: 'Austen', title: 'Emma', q: 'pride' })

    expect(mock.calls.map((call) => call.path)).toEqual(['/books?q=pride&title=Emma&author=Austen&genre=FICTION'])
  })

  it('builds the search of the storefront scenario exactly', async () => {
    const mock = mockFetch([{ method: 'GET', path: '/books?q=austen&genre=FICTION', respond: () => jsonResponse([]) }])

    await searchBooks({ q: 'austen', genre: 'FICTION' })

    expect(mock.calls.map((call) => call.path)).toEqual(['/books?q=austen&genre=FICTION'])
  })

  it('omits an empty q', async () => {
    mockFetch([{ method: 'GET', path: '/books?title=Emma&author=Austen&genre=POETRY', respond: () => jsonResponse([]) }])

    await searchBooks({ q: '', title: 'Emma', author: 'Austen', genre: 'POETRY' })
  })

  it('omits an empty title', async () => {
    mockFetch([{ method: 'GET', path: '/books?q=pride&author=Austen&genre=POETRY', respond: () => jsonResponse([]) }])

    await searchBooks({ q: 'pride', title: '', author: 'Austen', genre: 'POETRY' })
  })

  it('omits an empty author', async () => {
    mockFetch([{ method: 'GET', path: '/books?q=pride&title=Emma&genre=POETRY', respond: () => jsonResponse([]) }])

    await searchBooks({ q: 'pride', title: 'Emma', author: '', genre: 'POETRY' })
  })

  it('omits an empty genre', async () => {
    mockFetch([{ method: 'GET', path: '/books?q=pride&title=Emma&author=Austen', respond: () => jsonResponse([]) }])

    await searchBooks({ q: 'pride', title: 'Emma', author: 'Austen', genre: '' })
  })

  it('omits a criterion that holds only whitespace and one that is missing', async () => {
    mockFetch([{ method: 'GET', path: '/books?author=Austen', respond: () => jsonResponse([]) }])

    await searchBooks({ q: '   ', title: ' \t ', author: 'Austen', genre: undefined })
  })

  it('asks for the plain list, without a question mark, when there are no criteria', async () => {
    const mock = mockFetch([{ method: 'GET', path: '/books', respond: () => jsonResponse([]) }])

    await searchBooks({})
    await searchBooks({ q: '', title: '', author: '', genre: '' })
    await searchBooks()

    expect(mock.calls.map((call) => call.path)).toEqual(['/books', '/books', '/books'])
  })

  it('encodes an ampersand in a criterion', async () => {
    mockFetch([{ method: 'GET', path: '/books?q=pride+%26+prejudice', respond: () => jsonResponse([]) }])

    await searchBooks({ q: 'pride & prejudice' })
  })

  it('sends a criterion with edge whitespace as typed', async () => {
    const mock = mockFetch([{ method: 'GET', path: '/books?author=+Jane+Austen+', respond: () => jsonResponse([]) }])

    await searchBooks({ author: ' Jane Austen ' })

    expect(queryOf(mock.calls[0].path).get('author')).toBe(' Jane Austen ')
  })

  it('sends every criterion with its edge whitespace as typed', async () => {
    const mock = mockFetch([
      {
        method: 'GET',
        path: '/books?q=+pride+&title=+Emma+&author=+Austen+&genre=+fiction+',
        respond: () => jsonResponse([]),
      },
    ])

    await searchBooks({ q: ' pride ', title: ' Emma ', author: ' Austen ', genre: ' fiction ' })

    const query = queryOf(mock.calls[0].path)
    expect(query.get('q')).toBe(' pride ')
    expect(query.get('title')).toBe(' Emma ')
    expect(query.get('author')).toBe(' Austen ')
    expect(query.get('genre')).toBe(' fiction ')
  })

  it('returns the books in the order the server sent them', async () => {
    const books: BookView[] = [
      {
        id: '50000000-0000-4000-8000-00000000000a',
        isbn: '9780141439587',
        title: 'Emma',
        authors: 'Jane Austen',
        genre: 'FICTION',
        price: '7.99',
        currency: 'EUR',
        status: 'AVAILABLE',
        available: 3,
        inStock: true,
      },
      {
        id: '20000000-0000-4000-8000-00000000000b',
        isbn: '9780141439846',
        title: 'Dracula',
        authors: 'Bram Stoker, Florence Stoker',
        genre: 'FANTASY',
        price: '12.50',
        currency: 'USD',
        status: 'DISCONTINUED',
        available: 0,
        inStock: false,
      },
      {
        id: '70000000-0000-4000-8000-00000000000c',
        isbn: '9780140449136',
        title: 'The Odyssey',
        authors: 'Homer',
        genre: 'POETRY',
        price: '6.99',
        currency: 'EUR',
        status: 'AVAILABLE',
        available: 5,
        inStock: true,
      },
    ]
    mockFetch([{ method: 'GET', path: '/books', respond: () => jsonResponse(books) }])

    expect(await searchBooks()).toEqual(books)
  })

  it('rejects with the ProblemDetail of an unknown genre', async () => {
    mockFetch([
      {
        method: 'GET',
        path: '/books?genre=cookbooks',
        respond: () =>
          problemResponse(422, { title: 'Unknown genre', code: 'unknown-genre', details: [{ genre: 'FICTION' }] }),
      },
    ])

    const error = await searchBooks({ genre: 'cookbooks' }).then(
      () => undefined,
      (reason: unknown) => reason,
    )

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).problem.code).toBe('unknown-genre')
  })
})

describe('getBook', () => {
  it('asks for the book by its id and returns the detail view with the authors in credit order', async () => {
    const book: BookDetailView = {
      id: '6c4a9e12-3d87-4f05-a1b6-8e2d0c7f5b94',
      isbn: '9780201633610',
      title: 'Design Patterns',
      authors: ['Erich Gamma', 'Richard Helm', 'Ralph Johnson', 'John Vlissides'],
      genre: 'TECHNOLOGY',
      price: '49.99',
      currency: 'EUR',
      publicationYear: 1994,
      status: 'AVAILABLE',
      available: 4,
      inStock: true,
    }
    const mock = mockFetch([
      { method: 'GET', path: '/books/6c4a9e12-3d87-4f05-a1b6-8e2d0c7f5b94', respond: () => jsonResponse(book) },
    ])

    expect(await getBook('6c4a9e12-3d87-4f05-a1b6-8e2d0c7f5b94')).toEqual(book)
    expect(mock.calls.map((call) => [call.method, call.path])).toEqual([
      ['GET', '/books/6c4a9e12-3d87-4f05-a1b6-8e2d0c7f5b94'],
    ])
  })

  it('encodes the id segment', async () => {
    mockFetch([{ method: 'GET', path: '/books/a%2Fb%3Fc', respond: () => jsonResponse({}) }])

    await getBook('a/b?c')
  })

  it('does not check that the id is a UUID', async () => {
    mockFetch([{ method: 'GET', path: '/books/xyz', respond: () => jsonResponse({}) }])

    await getBook('xyz')
  })
})

describe('getStock', () => {
  it('asks for the stock of a book and returns the stock view', async () => {
    const stock: StockView = {
      bookId: '3f2b8c1e-5d4a-4c7b-9a1e-0b6d2f8e7a10',
      onHand: 7,
      reserved: 2,
      available: 5,
      reorderLevel: 3,
      lowStock: false,
    }
    const mock = mockFetch([
      { method: 'GET', path: '/inventory/3f2b8c1e-5d4a-4c7b-9a1e-0b6d2f8e7a10', respond: () => jsonResponse(stock) },
    ])

    expect(await getStock('3f2b8c1e-5d4a-4c7b-9a1e-0b6d2f8e7a10')).toEqual(stock)
    expect(mock.calls.map((call) => call.path)).toEqual(['/inventory/3f2b8c1e-5d4a-4c7b-9a1e-0b6d2f8e7a10'])
  })

  it('encodes the id segment', async () => {
    mockFetch([{ method: 'GET', path: '/inventory/a%2Fb%3Fc', respond: () => jsonResponse({}) }])

    await getStock('a/b?c')
  })
})

describe('getOrder', () => {
  it('asks for the order by its id and returns the view with a shipped instant', async () => {
    const order: OrderView = {
      id: '7d3e9a10-2b4c-4f51-8c6d-1a0b3c5d7e92',
      customerEmail: 'reader@example.com',
      status: 'SHIPPED',
      placedAt: '2026-06-15T10:00:00.123456Z',
      shippedAt: '2026-06-16T08:30:00Z',
      lines: [
        { isbn: '9780140449136', title: 'The Odyssey', quantity: 2, unitPrice: '12.50', lineTotal: '25.00' },
        { isbn: '9780143105428', title: 'Walden', quantity: 1, unitPrice: '8.00', lineTotal: '8.00' },
        { isbn: '9780141439587', title: 'Emma', quantity: 3, unitPrice: '15.00', lineTotal: '45.00' },
      ],
      total: '78.00',
      currency: 'EUR',
    }
    const mock = mockFetch([
      { method: 'GET', path: '/orders/7d3e9a10-2b4c-4f51-8c6d-1a0b3c5d7e92', respond: () => jsonResponse(order) },
    ])

    expect(await getOrder('7d3e9a10-2b4c-4f51-8c6d-1a0b3c5d7e92')).toEqual(order)
    expect(mock.calls.map((call) => call.path)).toEqual(['/orders/7d3e9a10-2b4c-4f51-8c6d-1a0b3c5d7e92'])
  })

  it('keeps a null shipped instant', async () => {
    mockFetch([
      {
        method: 'GET',
        path: '/orders/4e7a1c20-9d35-4b86-a2f1-0c8e6d3b5a47',
        respond: () => jsonResponse({ id: '4e7a1c20-9d35-4b86-a2f1-0c8e6d3b5a47', status: 'PLACED', shippedAt: null }),
      },
    ])

    const order = await getOrder('4e7a1c20-9d35-4b86-a2f1-0c8e6d3b5a47')

    expect(order.shippedAt).toBeNull()
  })

  it('encodes the id segment', async () => {
    mockFetch([{ method: 'GET', path: '/orders/a%2Fb%3Fc', respond: () => jsonResponse({}) }])

    await getOrder('a/b?c')
  })
})

describe('findOrders', () => {
  it('asks for the orders of an e-mail address and returns the summaries in the order the server sent them', async () => {
    const summaries: OrderSummaryView[] = [
      { id: '4e7a1c20-9d35-4b86-a2f1-0c8e6d3b5a47', status: 'PLACED', placedAt: '2026-06-02T11:15:00.250001Z', itemCount: 3, total: '41.00' },
      { id: '0a5d8e3c-6f21-4b97-8c40-d2e9b7a1f356', status: 'SHIPPED', placedAt: '2026-06-20T09:00:00Z', itemCount: 1, total: '8.00' },
      { id: '6e2f1a90-4b3c-4d85-9a17-0c8d5e3f2b41', status: 'PLACED', placedAt: '2026-06-11T17:45:10.5Z', itemCount: 12, total: '103.50' },
    ]
    const mock = mockFetch([
      { method: 'GET', path: '/orders?customerEmail=reader%40example.com', respond: () => jsonResponse(summaries) },
    ])

    expect(await findOrders('reader@example.com')).toEqual(summaries)
    expect(queryOf(mock.calls[0].path).get('customerEmail')).toBe('reader@example.com')
    expect(new URL(mock.calls[0].path, ['http', '//example.test'].join(':')).pathname).toBe('/orders')
  })

  it('keeps a plus sign in the address instead of turning it into a space', async () => {
    const mock = mockFetch([
      { method: 'GET', path: '/orders?customerEmail=reader%2Bshop%40example.com', respond: () => jsonResponse([]) },
    ])

    await findOrders('reader+shop@example.com')

    expect(queryOf(mock.calls[0].path).get('customerEmail')).toBe('reader+shop@example.com')
  })

  it('sends an address with edge whitespace and capitals as typed', async () => {
    const mock = mockFetch([
      { method: 'GET', path: '/orders?customerEmail=+Reader%40Example.COM+', respond: () => jsonResponse([]) },
    ])

    await findOrders(' Reader@Example.COM ')

    expect(queryOf(mock.calls[0].path).get('customerEmail')).toBe(' Reader@Example.COM ')
  })

  it('does not check the address and sends an empty one', async () => {
    mockFetch([
      {
        method: 'GET',
        path: '/orders?customerEmail=',
        respond: () =>
          problemResponse(422, {
            title: 'Invalid e-mail address',
            code: 'invalid-email',
            details: [{ field: 'customerEmail', message: 'An e-mail address is required' }],
          }),
      },
    ])

    const error = await findOrders('').then(
      () => undefined,
      (reason: unknown) => reason,
    )

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).problem.code).toBe('invalid-email')
  })
})

describe('placeOrder', () => {
  it('posts the payload unchanged to the place command and returns the new id', async () => {
    const mock = mockFetch([
      {
        method: 'POST',
        path: '/orders/commands/place',
        respond: () => jsonResponse({ id: '2d8c4f61-7e3a-4c05-b9d2-5a1f0e6c8b73' }, 202),
      },
    ])

    const result = await placeOrder({
      customerEmail: ' Reader@Example.COM ',
      lines: [
        { bookId: '5a0d3c71-8e24-4b19-a6f3-c2d9e1b47a58', quantity: 2 },
        { bookId: '1c7e92f4-3b86-4d05-8a1e-7f4b0d6e2c93', quantity: 1 },
        { bookId: '7f2e4b06-9d13-4c58-b0a7-3e6c1d8f5a29', quantity: 3 },
      ],
    })

    expect(result).toEqual({ id: '2d8c4f61-7e3a-4c05-b9d2-5a1f0e6c8b73' })
    expect(mock.calls).toHaveLength(1)
    expect(mock.calls[0].method).toBe('POST')
    expect(mock.calls[0].path).toBe('/orders/commands/place')
    expect(JSON.parse(mock.calls[0].body ?? '')).toEqual({
      customerEmail: ' Reader@Example.COM ',
      lines: [
        { bookId: '5a0d3c71-8e24-4b19-a6f3-c2d9e1b47a58', quantity: 2 },
        { bookId: '1c7e92f4-3b86-4d05-8a1e-7f4b0d6e2c93', quantity: 1 },
        { bookId: '7f2e4b06-9d13-4c58-b0a7-3e6c1d8f5a29', quantity: 3 },
      ],
    })
  })
})

describe('chat', () => {
  it('posts the payload unchanged to the assistant and returns the reply', async () => {
    const reply: AssistantReplyView = {
      conversationId: '0b6d2f8e-7a10-4c7b-9a1e-3f2b8c1e5d4a',
      reply: 'Yes, Pride and Prejudice is available for 12.50 EUR.',
    }
    const mock = mockFetch([{ method: 'POST', path: '/assistant/chat', respond: () => jsonResponse(reply) }])

    const result = await chat({ conversationId: '0b6d2f8e-7a10-4c7b-9a1e-3f2b8c1e5d4a', message: ' Is Emma in stock?\nThanks ' })

    expect(result).toEqual(reply)
    expect(mock.calls).toHaveLength(1)
    expect(mock.calls[0].method).toBe('POST')
    expect(mock.calls[0].path).toBe('/assistant/chat')
    expect(JSON.parse(mock.calls[0].body ?? '')).toEqual({
      conversationId: '0b6d2f8e-7a10-4c7b-9a1e-3f2b8c1e5d4a',
      message: ' Is Emma in stock?\nThanks ',
    })
  })

  it('rejects with the ProblemDetail of an offline assistant after one call', async () => {
    const mock = mockFetch([
      {
        method: 'POST',
        path: '/assistant/chat',
        respond: () => problemResponse(503, { title: 'Assistant offline', code: 'assistant-offline', details: [] }),
      },
    ])

    const error = await chat({ conversationId: '0b6d2f8e-7a10-4c7b-9a1e-3f2b8c1e5d4a', message: 'Hi' }).then(
      () => undefined,
      (reason: unknown) => reason,
    )

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).problem.code).toBe('assistant-offline')
    expect(mock.calls).toHaveLength(1)
  })
})
