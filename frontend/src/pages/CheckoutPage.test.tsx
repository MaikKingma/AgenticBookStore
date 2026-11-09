import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, onTestFinished, vi } from 'vitest'
import App from '../App'
import type { OrderView } from '../api/types'
import { jsonResponse, mockFetch, problemResponse } from '../test/fetchMock'
import type { MockRoute } from '../test/fetchMock'

const STORAGE_KEY = 'openbook.basket'
const PLACE = '/orders/commands/place'

// The ids of the three books in the basket: in basket order they go down and up (6, 0, 4), so a checkout that sorts the
// lines by id sends another order than the basket holds.
const NEUROMANCER_ID = '6d2f9a14-3b07-4c85-9e1a-8f4d0b6c2e73'
const DUNE_ID = '0e5b8d37-1a94-4f62-b3c8-7d2a9e6f1b05'
const PERSUASION_ID = '4a7c1e92-0b35-4d68-a9f4-2e8d6b1c7053'
// A book the server complains about that is not in the basket, and the order the server answers with. All different.
const UNKNOWN_ID = '1b7e5d08-3c94-4a26-b1f7-9e0d4c8a2653'
const ORDER_ID = '1d9c4e60-7a28-4b53-8f1e-5c0b3a6d9e72'

const BASKET_JSON =
  `[{"bookId":"${NEUROMANCER_ID}","title":"Neuromancer","price":"7.99","currency":"EUR","quantity":2},` +
  `{"bookId":"${DUNE_ID}","title":"Dune","price":"12.50","currency":"EUR","quantity":3},` +
  `{"bookId":"${PERSUASION_ID}","title":"Persuasion","price":"4.35","currency":"EUR","quantity":1}]`

const EMAIL = 'reader@example.com'

function seed(json: string = BASKET_JSON) {
  sessionStorage.setItem(STORAGE_KEY, json)
}

function storedText(): string | null {
  return sessionStorage.getItem(STORAGE_KEY)
}

// The order the bookstore answers with after a successful checkout. It prices the last book at 3.50, not at the 4.35 of
// the basket, so that the order page is seen to show the bookstore's figures and not the estimate of the basket.
function placedOrder(): OrderView {
  return {
    id: ORDER_ID,
    customerEmail: EMAIL,
    status: 'PLACED',
    placedAt: '2026-06-15T10:00:00.123456Z',
    shippedAt: null,
    lines: [
      { isbn: '9780262035613', title: 'Neuromancer', quantity: 2, unitPrice: '7.99', lineTotal: '15.98' },
      { isbn: '9780141439518', title: 'Dune', quantity: 3, unitPrice: '12.50', lineTotal: '37.50' },
      { isbn: '9780441569595', title: 'Persuasion', quantity: 1, unitPrice: '3.50', lineTotal: '3.50' },
    ],
    total: '56.98',
    currency: 'EUR',
  }
}

function accepted(): MockRoute[] {
  return [
    { method: 'POST', path: PLACE, respond: () => jsonResponse({ id: ORDER_ID }, 202) },
    { method: 'GET', path: `/orders/${ORDER_ID}`, respond: () => jsonResponse(placedOrder()) },
  ]
}

function failing(response: () => Response): MockRoute[] {
  return [{ method: 'POST', path: PLACE, respond: response }]
}

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((done) => {
    resolve = done
  })
  return { promise, resolve }
}

function openCheckout() {
  window.location.hash = '#/checkout'
  return render(<App />)
}

function emailInput(): HTMLInputElement {
  return screen.getByLabelText('E-mail address')
}

function confirmButton() {
  return screen.getByRole('button', { name: 'Confirm order' })
}

function basketBadge() {
  return within(screen.getByRole('banner')).getByRole('status', { name: 'Items in basket' })
}

// Records, for every submit event that reaches the document, whether the form handler had prevented its default.
function watchSubmits(): boolean[] {
  const seen: boolean[] = []
  const listener = (event: Event) => {
    seen.push(event.defaultPrevented)
  }
  document.addEventListener('submit', listener)
  onTestFinished(() => document.removeEventListener('submit', listener))
  return seen
}

