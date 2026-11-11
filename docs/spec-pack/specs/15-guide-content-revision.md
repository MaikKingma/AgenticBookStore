# Spec 15: Guide content revision after Maik's review

> Modules: `guide`, `docs/spec-pack/workshop` · Depends on: 11, 14 (start after spec 14 is merged) · Capability:
> `workshop-guide` · Decision D19

## Why
Maik reviewed the live guide on 2026-11-10. Some steps are too vague for people new to Claude Code, the sync moments
no longer match how the workshop runs (Maik and Tom walk around instead of demoing), and Level 4 should teach the
frameworks' own getting-started order.

## What changes

### All levels
1. **No sync moments.** Remove every sync moment from every level page, the welcome page timetable and any other
   reference in the guide. Their minutes go to solo time; each level keeps its total length. The workshop plan
   (`docs/spec-pack/workshop/workshop-plan.md`) changes the same way, so the guide and the pack agree and the
   coverage map stays anchored. Facilitators walk around; there is no facilitator demo step.
2. **Invite creativity.** Where it fits (at least once per level from Level 1 on, never as filler), encourage
   participants to invent their own rules, skills, hooks, guardrails or commands that suit them, in the spirit of
   "the best harness has a creative engineer tweaking it continuously". Keep these as short prompts or tips, not new
   required steps.

### Level 1
3. **Step 3, path-scoped rules:** spell it out for someone who has never seen a rule. Give the exact file path to
   create (under `.claude/rules/`), the complete file content including the metadata header (frontmatter with the
   path scope) and one sentence on what the header does and when the rule loads. The example must match what
   `solution/level-1` actually contains and Claude Code's current rules format (check the official docs; cite them in
   the spec folder).
4. **Step 5, context rot:** add a faithful text mock of the `/context` output grid in a code block (Maik's choice; no
   screenshot, agents cannot run Claude Code headless, D14), with short annotations on what to watch for (the share
   of messages and tool results growing, free space shrinking, when to `/compact` or `/clear`). Base the mock on the
   official docs and say it is illustrative.

### Level 2
5. **Step 5, jdtls missing:** drop "Still stuck after two minutes: watch the demo and continue with Part B" (there is
   no demo). Instead give debugging options: a ready-to-paste prompt asking Claude to diagnose the LSP setup, and
   alternative checks (is `jdtls` on the PATH, Java version, plugin enabled, restart the session). Put them in a
   collapsible titled section (Docusaurus `<details><summary>`), since they are not relevant to everyone. Continuing
   with Part B stays the fallback.

### Level 4
6. **Getting-started order per framework.** The `start/level-4-*` branches keep their prepared setup (Maik, D19):
   OpenSpec initialised, Spec Kit constitution written, Agent OS standards discovered and indexed. The guide says so,
   and adds a short "for your own brownfield project" note per track with the framework's own first steps in its
   documented order:
   - Agent OS: discover standards and plan product first (then index/inject), before shaping a spec.
   - Spec Kit and OpenSpec: their documented first commands for an existing codebase (for example Spec Kit's
     constitution step, OpenSpec's init and project context), in the order their docs give.
   Review the existing Level 4 track steps against each framework's official docs (pinned versions in
   `product/tech-stack.md`) and correct any command or order that differs. Cite the doc pages in the spec folder.

### Carried over from spec 14, spec 10 and the reviews
7. **OpenSpec install route (PF-T11-1, blocker for Track A):** `start/level-4-openspec` ships no `openspec` CLI and the
   setup page says Node.js is not needed. Give a working, pinned install route (version from `product/tech-stack.md`)
   on the setup page and the Level 4 OpenSpec track, and correct the Node.js sentence. The branch stays as it is.
8. **Wide tables at phone width (spec 14 R5, A4):** axe `scrollable-region-focusable` on the wide tables of Level 4,
   reference/architecture and reference/cheat-sheet at 390 px. Wrap them in a focusable, labelled scroll container
   and drop spec 14's A4 exclusion so the 390 px sweep covers those tables again.
9. **Spec 14 test gaps RA-1 to RA-4** (see `agent-os/specs/2026-11-10-1610-14-guide-branding/plan.md`): close them
   if they touch pages this spec edits anyway.

## Out of scope
Spec 14's branding (header, logo, breadcrumb, colours). Level branch content (spec 10). The storefront.

## Requirements
- All guide checks stay green: `npm run check` (structure, coverage anchored to the pack, content, PowerShell lines),
  `typecheck`, `build`, `check:built`, `build:offline`, browser checks. Where a frozen rule, fixture, hash or coverage
  entry pins sync moments or changed text, update it in the same task with the reason so it still guards the same
  thing; never weaken or skip a check.
- Every new command on a page must be in the pack (pack and guide change together) and pass the command checker.
- `./mvnw clean verify` from the repo root stays green.
- Plain English, no em dashes, ASCII only.

## Tasks (suggested)
1. Remove sync moments across the pack and the guide, rebalance the timetable (item 1).
2. Level 1 rules and `/context` mock (items 3, 4).
3. Level 2 debugging section (item 5).
4. Level 4 getting-started order per framework (item 6).
5. Creativity prompts across levels (item 2), last, so it sees the final pages.

## Definition of done
- Requirements green on the spec PR, CI green, built pages show no sync moment.
- After the merge into `factory` the Guide workflow redeploys the site.
