# Spec 16: Bonus level, subagents and a workflow for spec-driven development

> Module: `guide` (and `docs/spec-pack/workshop`) · Depends on: 15 · Capability: `workshop-guide` · Decision D20

## Status
Started by the owner on 2026-11-11 during the live workshop as a full SDD cycle.

## Why
Fast participants need something after Level 4. The natural next step after spec-driven development is to let a
small team of agents carry a feature through its flow, and to have Claude write that flow down as code.

## What changes
A new last page, "Bonus level: orchestrate your own team", after Level 4 and before the wrap-up, clearly marked as
optional for those who finish early. It is an exercise without solutions:

1. **Think first.** Participants sketch the normal flow a feature goes through in their team (for example: understand
   the request, research, plan, implement, verify, review), and decide where an agent helps and where a human gate
   belongs.
2. **Define two subagents.** One research agent and one QA agent (Claude Code subagent files under
   `.claude/agents/`), with a role, the tools each may use and what each hands back. The guide explains the parts of a
   subagent definition and links the official docs; it does not give finished agent files.
3. **A simple team orchestration.** The main session coordinates the two agents along the flow from step 1, building
   on the Level 4 spec workflow.
4. **Let Claude write the workflow as code.** Instructions for using Claude Code's workflow feature (the workflow
   command and how a workflow script orchestrates agents) so Claude writes the orchestration from step 3 as a
   workflow. Verify the exact command, file location and syntax against the official Claude Code docs for the version
   participants use, and cite them in the spec folder.
5. **Encourage creativity**: adapt the flow, add a gate, swap an agent role.

No solution branch, no reference agent files, no finished workflow script anywhere in the repo or the guide.

## Out of scope
New level branches, solution content, changes to the bookstore or the storefront.

## Requirements
- The page fits the guide's structure and checks (level bar or a clear "bonus" marker, "cleared when" checklist if
  the structure rule requires one; update frozen rules with a reason, never weaken them). The workshop plan gains the
  bonus section so the coverage map stays anchored.
- Every command on the page is in the pack and passes the command checker.
- Plain English, no em dashes, ASCII only; all guide checks and `./mvnw clean verify` green.

## Definition of done
Requirements green on the spec PR, CI green; after the merge the Guide workflow redeploys the site with the bonus page.
