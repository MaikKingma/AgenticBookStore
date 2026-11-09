import { act, renderHook } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { BasketProvider, useBasket } from './basket'
import { lineTotal } from './estimate'
import { jsonResponse, mockFetch, problemResponse } from '../test/fetchMock'

const STORAGE_KEY = 'openbook.basket'

// Three books. Taken in the order B, A, C no attribute is sorted in either direction (id, title, price as text and as
// a number, currency), so a basket that sorts its lines cannot pass by accident.
const BOOK_B = { id: '5b0e7a34-8c21-4d96-9f13-2a6c0e8d4b57', title: 'Emma', price: '12.50', currency: 'GBP' } as const
const BOOK_A = { id: '1a9d3c68-4e75-4b02-8a1f-7c5e2d9b0a36', title: 'Dune', price: '7.99', currency: 'EUR' } as const
const BOOK_C = { id: '7c3f1b82-6a04-4e59-b7d8-0e9a5c2f1d64', title: 'Neuromancer', price: '10.00', currency: 'USD' } as const

type Book = { id: string; title: string; price: string; currency: string }

// A line as the basket keeps and stores it.
function lineOf(book: Book, quantity: number) {
  return { bookId: book.id, title: book.title, price: book.price, currency: book.currency, quantity }
}

function mount() {
  return renderHook(() => useBasket(), { wrapper: BasketProvider })
}

// Runs a basket action inside act and hands back what it returned.
function press<T>(action: () => T): T {
  let outcome!: T
  act(() => {
    outcome = action()
  })
  return outcome
}

function storedText(): string | null {
  return sessionStorage.getItem(STORAGE_KEY)
}

function stored(): unknown {
  const text = storedText()
  return text === null ? null : JSON.parse(text)
}

// A book that exists only to fill lines. Its id, title and price come from the index in an order that is not the order
// of the index (7 is coprime to 23, so the 20 ids are distinct), and the currency cycles through three codes.
function numberedBook(index: number): Book {
  const scrambled = (index * 7) % 23
  return {
    id: `${String(scrambled).padStart(8, '0')}-0000-4000-8000-000000000000`,
    title: `Book ${scrambled}`,
    price: `${1 + ((index * 5) % 13)}.50`,
    currency: ['EUR', 'GBP', 'USD'][index % 3],
  }
}

function fillLines(result: { current: ReturnType<typeof useBasket> }, count: number) {
  for (let index = 1; index <= count; index++) {
    expect(press(() => result.current.add(numberedBook(index)))).toBeUndefined()
  }
}

describe('adding books', () => {
  it('starts empty and stores nothing', () => {
    const { result } = mount()

    expect(result.current.lines).toEqual([])
    expect(result.current.itemCount).toBe(0)
    expect(storedText()).toBeNull()
  })

  it('keeps one line for a book added twice and set to 3, with a line total of three times the price', () => {
    const { result } = mount()

    expect(press(() => result.current.add(BOOK_A))).toBeUndefined()
    expect(press(() => result.current.add(BOOK_A))).toBeUndefined()
    expect(result.current.lines).toEqual([lineOf(BOOK_A, 2)])
    expect(press(() => result.current.setQuantity(BOOK_A.id, 3))).toBeUndefined()

    expect(result.current.lines).toEqual([lineOf(BOOK_A, 3)])
    expect(lineTotal(result.current.lines[0].price, result.current.lines[0].quantity)).toBe('23.97')
  })

  it('keeps the lines in the order the books were first added', () => {
    const { result } = mount()

    press(() => result.current.add(BOOK_B))
    press(() => result.current.add(BOOK_A))
    press(() => result.current.add(BOOK_C))

    expect(result.current.lines).toEqual([lineOf(BOOK_B, 1), lineOf(BOOK_A, 1), lineOf(BOOK_C, 1)])
  })

  it('merges an added book into its line without moving it', () => {
    const { result } = mount()
    press(() => result.current.add(BOOK_A))
    press(() => result.current.add(BOOK_B))
    press(() => result.current.add(BOOK_C))

    press(() => result.current.add(BOOK_A))

    expect(result.current.lines).toEqual([lineOf(BOOK_A, 2), lineOf(BOOK_B, 1), lineOf(BOOK_C, 1)])
  })

  it('counts items as the sum of the quantities: the same book twice is 2', () => {
    const { result } = mount()
    press(() => result.current.add(BOOK_A))
    expect(result.current.itemCount).toBe(1)

    press(() => result.current.add(BOOK_A))
    expect(result.current.itemCount).toBe(2)

    press(() => result.current.add(BOOK_B))
    press(() => result.current.setQuantity(BOOK_B.id, 4))
    expect(result.current.itemCount).toBe(6)
  })
})

