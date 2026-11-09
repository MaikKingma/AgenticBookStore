// Limits the storefront mirrors from the bookstore, so that the basket refuses early what the bookstore would refuse
// when the order is placed. They are copies, not rules of this application: the bookstore stays the authority and
// answers with its own error whatever the browser lets through.

// At most this many copies of one book in one order line. Owned by spec 04 (orders), enforced on the server by the
// `OrderFlow` class.
export const MAX_QUANTITY_PER_LINE = 10

// At least this many copies in an order line. Owned by spec 04 (orders), enforced on the server by the `OrderFlow`
// class.
export const MIN_QUANTITY_PER_LINE = 1

// At most this many different books (order lines) in one order. Owned by spec 04 (orders), enforced on the server by
// the `PlaceOrderPayload` class.
export const MAX_BASKET_LINES = 20
