import { createContext, useContext, useEffect, useMemo, useReducer, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { placeOrder } from '../api/orders'
import type { Problem } from '../api/types'
import { MAX_BASKET_LINES, MAX_QUANTITY_PER_LINE, MIN_QUANTITY_PER_LINE } from '../limits'
import { problemOf } from '../pages/problem'
import { navigate } from '../router'
import { isMoney } from './estimate'

// The basket of the visitor: one line per book, in the order the books were first added. It lives in the browser and is
// kept in sessionStorage, so it survives a reload of the page and goes away with the tab. The prices in the lines are
// what the bookstore showed when the book was added; the bookstore prices the order itself.
//
// The order that is being placed lives here too, not in the checkout page: the request outlives the page that sent it,
// so every checkout page, also one opened again meanwhile, sees the same state and cannot send a second order.

export interface BasketLine {
  bookId: string
  title: string
  price: string
  currency: string
  quantity: number
}

// What the basket needs to know about a book to add it.
export interface BasketBook {
  id: string
  title: string
  price: string
  currency: string
}

// The actions return the reason when the basket refuses them and undefined when they were done.
export interface Basket {
  lines: readonly BasketLine[]
  // The number of items: the quantities added up (the same book twice counts 2).
  itemCount: number
  add: (book: BasketBook) => string | undefined
  setQuantity: (bookId: string, quantity: number) => string | undefined
  remove: (bookId: string) => void
  clear: () => void
  // True from the moment an order is sent until the bookstore has answered.
  placing: boolean
  // The error answer to the last order, until the next order is sent.
  orderProblem: Problem | undefined
  // Sends the lines of the basket as one order, unless an order is being placed already. The text is sent as typed.
  // On 202 the lines of the submitted books are removed, whatever their quantity is by then and wherever the visitor is,
  // and the order page of the answer is shown. Any other answer leaves the basket as it is.
  placeOrder: (customerEmail: string) => void
}

// The lines are stored as a JSON list. An empty basket has no entry.
const STORAGE_KEY = 'openbook.basket'

type Action =
  | { type: 'add'; book: BasketBook }
  | { type: 'setQuantity'; bookId: string; quantity: number }
  | { type: 'remove'; bookId: string }
  | { type: 'removeBooks'; bookIds: readonly string[] }
  | { type: 'clear' }

type Decision = { lines: readonly BasketLine[] } | { refused: string }

const QUANTITY_MESSAGE = `Quantity must be between ${MIN_QUANTITY_PER_LINE} and ${MAX_QUANTITY_PER_LINE}.`
const LINE_FULL_MESSAGE = `A basket line holds at most ${MAX_QUANTITY_PER_LINE} copies of a book.`
const BASKET_FULL_MESSAGE = `The basket holds at most ${MAX_BASKET_LINES} different books.`

// The one place that decides what an action does. A refusal leaves the lines as they are.
function decide(lines: readonly BasketLine[], action: Action): Decision {
  switch (action.type) {
    case 'add': {
      const existing = lines.find((line) => line.bookId === action.book.id)
      if (existing !== undefined) {
        if (existing.quantity >= MAX_QUANTITY_PER_LINE) return { refused: LINE_FULL_MESSAGE }
        return { lines: lines.map((line) => (line === existing ? { ...line, quantity: line.quantity + 1 } : line)) }
      }
      if (lines.length >= MAX_BASKET_LINES) return { refused: BASKET_FULL_MESSAGE }
      const { id, title, price, currency } = action.book
      return { lines: [...lines, { bookId: id, title, price, currency, quantity: 1 }] }
    }
    case 'setQuantity': {
      const { bookId, quantity } = action
      if (!Number.isInteger(quantity) || quantity < MIN_QUANTITY_PER_LINE || quantity > MAX_QUANTITY_PER_LINE) {
        return { refused: QUANTITY_MESSAGE }
      }
      if (!lines.some((line) => line.bookId === bookId)) return { lines }
      return { lines: lines.map((line) => (line.bookId === bookId ? { ...line, quantity } : line)) }
    }
    case 'remove': {
      const remaining = lines.filter((line) => line.bookId !== action.bookId)
      return { lines: remaining.length === lines.length ? lines : remaining }
    }
    case 'removeBooks': {
      const remaining = lines.filter((line) => !action.bookIds.includes(line.bookId))
      return { lines: remaining.length === lines.length ? lines : remaining }
    }
    case 'clear':
      return { lines: [] }
  }
}

function reduce(lines: readonly BasketLine[], action: Action): readonly BasketLine[] {
  const decision = decide(lines, action)
  return 'refused' in decision ? lines : decision.lines
}

// One stored entry as a line, or undefined when it is not one. Only the five known fields are kept.
function toLine(entry: unknown): BasketLine | undefined {
  if (typeof entry !== 'object' || entry === null) return undefined
  const { bookId, title, price, currency, quantity } = entry as Record<string, unknown>
  if (
    typeof bookId === 'string' &&
    typeof title === 'string' &&
    typeof price === 'string' &&
    isMoney(price) &&
    typeof currency === 'string' &&
    typeof quantity === 'number' &&
    Number.isInteger(quantity) &&
    quantity >= MIN_QUANTITY_PER_LINE &&
    quantity <= MAX_QUANTITY_PER_LINE
  ) {
    return { bookId, title, price, currency, quantity }
  }
  return undefined
}

// The stored JSON, or undefined when nothing is stored, the text is not JSON or the browser refuses to read.
function readStoredJson(): unknown {
  try {
    const text = sessionStorage.getItem(STORAGE_KEY)
    return text === null ? undefined : JSON.parse(text)
  } catch {
    return undefined
  }
}

// The stored basket. Anything that cannot be trusted (not a list, a bad line, a book twice, too many lines, and what
// readStoredJson turns into undefined) gives an empty basket, never a part of one.
function readStoredLines(): readonly BasketLine[] {
  const parsed = readStoredJson()
  if (!Array.isArray(parsed) || parsed.length > MAX_BASKET_LINES) return []
  const lines: BasketLine[] = []
  for (const entry of parsed) {
    const line = toLine(entry)
    if (line === undefined || lines.some((other) => other.bookId === line.bookId)) return []
    lines.push(line)
  }
  return lines
}

function writeStoredLines(lines: readonly BasketLine[]): void {
  try {
    if (lines.length === 0) sessionStorage.removeItem(STORAGE_KEY)
    else sessionStorage.setItem(STORAGE_KEY, JSON.stringify(lines))
  } catch {
    // The browser refuses to store: the basket keeps working for as long as the page is open.
  }
}

const BasketContext = createContext<Basket | undefined>(undefined)

export function BasketProvider({ children }: { children: ReactNode }) {
  // The reducer starts from the stored basket (the init function), so the first render already shows it.
  const [lines, dispatch] = useReducer(reduce, undefined, readStoredLines)
  const [placing, setPlacing] = useState(false)
  const [orderProblem, setOrderProblem] = useState<Problem | undefined>(undefined)
  // Set at once when an order is sent, so two submits in the same tick cannot both get through before a render.
  const sending = useRef(false)

  useEffect(() => {
    writeStoredLines(lines)
  }, [lines])

  const basket = useMemo<Basket>(() => {
    function run(action: Action): string | undefined {
      const decision = decide(lines, action)
      if ('refused' in decision) return decision.refused
      dispatch(action)
      return undefined
    }
    function place(customerEmail: string): void {
      if (sending.current) return
      sending.current = true
      setPlacing(true)
      setOrderProblem(undefined)
      const submitted = lines.map(({ bookId, quantity }) => ({ bookId, quantity }))
      placeOrder({ customerEmail, lines: submitted }).then(
        (placed) => {
          sending.current = false
          setPlacing(false)
          dispatch({ type: 'removeBooks', bookIds: submitted.map((line) => line.bookId) })
          navigate('/orders/' + encodeURIComponent(placed.id))
        },
        (error: unknown) => {
          sending.current = false
          setPlacing(false)
          setOrderProblem(problemOf(error))
        },
      )
    }
    return {
      lines,
      itemCount: lines.reduce((sum, line) => sum + line.quantity, 0),
      add: (book) => run({ type: 'add', book }),
      setQuantity: (bookId, quantity) => run({ type: 'setQuantity', bookId, quantity }),
      remove: (bookId) => {
        dispatch({ type: 'remove', bookId })
      },
      clear: () => {
        dispatch({ type: 'clear' })
      },
      placing,
      orderProblem,
      placeOrder: place,
    }
  }, [lines, placing, orderProblem])

  return <BasketContext value={basket}>{children}</BasketContext>
}

export function useBasket(): Basket {
  const basket = useContext(BasketContext)
  if (basket === undefined) {
    throw new Error('useBasket must be used inside a BasketProvider')
  }
  return basket
}
