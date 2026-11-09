import type { BookView } from '../api/types'
import { Link } from '../router'
import { StockBadge } from './StockBadge'

// One book of the catalogue. Every value is shown as the bookstore sent it: the authors string, the genre name, the
// price string with its own currency. The card links to the detail page and carries no basket button.
export function BookCard({ book }: { book: BookView }) {
  return (
    <li>
      <article className="card">
        <h2>
          <Link to={`/books/${encodeURIComponent(book.id)}`}>{book.title}</Link>
        </h2>
        <p>{book.authors}</p>
        <p>{book.genre}</p>
        <p className="card-price">{`${book.price} ${book.currency}`}</p>
        <StockBadge inStock={book.inStock} />
      </article>
    </li>
  )
}