describe('changing quantities', () => {
  it.each([[0], [11], [2.5], [-1], [Number.NaN]])('refuses the quantity %s from a line at 3 and keeps everything as it was', (quantity) => {
    const { result } = mount()
    press(() => result.current.add(BOOK_B))
    press(() => result.current.add(BOOK_A))
    press(() => result.current.setQuantity(BOOK_A.id, 3))
    press(() => result.current.add(BOOK_C))
    const linesBefore = result.current.lines
    const storageBefore = storedText()

    const message = press(() => result.current.setQuantity(BOOK_A.id, quantity))

    expect(message).toBe('Quantity must be between 1 and 10.')
    expect(result.current.lines).toBe(linesBefore)
    expect(result.current.lines).toEqual([lineOf(BOOK_B, 1), lineOf(BOOK_A, 3), lineOf(BOOK_C, 1)])
    expect(storedText()).toBe(storageBefore)
  })

  it.each([[1], [10]])('accepts the quantity %i from a line at 3', (quantity) => {
    const { result } = mount()
    press(() => result.current.add(BOOK_B))
    press(() => result.current.add(BOOK_A))
    press(() => result.current.setQuantity(BOOK_A.id, 3))
    press(() => result.current.add(BOOK_C))

    expect(press(() => result.current.setQuantity(BOOK_A.id, quantity))).toBeUndefined()

    expect(result.current.lines).toEqual([lineOf(BOOK_B, 1), lineOf(BOOK_A, quantity), lineOf(BOOK_C, 1)])
  })

  it('leaves the basket alone when the book has no line', () => {
    const { result } = mount()
    press(() => result.current.add(BOOK_B))
    press(() => result.current.add(BOOK_A))
    const linesBefore = result.current.lines

    expect(press(() => result.current.setQuantity(BOOK_C.id, 4))).toBeUndefined()

    expect(result.current.lines).toBe(linesBefore)
  })

  it('refuses to add a book whose line already holds 10, and keeps the line at 10', () => {
    const { result } = mount()
    press(() => result.current.add(BOOK_B))
    press(() => result.current.add(BOOK_A))
    press(() => result.current.setQuantity(BOOK_A.id, 10))
    const linesBefore = result.current.lines
    const storageBefore = storedText()

    const message = press(() => result.current.add(BOOK_A))

    expect(message).toBe('A basket line holds at most 10 copies of a book.')
    expect(result.current.lines).toBe(linesBefore)
    expect(result.current.lines).toEqual([lineOf(BOOK_B, 1), lineOf(BOOK_A, 10)])
    expect(storedText()).toBe(storageBefore)
  })

  it('accepts the add that brings a line from 9 to 10', () => {
    const { result } = mount()
    press(() => result.current.add(BOOK_A))
    press(() => result.current.setQuantity(BOOK_A.id, 9))

    expect(press(() => result.current.add(BOOK_A))).toBeUndefined()

    expect(result.current.lines).toEqual([lineOf(BOOK_A, 10)])
  })

  it('counts both adds that arrive in one batch', () => {
    const { result } = mount()
    press(() => result.current.add(BOOK_B))
    press(() => result.current.add(BOOK_A))
    press(() => result.current.add(BOOK_C))

    act(() => {
      result.current.add(BOOK_A)
      result.current.add(BOOK_A)
    })

    expect(result.current.lines).toEqual([lineOf(BOOK_B, 1), lineOf(BOOK_A, 3), lineOf(BOOK_C, 1)])
    expect(result.current.itemCount).toBe(5)
    expect(stored()).toEqual([lineOf(BOOK_B, 1), lineOf(BOOK_A, 3), lineOf(BOOK_C, 1)])
  })

  it('keeps the line at 10 when two adds in one batch start from 9', () => {
    const { result } = mount()
    press(() => result.current.add(BOOK_B))
    press(() => result.current.add(BOOK_A))
    press(() => result.current.add(BOOK_C))
    press(() => result.current.setQuantity(BOOK_A.id, 9))

    act(() => {
      result.current.add(BOOK_A)
      result.current.add(BOOK_A)
    })

    expect(result.current.lines).toEqual([lineOf(BOOK_B, 1), lineOf(BOOK_A, 10), lineOf(BOOK_C, 1)])
    expect(stored()).toEqual([lineOf(BOOK_B, 1), lineOf(BOOK_A, 10), lineOf(BOOK_C, 1)])
  })

  it('keeps 20 lines when two adds of new books arrive in one batch at 19 lines', () => {
    const { result } = mount()
    fillLines(result, 19)

    act(() => {
      result.current.add(BOOK_A)
      result.current.add(BOOK_B)
    })

    expect(result.current.lines).toHaveLength(20)
    expect(result.current.lines[19]).toEqual(lineOf(BOOK_A, 1))
    expect((stored() as unknown[]).length).toBe(20)
  })
})

