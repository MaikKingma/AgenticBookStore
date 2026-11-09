import { describe, expect, it } from 'vitest'
import { GENRES, GENRE_LABELS, genreLabel } from './genres'

describe('genre labels', () => {
  it('has exactly one label for each genre of the form and no other', () => {
    expect(Object.keys(GENRE_LABELS).sort()).toEqual([...GENRES].sort())
  })

  it('labels the genres of the form in declaration order with sentence case text', () => {
    expect(GENRES.map((genre) => genreLabel(genre))).toEqual([
      'Fiction',
      'Science fiction',
      'Fantasy',
      'Mystery',
      'Romance',
      'History',
      'Science',
      'Technology',
      'Children',
      'Poetry',
    ])
  })

  it('reads the label from the map', () => {
    for (const genre of GENRES) {
      expect(genreLabel(genre)).toBe(GENRE_LABELS[genre])
    }
  })

  it.each([['GRAPHIC_NOVEL'], ['cookbooks'], ['fiction'], [''], ['toString']])(
    'returns a genre it does not know as given: %j',
    (unknown) => {
      expect(genreLabel(unknown)).toBe(unknown)
    },
  )
})
