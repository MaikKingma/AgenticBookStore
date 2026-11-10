---
agent: res-3
task: res-3
status: completed
spec: docs/spec-pack/specs/11-workshop-guide-site.md (note: the file is named 11-workshop-guide-site.md, not 11-workshop-guide.md)
updated: 2026-11-08T20:20:00Z
---

# Research: guide content inventory and post-spec-10 gaps

Abbreviations (all under `docs/spec-pack/`): WP = `workshop/workshop-plan.md`, SDD = `workshop/sdd-framework-guide.md`,
FB = `workshop/level-4-feature-briefs.md`, RM = `product/roadmap.md`, AS = `product/architecture-standard.md`,
S09 / S10 / S11 = `specs/09-workshop-baseline.md` / `10-harness-reference-solutions.md` / `11-workshop-guide-site.md`,
D = `orchestrator/decisions.md`, PG = `orchestrator/progress.md`. All line numbers read on branch
`sdd/11-workshop-guide/spec` (HEAD 54c346e). Everything below is verified by reading unless marked "unverified".

## TL;DR

The pack states almost everything the guide needs: WP sections 3 to 9 hold every step, command and "Level cleared when" bullet;
SDD Part 2 holds the three tracks; FB holds the briefs. There is NO participant email anywhere in the pack (WP:45 only
refers to it), so the setup page prerequisites must come from WP:45-47, WP:59-66 and S09:38-39. Pack-stated catch-up
branches are ambiguous (WP gives `solution/level-N` as "catch-up / compare" at level N, RM:61 and WP:347 use it to
start the next level), and Level 0 has no catch-up in the pack beyond "`main` (tag `level-0-start`)". Content that only exists
after specs 09 and 10 splits cleanly: file texts of `solution/level-1..3` and the bash hook scripts are quotable from S10
(line numbers below), but the PowerShell hook scripts, the real `docs/lsp-setup.md`, the ACL contract test, the
`start/level-4-*` contents and the Windows hook instructions are NOT stated anywhere and need a post-spec-10 follow-up.
Anthropic Academy and next-step URLs are verified by fetch (section 11). The Windows story is internally unresolved
(WSL vs native with `.ps1`, see Contradictions C1).

## 1. Cross-cutting facts

### 1.1 Header box per level (S11:49-51)
Time comes from the agenda (WP:29-34). Branches per WP/RM/D. The pack never says "Start branch" literally; it says "Start:".

| Level | Goal (verbatim) | Start branch (WP) | Catch-up command (pack) | Time (min) and split |
|---|---|---|---|---|
| 0 | "experience the failure modes on a codebase where the agent knows nothing about our conventions." WP:87 | `main` (tag `level-0-start`) WP:89, RM:52 | none stated. Implied by RM:52 and S09:62-66: `git stash -u && git switch main` (S09 moves tag `level-0-start` with `main`, so `main` and the tag are the same commit). GAP: pack never says it; suggest `main`, mention tag as alternative | 25: sync 3, solo 19, debrief 3 (WP:29) |
| 1 | "give the agent the smallest set of high-signal context that makes the right thing the obvious thing." WP:130 | `main` again (clean) WP:132 | to start the level: `git stash -u && git switch main` (RM:52, implied). Compare after: `solution/level-1` (WP:132) | 35: sync 5, solo 27, debrief 3 (WP:30) |
| 2 | "let the agent see what you see: the code through a language server, and real external data through our own MCP server, instead of guessing." WP:177-178 | "your Level 1 result, or `solution/level-1`" WP:180 | `git stash -u && git switch solution/level-1` to start; compare after: `solution/level-2` (WP:180) | 20: sync 3, solo 15 (LSP 8, MCP 7), debrief 2 (WP:32) |
| 3 | "turn the things that must happen every time into guarantees, and package repeatable work." WP:228 | "your Level 2 result, or `solution/level-2`" WP:230 | `git stash -u && git switch solution/level-2` to start; compare: `solution/level-3` (WP:230) | 30: sync 4, solo 24 (permissions 4, hooks 11, skill and command 9), debrief 2 (WP:33) |
| 4 | "turn a vague request into a spec, a plan and tasks, then let the agent implement and verify it inside the harness you built." WP:297-298 | the track branch `start/level-4-openspec|speckit|agentos`, built from `solution/level-3` (WP:301-307, SDD:93-100) | same command as start: `git stash -u && git switch start/level-4-<track>` (SDD:94-96) | 45: sync 5, solo 37, debrief 3 (WP:34); tracks "about 35 minutes" each (SDD:104,117,129) |

Ambiguity to resolve in the content tasks (A1): WP:132/180/230 write "Catch-up / compare: `solution/level-N`" under level N, which
is the END state of level N. RM:61 and WP:347 ("at the start of each level `git switch solution/level-N`") use the
previous level's solution to start the next. The header box "Catch-up" row should name the branch you would switch to if
behind at the START of this level, i.e. `solution/level-(N-1)`, and a separate "Compare with" line the end-state branch. Level 1's
own plan line would otherwise hand participants the answer before they start. This is a decision for the orchestrator, not
stated by the pack.

### 1.2 Sync moments (render as `:::info Sync moment`, "Look up, Maik demos this now")
| Level | Min | Text from the plan (WP) |
|---|---|---|
| 0 | 3 | WP:92-93: Maik opens the repo, shows the onion diagram and the `ArchitectureTest`, then gives the task to Claude Code live without any context; the room sees the first minute. |
| 1 | 5 | WP:134-135: run `/init` live, read the generated `CLAUDE.md` together, cross out everything Claude could have read from the code itself; show `/context` and `/memory`. |
| 2 | 3 | WP:182-184: Maik shows the LSP install check (`which jdtls`, `/plugin install jdtls-lsp@claude-plugins-official`, the **Installed** tab of `/plugin`), then `claude mcp add` for the distributor and one MCP question in the chat. |
| 3 | 4 | WP:232-233: live demo of one hook blocking a commit (red tests, exit code 2, the agent reads the stderr and fixes the test). |
| 4 | 5 | WP:314-315: one spec artefact per framework side by side (OpenSpec delta spec, Spec Kit `spec.md`, Agent OS `shape.md`) so everyone sees the same idea in three shapes. |
Setup check, keynote (45 min WP:28) and break (WP:171-173) have no sync moment. Debrief texts (room activity, participant-useful as reflection prompts, optional): WP:121-122, 168-169, 223-224, 292-293, 323-324.

