---
title: Cheat sheet
sidebar_position: 1
---

One page to print. Commands with a `/` run inside Claude Code.

## Commands and files

- `claude --version`, `/status`: check the install and your login; `/model sonnet`, `/usage`: use Sonnet, check your usage limit
- `/rewind` or `Esc` twice: see checkpoints, git is the safety net
- `/init`, `/context`, `/memory`: first `CLAUDE.md`, context grid, memory files
- `/clear`, `/compact focus on <topic>`: start fresh, compact a drifting task
- `/plugin`: plugins, tabs **Installed** and **Errors**
- `/plugin marketplace add anthropics/claude-plugins-official` first, if missing, then `/plugin install jdtls-lsp@claude-plugins-official`
- `claude mcp add --transport http --scope project distributor http://localhost:8090/mcp`: writes `.mcp.json`
- `/mcp`, `claude --debug`: check the MCP servers, debug output
- `Shift+Tab`, `/plan`: cycle the modes, plan read-only; `/ship <what changes>`: `.claude/commands/ship.md`, uses `$ARGUMENTS`
- `CLAUDE.md`: the project instructions, at most 60 lines; `~/.claude/CLAUDE.md`, `CLAUDE.local.md`: your user and personal files
- `.claude/rules/`: path-scoped rules with `paths:` frontmatter; `docs/architecture.md`: the architecture document
- `.claude/settings.json`: permissions (deny, then ask, then allow), hooks
- `.claude/hooks/tests-before-commit.sh`: the hook script, exit code 2 blocks
- `.githooks/pre-commit`: on with `git config core.hooksPath .githooks`
- `.claude/skills/new-use-case/SKILL.md`: a skill, a repeatable workflow
- `.claude/agents/architecture-reviewer.md`: stretch, a review subagent

Hooks: exit code 2 blocks, any other non-zero exit code does not. The command finds its script through `$CLAUDE_PROJECT_DIR`. The Stop hook checks `stop_hook_active`.

## Level 4 commands

| Track | Branch | Commands |
|---|---|---|
| A: OpenSpec | `start/level-4-openspec` | `/opsx:explore`, `/opsx:propose`, `openspec validate --strict`, `/opsx:update`, `/opsx:apply`, `/opsx:archive` |
| B: Spec Kit | `start/level-4-speckit` | `/speckit-specify`, `/speckit-clarify`, `/speckit-plan`, `/speckit-tasks`, `/speckit-implement`, `/speckit-converge` |
| C: Agent OS | `start/level-4-agentos` | `/plan` then `/shape-spec`, `/inject-standards testing`, implement the plan |

`openspec ...` runs in the terminal, `/opsx:...` in Claude Code. In every track you commit through your `/ship` command.
