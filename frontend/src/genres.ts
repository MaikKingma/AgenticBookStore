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

// The label the shop shows for every genre: sentence case, never the enum name. The map is keyed by the Genre type, so
// a genre the type gains and this map lacks is a compile error. The search form still sends the enum name.
export const GENRE_LABELS = Object.freeze({
  FICTION: 'Fiction',
  SCIENCE_FICTION: 'Science fiction',
  FANTASY: 'Fantasy',
  MYSTERY: 'Mystery',
  ROMANCE: 'Romance',
  HISTORY: 'History',
  SCIENCE: 'Science',
  TECHNOLOGY: 'Technology',
  CHILDREN: 'Children',
  POETRY: 'Poetry',
} satisfies Record<Genre, string>)

// The label of a genre as the bookstore sent it. A genre this shop does not know (the bookstore gained one before the
// shop was rebuilt) is shown as sent, so the page is never blank and never fails.
export function genreLabel(genre: string): string {
  return Object.hasOwn(GENRE_LABELS, genre) ? GENRE_LABELS[genre as Genre] : genre
}
