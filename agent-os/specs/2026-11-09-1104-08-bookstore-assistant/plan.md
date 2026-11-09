---
spec: 08-bookstore-assistant
source: docs/spec-pack/specs/08-bookstore-assistant.md
status: in-progress
accepted-by: orchestrator (autonomous)
gate1: 2026-11-09T11:51:45Z
qa-lead: on
base: factory
spec-branch: sdd/08-bookstore-assistant/spec
cut-from: origin/sdd/07-restock-process/spec (9196b08)
modules: [root, bookstore, distributor]
---

# Plan: Spec 08 Bookstore assistant

Restates `docs/spec-pack/specs/08-bookstore-assistant.md` (source of truth). Shaping decisions O1 to O8 and the accepted tech-lead calls D1 to D11 are in `shape.md`, standards in `standards.md`, research summary in `references.md`. The full task cards (file scope, criteria, named tests, QA probes, risks) are section 4 of the tech-lead strategy; the QA strategy (tiers, Gate 1 findings G1 to G23, common directive C0, per-task directives, probes, qa-final focus) is in the QA lead's ledger. Both live in the cycle's comms and are passed to every agent by path: `.claude/comms/mailboxes/lead-1/outbox/strategy-08-bookstore-assistant.md` (section 3 ground rules, section 4 task cards) and `.claude/comms/mailboxes/qa-lead/outbox/qa-ledger-08-bookstore-assistant.md` (section "QA strategy"). Precedence for a task's acceptance criteria: this plan's task section, then the Gate 1 findings it names (they win over the card), then the card.

Path abbreviations: B = `bookstore/src/main/java/nl/openvalue/bookstore/`, R = `bookstore/src/main/resources/`, T = `bookstore/src/test/java/nl/openvalue/bookstore/`, F = `bookstore/src/test/java/nl/openvalue/archfixtures/`.

## Summary

The bookstore gets an AI assistant as an inbound adapter in `assistant`: `POST /assistant/chat` takes `ChatPayload {conversationId, message}` and answers `AssistantReplyView {conversationId, reply}`. A Spring AI `ChatClient` runs with the product-owned system prompt `prompts/assistant-system.st`, four read-only tools in `BookstoreTools` that call `CatalogueFlow` and `InventoryFlow` only, and a 20-message window memory per conversation. Without `BOOKSTORE_AI_API_KEY` the application starts and the endpoint answers 503 `assistant-offline` ("Assistant offline: set BOOKSTORE_AI_API_KEY"), through `AssistantModeConfig` (an environment post-processor that sets `spring.ai.model.chat=none`) and `OfflineChatModel`. Guardrails: messages up to 1000 characters (400 above), 20 requests per minute per instance (429, the model is not called). Default model `claude-haiku-5-5`, no sampling setting (the API rejects it), base URL pinned. Tests never call a real model: `ScriptedChatModel` drives the real tool path through ChatClient's tool-calling advisor. Referee `./mvnw clean verify` from the repo root, green after every task, budget 60 s (baseline 19.9 s on 9196b08: bookstore 1138 tests, distributor 190, 10 Started lines, ArchitectureTest 22 rules, ArchitectureSelfTest 58).

## Ground rules for every task

