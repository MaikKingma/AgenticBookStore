import { useEffect, useState } from 'react'
import { searchBooks } from '../api/books'
import { ApiError } from '../api/client'
import type { BookView, Problem } from '../api/types'
import { BookCard } from '../components/BookCard'
import { ProblemDetailAlert } from '../components/ProblemDetailAlert'
import { SearchForm } from '../components/SearchForm'
import type { SearchCriteria } from '../components/SearchForm'
import { GENRES } from '../genres'

// Where the last search is kept for the rest of the browser session.
const LAST_SEARCH_KEY = 'openbook.lastSearch'

function emptySearch(): SearchCriteria {
  return { q: '', title: '', author: '', genre: '' }
}

// The stored search, or an empty one when nothing is stored or what is stored cannot be used (not JSON, not four
// texts, a genre the form does not offer).
function readLastSearch(): SearchCriteria {
  const stored = sessionStorage.getItem(LAST_SEARCH_KEY)
  if (stored === null) return emptySearch()
  try {
    const parsed: unknown = JSON.parse(stored)
    if (typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed)) {
      const { q, title, author, genre } = parsed as Record<string, unknown>
      if (
        typeof q === 'string' &&
        typeof title === 'string' &&
        typeof author === 'string' &&
        typeof genre === 'string' &&
        (genre === '' || (GENRES as readonly string[]).includes(genre))
      ) {
        return { q, title, author, genre }
      }
    }
  } catch {
    // Not JSON: fall through to the empty search.
  }
  return emptySearch()
}

type View =
  | { status: 'loading' }
  | { status: 'loaded'; books: BookView[] }
  | { status: 'failed'; problem: Problem }

// A request that failed for a reason the API client did not turn into a problem (a bug, not a bookstore answer).
function problemOf(error: unknown): Problem {
  return error instanceof ApiError ? error.problem : { status: 0, title: 'Something went wrong', details: [] }
}

// Lists the catalogue and searches it. The first request uses the last search of this session.
export function CataloguePage() {
  const [criteria, setCriteria] = useState(readLastSearch)
  const [view, setView] = useState<View>({ status: 'loading' })

  // Runs again for every submitted search. An answer that arrives after a newer search was started is dropped.
  useEffect(() => {
    let latest = true
    searchBooks(criteria).then(
      (books) => {
        if (latest) setView({ status: 'loaded', books })
      },
      (error: unknown) => {
        if (latest) setView({ status: 'failed', problem: problemOf(error) })
      },
    )
    return () => {
      latest = false
    }
  }, [criteria])

  function search(next: SearchCriteria) {
    sessionStorage.setItem(LAST_SEARCH_KEY, JSON.stringify(next))
    setView({ status: 'loading' })
    setCriteria(next)
  }

  return (
    <section>
      <h1>Catalogue</h1>
      <SearchForm initial={criteria} onSearch={search} />
      {view.status === 'loading' && <p role="status">Loading books</p>}
      {view.status === 'failed' && <ProblemDetailAlert problem={view.problem} />}
      {view.status === 'loaded' &&
        (view.books.length === 0 ? (
          <p role="status">No books match your search.</p>
        ) : (
          <ul className="book-grid">
            {view.books.map((book) => (
              <BookCard key={book.id} book={book} />
            ))}
          </ul>
        ))}
    </section>
  )
}
