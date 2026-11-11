---
title: Welcome
slug: /
sidebar_position: 1
---

Welcome to Crafting your Agentic Harness, the J-Fall 2026 pre-conference workshop of OpenValue AI. It is a 4 hour workshop at beginner level, in English. Maik Kingma runs it.

You work on the Agentic Bookstore, a brownfield app with a strict, test-enforced architecture (a Clean Hexagonal Onion), and you use Claude Code as your harness. Claude Code exposes every lever of a harness (context files, rules, skills, commands, subagents, hooks, permissions, MCP, language servers) as plain files or plugins, so you learn the concepts without building a harness from scratch.

:::tip[Start here]
Run the [setup check](./00-setup-check.md) before the workshop, and again in the first 10 minutes of the day.
:::

## Agenda

The workshop is five levels that build on each other. Level 0 is the tutorial level. Each level closes with a short debrief, so the room stays together.

Times are relative to the start of the workshop.

| Time | Block | Minutes | Format |
|---|---|---|---|
| 0:00 | [Welcome, setup check while people settle](./00-setup-check.md) | 10 | Room |
| 0:10 | Keynote: Agentic engineering and the harness | 45 | Slides |
| 0:55 | [Level 0: Break the agent](./01-level-0-break-the-agent.mdx) | 25 | Solo 22, debrief 3 |
| 1:20 | [Level 1: Context engineering](./02-level-1-context-engineering.mdx) | 35 | Solo 32, debrief 3 |
| 1:55 | Break | 15 | |
| 2:10 | [Level 2: Give your agent eyes (LSP + MCP)](./03-level-2-give-your-agent-eyes.mdx) | 20 | Solo 18 (LSP 10, MCP 8), debrief 2 |
| 2:30 | [Level 3: Behavioral control (permissions, hooks, skills, commands, subagents)](./04-level-3-behavioral-control.mdx) | 30 | Solo 28 (permissions 4, hooks 14, skill and command 10), debrief 2 |
| 3:00 | [Level 4: Spec-driven development](./05-level-4-spec-driven-development.mdx) | 45 | Solo 42, debrief 3 |
| 3:45 | [Wrap-up: who cleared which level, show and tell, Q&A](./06-wrap-up.md) | 15 | Room |

Together that is 240 minutes.

During the levels Maik and Tom walk around and help.

Level 2 and Level 3 have stretch goals, and Level 3 has an optional Stop hook. If you are behind, skip them and keep Level 4, because shipping a feature end to end is the payoff of the day.

## The five levels

- [Level 0: Break the agent](./01-level-0-break-the-agent.mdx) (25 minutes). Experience the failure modes on a codebase where the agent knows nothing about our conventions.
- [Level 1: Context engineering](./02-level-1-context-engineering.mdx) (35 minutes). Give the agent the smallest set of high-signal context that makes the right thing the obvious thing.
- [Level 2: Give your agent eyes (LSP + MCP)](./03-level-2-give-your-agent-eyes.mdx) (20 minutes). Let the agent see what you see: the code through a language server, and real external data through our own MCP server, instead of guessing.
- [Level 3: Behavioral control](./04-level-3-behavioral-control.mdx) (30 minutes). Turn the things that must happen every time into guarantees, and package repeatable work.
- [Level 4: Spec-driven development](./05-level-4-spec-driven-development.mdx) (45 minutes). Turn a vague request into a spec, a plan and tasks, then let the agent implement and verify it inside the harness you built. You pick OpenSpec, Spec Kit or Agent OS.

## How to use this guide

- Work at your own pace. Fast people move on, slow people catch up.
- Every level page starts with a progress indicator and an at a glance box: the goal, the branch to start from, the catch-up command and the time.
- Checkpoints give you a command and the output to expect.
- Every level ends with "Level cleared when", a checklist you run yourself.
- If you are far behind, run the catch-up command from the at a glance box at the start of a level. Each level page names its own, and [I am far behind](./reference/troubleshooting.md#far-behind) lists them all.
- The workshop API key is shown on the screen on the day. Never commit it.
- The guide also works offline. If you have the zip, extract it and open `index.html` in a browser. If the first page is blank, see [Offline copy shows a blank page](./reference/troubleshooting.md#offline-copy-blank-page).
- If something does not work, start with [Troubleshooting](./reference/troubleshooting.md). The [cheat sheet](./reference/cheat-sheet.md) lists the commands and files of the day.
