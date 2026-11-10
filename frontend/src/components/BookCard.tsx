import type { BookView } from '../api/types'
import { genreLabel } from '../genres'
import { Link } from '../router'
import { StockBadge } from './StockBadge'

// One book of the catalogue. The authors string and the price string with its own currency are shown as the bookstore
// sent them; the genre is shown as its sentence-case label, never as the enum name. The card links to the detail page
// and carries no basket button.
export function BookCard({ book }: { book: BookView }) {
  return (
    <li>
      <article className="card">
        <h2>
          <Link to={`/books/${encodeURIComponent(book.id)}`}>{book.title}</Link>
        </h2>
        <p>{book.authors}</p>
        <p>{genreLabel(book.genre)}</p>
        <p className="card-price">{`${book.price} ${book.currency}`}</p>
        <StockBadge inStock={book.inStock} />
      </article>
    </li>
  )
}
