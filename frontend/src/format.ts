// Writes an instant as the bookstore sends it ("2026-06-15T07:30:00.123456Z", always in UTC) as "2026-06-15 07:30:00 UTC".
// The text is cut apart and put together again: no clock, no zone and no locale is involved, so the day and the hour
// are the ones the bookstore sent in every browser. The fraction of a second is dropped. Anything that is not an
// instant in this form is given back as it came, so that a page never shows a made up or shifted time.

const INSTANT = /^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2}:\d{2})(?:\.\d+)?Z$/

export function formatInstant(instant: string): string {
  const match = INSTANT.exec(instant)
  return match === null ? instant : `${match[1]} ${match[2]} UTC`
}
