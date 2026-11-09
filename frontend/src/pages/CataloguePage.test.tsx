import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, onTestFinished, vi } from 'vitest'
import type { BookView } from '../api/types'
import { jsonResponse, mockFetch, problemResponse } from '../test/fetchMock'
import { CataloguePage } from './CataloguePage'

// The three books of the first listing. Every attribute differs between the books and is sorted in no direction over
// the three (title, authors, genre, price as a string and as a number, currency, status, available, inStock, ids).
// The genre of the second book has an underscore, so a card that rewrites the name (spaces, case) is caught.
// Two books are on purpose out of step: the first has no copies but is in stock, the second has copies but is not.
const LISTING: readonly BookView[] = Object.freeze([
  {
    id: '4b6e0a52-9d3f-4c1e-8a77-2f5d1c9b0e13',
    isbn: '9783161484100',
    title: 'Good Omens',
    authors: 'Terry Pratchett, Neil Gaiman',
    genre: 'FANTASY',
    price: '12.50',
    currency: 'EUR',
    status: 'AVAILABLE',
    available: 0,
    inStock: true,
  },
  {
    id: '1a9c3f87-5e20-4b6d-9c14-7e8d2a0f6b35',
    isbn: '9780306406157',
    title: 'Emma',
    authors: 'Jane Austen',
    genre: 'SCIENCE_FICTION',
    price: '7.99',
    currency: 'GBP',
    status: 'DISCONTINUED',
    available: 3,
    inStock: false,
  },
  {
    id: '7e2d5c91-0b48-4f3a-a6d7-3c1e9b8a4f20',
    isbn: '9780596520687',
    title: 'Neuromancer',
    authors: 'William Gibson',
    genre: 'FICTION',
    price: '10.00',
    currency: 'EUR',
    status: 'AVAILABLE',
    available: 2,
    inStock: true,
  },
])

// The answer to a search. None of its books is in the first listing. It is a fixture, not a model of the server's
// genre filter, so that the genre differs between the books.
const SEARCH_RESULT: readonly BookView[] = Object.freeze([
  {
    id: '6f13c8a4-7d29-4e50-b3a1-9c0d5e2f8b71',
    isbn: '9780141439518',
    title: 'Pride and Prejudice and Zombies',
    authors: 'Jane Austen, Seth Grahame-Smith',
    genre: 'FICTION',
    price: '19.99',
    currency: 'EUR',
    status: 'AVAILABLE',
    available: 5,
    inStock: true,
  },
  {
    id: '2e7b5d90-1c64-4a8f-8e3b-5a9f0d7c2e14',
    isbn: '9780199535569',
    title: 'Northanger Abbey',
    authors: 'Jane Austen',
    genre: 'FANTASY',
    price: '8.50',
    currency: 'USD',
    status: 'DISCONTINUED',
    available: 0,
    inStock: false,
  },
  {
    id: '70c4e1b8-3a95-4d26-9f08-1b6e4c7a3d52',
    isbn: '9780141439815',
    title: 'Sense and Sensibility and Sea Monsters',
    authors: 'Jane Austen, Ben H. Winters',
    genre: 'FICTION',
    price: '14.00',
    currency: 'EUR',
    status: 'AVAILABLE',
    available: 1,
    inStock: true,
  },
])

const LAST_SEARCH_KEY = 'openbook.lastSearch'

const LISTING_ROUTE = { method: 'GET', path: '/books', respond: () => jsonResponse(LISTING) } as const

function titlesShown(): Array<string | null> {
  return screen.getAllByRole('heading', { level: 2 }).map((heading) => heading.textContent)
}

function searchBox() {
  return screen.getByRole('searchbox', { name: 'Search' })
}

function genreSelect() {
  return screen.getByRole('combobox', { name: 'Genre' })
}

