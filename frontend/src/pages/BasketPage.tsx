import { useId, useState } from 'react'
import { EstimatedTotal } from '../components/BasketSummary'
import { MAX_QUANTITY_PER_LINE, MIN_QUANTITY_PER_LINE } from '../limits'
import { Link } from '../router'
import { useBasket } from '../state/basket'
import type { BasketLine } from '../state/basket'
import { lineTotal } from '../state/estimate'

// The refusal of the basket for one line, shown under that line.
interface Refusal {
  bookId: string
  message: string
}

function BasketLineItem({
  line,
  refusal,
  onQuantity,
  onRemove,
}: {
  line: BasketLine
  refusal: string | undefined
  onQuantity: (bookId: string, typed: string) => void
  onRemove: (bookId: string) => void
}) {
  const id = useId()
  return (
    <li className="card basket-line">
      <h2>{line.title}</h2>
      <p>{`${line.price} ${line.currency} each`}</p>
      <div className="quantity-field">
        <label htmlFor={id}>{`Quantity for ${line.title}`}</label>
        <input
          id={id}
          type="number"
          min={MIN_QUANTITY_PER_LINE}
          max={MAX_QUANTITY_PER_LINE}
          value={line.quantity}
          onChange={(event) => onQuantity(line.bookId, event.target.value)}
        />
      </div>
      <p>
        Line total: <strong>{`${lineTotal(line.price, line.quantity)} ${line.currency}`}</strong>
      </p>
      {refusal !== undefined && (
        <p role="alert" className="alert">
          {refusal}
        </p>
      )}
      <button
        type="button"
        className="button button-secondary"
        aria-label={`Remove ${line.title}`}
        onClick={() => onRemove(line.bookId)}
      >
        Remove
      </button>
    </li>
  )
}

// The basket with its lines. A quantity is changed in the input of its line; the basket decides what is allowed and
// answers a refusal with its message, which is shown under the line while the input and the line keep their quantity.
export function BasketPage() {
  const basket = useBasket()
  const [refusal, setRefusal] = useState<Refusal | undefined>(undefined)

  // The only place where typed text becomes a number. Anything that is not an allowed whole number (an empty text is 0,
  // "2.5" is 2.5) is refused by the basket, which owns that rule and its limits.
  function changeQuantity(bookId: string, typed: string) {
    const message = basket.setQuantity(bookId, Number(typed))
    setRefusal(message === undefined ? undefined : { bookId, message })
  }

  return (
    <section>
      <h1>Basket</h1>
      {basket.lines.length === 0 ? (
        <>
          <p role="status">Your basket is empty.</p>
          <p>
            <Link to="/">Browse the catalogue</Link>
          </p>
        </>
      ) : (
        <>
          <ul className="basket-lines">
            {basket.lines.map((line) => (
              <BasketLineItem
                key={line.bookId}
                line={line}
                refusal={refusal?.bookId === line.bookId ? refusal.message : undefined}
                onQuantity={changeQuantity}
                onRemove={basket.remove}
              />
            ))}
          </ul>
          <EstimatedTotal lines={basket.lines} />
          <p>
            <Link to="/checkout" className="button button-primary">
              Go to checkout
            </Link>
          </p>
        </>
      )}
    </section>
  )
}
