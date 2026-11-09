import { getJson } from './client'
import type { BookDetailView, BookView } from './types'

// All criteria are optional. The genre is free text here: the server answers an unknown genre with its own message.
export interface BookSearch {
  q?: string
  title?: string
  author?: string
  genre?: string
}

// The query always lists the criteria in the order q, title, author, genre. A criterion that is missing, empty or only
// whitespace is left out (the server treats it as absent too); any other value is sent as typed.
export function searchBooks(criteria: BookSearch = {}): Promise<BookView[]> {
  const query = new URLSearchParams()
  for (const name of ['q', 'title', 'author', 'genre'] as const) {
    const value = criteria[name]
    if (value !== undefined && value.trim() !== '') {
      query.append(name, value)
    }
  }
  const text = query.toString()
  return getJson<BookView[]>(text === '' ? '/books' : `/books?${text}`)
}

export function getBook(id: string): Promise<BookDetailView> {
  return getJson<BookDetailView>(`/books/${encodeURIComponent(id)}`)
}
