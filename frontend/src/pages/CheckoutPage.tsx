import { useId, useState } from 'react'
import type { FormEvent } from 'react'
import { BasketSummary } from '../components/BasketSummary'
import { ProblemDetailAlert } from '../components/ProblemDetailAlert'
import { Link } from '../router'
import { useBasket } from '../state/basket'

// Places the basket as one order. The form does not check the e-mail address: the text is sent as typed and the
// bookstore answers with its own error when it does not accept it. The basket keeps the submitted lines until the
// bookstore has said 202; every other answer leaves it as it is and shows the alert, so the visitor can try again.
// The order that is being placed belongs to the basket, which outlives this page: while it runs, every checkout page
// shows the same wait, and the way back to the basket is replaced by a text that says why.
export function CheckoutPage() {
  const basket = useBasket()
  const [email, setEmail] = useState('')
  const emailId = useId()

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    basket.placeOrder(email)
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
      {basket.placing ? (
        <p role="status">Your order is being placed. You can change the basket again when the bookstore has answered.</p>
      ) : (
        <p>
          <Link to="/basket">Back to the basket</Link>
        </p>
      )}
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
          <button type="submit" className="button button-primary" disabled={basket.placing}>
            Confirm order
          </button>
        </div>
      </form>
      {basket.orderProblem !== undefined && (
        <ProblemDetailAlert problem={basket.orderProblem} titleOf={(bookId) => basket.lines.find((line) => line.bookId === bookId)?.title} />
      )}
    </section>
  )
}
