# Shape: Spec 10 Harness reference solutions and level branches (prepare phase)

Source of truth: `docs/spec-pack/specs/10-harness-reference-solutions.md`. This file records the scope of this cycle
and the shaping decisions; it never changes a requirement, scenario, path, name or file text of the pack spec.

## Scope of this cycle

- In (prepare phase): author every file each level branch adds, stage them on `factory` under
  `docs/spec-pack/level-branches/`, build the tooling that cuts the branches later, run every scenario that can run
  without `main` against factory's product code in scratch sandboxes, including the macOS LSP rehearsal (decision D8),
  and run per-task QA.
- Out (resumed run, after spec 09 publishes `main` and tag `level-0-start`): cutting `solution/level-1` to
  `solution/level-3` and `start/level-4-openspec`, `start/level-4-speckit`, `start/level-4-agentos` from
  `level-0-start`, verify on each branch, re-running the base-dependent scenarios on the real base, qa-final over the
  branches. Nobody creates, pushes or moves a `solution/*`, `start/*` or `level-*` ref in this phase.
- Out (manual checks for Maik): the LSP scenarios on Linux and Windows through WSL (D8); every Windows run of the
  Level 3 hooks; "Plugin loads from the project declaration" (needs the interactive trust dialog); "Rehearsed in
  time" for the three Level 4 tracks (needs a human facilitator new to the framework).
- Out (optional pack task 6): `solution/level-4-<track>-<brief>` branches. Not built in this cycle.

## Decisions

- J1 Staging mechanism. Level files live in `docs/spec-pack/level-branches/<step>/` with `manifest.tsv`, `files/`
  and, for product files that already exist on `level-0-start`, `patches/` in git diff format. Every staged path
  component that starts with a dot is renamed `dot-...` and `CLAUDE.md` is staged as `CLAUDE.staged.md`, because
  Claude Code loads `CLAUDE.md`, `.claude/rules/` and `.claude/skills/` from subfolders as soon as a session touches a
  file there (research res-1): a literal copy would load the Level 1 rules and the Level 3 skill into every factory
  session that reads the staging folder. `docs/spec-pack/` is already on spec 09's strip list, so nothing staged can
  reach `main`, and the folder is inert for `./mvnw clean verify` and CI (research res-3). Scripts simulate
  `level-0-start` from any factory ref (git archive plus spec 09's strip list, preferring the publish script's
  `--list-paths` when the ref has it), apply one step or the whole chain, and cut the branches locally later.
  Rejected: real local branches off factory (they carry factory's agent config), a patch series only (hard to review),
  staging at the target paths (collides with factory's own `CLAUDE.md`, `.claude/settings.json` and skills).
- J2 Cancel order (reference code only, cheap and reversible): `Order.cancel(Clock)` records `cancelledAt` with a V5
  migration; `StockItem.release(Quantity)` lowers `reserved`, rejects more than reserved and handles the low-stock
  signal like `receive`; 409 codes `order-already-shipped` (existing) and `order-already-cancelled`; shipping a
  cancelled order is 409 `order-already-cancelled`; no domain event. Views mirror `shippedAt`.
- J3 Level 4 tools run without global installs (`npx -y @fission-ai/openspec@1.16.0`,
  `uvx --from specify-cli==1.3.1 specify`, Agent OS cloned into a scratch folder with its commit recorded; the pack's
  `~/agent-os` is never touched). The scripted form of `openspec validate --strict` is
  `openspec validate --all --strict --no-interactive`. The framework slash steps run headless with their inputs inline;
  generated files are committed as they are. The Level 3 allow list is not extended for the framework scripts.
- J4 No tech-lead strategy: the only design question (staging) is settled by three research reports; everything else
  is verbatim pack text or pattern-following exercise code.
- J5 `brew install jdtls` for the macOS rehearsal (D8; the pack's own install command; reversible).
- J6 Not adopted, decisions for Maik: `"onFailure": "block"` on the commit hook (a first build slower than 180 s
  lets the commit through) and PowerShell-tool handlers and rules for Windows without Git Bash.
- J7 Headless sandbox sessions are isolated with `--setting-sources project,local` and
  `ENABLE_CLAUDEAI_MCP_SERVERS=false`; untrusted sandboxes ignore project allow rules in `-p` mode, so the Level 3
  allow list is passed with `--settings` and recorded as a stand-in for the trust dialog. Every sandbox that may start
  the app uses its own `COMPOSE_PROJECT_NAME` and free ports. No user-scope writes. `ANTHROPIC_API_KEY` is never set,
  read or written.
- J8 `docs/lsp-setup.md` says "JDK 21 or later" instead of the pack bullet's "JDK 17 or later": the upstream `jdtls`
  wrapper refuses Java below 21 (research res-1; T6 confirms it on the installed wrapper). The note is written from
  the bullets, not copied verbatim; the pack bullet needs the same edit (Maik).
- J9 QA lead Gate 1 findings G1 to G13 adopted: evidence never as `.log`, the Windows note under `records/` (root
  `.gitignore` ignores `notes/`), a `group` column in the manifest so a level-3 tree without cancel order can be
  built, a pinned `--list-paths` contract, atomic apply, the frontend display of `CANCELLED` as a residual, the
  assistant row of `reference.md` re-checked at cut time.

## Spec 11 coordination

Branch names are exactly the D6 names. The Windows hook note (`notes/windows-hooks.md` in the staging folder) and the
`git config core.hooksPath .githooks` activation are content for the spec 11 guide; this cycle does not edit `guide/`.
