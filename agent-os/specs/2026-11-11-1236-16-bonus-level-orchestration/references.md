# References: spec 16 bonus level orchestration

## Pack and decisions

- Pack spec: `docs/spec-pack/specs/16-bonus-level-orchestration.md`; decision D20 in `docs/spec-pack/orchestrator/decisions.md` (D17 branch contract, D14 no headless Claude Code, D19 creativity tips).
- Earlier guide specs: `agent-os/specs/2026-11-08-2206-11-workshop-guide/`, `agent-os/specs/2026-11-10-1610-14-guide-branding/`, `agent-os/specs/2026-11-11-0145-15-guide-content-revision/`.
- Guide conventions and checker: `guide/README.md`; rules in `guide/scripts/rules/`.

## Official Claude Code documentation relied on (HR1)

Fetched raw as markdown with curl on 2026-11-11 (research res-6). These are live pages: line numbers and hashes drift,
so qa-final re-fetches every page and re-finds every quote with `grep -F` before hand-back.

- Dynamic workflows: https://code.claude.com/docs/en/workflows (raw: https://code.claude.com/docs/en/workflows.md)
- Custom subagents: https://code.claude.com/docs/en/sub-agents (raw: `.md`)
- Choosing between parallel options: https://code.claude.com/docs/en/agents
- The `.claude` directory: https://code.claude.com/docs/en/claude-directory
- Tools reference: https://code.claude.com/docs/en/tools-reference
- Commands: https://code.claude.com/docs/en/commands
- Agent teams (experimental; NOT presented on the page): https://code.claude.com/docs/en/agent-teams
- Agent SDK TypeScript, Workflow tool: https://code.claude.com/docs/en/agent-sdk/typescript
- Changelog: https://raw.githubusercontent.com/anthropics/claude-code/main/CHANGELOG.md (2.1.154 introduces dynamic workflows; 2.1.198 removes the `/agents` wizard; newest 2.1.322 at fetch time)

## Verified claims (res-6, verbatim)

Line numbers are file lines in the saved raw markdown. Files: WF = workflows.md, SA = sub-agents.md, TR = tools-reference.md, CMD = commands.md, CD = claude-directory.md, AT = agent-teams.md, SDK = agent-sdk_typescript.md, CL = CHANGELOG.md. URLs are in the file table below.