describe('the number of lines', () => {
  it('accepts 20 different books, refuses the 21st and keeps 20 lines in the basket and in storage', () => {
    const { result } = mount()
    fillLines(result, 20)
    const linesBefore = result.current.lines
    const storageBefore = storedText()

    const message = press(() => result.current.add(BOOK_A))

    expect(message).toBe('The basket holds at most 20 different books.')
    expect(result.current.lines).toBe(linesBefore)
    expect(result.current.lines).toHaveLength(20)
    expect(storedText()).toBe(storageBefore)
    expect((stored() as unknown[]).length).toBe(20)
  })

  it('still adds to a book that has a line when the basket has 20 lines', () => {
    const { result } = mount()
    fillLines(result, 20)

    expect(press(() => result.current.add(numberedBook(7)))).toBeUndefined()

    expect(result.current.lines).toHaveLength(20)
    expect(result.current.lines[6]).toEqual(lineOf(numberedBook(7), 2))
  })

  it('accepts the 20th line and takes a new book again after a line is removed', () => {
    const { result } = mount()
    fillLines(result, 19)
    expect(press(() => result.current.add(BOOK_A))).toBeUndefined()
    expect(result.current.lines).toHaveLength(20)
    expect(press(() => result.current.add(BOOK_B))).toBe('The basket holds at most 20 different books.')

    press(() => result.current.remove(numberedBook(3).id))
    expect(press(() => result.current.add(BOOK_B))).toBeUndefined()

    expect(result.current.lines).toHaveLength(20)
    expect(result.current.lines[19]).toEqual(lineOf(BOOK_B, 1))
  })
})

describe('removing and clearing', () => {
  it('removes the line of the book and keeps the others in their order', () => {
    const { result } = mount()
    press(() => result.current.add(BOOK_B))
    press(() => result.current.add(BOOK_A))
    press(() => result.current.add(BOOK_C))

    press(() => result.current.remove(BOOK_A.id))

    expect(result.current.lines).toEqual([lineOf(BOOK_B, 1), lineOf(BOOK_C, 1)])
  })

  it('leaves the basket alone when the book has no line', () => {
    const { result } = mount()
    press(() => result.current.add(BOOK_B))
    press(() => result.current.add(BOOK_C))
    const linesBefore = result.current.lines

    press(() => result.current.remove(BOOK_A.id))

    expect(result.current.lines).toBe(linesBefore)
  })

  it('empties the state and the storage when the basket is cleared', () => {
    const { result } = mount()
    press(() => result.current.add(BOOK_B))
    press(() => result.current.add(BOOK_A))
    press(() => result.current.add(BOOK_C))
    expect(stored()).toHaveLength(3)

    press(() => result.current.clear())

    expect(result.current.lines).toEqual([])
    expect(result.current.itemCount).toBe(0)
    expect(storedText()).toBeNull()
  })

  it('removes the storage entry with the last line', () => {
    const { result } = mount()
    press(() => result.current.add(BOOK_A))
    expect(storedText()).not.toBeNull()

    press(() => result.current.remove(BOOK_A.id))

    expect(result.current.lines).toEqual([])
    expect(storedText()).toBeNull()
  })
})

