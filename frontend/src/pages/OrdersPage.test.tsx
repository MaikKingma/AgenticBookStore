import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, onTestFinished, vi } from 'vitest'
import type { OrderSummaryView } from '../api/types'
import { jsonResponse, mockFetch, problemResponse } from '../test/fetchMock'
import type { FetchMock } from '../test/fetchMock'
import { OrdersPage } from './OrdersPage'

const FIRST_ID = '5e1a7c93-4b02-4d68-8a9f-0c3e6b1d7a24'
const SECOND_ID = '2d6b0f48-9c17-4e35-b8a2-7f1d4a6c0e59'
const THIRD_ID = '7a9c3e15-6d80-4f27-a1b4-3e5c8d0f2b61'

// The bookstore sends the orders newest first; the page keeps that order whatever it is. These three are in no order of
// their ids, statuses, times (as text or as moments: the second has the latest time, and the first and the third are
// in the same second, the third without a fraction), item counts or totals (as text or as numbers).
function summaries(): OrderSummaryView[] {
  return [
    { id: FIRST_ID, status: 'SHIPPED', placedAt: '2026-06-15T10:00:05.250000Z', itemCount: 3, total: '31.50' },
    { id: SECOND_ID, status: 'PLACED', placedAt: '2026-06-18T16:45:30.1Z', itemCount: 7, total: '9.99' },
    { id: THIRD_ID, status: 'SHIPPED', placedAt: '2026-06-15T10:00:05Z', itemCount: 2, total: '12.00' },
  ]
}

const SEARCH = '/orders?customerEmail='

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((done) => {
    resolve = done
  })
  return { promise, resolve }
}

function emailInput(): HTMLInputElement {
  return screen.getByLabelText('E-mail address')
}

function submitButton() {
  return screen.getByRole('button', { name: 'Show orders' })
}

function rowTexts(): Array<string | null> {
  return screen.getAllByRole('listitem').map((item) => item.textContent)
}