| ID | Verdict | Exact quote | File:line |
|---|---|---|---|
| W1 | VERIFIED | "A dynamic workflow is a JavaScript script that orchestrates many [subagents] at once. Claude writes the script for the task you describe, and a runtime executes it in the background while your session stays responsive." Title: "Orchestrate subagents at scale with dynamic workflows" | WF:5, WF:13 |
| W2 | VERIFIED | "include the keyword `ultracode` in your prompt. Asking in your own words, for example "use a workflow" or "run a workflow", also works: Claude treats a direct request as the same opt-in." And: "set `/effort ultracode` and Claude plans a workflow for every substantive task in the session." And: "This applies to every task in the session" | WF:115, WF:109, WF:150 |
| W3 | VERIFIED | "Run `/workflows`, select the run you want to keep, and press `s`." "The workflow runs as `/<name>` in future sessions from either location." "Claude Code includes `/deep-research` as a built-in workflow". Key table row: "`s` | Save the run's script as a command". Also "`/workflows` | Open the workflow progress view to watch, pause, resume, or save running and completed workflows" | WF:196, WF:201, WF:72, WF:98; CMD:172 |
| W4 | VERIFIED | "`.claude/workflows/` in your project: shared with everyone who clones the repo" and "`~/.claude/workflows/` in your home directory: available in every project, visible only to you." Extension: "edit its `.js` file"; directory table "`workflows/*.js` | Project and global" | WF:198-199, WF:315, CD:77 |
| W5 | VERIFIED WITH GAP | Example: `export const meta = {` `name: 'audit-routes',` `description: '...'` `}`; `const found = await agent('List every .ts file under src/routes/.', { schema: {...} })`; `await pipeline(found.files, file => agent(..., { label: file }))`; `return audits.filter(Boolean)`. Text: "The body is plain JavaScript with top-level `await`. `agent()` spawns one subagent, `pipeline()` runs one per item in a list, and `parallel()` runs a set of agent tasks at the same time and waits for all of them." "you can call `phase()` to group the agents that follow under a title in the progress view, call `log()` to show a message above the phases, and read the `args` global." "keep `export const meta` as the first statement, and keep it a plain object literal with a `name` and a `description`." GAP: "ends with `return`" is only shown in the example (WF:300). No sentence states it as a rule. | WF:287-301, WF:303, WF:321-322, WF:220; SDK:3573 |
| W6 | VERIFIED | "Dynamic workflows are available on all paid plans, with Anthropic API access, and on Amazon Bedrock, Google Cloud's Agent Platform, and Microsoft Foundry. On Pro, turn them on from the Dynamic workflows row in `/config`." (Page says "Google Cloud's Agent Platform", not "Vertex".) Admin: "To turn workflows off for your whole organization, set `"disableWorkflows": true` in managed settings, or use the toggle on the Claude Code admin settings page." Label search: grep -i "preview\|beta\|experimental" on WF returns no line. No preview/beta/research-preview/experimental label appears on the page. | WF:11, WF:490; grep on WF: no hit |
| W7 | VERIFIED | Under heading `## 2.1.154`: "Introducing dynamic workflows: ask Claude to create a workflow and it orchestrates work across tens to hundreds of agents in the background, so you can take on larger, more complex tasks. Run `/workflows` to view your runs". Newest version at top: `## 2.1.322` ("Added fast mode on Vertex in the global, us and eu regions..."). Note SDK: "The `Workflow` tool is available in Agent SDK v0.3.149 and later." | CL:6186, CL:6189, CL:3; SDK:3569 |
| W8 | VERIFIED | Table row: "No mid-run user input | A run pauses on its own only for agent permission prompts and a usage-limit wait. For sign-off between stages, run each stage as its own workflow" | WF:352 |
| W9 | NOT FOUND | See pattern list below. Closest text: WF:340 "Two agents that run with the same model, effort level, agent type, tools, output schema, and working directory build the same tools-and-system-prompt prefix" (mentions "agent type" as a cache-key property, no syntax). WF:504 only links to custom subagents as "the worker primitive workflows orchestrate". The only documented `agent()` option names are `schema`, `label`, `stallMs`, and a model the script names (WF:452 "A model the script names for a stage"). | see below |
| W10 | NOT FOUND | No platform requirement or caveat for workflows on Windows or WSL. The only platform mentions on WF: WF:127 "press `Option+W` on macOS or `Alt+W` on Windows and Linux to dismiss the highlight"; WF:407 a run does not auto-pause at a usage limit in "an SSH, WSL, Dispatch, or scheduled task session" in the Code tab. CHANGELOG: CL:1509 "Fixed the Workflow tool refusing script files with CRLF line endings, such as files checked out on Windows". | WF:127, WF:407; CL:1509 |
| S1 | VERIFIED | Priority table: "`.claude/agents/` | Current project | 3 | Ask Claude, or create the file manually" and "`~/.claude/agents/` | All your projects | 4". "When multiple subagents share the same name, Claude Code uses the one from the higher-priority location." (Managed settings=1 and `--agents` flag=2 outrank both; plugin=5 lowest.) | SA:156-164 |
| S2 | VERIFIED | "Subagents are Markdown files with YAML frontmatter." "Subagent files use YAML frontmatter for configuration, followed by the system prompt in Markdown" "The body becomes the system prompt". "Only `name` and `description` are required." Full field table in next section. | SA:94, SA:242, SA:266, SA:289, SA:296-316 |
| S3 | VERIFIED WITH GAP | "`tools` | No | Tools the subagent can use, as a comma-separated string such as `Read, Grep, Bash` or a YAML list. Inherits every tool available to subagents if omitted." Example `tools: Read, Grep, Glob, Bash`. GAP: no sentence says `Bash(...)` patterns are valid in `tools`. The docs say the opposite for `disallowedTools`: "An entry with a specifier, such as `Bash(git push *)`, still removes the whole tool" and "To keep Bash and block specific commands, add a Bash deny rule such as `Bash(git push *)` to `permissions.deny`". The only parenthesized form allowed in `tools` is `Agent(agent_type)`, main-thread-only (SA:503, SA:527). | SA:300-301, SA:458, SA:492, SA:503, SA:527 |
| S4 | VERIFIED | Tool names: `Read`, `Grep`, `Glob`, `Bash`, `Edit`, `Write`, `WebFetch`, `WebSearch` (table rows). Glob/Grep note: "`Glob` | Finds files based on pattern matching. Absent by default on macOS, Linux, and WSL." "`Grep` | ... Absent by default on macOS, Linux, and WSL." And: "On macOS, Linux, and WSL, Claude Code leaves Glob and Grep out of the default tool set, and Claude searches with `find` and `grep` through the Bash tool instead." Subagent exception: "A subagent lists `Glob` or `Grep` in its `tools` field and leaves out `Bash`. The listed tools come back for that subagent only". SA:429: "On macOS, Linux, and WSL, a subagent can also receive the Glob and Grep tools when the main conversation doesn't have them". Background subagents keep only a fixed built-in set that includes Read, Grep, Glob, Bash, Edit, Write, WebFetch, WebSearch (SA:440). | TR:24, 28, 34, 35, 43, 63, 64, 66; TR:313, TR:319; SA:429, SA:440 |
| S5 | VERIFIED | Automatic: "Claude automatically delegates tasks based on the task description in your request, the `description` field in subagent configurations, and current context." Explicit by name: "`Use the test-runner subagent to fix failing tests`" ("name the subagent in your prompt; Claude decides whether to delegate"). @-mention: `@"code-reviewer (agent)" look at the auth changes` ("guarantees the subagent runs for one task"); manual form "`@agent-<name>` for local subagents". Returns: "the subagent does that work in its own context and returns only the summary". Chaining: "ask Claude to use subagents in sequence. Each subagent completes its task and returns results to Claude, which then passes relevant context to the next subagent." Example: "Use the code-reviewer subagent to find performance issues, then use the optimizer subagent to fix them". | SA:9, SA:844, SA:854-855, SA:858-868, SA:875, SA:1034-1037 |
| S6 | VERIFIED | "By default, a subagent can spawn subagents of its own, up to three layers below the main conversation. At the depth limit, Claude Code withholds the `Agent` tool from every subagent except a fork". Change with `CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH`. A subagent needs `Agent` in its tools (or inheritance) to spawn: SA:527 "listing `Agent` in `tools` lets that subagent spawn subagents of its own while the depth limit allows it". Also relevant: the `Workflow` tool is removed from subagents (SA:438 "* `Workflow`" in the list "removes these tools, even when listed in the `tools` field"). | SA:1061, SA:1068, SA:527, SA:438 |
| S7 | VERIFIED | "Running `/agents` prints a reminder to ask Claude or edit `.claude/agents/` and `~/.claude/agents/` directly. On Claude Code v2.1.197 and earlier, `/agents` opens an interactive wizard with a **Running** tab that lists live subagents and a **Library** tab". CHANGELOG under `## 2.1.198`: "Removed the `/agents` wizard; ask Claude to create or manage subagents, or edit `.claude/agents/` directly" | SA:147-148; CL:5575 (heading), CL:5609 |
| S8 | VERIFIED | "Agent teams are experimental and disabled by default. Enable them by setting `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1` in your settings.json or environment." | AT:11, AT:48 |
| S9 | VERIFIED | "Each subagent starts with a fresh, isolated context window. It doesn't see your conversation history, the skills you've already invoked, or the files Claude has already read." (Exception: a fork.) Also SA:11 "Each subagent runs in its own context window". | SA:1101, SA:11 |

