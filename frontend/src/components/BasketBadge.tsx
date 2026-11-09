import { useBasket } from '../state/basket'

// The number of items in the basket (the quantities added up, so the same book twice is 2), shown in the header next
// to the Basket link. It is a live region, so a screen reader hears the number change when a book is added.
export function BasketBadge() {
  const { itemCount } = useBasket()
  return (
    <span className="count-badge" role="status" aria-label="Items in basket">
      {itemCount}
    </span>
  )
}
