import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { BookDetailView, StockView } from '../api/types'
import { BasketProvider } from '../state/basket'
import { jsonResponse, mockFetch, problemResponse } from '../test/fetchMock'
import type { MockRoute } from '../test/fetchMock'
import { BookDetailPage } from './BookDetailPage'

const BOOK_ID = '3e9a5b17-0d42-4c86-b1f3-8a7d6e2c9054'
const OTHER_ID = '6a1c7d28-5f93-4e04-9b6a-0c8e3d1f7a52'
const STORAGE_KEY = 'openbook.basket'

// The authors are not in alphabetical order by name, by surname or by the length of the name, so a page that sorts them
// cannot pass. The detail says 4 copies are available while the stock answer says 7: the page has to take the number
// from the stock answer.
function bookView(overrides: Partial<BookDetailView> = {}): BookDetailView {
  return {
    id: BOOK_ID,
    isbn: '9780262035613',
    title: 'Deep Learning',
    authors: ['Ian Goodfellow', 'Yoshua Bengio', 'Aaron Courville'],
    genre: 'TECHNOLOGY',
    price: '19.99',
    currency: 'EUR',
    publicationYear: 2016,
    status: 'AVAILABLE',
    available: 4,
    inStock: true,
    ...overrides,
  }
}

function stockView(overrides: Partial<StockView> = {}): StockView {
  return { bookId: BOOK_ID, onHand: 9, reserved: 2, available: 7, reorderLevel: 3, lowStock: false, ...overrides }
}

function routes(book: BookDetailView, stock: StockView, id = BOOK_ID): MockRoute[] {
  return [
    { method: 'GET', path: `/books/${id}`, respond: () => jsonResponse(book) },
    { method: 'GET', path: `/inventory/${id}`, respond: () => jsonResponse(stock) },
  ]
}

function page(id = BOOK_ID) {
  return (
    <BasketProvider>
      <BookDetailPage id={id} />
    </BasketProvider>
  )
}

function addButton() {
  return screen.getByRole('button', { name: 'Add to basket' })
}

function storedLines(): unknown {
  const text = sessionStorage.getItem(STORAGE_KEY)
  return text === null ? null : JSON.parse(text)
}

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((done) => {
    resolve = done
  })
  return { promise, resolve }
}

