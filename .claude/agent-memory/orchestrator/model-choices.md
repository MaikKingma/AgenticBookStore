---
name: model-choices
description: Which task kinds held on sonnet and which needed opus in this repo; the owner's sonnet-first rule overrides the definition's escalation table
applies-to:
  - any
metadata:
  type: project
---

- **Owner rule:** sonnet for implementation, per-task verifiers and research; opus only for qa-lead, qa-final and
  critical-tier tasks. It overrides "rework round >= 2 -> opus" in the orchestrator definition.
- **Sonnet held (one round, or test-only rework):** web command/query adapters, `.http` collections, CI workflows,
  DTO/port tasks, services, REST, chaos, packaging and README, command endpoints, integration tests, test-only
  follow-ups, seed/docs/count tasks, guide content tasks, frontend state, labels and CSS, LLM tools, every lite bugfix
  task. The QA lead rated the sonnet verifier as good as opus on an elevated tier.
- **Opus held where used:** build skeleton, database and test infrastructure, ArchUnit tasks, domain and flows, data
  and persistence adapters, RestClient adapter and import flow, process adapter, seed data with MCP, the publish
  script, LLM config, ChatClient wiring and HTTP adapter.
- **Sonnet needed help:** a mapper with validator hand-offs (spec 04 T2: lead REJECT plus an opus round); a domain task
  with region witnesses (spec 07: two test-only rounds). Name the witness family in round 0 instead of escalating
  ([[round0-directive-traps]]).
- One combined senior research brief can settle several framework questions of a lite bugfix in about 20 minutes.
