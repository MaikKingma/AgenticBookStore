import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useEffect } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { HashRouter, Link, navigate, useRoute } from './router'

// Shows the route that useRoute reports as "name|id" (the id part is empty for routes without a parameter).
function RouteProbe() {
  const route = useRoute()
  return <p>{`${route.name}|${'id' in route ? route.id : ''}`}</p>
}

const BOOK_ID = '3f2a9c10-6b4d-4e8a-9c71-5d0e8b2a4f16'
const ORDER_ID = '6c1d7e92-0a35-4b8f-8d24-1e9f3a7c5b40'

const KNOWN = [
  ['', 'catalogue|'],
  ['#/', 'catalogue|'],
  [`#/books/${BOOK_ID}`, `book|${BOOK_ID}`],
  ['#/basket', 'basket|'],
  ['#/checkout', 'checkout|'],
  ['#/orders', 'orders|'],
  [`#/orders/${ORDER_ID}`, `order|${ORDER_ID}`],
  ['#/assistant', 'assistant|'],
  // The parameter is decoded.
  ['#/books/a%20b', 'book|a b'],
  ['#/orders/a%2Fb%3F', 'order|a/b?'],
  // Decoded once, not twice: %2541 is "%41", not "A".
  ['#/books/a%2541', 'book|a%41'],
] as const

const UNKNOWN = [
  '#/nothing',
  '#/Basket',
  '#basket',
  '#/books',
  '#/books/',
  `#/books/${BOOK_ID}/extra`,
  '#/basket/',
  '#/orders/',
  `#/orders/${ORDER_ID}/lines`,
  '#//',
  // A second segment after an unknown first segment, or after a route that takes none, is no book or order.
  '#/basket/x',
  '#/assistant/x',
  '#/foo/bar',
  // The prefix must be "#/", and a query string is not part of the route.
  '#xbasket',
  '#/basket?x=1',
  // A malformed percent escape in the parameter must not crash the page.
  '#/books/%E0%A4%A',
  '#/orders/%',
] as const

describe('useRoute', () => {
  it.each(KNOWN)('reads the hash %j as %s', (hash, expected) => {
    window.location.hash = hash

    render(<RouteProbe />)

    expect(screen.getByText(expected)).toBeInTheDocument()
  })

  it.each(UNKNOWN)('reads the hash %j as an unknown route', (hash) => {
    window.location.hash = hash

    render(<RouteProbe />)

    expect(screen.getByText('not-found|')).toBeInTheDocument()
  })

  it('follows the hash while the component stays mounted', async () => {
    window.location.hash = '#/basket'
    render(<RouteProbe />)
    expect(screen.getByText('basket|')).toBeInTheDocument()

    window.location.hash = `#/orders/${ORDER_ID}`

    expect(await screen.findByText(`order|${ORDER_ID}`)).toBeInTheDocument()
    expect(screen.queryByText('basket|')).not.toBeInTheDocument()
  })

  it('stops listening for hash changes when the component unmounts', () => {
    const added = vi.spyOn(window, 'addEventListener')
    const removed = vi.spyOn(window, 'removeEventListener')

    const view = render(<RouteProbe />)
    const handlers = added.mock.calls.filter(([type]) => type === 'hashchange').map(([, handler]) => handler)
    expect(handlers).toHaveLength(1)
    expect(removed.mock.calls.filter(([type]) => type === 'hashchange')).toEqual([])

    view.unmount()

    expect(removed.mock.calls.filter(([type]) => type === 'hashchange').map(([, handler]) => handler)).toEqual(handlers)
  })
})

describe('HashRouter', () => {
  it.each(KNOWN)('hands the route of the hash %j to its render function', (hash, expected) => {
    window.location.hash = hash

    render(<HashRouter>{(route) => <p>{`${route.name}|${'id' in route ? route.id : ''}`}</p>}</HashRouter>)

    expect(screen.getByText(expected)).toBeInTheDocument()
  })

  it.each(UNKNOWN)('shows the page not found view for the hash %j and never calls the render function', (hash) => {
    window.location.hash = hash
    const page = vi.fn(() => <p>a known page</p>)

    render(<HashRouter>{page}</HashRouter>)

    expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to the catalogue' })).toHaveAttribute('href', '#/')
    expect(screen.queryByText('a known page')).not.toBeInTheDocument()
    expect(page).not.toHaveBeenCalled()
  })

  it('renders the next route in the same tree without mounting the page again', async () => {
    const mounted = vi.fn()
    function Page({ name }: { name: string }) {
      useEffect(() => {
        mounted()
      }, [])
      return <p>{`page ${name}`}</p>
    }
    window.location.hash = '#/basket'
    render(<HashRouter>{(route) => <Page name={route.name} />}</HashRouter>)
    expect(screen.getByText('page basket')).toBeInTheDocument()

    window.location.hash = '#/orders'

    expect(await screen.findByText('page orders')).toBeInTheDocument()
    expect(screen.queryByText('page basket')).not.toBeInTheDocument()
    expect(mounted).toHaveBeenCalledTimes(1)
  })

  it('moves from a known page to the not found view and back', async () => {
    window.location.hash = '#/checkout'
    render(<HashRouter>{(route) => <p>{`page ${route.name}`}</p>}</HashRouter>)
    expect(screen.getByText('page checkout')).toBeInTheDocument()

    window.location.hash = '#/nowhere'
    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
    expect(screen.queryByText('page checkout')).not.toBeInTheDocument()

    window.location.hash = '#/assistant'
    expect(await screen.findByText('page assistant')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Page not found' })).not.toBeInTheDocument()
  })
})

describe('navigate', () => {
  it('sets the hash to the path and the route follows', async () => {
    render(<RouteProbe />)
    expect(screen.getByText('catalogue|')).toBeInTheDocument()

    navigate(`/orders/${ORDER_ID}`)

    expect(window.location.hash).toBe(`#/orders/${ORDER_ID}`)
    expect(await screen.findByText(`order|${ORDER_ID}`)).toBeInTheDocument()
  })

  it('passes the path through as given', () => {
    navigate('/books/a%20b')

    expect(window.location.hash).toBe('#/books/a%20b')
  })
})

describe('Link', () => {
  it('renders an anchor whose href is the path behind a hash sign', () => {
    render(<Link to="/basket">Your basket</Link>)

    expect(screen.getByRole('link', { name: 'Your basket' })).toHaveAttribute('href', '#/basket')
  })

  it('passes class names and other anchor attributes through', () => {
    render(
      <Link to="/orders" className="extra" aria-current="page">
        Orders
      </Link>,
    )

    const link = screen.getByRole('link', { name: 'Orders' })
    expect(link).toHaveAttribute('href', '#/orders')
    expect(link).toHaveClass('extra')
    expect(link).toHaveAttribute('aria-current', 'page')
  })

  it('moves the route when it is clicked', async () => {
    const user = userEvent.setup()
    render(
      <>
        <Link to={`/books/${BOOK_ID}`}>Open the book</Link>
        <RouteProbe />
      </>,
    )
    expect(screen.getByText('catalogue|')).toBeInTheDocument()

    await user.click(screen.getByRole('link', { name: 'Open the book' }))

    expect(await screen.findByText(`book|${BOOK_ID}`)).toBeInTheDocument()
    expect(window.location.hash).toBe(`#/books/${BOOK_ID}`)
  })
})

describe('the page not found view', () => {
  it('sits inside the router output so a shell can frame it', () => {
    window.location.hash = '#/nothing'

    render(
      <main>
        <HashRouter>{() => null}</HashRouter>
      </main>,
    )

    expect(within(screen.getByRole('main')).getByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
  })
})
