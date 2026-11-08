---
name: distributor-design-decisions
description: Rationale behind spec 05 distributor design calls (advice scope, REST 400 vs MCP clamp, real services in slices, strict seed reader, shared ISBNs); use when shaping specs 06, 09, 10 or labs that touch the distributor
metadata:
  type: project
---

Status: proposed in the spec 05 strategy (lead-1 outbox, 2026-11-08); verify against `distributor/` and the spec 05
plan under `agent-os/specs/` before relying on any bullet. The distributor is outside the onion: no skills, no ArchUnit.

- **Advice scoped with `basePackageClasses`, no catch-all handler.** **Why:** a global advice with broad handlers also
  applies to non-HandlerMethod handlers such as the MCP router function at `/mcp`. Cost: unmapped `/api/...` paths
  fall to Boot's default 404 JSON (accepted).
- **REST rejects (400 `INVALID_REQUEST`), MCP clamps.** **Why:** program callers get silent paging bugs from a clamp;
  the MCP scenario mandates clamping (limit 50 gives at most 20) and LLMs recover better from a smaller result.
- **Slices use the real in-memory services with fixture catalogues and fixed clocks, not `@MockitoBean`.** **Why:** a
  mocked restock service makes the Friday-to-Tuesday API scenario vacuous.
- **Seed read through Boot's `JsonMapper` with a per-call strict reader.** **Why:** tech-stack forbids a custom
  ObjectMapper unless a spec asks; unknown keys must still fail fast.
- **Tool text cap enforced in code (trim trailing items), not by data.** **Why:** long titles push 20 summaries past
  4000 characters; a data-only guarantee breaks when labs add titles.
- **Shared ISBNs are classics only; distributor EANs use a `97899` prefix plus P&P.** **Why:** new-release years move
  with the clock while spec 09 seeds fixed years; the prefix keeps bookstore-only ISBNs (spec 07 not-found path)
  easy to keep out.
- **MCP task ordered before REST.** **Why:** fail fast on the piece an external client (Claude Code) must accept.

Related: [[strategy-constraints]], [[test-context-cost-model]].
