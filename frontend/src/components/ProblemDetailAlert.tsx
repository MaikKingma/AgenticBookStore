import type { Problem } from '../api/types'

// One entry of the details list as text. The server fills the entries with flat string maps whose keys depend on the
// error: { field, message } for a validation failure, { genre } for an unknown genre, { bookId, requested, available }
// for a shortage. Any other shape is shown as "key: value" pairs in the order they came in, so no detail is hidden.
function describeDetail(detail: Record<string, string>): string {
  const keys = Object.keys(detail).sort().join(',')
  if (keys === 'field,message') return `${detail.field}: ${detail.message}`
  if (keys === 'genre') return detail.genre
  if (keys === 'available,bookId,requested') {
    return `Book ${detail.bookId}: requested ${detail.requested}, available ${detail.available}`
  }
  return Object.entries(detail)
    .map(([key, value]) => `${key}: ${value}`)
    .join(', ')
}

// Shows any failed request: the title, the detail text when there is one, and the list of details when there are any.
// The list keys are the positions, because a field can appear twice with different messages.
export function ProblemDetailAlert({ problem }: { problem: Problem }) {
  return (
    <div role="alert" className="alert">
      <p>
        <strong>{problem.title}</strong>
      </p>
      {problem.detail !== undefined && <p>{problem.detail}</p>}
      {problem.details.length > 0 && (
        <ul>
          {problem.details.map((detail, index) => (
            <li key={index}>{describeDetail(detail)}</li>
          ))}
        </ul>
      )}
    </div>
  )
}
