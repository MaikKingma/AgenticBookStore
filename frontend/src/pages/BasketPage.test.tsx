import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { BasketProvider } from '../state/basket'
import { BasketPage } from './BasketPage'

const STORAGE_KEY = 'openbook.basket'

// Three lines in the order they were added. In basket order the ids, titles, prices and quantities all go up and down
// (neither ascending nor descending, as numbers or as text), so a page that sorts the lines cannot pass. The price
// 12.50 sits on the middle line (trailing zero), 4.35 on the last (a price whose cents are a classic float trap).
const BASKET_JSON =
  '[{"bookId":"6d2f9a14-3b07-4c85-9e1a-8f4d0b6c2e73","title":"Neuromancer","price":"7.99","currency":"EUR","quantity":2},' +
  '{"bookId":"0e5b8d37-1a94-4f62-b3c8-7d2a9e6f1b05","title":"Dune","price":"12.50","currency":"EUR","quantity":3},' +
  '{"bookId":"4a7c1e92-0b35-4d68-a9f4-2e8d6b1c7053","title":"Persuasion","price":"4.35","currency":"EUR","quantity":1}]'

function seed(json: string = BASKET_JSON) {
  sessionStorage.setItem(STORAGE_KEY, json)
}

function page() {
  return (
    <BasketProvider>
      <BasketPage />
    </BasketProvider>
  )
}

function storedText(): string | null {
  return sessionStorage.getItem(STORAGE_KEY)
}

function storedQuantities(): unknown {
  const text = storedText()
  return text === null ? null : (JSON.parse(text) as Array<{ quantity: unknown }>).map((line) => line.quantity)
}

function lineOf(title: string): HTMLElement {
  return screen.getAllByRole('listitem').find((item) => within(item).queryByRole('heading', { name: title }) !== null)!
}

function quantityInput(title: string): HTMLInputElement {
  return screen.getByRole('spinbutton', { name: `Quantity for ${title}` })
}

describe('BasketPage with lines', () => {
  it('has the heading Basket and lists the lines in the order they were added', () => {
    seed()

    render(page())

    expect(screen.getByRole('heading', { level: 1, name: 'Basket' })).toBeInTheDocument()
    expect(screen.getAllByRole('heading', { level: 2 }).map((heading) => heading.textContent)).toEqual([
      'Neuromancer',
      'Dune',
      'Persuasion',
    ])
  })

  it('shows the quantity of each line in an input labelled with the title of the book', () => {
    seed()

    render(page())

    expect(screen.getAllByRole('spinbutton').map((input) => (input as HTMLInputElement).value)).toEqual(['2', '3', '1'])
    expect(quantityInput('Neuromancer').value).toBe('2')
    expect(quantityInput('Dune').value).toBe('3')
    expect(quantityInput('Persuasion').value).toBe('1')
  })

  it('lets the browser offer the allowed quantities, from 1 to 10, on every input', () => {
    seed()

    render(page())

    expect(screen.getAllByRole('spinbutton').map((input) => [input.getAttribute('min'), input.getAttribute('max')])).toEqual([
      ['1', '10'],
      ['1', '10'],
      ['1', '10'],
    ])
  })

  it('shows the price of one copy and the total of each line, with the currency of the line', () => {
    seed()

    render(page())

    expect(within(lineOf('Neuromancer')).getByText('7.99 EUR each')).toBeInTheDocument()
    expect(within(lineOf('Neuromancer')).getByText('15.98 EUR')).toBeInTheDocument()
    expect(within(lineOf('Dune')).getByText('12.50 EUR each')).toBeInTheDocument()
    expect(within(lineOf('Dune')).getByText('37.50 EUR')).toBeInTheDocument()
    expect(within(lineOf('Persuasion')).getByText('4.35 EUR each')).toBeInTheDocument()
    expect(within(lineOf('Persuasion')).getByText('4.35 EUR')).toBeInTheDocument()
  })

  it('takes the currency from the lines, not from a fixed value', () => {
    seed(BASKET_JSON.replaceAll('"EUR"', '"GBP"'))

    render(page())

    expect(within(lineOf('Dune')).getByText('37.50 GBP')).toBeInTheDocument()
    expect(screen.getByText('57.83 GBP')).toBeInTheDocument()
    expect(screen.queryByText(/EUR/)).not.toBeInTheDocument()
  })

  it('writes the estimated total in the currency of the first line, and each line total in its own currency', () => {
    seed(
      '[{"bookId":"6d2f9a14-3b07-4c85-9e1a-8f4d0b6c2e73","title":"Neuromancer","price":"7.99","currency":"GBP","quantity":2},' +
        '{"bookId":"0e5b8d37-1a94-4f62-b3c8-7d2a9e6f1b05","title":"Dune","price":"12.50","currency":"EUR","quantity":3},' +
        '{"bookId":"4a7c1e92-0b35-4d68-a9f4-2e8d6b1c7053","title":"Persuasion","price":"4.35","currency":"USD","quantity":1}]',
    )

    render(page())

    expect(within(lineOf('Neuromancer')).getByText('15.98 GBP')).toBeInTheDocument()
    expect(within(lineOf('Dune')).getByText('37.50 EUR')).toBeInTheDocument()
    expect(within(lineOf('Persuasion')).getByText('4.35 USD')).toBeInTheDocument()
    expect(screen.getByText('57.83 GBP').parentElement?.textContent).toBe('Estimated total: 57.83 GBP')
  })

  it('shows an Estimated total next to the figure, worked out from the lines, and says the bookstore confirms it', () => {
    seed()

    render(page())

    const total = screen.getByText('57.83 EUR')
    expect(total.parentElement?.textContent).toBe('Estimated total: 57.83 EUR')
    expect(screen.getByText('The bookstore confirms the total when the order is placed.')).toBeInTheDocument()
  })

  it('links to the checkout', () => {
    seed()

    render(page())

    expect(screen.getByRole('link', { name: 'Go to checkout' })).toHaveAttribute('href', '#/checkout')
  })

})