## W9 grep patterns run (all NOT FOUND for a how-to)

On workflows.md: `agentType|subagent_type|custom agent|custom subagent|agents/|type:` (hits only: line 293 a JSON schema `type: 'object'`, line 504 a link text "Create custom subagents"); `agent type|agentType|subagent_type|\.claude/agents|custom` (hits: line 340 and 504 only).
On sub-agents.md: `workflow` (no line says a workflow `agent()` can name a subagent type; line 390 says one model setting applies to "every subagent, teammate, and workflow agent"; line 1095 says "Agents that other features run, such as workflow agents and agent team teammates, follow their own limits instead"); `agentType|subagent_type|agent\(` (only the Agent tool's `subagent_type`, not workflow `agent()`).
On agent-sdk typescript.md: `agentType|subagent_type|agent\(`. Hits are `subagent_type` on the Agent tool input (SDK:3325) and outputs, `agentType` on output/context types (SDK:996, 4061). The Workflow tool section (SDK:3553-3580) lists only `script`, `name`, `scriptPath`, `args`, `resumeFromRunId`, `title`, `description`.
On CHANGELOG: `agent\(.*(agentType|subagent_type|type:)` and `workflow.*(custom (sub)?agent|\.claude/agents)` and the reverse order: no hit.
Not searched: pages outside the nine fetched (for example the plugin or skills pages).

### Fetched files (2026-11-11)

| File | URL | sha256 | bytes | fetched UTC |
|---|---|---|---|---|
| workflows.md | https://code.claude.com/docs/en/workflows.md | 5b262faf3ba1bb40315a27d560371bed5b4969b7ad6855bbcecf284487a49b54 | 45433 | 2026-11-11T12:16:38Z |
| sub-agents.md | https://code.claude.com/docs/en/sub-agents.md | 47e8c8b6739a522497dd10349af0c8e9a9f56898049017aafa44f0bfeb1047b6 | 116978 | 2026-11-11T12:16:38Z |
| agents.md | https://code.claude.com/docs/en/agents.md | de72ed10bf4598b409997457f9c8d3c8402751488eb1d5c897339ef0d51c1ea4 | 9383 | 2026-11-11T12:16:39Z |
| claude-directory.md | https://code.claude.com/docs/en/claude-directory.md | 1b83742fc3e01b6a1006b0a585015af83460a621971c0ce421f1089f5807632f | 32439 | 2026-11-11T12:16:39Z |
| tools-reference.md | https://code.claude.com/docs/en/tools-reference.md | 0bd9feb89bc2f4cc25af4459f397f5202b04083aeaee89f4ac9e1ee7d857e4e2 | 90939 | 2026-11-11T12:16:5x Z (retry) |
| commands.md | https://code.claude.com/docs/en/commands.md | e55acc52f1d5c3ae220d3555586c307e89f28d34e02ded783ae14611f08b9f70 | 57317 | 2026-11-11T12:16:44Z |
| agent-teams.md | https://code.claude.com/docs/en/agent-teams.md | a03340d7283bf79fec4d2918ba480ada4f7cd661334f811fa1f77b9e377e2c5d | 43855 | 2026-11-11T12:16:45Z |
| agent-sdk_typescript.md | https://code.claude.com/docs/en/agent-sdk/typescript.md | bd7cea08ed2118c182a46236b71f38d9873520acfc0eec2a3978b08f75911f68 | 332547 | 2026-11-11T12:16:45Z |
| CHANGELOG.md | https://raw.githubusercontent.com/anthropics/claude-code/main/CHANGELOG.md | a074556107911c423a449df2f0b1dbbabb51aaa66383b81c3ba4fea1c52679cf | 1193584 | 2026-11-11T12:16:46Z |

## Claims the page must not make (from the table above)

- W9: that a workflow's `agent()` call uses a `.claude/agents/` file by name (undocumented).
- W5: that a workflow script must end with `return` (only the example does).
- S3: `Bash(...)` patterns inside a subagent's `tools` line (not documented; command limits belong in permission rules).
- W10: any Windows or WSL statement about workflows.
- S8: agent teams as a normal feature (experimental, off by default).
- S7: that `/agents` opens a wizard (removed in 2.1.198).
- That a subagent can start a workflow (the `Workflow` tool is removed from every subagent, SA:438).
- `ultracode`, `/effort ultracode` and workflow script API names (strategy choice: the page describes, it does not
  teach the API; no script code anywhere, HR2).

## Codebase references

- Page placement, pins and live sitemap: research res-3 (11 live routes: `/`, `setup-check`, `level-0-break-the-agent`,
  `level-1-context-engineering`, `level-2-give-your-agent-eyes`, `level-3-behavioral-control`,
  `level-4-spec-driven-development`, `wrap-up`, `reference/cheat-sheet`, `reference/architecture`,
  `reference/troubleshooting`).
- Checks, pack definition (five files, `guide/scripts/coverage.mjs:20-26`), literal sweep, gate recipe: research res-4.
- Content context and the existing `architecture-reviewer` agent on `solution/level-3` and `start/level-4-*`: research res-5.
- Strategy and task breakdown: lead-1. QA strategy: `qa-strategy.md`.
- Research artifacts live in the gitignored comms tree of the cycle worktree
  (`.claude/comms/mailboxes/<id>/outbox/`); their decisions are carried into `shape.md` and `plan.md`.

## Claims table of the bonus page (T4, copied from the implementer's outbox after T4's gate PASS, SA2)

Copied verbatim from the T4 implementer outbox (section Claims table, after round 1) by the orchestrator (SA2). Raw doc files: WF = workflows.md, SA = sub-agents.md, TR = tools-reference.md, CMD = commands.md, CL = CHANGELOG.md, as fetched on 2026-11-11.


Source: `s16-t4-impl-claims.md` (generated by `impl-t4-claims.mjs`: page line by `includes` on the final page, doc line by fixed-string match). Proof by `grep -nF -- "<quote>" <file>` for all 64 (quote, file) pairs: `pairs 64, missing 0` (`s16-t4-impl-grepproof.txt`, each line `file:line [grep -nF found] quote`).
Files: SA = sub-agents.md, WF = workflows.md, TR = tools-reference.md, CMD = commands.md, CL = CHANGELOG.md (URLs in the table). New ids (all with a verbatim quote): S10 deny rule applies to subagents, S11 settings hooks apply inside subagents, S12 `tools` allow-list means no edit or write, S13 `model` field and `inherit`, S14 first agent file in a new `agents` directory needs a restart, W12 View raw script and the prompt depends on permission mode, W13 runs count toward plan usage and a small slice first. The three outline claims: "permissions and hooks still apply to subagents" = S10 (SA:492) and S11 (SA:759); "a run counts toward your plan's usage" = W13 (WF:441); "read the script before you approve" = W12 (WF:166, WF:171). Not behaviour claims, so no row: vocabulary and instructions, "Parts A to C do not use workflows", Level 3 and Level 4 content, the questions in the tables. No banned claim of `references.md` appears (grep above).

| page line | page sentence | claim id | official URL | file:line (raw .md) | verbatim quote |
|---|---|---|---|---|---|
| 17 | Subagent**: a helper that the main session hands one task to. | S5 | https://code.claude.com/docs/en/sub-agents | SA:9 | Subagents are specialized AI assistants that handle specific types of tasks. |
| 18 | Frontmatter**: the settings block at the top of a Markdown file. | S2 | https://code.claude.com/docs/en/sub-agents | SA:289 | between `---` markers at the top of its file |
| 19 | Workflow**: a Claude Code dynamic workflow, a script that Claude writes | W1 | https://code.claude.com/docs/en/workflows | WF:13 | Claude writes the script for the task you describe, and a runtime executes it in the background while your session stays responsive. |
| 29 | **You need:** Claude Code 2.1.154 or later and workflows on your account | W7 | https://raw.githubusercontent.com/anthropics/claude-code/main/CHANGELOG.md | CL:6189 | - Introducing dynamic workflows: ask Claude to create a workflow |
| 29 | **You need:** Claude Code 2.1.154 or later and workflows on your account | W7 | https://raw.githubusercontent.com/anthropics/claude-code/main/CHANGELOG.md | CL:6186 | ## 2.1.154 |
| 29 | **You need:** Claude Code 2.1.154 or later and workflows on your account | W6 | https://code.claude.com/docs/en/workflows | WF:11 | Dynamic workflows are available on all paid plans |
| 65 | A subagent works in its own context window. | S9 | https://code.claude.com/docs/en/sub-agents | SA:11 | Each subagent runs in its own context window |
| 65 | It does not see your conversation | S9 | https://code.claude.com/docs/en/sub-agents | SA:1101 | It doesn't see your conversation history |
| 65 | it hands back a summary when it is done | S5 | https://code.claude.com/docs/en/sub-agents | SA:9 | the subagent does that work in its own context and returns only the summary |
| 65 | That keeps long searches and logs out of your main session. | S5 | https://code.claude.com/docs/en/sub-agents | SA:9 | would flood your main conversation with search results, logs, or file contents you won't reference again |
| 67 | A subagent is a Markdown file with frontmatter. | S2 | https://code.claude.com/docs/en/sub-agents | SA:94 | Subagents are Markdown files with YAML frontmatter. |
| 67 | The file lives in `.claude/agents/` in your project. | S1 | https://code.claude.com/docs/en/sub-agents | SA:162 | \| `.claude/agents/` \| Current project \| 3 \| Ask Claude, or create the file manually \| |
| 67 | Below the frontmatter, the body holds the instructions for the subagent. | S2 | https://code.claude.com/docs/en/sub-agents | SA:266 | The body becomes the system prompt that guides the subagent's behavior. |
| 67 | Only `name` and `description` are required. | S2 | https://code.claude.com/docs/en/sub-agents | SA:289 | Only `name` and `description` are required. |
| 73 | \| `name` \| Names the subagent. Required. | S2 | https://code.claude.com/docs/en/sub-agents | SA:298 | \| `name` \| Yes \| Unique identifier |
| 74 | \| `description` \| Tells Claude when to hand a task to it. Required. | S2 | https://code.claude.com/docs/en/sub-agents | SA:299 | \| `description` \| Yes \| When Claude should delegate to this subagent \| |
| 75 | \| `tools` \| Lists the tools it may use. Optional: leave it out and it inherits every tool available to subagents. | S3 | https://code.claude.com/docs/en/sub-agents | SA:300 | \| `tools` \| No \| [Tools](#available-tools) the subagent can use |
| 75 | (same row) | S3 | https://code.claude.com/docs/en/sub-agents | SA:300 | Inherits every tool available to subagents if omitted. |
| 76 | \| `model` \| Picks the model it runs on. Optional: use an alias such as `sonnet`, or `inherit` for the model of the main session. | S13 | https://code.claude.com/docs/en/sub-agents | SA:302 | to use: `sonnet`, `opus`, `haiku`, `fable`, a full model ID such as `claude-opus-5-5`, or `inherit`. |
| 76 | (same row) | S13 | https://code.claude.com/docs/en/sub-agents | SA:302 | \| `model` \| No \| |
| 76 | (same row) | S13 | https://code.claude.com/docs/en/sub-agents | SA:353 | * **inherit**: use the same model as the main conversation |
| 77 | \| The body \| The instructions below the frontmatter. | S2 | https://code.claude.com/docs/en/sub-agents | SA:266 | The body becomes the system prompt that guides the subagent's behavior. |
| 78 | \| What it hands back \| The summary it gives to the main session. | S5 | https://code.claude.com/docs/en/sub-agents | SA:9 | the subagent does that work in its own context and returns only the summary |
| 85 | A research agent that only reads does not need `Edit` or `Write`: leave them out of its `tools`. | S12 | https://code.claude.com/docs/en/sub-agents | SA:452 | This example uses `tools` to allow only Read, Grep, Glob, and Bash. The subagent can't edit files, write files, or use any MCP tools: |
| 85 | Tool names such as `Read`, `Edit`, `Write` and `Bash` are listed in the [tools reference] | S4 | https://code.claude.com/docs/en/tools-reference | TR:43 | \| `Read` \| Reads the contents of files. |
| 85 | (same sentence) | S4 | https://code.claude.com/docs/en/tools-reference | TR:28 | \| `Edit` \| Makes targeted edits to specific files. |
| 85 | (same sentence) | S4 | https://code.claude.com/docs/en/tools-reference | TR:66 | \| `Write` \| Creates or overwrites files. |
| 85 | (same sentence) | S4 | https://code.claude.com/docs/en/tools-reference | TR:24 | \| `Bash` \| Executes shell commands in your environment. |
| 87 | If an agent keeps `Bash`, limit risky commands with the permission rules from Level 3, not with the `tools` line. | S3 | https://code.claude.com/docs/en/sub-agents | SA:492 | To keep Bash and block specific commands, add a [Bash deny rule] |
| 87 | A Bash deny rule in your settings applies to the main session and to subagents. | S10 | https://code.claude.com/docs/en/sub-agents | SA:492 | The rule applies to the main conversation and to subagents. |
| 87 | Your Level 3 hooks live in your settings too, and hooks from settings files also apply inside subagents. | S11 | https://code.claude.com/docs/en/sub-agents | SA:759 | all apply inside subagents, so a `PreToolUse` hook in `settings.json` also runs before every tool a subagent uses. |
| 89 | ask the main session to write it for you, or write it yourself. | S1 | https://code.claude.com/docs/en/sub-agents | SA:94 | To create one, ask Claude to write it for you, or |
| 89 | This is the whole shape of the frontmatter, with every value left for you: | S2 | https://code.claude.com/docs/en/sub-agents | SA:242 | Subagent files use YAML frontmatter for configuration, followed by the system prompt in Markdown: |
| 102 | lists every field. | S2 | https://code.claude.com/docs/en/sub-agents | SA:296 | \| Field \| Required \| Description \| |
| 108 | restart Claude Code first so that it loads your first agent file. | S14 | https://code.claude.com/docs/en/sub-agents | SA:250 | after creating a scope's first agent file in a new `agents` directory, restart to load it. |
| 109 | Name the agent in your request. Claude decides whether to hand the task over | S5 | https://code.claude.com/docs/en/sub-agents | SA:854 | name the subagent in your prompt; Claude decides whether to delegate |
| 109 | Ask the main session to use your research agent for the first step of your flow. | S5 | https://code.claude.com/docs/en/sub-agents | SA:861 | Use the test-runner subagent to fix failing tests |
| 112 | Ask the main session to use your QA agent on the change. | S5 | https://code.claude.com/docs/en/sub-agents | SA:861 | Use the test-runner subagent to fix failing tests |
| 112 | (same sentence) | S5 | https://code.claude.com/docs/en/sub-agents | SA:858 | For natural language, there's no special syntax. Name the subagent and Claude typically delegates: |
| 115 | You can also chain the agents in one request: ask Claude to use the research agent, then the QA agent. | S5 | https://code.claude.com/docs/en/sub-agents | SA:1034 | ask Claude to use subagents in sequence. |
| 115 | Each agent hands its results to Claude, which passes the relevant context to the next one. | S5 | https://code.claude.com/docs/en/sub-agents | SA:1034 | Each subagent completes its task and returns results to Claude, which then passes relevant context to the next subagent. |
| 121 | A workflow is a script that Claude writes for the task you describe, and a runtime runs it in the background while your session stays responsive. | W1 | https://code.claude.com/docs/en/workflows | WF:13 | Claude writes the script for the task you describe, and a runtime executes it in the background while your session stays responsive. |
| 125 | You need 2.1.154 or later, the version that introduced dynamic workflows. | W7 | https://raw.githubusercontent.com/anthropics/claude-code/main/CHANGELOG.md | CL:6186 | ## 2.1.154 |
| 125 | (same sentence) | W7 | https://raw.githubusercontent.com/anthropics/claude-code/main/CHANGELOG.md | CL:6189 | - Introducing dynamic workflows: ask Claude to create a workflow |
| 126 | Workflows are available on all paid plans. | W6 | https://code.claude.com/docs/en/workflows | WF:11 | Dynamic workflows are available on all paid plans |
| 126 | On Pro, open `/config` and turn on the Dynamic workflows row. | W6 | https://code.claude.com/docs/en/workflows | WF:11 | On Pro, turn them on from the Dynamic workflows row in `/config`. |
| 127 | An organization admin can switch workflows off for the whole organization. | W6 | https://code.claude.com/docs/en/workflows | WF:490 | To turn workflows off for your whole organization |
| 133 | ask Claude in your own words for a workflow that runs one stage of your flow | W2 | https://code.claude.com/docs/en/workflows | WF:115 | Asking in your own words, for example "use a workflow" or "run a workflow", also works |
| 133 | Claude treats a direct request as the opt-in. | W2 | https://code.claude.com/docs/en/workflows | WF:115 | Claude treats a direct request as the same opt-in. |
| 134 | In default (manual) and acceptEdits mode, Claude Code asks you to approve every run | W12 | https://code.claude.com/docs/en/workflows | WF:176 | \| Manual, accept edits \| Every run, unless you've selected **Yes, and don't ask again** for that workflow in this project \| |
| 134 | (same sentence) | W12 | https://code.claude.com/docs/en/workflows | WF:171 | Whether you see this prompt depends on your |
| 134 | default (manual) and acceptEdits | S15 | https://code.claude.com/docs/en/sub-agents | SA:590 | so Manual mode is `default` |
| 134 | (same sentence) | S15 | https://code.claude.com/docs/en/sub-agents | SA:303 | `default`, `acceptEdits`, `auto`, `dontAsk`, `bypassPermissions`, `plan`, or `manual` as an alias for `default` |
| 134 | the prompt has a **View raw script** option | W12 | https://code.claude.com/docs/en/workflows | WF:166 | * **View raw script**: read the script before deciding |
| 134 | In auto mode it asks only at the first launch | W12 | https://code.claude.com/docs/en/workflows | WF:175 | \| Auto \| First launch only. Any **Yes** records consent in your user settings, and later launches start without prompting. |
| 134 | and in bypass mode never | W12 | https://code.claude.com/docs/en/workflows | WF:177 | \| Bypass permissions \| Claude Code doesn't prompt you. The run starts immediately \| |
| 134 | So before step 1, switch from auto to default or acceptEdits mode, as in Level 3. | (advice) | Level 3 page | n/a | instruction, follows from the three mode rows above |
| 135 | Watch the run with `/workflows`. | W3 | https://code.claude.com/docs/en/workflows | WF:52 | Run `/workflows` to open its progress view: |
| 135 | (same sentence) | W3 | https://code.claude.com/docs/en/commands | CMD:172 | progress view to watch, pause, resume, or save running and completed workflows |
| 136 | A workflow has no mid-run user input: it pauses on its own only for permission prompts and a usage-limit wait. | W8 | https://code.claude.com/docs/en/workflows | WF:352 | \| No mid-run user input \| A run pauses on its own only for agent permission prompts and a |
| 136 | run each stage as its own workflow, read the result, and only then start the next one. | W8 | https://code.claude.com/docs/en/workflows | WF:352 | For sign-off between stages, run each stage as its own workflow |
| 137 | A run starts many agents, and runs count toward your plan's usage and rate limits. | W13 | https://code.claude.com/docs/en/workflows | WF:441 | A workflow spawns many agents, so a single run can use meaningfully more tokens than working through the same task in conversation. Runs count toward your plan's usage and rate limits. |
| 137 | Start with a small slice of the feature. | W13 | https://code.claude.com/docs/en/workflows | WF:443 | run the workflow on a small slice first |
| 138 | save it from the `/workflows` view | W3 | https://code.claude.com/docs/en/workflows | WF:98 | \| `s` \| [Save](#save-the-workflow-for-reuse) the run's script as a command \| |
| 138 | (same sentence) | W3 | https://code.claude.com/docs/en/commands | CMD:172 | progress view to watch, pause, resume, or save running and completed workflows |
| 138 | in the project location | W4 | https://code.claude.com/docs/en/workflows | WF:198 | `.claude/workflows/` in your project: shared with everyone who clones the repo |
| 138 | run it again later as `/<name>`. | W3 | https://code.claude.com/docs/en/workflows | WF:201 | The workflow runs as `/<name>` in future sessions from either location. |
| 140 | Start every workflow from the main session: a subagent cannot start one. | S6 | https://code.claude.com/docs/en/sub-agents | SA:429 | The first filter removes these tools, even when listed in the `tools` field: |
| 140 | (same sentence) | S6 | https://code.claude.com/docs/en/sub-agents | SA:438 | * `Workflow` |

Judgement notes for the verifier: row 29/126 "available on all paid plans" is W6 verbatim; the admin sentence (127) cites the managed-settings and admin-settings sentence, which names the organization; the first half of line 140 ("start from the main session") rests on W1 (the session stays responsive) and the S6 removal of `Workflow` from subagents, it is the converse of a banned claim, not the banned claim.