describe('CataloguePage listing', () => {
  it('shows one card per book with every value as the bookstore sent it, in the order it sent them', async () => {
    mockFetch([LISTING_ROUTE])

    render(<CataloguePage />)

    expect(await screen.findByRole('heading', { level: 1, name: 'Catalogue' })).toBeInTheDocument()
    const cards = await screen.findAllByRole('listitem')
    expect(titlesShown()).toEqual(['Good Omens', 'Emma', 'Neuromancer'])
    const expected = [
      { title: 'Good Omens', authors: 'Terry Pratchett, Neil Gaiman', genre: 'FANTASY', price: '12.50 EUR', badge: 'In stock' },
      { title: 'Emma', authors: 'Jane Austen', genre: 'SCIENCE_FICTION', price: '7.99 GBP', badge: 'Out of stock' },
      { title: 'Neuromancer', authors: 'William Gibson', genre: 'FICTION', price: '10.00 EUR', badge: 'In stock' },
    ]
    expect(cards).toHaveLength(expected.length)
    expected.forEach((book, index) => {
      const card = within(cards[index])
      expect(card.getByRole('heading', { level: 2 }).textContent).toBe(book.title)
      expect(card.getByText(book.authors)).toBeInTheDocument()
      expect(card.getByText(book.genre)).toBeInTheDocument()
      expect(card.getByText(book.price)).toBeInTheDocument()
      expect(card.getByText(book.badge)).toBeInTheDocument()
    })
  })

  it('reads the badge from inStock: no copies and in stock, three copies and not in stock', async () => {
    mockFetch([LISTING_ROUTE])

    render(<CataloguePage />)

    const cards = await screen.findAllByRole('listitem')
    expect(within(cards[0]).getByText('In stock')).toBeInTheDocument()
    expect(within(cards[0]).queryByText('Out of stock')).not.toBeInTheDocument()
    expect(within(cards[1]).getByText('Out of stock')).toBeInTheDocument()
    expect(within(cards[1]).queryByText('In stock')).not.toBeInTheDocument()
  })

  it('links every card to the detail page of its own book', async () => {
    mockFetch([LISTING_ROUTE])

    render(<CataloguePage />)

    const links = await screen.findAllByRole('link')
    expect(links.map((link) => [link.textContent, link.getAttribute('href')])).toEqual([
      ['Good Omens', '#/books/4b6e0a52-9d3f-4c1e-8a77-2f5d1c9b0e13'],
      ['Emma', '#/books/1a9c3f87-5e20-4b6d-9c14-7e8d2a0f6b35'],
      ['Neuromancer', '#/books/7e2d5c91-0b48-4f3a-a6d7-3c1e9b8a4f20'],
    ])
  })

  it('has no basket button on a card', async () => {
    mockFetch([LISTING_ROUTE])

    render(<CataloguePage />)

    const cards = await screen.findAllByRole('listitem')
    expect(cards).toHaveLength(3)
    for (const card of cards) {
      expect(within(card).queryByRole('button')).not.toBeInTheDocument()
    }
  })

  it('says so when the catalogue is empty', async () => {
    mockFetch([{ method: 'GET', path: '/books', respond: () => jsonResponse([]) }])

    render(<CataloguePage />)

    expect(await screen.findByText('No books match your search.')).toBeInTheDocument()
    expect(screen.queryAllByRole('listitem')).toEqual([])
  })

  it('shows a loading message until the answer arrives', async () => {
    let release!: (response: Response) => void
    const pending = new Promise<Response>((resolve) => {
      release = resolve
    })
    mockFetch([{ method: 'GET', path: '/books', respond: () => pending }])

    render(<CataloguePage />)
    expect(screen.getByRole('status')).toHaveTextContent('Loading books')
    release(jsonResponse(LISTING))

    expect(await screen.findAllByRole('listitem')).toHaveLength(3)
    expect(screen.queryByText('Loading books')).not.toBeInTheDocument()
  })
})

