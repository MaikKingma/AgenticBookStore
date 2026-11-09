import { useId, useState } from 'react'
import type { FormEvent } from 'react'
import { placeOrder } from '../api/orders'
import type { Problem } from '../api/types'
import { BasketSummary } from '../components/BasketSummary'
import { ProblemDetailAlert } from '../components/ProblemDetailAlert'
import { Link, navigate } from '../router'
import { useBasket } from '../state/basket'
import { problemOf } from './problem'

// Places the basket as one order. The form does not check the e-mail address: the text is sent as typed and the
// bookstore answers with its own error when it does not accept it. The basket stays until the bookstore has said 202;
// every other answer leaves it as it is and shows the alert, so the visitor can try again.
export function CheckoutPage() {
  const basket = useBasket()
  const [email, setEmail] = useState('')
  const [pending, setPending] = useState(false)
  const [problem, setProblem] = useState<Problem | undefined>(undefined)
  const emailId = useId()

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (pending) return
    setPending(true)
    setProblem(undefined)
    placeOrder({
      customerEmail: email,
      lines: basket.lines.map(({ bookId, quantity }) => ({ bookId, quantity })),
    }).then(
      (placed) => {
        basket.clear()
        navigate('/orders/' + encodeURIComponent(placed.id))
      },
      (error: unknown) => {
        setProblem(problemOf(error))
        setPending(false)
      },
    )
  }

  if (basket.lines.length === 0) {
    return (
      <section>
        <h1>Checkout</h1>
        <p role="status">Your basket is empty.</p>
        <p>
          <Link to="/">Browse the catalogue</Link>
        </p>
      </section>
    )
  }

  return (
    <section>
      <h1>Checkout</h1>
      <p>
        <Link to="/basket">Back to the basket</Link>
      </p>
      <BasketSummary lines={basket.lines} />
      <form className="card checkout-form" aria-label="Order details" noValidate onSubmit={submit}>
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
          <button type="submit" className="button button-primary" disabled={pending}>
            Confirm order
          </button>
        </div>
      </form>
      {problem !== undefined && (
        <ProblemDetailAlert problem={problem} titleOf={(bookId) => basket.lines.find((line) => line.bookId === bookId)?.title} />
      )}
    </section>
  )
}
