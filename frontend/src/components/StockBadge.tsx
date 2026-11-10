// The badge shows what the bookstore says in `inStock`. The number of copies is never looked at: the server decides
// when a book counts as in stock.
export function StockBadge({ inStock }: { inStock: boolean }) {
  return inStock ? (
    <span className="badge badge-in-stock">In stock</span>
  ) : (
    <span className="badge badge-out-of-stock">Out of stock</span>
  )
}