- The pack spec decides what; `docs/spec-pack/product/architecture-standard.md`, `tech-stack.md` and `docs/spec-pack/orchestrator/decisions.md` win; then O1 to O8 and D1 to D11 (`shape.md`). Bookstore code goes through the `hexagonal-slice` skill (with `clean-hexagonal-onion`, `ddd-building-blocks`, `layer-testing`). A contradiction is reported as blocked, never resolved by the implementer.
- Only the task's fileScope is edited, never anything else, not even temporarily. Mutation probes on production code are the verifier's job: the implementer describes each mutant in its outbox.
- Never skip, disable, delete or weaken a test or an ArchUnit rule; never edit `ArchitectureTest.java`; never add `ignoreDependency`; no `@Disabled`, `assume*`, `@DirtiesContext`; never `--no-verify`.
- No test and no agent calls the real Anthropic API. The live check with a key is the owner's gate. Agents never set, read or export `ANTHROPIC_API_KEY`, and the literal appears nowhere under `bookstore/` or `distributor/` (no test asserting its absence either). A fake `BOOKSTORE_AI_API_KEY` value (never a real key) is used only for the QA C0-1b offline proof.
- No `temperature`, `top-p`, `top-k` in configuration (O2); no `spring.ai.tools.limits.*` (O8); no `spring.ai.tools.throw-exception-on-error`.
- Test doubles for full contexts are plain classes added by `@Import`, never `@Component`. No `@MockitoBean` or `@Import` on an `IntegrationTestBase` subclass except the one named in T5.
- Falsifiers first: two values per pass-through field, both sides of every cap (10 search results, 5 recommendations, 1000 characters, 20 requests per minute, 20 memory messages), reverse-ordered data plus ties, every ordering claim pinned pairwise. No error body or tool error text contains a stack trace.
- Plain English, no em dashes; every new or changed file is ASCII (`rg -n '[^\x00-\x7F]' <file>` empty). Product files and tests never reference `docs/spec-pack`, `agent-os`, `.claude`, `CLAUDE.md`.
- Git is read-only for every agent; the orchestrator owns commits, branches and PRs.
- Every implementer records its final test method names per acceptance criterion in its outbox (the task's rows of the scenario-to-test map in `shape.md`).
- Scratch and evidence files: session scratchpad `/private/tmp/claude-501/-Users-maikkingma-OpenValue-AI-Conferences-AgenticBookStore/cb0898b6-6f7b-44fd-8024-5110d0f772e2/scratchpad/` with prefix `s08-T<n>-<role>-`. Long runs through `bash ~/.claude/scripts/with-budget.sh <s> ...`.

## Task table

| id | summary | module | dependsOn | batch | qaTier | qaModel | implModel | status | verdict | qa-lead | rounds | pr | commit |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| T1 | Starter, configuration (O1 to O3), test-profile key blank (D1), `AssistantProperties`, `AssistantModeConfig` in `spring.factories`, `OfflineChatModel`, first `AssistantConfig`, ArchUnit fixture uses the real `ChatClient` | bookstore | [] | 1 | critical | opus | opus | done (branch `sdd/08-bookstore-assistant/T1`) | PASS (r1) | ENDORSE | 1 | #113 | 76be904 |
| T2 | `BookSuggestionView`, `BookDetailsView`, `AvailabilityView`, `BookstoreTools` with caps, ordering and curated error texts | bookstore | [T1] | 2 | elevated | sonnet | sonnet | done (branch `sdd/08-bookstore-assistant/T2`) | PASS (r1) | ENDORSE | 1 | #116 | 699a8d9 |
| T3 | `assistant-system.st`, `ChatMemory` (20), `ChatClient` bean, `ScriptedChatModel` with the silent tool-drop negative control | bookstore | [T2] | 3 | critical | opus | opus | todo | | | | | |
| T4 | `AssistantCommands`, `ChatPayload`, `AssistantReplyView`, `AssistantRateLimiter`, `AssistantExceptionHandler`; slice, limiter and offline HTTP tests | bookstore | [T3] | 4 | critical | opus | opus | todo | | | | | |
| T5 | `AssistantIntegrationTest` (grounding, memory, tool error, pending restock) and ArchUnit non-vacuity | bookstore | [T4] | 5 | elevated | sonnet | sonnet | todo | | | | | |
| T6 | README section "Assistant" with the single `ANTHROPIC_API_KEY` warning | root | [T4] | 6 | standard | sonnet | sonnet | todo | | | | | |

Execution and merge order, one task at a time: T1, T2, T3, T4, T5, T6. Owner's gate after T6: the manual live check with the workshop key (transcript in the spec PR).

qaDirective of every task: C0 plus the task's T<n> block of the QA ledger (T6: the C0 items the ledger names), plus every `## QA feed-forward` entry recorded before the task starts.

## Tasks

### T1: Dependency, configuration, offline mechanism, ArchUnit stub swap
- fileScope: `bookstore/pom.xml`; `R/application.yml`; `bookstore/src/test/resources/application-test.yml`; `R/META-INF/spring.factories`; `B/assistant/AssistantProperties.java`, `B/assistant/AssistantModeConfig.java`, `B/assistant/OfflineChatModel.java`, `B/assistant/AssistantConfig.java`; `T/assistant/AssistantSettingsTest.java`, `T/assistant/AssistantModeConfigTest.java`, `T/assistant/OfflineChatModelTest.java`, `T/assistant/AssistantConfigContextTest.java`, `T/AssistantModeIntegrationTest.java`; `F/command/ChatCommands.java`; `bookstore/src/test/java/org/springframework/ai/archfixtures/StubChatClient.java` (delete); `T/ArchitectureSelfTest.java` (import line, named set, one addition).
- Acceptance criteria: strategy card T1, plus Gate 1 finding G8 (the configuration tests build their environment without the process environment, so they stay green with `BOOKSTORE_AI_API_KEY` exported).

### T2: View records and `BookstoreTools`
- fileScope: `B/assistant/BookSuggestionView.java`, `B/assistant/BookDetailsView.java`, `B/assistant/AvailabilityView.java`, `B/assistant/BookstoreTools.java`; `T/assistant/BookstoreToolsTest.java`.
- Acceptance criteria: strategy card T2, plus Gate 1 findings G1 (criterion 8 rewritten: the valid genre names come from the `InvalidInputException` details, `assistant` never reads `domain.Genre`; a test with made-up names fails a hard-coded list), G2, G4 to G7, G3 (the D5 technical-failure path logs a WARN with the exception, asserted with a log recorder, not console capture) and G5 (a null or blank `bookId` gives the malformed-id text, no flow call).

### T3: System prompt, `AssistantConfig` ChatClient, chat memory, `ScriptedChatModel`
- fileScope: `R/prompts/assistant-system.st`; `B/assistant/AssistantConfig.java`; `T/assistant/ScriptedChatModel.java`, `T/assistant/ScriptedChatModelTest.java`, `T/assistant/AssistantConfigTest.java`, `T/assistant/AssistantSystemPromptTest.java`; `T/assistant/AssistantConfigContextTest.java` (supporting beans only); `T/AssistantModeIntegrationTest.java` (additions only).
- Acceptance criteria: strategy card T3, plus Gate 1 findings G10 (a test pins that the `ChatMemory` bean is ours from `AssistantConfig`, not Spring AI's auto-configured default) and G11, plus G24 (QA feed-forward FF2 from T1): `AssistantModeIntegrationTest.offlineSwitchComesAfterTheConfigurationFiles` lists the shared context's property source names in order, asserts that `assistantOfflineMode` and sources containing `application-test.yml` and `application.yml` exist, and that `assistantOfflineMode` comes after every source whose name contains `application`; must-fail probe for the verifier: `@Order(Ordered.HIGHEST_PRECEDENCE)` on `AssistantModeConfig`.

### T4: `AssistantCommands`, `AssistantRateLimiter`, `AssistantExceptionHandler`
- fileScope: `B/assistant/AssistantCommands.java`, `B/assistant/ChatPayload.java`, `B/assistant/AssistantReplyView.java`, `B/assistant/AssistantRateLimiter.java`, `B/assistant/AssistantExceptionHandler.java`; `T/assistant/AssistantCommandsTest.java`, `T/assistant/AssistantCommandsLengthTest.java`, `T/assistant/AssistantRateLimiterTest.java`, `T/assistant/MutableClock.java`; `T/AssistantModeIntegrationTest.java` (additions only).
- Acceptance criteria: strategy card T4, plus Gate 1 finding G16 (the 429 and 503 cases also send the storefront's exact header `Accept: application/json, application/problem+json`).

### T5: Full-context integration tests and ArchUnit non-vacuity
- fileScope: `T/AssistantIntegrationTest.java`; `T/IntegrationTestBase.java` (one Javadoc sentence); `T/ArchitectureSelfTest.java` (additions only); `T/assistant/ScriptedChatModel.java` (additions only); `T/assistant/BookstoreToolsTest.java` (additions only, amendment A2).
- Acceptance criteria: strategy card T5, plus F-T2-1 (amendment A2): in `BookstoreToolsTest.invalidInputWithoutAGenreListKeepsJustItsDetail`, `catalogue.search("long", null, null, null)` throws `new InvalidInputException("invalid-query", "Invalid query", "Query too long", List.of(Map.of("field", "q", "message", "Query too long")))` and the tool text is exactly `"Query too long"`; must-fail mutant for the verifier: `.filter(Objects::nonNull)` removed from `validGenres`.

### T6: README section "Assistant"
- fileScope: `README.md`.
- Acceptance criteria: strategy card T6.

## Gate 1 record

Self-accepted by the orchestrator (owner delegated gate approvals). Checklist: every task has summary, criteria, fileScope, one module, dependsOn, qaTier, qaModel, qaDirective; the criteria the QA lead flagged untestable as written are rewritten (G1, G8, G22 in C0) or recorded as inspection items in the ledger (behavioural prompt rules go to the owner's live check); batches follow topological order; no decision outside the orchestrator's authority (O2 and O3 are cheap, reversible and flagged to the owner).

## QA feed-forward

- T1 FF1 (every later verifier and qa-final): C0 amendment A1 of the QA ledger applies (keyed-log network grep without bare numbers, the `spring.ai.anthropic.api-key` property-name constant allowed in `assistant`, sampling grep `temperature|\btop[-_]?[pk]\b|\btop[pk]\b`, log greps count the bookstore section only) and the re-baseline A1-5 on 0d67746.
- T1 FF2 (T3): Gate 1 finding G24, see task T3.
- T1 FF3 (every later verifier): read the implementation outbox for in-place production edits and report them as a process finding with the file's hash check, never as evidence.
- T1 FF4 (every agent): `updated` stamps come from `date -u`, never estimated.
- T1 ship-gate baseline for T2 (plain log `s08-T1-orch-shipgate-plain.log` in the session scratchpad): BUILD SUCCESS 27.1 s, bookstore 1192, distributor 190, ArchitectureTest 22, ArchitectureSelfTest 63, Started lines unchanged from the 0d67746 base (10 in the bookstore section, 15 over the whole log with the FF5 regex; corrected after T2).
- T2 FF5 (plan and every brief): count Started lines in the bookstore section only with `Started .* in .* seconds`: 10 for T1 to T3, 12 from T4, 13 from T5 (15 over the whole log through T3). Never `Started .*Test in`.
- T2 FF6 (every verifier): every round writes its own `s08-T<n>-qa-r<k>-hashes.txt` and quotes the `stat -f '%Sm %N'` order line, even when production did not change.
- T2 FF7 (implDirective T3 to T5): a test that feeds a rejection gives it non-empty `details` with keys other than the usual ones for that path, as well as the empty case.
- T2 FF8 (every verifier): a kill that happens only because an unstubbed mock throws an NPE that a catch-all turns into the generic text is fragile; report it and ask for a lenient stub on the boundary row.

## Amendments

- A1 (2026-11-09, orchestrator, on the main session's instruction): spec 07 merged into factory (776bf86). `origin/factory` (2fda3b6, with spec 12's `frontend` module and fix-12) was merged into the spec branch with a merge commit (0d67746) and into the T1 branch before its ship gate. Referee on 0d67746: 24.7 s, bookstore 1166, distributor 190. Consequence: the module vocabulary gains `frontend`; the QA lead re-baselined C0 (A1-5 in its ledger).
- A2 (2026-11-09, orchestrator, after the T2 meta-review): the QA lead's test-only follow-up F-T2-1 (a non-genre rejection detail witness for `validGenres`) is added to T5 instead of a separate task, with `BookstoreToolsTest.java` (additions only) in T5's fileScope. Cheap, test-only, no production change; gate re-run self-accepted.
