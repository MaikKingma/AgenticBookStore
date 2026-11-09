import { describe, expect, it } from 'vitest'
import { formatInstant } from './format'

describe('formatInstant', () => {
  it.each([
    ['2026-06-15T10:00:00.123456Z', '2026-06-15 10:00:00 UTC'],
    ['2026-06-15T10:00:05.1Z', '2026-06-15 10:00:05 UTC'],
    ['2026-06-18T16:45:30.123456789Z', '2026-06-18 16:45:30 UTC'],
  ])('writes the instant %s without its fraction of a second as %s', (instant, expected) => {
    expect(formatInstant(instant)).toBe(expected)
  })

  it.each([
    ['2025-12-24T00:00:00Z', '2025-12-24 00:00:00 UTC'],
    ['2026-06-15T10:00:05Z', '2026-06-15 10:00:05 UTC'],
  ])('writes the instant %s, which has no fraction, as %s', (instant, expected) => {
    expect(formatInstant(instant)).toBe(expected)
  })

  it('keeps the day and the hour of an instant near midnight and at the end of a year, whatever zone the browser is in', () => {
    // A conversion to the local zone would move this instant into the next day or the next year.
    expect(formatInstant('2025-12-31T23:30:00Z')).toBe('2025-12-31 23:30:00 UTC')
    expect(formatInstant('2026-01-01T00:00:00Z')).toBe('2026-01-01 00:00:00 UTC')
  })

  it.each([
    ['text that is no instant', 'yesterday at noon'],
    ['an empty text', ''],
    ['an instant with an offset instead of Z', '2026-06-15T10:00:00+02:00'],
    ['an instant without a zone', '2026-06-15T10:00:00'],
    ['a date without a time', '2026-06-15'],
    ['an instant with spaces around it', ' 2026-06-15T10:00:00Z '],
    ['an instant with text before it', 'at 2026-06-15T10:00:00Z'],
    ['an instant with text after it', '2026-06-15T10:00:00Z or later'],
  ])('gives %s back as it was sent, so that nothing is invented or shifted', (_name, text) => {
    expect(formatInstant(text)).toBe(text)
  })
})
