---
name: qa-frontend-falsifiers
description: Witnesses that falsify React, Vitest and RTL criteria: mid-value refusals, storage reads, integer cents, query encoding, absence assertions, status-keyed rendering, decorrelated badges, list-order and key witnesses, money display sites, reducer double-dispatch, cap mirrors, text-source fixtures, lookup fallbacks
metadata:
  type: feedback
---

Write these into the qaDirective falsifier list at strategy time. C0-16 etc. refer to the common directive block. Siblings: [[qa-frontend-css-live-checks]] (contrast, layout, live browser), [[qa-test-data-falsifiers]].

- Refusals are tested from a MID value (a line at 3, set 0 or 11) or a clamp survives.
- Persistence is read from storage right after the change, without unmount (a reload runs no cleanup).
- Integer-cents conversion needs "4.35" or "19.99" (`Math.floor(p * 100)` is one cent short).
- Every absence assertion runs after an awaited presence assertion, or it is vacuous.
- Status-keyed rendering needs a second code on the same status (409 `book-not-orderable` with empty details beside `insufficient-stock`).
- A badge "from field X" needs decorrelation both ways (`available 3, inStock false` and `available 0, inStock true`).
- A string list (authors) is pinned by C0-16 only on the string itself; real credit orders are often sorted by surname and length, so surname or length re-sorts survive: name the witness `['Ian Goodfellow', 'Yoshua Bengio', 'Aaron Courville']` (full peak, surname valley, lengths 14, 13, 15) and tabulate every array inside any pass-through assertion.
- A displayed enum needs a value with an underscore (`SCIENCE_FICTION`) or humanising survives; an unknown route needs a valid id after an unknown first segment (`#/basket/x`); a decode needs a double-escape (`%2541`); a stale-answer guard needs a late success AND a late failure.
- A trailing-zero money witness ("12.50", "10.00") goes on EVERY site a price string passes through (display and the hand-over to state), not only into the arithmetic module, or `Number(price)` survives there.
- Callbacks that decide on the rendered state and then dispatch to a reducer that re-decides need two actions inside one `act` (a line at 9 plus two adds; 19 lines plus two new books; two adds both counted), or the reducer's refusal branch is dead to the tests. A corrupt-storage list needs `null` alone and between valid lines.
- A browser mirror of a server length cap on an untrimmed field needs boundary rows with edge whitespace on each side (`'a'.repeat(1000) + ' '`, `' ' + 'a'.repeat(1000)`, and 999 plus a space enabled) and one astral character (`String.fromCodePoint(0x1f4d6)`, length 2) at cap and cap + 1, or trimmed-length, one-sided trim and code-point counters survive.
- "Show our own text, not the server's" is untestable when the server sends that same text (the bookstore's `assistant-unavailable` detail IS the page text): the fixture detail and title must differ from the page text, and `details` must be non-empty, or rendering `problem.detail` or keeping `details: problem.details` survives.
- A lookup helper with a fallback ("unknown value shown as sent") pinned only by its unit test lets each consumer that reads the map directly survive when its input is typed as the union (`GENRE_LABELS[book.genre]` type-checks). Name a consumer witness per render site (a value cast `as BookView['genre']`) at strategy time; found late, a gate grep (map indexed only in its module) plus a residual, not a REJECT.
