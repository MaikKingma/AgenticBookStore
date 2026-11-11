---
name: http-replay-techniques
description: How to prove .http collection files live (replay driver, node handler shim, state traps of the seeded bookstore) without an IDE
metadata:
  type: reference
---
- Replay driver shape that worked: python3 -I script splits the file's own bytes at `###`, substitutes `{{var}}` and `{{$uuid}}`,
  sends with urllib, and runs each block's `> {% ... %}` handler through `node -e` with a tiny shim (`client.global.set/get`,
  `client.test`, `client.assert`, `response.status/body`). Flag a capture of `undefined` (a 409/503 body has no `id`),
  unresolved variables, and a missing status line as FAIL.
- Make statuses machine-checkable: one line `# Expected status: 202 (distributor running); 503 (distributor stopped).` per block;
  the driver mode picks the alternative by its label. A free-text tail after the code breaks the grammar.
- Run-mode blocks need three driver modes: distributor up, jar killed (imports and manual restock 503), and
  `--distributor.chaos.failure-rate=1.0` (only the chaos block), each with a fresh container.
- Seeded state traps: new books start with a stock row at 0 (assert low-stock membership by id, never a count after other files
  ran); a dip fires the automatic restock only when the signal is false, and `receive` resets it.
- Run the jar with `env -u BOOKSTORE_AI_API_KEY` for the offline assistant path (503 offline, 400 before offline, 429 only with a key).
