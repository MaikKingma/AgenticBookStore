import { useEffect, useId, useState } from 'react'
import type { FormEvent } from 'react'
import { findOrders } from '../api/orders'
import type { OrderSummaryView, Problem } from '../api/types'
import { ProblemDetailAlert } from '../components/ProblemDetailAlert'
import { formatInstant } from '../format'
import { Link } from '../router'
import { problemOf } from './problem'

// The order list has no currency of its own. The bookstore takes orders in euro only, so the totals are shown in euro.
const CURRENCY = 'EUR'

type View =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'loaded'; orders: OrderSummaryView[] }
  | { status: 'failed'; problem: Problem }

// The orders of one e-mail address, in the order the bookstore sends them (newest first, decided by the bookstore).
export function OrdersPage() {
  const [email, setEmail] = useState('')
  // Every submit makes a new object, so that asking again for the same address runs the request again.
  const [search, setSearch] = useState<{ email: string } | undefined>(undefined)
  const [view, setView] = useState<View>({ status: 'idle' })
  const emailId = useId()

  // Runs for every submitted search. An answer that arrives after a newer search was started is dropped.
  useEffect(() => {
    if (search === undefined) return
    let latest = true
    findOrders(search.email).then(
      (orders) => {
        if (latest) setView({ status: 'loaded', orders })
      },
      (error: unknown) => {
        if (latest) setView({ status: 'failed', problem: problemOf(error) })
      },
    )
    return () => {
      latest = false
    }
  }, [search])

  // The address is sent as typed, an empty field included: the bookstore decides what it accepts.
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setView({ status: 'loading' })
    setSearch({ email })
  }

  return (
    <section>
      <h1>Orders</h1>
      <form className="card orders-form" aria-label="Find your orders" noValidate onSubmit={submit}>
        <label htmlFor={emailId}>E-mail address</label>
        <input
          id={emailId}
          type="text"
          inputMode="email"
          autoComplete="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />
        <div className="form-actions">
          <button type="submit" className="button button-primary">
            Show orders
          </button>
        </div>
      </form>
      {view.status === 'loading' && <p role="status">Loading orders</p>}
      {view.status === 'failed' && <ProblemDetailAlert problem={view.problem} />}
      {view.status === 'loaded' &&
        (view.orders.length === 0 ? (
          <p role="status">No orders found for this e-mail address.</p>
        ) : (
          <ul className="order-list">
            {view.orders.map((order) => (
              <li key={order.id} className="card">
                <Link to={'/orders/' + encodeURIComponent(order.id)}>{`Order ${order.id}`}</Link>{' '}
                <span>{`${order.status} | ${formatInstant(order.placedAt)} | Items: ${order.itemCount} | ${order.total} ${CURRENCY}`}</span>
              </li>
            ))}
          </ul>
        ))}
    </section>
  )
}
