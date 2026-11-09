import { getJson } from './client'
import type { StockView } from './types'

export function getStock(bookId: string): Promise<StockView> {
  return getJson<StockView>(`/inventory/${encodeURIComponent(bookId)}`)
}
