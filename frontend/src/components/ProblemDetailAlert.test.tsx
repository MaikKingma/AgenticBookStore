import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { Problem } from '../api/types'
import { ProblemDetailAlert } from './ProblemDetailAlert'

function itemsOf(alert: HTMLElement): Array<string | null> {
  return within(alert).getAllByRole('listitem').map((item) => item.textContent)
}

describe('ProblemDetailAlert', () => {
  it('is an alert that shows the title and the detail', () => {
    const problem: Problem = {
      status: 409,
      title: 'Insufficient stock',
      detail: 'Not enough copies to place the order',
      code: 'insufficient-stock',
      details: [],
    }

    render(<ProblemDetailAlert problem={problem} />)

    const alert = screen.getByRole('alert')
    expect(within(alert).getByText('Insufficient stock')).toBeInTheDocument()
    expect(within(alert).getByText('Not enough copies to place the order')).toBeInTheDocument()
  })

  it('shows a problem with a title only and no details without a list', () => {
    render(<ProblemDetailAlert problem={{ status: 0, title: 'Cannot reach the bookstore', details: [] }} />)

    const alert = screen.getByRole('alert')
    expect(within(alert).getByText('Cannot reach the bookstore')).toBeInTheDocument()
    expect(within(alert).queryByRole('list')).not.toBeInTheDocument()
    expect(alert.textContent).toBe('Cannot reach the bookstore')
  })

  it('shows the message next to the field name and keeps both entries of a repeated field in the given order', () => {
    const problem: Problem = {
      status: 400,
      title: 'Validation failed',
      code: 'validation-failed',
      details: [
        { field: 'customerEmail', message: 'must be a well-formed email address' },
        { field: 'lines', message: 'must not be empty' },
        { field: 'customerEmail', message: 'must not be blank' },
      ],
    }

    render(<ProblemDetailAlert problem={problem} />)

    expect(itemsOf(screen.getByRole('alert'))).toEqual([
      'customerEmail: must be a well-formed email address',
      'lines: must not be empty',
      'customerEmail: must not be blank',
    ])
  })

  it('lists the genre names of an unknown genre problem in the given order', () => {
    const problem: Problem = {
      status: 422,
      title: 'Unknown genre',
      code: 'unknown-genre',
      details: [{ genre: 'POETRY' }, { genre: 'FICTION' }, { genre: 'HISTORY' }],
    }

    render(<ProblemDetailAlert problem={problem} />)

    expect(itemsOf(screen.getByRole('alert'))).toEqual(['POETRY', 'FICTION', 'HISTORY'])
  })

  it('shows each shortage with the requested and the available quantity exactly as given, a zero included', () => {
    const problem: Problem = {
      status: 409,
      title: 'Insufficient stock',
      code: 'insufficient-stock',
      details: [
        { bookId: '5a3e9d71-2c84-4f06-b1d9-8e7a0c3b6f42', requested: '3', available: '0' },
        { bookId: '1c8f2b46-9e05-4a73-8d1c-6b2f4e9a7d30', requested: '10', available: '4' },
        { bookId: '7d0b6e25-3f91-4c8a-a5e2-0d9b1c4f8a67', requested: '2', available: '1' },
      ],
    }

    render(<ProblemDetailAlert problem={problem} />)

    expect(itemsOf(screen.getByRole('alert'))).toEqual([
      'Book 5a3e9d71-2c84-4f06-b1d9-8e7a0c3b6f42: requested 3, available 0',
      'Book 1c8f2b46-9e05-4a73-8d1c-6b2f4e9a7d30: requested 10, available 4',
      'Book 7d0b6e25-3f91-4c8a-a5e2-0d9b1c4f8a67: requested 2, available 1',
    ])
  })

  it('shows a detail of an unknown shape as key and value text in the key order it came in', () => {
    const problem: Problem = {
      status: 500,
      title: 'Something else',
      details: [{ foo: 'bar' }, { code: 'x7', note: 'second' }, { zebra: '1', apple: '2' }],
    }

    render(<ProblemDetailAlert problem={problem} />)

    expect(itemsOf(screen.getByRole('alert'))).toEqual(['foo: bar', 'code: x7, note: second', 'zebra: 1, apple: 2'])
  })

  it('treats a detail with extra keys beside field and message as an unknown shape', () => {
    const problem: Problem = {
      status: 400,
      title: 'Validation failed',
      details: [{ field: 'customerEmail', message: 'must not be blank', rejected: 'none' }],
    }

    render(<ProblemDetailAlert problem={problem} />)

    expect(itemsOf(screen.getByRole('alert'))).toEqual(['field: customerEmail, message: must not be blank, rejected: none'])
  })

  it('shows an empty detail object as an empty list item rather than dropping it', () => {
    render(<ProblemDetailAlert problem={{ status: 500, title: 'Broken', details: [{}] }} />)

    expect(itemsOf(screen.getByRole('alert'))).toEqual([''])
  })

  it('renders markup in a title or a value as plain text', () => {
    const markup = ['<', 'b>bold<', '/b>'].join('')
    render(<ProblemDetailAlert problem={{ status: 500, title: markup, details: [{ foo: markup }] }} />)

    const alert = screen.getByRole('alert')
    expect(within(alert).getByText(markup)).toBeInTheDocument()
    expect(within(alert).getByRole('listitem').textContent).toBe(`foo: ${markup}`)
    expect(alert.querySelector('b')).toBeNull()
  })
})
