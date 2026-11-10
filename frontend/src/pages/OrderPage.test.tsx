import { act, render, screen, waitFor, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { OrderView } from '../api/types'
import { jsonResponse, mockFetch, problemResponse } from '../test/fetchMock'
import { OrderPage } from './OrderPage'

const ORDER_ID = '3c8a5e17-9d04-4b62-a1f8-6e2b0d7c4a95'
const OTHER_ID = '7e0d3b59-2a86-4c14-9f7b-1d5a8c6e0b32'
const MISSING_ID = '2f9b6c03-8e51-4a7d-b3c0-5a1e7d9f4b68'

// The three lines are not in the order of their titles, ISBNs, quantities, unit prices or line totals (each goes up and
// down). The line total of 12.50 x 2 is 24.00 and not 25.00, and the line totals add up to 32.00 while the total is
// 31.50: a page that works anything out shows other figures than the bookstore sent.
function orderView(overrides: Partial<OrderView> = {}): OrderView {
  return {
    id: ORDER_ID,
    customerEmail: 'Reader@Example.com',
    status: 'PLACED',
    placedAt: '2026-06-15T10:00:00.123456Z',
    shippedAt: null,
    lines: [
      { isbn: '9780262035613', title: 'Neuromancer', quantity: 4, unitPrice: '1.25', lineTotal: '5.00' },
      { isbn: '9780141439518', title: 'Dune', quantity: 2, unitPrice: '12.50', lineTotal: '24.00' },
      { isbn: '9780441569595', title: 'Persuasion', quantity: 3, unitPrice: '1.00', lineTotal: '3.00' },
    ],
    total: '31.50',
    currency: 'EUR',
    ...overrides,
  }
}

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((done) => {
    resolve = done
  })
  return { promise, resolve }
}

function showOrder(order: OrderView, id = ORDER_ID) {
  return mockFetch([{ method: 'GET', path: `/orders/${id}`, respond: () => jsonResponse(order) }])
}

function facts(): Array<[string | null, string | null]> {
  const terms = screen.getAllByRole('term').map((term) => term.textContent)
  const definitions = screen.getAllByRole('definition').map((definition) => definition.textContent)
  return terms.map((term, index): [string | null, string | null] => [term, definitions[index]])
}

function lineRows(): Array<Array<string | null>> {
  return screen.getAllByRole('row').slice(1).map((row) => within(row).getAllByRole('cell').map((cell) => cell.textContent))
}