### 1.3 Participant email and prerequisites (task item 10)
- The participant email is NOT in the pack. Searched all pack files, CLAUDE.md, the product folder: the only references are WP:45 ("see the participant email") and S11:28 ("mirrors the participant email"). It must be created or the page should state the pack-stated list (below) and the follow-up noted.
- Prerequisites the pack states: Claude Code; a Claude Pro/Max/Team/Enterprise account (WP:51); Temurin 25 (WP:45, check `java -version` -> 25 WP:52); git; Docker Desktop or another Docker engine, installed and running (WP:45-46, 53); `jdtls` binary on the `PATH` (WP:46, 59-66); no Node.js needed, the build installs its own (WP:46-47); do `./mvnw verify` once at home so dependencies and the PostgreSQL image are cached (WP:56, 338); Windows: WSL (jdtls WP:66, 190) or Git for Windows / WSL2 for bash hooks (WP:344); admin rights or a personal laptop (WP:346, email text but participant-useful); Java 25 and Docker per S09:38-39; the workshop API key comes on the day (WP:18, 71-76) and is revoked afterwards (WP:332).
- Gap: OpenSpec CLI is a Track A need (`openspec validate --strict`, SDD:107) and is not in any prerequisite list. PG:94 records this as deliberately left. S10:426-428 only runs `openspec init`; the pack never says the `openspec` binary is on `PATH` on `start/level-4-openspec`. Ask: is it installed by the branch or by the participant (install lines exist only in SDD:149).
- Gap: the model-key: Level steps never use `BOOKSTORE_AI_API_KEY` for any command beyond the setup export (WP:71-76); S09:44 says offline mode exists. Page can say "needed for the assistant, not for the levels" only if the orchestrator confirms; pack does not state it.

## 2. Page-by-page inventory