async function placeOrderAs(user: ReturnType<typeof userEvent.setup>, email: string) {
  await user.type(emailInput(), email)
  await user.click(confirmButton())
}

function alertItems(alert: HTMLElement): Array<string | null> {
  return within(alert).getAllByRole('listitem').map((item) => item.textContent)
}

describe('CheckoutPage form', () => {
  it('has the heading Checkout, a form that does not validate, an e-mail field and a Confirm order button', () => {
    seed()

    openCheckout()

    expect(screen.getByRole('heading', { level: 1, name: 'Checkout' })).toBeInTheDocument()
    const form = screen.getByRole('form', { name: 'Order details' })
    expect(form).toHaveAttribute('novalidate')
    expect(emailInput()).toHaveValue('')
    expect(within(form).getByRole('button', { name: 'Confirm order' })).toBeEnabled()
    expect(screen.getByRole('link', { name: 'Back to the basket' })).toHaveAttribute('href', '#/basket')
  })

  it('shows the lines of the basket in basket order, with their totals, and the estimated total with its sentence', () => {
    seed()

    openCheckout()

    const summary = within(screen.getByRole('main')).getByRole('list')
    expect(within(summary).getAllByRole('listitem').map((item) => item.textContent)).toEqual([
      'Neuromancer, 2 x 7.99 EUR: 15.98 EUR',
      'Dune, 3 x 12.50 EUR: 37.50 EUR',
      'Persuasion, 1 x 4.35 EUR: 4.35 EUR',
    ])
    expect(screen.getByText('57.83 EUR').parentElement?.textContent).toBe('Estimated total: 57.83 EUR')
    expect(screen.getByText('The bookstore confirms the total when the order is placed.')).toBeInTheDocument()
  })

  it('says the basket is empty, with a link to the catalogue and no form, when there is nothing to order', () => {
    openCheckout()

    expect(screen.getByRole('heading', { level: 1, name: 'Checkout' })).toBeInTheDocument()
    expect(within(screen.getByRole('main')).getByRole('status')).toHaveTextContent('Your basket is empty.')
    expect(within(screen.getByRole('main')).getByRole('link', { name: 'Browse the catalogue' })).toHaveAttribute('href', '#/')
    expect(screen.queryByRole('form')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Confirm order' })).not.toBeInTheDocument()
  })
})

describe('CheckoutPage placing the order', () => {
  it('posts the e-mail and the lines in basket order with numbers for the quantities, and nothing else', async () => {
    seed()
    const mock = mockFetch(accepted())
    const user = userEvent.setup()
    openCheckout()

    await placeOrderAs(user, EMAIL)

    expect(await screen.findByText('PLACED')).toBeInTheDocument()
    const post = mock.calls.find((call) => call.method === 'POST')!
    expect(post.path).toBe(PLACE)
    expect(post.headers.get('Content-Type')).toBe('application/json')
    const body = JSON.parse(post.body ?? 'null') as unknown
    expect(body).toEqual({
      customerEmail: 'reader@example.com',
      lines: [
        { bookId: NEUROMANCER_ID, quantity: 2 },
        { bookId: DUNE_ID, quantity: 3 },
        { bookId: PERSUASION_ID, quantity: 1 },
      ],
    })
    expect(Object.keys(body as object).sort()).toEqual(['customerEmail', 'lines'])
    expect((body as { lines: object[] }).lines.map((line) => Object.keys(line).sort())).toEqual([
      ['bookId', 'quantity'],
      ['bookId', 'quantity'],
      ['bookId', 'quantity'],
    ])
  })

  it('sends the e-mail exactly as typed, with its capitals and the spaces at both ends', async () => {
    seed()
    const mock = mockFetch(accepted())
    const user = userEvent.setup()
    openCheckout()

    await placeOrderAs(user, '  Reader@Example.COM ')

    expect(await screen.findByText('PLACED')).toBeInTheDocument()
    const post = mock.calls.find((call) => call.method === 'POST')!
    expect((JSON.parse(post.body ?? 'null') as { customerEmail: string }).customerEmail).toBe('  Reader@Example.COM ')
  })

  it('sends text that is no e-mail address too: the form does not validate, the bookstore does', async () => {
    seed()
    const mock = mockFetch(accepted())
    const user = userEvent.setup()
    openCheckout()

    await placeOrderAs(user, 'not-an-email')

    expect(await screen.findByText('PLACED')).toBeInTheDocument()
    const post = mock.calls.find((call) => call.method === 'POST')!
    expect((JSON.parse(post.body ?? 'null') as { customerEmail: string }).customerEmail).toBe('not-an-email')
  })

  it('sends an empty e-mail field as an empty text', async () => {
    seed()
    const mock = mockFetch(accepted())
    const user = userEvent.setup()
    openCheckout()

    await user.click(confirmButton())

    expect(await screen.findByText('PLACED')).toBeInTheDocument()
    const post = mock.calls.find((call) => call.method === 'POST')!
    expect((JSON.parse(post.body ?? 'null') as { customerEmail: string }).customerEmail).toBe('')
  })

  it('prevents the default action of the form submit', async () => {
    seed()
    mockFetch(accepted())
    const submits = watchSubmits()
    const user = userEvent.setup()
    openCheckout()

    await placeOrderAs(user, EMAIL)

    expect(await screen.findByText('PLACED')).toBeInTheDocument()
    expect(submits).toEqual([true])
  })

  it('disables the button while the bookstore thinks, sends one request for two clicks and two submits, and keeps the basket until the answer', async () => {
    seed()
    const held = deferred<Response>()
    const mock = mockFetch([
      { method: 'POST', path: PLACE, respond: () => held.promise },
      { method: 'GET', path: `/orders/${ORDER_ID}`, respond: () => jsonResponse(placedOrder()) },
    ])
    const user = userEvent.setup()
    openCheckout()
    await user.type(emailInput(), EMAIL)

    await user.click(confirmButton())
    await user.click(confirmButton())
    fireEvent.submit(screen.getByRole('form', { name: 'Order details' }))

    expect(confirmButton()).toBeDisabled()
    expect(mock.calls.map((call) => `${call.method} ${call.path}`)).toEqual([`POST ${PLACE}`])
    expect(storedText()).toBe(BASKET_JSON)
    expect(basketBadge().textContent).toBe('6')

    held.resolve(jsonResponse({ id: ORDER_ID }, 202))

    expect(await screen.findByText('PLACED')).toBeInTheDocument()
    expect(mock.calls.map((call) => `${call.method} ${call.path}`)).toEqual([`POST ${PLACE}`, `GET /orders/${ORDER_ID}`])
  })

  it('empties the basket and its storage after the 202, empties the badge and shows the order of the answer', async () => {
    seed()
    const mock = mockFetch(accepted())
    const user = userEvent.setup()
    openCheckout()
    expect(basketBadge().textContent).toBe('6')

    await placeOrderAs(user, EMAIL)

    expect(await screen.findByText('PLACED')).toBeInTheDocument()
    expect(storedText()).toBeNull()
    expect(basketBadge().textContent).toBe('0')
    expect(window.location.hash).toBe(`#/orders/${ORDER_ID}`)
    expect(screen.getByRole('heading', { level: 1, name: 'Order' })).toBeInTheDocument()
    expect(mock.calls.map((call) => `${call.method} ${call.path}`)).toEqual([`POST ${PLACE}`, `GET /orders/${ORDER_ID}`])
    expect(screen.getAllByRole('row').slice(1).map((row) => within(row).getAllByRole('cell').map((cell) => cell.textContent))).toEqual([
      ['Neuromancer', '9780262035613', '2', '7.99 EUR', '15.98 EUR'],
      ['Dune', '9780141439518', '3', '12.50 EUR', '37.50 EUR'],
      ['Persuasion', '9780441569595', '1', '3.50 EUR', '3.50 EUR'],
    ])
    expect(screen.getByText('56.98 EUR')).toBeInTheDocument()
    expect(screen.queryByText('57.83 EUR')).not.toBeInTheDocument()
  })

  it('navigates with the id of the order encoded as a path segment', async () => {
    seed()
    const oddId = 'odd id/1'
    const encoded = 'odd%20id%2F1'
    const mock = mockFetch([
      { method: 'POST', path: PLACE, respond: () => jsonResponse({ id: oddId }, 202) },
      { method: 'GET', path: `/orders/${encoded}`, respond: () => jsonResponse({ ...placedOrder(), id: oddId }) },
    ])
    const user = userEvent.setup()
    openCheckout()

    await placeOrderAs(user, EMAIL)

    expect(await screen.findByText('PLACED')).toBeInTheDocument()
    expect(window.location.hash).toBe(`#/orders/${encoded}`)
    expect(mock.calls.map((call) => call.path)).toEqual([PLACE, `/orders/${encoded}`])
  })

  it('shows the order page, with the Orders link marked as the current page, not the checkout', async () => {
    seed()
    mockFetch(accepted())
    const user = userEvent.setup()
    openCheckout()
    expect(within(screen.getByRole('navigation', { name: 'Main' })).getByRole('link', { name: 'Basket' })).toHaveAttribute('aria-current', 'page')

    await placeOrderAs(user, EMAIL)

    expect(await screen.findByText('PLACED')).toBeInTheDocument()
    expect(screen.queryByRole('form')).not.toBeInTheDocument()
    expect(within(screen.getByRole('navigation', { name: 'Main' })).getByRole('link', { name: 'Orders' })).toHaveAttribute('aria-current', 'page')
  })
})

describe('CheckoutPage answers that are not an order', () => {
  async function submitFailure(response: () => Response) {
    seed()
    const mock = mockFetch(failing(response))
    const user = userEvent.setup()
    openCheckout()
    await placeOrderAs(user, EMAIL)
    const alert = await screen.findByRole('alert')
    return { alert, mock, user }
  }

  function expectBasketKept() {
    expect(storedText()).toBe(BASKET_JSON)
    expect(basketBadge().textContent).toBe('6')
    expect(window.location.hash).toBe('#/checkout')
    expect(confirmButton()).toBeEnabled()
    expect(emailInput()).toHaveValue(EMAIL)
  }

  it('lists each shortage of an insufficient stock answer as given, with the titles of the basket and the id of a book it does not hold', async () => {
    const { alert } = await submitFailure(() =>
      problemResponse(409, {
        title: 'Insufficient stock',
        detail: 'Not enough copies to place the order',
        code: 'insufficient-stock',
        details: [
          { bookId: NEUROMANCER_ID, requested: '3', available: '0' },
          { bookId: UNKNOWN_ID, requested: '10', available: '4' },
          { bookId: PERSUASION_ID, requested: '2', available: '1' },
        ],
      }),
    )

    expect(within(alert).getByText('Insufficient stock')).toBeInTheDocument()
    expect(within(alert).getByText('Not enough copies to place the order')).toBeInTheDocument()
    expect(alertItems(alert)).toEqual([
      'Book Neuromancer: requested 3, available 0',
      `Book ${UNKNOWN_ID}: requested 10, available 4`,
      'Book Persuasion: requested 2, available 1',
    ])
    expectBasketKept()
  })

  it('keeps the stored basket byte for byte after an insufficient stock answer', async () => {
    await submitFailure(() =>
      problemResponse(409, {
        title: 'Insufficient stock',
        code: 'insufficient-stock',
        details: [{ bookId: DUNE_ID, requested: '3', available: '0' }],
      }),
    )

    expect(storedText()).toBe(BASKET_JSON)
  })

  it('shows a book that cannot be ordered as the plain alert with its title and detail, without a list and with the basket kept', async () => {
    const { alert } = await submitFailure(() =>
      problemResponse(409, {
        title: 'Book not orderable',
        detail: 'The book is discontinued',
        code: 'book-not-orderable',
        details: [],
      }),
    )

    expect(within(alert).getByText('Book not orderable')).toBeInTheDocument()
    expect(within(alert).getByText('The book is discontinued')).toBeInTheDocument()
    expect(within(alert).queryByRole('list')).not.toBeInTheDocument()
    expect(alert.textContent).toBe('Book not orderableThe book is discontinued')
    expectBasketKept()
  })

  it('shows the message next to the field name for a validation failure, both messages of a repeated field and an indexed field as sent', async () => {
    const { alert } = await submitFailure(() =>
      problemResponse(400, {
        title: 'Validation failed',
        code: 'validation-failed',
        details: [
          { field: 'customerEmail', message: 'must be a well-formed email address' },
          { field: 'lines[1].quantity', message: 'quantity must be between 1 and 10' },
          { field: 'customerEmail', message: 'must not be blank' },
        ],
      }),
    )

    expect(within(alert).getByText('Validation failed')).toBeInTheDocument()
    expect(alertItems(alert)).toEqual([
      'customerEmail: must be a well-formed email address',
      'lines[1].quantity: quantity must be between 1 and 10',
      'customerEmail: must not be blank',
    ])
    expectBasketKept()
  })

  it('shows the title and the messages of a quantity out of range answer and keeps the basket', async () => {
    const { alert } = await submitFailure(() =>
      problemResponse(422, {
        title: 'Quantity out of range',
        code: 'quantity-out-of-range',
        details: [
          { field: 'lines', message: 'The quantity 12 is not between 1 and 10' },
          { field: 'lines', message: 'The quantity 0 is not between 1 and 10' },
          { field: 'lines', message: 'The quantity 11 is not between 1 and 10' },
        ],
      }),
    )

    expect(within(alert).getByText('Quantity out of range')).toBeInTheDocument()
    expect(alertItems(alert)).toEqual([
      'lines: The quantity 12 is not between 1 and 10',
      'lines: The quantity 0 is not between 1 and 10',
      'lines: The quantity 11 is not between 1 and 10',
    ])
    expectBasketKept()
  })

  it('shows the alert for a bookstore that cannot be reached, keeps the basket and enables the button again', async () => {
    const down = () => {
      throw new Error('connection refused')
    }
    seed()
    const mock = mockFetch([{ method: 'POST', path: PLACE, respond: down }])
    const user = userEvent.setup()
    openCheckout()

    await placeOrderAs(user, EMAIL)

    const alert = await screen.findByRole('alert')
    expect(within(alert).getByText('Cannot reach the bookstore')).toBeInTheDocument()
    expect(mock.calls.map((call) => `${call.method} ${call.path}`)).toEqual([`POST ${PLACE}`])
    expectBasketKept()
  })

  it('shows a generic alert when the request fails in a way the API client does not report as a problem', async () => {
    seed()
    // An answer without headers makes the client itself throw a TypeError instead of an ApiError.
    vi.stubGlobal('fetch', async () => ({}))
    const user = userEvent.setup()
    openCheckout()

    await placeOrderAs(user, EMAIL)

    const alert = await screen.findByRole('alert')
    expect(within(alert).getByText('Something went wrong')).toBeInTheDocument()
    expectBasketKept()
  })

  it('takes the alert away while the next attempt runs and goes to the order when it works', async () => {
    seed()
    let attempts = 0
    const held = deferred<Response>()
    mockFetch([
      {
        method: 'POST',
        path: PLACE,
        respond: () => {
          attempts += 1
          return attempts === 1
            ? problemResponse(409, { title: 'Insufficient stock', code: 'insufficient-stock', details: [] })
            : held.promise
        },
      },
      { method: 'GET', path: `/orders/${ORDER_ID}`, respond: () => jsonResponse(placedOrder()) },
    ])
    const user = userEvent.setup()
    openCheckout()
    await placeOrderAs(user, EMAIL)
    expect(await screen.findByRole('alert')).toHaveTextContent('Insufficient stock')

    await user.click(confirmButton())

    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(confirmButton()).toBeDisabled()
    held.resolve(jsonResponse({ id: ORDER_ID }, 202))
    expect(await screen.findByText('PLACED')).toBeInTheDocument()
    await waitFor(() => expect(storedText()).toBeNull())
    expect(attempts).toBe(2)
  })
})
