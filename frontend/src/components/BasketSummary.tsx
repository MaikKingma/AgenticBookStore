import type { BasketLine } from '../state/basket'
import { basketTotal, lineTotal } from '../state/estimate'

// The total of the basket as an estimate. The currency is the one of the first line: the bookstore prices every book in
// one currency and basketTotal adds the amounts without looking at the currency. The bookstore works out the real total
// when it takes the order, and the sentence under the figure says so.
export function EstimatedTotal({ lines }: { lines: readonly BasketLine[] }) {
  return (
    <div className="basket-total">
      <p>
        Estimated total: <strong>{`${basketTotal(lines)} ${lines[0].currency}`}</strong>
      </p>
      <p>The bookstore confirms the total when the order is placed.</p>
    </div>
  )
}

// The lines of the basket to read, in basket order, with their totals, followed by the estimated total.
export function BasketSummary({ lines }: { lines: readonly BasketLine[] }) {
  return (
    <div>
      <ul className="basket-summary">
        {lines.map((line) => (
          <li key={line.bookId}>
            {`${line.title}, ${line.quantity} x ${line.price} ${line.currency}: `}
            <strong>{`${lineTotal(line.price, line.quantity)} ${line.currency}`}</strong>
          </li>
        ))}
      </ul>
      <EstimatedTotal lines={lines} />
    </div>
  )
}
