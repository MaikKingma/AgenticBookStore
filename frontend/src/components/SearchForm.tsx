import { useId, useState } from 'react'
import type { BookSearch } from '../api/books'
import { GENRES, genreLabel } from '../genres'

// The four criteria of the catalogue search. An empty text means "not set"; the API function leaves it out.
export type SearchCriteria = Required<BookSearch>

// The search bar of the catalogue. The text is kept as typed and handed over as it is, spaces included.
export function SearchForm({ initial, onSearch }: { initial: SearchCriteria; onSearch: (criteria: SearchCriteria) => void }) {
  const [draft, setDraft] = useState(initial)
  const id = useId()

  return (
    <form
      className="card search-form"
      role="search"
      onSubmit={(event) => {
        event.preventDefault()
        onSearch(draft)
      }}
    >
      <div className="search-main">
        <label htmlFor={`${id}-q`}>Search</label>
        <input
          id={`${id}-q`}
          type="search"
          value={draft.q}
          onChange={(event) => setDraft({ ...draft, q: event.target.value })}
        />
      </div>
      <div>
        <label htmlFor={`${id}-title`}>Title</label>
        <input
          id={`${id}-title`}
          type="text"
          value={draft.title}
          onChange={(event) => setDraft({ ...draft, title: event.target.value })}
        />
      </div>
      <div>
        <label htmlFor={`${id}-author`}>Author</label>
        <input
          id={`${id}-author`}
          type="text"
          value={draft.author}
          onChange={(event) => setDraft({ ...draft, author: event.target.value })}
        />
      </div>
      <div>
        <label htmlFor={`${id}-genre`}>Genre</label>
        <select
          id={`${id}-genre`}
          value={draft.genre}
          onChange={(event) => setDraft({ ...draft, genre: event.target.value })}
        >
          <option value="">Any genre</option>
          {GENRES.map((genre) => (
            <option key={genre} value={genre}>
              {genreLabel(genre)}
            </option>
          ))}
        </select>
      </div>
      <div className="search-actions">
        <button type="submit" className="button button-primary">
          Search
        </button>
      </div>
    </form>
  )
}