function parsedEmail(mock: FetchMock, index = 0): string | null {
  return new URL(mock.calls[index].path, `${'http'}://x`).searchParams.get('customerEmail')
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

describe('OrdersPage form', () => {
  it('has the heading Orders and a form with an e-mail field and a Show orders button, and asks for nothing yet', () => {
    const mock = mockFetch([])

    render(<OrdersPage />)

    expect(screen.getByRole('heading', { level: 1, name: 'Orders' })).toBeInTheDocument()
    const form = screen.getByRole('form', { name: 'Find your orders' })
    expect(form).toHaveAttribute('novalidate')
    expect(emailInput()).toHaveValue('')
    expect(within(form).getByRole('button', { name: 'Show orders' })).toBeEnabled()
    expect(screen.queryByRole('list')).not.toBeInTheDocument()
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
    expect(mock.calls).toEqual([])
  })

  it('prevents the default action of the form submit', async () => {
    mockFetch([{ method: 'GET', path: `${SEARCH}reader%40example.com`, respond: () => jsonResponse(summaries()) }])
    const submits = watchSubmits()
    const user = userEvent.setup()
    render(<OrdersPage />)

    await user.type(emailInput(), 'reader@example.com')
    await user.click(submitButton())

    expect(await screen.findAllByRole('listitem')).toHaveLength(3)
    expect(submits).toEqual([true])
  })
})

describe('OrdersPage listing', () => {
  it('lists the orders in the order the bookstore sent them, with status, time, item count and total in euro', async () => {
    const mock = mockFetch([{ method: 'GET', path: `${SEARCH}reader%40example.com`, respond: () => jsonResponse(summaries()) }])
    const user = userEvent.setup()
    render(<OrdersPage />)

    await user.type(emailInput(), 'reader@example.com')
    await user.click(submitButton())

    expect(await screen.findAllByRole('listitem')).toHaveLength(3)
    expect(rowTexts()).toEqual([
      `Order ${FIRST_ID} SHIPPED | 2026-06-15 10:00:05 UTC | Items: 3 | 31.50 EUR`,
      `Order ${SECOND_ID} PLACED | 2026-06-18 16:45:30 UTC | Items: 7 | 9.99 EUR`,
      `Order ${THIRD_ID} SHIPPED | 2026-06-15 10:00:05 UTC | Items: 2 | 12.00 EUR`,
    ])
    expect(parsedEmail(mock)).toBe('reader@example.com')
    expect(mock.calls.map((call) => call.path)).toEqual([`${SEARCH}reader%40example.com`])
  })

  it('links each order to its own page', async () => {
    mockFetch([{ method: 'GET', path: `${SEARCH}reader%40example.com`, respond: () => jsonResponse(summaries()) }])
    const user = userEvent.setup()
    render(<OrdersPage />)

    await user.type(emailInput(), 'reader@example.com')
    await user.click(submitButton())

    expect(await screen.findAllByRole('link')).toHaveLength(3)
    expect(screen.getAllByRole('link').map((link) => [link.textContent, link.getAttribute('href')])).toEqual([
      [`Order ${FIRST_ID}`, `#/orders/${FIRST_ID}`],
      [`Order ${SECOND_ID}`, `#/orders/${SECOND_ID}`],
      [`Order ${THIRD_ID}`, `#/orders/${THIRD_ID}`],
    ])
  })

  it('encodes an id that needs escaping in the link of its order', async () => {
    mockFetch([
      {
        method: 'GET',
        path: `${SEARCH}reader%40example.com`,
        respond: () => jsonResponse([{ ...summaries()[0], id: 'odd id/1' }, summaries()[1], summaries()[2]]),
      },
    ])
    const user = userEvent.setup()
    render(<OrdersPage />)

    await user.type(emailInput(), 'reader@example.com')
    await user.click(submitButton())

    expect(await screen.findAllByRole('link')).toHaveLength(3)
    expect(screen.getAllByRole('link').map((link) => link.getAttribute('href'))).toEqual([
      '#/orders/odd%20id%2F1',
      `#/orders/${SECOND_ID}`,
      `#/orders/${THIRD_ID}`,
    ])
  })

  it('sends the address exactly as typed, with its capitals and the spaces at both ends', async () => {
    const mock = mockFetch([
      { method: 'GET', path: `${SEARCH}+Reader%40Example.COM+`, respond: () => jsonResponse(summaries()) },
    ])
    const user = userEvent.setup()
    render(<OrdersPage />)

    await user.type(emailInput(), ' Reader@Example.COM ')
    await user.click(submitButton())

    expect(await screen.findAllByRole('listitem')).toHaveLength(3)
    expect(parsedEmail(mock)).toBe(' Reader@Example.COM ')
  })

  it('sends a plus sign of the address as a plus sign, not as a space', async () => {
    const mock = mockFetch([
      { method: 'GET', path: `${SEARCH}reader%2Bbooks%40example.com`, respond: () => jsonResponse(summaries()) },
    ])
    const user = userEvent.setup()
    render(<OrdersPage />)

    await user.type(emailInput(), 'reader+books@example.com')
    await user.click(submitButton())

    expect(await screen.findAllByRole('listitem')).toHaveLength(3)
    expect(parsedEmail(mock)).toBe('reader+books@example.com')
  })

  it('says so when the address has no orders', async () => {
    mockFetch([{ method: 'GET', path: `${SEARCH}reader%40example.com`, respond: () => jsonResponse([]) }])
    const user = userEvent.setup()
    render(<OrdersPage />)

    await user.type(emailInput(), 'reader@example.com')
    await user.click(submitButton())

    expect(await screen.findByRole('status')).toHaveTextContent('No orders found for this e-mail address.')
    expect(screen.queryByRole('list')).not.toBeInTheDocument()
  })

  it('shows a loading message until the answer arrives', async () => {
    const held = deferred<Response>()
    mockFetch([{ method: 'GET', path: `${SEARCH}reader%40example.com`, respond: () => held.promise }])
    const user = userEvent.setup()
    render(<OrdersPage />)

    await user.type(emailInput(), 'reader@example.com')
    await user.click(submitButton())

    expect(screen.getByRole('status')).toHaveTextContent('Loading orders')
    expect(screen.queryByRole('list')).not.toBeInTheDocument()

    held.resolve(jsonResponse(summaries()))

    expect(await screen.findAllByRole('listitem')).toHaveLength(3)
    expect(screen.queryByText('Loading orders')).not.toBeInTheDocument()
  })

  it('asks again when the same address is submitted again', async () => {
    const mock = mockFetch([{ method: 'GET', path: `${SEARCH}reader%40example.com`, respond: () => jsonResponse(summaries()) }])
    const user = userEvent.setup()
    render(<OrdersPage />)
    await user.type(emailInput(), 'reader@example.com')
    await user.click(submitButton())
    expect(await screen.findAllByRole('listitem')).toHaveLength(3)

    await user.click(submitButton())

    await waitFor(() => expect(mock.calls).toHaveLength(2))
    expect(await screen.findAllByRole('listitem')).toHaveLength(3)
  })
})

describe('OrdersPage errors', () => {
  it('shows the title and the details of an invalid e-mail answer', async () => {
    mockFetch([
      {
        method: 'GET',
        path: `${SEARCH}not-an-email`,
        respond: () =>
          problemResponse(422, {
            title: 'Invalid e-mail address',
            detail: 'The address is not an e-mail address',
            code: 'invalid-email',
            details: [
              { field: 'customerEmail', message: 'must have a domain' },
              { field: 'customerEmail', message: 'must contain an at sign' },
              { field: 'customerEmail', message: 'must have a name before the at sign' },
            ],
          }),
      },
    ])
    const user = userEvent.setup()
    render(<OrdersPage />)

    await user.type(emailInput(), 'not-an-email')
    await user.click(submitButton())

    const alert = await screen.findByRole('alert')
    expect(within(alert).getByText('Invalid e-mail address')).toBeInTheDocument()
    expect(within(alert).getByText('The address is not an e-mail address')).toBeInTheDocument()
    expect(within(alert).getAllByRole('listitem').map((item) => item.textContent)).toEqual([
      'customerEmail: must have a domain',
      'customerEmail: must contain an at sign',
      'customerEmail: must have a name before the at sign',
    ])
    expect(screen.queryByText(/Order /)).not.toBeInTheDocument()
  })

  it('asks the bookstore with an empty field too, and shows what the bookstore answers', async () => {
    const mock = mockFetch([
      {
        method: 'GET',
        path: SEARCH,
        respond: () => problemResponse(422, { title: 'Invalid e-mail address', code: 'invalid-email', details: [] }),
      },
    ])
    const user = userEvent.setup()
    render(<OrdersPage />)

    await user.click(submitButton())

    expect(within(await screen.findByRole('alert')).getByText('Invalid e-mail address')).toBeInTheDocument()
    expect(parsedEmail(mock)).toBe('')
    expect(mock.calls.map((call) => call.path)).toEqual([SEARCH])
  })

  it('replaces the alert with the list when the next search works', async () => {
    let attempts = 0
    mockFetch([
      {
        method: 'GET',
        path: `${SEARCH}reader%40example.com`,
        respond: () => {
          attempts += 1
          return attempts === 1
            ? problemResponse(422, { title: 'Invalid e-mail address', code: 'invalid-email', details: [] })
            : jsonResponse(summaries())
        },
      },
    ])
    const user = userEvent.setup()
    render(<OrdersPage />)
    await user.type(emailInput(), 'reader@example.com')
    await user.click(submitButton())
    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid e-mail address')

    await user.click(submitButton())

    expect(await screen.findAllByRole('listitem')).toHaveLength(3)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('shows the alert for a bookstore that cannot be reached', async () => {
    const mock = mockFetch([
      {
        method: 'GET',
        path: `${SEARCH}reader%40example.com`,
        respond: () => {
          throw new Error('connection refused')
        },
      },
    ])
    const user = userEvent.setup()
    render(<OrdersPage />)

    await user.type(emailInput(), 'reader@example.com')
    await user.click(submitButton())

    expect(within(await screen.findByRole('alert')).getByText('Cannot reach the bookstore')).toBeInTheDocument()
    expect(mock.calls.map((call) => call.path)).toEqual([`${SEARCH}reader%40example.com`])
  })

  it('shows a generic alert when the request fails in a way the API client does not report as a problem', async () => {
    // An answer without headers makes the client itself throw a TypeError instead of an ApiError.
    vi.stubGlobal('fetch', async () => ({}))
    render(<OrdersPage />)

    fireEvent.submit(screen.getByRole('form', { name: 'Find your orders' }))

    expect(within(await screen.findByRole('alert')).getByText('Something went wrong')).toBeInTheDocument()
  })
})

describe('OrdersPage when a newer search is started', () => {
  // The answer for the first address is held back by hand; the answer for the second one arrives at once.
  function setup(late: Response) {
    const held = deferred<Response>()
    mockFetch([
      { method: 'GET', path: `${SEARCH}first%40example.com`, respond: () => held.promise },
      {
        method: 'GET',
        path: `${SEARCH}second%40example.com`,
        respond: () => jsonResponse([summaries()[2], summaries()[1], summaries()[0]]),
      },
    ])
    return { release: () => held.resolve(late) }
  }

  async function searchTwice(user: ReturnType<typeof userEvent.setup>) {
    await user.type(emailInput(), 'first@example.com')
    await user.click(submitButton())
    await user.clear(emailInput())
    await user.type(emailInput(), 'second@example.com')
    await user.click(submitButton())
    expect(await screen.findAllByRole('listitem')).toHaveLength(3)
  }

  async function flush() {
    await act(async () => {
      for (let turn = 0; turn < 50; turn++) await Promise.resolve()
    })
  }

  it('keeps the list of the newer search when the answer of the older one arrives late', async () => {
    const lateList = jsonResponse([summaries()[0], summaries()[2], summaries()[1]])
    const bodyRead = vi.spyOn(lateList, 'text')
    const held = setup(lateList)
    const user = userEvent.setup()
    render(<OrdersPage />)
    await searchTwice(user)
    const before = rowTexts()

    held.release()
    await waitFor(() => expect(bodyRead).toHaveBeenCalled())
    await flush()

    expect(rowTexts()).toEqual(before)
    expect(before.map((text) => text?.slice(0, 42))).toEqual([
      `Order ${THIRD_ID}`,
      `Order ${SECOND_ID}`,
      `Order ${FIRST_ID}`,
    ])
  })

  it('keeps the list of the newer search when the failure of the older one arrives late', async () => {
    const lateFailure = problemResponse(422, { title: 'Invalid e-mail address', code: 'invalid-email', details: [] })
    const bodyRead = vi.spyOn(lateFailure, 'text')
    const held = setup(lateFailure)
    const user = userEvent.setup()
    render(<OrdersPage />)
    await searchTwice(user)

    held.release()
    await waitFor(() => expect(bodyRead).toHaveBeenCalled())
    await flush()

    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(screen.getAllByRole('listitem')).toHaveLength(3)
  })
})
