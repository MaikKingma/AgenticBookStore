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

describe('the shell', () => {
  it('puts the page in the main landmark and the shop line in the footer', async () => {
    answerCatalogue()

    render(<App />)

    expect(await screen.findByText('No books match your search.')).toBeInTheDocument()
    expect(within(screen.getByRole('main')).getByRole('heading', { level: 1, name: 'Catalogue' })).toBeInTheDocument()
    expect(screen.getByRole('contentinfo')).toHaveTextContent('OpenBook, the J-Fall 2026 workshop shop by OpenValue')
  })
})
