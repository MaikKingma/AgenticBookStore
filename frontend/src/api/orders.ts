import { getJson, postJson } from './client'
import type { CommandResult, OrderSummaryView, OrderView, PlaceOrderPayload } from './types'

export function getOrder(id: string): Promise<OrderView> {
  return getJson<OrderView>(`/orders/${encodeURIComponent(id)}`)
}

// The address is sent as typed, encoded as a query parameter. The server normalises it and checks it.
export function findOrders(customerEmail: string): Promise<OrderSummaryView[]> {
  const query = new URLSearchParams({ customerEmail })
  return getJson<OrderSummaryView[]>(`/orders?${query.toString()}`)
}

// The server answers 202 with the id of the new order.
export function placeOrder(payload: PlaceOrderPayload): Promise<CommandResult> {
  return postJson<CommandResult>('/orders/commands/place', payload)
}
