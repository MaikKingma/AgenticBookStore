import { ApiError } from '../api/client'
import type { Problem } from '../api/types'

// A request that failed for a reason the API client did not turn into a problem (a bug, not a bookstore answer).
export function problemOf(error: unknown): Problem {
  return error instanceof ApiError ? error.problem : { status: 0, title: 'Something went wrong', details: [] }
}
