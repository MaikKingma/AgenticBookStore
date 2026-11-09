// Wire types of the bookstore REST API. Money amounts and instants are plain strings exactly as the server sends them
// (for example "12.50" and "2026-06-15T10:00:00.123456Z"); the storefront never converts them to numbers or dates.

export type Genre =
  | 'FICTION'
  | 'SCIENCE_FICTION'
  | 'FANTASY'
  | 'MYSTERY'
  | 'ROMANCE'
  | 'HISTORY'
  | 'SCIENCE'
  | 'TECHNOLOGY'
  | 'CHILDREN'
  | 'POETRY'

export type BookStatus = 'AVAILABLE' | 'DISCONTINUED'

export type OrderStatus = 'PLACED' | 'SHIPPED'

// Element of GET /books. The authors arrive as one comma-joined string.
export interface BookView {
  id: string
  isbn: string
  title: string
  authors: string
  genre: Genre
  price: string
  currency: string
  status: BookStatus
  available: number
  inStock: boolean
}

// GET /books/{id}. The authors arrive as a list and the publication year is added.
export interface BookDetailView {
  id: string
  isbn: string
  title: string
  authors: string[]
  genre: Genre
  price: string
  currency: string
  publicationYear: number
  status: BookStatus
  available: number
  inStock: boolean
}

// GET /inventory/{bookId}
export interface StockView {
  bookId: string
  onHand: number
  reserved: number
  available: number
  reorderLevel: number
  lowStock: boolean
}

export interface OrderLinePayload {
  bookId: string
  quantity: number
}

// Request body of POST /orders/commands/place
export interface PlaceOrderPayload {
  customerEmail: string
  lines: OrderLinePayload[]
}

// 202 body of POST /orders/commands/place
export interface CommandResult {
  id: string
}

export interface OrderLineView {
  isbn: string
  title: string
  quantity: number
  unitPrice: string
  lineTotal: string
}

// GET /orders/{id}
export interface OrderView {
  id: string
  customerEmail: string
  status: OrderStatus
  placedAt: string
  shippedAt: string | null
  lines: OrderLineView[]
  total: string
  currency: string
}

// Element of GET /orders?customerEmail=
export interface OrderSummaryView {
  id: string
  status: OrderStatus
  placedAt: string
  itemCount: number
  total: string
}

// Request body of POST /assistant/chat
export interface ChatPayload {
  conversationId: string
  message: string
}

// 200 body of POST /assistant/chat
export interface AssistantReplyView {
  conversationId: string
  reply: string
}

// Any failed request, normalised. The details are flat string maps whose keys depend on the code, for example
// { field, message }, { genre } or { bookId, requested, available }.
export interface Problem {
  status: number
  title: string
  detail?: string
  code?: string
  details: Array<Record<string, string>>
}
