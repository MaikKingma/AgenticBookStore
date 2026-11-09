---
name: qa-frontend-falsifiers
description: Witnesses that falsify frontend criteria (React, Vitest, RTL, CSS, contrast and style-guard tests, live-browser layout checks); read before writing the falsifier list of a UI task
metadata:
  type: feedback
---

Split out of [[qa-test-data-falsifiers]] (first written for spec 12). Write these witnesses into the qaDirective falsifier list at strategy time. Item numbers such as C0-16 refer to the common directive of [[qa-directive-traps]].

## React / Vitest / RTL witnesses

- Refusals are tested from a MID value (a line at 3, set 0 or 11) or a clamp survives.
- Persistence is read from storage right after the change, without unmount (a reload runs no cleanup).
- Integer-cents conversion needs "4.35" or "19.99" (`Math.floor(p * 100)` is one cent short).
- An e-mail query parameter needs a `+` (unencoded it decodes as a space).
- Query-order claims pass the criteria in a key order that differs from the output and from alphabetical.
- Every absence assertion runs after an awaited presence assertion, or it is vacuous.
- Status-keyed rendering needs a second code on the same status (409 `book-not-orderable` with empty details beside `insufficient-stock`).
- A badge "from field X" needs decorrelation both ways (`available 3, inStock false` and `available 0, inStock true`).
- A string list (authors) is pinned by C0-16 only on the string itself; real credit orders are often sorted by surname and length (Gamma, Helm, Johnson, Vlissides), so surname or length re-sorts survive. Not a breach in a pure pass-through; name the witness for the rendering task: `['Ian Goodfellow', 'Yoshua Bengio', 'Aaron Courville']` (full peak, surname valley, lengths 14, 13, 15). Tabulation must cover every array inside any pass-through assertion, not only fixtures whose test title claims an order (spec 12 T5 lost two rework rounds to that).
- More witnesses to name at strategy time (spec 12 T6 lost a round): a displayed enum needs a value with an underscore (`SCIENCE_FICTION`) or humanising survives; an unknown route needs a valid id after an unknown first segment (`#/basket/x`); a decode needs a double-escape (`%2541`); a stale-answer guard needs a late success AND a late failure.
- A single-flight ref guard needs a next send after EACH answer kind, or a reset dropped from one branch survives (fix-12 T2 lost a round).
- Heterogeneous objects (unknown-shape details) are swept by flat value, reverse and entry order; a per-key sort over objects lacking the key is no order mutant.
- A trailing-zero money witness ("12.50", "10.00") goes on EVERY site a price string passes through (display and the hand-over to state), not only into the arithmetic module, or `Number(price)` survives there (spec 12 T7 lost a round).
- Callbacks that decide on the rendered state and then dispatch to a reducer that re-decides need two actions inside one `act` (a line at 9 plus two adds; 19 lines plus two new books; two adds both counted), or the reducer's refusal branch is dead to the tests. A corrupt-storage list needs `null` alone and between valid lines.
- A browser mirror of a server length cap on an untrimmed field needs boundary rows with edge whitespace on each side (`'a'.repeat(1000) + ' '`, `' ' + 'a'.repeat(1000)`, and 999 plus a space enabled) and one astral character (`String.fromCodePoint(0x1f4d6)`, length 2) at cap and cap + 1, or trimmed-length, one-sided trim and code-point counters survive (spec 12 T9 lost a round).
- A list keyed by index needs two identical entries under the console trap, or `key={text}` survives (T8 A-key, T9 F2).
- "Show our own text, not the server's" is untestable with the realistic fixture when the server sends that same text (the bookstore's `assistant-unavailable` detail IS the spec 12 text): the fixture detail and title must differ from the page text, and the `details` list must be non-empty, or rendering `problem.detail` or keeping `details: problem.details` survives (fix-08 T2 lost a round on the list).

## Contrast and style-guard tests

- Auto-extracting text pairs from rules that declare colour and background together misses inherited pairs: demand a per-`color` coverage rule (same-rule background or a named-surface comment) that fails otherwise.
- Calibration must call the SAME predicate and threshold as the pair checks (4.22 blue on white kills a 4.0 threshold).
- A brand logotype below 4.5 needs an asserted exemption list, never a structural dodge.
- A raw-glob guard needs anti-vacuity (named files in the scanned set, exact exclusion list) plus a probe in a NEW file.
- A named-surface comment is a trusted claim: demand a test that the claimed surface exists (the ancestor rule's background), or deleting the ancestor rule leaves white-on-snow links green (spec 12 T4 K4).
- A threshold constant is pinned only between the nearest calibration pairs: name a band witness (`#767676` on white 4.54 passes, `#777777` 4.48 fails), or record it as equivalent only after checking no pair of the pinned token set lies in the band.
- A class contract checked as "some selector names the class" lets base blocks be deleted; later tasks need an insertion-only diff on the stylesheet.
- A text pin on a stylesheet (exact selector list plus value) cannot see the cascade: a later overriding rule survives it. Close that with a live `getComputedStyle(cell)` value plus the hash pin, not a REJECT (spec 12 T11).

## Layout and live-browser checks

- Layout claims in CSS (a scroll wrapper, wrapping, sticky) have no falsifier in jsdom: put a short live-browser measurement with realistic data at 390 px into the task that writes the rule, not only into the final walk. An `overflow-x: auto` wrapper needs `scrollWidth > clientWidth` on it and one client rect per word of every cell; an inherited `overflow-wrap: anywhere` (from a card) shrinks a table's min-content, so the wrapper never scrolls and headings break mid-word (spec 12, found only at T10).
- A data-URI favicon with an SVG `xmlns` URL trips any "no absolute URL in index.html" test; `data:,` avoids the request with no URL and no colour literal.
- The Boot jar serves `/shop/index.html` with `Last-Modified` 1980 and no `Cache-Control`, so a reused browser keeps the old page after a rebuild: every live check of a new build runs in a brand-new browser context.