describe('OrderPage content', () => {
  it('shows a loading message under the heading Order and a link to the orders until the answer arrives', async () => {
    const held = deferred<Response>()
    mockFetch([{ method: 'GET', path: `/orders/${ORDER_ID}`, respond: () => held.promise }])

    render(<OrderPage id={ORDER_ID} />)

    expect(screen.getByRole('heading', { level: 1, name: 'Order' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Loading order')
    expect(screen.getByRole('link', { name: 'Back to your orders' })).toHaveAttribute('href', '#/orders')
    expect(screen.queryByRole('table')).not.toBeInTheDocument()

    held.resolve(jsonResponse(orderView()))

    expect(await screen.findByText('PLACED')).toBeInTheDocument()
    expect(screen.queryByText('Loading order')).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Order' })).toBeInTheDocument()
  })

  it('shows the status, the formatted time of placing, that nothing was shipped, the address and the total', async () => {
    const mock = showOrder(orderView())

    render(<OrderPage id={ORDER_ID} />)

    expect(await screen.findByText('PLACED')).toBeInTheDocument()
    expect(facts()).toEqual([
      ['Order number', ORDER_ID],
      ['Status', 'PLACED'],
      ['Placed', '2026-06-15 10:00:00 UTC'],
      ['Shipped', 'Not shipped yet'],
      ['E-mail address', 'Reader@Example.com'],
      ['Total', '31.50 EUR'],
    ])
    expect(screen.queryByText('null')).not.toBeInTheDocument()
    expect(mock.calls.map((call) => call.path)).toEqual([`/orders/${ORDER_ID}`])
  })

  it('shows the formatted time of shipping and the status SHIPPED for a shipped order', async () => {
    showOrder(orderView({ status: 'SHIPPED', shippedAt: '2026-06-16T08:30:15Z' }))

    render(<OrderPage id={ORDER_ID} />)

    expect(await screen.findByText('SHIPPED')).toBeInTheDocument()
    expect(facts()[3]).toEqual(['Shipped', '2026-06-16 08:30:15 UTC'])
    expect(screen.queryByText('Not shipped yet')).not.toBeInTheDocument()
    expect(screen.queryByText('PLACED')).not.toBeInTheDocument()
  })

  it('shows the lines in the order sent with the unit price and the line total of the bookstore', async () => {
    showOrder(orderView())

    render(<OrderPage id={ORDER_ID} />)

    expect(await screen.findByText('PLACED')).toBeInTheDocument()
    expect(screen.getAllByRole('columnheader').map((header) => header.textContent)).toEqual([
      'Title',
      'ISBN',
      'Quantity',
      'Unit price',
      'Line total',
    ])
    expect(lineRows()).toEqual([
      ['Neuromancer', '9780262035613', '4', '1.25 EUR', '5.00 EUR'],
      ['Dune', '9780141439518', '2', '12.50 EUR', '24.00 EUR'],
      ['Persuasion', '9780441569595', '3', '1.00 EUR', '3.00 EUR'],
    ])
  })

  it('shows the line total 24.00 for 12.50 x 2 and not the 25.00 it would work out', async () => {
    showOrder(orderView())

    render(<OrderPage id={ORDER_ID} />)

    expect(await screen.findByText('24.00 EUR')).toBeInTheDocument()
    expect(screen.queryByText('25.00 EUR')).not.toBeInTheDocument()
  })

  it('shows the total 31.50 although the line totals add up to 32.00, and adds nothing up itself', async () => {
    showOrder(orderView())

    render(<OrderPage id={ORDER_ID} />)

    expect(await screen.findByText('31.50 EUR')).toBeInTheDocument()
    expect(screen.queryByText('32.00 EUR')).not.toBeInTheDocument()
    expect(screen.queryByText('33.00 EUR')).not.toBeInTheDocument()
  })

  it('writes every amount with the currency of the order', async () => {
    showOrder(orderView({ currency: 'GBP' }))

    render(<OrderPage id={ORDER_ID} />)

    expect(await screen.findByText('31.50 GBP')).toBeInTheDocument()
    expect(lineRows()[1]).toEqual(['Dune', '9780141439518', '2', '12.50 GBP', '24.00 GBP'])
    expect(screen.queryByText(/EUR/)).not.toBeInTheDocument()
  })

  it('asks for an id with a character that needs escaping with the id encoded as a path segment', async () => {
    const mock = mockFetch([
      { method: 'GET', path: '/orders/odd%20id%2F1', respond: () => jsonResponse(orderView({ id: 'odd id/1' })) },
    ])

    render(<OrderPage id="odd id/1" />)

    expect(await screen.findByText('PLACED')).toBeInTheDocument()
    expect(mock.calls.map((call) => call.path)).toEqual(['/orders/odd%20id%2F1'])
  })
})

describe('OrderPage errors', () => {
  it('shows the alert of the bookstore when the order does not exist, and no order', async () => {
    mockFetch([
      {
        method: 'GET',
        path: `/orders/${MISSING_ID}`,
        respond: () =>
          problemResponse(404, {
            title: 'Order not found',
            detail: `No order with id ${MISSING_ID}`,
            code: 'order-not-found',
            details: [],
          }),
      },
    ])

    render(<OrderPage id={MISSING_ID} />)

    const alert = await screen.findByRole('alert')
    expect(within(alert).getByText('Order not found')).toBeInTheDocument()
    expect(within(alert).getByText(`No order with id ${MISSING_ID}`)).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    expect(screen.queryByText('Loading order')).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Order' })).toBeInTheDocument()
  })

  it('shows the alert for the plain error answer of the framework to an address that is no order number', async () => {
    mockFetch([
      {
        method: 'GET',
        path: '/orders/not-a-uuid',
        respond: () =>
          jsonResponse(
            { timestamp: '2026-06-15T10:00:00.000+00:00', status: 400, error: 'Bad Request', path: '/orders/not-a-uuid' },
            400,
            'Bad Request',
          ),
      },
    ])

    render(<OrderPage id="not-a-uuid" />)

    const alert = await screen.findByRole('alert')
    expect(within(alert).getByText('Bad Request')).toBeInTheDocument()
    expect(within(alert).queryByRole('list')).not.toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('shows the alert for a bookstore that cannot be reached', async () => {
    const mock = mockFetch([
      {
        method: 'GET',
        path: `/orders/${ORDER_ID}`,
        respond: () => {
          throw new Error('connection refused')
        },
      },
    ])

    render(<OrderPage id={ORDER_ID} />)

    const alert = await screen.findByRole('alert')
    expect(within(alert).getByText('Cannot reach the bookstore')).toBeInTheDocument()
    expect(mock.calls.map((call) => call.path)).toEqual([`/orders/${ORDER_ID}`])
  })

  it('shows a generic alert when the request fails in a way the API client does not report as a problem', async () => {
    // An answer without headers makes the client itself throw a TypeError instead of an ApiError.
    vi.stubGlobal('fetch', async () => ({}))

    render(<OrderPage id={ORDER_ID} />)

    expect(within(await screen.findByRole('alert')).getByText('Something went wrong')).toBeInTheDocument()
  })
})

describe('OrderPage when the order in the address changes', () => {
  // The answer for the first order is held back by hand; the answer for the other one arrives at once.
  function setupTwoOrders(late: Response) {
    const held = deferred<Response>()
    mockFetch([
      { method: 'GET', path: `/orders/${ORDER_ID}`, respond: () => held.promise },
      {
        method: 'GET',
        path: `/orders/${OTHER_ID}`,
        respond: () => jsonResponse(orderView({ id: OTHER_ID, status: 'SHIPPED', shippedAt: '2026-06-16T08:30:15Z' })),
      },
    ])
    return { release: () => held.resolve(late) }
  }

  async function flush() {
    await act(async () => {
      for (let turn = 0; turn < 50; turn++) await Promise.resolve()
    })
  }

  it('shows the loading message for the new order instead of the old one, then the new order', async () => {
    const held = deferred<Response>()
    mockFetch([
      { method: 'GET', path: `/orders/${ORDER_ID}`, respond: () => jsonResponse(orderView()) },
      { method: 'GET', path: `/orders/${OTHER_ID}`, respond: () => held.promise },
    ])
    const view = render(<OrderPage id={ORDER_ID} />)
    expect(await screen.findByText('PLACED')).toBeInTheDocument()

    view.rerender(<OrderPage id={OTHER_ID} />)

    expect(screen.getByRole('status')).toHaveTextContent('Loading order')
    expect(screen.queryByText('PLACED')).not.toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()

    held.resolve(jsonResponse(orderView({ id: OTHER_ID, status: 'SHIPPED', shippedAt: '2026-06-16T08:30:15Z' })))
    expect(await screen.findByText('SHIPPED')).toBeInTheDocument()
    expect(screen.queryByText('PLACED')).not.toBeInTheDocument()
  })

  it('keeps the new order when the answer of the old one arrives late', async () => {
    const lateOrder = jsonResponse(orderView())
    const bodyRead = vi.spyOn(lateOrder, 'text')
    const held = setupTwoOrders(lateOrder)
    const view = render(<OrderPage id={ORDER_ID} />)
    view.rerender(<OrderPage id={OTHER_ID} />)
    expect(await screen.findByText('SHIPPED')).toBeInTheDocument()

    held.release()
    await waitFor(() => expect(bodyRead).toHaveBeenCalled())
    await flush()

    expect(screen.getByText('SHIPPED')).toBeInTheDocument()
    expect(screen.queryByText('PLACED')).not.toBeInTheDocument()
    expect(facts()[0]).toEqual(['Order number', OTHER_ID])
  })

  it('keeps the new order when the failure of the old one arrives late', async () => {
    const lateFailure = problemResponse(404, {
      title: 'Order not found',
      detail: `No order with id ${ORDER_ID}`,
      code: 'order-not-found',
      details: [],
    })
    const bodyRead = vi.spyOn(lateFailure, 'text')
    const held = setupTwoOrders(lateFailure)
    const view = render(<OrderPage id={ORDER_ID} />)
    view.rerender(<OrderPage id={OTHER_ID} />)
    expect(await screen.findByText('SHIPPED')).toBeInTheDocument()

    held.release()
    await waitFor(() => expect(bodyRead).toHaveBeenCalled())
    await flush()

    expect(screen.getByText('SHIPPED')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
