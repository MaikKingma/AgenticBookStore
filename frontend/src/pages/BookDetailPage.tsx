import { useEffect, useState } from 'react'
import { getBook } from '../api/books'
import { ApiError } from '../api/client'
import { getStock } from '../api/inventory'
import type { BookDetailView, Problem, StockView } from '../api/types'
import { genreLabel } from '../genres'
import { ProblemDetailAlert } from '../components/ProblemDetailAlert'
import { StockBadge } from '../components/StockBadge'
import { Link } from '../router'
import { useBasket } from '../state/basket'

// The answer for one book id. Keeping the id in the result lets the page ignore an answer that belongs to another book
// than the one in the address now.
type Result =
  | { status: 'loaded'; id: string; book: BookDetailView; stock: StockView }
  | { status: 'failed'; id: string; problem: Problem }

// A request that failed for a reason the API client did not turn into a problem (a bug, not a bookstore answer).
function problemOf(error: unknown): Problem {
  return error instanceof ApiError ? error.problem : { status: 0, title: 'Something went wrong', details: [] }
}

type Feedback = { kind: 'added' } | { kind: 'refused'; message: string }

// The facts of a loaded book and the button that puts it in the basket. Between two books the page shows the loading
// message, so this component is gone and the feedback of one book never shows on another.
function BookDetail({ book, stock }: { book: BookDetailView; stock: StockView }) {
  const basket = useBasket()
  const [feedback, setFeedback] = useState<Feedback | undefined>(undefined)

  // The copies come from the stock answer as given: none means out of stock, and nothing is calculated from them.
  const discontinued = book.status === 'DISCONTINUED'
  const outOfStock = stock.available === 0

  function add() {
    const message = basket.add({ id: book.id, title: book.title, price: book.price, currency: book.currency })
    setFeedback(message === undefined ? { kind: 'added' } : { kind: 'refused', message })
  }

  return (
    <div className="card">
      <dl className="book-facts">
        <dt>ISBN</dt>
        <dd>{book.isbn}</dd>
        <dt>Authors</dt>
        <dd>
          <ul>
            {book.authors.map((author, index) => (
              <li key={index}>{author}</li>
            ))}
          </ul>
        </dd>
        <dt>Genre</dt>
        <dd>{genreLabel(book.genre)}</dd>
        <dt>Published</dt>
        <dd>{book.publicationYear}</dd>
        <dt>Price</dt>
        <dd>{`${book.price} ${book.currency}`}</dd>
        <dt>Availability</dt>
        <dd>
          {discontinued ? (
            <span className="badge badge-out-of-stock">Discontinued</span>
          ) : (
            <>
              <StockBadge inStock={!outOfStock} />
              {!outOfStock && <span className="stock-note">{`${stock.available} available`}</span>}
              {!outOfStock && stock.lowStock && <span className="stock-note">Low stock</span>}
            </>
          )}
        </dd>
      </dl>
      <div className="book-actions">
        <button type="button" className="button button-primary" disabled={discontinued || outOfStock} onClick={add}>
          Add to basket
        </button>
        {feedback?.kind === 'added' && (
          <p role="status">
            Added to your basket. <Link to="/basket">View basket</Link>
          </p>
        )}
        {feedback?.kind === 'refused' && (
          <p role="alert" className="alert">
            {feedback.message}
          </p>
        )}
      </div>
    </div>
  )
}

// One book with its stock level. The book and its stock are two requests; the page shows them together, or the alert
// of the first one that failed.
export function BookDetailPage({ id }: { id: string }) {
  const [result, setResult] = useState<Result | undefined>(undefined)

  // Runs again when the book in the address changes. An answer that arrives after that is dropped.
  useEffect(() => {
    let latest = true
    Promise.all([getBook(id), getStock(id)]).then(
      ([book, stock]) => {
        if (latest) setResult({ status: 'loaded', id, book, stock })
      },
      (error: unknown) => {
        if (latest) setResult({ status: 'failed', id, problem: problemOf(error) })
      },
    )
    return () => {
      latest = false
    }
  }, [id])

  const view = result?.id === id ? result : undefined

  return (
    <section>
      <h1>{view?.status === 'loaded' ? view.book.title : 'Book'}</h1>
      <p>
        <Link to="/">Back to the catalogue</Link>
      </p>
      {view === undefined && <p role="status">Loading book</p>}
      {view?.status === 'failed' && <ProblemDetailAlert problem={view.problem} />}
      {view?.status === 'loaded' && <BookDetail book={view.book} stock={view.stock} />}
    </section>
  )
}
