import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import App from './App.tsx'
import { jsonResponse, mockFetch } from './test/fetchMock'

const BOOK_ID = '5d8e2a14-7c03-4b9f-a1e6-3f0c9d4b7a28'
const ORDER_ID = '2b7f4e60-9a15-4d3c-8e08-6c1a5f9d0b73'

// The catalogue page asks for the list of books on every visit. An empty list is enough where the shell is under test.
function answerCatalogue() {
  mockFetch([{ method: 'GET', path: '/books', respond: () => jsonResponse([]) }])
}

function activeNavigation(): Array<string | null> {
  const navigation = screen.getByRole('navigation', { name: 'Main' })
  return within(navigation)
    .getAllByRole('link')
    .filter((link) => link.getAttribute('aria-current') === 'page')
    .map((link) => link.textContent)
}

describe('the wordmark', () => {
  it('is a home link named OpenBook with Open and Book in two spans of their own classes', async () => {
    answerCatalogue()

    render(<App />)

    expect(await screen.findByText('No books match your search.')).toBeInTheDocument()
    const home = within(screen.getByRole('banner')).getByRole('link', { name: 'OpenBook' })
    expect(home).toHaveAttribute('href', '#/')
    expect(within(home).getByText('Open')).toHaveClass('wordmark')
    expect(within(home).getByText('Book')).toHaveClass('wordmark-book')
    expect(within(home).getByText('Open')).not.toHaveClass('wordmark-book')
    expect(within(home).getByText('Book')).not.toHaveClass('wordmark')
  })

  it('stays in the header when another page is opened', async () => {
    answerCatalogue()
    const user = userEvent.setup()
    render(<App />)
    expect(await screen.findByText('No books match your search.')).toBeInTheDocument()
    expect(within(screen.getByRole('banner')).getByRole('link', { name: 'OpenBook' })).toBeInTheDocument()

    await user.click(screen.getByRole('link', { name: 'Basket' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Basket' })).toBeInTheDocument()
    expect(within(screen.getByRole('banner')).getByRole('link', { name: 'OpenBook' })).toHaveAttribute('href', '#/')
  })

  it('takes the visitor back to the catalogue when it is clicked', async () => {
    window.location.hash = '#/assistant'
    answerCatalogue()
    const user = userEvent.setup()
    render(<App />)
    expect(screen.getByRole('heading', { level: 1, name: 'Assistant' })).toBeInTheDocument()

    await user.click(screen.getByRole('link', { name: 'OpenBook' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Catalogue' })).toBeInTheDocument()
    expect(await screen.findByText('No books match your search.')).toBeInTheDocument()
    expect(window.location.hash).toBe('#/')
  })
})

describe('the navigation', () => {
  it('lists Catalogue, Basket, Orders and Assistant with their hashes', async () => {
    answerCatalogue()

    render(<App />)

    expect(await screen.findByText('No books match your search.')).toBeInTheDocument()
    const links = within(screen.getByRole('navigation', { name: 'Main' })).getAllByRole('link')
    expect(links.map((link) => [link.textContent, link.getAttribute('href')])).toEqual([
      ['Catalogue', '#/'],
      ['Basket', '#/basket'],
      ['Orders', '#/orders'],
      ['Assistant', '#/assistant'],
    ])
  })

  it.each([
    ['', 'Catalogue'],
    ['#/', 'Catalogue'],
  ])('marks Catalogue as the current page for the hash %j', async (hash, heading) => {
    window.location.hash = hash
    answerCatalogue()

    render(<App />)

    expect(await screen.findByRole('heading', { level: 1, name: heading })).toBeInTheDocument()
    expect(await screen.findByText('No books match your search.')).toBeInTheDocument()
    expect(activeNavigation()).toEqual(['Catalogue'])
  })

  it.each([
    [`#/books/${BOOK_ID}`, 'Book', 'Catalogue'],
    ['#/basket', 'Basket', 'Basket'],
    ['#/checkout', 'Checkout', 'Basket'],
    ['#/orders', 'Orders', 'Orders'],
    [`#/orders/${ORDER_ID}`, 'Order', 'Orders'],
    ['#/assistant', 'Assistant', 'Assistant'],
  ])('opens the hash %j with the heading %s and the current mark on %s alone', (hash, heading, active) => {
    window.location.hash = hash
    // The book page asks for its book and its stock and gets no answer, so the test ends before anything changes.
    mockFetch([
      { method: 'GET', path: `/books/${BOOK_ID}`, respond: () => new Promise<Response>(() => {}) },
      { method: 'GET', path: `/inventory/${BOOK_ID}`, respond: () => new Promise<Response>(() => {}) },
    ])

    render(<App />)

    expect(screen.getByRole('heading', { level: 1, name: heading })).toBeInTheDocument()
    expect(activeNavigation()).toEqual([active])
  })

  it('marks nothing as current on a page that does not exist, and keeps the header and the way home', () => {
    window.location.hash = '#/nowhere'

    render(<App />)

    expect(screen.getByRole('heading', { level: 1, name: 'Page not found' })).toBeInTheDocument()
    expect(activeNavigation()).toEqual([])
    expect(within(screen.getByRole('banner')).getByRole('link', { name: 'OpenBook' })).toBeInTheDocument()
    expect(within(screen.getByRole('main')).getByRole('link', { name: 'Back to the catalogue' })).toHaveAttribute('href', '#/')
  })

  it('moves the current mark when a navigation link is clicked', async () => {
    answerCatalogue()
    const user = userEvent.setup()
    render(<App />)
    expect(await screen.findByText('No books match your search.')).toBeInTheDocument()
    expect(activeNavigation()).toEqual(['Catalogue'])

    await user.click(screen.getByRole('link', { name: 'Orders' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Orders' })).toBeInTheDocument()
    expect(activeNavigation()).toEqual(['Orders'])
  })
})

// The detail page asks for the book and for its stock. These answers are the ones of a book that can be ordered.
function answerBook(stockAvailable = 7) {
  return mockFetch([
    {
      method: 'GET',
      path: `/books/${BOOK_ID}`,
      respond: () =>
        jsonResponse({
          id: BOOK_ID,
          isbn: '9780441569595',
          title: 'Neuromancer',
          authors: ['William Gibson', 'Bruce Sterling', 'Neal Stephenson'],
          genre: 'SCIENCE_FICTION',
          price: '10.00',
          currency: 'USD',
          publicationYear: 1984,
          status: 'AVAILABLE',
          available: 2,
          inStock: true,
        }),
    },
    {
      method: 'GET',
      path: `/inventory/${BOOK_ID}`,
      respond: () =>
        jsonResponse({ bookId: BOOK_ID, onHand: 9, reserved: 2, available: stockAvailable, reorderLevel: 3, lowStock: false }),
    },
  ])
}

function basketBadge() {
  return within(screen.getByRole('banner')).getByRole('status', { name: 'Items in basket' })
}

describe('the book page', () => {
  it('opens as a deep link: the hash of a book is set before the app starts and the page shows that book', async () => {
    window.location.hash = `#/books/${BOOK_ID}`
    const mock = answerBook()

    render(<App />)

    expect(await screen.findByRole('heading', { level: 1, name: 'Neuromancer' })).toBeInTheDocument()
    expect(within(screen.getByRole('main')).getByText('9780441569595')).toBeInTheDocument()
    expect(await screen.findByText('7 available')).toBeInTheDocument()
    expect(activeNavigation()).toEqual(['Catalogue'])
    expect([...mock.calls.map((call) => call.path)].sort()).toEqual([`/books/${BOOK_ID}`, `/inventory/${BOOK_ID}`])
  })

  it('opens the book that a catalogue link points to', async () => {
    window.location.hash = '#/'
    mockFetch([
      {
        method: 'GET',
        path: '/books',
        respond: () =>
          jsonResponse([
            {
              id: BOOK_ID,
              isbn: '9780441569595',
              title: 'Neuromancer',
              authors: 'William Gibson',
              genre: 'SCIENCE_FICTION',
              price: '10.00',
              currency: 'USD',
              status: 'AVAILABLE',
              available: 2,
              inStock: true,
            },
          ]),
      },
      // The answers of the book page never arrive in this test.
      { method: 'GET', path: `/books/${BOOK_ID}`, respond: () => new Promise<Response>(() => {}) },
      { method: 'GET', path: `/inventory/${BOOK_ID}`, respond: () => new Promise<Response>(() => {}) },
    ])
    const user = userEvent.setup()
    render(<App />)

    await user.click(await screen.findByRole('link', { name: 'Neuromancer' }))

    expect(window.location.hash).toBe(`#/books/${BOOK_ID}`)
    expect(await screen.findByRole('heading', { level: 1, name: 'Book' })).toBeInTheDocument()
    expect(screen.getByText('Loading book')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Catalogue' })).not.toBeInTheDocument()
  })
})

describe('the basket count in the header', () => {
  it('sits in the main navigation of the header, next to the Basket link, and the header keeps its class', () => {
    window.location.hash = '#/basket'

    render(<App />)

    const banner = screen.getByRole('banner')
    expect(banner).toHaveClass('site-header')
    const navigation = within(banner).getByRole('navigation', { name: 'Main' })
    const badge = within(navigation).getByRole('status', { name: 'Items in basket' })
    expect(badge).toHaveClass('count-badge')
    expect(within(navigation).getByRole('link', { name: 'Basket' }).nextElementSibling).toBe(badge)
  })

  it('shows 0 for an empty basket', () => {
    window.location.hash = '#/basket'

    render(<App />)

    expect(basketBadge().textContent).toBe('0')
  })

  it('counts items, not lines: the same book added twice shows 2, without a reload', async () => {
    window.location.hash = `#/books/${BOOK_ID}`
    answerBook()
    const user = userEvent.setup()
    render(<App />)
    await screen.findByRole('heading', { level: 1, name: 'Neuromancer' })
    expect(basketBadge().textContent).toBe('0')

    await user.click(screen.getByRole('button', { name: 'Add to basket' }))
    expect(basketBadge().textContent).toBe('1')
    await user.click(screen.getByRole('button', { name: 'Add to basket' }))

    expect(basketBadge().textContent).toBe('2')
    expect(JSON.parse(sessionStorage.getItem('openbook.basket') ?? 'null')).toHaveLength(1)
  })

  it('stays when another page is opened', async () => {
    window.location.hash = `#/books/${BOOK_ID}`
    answerBook()
    const user = userEvent.setup()
    render(<App />)
    await screen.findByRole('heading', { level: 1, name: 'Neuromancer' })
    await user.click(screen.getByRole('button', { name: 'Add to basket' }))

    await user.click(screen.getByRole('link', { name: 'Basket' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Basket' })).toBeInTheDocument()
    expect(basketBadge().textContent).toBe('1')
  })

  it('shows the stored basket in the first render: quantities 2, 1 and 4 in three lines make 7', () => {
    sessionStorage.setItem(
      'openbook.basket',
      '[{"bookId":"2f6a9d41-8b03-4c57-9e1d-6a7c0b5e3f82","title":"Emma","price":"12.50","currency":"GBP","quantity":2},' +
        `{"bookId":"${BOOK_ID}","title":"Neuromancer","price":"7.99","currency":"USD","quantity":1},` +
        '{"bookId":"0c4e8b15-3a96-4d27-b1e8-5f7a2c9d6e03","title":"Dune","price":"10.00","currency":"EUR","quantity":4}]',
    )
    window.location.hash = '#/basket'

    render(<App />)

    expect(basketBadge().textContent).toBe('7')
  })
})

describe('the shell', () => {
  it('puts the page in the main landmark and the shop line in the footer', async () => {
    answerCatalogue()

    render(<App />)

    expect(await screen.findByText('No books match your search.')).toBeInTheDocument()
    expect(within(screen.getByRole('main')).getByRole('heading', { level: 1, name: 'Catalogue' })).toBeInTheDocument()
    expect(screen.getByRole('contentinfo')).toHaveTextContent('OpenBook, the J-Fall 2026 workshop shop by OpenValue')
  })
})