describe('BookDetailPage content', () => {
  it('shows the title, the ISBN, the authors as a list in the order sent, the year, the price and the genre', async () => {
    const mock = mockFetch(routes(bookView(), stockView()))

    render(page())

    expect(await screen.findByRole('heading', { level: 1, name: 'Deep Learning' })).toBeInTheDocument()
    expect(screen.getByText('9780262035613')).toBeInTheDocument()
    const authors = screen.getByRole('list')
    expect(within(authors).getAllByRole('listitem').map((item) => item.textContent)).toEqual([
      'Ian Goodfellow',
      'Yoshua Bengio',
      'Aaron Courville',
    ])
    expect(screen.getByText('2016')).toBeInTheDocument()
    expect(screen.getByText('19.99 EUR')).toBeInTheDocument()
    expect(screen.getByText('TECHNOLOGY')).toBeInTheDocument()
    expect([...mock.calls.map((call) => call.path)].sort()).toEqual([`/books/${BOOK_ID}`, `/inventory/${BOOK_ID}`])
  })

  it('shows the price with the currency the bookstore sent, the genre as sent and the year', async () => {
    mockFetch(routes(bookView({ price: '7.99', currency: 'GBP', genre: 'SCIENCE_FICTION', publicationYear: 1984 }), stockView()))

    render(page())

    expect(await screen.findByText('7.99 GBP')).toBeInTheDocument()
    expect(screen.getByText('SCIENCE_FICTION')).toBeInTheDocument()
    expect(screen.getByText('1984')).toBeInTheDocument()
    expect(screen.queryByText('EUR')).not.toBeInTheDocument()
  })

  it.each([
    ['12.50', 'USD'],
    ['10.00', 'GBP'],
  ])('shows the price %s with its trailing zero verbatim, followed by the currency %s', async (price, currency) => {
    mockFetch(routes(bookView({ price, currency }), stockView()))

    render(page())

    expect(await screen.findByText(`${price} ${currency}`)).toBeInTheDocument()
  })

  it('shows a link back to the catalogue', async () => {
    mockFetch(routes(bookView(), stockView()))

    render(page())

    await screen.findByRole('heading', { level: 1, name: 'Deep Learning' })
    expect(screen.getByRole('link', { name: 'Back to the catalogue' })).toHaveAttribute('href', '#/')
  })

  it('shows a loading message under the heading Book until the answers arrive', async () => {
    const book = deferred<Response>()
    const stock = deferred<Response>()
    mockFetch([
      { method: 'GET', path: `/books/${BOOK_ID}`, respond: () => book.promise },
      { method: 'GET', path: `/inventory/${BOOK_ID}`, respond: () => stock.promise },
    ])

    render(page())

    expect(screen.getByRole('heading', { level: 1, name: 'Book' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Loading book')
    expect(screen.queryByRole('button', { name: 'Add to basket' })).not.toBeInTheDocument()

    book.resolve(jsonResponse(bookView()))
    stock.resolve(jsonResponse(stockView()))

    expect(await screen.findByRole('heading', { level: 1, name: 'Deep Learning' })).toBeInTheDocument()
    expect(screen.queryByText('Loading book')).not.toBeInTheDocument()
  })
})

describe('BookDetailPage stock level', () => {
  it('shows the available number of the stock answer, not the one of the book', async () => {
    mockFetch(routes(bookView({ available: 4 }), stockView({ available: 7 })))

    render(page())

    expect(await screen.findByText('7 available')).toBeInTheDocument()
    expect(screen.queryByText('4 available')).not.toBeInTheDocument()
    expect(screen.getByText('In stock')).toBeInTheDocument()
    expect(addButton()).toBeEnabled()
  })

  it('offers the book when the stock has copies although the book says there are none', async () => {
    mockFetch(routes(bookView({ available: 0, inStock: false }), stockView({ available: 7 })))

    render(page())

    expect(await screen.findByText('7 available')).toBeInTheDocument()
    expect(addButton()).toBeEnabled()
    expect(screen.queryByText('Out of stock')).not.toBeInTheDocument()
  })

  it.each([[false], [true]])('shows Out of stock and a disabled button for no copies, with low stock %s', async (lowStock) => {
    mockFetch(routes(bookView({ available: 4 }), stockView({ available: 0, lowStock })))

    render(page())

    expect(await screen.findByText('Out of stock')).toBeInTheDocument()
    expect(addButton()).toBeDisabled()
    expect(screen.queryByText('In stock')).not.toBeInTheDocument()
    expect(screen.queryByText(/available/)).not.toBeInTheDocument()
    expect(screen.queryByText('Low stock')).not.toBeInTheDocument()
  })

  it('shows Discontinued and a disabled button for a discontinued book although 5 copies are available', async () => {
    mockFetch(routes(bookView({ status: 'DISCONTINUED' }), stockView({ available: 5 })))

    render(page())

    expect(await screen.findByText('Discontinued')).toBeInTheDocument()
    expect(addButton()).toBeDisabled()
    expect(screen.queryByText('5 available')).not.toBeInTheDocument()
    expect(screen.queryByText('Out of stock')).not.toBeInTheDocument()
    expect(screen.queryByText('In stock')).not.toBeInTheDocument()
  })

  it('shows Discontinued, not Out of stock, for a discontinued book without copies', async () => {
    mockFetch(routes(bookView({ status: 'DISCONTINUED' }), stockView({ available: 0 })))

    render(page())

    expect(await screen.findByText('Discontinued')).toBeInTheDocument()
    expect(addButton()).toBeDisabled()
    expect(screen.queryByText('Out of stock')).not.toBeInTheDocument()
  })

  it('keeps the button enabled and says Low stock when only 2 copies are available', async () => {
    mockFetch(routes(bookView(), stockView({ available: 2, lowStock: true })))

    render(page())

    expect(await screen.findByText('2 available')).toBeInTheDocument()
    expect(screen.getByText('Low stock')).toBeInTheDocument()
    expect(addButton()).toBeEnabled()
  })

  it('does not say Low stock when the stock answer does not', async () => {
    mockFetch(routes(bookView(), stockView({ available: 1, lowStock: false })))

    render(page())

    expect(await screen.findByText('1 available')).toBeInTheDocument()
    expect(screen.queryByText('Low stock')).not.toBeInTheDocument()
  })
})

describe('BookDetailPage errors', () => {
  it('shows the alert of the bookstore when the book does not exist, and no button', async () => {
    mockFetch([
      {
        method: 'GET',
        path: `/books/${BOOK_ID}`,
        respond: () =>
          problemResponse(404, {
            title: 'Book not found',
            detail: `No book with id ${BOOK_ID}`,
            code: 'book-not-found',
            details: [],
          }),
      },
      { method: 'GET', path: `/inventory/${BOOK_ID}`, respond: () => jsonResponse(stockView()) },
    ])

    render(page())

    const alert = await screen.findByRole('alert')
    expect(within(alert).getByText('Book not found')).toBeInTheDocument()
    expect(within(alert).getByText(`No book with id ${BOOK_ID}`)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Add to basket' })).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Book' })).toBeInTheDocument()
  })

  it('shows the alert and no button when the stock answer is a 404 although the book exists', async () => {
    mockFetch([
      { method: 'GET', path: `/books/${BOOK_ID}`, respond: () => jsonResponse(bookView()) },
      {
        method: 'GET',
        path: `/inventory/${BOOK_ID}`,
        respond: () =>
          problemResponse(404, {
            title: 'Book not found',
            detail: `No stock item for the book with id ${BOOK_ID}`,
            code: 'book-not-found',
            details: [],
          }),
      },
    ])

    render(page())

    const alert = await screen.findByRole('alert')
    expect(within(alert).getByText(`No stock item for the book with id ${BOOK_ID}`)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Add to basket' })).not.toBeInTheDocument()
    expect(screen.queryByText('Deep Learning')).not.toBeInTheDocument()
  })

  it('shows the alert for a bookstore that cannot be reached', async () => {
    const down = () => {
      throw new Error('connection refused')
    }
    const mock = mockFetch([
      { method: 'GET', path: `/books/${BOOK_ID}`, respond: down },
      { method: 'GET', path: `/inventory/${BOOK_ID}`, respond: down },
    ])

    render(page())

    const alert = await screen.findByRole('alert')
    expect(within(alert).getByText('Cannot reach the bookstore')).toBeInTheDocument()
    expect([...mock.calls.map((call) => call.path)].sort()).toEqual([`/books/${BOOK_ID}`, `/inventory/${BOOK_ID}`])
  })

  it('shows a generic alert when the request fails in a way the API client does not report as a problem', async () => {
    // An answer without headers makes the client itself throw a TypeError instead of an ApiError.
    vi.stubGlobal('fetch', async () => ({}))

    render(page())

    const alert = await screen.findByRole('alert')
    expect(within(alert).getByText('Something went wrong')).toBeInTheDocument()
  })
})

describe('BookDetailPage adding to the basket', () => {
  it('adds the line, stores it and confirms with a link to the basket', async () => {
    mockFetch(routes(bookView(), stockView()))
    const user = userEvent.setup()
    render(page())
    await screen.findByRole('heading', { level: 1, name: 'Deep Learning' })
    expect(screen.queryByRole('status')).not.toBeInTheDocument()

    await user.click(addButton())

    const confirmation = await screen.findByRole('status')
    expect(confirmation).toHaveTextContent('Added to your basket.')
    expect(within(confirmation).getByRole('link', { name: 'View basket' })).toHaveAttribute('href', '#/basket')
    expect(storedLines()).toEqual([
      { bookId: BOOK_ID, title: 'Deep Learning', price: '19.99', currency: 'EUR', quantity: 1 },
    ])
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it.each([
    ['12.50', 'USD'],
    ['10.00', 'GBP'],
  ])('hands the price %s to the basket verbatim, with its trailing zero', async (price, currency) => {
    mockFetch(routes(bookView({ price, currency }), stockView()))
    const user = userEvent.setup()
    render(page())
    await screen.findByRole('heading', { level: 1, name: 'Deep Learning' })

    await user.click(addButton())

    expect(await screen.findByRole('status')).toHaveTextContent('Added to your basket.')
    expect(storedLines()).toEqual([{ bookId: BOOK_ID, title: 'Deep Learning', price, currency, quantity: 1 }])
  })

  it('adds one more copy of the same book to its line on a second click', async () => {
    mockFetch(routes(bookView({ price: '7.99', currency: 'GBP' }), stockView()))
    const user = userEvent.setup()
    render(page())
    await screen.findByRole('heading', { level: 1, name: 'Deep Learning' })

    await user.click(addButton())
    await user.click(addButton())

    expect(storedLines()).toEqual([
      { bookId: BOOK_ID, title: 'Deep Learning', price: '7.99', currency: 'GBP', quantity: 2 },
    ])
  })

  it('shows the message of the basket and keeps the quantity at 10 when the line is full', async () => {
    sessionStorage.setItem(
      STORAGE_KEY,
      `[{"bookId":"${OTHER_ID}","title":"Dune","price":"12.50","currency":"USD","quantity":3},` +
        `{"bookId":"${BOOK_ID}","title":"Deep Learning","price":"19.99","currency":"EUR","quantity":10},` +
        '{"bookId":"7b4d8e53-9a17-4c62-8f05-0e3a1d6c2b94","title":"Emma","price":"14.00","currency":"GBP","quantity":1}]',
    )
    mockFetch(routes(bookView(), stockView()))
    const user = userEvent.setup()
    render(page())
    await screen.findByRole('heading', { level: 1, name: 'Deep Learning' })

    await user.click(addButton())

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('A basket line holds at most 10 copies of a book.')
    expect(screen.queryByText('Added to your basket.')).not.toBeInTheDocument()
    expect((storedLines() as Array<{ quantity: number }>).map((line) => line.quantity)).toEqual([3, 10, 1])
  })

  it('replaces the confirmation with the message when the next click is refused', async () => {
    sessionStorage.setItem(
      STORAGE_KEY,
      `[{"bookId":"${BOOK_ID}","title":"Deep Learning","price":"19.99","currency":"EUR","quantity":9}]`,
    )
    mockFetch(routes(bookView(), stockView()))
    const user = userEvent.setup()
    render(page())
    await screen.findByRole('heading', { level: 1, name: 'Deep Learning' })

    await user.click(addButton())
    expect(await screen.findByRole('status')).toHaveTextContent('Added to your basket.')
    await user.click(addButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('A basket line holds at most 10 copies of a book.')
    expect(screen.queryByText('Added to your basket.')).not.toBeInTheDocument()
  })

  it('shows the message of the basket when it already holds 20 other books', async () => {
    // Ids, titles and prices are not in the order of the index (7 is coprime to 23, so the ids are distinct).
    const lines = Array.from({ length: 20 }, (_, index) => ({
      bookId: `${String(((index + 1) * 7) % 23).padStart(8, '0')}-0000-4000-8000-000000000000`,
      title: `Book ${((index + 1) * 7) % 23}`,
      price: `${1 + (((index + 1) * 5) % 13)}.50`,
      currency: ['EUR', 'GBP', 'USD'][(index + 1) % 3],
      quantity: 1,
    }))
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(lines))
    mockFetch(routes(bookView(), stockView()))
    const user = userEvent.setup()
    render(page())
    await screen.findByRole('heading', { level: 1, name: 'Deep Learning' })

    await user.click(addButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('The basket holds at most 20 different books.')
    expect(storedLines()).toEqual(lines)
  })

  it('has no clickable button for an out of stock book, so nothing is added', async () => {
    mockFetch(routes(bookView(), stockView({ available: 0 })))
    const user = userEvent.setup()
    render(page())
    await screen.findByText('Out of stock')

    await user.click(addButton())

    expect(storedLines()).toBeNull()
    expect(screen.queryByText('Added to your basket.')).not.toBeInTheDocument()
  })
})

describe('BookDetailPage when the book in the address changes', () => {
  // The answers of book A are held back by hand, the answers of book B arrive at once.
  function setupTwoBooks(lateBook: Response, lateStock: Response) {
    const heldBook = deferred<Response>()
    const heldStock = deferred<Response>()
    mockFetch([
      { method: 'GET', path: `/books/${BOOK_ID}`, respond: () => heldBook.promise },
      { method: 'GET', path: `/inventory/${BOOK_ID}`, respond: () => heldStock.promise },
      ...routes(
        bookView({ id: OTHER_ID, title: 'Neuromancer', authors: ['William Gibson'], price: '12.50', currency: 'USD' }),
        stockView({ bookId: OTHER_ID, available: 3 }),
        OTHER_ID,
      ),
    ])
    return {
      release: () => {
        heldBook.resolve(lateBook)
        heldStock.resolve(lateStock)
      },
    }
  }

  async function flush() {
    await act(async () => {
      for (let turn = 0; turn < 50; turn++) await Promise.resolve()
    })
  }

  it('shows the loading message for the new book instead of the old one, then the new book', async () => {
    const heldBook = deferred<Response>()
    const heldStock = deferred<Response>()
    mockFetch([
      ...routes(bookView(), stockView()),
      { method: 'GET', path: `/books/${OTHER_ID}`, respond: () => heldBook.promise },
      { method: 'GET', path: `/inventory/${OTHER_ID}`, respond: () => heldStock.promise },
    ])
    const view = render(page())
    expect(await screen.findByRole('heading', { level: 1, name: 'Deep Learning' })).toBeInTheDocument()

    view.rerender(page(OTHER_ID))

    expect(screen.getByRole('heading', { level: 1, name: 'Book' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Loading book')
    expect(screen.queryByText('Deep Learning')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Add to basket' })).not.toBeInTheDocument()

    heldBook.resolve(
      jsonResponse(bookView({ id: OTHER_ID, title: 'Neuromancer', authors: ['William Gibson'], price: '12.50', currency: 'USD' })),
    )
    heldStock.resolve(jsonResponse(stockView({ bookId: OTHER_ID, available: 3 })))
    expect(await screen.findByRole('heading', { level: 1, name: 'Neuromancer' })).toBeInTheDocument()
    expect(screen.getByText('3 available')).toBeInTheDocument()
    expect(screen.queryByText('Deep Learning')).not.toBeInTheDocument()
  })

  it('does not show the confirmation of the old book on the new one', async () => {
    mockFetch([
      ...routes(bookView(), stockView()),
      ...routes(bookView({ id: OTHER_ID, title: 'Neuromancer' }), stockView({ bookId: OTHER_ID, available: 3 }), OTHER_ID),
    ])
    const user = userEvent.setup()
    const view = render(page())
    await screen.findByRole('heading', { level: 1, name: 'Deep Learning' })
    await user.click(addButton())
    expect(await screen.findByRole('status')).toHaveTextContent('Added to your basket.')

    view.rerender(page(OTHER_ID))

    expect(await screen.findByRole('heading', { level: 1, name: 'Neuromancer' })).toBeInTheDocument()
    expect(screen.queryByText('Added to your basket.')).not.toBeInTheDocument()
    expect(addButton()).toBeEnabled()
  })

  it('keeps the new book when the answers of the old one arrive late', async () => {
    const lateBook = jsonResponse(bookView())
    const bodyRead = vi.spyOn(lateBook, 'text')
    const held = setupTwoBooks(lateBook, jsonResponse(stockView()))
    const view = render(page())
    view.rerender(page(OTHER_ID))
    expect(await screen.findByRole('heading', { level: 1, name: 'Neuromancer' })).toBeInTheDocument()

    held.release()
    await waitFor(() => expect(bodyRead).toHaveBeenCalled())
    await flush()

    expect(screen.getByRole('heading', { level: 1, name: 'Neuromancer' })).toBeInTheDocument()
    expect(screen.queryByText('Deep Learning')).not.toBeInTheDocument()
    expect(screen.getByText('3 available')).toBeInTheDocument()
  })

  it('keeps the new book when the failure of the old one arrives late', async () => {
    const lateFailure = problemResponse(404, {
      title: 'Book not found',
      detail: `No book with id ${BOOK_ID}`,
      code: 'book-not-found',
      details: [],
    })
    const bodyRead = vi.spyOn(lateFailure, 'text')
    const held = setupTwoBooks(lateFailure, jsonResponse(stockView()))
    const view = render(page())
    view.rerender(page(OTHER_ID))
    expect(await screen.findByRole('heading', { level: 1, name: 'Neuromancer' })).toBeInTheDocument()

    held.release()
    await waitFor(() => expect(bodyRead).toHaveBeenCalled())
    await flush()

    expect(screen.getByRole('heading', { level: 1, name: 'Neuromancer' })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(screen.getByText('3 available')).toBeInTheDocument()
  })
})