describe('the session storage', () => {
  it('holds the lines after every change, without waiting for the basket to unmount', () => {
    const { result } = mount()

    press(() => result.current.add(BOOK_B))
    expect(stored()).toEqual([lineOf(BOOK_B, 1)])

    press(() => result.current.add(BOOK_A))
    expect(stored()).toEqual([lineOf(BOOK_B, 1), lineOf(BOOK_A, 1)])

    press(() => result.current.setQuantity(BOOK_A.id, 3))
    press(() => result.current.add(BOOK_C))
    expect(stored()).toEqual([lineOf(BOOK_B, 1), lineOf(BOOK_A, 3), lineOf(BOOK_C, 1)])

    press(() => result.current.remove(BOOK_B.id))
    expect(stored()).toEqual([lineOf(BOOK_A, 3), lineOf(BOOK_C, 1)])
  })

  it('stores each line with exactly bookId, title, price, currency and a numeric quantity', () => {
    const { result } = mount()
    press(() => result.current.add(BOOK_B))
    press(() => result.current.add(BOOK_A))
    press(() => result.current.setQuantity(BOOK_A.id, 3))
    press(() => result.current.add(BOOK_C))

    const lines = stored() as Array<Record<string, unknown>>

    expect(lines.map((line) => Object.keys(line).sort())).toEqual([
      ['bookId', 'currency', 'price', 'quantity', 'title'],
      ['bookId', 'currency', 'price', 'quantity', 'title'],
      ['bookId', 'currency', 'price', 'quantity', 'title'],
    ])
    expect(lines.map((line) => typeof line.quantity)).toEqual(['number', 'number', 'number'])
    expect(lines.map((line) => line.quantity)).toEqual([1, 3, 1])
  })

  it('gives a new basket the two lines of the one that was unmounted', () => {
    const first = mount()
    press(() => first.result.current.add(BOOK_B))
    press(() => first.result.current.add(BOOK_A))
    press(() => first.result.current.setQuantity(BOOK_A.id, 3))
    first.unmount()

    const second = mount()

    expect(second.result.current.lines).toEqual([lineOf(BOOK_B, 1), lineOf(BOOK_A, 3)])
    expect(second.result.current.itemCount).toBe(4)
  })

  it('starts from what is stored, already in the first render', () => {
    sessionStorage.setItem(
      STORAGE_KEY,
      '[{"bookId":"5b0e7a34-8c21-4d96-9f13-2a6c0e8d4b57","title":"Emma","price":"12.50","currency":"GBP","quantity":4},' +
        '{"bookId":"1a9d3c68-4e75-4b02-8a1f-7c5e2d9b0a36","title":"Dune","price":"7.99","currency":"EUR","quantity":1},' +
        '{"bookId":"7c3f1b82-6a04-4e59-b7d8-0e9a5c2f1d64","title":"Neuromancer","price":"10.00","currency":"USD","quantity":10}]',
    )
    const firstRenders: number[] = []

    const { result } = renderHook(
      () => {
        const basket = useBasket()
        firstRenders.push(basket.lines.length)
        return basket
      },
      { wrapper: BasketProvider },
    )

    expect(firstRenders[0]).toBe(3)
    expect(result.current.lines).toEqual([lineOf(BOOK_B, 4), lineOf(BOOK_A, 1), lineOf(BOOK_C, 10)])
    expect(result.current.itemCount).toBe(15)
  })

  it('keeps only the five known fields of a stored line', () => {
    sessionStorage.setItem(
      STORAGE_KEY,
      '[{"bookId":"5b0e7a34-8c21-4d96-9f13-2a6c0e8d4b57","title":"Emma","price":"12.50","currency":"GBP","quantity":4,"note":"x"},' +
        '{"bookId":"1a9d3c68-4e75-4b02-8a1f-7c5e2d9b0a36","title":"Dune","price":"7.99","currency":"EUR","quantity":1,"extra":true},' +
        '{"bookId":"7c3f1b82-6a04-4e59-b7d8-0e9a5c2f1d64","title":"Neuromancer","price":"10.00","currency":"USD","quantity":10,"note":"y"}]',
    )

    const { result } = mount()

    expect(result.current.lines).toEqual([lineOf(BOOK_B, 4), lineOf(BOOK_A, 1), lineOf(BOOK_C, 10)])
    expect(result.current.lines.map((line) => Object.keys(line).sort())).toEqual([
      ['bookId', 'currency', 'price', 'quantity', 'title'],
      ['bookId', 'currency', 'price', 'quantity', 'title'],
      ['bookId', 'currency', 'price', 'quantity', 'title'],
    ])
  })

  it.each([
    ['text that is not JSON', '{not json'],
    ['an object instead of a list', '{}'],
    ['null', 'null'],
    ['a list that holds a number', '[1]'],
    ['a list that holds null', '[null]'],
  ])('gives an empty basket for %s', (_name, text) => {
    sessionStorage.setItem(STORAGE_KEY, text)

    const { result } = mount()

    expect(result.current.lines).toEqual([])
    expect(result.current.itemCount).toBe(0)
  })

  it.each([
    ['null', null],
    ['a line without a title', { bookId: BOOK_C.id, price: '10.00', currency: 'USD', quantity: 1 }],
    ['a title that is a number', { bookId: BOOK_C.id, title: 5, price: '10.00', currency: 'USD', quantity: 1 }],
    ['a book id that is a number', { bookId: 7, title: 'Neuromancer', price: '10.00', currency: 'USD', quantity: 1 }],
    ['a price that is no amount of money', { bookId: BOOK_C.id, title: 'Neuromancer', price: 'free', currency: 'USD', quantity: 1 }],
    ['a price that is a number', { bookId: BOOK_C.id, title: 'Neuromancer', price: 10, currency: 'USD', quantity: 1 }],
    ['a currency that is a number', { bookId: BOOK_C.id, title: 'Neuromancer', price: '10.00', currency: 3, quantity: 1 }],
    ['a quantity of 0', { bookId: BOOK_C.id, title: 'Neuromancer', price: '10.00', currency: 'USD', quantity: 0 }],
    ['a quantity of 11', { bookId: BOOK_C.id, title: 'Neuromancer', price: '10.00', currency: 'USD', quantity: 11 }],
    ['a quantity of 2.5', { bookId: BOOK_C.id, title: 'Neuromancer', price: '10.00', currency: 'USD', quantity: 2.5 }],
    ['a quantity written as text', { bookId: BOOK_C.id, title: 'Neuromancer', price: '10.00', currency: 'USD', quantity: '2' }],
    ['the book of the first line again', { bookId: BOOK_B.id, title: 'Emma', price: '12.50', currency: 'GBP', quantity: 2 }],
  ])('gives an empty basket, not a part of it, for a list with %s', (_name, bad) => {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify([lineOf(BOOK_B, 2), bad, lineOf(BOOK_A, 1)]))

    const { result } = mount()

    expect(result.current.lines).toEqual([])
  })

  it('gives an empty basket for a stored list of 21 lines, and accepts 20', () => {
    const lines = (count: number) =>
      Array.from({ length: count }, (_, index) => lineOf(numberedBook(index + 1), 1))
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(lines(20)))
    expect(mount().result.current.lines).toHaveLength(20)

    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(lines(21)))
    expect(mount().result.current.lines).toEqual([])
  })

  it('works with an empty basket when the browser refuses to read the storage', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('Access denied', 'SecurityError')
    })

    const { result } = mount()

    expect(result.current.lines).toEqual([])
  })

  it('keeps working when the browser refuses to write the storage', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('Quota exceeded', 'QuotaExceededError')
    })
    const { result } = mount()

    expect(press(() => result.current.add(BOOK_A))).toBeUndefined()

    expect(result.current.lines).toEqual([lineOf(BOOK_A, 1)])
  })

  it('keeps working when the browser refuses to remove the storage entry', () => {
    const { result } = mount()
    press(() => result.current.add(BOOK_A))
    vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => {
      throw new DOMException('Access denied', 'SecurityError')
    })

    press(() => result.current.clear())

    expect(result.current.lines).toEqual([])
  })
})

