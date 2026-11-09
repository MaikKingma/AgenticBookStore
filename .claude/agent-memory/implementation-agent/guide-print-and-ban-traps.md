---
name: guide-print-and-ban-traps
description: Traps for printable Docusaurus pages (A4 page budget, how to count printed pages) and for adding a checker rule that the frozen live self-tests compare line by line
metadata:
  type: reference
---

- A4 print budget in Chromium (`page.pdf({format: 'A4'})`, margin 0): 1123 px per page, minus the 32 px bottom padding of
  `.container`. Infima table rows cost 51 px (one line), 78 px, 104 px; a bullet line costs 30 px. A 24-row table
  cannot share a page with anything, a bullet list of the same items fits. Measure at viewport 794 with
  `emulateMedia({media: 'print'})`: heading tops and `scrollHeight` predict the page count.
- Count printed pages without PDF tools: the `/Count N` of the `/Type /Pages` object (regex on the latin1 text).
  "The second page only holds section X" = print again with section X set to `display:none` and expect one page.
- A frozen "live guide" test that compares the CLI stdout line by line to in-process `runChecks` forbids a new `ok`
  line from an opt-in rule: print nothing when the rule passes, a `FAIL <rule>` line plus errors when it fails.
  Opt-in rules reach the CLI through `runChecks` options set only for the default root (`positional.length === 0`).
- In a rendered link check, heading permalinks (`a.hash-link`) also match `href*=reference/...` in the offline hash build
  (`#/reference/page#id`, a double hash); exclude `.hash-link` and take the id after the last `#`.
- Never `sleep` in a Bash call (blocked): start the run with `run_in_background` and wait for the notification.
