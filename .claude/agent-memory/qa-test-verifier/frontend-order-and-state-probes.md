---
name: frontend-order-and-state-probes
description: Frontend page and hook mutants - pass-through order, money strings, router and form gaps, input limits
applies-to:
  - frontend/**
metadata:
  type: feedback
---
Rule: for a "passed through in order" claim (API client lists, ProblemDetail `details`), probe a stable sort per attribute in BOTH directions after the fetch and run it on the whole suite.
**Why:** fixtures that look unordered often are not (`SHIPPED, PLACED, PLACED` or `EUR, EUR, USD` is non-strictly sorted); a two-element "in request order" claim can never satisfy a three-element rule.
**How to apply:**
- Add one ascending control mutant that must die (proves mutants reach the assertion). Read every column before believing "not sorted"; also tabulate DERIVED displayed columns (line totals "15.98", "37.50", "4.35" were ascending as text) and string-array fixtures by derived keys (last word, length): famous author lists can be ordered by coincidence. Survivors on held-attribute-only rules are informational; suggest a fixture whose derived keys are unordered too.
- Find order fixtures by structure, not test title: every array inside any `toEqual`/`toStrictEqual` pass-through (arrays in a view, in a SENT payload asserted "unchanged", a helper's `mock.calls`). A generated sweep (attribute x direction x string/number compare, about 70 mutants at 2 s) backs the table. A title-only scan missed three fixtures and cost a rework round.
- Sort mutants on reducer or hook state: return the SAME array when nothing moves (`s.every((l, i) => l === xs[i]) ? xs : s`), else `toBe` identity assertions kill for the wrong reason; sweep reducer output, hydration and exposed value; compare numbers as strings too (`[3, 10, 1]` is strictly descending as text).
- Money display: "shown as sent" and "handed over as sent" for a decimal string need a trailing zero (`"12.50"`); `Number(price)` survives every `19.99` fixture, in the display AND in the page-to-store call (probe both sites). Check the texts a test really asserts, not the fixtures it defines.

Rule: probe routers, forms, in-flight flags and error mapping at the branches the implementer's own list leaves out.
**Why:** each of these found a real gap where the implementer's mutant list was clean.
**How to apply:**
- Hand-rolled route parser: the final `return notFound` branch (valid id after an unknown first segment, `#/basket/x`) is unexecuted when an empty-segment rule catches every UNKNOWN entry first; probe by returning a known page there. A display decision "shown as sent" needs a value a plausible transform changes (underscore genre vs `.replace('_', ' ')`).
- Remove `event.preventDefault()` from a form `onSubmit`: the test stays green and jsdom prints `Not implemented: HTMLFormElement's requestSubmit()` straight to stderr, bypassing a `console.error` spy trap; only a `defaultPrevented` assertion or the stderr grep guards it. A document-level `submit` listener is a sound "was it cancelled" witness (React listens on the root).
- In-flight flag in a context provider (ref guard plus `placing` state): probe the SUCCESS-branch reset as well as the error branch. Deleting `sending.current = false` from the 202 branch survived every test placing one order (every later order silently dropped). Prove with a temporary renderHook witness (`BasketProvider`, two `placeOrder` calls, POST count 2). A module-level guard in the page is browser-identical; only a provider-level same-tick hook test separates them. A reducer re-deciding what the callback already decided has a refusal branch only two dispatches in one `act()` reach.
- Error-text mapping (a code recognised, the page shows its own text and hides server title, detail AND `details`): a shared fixture helper that always sends `details: []` lets "keep `details: problem.details`" survive; the fixture needs a non-empty list for that code. "Keyed on status, not code": page-level alternative render (`problem.status === 409 ? <own list> : <Alert>`) killed by an empty-details same-status witness (`queryByRole('list')` null).
- Text-input limit pages (1000 chars): `'a'.repeat(1001)` leaves three mutants alive: limit on `draft.trim().length` (guard, flag, counter text) and `[...draft].length`. Close with edge whitespace at the boundary (1000 plus a space too long; 999 plus a space not) and an astral char (`String.fromCodePoint(0x1f4d6)` = 2 UTF-16 units, as server `@Size` counts). A list keyed by message text survives unless a test sends the same text twice (the duplicate-key warning is the only witness): probe `key={message.text}`.
Related: [[frontend-vitest-probe-techniques]], [[live-ui-walk-verification]]
