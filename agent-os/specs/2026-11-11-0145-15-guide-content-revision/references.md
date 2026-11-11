# References for spec 15

Official documentation read on 2026-11-11 by the shaping researchers.

## Claude Code (Level 1 and Level 2)

- Memory and rules (`.claude/rules/*.md`, `paths` frontmatter, when path-scoped rules load):
  https://code.claude.com/docs/en/memory
- Context window and `/context`: https://code.claude.com/docs/en/context-window
- Built-in commands (`/context`, `/compact`, `/clear`, `/memory`): https://code.claude.com/docs/en/commands
- Costs and context management: https://code.claude.com/docs/en/costs
- Code intelligence (LSP) plugins: https://code.claude.com/docs/en/plugins/code-intelligence
- Plugin troubleshooting, language server does not start:
  https://code.claude.com/docs/en/plugins/troubleshooting
- Discover and manage plugins (`/plugin`, Errors tab): https://code.claude.com/docs/en/discover-plugins

## Level 4 frameworks (pinned in `docs/spec-pack/product/tech-stack.md`)

- OpenSpec 1.16.0: https://github.com/Fission-AI/OpenSpec (README and docs, existing projects, `openspec init`,
  `openspec/config.yaml` context, `/opsx` flow); install script https://openspec.dev/cli/install.sh and
  https://openspec.dev/cli/install.ps1 (both honour `OPENSPEC_VERSION`); npm package
  https://registry.npmjs.org/@fission-ai/openspec/-/openspec-1.16.0.tgz (engines: node 22.12.0 or later).
- Spec Kit (`specify-cli` 1.3.1): https://github.com/github/spec-kit (getting started, existing projects,
  constitution first); package metadata https://pypi.org/pypi/specify-cli/1.3.1/json
- Agent OS 3.0: https://buildermethods.com/agent-os (workflow pages: discover standards, inject standards, plan
  product, shape spec) and https://github.com/buildermethods/agent-os/tree/main/commands/agent-os

## Docusaurus 3.10.2 (wide tables)

- MDX and React in Markdown: https://docusaurus.io/docs/markdown-features/react
- Swizzling: https://docusaurus.io/docs/swizzling

## Repo inputs

- Pack spec: `docs/spec-pack/specs/15-guide-content-revision.md`; decision D19 in
  `docs/spec-pack/orchestrator/decisions.md`.
- Spec 14 folder: `agent-os/specs/2026-11-10-1610-14-guide-branding/` (A4, R5, R6 with RA-1 to RA-4,
  `checker-baseline-hashes.txt`, the T1 baseline of this spec).
- Spec 11 folder: `agent-os/specs/2026-11-08-2206-11-workshop-guide/`.
- Spec 10 folder (PF-T11-1, PF-T13-1, PF-T13-4): `agent-os/specs/2026-11-09-1730-10-harness-reference-solutions/plan.md`.
- Level branches, read only: `origin/solution/level-1` (`.claude/rules/`), `origin/solution/level-2`,
  `origin/start/level-4-openspec`, `origin/start/level-4-speckit`, `origin/start/level-4-agentos`.