describe('useBasket', () => {
  it('refuses to work outside a BasketProvider', () => {
    expect(() => renderHook(() => useBasket())).toThrow('useBasket must be used inside a BasketProvider')
    // React reports the error it rethrows to the console as well.
    vi.mocked(console.error).mockClear()
  })
})

describe('placing an order', () => {
  const PLACED_ID = '3d6b0f91-2e58-4a74-8c13-9b5e7a0d2f46'

  function deferred<T>() {
    let resolve!: (value: T) => void
    const promise = new Promise<T>((done) => {
      resolve = done
    })
    return { promise, resolve }
  }

  // A basket of three lines in the order B, A, C, mounted, with the bookstore held back until the test answers. The
  // first order is answered by `held`; any later order is never answered.
  function mountWithOrder() {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify([lineOf(BOOK_B, 2), lineOf(BOOK_A, 3), lineOf(BOOK_C, 1)]))
    const held = deferred<Response>()
    const answers = [held.promise]
    const mock = mockFetch([
      {
        method: 'POST',
        path: '/orders/commands/place',
        respond: () => answers.shift() ?? new Promise<Response>(() => {}),
      },
    ])
    return { ...mount(), held, mock }
  }

  async function answer(held: { resolve: (value: Response) => void }, response: Response) {
    await act(async () => {
      held.resolve(response)
      await Promise.resolve()
    })
  }

  it('sends one request for two calls in the same tick and says it is placing', () => {
    const { result, mock } = mountWithOrder()

    act(() => {
      result.current.placeOrder('reader@example.com')
      result.current.placeOrder('reader@example.com')
    })

    expect(mock.calls.map((call) => `${call.method} ${call.path}`)).toEqual(['POST /orders/commands/place'])
    expect(result.current.placing).toBe(true)
    expect(result.current.orderProblem).toBeUndefined()
  })

  it('removes the lines of the submitted books whatever happened to them meanwhile, and keeps the lines of other books', async () => {
    const { result, held } = mountWithOrder()
    act(() => result.current.placeOrder('reader@example.com'))
    press(() => result.current.remove(BOOK_B.id))
    expect(press(() => result.current.setQuantity(BOOK_A.id, 7))).toBeUndefined()
    expect(press(() => result.current.add(numberedBook(1)))).toBeUndefined()
    expect(result.current.lines).toEqual([lineOf(BOOK_A, 7), lineOf(BOOK_C, 1), lineOf(numberedBook(1), 1)])

    await answer(held, jsonResponse({ id: PLACED_ID }, 202))

    expect(result.current.lines).toEqual([lineOf(numberedBook(1), 1)])
    expect(stored()).toEqual([lineOf(numberedBook(1), 1)])
    expect(result.current.placing).toBe(false)
    expect(window.location.hash).toBe(`#/orders/${PLACED_ID}`)
  })

  it('keeps the lines and the stored text after an error answer, reports it and takes it away when the next order is sent', async () => {
    const { result, held } = mountWithOrder()
    const before = storedText()
    act(() => result.current.placeOrder('reader@example.com'))

    await answer(held, problemResponse(409, { title: 'Insufficient stock', code: 'insufficient-stock', details: [] }))

    expect(result.current.placing).toBe(false)
    expect(result.current.orderProblem?.status).toBe(409)
    expect(result.current.orderProblem?.title).toBe('Insufficient stock')
    expect(result.current.lines).toEqual([lineOf(BOOK_B, 2), lineOf(BOOK_A, 3), lineOf(BOOK_C, 1)])
    expect(storedText()).toBe(before)

    act(() => result.current.placeOrder('reader@example.com'))

    expect(result.current.placing).toBe(true)
    expect(result.current.orderProblem).toBeUndefined()
  })

  it('sends the next order after an accepted one, with only the lines of that moment', async () => {
    const { result, held, mock } = mountWithOrder()
    act(() => result.current.placeOrder('reader@example.com'))

    await answer(held, jsonResponse({ id: PLACED_ID }, 202))

    expect(result.current.placing).toBe(false)
    expect(press(() => result.current.add(BOOK_C))).toBeUndefined()
    act(() => result.current.placeOrder('reader@example.com'))

    const placed = mock.calls.filter((call) => call.method === 'POST' && call.path === '/orders/commands/place')
    expect(placed).toHaveLength(2)
    expect(JSON.parse(placed[1].body ?? 'null')).toEqual({
      customerEmail: 'reader@example.com',
      lines: [{ bookId: BOOK_C.id, quantity: 1 }],
    })
    expect(result.current.placing).toBe(true)
  })
})
