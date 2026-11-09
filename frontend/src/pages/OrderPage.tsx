import { useEffect, useState } from 'react'
import { getOrder } from '../api/orders'
import type { OrderView, Problem } from '../api/types'
import { ProblemDetailAlert } from '../components/ProblemDetailAlert'
import { formatInstant } from '../format'
import { Link } from '../router'
import { problemOf } from './problem'

// The answer for one order id. Keeping the id in the result lets the page ignore an answer that belongs to another
// order than the one in the address now.
type Result =
  | { status: 'loaded'; id: string; order: OrderView }
  | { status: 'failed'; id: string; problem: Problem }

// The order exactly as the bookstore sent it. Nothing is added up or worked out here: the line totals and the total are
// the bookstore's, and the currency is the one of the order.
function Order({ order }: { order: OrderView }) {
  const money = (amount: string) => `${amount} ${order.currency}`
  return (
    <div className="card">
      <dl className="book-facts">
        <dt>Order number</dt>
        <dd>{order.id}</dd>
        <dt>Status</dt>
        <dd>{order.status}</dd>
        <dt>Placed</dt>
        <dd>{formatInstant(order.placedAt)}</dd>
        <dt>Shipped</dt>
        <dd>{order.shippedAt === null ? 'Not shipped yet' : formatInstant(order.shippedAt)}</dd>
        <dt>E-mail address</dt>
        <dd>{order.customerEmail}</dd>
        <dt>Total</dt>
        <dd>{money(order.total)}</dd>
      </dl>
      <h2>Items</h2>
      <div className="table-scroll">
        <table className="order-lines">
          <thead>
            <tr>
              <th scope="col">Title</th>
              <th scope="col">ISBN</th>
              <th scope="col">Quantity</th>
              <th scope="col">Unit price</th>
              <th scope="col">Line total</th>
            </tr>
          </thead>
          <tbody>
            {order.lines.map((line, index) => (
              <tr key={index}>
                <td>{line.title}</td>
                <td>{line.isbn}</td>
                <td>{line.quantity}</td>
                <td>{money(line.unitPrice)}</td>
                <td>{money(line.lineTotal)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

// One order with its status, its lines and its total.
export function OrderPage({ id }: { id: string }) {
  const [result, setResult] = useState<Result | undefined>(undefined)

  // Runs again when the order in the address changes. An answer that arrives after that is dropped.
  useEffect(() => {
    let latest = true
    getOrder(id).then(
      (order) => {
        if (latest) setResult({ status: 'loaded', id, order })
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
      <h1>Order</h1>
      <p>
        <Link to="/orders">Back to your orders</Link>
      </p>
      {view === undefined && <p role="status">Loading order</p>}
      {view?.status === 'failed' && <ProblemDetailAlert problem={view.problem} />}
      {view?.status === 'loaded' && <Order order={view.order} />}
    </section>
  )
}
