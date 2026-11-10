---
name: guide-site-design-decisions
description: Spec 11 (Docusaurus guide) strategy rationale - checker-first skeletons, marker-gated coverage, pack-anchored manifest, check-time source reading, contrast by variable name, Infima and MDX traps
metadata:
  type: project
---

Rationale behind the spec 11 strategy (full plan: lead-1 outbox `strategy-11-workshop-guide.md`, then the spec folder).
Settled config facts (two builds, hash router, bracket admonitions, pins) live in the spec folder and research; not here.

- **Checker first, content second.** For docs that restate a living source, write the checker and a coverage manifest
  before any page body. **Why:** verifiers otherwise compare checklists by eye on every task, and drift after the cycle
  goes unseen. **How to apply:** infrastructure tasks (scaffold, CI, brand, skeleton + structure checks, manifest),
  then one content task per large page.
- **Skeleton marker = per-page feature flag.** Skeleton pages carry final titles, header boxes, final checklists and
  contractual anchors plus a marker comment; the marker exempts only body coverage, never structure; the last task bans
  it. **Why:** keeps every merge green while coverage rules are live, and links/anchors resolve before bodies exist.
- **Anchored manifest.** Every coverage snippet must exist in the named source section AND on the page. **Why:** a
  manifest written by the same agents would otherwise grade invented content. Content tasks may not edit it.
- **Read the source at check time, do not snapshot.** Valid because `guide/` only lives on `factory` (the spec PR merges
  after spec 09 published `main`). If a guide ever ships to a stripped branch, revisit.
- **Windows tabs anchored to the pack.** Every line in a PowerShell tab must exist in the pack text: mechanical
  "never invent Windows commands".
- **Contrast script reads CSS variables by name.** No colour literals in the script; declared pairs per mode; text 4.5,
  non-text 3.0; anti-vacuity on pair count. axe on rendered pages catches undeclared Infima defaults.
- **Infima traps.** Navbar link hover defaults to primary (dark teal on navy ~1.3:1); dark-mode admonition palettes are
  off-brand; the spec's literal "navy labels" fail on a navy dark background. Declare these variables explicitly.
- **Script-name contract.** Fix npm script names in the scaffold task so the workflow never changes when later tasks add
  checks (API-first between `package.json` and CI).
- **Trap: research lives in git-ignored comms.** Ask the orchestrator to copy research into the spec folder so agents
  in other worktrees can read it by repo path.

Related: [[archunit-referee-design]] (anti-vacuity guards), [[strategy-constraints]] (escape trap, stripped paths).