describe('BasketPage changing a quantity', () => {
  it('shows 25.00 for the price 12.50 when its quantity is changed to 2, keeping the trailing zero of the cents', () => {
    seed()
    render(page())

    fireEvent.change(quantityInput('Dune'), { target: { value: '2' } })

    expect(within(lineOf('Dune')).getByText('25.00 EUR')).toBeInTheDocument()
    expect(screen.queryByText('25 EUR')).not.toBeInTheDocument()
    expect(screen.queryByText('37.50 EUR')).not.toBeInTheDocument()
    expect(screen.getByText('45.33 EUR')).toBeInTheDocument()
  })

  it('shows 13.05 for the price 4.35 when its quantity is changed to 3, without a rounding error', () => {
    seed()
    render(page())

    fireEvent.change(quantityInput('Persuasion'), { target: { value: '3' } })

    expect(within(lineOf('Persuasion')).getByText('13.05 EUR')).toBeInTheDocument()
    expect(screen.getByText('66.53 EUR')).toBeInTheDocument()
  })

  it('stores a typed 4 as the number 4 and shows it', () => {
    seed()
    render(page())

    fireEvent.change(quantityInput('Dune'), { target: { value: '4' } })

    expect(quantityInput('Dune').value).toBe('4')
    expect(storedQuantities()).toEqual([2, 4, 1])
    expect(within(lineOf('Dune')).getByText('50.00 EUR')).toBeInTheDocument()
  })

  it.each([['1'], ['10']])('accepts the quantity %s without a message', (typed) => {
    seed()
    render(page())

    fireEvent.change(quantityInput('Dune'), { target: { value: typed } })

    expect(quantityInput('Dune').value).toBe(typed)
    expect(storedQuantities()).toEqual([2, Number(typed), 1])
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it.each([
    ['0', '0'],
    ['11', '11'],
    ['an empty text', ''],
    ['2.5', '2.5'],
  ])('refuses %s with the message, and the input and the stored line stay at 3', (_name, typed) => {
    seed()
    render(page())
    const before = storedText()

    fireEvent.change(quantityInput('Dune'), { target: { value: typed } })

    expect(within(lineOf('Dune')).getByRole('alert')).toHaveTextContent('Quantity must be between 1 and 10.')
    expect(quantityInput('Dune').value).toBe('3')
    expect(storedText()).toBe(before)
    expect(within(lineOf('Dune')).getByText('37.50 EUR')).toBeInTheDocument()
    expect(screen.getByText('57.83 EUR')).toBeInTheDocument()
  })

  it('shows the message on the line that was refused and on no other', () => {
    seed()
    render(page())

    fireEvent.change(quantityInput('Persuasion'), { target: { value: '11' } })

    expect(screen.getAllByRole('alert')).toHaveLength(1)
    expect(within(lineOf('Persuasion')).getByRole('alert')).toBeInTheDocument()
    expect(within(lineOf('Neuromancer')).queryByRole('alert')).not.toBeInTheDocument()
    expect(within(lineOf('Dune')).queryByRole('alert')).not.toBeInTheDocument()
  })

  it('takes the message away when the next change is accepted', () => {
    seed()
    render(page())
    fireEvent.change(quantityInput('Dune'), { target: { value: '0' } })
    expect(screen.getByRole('alert')).toHaveTextContent('Quantity must be between 1 and 10.')

    fireEvent.change(quantityInput('Dune'), { target: { value: '5' } })

    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(storedQuantities()).toEqual([2, 5, 1])
  })

  it('moves the message to another line when that line is refused next', () => {
    seed()
    render(page())
    fireEvent.change(quantityInput('Dune'), { target: { value: '0' } })

    fireEvent.change(quantityInput('Neuromancer'), { target: { value: '11' } })

    expect(screen.getAllByRole('alert')).toHaveLength(1)
    expect(within(lineOf('Neuromancer')).getByRole('alert')).toBeInTheDocument()
  })
})

describe('BasketPage removing a line', () => {
  it('removes the line of the button that was clicked and keeps the others in their order', async () => {
    seed()
    const user = userEvent.setup()
    render(page())

    await user.click(screen.getByRole('button', { name: 'Remove Dune' }))

    expect(screen.getAllByRole('heading', { level: 2 }).map((heading) => heading.textContent)).toEqual([
      'Neuromancer',
      'Persuasion',
    ])
    expect(storedQuantities()).toEqual([2, 1])
    expect(screen.getByText('20.33 EUR')).toBeInTheDocument()
  })

  it('has one remove button per line, named after its book', () => {
    seed()

    render(page())

    expect(screen.getAllByRole('button').map((button) => button.getAttribute('aria-label'))).toEqual([
      'Remove Neuromancer',
      'Remove Dune',
      'Remove Persuasion',
    ])
  })

  it('shows the empty basket message when the last line is removed, and the storage has no entry', async () => {
    sessionStorage.setItem(
      STORAGE_KEY,
      '[{"bookId":"4a7c1e92-0b35-4d68-a9f4-2e8d6b1c7053","title":"Persuasion","price":"4.35","currency":"EUR","quantity":1}]',
    )
    const user = userEvent.setup()
    render(page())

    await user.click(screen.getByRole('button', { name: 'Remove Persuasion' }))

    expect(screen.getByText('Your basket is empty.')).toBeInTheDocument()
    expect(storedText()).toBeNull()
    expect(screen.queryByText(/Estimated total/)).not.toBeInTheDocument()
  })
})

describe('BasketPage with an empty basket', () => {
  it('says so and links to the catalogue, with no total and no checkout link', () => {
    render(page())

    expect(screen.getByRole('heading', { level: 1, name: 'Basket' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Your basket is empty.')
    expect(screen.getByRole('link', { name: 'Browse the catalogue' })).toHaveAttribute('href', '#/')
    expect(screen.queryByText(/Estimated total/)).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Go to checkout' })).not.toBeInTheDocument()
    expect(storedText()).toBeNull()
  })

  it('shows the empty basket message for a stored basket that cannot be read', () => {
    seed('{not json')

    render(page())

    expect(screen.getByText('Your basket is empty.')).toBeInTheDocument()
  })
})
