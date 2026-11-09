import type { Genre } from './api/types'

// The genre names of the search form, in declaration order. This mirrors the bookstore Genre enum and is only here so
// the form can offer a list. The bookstore stays the authority: it answers an unknown genre with its own list, and
// the alert shows that list, not this one.
export const GENRES = [
  'FICTION',
  'SCIENCE_FICTION',
  'FANTASY',
  'MYSTERY',
  'ROMANCE',
  'HISTORY',
  'SCIENCE',
  'TECHNOLOGY',
  'CHILDREN',
  'POETRY',
] as const satisfies readonly Genre[]