describe('CataloguePage search', () => {
  it('offers Any genre and the ten genre names in declaration order', async () => {
    mockFetch([LISTING_ROUTE])

    render(<CataloguePage />)

    await screen.findAllByRole('listitem')
    expect(within(genreSelect()).getAllByRole('option').map((option) => option.textContent)).toEqual([
      'Any genre',
      'FICTION',
      'SCIENCE_FICTION',
      'FANTASY',
      'MYSTERY',
      'ROMANCE',
      'HISTORY',
      'SCIENCE',
      'TECHNOLOGY',
      'CHILDREN',
      'POETRY',
    ])
    expect(genreSelect()).toHaveValue('')
  })

  it('asks for the entered text and genre and replaces the listing with the result', async () => {
    const mock = mockFetch([
      LISTING_ROUTE,
      { method: 'GET', path: '/books?q=austen&genre=FICTION', respond: () => jsonResponse(SEARCH_RESULT) },
    ])
    const user = userEvent.setup()
    // A listener on the document runs after React's listener on the root, so it sees whether the page cancelled the
    // browser's own form submission (which would reload the page and lose the search).
    const submitCancelled: boolean[] = []
    const recordSubmit = (event: Event) => submitCancelled.push(event.defaultPrevented)
    document.addEventListener('submit', recordSubmit)
    onTestFinished(() => document.removeEventListener('submit', recordSubmit))
    render(<CataloguePage />)
    await screen.findAllByRole('listitem')
    expect(titlesShown()).toEqual(['Good Omens', 'Emma', 'Neuromancer'])

    await user.type(searchBox(), 'austen')
    await user.selectOptions(genreSelect(), 'FICTION')
    await user.click(screen.getByRole('button', { name: 'Search' }))

    expect(await screen.findByRole('heading', { level: 2, name: 'Northanger Abbey' })).toBeInTheDocument()
    expect(titlesShown()).toEqual(['Pride and Prejudice and Zombies', 'Northanger Abbey', 'Sense and Sensibility and Sea Monsters'])
    expect(screen.queryByRole('heading', { name: 'Good Omens' })).not.toBeInTheDocument()
    expect(mock.calls.map((call) => call.path)).toEqual(['/books', '/books?q=austen&genre=FICTION'])
    expect(submitCancelled).toEqual([true])
  })

  it('sends the title and author criteria, and no genre after going back to Any genre', async () => {
    const mock = mockFetch([
      LISTING_ROUTE,
      { method: 'GET', path: '/books?title=Emma&author=Austen', respond: () => jsonResponse([]) },
    ])
    const user = userEvent.setup()
    render(<CataloguePage />)
    await screen.findAllByRole('listitem')

    await user.type(screen.getByRole('textbox', { name: 'Title' }), 'Emma')
    await user.type(screen.getByRole('textbox', { name: 'Author' }), 'Austen')
    await user.selectOptions(genreSelect(), 'POETRY')
    await user.selectOptions(genreSelect(), 'Any genre')
    await user.click(screen.getByRole('button', { name: 'Search' }))

    expect(await screen.findByText('No books match your search.')).toBeInTheDocument()
    expect(mock.calls.map((call) => call.path)).toEqual(['/books', '/books?title=Emma&author=Austen'])
  })

  it('sends text with spaces at both ends exactly as typed', async () => {
    const mock = mockFetch([
      LISTING_ROUTE,
      { method: 'GET', path: '/books?q=+pride+&title=+Emma+&author=+Austen+', respond: () => jsonResponse([]) },
    ])
    const user = userEvent.setup()
    render(<CataloguePage />)
    await screen.findAllByRole('listitem')

    await user.type(searchBox(), ' pride ')
    await user.type(screen.getByRole('textbox', { name: 'Title' }), ' Emma ')
    await user.type(screen.getByRole('textbox', { name: 'Author' }), ' Austen ')
    await user.click(screen.getByRole('button', { name: 'Search' }))

    expect(await screen.findByText('No books match your search.')).toBeInTheDocument()
    expect(mock.calls.map((call) => call.path)).toEqual(['/books', '/books?q=+pride+&title=+Emma+&author=+Austen+'])
  })

  it('shows the answer of the latest search when an older answer arrives late', async () => {
    let releaseFirst!: (response: Response) => void
    let releaseSecond!: (response: Response) => void
    const first = new Promise<Response>((resolve) => {
      releaseFirst = resolve
    })
    const second = new Promise<Response>((resolve) => {
      releaseSecond = resolve
    })
    const mock = mockFetch([
      { method: 'GET', path: '/books', respond: () => first },
      { method: 'GET', path: '/books?q=austen', respond: () => second },
    ])
    const user = userEvent.setup()
    render(<CataloguePage />)
    await user.type(searchBox(), 'austen')
    await user.click(screen.getByRole('button', { name: 'Search' }))
    await waitFor(() => expect(mock.calls).toHaveLength(2))

    releaseSecond(jsonResponse(SEARCH_RESULT))
    expect(await screen.findByRole('heading', { level: 2, name: 'Northanger Abbey' })).toBeInTheDocument()
    const lateAnswer = jsonResponse(LISTING)
    const bodyRead = vi.spyOn(lateAnswer, 'text')
    releaseFirst(lateAnswer)
    await waitFor(() => expect(bodyRead).toHaveBeenCalled())

    expect(await screen.findByRole('heading', { level: 2, name: 'Northanger Abbey' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Good Omens' })).not.toBeInTheDocument()
  })
})

describe('CataloguePage errors', () => {
  // The bookstore's own list of genres: not in the order of the form, and it holds a name the form does not offer.
  const SERVER_GENRES = [
    'MYSTERY',
    'FANTASY',
    'POETRY',
    'CHILDREN',
    'SCIENCE',
    'FICTION',
    'GRAPHIC_NOVEL',
    'ROMANCE',
    'HISTORY',
    'SCIENCE_FICTION',
  ]

  it('shows the title, the detail and the genre list of the server for an unknown genre', async () => {
    const mock = mockFetch([
      LISTING_ROUTE,
      {
        method: 'GET',
        path: '/books?genre=MYSTERY',
        respond: () =>
          problemResponse(422, {
            title: 'Unknown genre',
            detail: 'No genre called MYSTERY is known',
            code: 'unknown-genre',
            details: SERVER_GENRES.map((genre) => ({ genre })),
          }),
      },
    ])
    const user = userEvent.setup()
    render(<CataloguePage />)
    await screen.findAllByRole('listitem')

    await user.selectOptions(genreSelect(), 'MYSTERY')
    await user.click(screen.getByRole('button', { name: 'Search' }))

    const alert = await screen.findByRole('alert')
    expect(within(alert).getByText('Unknown genre')).toBeInTheDocument()
    expect(within(alert).getByText('No genre called MYSTERY is known')).toBeInTheDocument()
    expect(within(alert).getAllByRole('listitem').map((item) => item.textContent)).toEqual(SERVER_GENRES)
    expect(screen.queryByRole('heading', { name: 'Good Omens' })).not.toBeInTheDocument()
    expect(mock.calls.map((call) => call.path)).toEqual(['/books', '/books?genre=MYSTERY'])
  })

  it('shows the alert for a bookstore that cannot be reached', async () => {
    const mock = mockFetch([
      {
        method: 'GET',
        path: '/books',
        respond: () => {
          throw new Error('connection refused')
        },
      },
    ])

    render(<CataloguePage />)

    const alert = await screen.findByRole('alert')
    expect(within(alert).getByText('Cannot reach the bookstore')).toBeInTheDocument()
    expect(mock.calls.map((call) => call.path)).toEqual(['/books'])
  })

  it('shows the alert for the plain error JSON of the framework', async () => {
    mockFetch([
      {
        method: 'GET',
        path: '/books',
        respond: () => jsonResponse({ status: 400, error: 'Bad Request', path: '/books' }, 400, 'Bad Request'),
      },
    ])

    render(<CataloguePage />)

    const alert = await screen.findByRole('alert')
    expect(within(alert).getByText('Bad Request')).toBeInTheDocument()
    expect(within(alert).queryByRole('list')).not.toBeInTheDocument()
  })

  it('shows a generic alert when the request fails in a way the API client does not report as a problem', async () => {
    // An answer without headers makes the client itself throw a TypeError instead of an ApiError.
    vi.stubGlobal('fetch', async () => ({}))

    render(<CataloguePage />)

    const alert = await screen.findByRole('alert')
    expect(within(alert).getByText('Something went wrong')).toBeInTheDocument()
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('replaces the alert with the result of the next search', async () => {
    mockFetch([
      {
        method: 'GET',
        path: '/books',
        respond: () => problemResponse(503, { title: 'Catalogue offline', code: 'catalogue-offline', details: [] }),
      },
      { method: 'GET', path: '/books?q=emma', respond: () => jsonResponse(SEARCH_RESULT) },
    ])
    const user = userEvent.setup()
    render(<CataloguePage />)
    expect(await screen.findByText('Catalogue offline')).toBeInTheDocument()

    await user.type(searchBox(), 'emma')
    await user.click(screen.getByRole('button', { name: 'Search' }))

    expect(await screen.findByRole('heading', { level: 2, name: 'Northanger Abbey' })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})

describe('CataloguePage last search', () => {
  it('keeps the search in the session and uses it when the page comes back', async () => {
    const mock = mockFetch([
      LISTING_ROUTE,
      { method: 'GET', path: '/books?q=austen&genre=FICTION', respond: () => jsonResponse(SEARCH_RESULT) },
    ])
    const user = userEvent.setup()
    const firstVisit = render(<CataloguePage />)
    await screen.findAllByRole('listitem')
    await user.type(searchBox(), 'austen')
    await user.selectOptions(genreSelect(), 'FICTION')
    await user.click(screen.getByRole('button', { name: 'Search' }))
    expect(await screen.findByRole('heading', { level: 2, name: 'Northanger Abbey' })).toBeInTheDocument()
    expect(JSON.parse(sessionStorage.getItem(LAST_SEARCH_KEY) ?? 'null')).toEqual({
      q: 'austen',
      title: '',
      author: '',
      genre: 'FICTION',
    })

    firstVisit.unmount()
    render(<CataloguePage />)

    expect(await screen.findByRole('heading', { level: 2, name: 'Northanger Abbey' })).toBeInTheDocument()
    expect(searchBox()).toHaveValue('austen')
    expect(screen.getByRole('textbox', { name: 'Title' })).toHaveValue('')
    expect(screen.getByRole('textbox', { name: 'Author' })).toHaveValue('')
    expect(genreSelect()).toHaveValue('FICTION')
    expect(mock.calls.map((call) => call.path)).toEqual([
      '/books',
      '/books?q=austen&genre=FICTION',
      '/books?q=austen&genre=FICTION',
    ])
  })

  it('fills the form and the first request from a stored search, spaces at both ends included', async () => {
    sessionStorage.setItem(
      LAST_SEARCH_KEY,
      JSON.stringify({ q: '', title: ' Emma ', author: ' Austen ', genre: 'ROMANCE' }),
    )
    const mock = mockFetch([
      { method: 'GET', path: '/books?title=+Emma+&author=+Austen+&genre=ROMANCE', respond: () => jsonResponse([]) },
    ])

    render(<CataloguePage />)

    expect(await screen.findByText('No books match your search.')).toBeInTheDocument()
    expect(searchBox()).toHaveValue('')
    expect(screen.getByRole('textbox', { name: 'Title' })).toHaveValue(' Emma ')
    expect(screen.getByRole('textbox', { name: 'Author' })).toHaveValue(' Austen ')
    expect(genreSelect()).toHaveValue('ROMANCE')
    expect(mock.calls.map((call) => call.path)).toEqual(['/books?title=+Emma+&author=+Austen+&genre=ROMANCE'])
  })

  it.each([
    ['text that is not JSON', 'not json'],
    ['JSON null', 'null'],
    ['a JSON array', '[]'],
    ['a number where text belongs', '{"q":5,"title":"","author":"","genre":""}'],
    ['a missing field', '{"q":"austen","title":"","author":""}'],
    ['a genre the form does not offer', '{"q":"austen","title":"","author":"","genre":"cookbooks"}'],
  ])('starts with an empty search when the stored one is %s', async (_name, stored) => {
    sessionStorage.setItem(LAST_SEARCH_KEY, stored)
    const mock = mockFetch([LISTING_ROUTE])

    render(<CataloguePage />)

    expect(await screen.findAllByRole('listitem')).toHaveLength(3)
    expect(searchBox()).toHaveValue('')
    expect(genreSelect()).toHaveValue('')
    expect(mock.calls.map((call) => call.path)).toEqual(['/books'])
  })
})