### 2.1 `index.md` (welcome, agenda, five levels, how to use)
- Sources: WP:3 (event, 4 hours, beginner, English), WP:13 (levels match J-Spring talk, "Level cleared when"), WP:15 (guide plus live sync moments, catch-up branches), agenda table WP:25-35 (total 240 min, WP:37), buffer note WP:39-41 (rewrite as "skip stretch goals if behind"; drop "keep Level 4 intact" facilitator reasoning or keep as advice), the five level names D:41-48, `LevelProgress` S11:41,49, how to use: sync moments, checkpoints, catch-up rule RM:61 / WP:347, take-home S11:6-9.
- Content: welcome; agenda (include Keynote and Break rows, not the Break's facilitator line WP:173); the five levels with one line each (Goals, section 1.1); how to use (sync moments, checkpoints, "Level cleared when", catch-up command, `notes/level-0.md`, pages are self-paced, key shown on screen).
- Exclude: WP:5-6 (generated-from note), WP:8-19 format rationale table (optional: rows "Harness", "Code intelligence", "MCP", "LLM key", "SDD" can be paraphrased into one line each; "Why" column is facilitator reasoning).

### 2.2 `00-setup-check.md`
Items to include (all verbatim from WP:49-79):
| Source | Item | Page location |
|---|---|---|
| WP:50 | `claude --version` | setup commands |
| WP:51 | `claude` then `/status` (logged in with Pro/Max/Team/Enterprise) | setup commands |
| WP:52 | `java -version` (25) | setup commands |
| WP:53 | `docker info` | setup commands |
| WP:54 | `which jdtls` (Windows PowerShell: `Get-Command jdtls`) | OS tabs |
| WP:55 | `git clone https://github.com/MaikKingma/AgenticBookStore.git && cd AgenticBookStore` | setup commands |
| WP:56 | `./mvnw verify` green, under a minute | checkpoint |
| WP:59-66 | jdtls install per OS (macOS `brew install jdtls`; Arch `yay -S jdtls`; other Linux: extract a JDT LS release and put a `jdtls` wrapper script on the `PATH`; Windows: WSL and install as on Linux there, "Native Windows is not verified yet") | jdtls tabs (S11:61-63) |
| WP:68-69 | setup covers the binary only; plugin is installed in Level 2 Part A | note |
| WP:71-76 | `export BOOKSTORE_AI_API_KEY=<key on screen>` / `$env:BOOKSTORE_AI_API_KEY="<key on screen>"` | OS tabs |
| WP:78-79 | `/model sonnet` inside Claude Code, reason (5-hour shared window, Opus drains faster) | step |
| WP:341, 18 | only `BOOKSTORE_AI_API_KEY`, never `ANTHROPIC_API_KEY` | warning, link to troubleshooting |
Exclude: WP:81-83 fallback kit (facilitator). Gap: `git clone ... && cd` has no PowerShell variant; `&&` is valid in PowerShell 7 only (unverified here). Windows PowerShell form not stated by the pack.

### 2.3 `01-level-0-break-the-agent.mdx` (sources WP:85-126)
Items (all must appear):
| Source | Item |
|---|---|
| WP:89-90 | Start state: no `CLAUDE.md`, no `.claude/`, no MCP; React storefront in `frontend/` |
| WP:92-93 | Sync moment |
| WP:95-98 | Task, paste literally: "Add discounts to the bookstore. A book can get a discount percentage. Show the discounted price when people search the catalogue." |
| WP:101-102 | Step 1: `claude`; sessions start in auto permission mode (unverified here, Claude Code behaviour claim); leave it |
| WP:103 | Step 2: paste task, do not help, answer only "you decide" |
| WP:104-110 | Step 3: `./mvnw verify`, `git status`, `git diff --stat`, six check questions (Flyway migration, `double` for money, where the rule lives, tests written and run, unverified claims, files outside the catalogue) |
| WP:111-113 | Step 4: observation card |
| WP:114-116 | Step 5: `git restore . && git clean -fd`, or `Esc` twice / `/rewind`; Bash changes not tracked by checkpoints, so git is the safety net |
| WP:117-119 | Level cleared when (see 1.4) |
Exclude: WP:124-126 expected findings; WP:121-122 debrief may be kept as "three columns" explanation because the card uses the same columns (this is the participant-useful part).
Observation card (S11:57-60, WP:111-113, D:70-71): table with columns Failure mode | Evidence (the failing test, the ArchUnit output or the wrong file) | Column (didn't know, didn't care, didn't check), at least three rows, plus one line "What went right". Participants copy it into `notes/level-0.md`. Column meaning (WP:121-122): didn't know = context, didn't care = behaviour, didn't check = verification. Level 1 step 6 refers to "your observation card" (WP:160). Git-ignored: factory `.gitignore:31-32` has `notes/`. Spec 09 does NOT mention `.gitignore` or `notes/` anywhere (grep empty); publish script S09:55-61 removes only listed paths and squashes `factory`, so `main` inherits `.gitignore` as is. The pack never states it, so a follow-up check after spec 09 ("main `.gitignore` contains `notes/`") is needed. Also `CLAUDE.local.md` is ignored at `.gitignore:29`.
Windows variants: `git restore . && git clean -fd` (gap, see above). Checkpoints: none have pack-stated outputs (gap).

### 2.4 `02-level-1-context-engineering.mdx` (WP:128-169)
| Source | Item |
|---|---|
| WP:134-135 | Sync moment |
| WP:138-139 | Step 1: `/init`; read critically; the most useful line is the command that runs the tests (Anthropic guidance quote, WP:138-139) |
| WP:140-147 | Step 2: rewrite `CLAUDE.md` to at most 60 lines containing: build and test commands (`./mvnw verify`, `./mvnw -pl bookstore test -Dtest=BookTest`); definition of done (verify green including `ArchitectureTest`, never say "tests pass" without output); five rules (domain pure Java, money is `Money` BigDecimal never double, schema changes only via a new Flyway migration, inbound adapters only talk to flows, no Lombok); a path instead of an essay "Read docs/architecture.md before adding or moving classes", compare with import syntax `@docs/architecture.md` (import loads at session start, path loads on demand), try both and look at `/context` |
| WP:148-151 | Step 3: `.claude/rules/domain.md` `paths: ["bookstore/src/main/java/**/domain/**"]`; `data-and-migrations.md` `paths: ["bookstore/src/main/java/**/data/**", "bookstore/src/main/resources/db/**"]`; `testing.md` `paths: ["**/src/test/**"]` |
| WP:152-155 | Step 4: add conflicting line "Always use Lombok" to `~/.claude/CLAUDE.md`; create `CLAUDE.local.md` (e.g. preferred commit style); new session; `/memory`, `/context`; files concatenated not overridden; remove the Lombok line afterwards |
| WP:156-159 | Step 5: `/context`; `/clear` between unrelated tasks; `/compact focus on <topic>`; delegate big reads to a subagent ("use a subagent to find every place that formats money"); 1M-token window not an attention guarantee |
| WP:160 | Step 6: redo Level 0 task in a fresh session (`/clear`), compare with the card |
| WP:162-166 | Level cleared when |
Checkpoints stated by the pack: `/context` shows path-scoped rules loading only for matching files (WP:164); S10:113-115 spec scenario: after a question not touching `domain`, `CLAUDE.md` is loaded and neither `docs/architecture.md` nor `.claude/rules/domain.md`; after an edit in `domain` the rule is loaded. `wc -l CLAUDE.md` is NOT in the pack (authoring decision; not an invented output, but an invented command). Verbatim solution text exists in S10:25-109 (see post-spec-10 gaps). Windows: `~/.claude/CLAUDE.md` path form on Windows not stated (gap).

### 2.5 `03-level-2-give-your-agent-eyes.mdx` (WP:175-224)
| Source | Item |
|---|---|
| WP:182-184 | Sync moment |
| WP:188-189 | Part A intro: built-in read-only LSP tool; with a plugin gives go to definition, find references, type info, symbols, implementations, call hierarchy; diagnostics after every edit |
| WP:190 | A1: `which jdtls` (Windows: inside WSL), install as in setup check if missing |
| WP:191-194 | A2: `/plugin install jdtls-lsp@claude-plugins-official`; if marketplace missing first `/plugin marketplace add anthropics/claude-plugins-official`; choose scope (user keeps it out of repo, project writes `.claude/settings.json`, as on `solution/level-2`); confirm on the **Installed** tab of `/plugin` |
| WP:195-196 | A3: "Find all references to `Money.of`." answer from the language server, spans domain, flows and mappers, tests |
| WP:197-199 | A4: "In `Book.java`, make `price()` return `String` instead of `Money`. Do not touch the callers yet." Claude Code prints `Found N new diagnostic issues ...`; then "Fix the new diagnostics." |
| WP:200-202 | A5: nothing appears: `/plugin` tab **Errors**, `claude --debug`; `Executable not found in $PATH: "jdtls"` means binary missing; stuck after two minutes: watch the demo, continue with Part B |
| WP:205 | B1: `./mvnw -pl distributor spring-boot:run` (or `java -jar distributor.jar`) |
| WP:206-208 | B2: `claude mcp add --transport http --scope project distributor http://localhost:8090/mcp` writes `.mcp.json`; restart Claude Code if needed; `/mcp` |
| WP:209-210 | B3: "Which science-fiction titles does the distributor have that we do not sell yet?"; tools show as `mcp__distributor__*` |
| WP:211-213 | B4: "Using the distributor MCP, find a title that has a translator or illustrator among its contributors. Write an ACL contract test for `DistributorAppServiceImpl` with that exact payload shape and prove that only authors end up in our catalogue." (existing tests deliberately do not cover it) |
| WP:214 | B5: `distributor/src/main/java/.../mcp/CatalogMcpTools.java` (real path below) |
| WP:216 | Stretch: MCP tool `list_new_releases(days)` |
| WP:218-221 | Level cleared when |
Checkpoints from pack: `Found N new diagnostic issues ...` (WP:198, S10:174); `/mcp` shows distributor connected (WP:208, 219); S10:209: "connected with 4 tools" (verified on this branch: `search_titles`, `get_title`, `list_subjects`, `check_availability`, `CatalogMcpTools.java:38,66,81,91`); `mcp__distributor__*` (WP:210); references span at least two layers (S10:197). Real path (verified exists): `distributor/src/main/java/nl/openvalue/distributor/mcp/CatalogMcpTools.java`. OS: `which jdtls` vs `Get-Command jdtls` (WP:54 pack states); `java -jar distributor.jar` path not OS-specific. Port 8090 stated in WP:207 and S10:36. Potential conflict (A2): on `solution/level-2`, `.mcp.json` already exists (S10:128-138, uses `${DISTRIBUTOR_MCP_URL:-http://localhost:8090/mcp}`); the pack does not say what `claude mcp add` does there. Matters for participants who catch up; not for those who follow from `main`.

### 2.6 `04-level-3-behavioral-control.mdx` (WP:226-293)
| Source | Item |
|---|---|
| WP:232-233 | Sync moment |
| WP:236-245 | Part A: create or extend `.claude/settings.json` with the permissions JSON (allow: `Bash(./mvnw *)`, `Bash(git status *)`, `Bash(git diff *)`, `Bash(git add *)`, `Bash(git commit *)`; deny: `Read(./.env)`, `Read(./.env.*)`, `Bash(git push)`, `Bash(git push *)`, `Bash(* --no-verify*)`, `Bash(git commit -n *)`). Same JSON in S10:227-243. |
| WP:246-247 | Rules evaluated deny, then ask, then allow; `Shift+Tab` cycles modes (from auto: default, acceptEdits, plan); `/plan` read-only planning |
| WP:250-259 | Part B1: PreToolUse hook JSON with `matcher: Bash`, `if: Bash(git commit *)`, `timeout: 180`, `command: "\"$CLAUDE_PROJECT_DIR\"/.claude/hooks/tests-before-commit.sh"` |
| WP:260-262 | B2: create `.claude/hooks/tests-before-commit.sh`, `chmod +x`; runs `./mvnw -q verify`; on failure last 40 lines to stderr, exit 2 blocks and feeds reason back; any other non-zero does NOT block |
| WP:263-264 | B3: `git config core.hooksPath .githooks` (`.githooks/pre-commit` ships in the solution branch; write your own first) |
| WP:265 | B4: ask Claude to "change Money to round HALF_UP on purpose and commit" |
| WP:266-268 | B5: discussion why both hooks; `--no-verify` / `-n` skips the git hook, deny rules close it |
| WP:270-271 | Optional Stop hook: runs only fast `ArchitectureTest` when Java files changed, checks `stop_hook_active` |
| WP:274-278 | Part C1: `.claude/skills/new-use-case/SKILL.md` (layers: domain behaviour, flow method, port change, adapter, migration if needed, tests per layer, verify); frontmatter `name`, precise `description`, `allowed-tools`; supporting `reference.md` |
| WP:278 | C2: "Customers must be able to cancel an order that has not shipped yet." |
| WP:279-282 | C3: `.claude/commands/ship.md` with `disable-model-invocation: true`: shows `!`git status --short``, runs verify, proposes conventional commit message for `$ARGUMENTS`, commits; custom commands and skills same mechanism, both create `/ship` |
| WP:284-285 | Stretch: `.claude/agents/architecture-reviewer.md` (`tools: Read, Glob, Grep, Bash`, Bash only for `git diff`, `git status`) |
| WP:287-290 | Level cleared when |
Hook script tabs: bash text is quotable from S10:273-295 (tests-before-commit.sh) and S10:297-314 (architecture-check-on-stop.sh) but those exist only after spec 10; PowerShell variants are named only (S10:316-318), content NOT stated: gap. Checkpoints from pack: red commit blocked, hook exits 2, Claude's next message refers to the failing test from stderr (S10:402-404; WP:288); `git commit --no-verify -m "x"`, `git commit -n -m "x"`, `git push` are denied (S10:407-408); `.env` read denied (S10:410-413); skill loaded without being told, visible in transcript (S10:415-417). Cancel-order shape: S10:395-398 (202; 404; 409). Note the S10 `solution/level-3` settings include a Stop hook (S10:258-268) while WP marks it optional (WP:270): the guide must say the solution branch has it but the participant exercise does not require it.

### 2.7 `05-level-4-spec-driven-development.mdx` (WP:295-323, SDD:91-142, FB)
Page structure: start + track choice table WP:303-307 (Track, Branch, Weight, Commands), common start SDD:93-97 (`git stash -u`, `git switch start/level-4-<track>`, `claude`, `/model sonnet`), SDD:99-102 branch content and "write the brief into the first command in your own words and add the answers: your contribution as the human", sync moment WP:314-315, tabs per track, briefs section, Level cleared when (identical in WP:317-321 and SDD:138-142).
Track A OpenSpec (SDD:104-115): 1 `/opsx:explore` "reviews and ratings" (or your brief); 2 `/opsx:propose add-book-reviews` with brief and decisions; 3 terminal `openspec validate add-book-reviews --strict`, fix what it reports, common error: scenario heading with three `#` instead of four is silently ignored; 4 read `openspec/changes/add-book-reviews/specs/.../spec.md`, change with `/opsx:update`; 5 `/clear`, `/opsx:apply`, watch tasks tick off; 6 `/ship add book reviews`, the test-gate hook runs; 7 `/opsx:archive` (into `openspec/specs/`). Gotchas SDD:114-115: `openspec ...` in terminal, `/opsx:...` in Claude Code; restart Claude Code if commands do not show up.
Track B Spec Kit (SDD:117-127): 1 `/speckit-specify` brief and decisions, what and why not tech stack; 2 `/speckit-clarify` up to five questions; 3 `/speckit-plan` "Use the existing onion layers, see docs/architecture.md and the constitution."; 4 `/speckit-tasks`; 5 `/speckit-implement`, then `/speckit-converge` once; 6 `/ship add book reviews`. Skip `/speckit-checklist` and `/speckit-analyze` today (SDD:124). Gotchas SDD:126-127: dash names in Claude Code; active feature stored in `.specify/feature.json`, not taken from the git branch.
Track C Agent OS (SDD:129-137): 1 look at `agent-os/standards/` first, fix anything wrong; 2 `/plan` (or `Shift+Tab` until plan mode), then `/shape-spec` with brief and decisions; 3 answer questions, let it save the spec folder in `agent-os/specs/`; 4 approve the plan, implement; `/inject-standards testing` if tests drift; 5 `/ship add book reviews`. Gotchas SDD:136: `/shape-spec` refuses outside plan mode; Agent OS needs bash (Windows: WSL or Git Bash).
Per-track name placeholder: `add-book-reviews` and "add book reviews" are Brief A specific; Brief B and C names are not given (gap; guide should say "use your own change name").
Briefs (FB:6-36), include verbatim the three quotes and "Questions your spec should answer"; Brief C two routes (FB:29-36) including the rule that route 2 breaks two rules and requires an architecture change (update `docs/architecture.md`, `CLAUDE.md`, the ArchUnit rule). Brief headers: A "recommended for most people" FB:6; C "(advanced)" FB:22. Track and brief fit (FB:41-43: A fits every track in 35 min; B tight for Spec Kit; C for early finishers) is under "Facilitator notes" but is participant-useful guidance; the bullets at FB:43-45 (route 2 debrief story, "good specs for A name verified buyer", "ask every participant to show the mapping") are facilitator-only, except the mapping step, which is already a Level cleared when bullet. Decision for orchestrator: include FB:41 (time fit) as a hint, exclude the rest.
Optional take-home table (SDD:146-154): install and init per framework, with Windows variants (`irm https://openspec.dev/cli/install.ps1 | iex`, `--script ps`). Fits wrap-up or end of Level 4 "At home". Exclude SDD:7-8, 10-38 (versions, comparison; WP:303-307 weights suffice), 42-87 (Part 1, for Maik), 153-154 sources (can be kept as links).
Level 4 solution branches `solution/level-4-<track>-<brief>` are "facilitator reference, optional, wrap-up demo" (RM:59): not participant content.

### 2.8 `06-wrap-up.md` (WP:325-332, S11:34)
- Who cleared which level: show of hands Level 0 to Level 4 (WP:326), the page can list "Level cleared when" links.
- Two or three show their harness folder `tree .claude` and spec (WP:327). `tree` on Windows PowerShell: not stated (gap).
- Recap map back to the keynote: context, behaviour, structure, verification (WP:328). Mapping to levels is implied by WP:13 and the debrief columns (didn't know = context, didn't care = behaviour, didn't check = verification, WP:121-122); the pack does not state "structure" belongs to a specific level (Level 4 by implication).
- Take-home (WP:329-331): the guide stays online; Academy courses; next steps OpenCode and Claude Agent SDK.
- Remind: the workshop API key is revoked tonight (WP:332): participant-useful, include.
- Academy and next-step links, fetched 2026-11-08 (all verified, page titles match):
  - Claude Code 101: https://anthropic.skilljar.com/claude-code-101
  - Claude Code in Action: https://anthropic.skilljar.com/claude-code-in-action
  - Introduction to agent skills: https://anthropic.skilljar.com/introduction-to-agent-skills
  - Introduction to subagents: https://anthropic.skilljar.com/introduction-to-subagents
  - Introduction to Model Context Protocol: https://anthropic.skilljar.com/introduction-to-model-context-protocol (WP:329 says "Introduction to MCP"; the course title is "Introduction to Model Context Protocol")
  - Catalogue: https://anthropic.skilljar.com/ (lists all five)
  - OpenCode: https://opencode.ai/ (title "OpenCode | The open source coding agent for any model"). Unverified for the project's intent: the pack names only "OpenCode", no URL.
  - Claude Agent SDK overview: https://code.claude.com/docs/en/agent-sdk/overview (fetched OK, title "Agent SDK overview"). The older `docs.claude.com/en/api/agent-sdk/overview` redirects 301 to platform.claude.com then 307 to this URL; use the final one. The overview says Python and TypeScript SDKs and carries a note that third parties may not offer claude.ai login for agents built on the SDK (relevant if the guide suggests building a harness: use API keys).
  - anthropic.com/learn was not fetched; not needed.

### 2.9 `reference/cheat-sheet.md`
Commands and files with source line (first and main occurrence):
| Item | Source |
|---|---|
| `claude`, `claude --version` | WP:50-51 |
| `/status` | WP:51 |
| `/model sonnet` | WP:78 |
| `/usage` | WP:340 (risk row; participant-useful) |
| `/init` | WP:138 |
| `/context` | WP:135, 156 |
| `/memory` | WP:135, 154 |
| `/clear` | WP:157, 160 |
| `/compact focus on <topic>` | WP:157 |
| `/rewind`, `Esc` twice | WP:114 |
| `/plugin`, tabs Installed and Errors | WP:182, 194, 200 |
| `/plugin install jdtls-lsp@claude-plugins-official` | WP:191 |
| `/plugin marketplace add anthropics/claude-plugins-official` | WP:192 |
| `/mcp` | WP:208 |
| `claude mcp add --transport http --scope project distributor http://localhost:8090/mcp` | WP:207 |
| `claude --debug` | WP:200 |
| `Shift+Tab` (modes default, acceptEdits, plan) | WP:246-247 |
| `/plan` | WP:247, SDD:131 |
| `/ship <what changes>`, `$ARGUMENTS`, `!`cmd`` | WP:280-282, S10:361-378 |
| `/opsx:explore|propose|update|apply|archive`, `openspec validate --strict` | WP:305, SDD:105-113 |
| `/speckit-specify|clarify|plan|tasks|implement|converge` | WP:306, SDD:118-123 |
| `/shape-spec`, `/inject-standards` | WP:307, SDD:131-133 |
| `CLAUDE.md` | WP:140 |
| `~/.claude/CLAUDE.md` | WP:152 |
| `CLAUDE.local.md` (git-ignored; example file `CLAUDE.local.md.example` S10:104) | WP:153 |
| `.claude/rules/*.md` with `paths:` frontmatter | WP:148-151 |
| `.claude/settings.json` (permissions, hooks, plugin declaration) | WP:237, S10:140-152 |
| `.claude/skills/<name>/SKILL.md` and `reference.md` | WP:274-276 |
| `.claude/commands/ship.md` | WP:279 |
| `.claude/agents/architecture-reviewer.md` | WP:284 |
| `.claude/hooks/tests-before-commit.sh` | WP:260 |
| `.mcp.json` | WP:208 |
| `.githooks/pre-commit`, `git config core.hooksPath .githooks` | WP:263 |
| hook exit code 2 blocks, other non-zero does not; `$CLAUDE_PROJECT_DIR`; `stop_hook_active` | WP:261-262, 256, 271 |
| permission rule order deny, ask, allow | WP:246 |
| `docs/architecture.md` | WP:146 |
Note: `/usage` and `claude --debug` are in the brief list and both are in WP. Printable one page: Level 4 slash commands are a second table.

### 2.10 `reference/architecture.md` (AS)
Participant-relevant, at most one page:
- The onion diagram AS:19-30 and the package table AS:32-41 (role and typical classes).
- Dependency rules AS:45-55 (all nine, including the documented exception AS:53-55: `process` may reference `..domain..*Event`).
- Naming suffix table AS:65-73 (this is what ArchUnit enforces for Level 0 findings) plus the AS:59-63 sentence on top-level classes.
- ArchUnit referee: `ArchitectureTest` AS:158; all rules "enforced by ArchUnit, never by convention alone" AS:43.
- Testing table AS:149-158 compressed to one sentence per layer is optional; keep the "never weaken or delete an ArchitectureTest rule" idea (S10:101, CLAUDE.md of factory). Money rule AS:90-91, no Lombok/MapStruct AS:171-172 are Level 1's "five rules".
- Link to `docs/architecture.md` on `main` for the full team document (S09:47-48).
Exclude: AS:1-5 status header and framework usage; AS:7-9 provenance; building-block detail AS:75-146 (flows, DTOs, exceptions, adapters): too long, available in `docs/architecture.md`; AS:160-167 verify budget details (60 second budget shown in setup checkpoint only).
Contradictions with WP: none material. Small points: WP:144 "inbound adapters only talk to flows" matches AS:45-47 but AS:53 exception (process reads events) is not in WP; AS:117 says "extend one of the use-case exceptions below" but the exceptions are listed above (typo, not for the guide); AS:14 "exactly these top-level packages" is not enforced by ArchUnit (PG:33), so say "by convention and rules" rather than claiming an enforced list; AS:160 "must stay under 60 seconds" vs PG:26 (OQ4, frontend build may push past): the setup checkpoint "under a minute" (WP:56) may not hold after spec 12; PG:7 measured 24 s after spec 05 (before spec 12).

### 2.11 `reference/troubleshooting.md` (S11:38)
Participant-useful risk rows (WP:334-347) rewritten without facilitator notes:
| Topic | Source | Participant version (facts only from the pack) | Gap |
|---|---|---|---|
| Windows hooks | WP:344, S10:316-318 | Claude Code runs natively; hook scripts are bash; use Git for Windows (Git Bash) or WSL2; `.ps1` variants exist on `solution/level-3`; Agent OS needs bash | How to point the hook command at the `.ps1` is not stated (S10:317 says write it and verify which shell Claude Code uses on Windows); follow-up |
| jdtls not starting | WP:200-202, 343, S10:175-176, WP:66 | check `which jdtls` / `Get-Command jdtls`; `/plugin` tab **Errors**; `claude --debug`; `Executable not found in $PATH: "jdtls"` = binary missing; levels 0 and 1 run without the plugin; fallback is to watch the demo and continue with Part B; Native Windows not verified yet | slow first index time and multi-module behaviour are rehearsed in spec 10 (WP:343), results unknown until then |
| usage limits | WP:78-79, 340 | `/model sonnet`; `/usage` to check; Pro limits shared over a 5-hour window; Opus drains faster; pair up if someone hits the limit | none |
| MCP not connecting | WP:205-208, 345, S10:208-209 | start the distributor first (`./mvnw -pl distributor spring-boot:run`, port 8090); `/mcp`; restart Claude Code; approve the project MCP server (S10:208); fallback STDIO: `./mvnw -pl distributor package`, then `claude mcp add distributor -- java -jar <absolute path>/distributor/target/distributor.jar --spring.profiles.active=stdio` | drop the "MCP spec 2026-07-28 vs Spring AI 2.0.2" jargon (facilitator) |
| `ANTHROPIC_API_KEY` | WP:341, 18, S09:44 | Claude Code would switch to that key; only ever use `BOOKSTORE_AI_API_KEY`; `/status` should show the subscription | the command to unset it is not in the pack (do not invent) |
Optional extra rows participants can use: Docker not running (`docker info`, WP:339, 53); far behind (`git stash -u && git switch solution/level-N`, WP:347); slow wifi (run `./mvnw verify` at home, WP:338).
Exclude: workshop key abuse WP:342; corporate laptops WP:346 (email material, mention only if the prerequisites page does); USB kit parts of WP:338-339; facilitator spare Max account WP:340; "Facilitator" prefix lines.

## 3. Facilitator-only content to exclude (with lines)
- WP:81-83 fallback kit.
- WP:124-126 expected findings (Level 0).
- WP:171-173 Break, facilitator line (the 15-minute Break stays in the agenda only).
- WP:334-347 risk table (rewritten selectively, section 2.11).
- WP:5-6 generation note; WP:8-19 "Why" column; WP:39-41 buffer strategy facilitator reasoning (keep one line "skip stretch goals if behind").
- WP:326-328 are room activities (show of hands, show and tell, keynote map): include only as wrap-up agenda text.
- FB:40-45 facilitator notes, with FB:41 the only line worth turning into a hint.
- S09:105-113 and S10 scenario rehearsals ("Rehearsed in time", S10:468-472), rehearsal records (S09:16, 124-127), spec tasks.
- SDD:7-8 and 42-87 (Part 1 is for Maik) and SDD:10-38.
- RM:59 `solution/level-4-<track>-<brief>` is facilitator reference.
- Question of the "fallback kit" for the offline bundle: S11:17-18 (zip on release and USB kit) is infrastructure, not guide text.

## 4. "Level cleared when" bullets, verbatim from the workshop plan
Level 0 (WP:117-119):
- Your observation card lists at least three failure modes, each with evidence (the failing test, the ArchUnit output or the wrong file).

Level 1 (WP:162-166):
- `CLAUDE.md` has at most 60 lines.
- `/context` shows the path-scoped rules loading only for matching files.
- In a fresh session, the Level 0 task ends with `./mvnw verify` green, shown in the terminal, and no ArchUnit violation.

Level 2 (WP:218-221):
- After a deliberate type error, Claude Code reports new diagnostics and fixes them.
- `/mcp` shows the distributor connected.
- The ACL contract test built from real MCP data is green.

Level 3 (WP:287-290):
- A deliberately red commit is blocked by the hook (exit 2) and then fixed by the agent.
- `git commit --no-verify` is denied.
- The cancel-order use case built with the skill is committed through `/ship`.

Level 4 (WP:317-321 and identical SDD:138-142):
- Spec artefacts committed next to the code.
- `./mvnw verify` green, including `ArchitectureTest`, shown in the terminal (not claimed).
- Commit made through your `/ship` command and passed the test-gate hook.
- Every acceptance scenario in your spec maps to a test (ask the agent to list the mapping).
(The wording is already identical between WP and SDD; no divergence.) Line wraps in WP:118-119, 165-166 are only markdown wraps.

## 5. Candidate `:::tip Checkpoint` items (command, expected output, source)
Stated by the pack:
| Command / action | Expected | Source |
|---|---|---|
| `java -version` | 25 | WP:52 |
| `claude` then `/status` | logged in with a Pro/Max/Team/Enterprise account | WP:51 |
| `./mvnw verify` | green, under a minute | WP:56 (caution: PG:7 24 s before spec 12; PG:26 OQ4) |
| `/context` after a question outside `domain` | `CLAUDE.md` loaded, rules for `domain` not; after editing in `domain` the rule loads | WP:164, S10:113-115 |
| Edit a file with a deliberate type error | `Found N new diagnostic issues ...` under the edit | WP:198, S10:174 |
| `/plugin` Installed tab | `jdtls-lsp` listed | WP:194 (S10:202 on `solution/level-2`) |
| `/mcp` | `distributor` connected (S10: with 4 tools) | WP:208, 219; S10:209 |
| Ask about distributor titles | tools appear as `mcp__distributor__*` | WP:210 |
| `git commit -m "test"` with a failing test | hook exits 2, commit blocked, stderr last 40 lines, Claude refers to the failing test | WP:261, 288; S10:402-404 |
| `git commit --no-verify -m "x"` / `git commit -n -m "x"` / `git push` | denied by permission system | WP:289; S10:407-408 |
| ask Claude to read `.env` | denied | S10:410-413 |
| `openspec validate add-book-reviews --strict` | passes after fixes (`--strict` must pass on the seeded spec, S10:444) | SDD:107 |
Gaps (no pack-stated output; do not invent): `claude --version` output string; `docker info` output; `which jdtls` path; the exact `./mvnw verify` success lines (a `BUILD SUCCESS` line appears only in PG evidence, not as a pack requirement); `git status` / `git diff --stat` results (Level 0 is open-ended); `/memory` list contents; `openspec validate` success text; output of `git switch ...`; `wc -l CLAUDE.md`.

## 6. OS tabs needed
| Item | macOS/Linux | Windows PowerShell | Stated by pack? |
|---|---|---|---|
| jdtls check | `which jdtls` | `Get-Command jdtls` | yes WP:54 (but WP:190 says "inside WSL") |
| jdtls install | `brew install jdtls`; `yay -S jdtls`; JDT LS release plus wrapper | use WSL, install as on Linux | yes WP:61-66 |
| key export | `export BOOKSTORE_AI_API_KEY=<key on screen>` | `$env:BOOKSTORE_AI_API_KEY="<key on screen>"` | yes WP:74-75 |
| Maven wrapper | `./mvnw verify` | `mvnw.cmd verify` | yes only in S10:33 (CLAUDE.md text); WP uses `./mvnw` everywhere |
| hook script | `tests-before-commit.sh` (bash) | `tests-before-commit.ps1` | bash text S10:273-295; ps1 content and hook command NOT stated (S10:316-318) |
| Stop hook script | `architecture-check-on-stop.sh` | `.ps1` | same gap |
| `chmod +x` | needed | not applicable | gap |
| reset `git restore . && git clean -fd` | as is | `&&` chaining: not stated | gap (two lines is safe) |
| `git clone ... && cd AgenticBookStore` | as is | not stated | gap |
| Level 4 `git stash -u` / `git switch` | as is | same, both git | fine |
| `tree .claude` (wrap-up WP:327) | `tree .claude` | not stated | gap |
| OpenSpec/Spec Kit install (take-home only) | SDD:149-150 | SDD:149-150 (`irm ... iex`, `--script ps`) | yes |
| Agent OS | bash | WSL or Git Bash | yes SDD:15, 136 |

## 7. Post-spec-09 / post-spec-10 gaps
"Quotable" = the spec text states the content precisely (the guide may quote it, but the final branch content may differ after
build and verification). Existing paths on `factory` verified on this worktree. None of `solution/*`, `start/*`, `level-0-start` exist yet (git: only `factory`, local pre-publish `main`, `sdd/*`, tags `spec-01-done..spec-05-done`). Note the local `main` is the old unpublished one (PG:57 says it is still 7c45791), not the future participant `main`.

| Page | Needs | Exists today? | Quotable? | Action |
|---|---|---|---|---|
| Level 0 / 1 | `docs/architecture.md` (referenced by CLAUDE.md, WP:146, 285) | no (spec 09, S09:47-48) | described only (what it contains) | link by path; follow-up verify link after spec 09 |
| Setup | README run commands, prerequisites | no (spec 09 S09:37-46) | commands quotable: `./mvnw verify`, `./mvnw -pl bookstore spring-boot:run`, `http://localhost:8080/shop/`, `./mvnw -pl distributor spring-boot:run` (S09:38-40) | quote commands; link README after spec 09 |
| Setup, Level 0 | tag `level-0-start` and `main` content, `notes/` in `.gitignore` | tag no; `.gitignore` yes on factory:31-32 | tag semantics S09:62-66 | state as `main`; follow-up: verify `main` `.gitignore` after spec 09 |
| Level 1 | contents of `solution/level-1`: `CLAUDE.md`, `.claude/rules/domain.md`, `data-and-migrations.md`, `testing.md`, `CLAUDE.local.md.example` | no | yes, full text S10:25-109 | Guide should NOT print them by default (they are the answer); link to the branch (decision for orchestrator; if shown, S10:25-109 is quotable) |
| Level 2 A | `docs/lsp-setup.md` | no | content bullets S10:164-177 (at most 40 lines), final text unknown | do not duplicate; link to the file on `solution/level-2`; WP:186-202 already holds the same facts |
| Level 2 A | plugin declaration in `.claude/settings.json` (`extraKnownMarketplaces`, `enabledPlugins`) | no | JSON S10:140-152 but S10:153-162 says "Re-verify at build time", fallback is removing both keys | WP:193 sentence "project scope writes to `.claude/settings.json` (as on `solution/level-2`)" depends on the spec 10 result; follow-up |
| Level 2 B | `.mcp.json` | no | JSON S10:128-138 | no need to quote; follow-up: what `claude mcp add` does on top of an existing file |
| Level 2 B | multi-role contributor ACL contract test | no | described S10:179-183 | link only; example ISBN not given by the pack (the participant finds it with the MCP) |
| Level 2 B | `CatalogMcpTools.java` | yes | real path `distributor/src/main/java/nl/openvalue/distributor/mcp/CatalogMcpTools.java` (WP:214 uses `...`); 4 tools `search_titles`, `get_title`, `list_subjects`, `check_availability` | replace the ellipsis with the real path |
| Level 3 B | `tests-before-commit.sh` bash | no | yes S10:273-295 | participants write it themselves (WP:260-262); quote S10 as "compare" or link |
| Level 3 B | `architecture-check-on-stop.sh` | no | yes S10:297-314 | optional |
| Level 3 B | PowerShell variants and Windows instructions | no | NO (S10:316-318 only names them; hook command for Windows, shell used, unverified) | follow-up after spec 10 task 4 |
| Level 3 B | `.githooks/pre-commit` and `git config core.hooksPath .githooks` | no | yes S10:320-326 | quote; note WP:263 says "write your own first" |
| Level 3 C | `SKILL.md`, `ship.md`, `architecture-reviewer.md` reference text | no | yes S10:328-354, 361-378, 380-392 | participants write them; the descriptions in WP:274-285 suffice; link for compare |
| Level 3 C | `reference.md` of the skill | no | described only (S10:356-359: generate from `docs/architecture.md`, under 80 lines) | link only |
| Level 3 C | cancel-order reference | no | described S10:395-398 | link only |
| Level 4 | `start/level-4-*` contents (`openspec/config.yaml`, seeded `openspec/specs/ordering/spec.md`, `.specify/memory/constitution.md`, `agent-os/standards/`) | no | config and constitution input quotable S10:430-441, 451-454; seeded spec and agent-os standards NOT | describe in one line what each branch preinstalls; follow-up for real paths (SDD:109 path has `...`) |
| Level 4 | OpenSpec CLI availability | no | not stated (see 1.3) | follow-up question |
| Wrap-up | `solution/level-4-<track>-<brief>` | no | optional (S10:483) | do not link |
| All | `README.md` of the repo for participants | no (spec 09) | S09:37-46 | link only after spec 09 |
Branch names fixed by D6 (D:53-55, RM:50-59). S10:12 says level branches are created "from `level-0-start`".

## 8. Contradictions and ambiguities affecting guide content
- C1 Windows story: WP:66 and WP:190 say native Windows is "not verified", jdtls via WSL ("Windows: inside WSL"); WP:344 says Claude Code runs natively with bash hooks via Git Bash or WSL2 and `.ps1` variants; S11:62-63 says "Windows: through WSL" for jdtls and tabs "macOS/Linux, Windows PowerShell" for commands (S11:55-56). It is unclear where a Windows participant runs Claude Code (inside WSL or native). If inside WSL, PowerShell tabs for `which jdtls`, `$env:` and hooks are moot. Decision needed before writing the Windows tabs.
- C2 Catch-up semantics: see A1 (WP:132, 180, 230 vs RM:61, WP:347). Level 0 has no catch-up statement at all; S11:50 requires one on every level page.
- C3 Level 3 Stop hook: optional in WP:270 and D:62-63, but present in the `solution/level-3` settings (S10:258-268, 297-314). The guide must say it is optional for the exercise.
- C4 `/ship` text: WP:279-282 (shows status, runs verify, proposes message, commits) vs S10:361-378 (adds a scope check: "every changed file belongs to: $ARGUMENTS"). Consistent but WP omits the scope check; the guide describes what the participant writes, so WP text governs; S10 is the reference.
- C5 Level 4 time: WP:34 solo 37 min vs SDD:104,117,129 "about 35 minutes" per track (fine, includes no debrief); SDD:21 estimates Spec Kit 60 to 90 min "less on a prepared branch with the short path".
- C6 Brief names: SDD examples use Brief A only (`add-book-reviews`, "add book reviews"); WP:311 and FB give A, B, C. The `/ship add book reviews` text is Brief A specific.
- C7 `/speckit-analyze` and `/speckit-checklist`: SDD:74 uses analyze in Part 1 (Maik) while SDD:124 tells participants to skip it. Not a contradiction for the guide, but do not copy Part 1.
- C8 Course name: WP:329 "Introduction to MCP" vs real title "Introduction to Model Context Protocol" (verified).
- C9 `solution/level-1` "Catch-up / compare" at Level 1 (WP:132) vs the Level 0 observation: Level 1 starts on `main` "again (clean)" (WP:132), RM:52 says `main` is used in Level 0 "and the start of Level 1". Consistent.
- C10 `.mcp.json` pre-exists on `solution/level-2` (S10:128) while Level 2 B2 asks participants to create it with `claude mcp add` (WP:207-208). Harmless on a Level 1 result; undefined for catch-up participants.
- C11 AS (docs/architecture) lists a package set "exactly" (AS:14) but ArchUnit does not enforce a ninth package (PG:33).
- C12 `./mvnw verify` "under a minute" (WP:56, AS:160) vs PG:26 (frontend build may exceed); the checkpoint text should not promise a number the build might break; confirm after spec 12.
- C13 The spec file name: brief says `11-workshop-guide`; the file is `specs/11-workshop-guide-site.md` (D:9, D:101 use that name).
- C14 Unverified Claude Code behaviour claims in WP that the guide would repeat (I did not verify them): new sessions start in auto permission mode (WP:101), "from auto: default, acceptEdits, plan" cycle (WP:246), permission order deny/ask/allow (WP:246), `if` field in hooks (WP:255), commands and skills "same mechanism now" (WP:281-282), import syntax `@docs/architecture.md` behaviour (WP:144-147), `/rewind` not tracking Bash changes (WP:115). The S10 plan itself only verified the plugin docs (S10:153-162).

## 9. QA checklist table: source line -> item -> page
(One row per atomic requirement; QA ticks each. Page abbreviations: SU setup, L0..L4, WU wrap-up, CS cheat sheet, TS troubleshooting, AR architecture, IX index.)

| Source | Item | Page |
|---|---|---|
| WP:50 | `claude --version` | SU |
| WP:51 | `claude` then `/status` | SU |
| WP:52 | `java -version` | SU |
| WP:53 | `docker info` | SU |
| WP:54 | `which jdtls` / `Get-Command jdtls` | SU |
| WP:55 | `git clone ...AgenticBookStore.git && cd AgenticBookStore` | SU |
| WP:56 | `./mvnw verify` | SU |
| WP:61-66 | jdtls install table (4 OS rows) | SU |
| WP:68-69 | plugin installed in Level 2 Part A | SU |
| WP:74-75 | key export, both shells | SU |
| WP:78-79 | `/model sonnet` | SU |
| WP:92-93 | L0 sync | L0 |
| WP:95-98 | L0 task text | L0 |
| WP:101 | `claude`, auto mode note | L0 |
| WP:102-103 | paste task, do not help | L0 |
| WP:104 | `./mvnw verify` | L0 |
| WP:105-110 | `git status`, `git diff --stat`, 6 check questions | L0 |
| WP:111-113 | observation card | L0 |
| WP:114-116 | reset `git restore . && git clean -fd`, `Esc` twice, `/rewind`, checkpoint note | L0 |
| WP:117-119 | L0 Level cleared when | L0 |
| WP:134-135 | L1 sync | L1 |
| WP:138-139 | `/init` | L1 |
| WP:140-147 | CLAUDE.md 60 lines content list, commands, DoD, 5 rules, path vs import | L1 |
| WP:148-151 | 3 rules files with `paths:` | L1 |
| WP:152-155 | `~/.claude/CLAUDE.md`, `CLAUDE.local.md`, `/memory`, `/context` | L1 |
| WP:156-159 | `/context`, `/clear`, `/compact`, subagent delegation | L1 |
| WP:160 | redo task | L1 |
| WP:162-166 | L1 Level cleared when (3 bullets) | L1 |
| WP:182-184 | L2 sync | L2 |
| WP:190 | `which jdtls` | L2 |
| WP:191-194 | `/plugin install ...`, marketplace add, scope, Installed tab | L2 |
| WP:195-196 | find references `Money.of` | L2 |
| WP:197-199 | type error prompt, `Found N new diagnostic issues`, fix prompt | L2 |
| WP:200-202 | Errors tab, `claude --debug`, error text, two minute rule | L2 |
| WP:205 | start distributor | L2 |
| WP:207-208 | `claude mcp add ...`, `.mcp.json`, `/mcp` | L2 |
| WP:209-210 | MCP question, `mcp__distributor__*` | L2 |
| WP:211-213 | ACL contract test prompt | L2 |
| WP:214 | `CatalogMcpTools.java` path | L2 |
| WP:216 | stretch `list_new_releases(days)` | L2 |
| WP:218-221 | L2 Level cleared when (3 bullets) | L2 |
| WP:232-233 | L3 sync | L3 |
| WP:236-245 | permissions JSON | L3 |
| WP:246-247 | rule order, `Shift+Tab`, `/plan` | L3 |
| WP:250-259 | hooks JSON | L3 |
| WP:260-262 | hook script requirements, `chmod +x`, exit 2 | L3 |
| WP:263-264 | `git config core.hooksPath .githooks` | L3 |
| WP:265 | HALF_UP prompt | L3 |
| WP:266-268 | why both hooks | L3 |
| WP:270-271 | optional Stop hook | L3 |
| WP:274-277 | skill creation | L3 |
| WP:278 | cancel order prompt | L3 |
| WP:279-282 | `/ship` command | L3 |
| WP:284-285 | stretch subagent | L3 |
| WP:287-290 | L3 Level cleared when (3 bullets) | L3 |
| WP:303-307 | track table | L4 |
| WP:311-312 | brief pick | L4 |
| WP:314-315 | L4 sync | L4 |
| WP:317-321 | L4 Level cleared when (4 bullets) | L4 |
| SDD:93-97 | common start commands | L4 |
| SDD:99-102 | branch content, own words, answers | L4 |
| SDD:104-115 | Track A steps 1 to 7 + gotchas | L4 tab A |
| SDD:117-127 | Track B steps 1 to 6 + skip note + gotchas | L4 tab B |
| SDD:129-137 | Track C steps 1 to 5 + gotchas | L4 tab C |
| FB:6-12, 14-20, 22-36 | Briefs A, B, C with questions and two routes | L4 section |
| WP:326-332 | wrap-up items, take-home, key revoked | WU |
| WP:329 | 5 Academy courses | WU |
| WP:330-331 | OpenCode, Claude Agent SDK | WU |
| S11:34 | who cleared which level, recap map | WU |
| SDD:146-152 | install after the workshop (optional) | WU or L4 |
| WP:25-35 | agenda | IX |
| S11:38 | five troubleshooting topics | TS |
| AS:19-30, 32-41, 45-55, 65-73, 158 | onion, packages, rules, suffixes, referee | AR |
| cheat sheet rows (section 2.9) | commands and files | CS |

## 10. Sources
All verified by reading: WP, SDD, FB, RM, AS, S09, S10, S11, D, PG (lines 1-60 only, used for PG:7, 26, 33, 57, 94), `.gitignore` of this branch, `distributor/.../CatalogMcpTools.java` (grep of `@McpTool`), `git branch -a`, `git tag`. Not read in full: `product/mission.md` (read, lines 1-33, nothing to inventory), `product/tech-stack.md`, `orchestrator/sdd-mapping.md`, `README.md` of the pack, specs 01 to 08 and 12.
Fetched 2026-11-08: https://anthropic.skilljar.com/ and the five course pages (all OK), https://opencode.ai/ (OK), https://code.claude.com/docs/en/agent-sdk/overview (OK after redirects). Not verified: Claude Code behaviour claims in C14 (outside this topic).

## 11. Open questions for the orchestrator
1. A1 and C2: define the "Catch-up" and "Compare" semantics and the Level 0 catch-up (suggest `main`).
2. C1: where do Windows participants run Claude Code (WSL or native)? Decides the PowerShell tabs.
3. Show the `solution/level-1..3` file texts on the pages, or only link the branches (they are the answers)?
4. The participant email does not exist: write one (separate task) or let the setup page be the source?
5. OpenSpec CLI on `start/level-4-openspec`: preinstalled or participant-installed?
6. Follow-ups after spec 09: verify `main` `.gitignore` contains `notes/`; `docs/architecture.md`, README links. After spec 10: PowerShell hook scripts and Windows pointer text, real `docs/lsp-setup.md`, result of the plugin-declaration re-verification (WP:193), `.mcp.json` behaviour on catch-up, real paths in `start/level-4-*` (SDD:109), first-index times (WP:343).
7. Should guide checkpoints promise "under a minute" given PG:26?
