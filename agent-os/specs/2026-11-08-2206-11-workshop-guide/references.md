# References: spec 11, workshop guide site

## Pack sources (the guide restates these; they stay the source of truth)

- `docs/spec-pack/specs/11-workshop-guide-site.md` (the spec)
- `docs/spec-pack/workshop/workshop-plan.md` sections 2 to 10 (agenda, setup check, Levels 0 to 4, wrap-up; section 11
  risks feed the troubleshooting page without facilitator notes)
- `docs/spec-pack/workshop/sdd-framework-guide.md` Part 2 (Level 4 tracks)
- `docs/spec-pack/workshop/level-4-feature-briefs.md` lines 6 to 36 (briefs; facilitator notes excluded)
- `docs/spec-pack/product/roadmap.md` (branches and tags, catch-up rule), `product/architecture-standard.md`,
  `product/tech-stack.md`
- `docs/spec-pack/specs/09-workshop-baseline.md` and `10-harness-reference-solutions.md` (prerequisites, hook
  scripts, solution branch contents)
- `docs/spec-pack/orchestrator/decisions.md` D6 and D7

## Research and strategy (copied into this folder)

- `research/research-docusaurus-offline-config.md` (res-1, senior): configs, file:// verification, MDX traps, WebKit
  first-load flake
- `research/research-guide-workflow.md` (res-2): guide.yml design, SHA pins, gate, actionlint
- `research/research-guide-content-inventory.md` (res-3): page-by-page inventory, verbatim checklists, gap table,
  the `source line -> item -> page` QA table (section 9)
- `research/research-openvalue-colours.md` (res-4): deck colour census (palette superseded by D7)
- `strategy.md` (lead-1): approach, ground rules, page grammar, npm script contract, task cards T1 to T11,
  follow-ups F1 to F13
- `qa-strategy.md` (qa-lead, snapshot at Gate 1): tiers, the common verifier block C0, task directives, Gate 1
  findings G-1 to G-14, qa-final focus. The live ledger is
  `.claude/comms/mailboxes/qa-lead/outbox/qa-ledger-11-workshop-guide.md` (local, git-ignored).

## Existing code referenced

- `.github/workflows/ci.yml` (pinning, timeout and concurrency style)
- `distributor/src/main/java/nl/openvalue/distributor/mcp/CatalogMcpTools.java` (Level 2 Part B pointer)
- Prototype of res-1 (local scratch only, not committed)
