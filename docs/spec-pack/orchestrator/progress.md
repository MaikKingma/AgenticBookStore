# Factory progress

Product goal: A small, realistic, test-enforced brownfield bookstore (Spring Boot 4, Java 25, Clean Hexagonal Onion) that workshop participants harness with Claude Code over four labs.
Current phase: 3, spec 02 step 2 (run the cycle)
Current spec: 02 (in progress). Goal: the catalogue slice through every onion layer (Book aggregate, Money, ISBN, search), the pattern every later slice mirrors.
Factory skills installed and invocable: yes (all four invoked on 2026-11-06, Phase 1 step 9)
Clean `./mvnw clean verify` time: 10 s wall clock, Maven 9.6 s, after spec 01 (budget 60)

| Spec | Status | Tag / commit | Verify time | Evidence and notes |
|---|---|---|---|---|
| 01 | done | spec-01-done | 10 s (Maven 9.558 s) | PR #1 merged as b6f8534; review fixes on top; 56 + 3 tests, 0 skipped; CI run 42321683398 green (Linux verify, Windows build); see DoD 01 below |
| 02 | in progress | | | orchestrator agent run started 2026-11-07 |
| 03 | todo | | | |
| 04 | todo | | | |
| 05 | todo | | | |
| 06 | todo | | | |
| 07 | todo | | | |
| 08 | todo | | | |
| 12 | todo | | | |
| 09 | todo | | | |
| 10 | todo | | | |
| 11 | todo | | | |

## Open questions

- OQ4 (2026-11-06, Phase 2, D2): the frontend build (Node.js download on the first run, `npm ci`, Vitest, Vite build) will likely push `./mvnw clean verify` past the 60 second budget on later specs. Not a decision yet; recorded so it is no surprise when the measurement after spec 12 lands here.

- OQ1 closed 2026-11-07: Maik copied `.claude/settings.json` in; committed on factory as 07cfbba.

## Manual checks for Maik

- Optional: `gh auth refresh -h github.com -s workflow`. The gh token lacks the `workflow` scope, so the orchestrator pushed the branch that adds `.github/workflows/ci.yml` over SSH. Later specs that touch the workflow need the same workaround until the scope is added.
- Spec-pack suggestion from the spec 01 review (no action needed now): no ArchUnit rule rejects a ninth top-level package in the bookstore (for example `util`). The standard says there are exactly eight. If you want it enforced, a later spec can add the rule and its fixture.

## Definition of done evidence

### DoD 01 (2026-11-07, on factory after the review fixes)

- `./mvnw verify` green locally with Docker 29.8.2, and in CI: local `./mvnw -B clean verify` from the repo root: `Tests run: 56, Failures: 0, Errors: 0, Skipped: 0` (bookstore), `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0` (distributor), `BUILD SUCCESS`, `Total time: 9.558 s`, 10 s wall clock. CI on the merge commit b6f8534: run https://github.com/MaikKingma/AgenticBookStore/actions/runs/42321683398 success (Linux `verify` job 128247464370, Windows `windows-build` job 128247464001). The CI run on the tagged commit is checked after the push (see the decision log).
- `grep -ri h2 bookstore/pom.xml bookstore/src pom.xml` returns nothing: output empty, `wc -l` = 0.
- Verify time recorded here: 10 s wall clock (Maven 9.558 s); the orchestrator measured 9.505 s on b6f8534 and 9.450 s on a fresh clone of a997d98.
- `ArchitectureSelfTest` proves every rule fails on its fixture: Surefire `ArchitectureSelfTest` tests=28 failures=0 errors=0 skipped=0; `ArchitectureTest` tests=20. One self-test per rule asserts the designated violator is named as the origin of the violation; a mutation probe (query layer rule changed to the data layer) makes `queryIsNotAccessedByOtherLayers` fail with "names nl.openvalue.archfixtures.data.BookDataServiceImpl as origin", then the file was restored (`git checkout`, tree clean).
- No agent files on `main`: not provable yet, the publish script comes with spec 09. Proxy: no product file references `docs/spec-pack`, `agent-os`, `.claude` or `CLAUDE.md` (review check); `main` is untouched (still 7c45791).


## Decision log

- 2026-11-06 Phase 0: tool checks all pass (Java 25.0.4, Docker 29.8.2, Node 24.20.0, npm 11.19.0, git 2.50.1, gh as MaikKingma with ADMIN on MaikKingma/AgenticBookStore, start.spring.io answers).
- 2026-11-06 Phase 0: SDD tooling found and mapped, see `sdd-mapping.md`.
- 2026-11-06 Phase 0, Maik: Phase 3 is driven by the `orchestrator` agent (one run per spec, `merge: true`), not by `/sdd-cycle` in the main session.
- 2026-11-06 Phase 0, Maik: branch model is PRs into `factory`. Task PRs squash-merge into `sdd/<slug>/spec`; the spec PR merges into `factory` with a merge commit; tag `spec-NN-done` on factory after the merge.
- 2026-11-06 Phase 0, Maik: `agent-os/product` is a symlink to `../docs/spec-pack/product`; a pointer standard `agent-os/standards/architecture.md` is indexed.
- 2026-11-06 Phase 0, Maik: keep the existing clone, branch `factory` from `main` (7c45791), commit the untracked agent-os commands and standards index in the bootstrap.
- 2026-11-06 Phase 1: D1 to D5 are listed in `decisions.md`; they are applied in Phase 2.
- 2026-11-06 Phase 2, Maik (OQ2 closed): the Windows CI job is compile-only (`mvnw.cmd -B -DskipTests verify`); database tests run on the Linux job. Applied to spec 01.
- 2026-11-06 Phase 2, Maik (OQ3 closed): the guide deploys to GitHub Pages from `factory` on pushes touching `guide/**`. Applied to spec 11.
- 2026-11-06 Phase 2, Maik (OQ1): Maik copies `.claude/settings.json` in himself; it is committed when present.
- 2026-11-06 Phase 2 cross-check (read-only subagent over all specs, product files, workshop files and the four skills): no leftover H2, second repository, old build order, static chat page or placeholder. It found 20 cross-file inconsistencies and 15 skill-versus-spec disagreements; all fixed in the pack copy and the skills, except these, deliberately left: the architecture standard's header and the sdd-framework-guide still name `agent-os/standards/architecture/clean-hexagonal-onion.md` and `/plan-product` for the participants' Lab 4 track C branch (that is the participants' Agent OS install, not the factory's pointer standard); spec 08's system prompt sentence about pointing customers to the shop (product text, now that a shop exists Maik may want to revisit it); the OpenSpec CLI as a Track A prerequisite not listed in Lab 0 (pre-existing, outside D1 to D5); the standard's sentence "every external failure is translated into a port exception" versus spec 06's `Optional.empty()` for a 404 (the skills now carry the spec 06 exception; the standard was not changed).
- 2026-11-06 Phase 2, judgment calls: (1) the architecture standard section 1 now states that the bookstore root package holds `BookstoreApplication` and only spec-mandated cross-cutting configuration (spec 04 `Clock`, spec 12 `StorefrontConfig`). This is the one change to the standard; D3 requires a home for the serving code, and spec 04 already established the practice. (2) The skills' "never edit ArchitectureTest" became "never weaken or remove a rule", because specs 02, 06 and 07 add rules, a fixture and the one named exception. (3) The ddd skill's Clock rule now defers to the spec signatures and names how the Clock reaches aggregates whose spec signature has none (spec 03 events, spec 02 publication year). (4) The onion skill's package table puts the assistant row before command and query, so spec 08's `AssistantCommands` lands in `assistant`.
- 2026-11-06 Phase 2: versions pinned in tech-stack.md from the official release pages (React 19.3.1, Vite 8.3.6, Vitest 5.1.0, RTL 16.3.3, jest-dom 7.0.1, user-event 14.6.8, Node.js 26.13.0 Active LTS, frontend-maven-plugin 2.0.2, postgres:18.6, Testcontainers 2.0.5 as managed by Boot 4.1.2, Docusaurus 3.10.2). Compatibility: Vite 8 and Docusaurus 3.10 accept Node.js 26; Boot 4.1.2 manages Testcontainers 2.0.5.
- 2026-11-06 Phase 2: spec 12 written by the orchestrator in the pack structure (see decisions.md D3 for why the shaping command was not used for it).
- 2026-11-06 Phase 2: storefront uses hash routing and one `StorefrontConfig` in the bookstore root package (the standard allows spec-mandated cross-cutting configuration there, like spec 04's Clock bean). Frontend output reaches the bookstore as a Maven resource from `frontend/dist`, not as a jar dependency, so `./mvnw -pl bookstore spring-boot:run` keeps working.
- 2026-11-07 Phase 3, spec 01: orchestrator agent run completed. Spec PR #1 (https://github.com/MaikKingma/AgenticBookStore/pull/1) merged into factory as b6f8534; task PRs #2 to #7 squash-merged into `sdd/01-foundation/spec`; qa-final PASS endorsed by the QA lead; ledger `.claude/comms/mailboxes/orchestrator/outbox/run-01-foundation.md`; plan `agent-os/specs/2026-11-06-2155-01-foundation/plan.md`.
- 2026-11-07 Phase 3, spec 01, judgment calls by the orchestrator agent, accepted: (1) `management.endpoint.health.show-components=always` in the bookstore, because the spec's Compose scenario expects `db` in the health response and Boot hides components by default; details stay hidden and the server binds to 127.0.0.1. Spec 01's property list now names it. (2) The plan-only clause "no package-info.class is produced" dropped (maven-compiler-plugin 3.15.0 writes one by default; not a pack requirement). (3) Rule 3 of the standard encoded as six ArchUnit rules; rule 9 as the single named ignoreDependency. (4) Framework warnings (Mockito dynamic agent, JDK CDS, flyway prefixes.txt 401) accepted, not fixed.
- 2026-11-07 Phase 3, spec 01 review (fresh read-only subagent): no blocker. Fixed on factory before tagging: `DatabaseCleanerIntegrationTest` no longer asserts that the schema has no application table (that would fail on spec 02's first migration) and uses probe table names (`"CleanerProbe"`, `cleaner_probe_line`) instead of `"Order"` and `order_line`, which spec 04 may migrate; `ArchitectureSelfTest` now matches each violator as the origin of the violation (three one-token layer swaps survived before) and pins all seven naming suffix sets against the standard; `PostgresTestConfig` documents that `@DirtiesContext` must never be used in the bookstore suite; unused `testcontainers-junit-jupiter` dependency removed (it carries the only annotation that could silently skip tests when Docker is missing); empty start.spring.io boilerplate removed from both module POMs. Not changed: test-only stub type in `org.springframework.ai.archfixtures` (replace with a real Spring AI type in spec 08), `areAnnotatedWith` for rule 8 (meta-annotations only if a spec asks), CI running twice for PRs from the same repository (matches the spec literally), the test profile assertion (note for the spec 09 README: run tests through the wrapper or set `-Dspring.profiles.active=test` in the IDE).
- 2026-11-07 Phase 3, spec 01: open follow-ups F1 to F15 from the QA lead are recorded in the plan and QA ledger for the spec 02 strategy; none blocks spec 01.
- 2026-11-07 Phase 3, spec 02 orientation, judgment calls: (1) `http/catalogue.http` lives in a repo-root `http/` folder, as spec 09 lists `http/*.http` next to `README.md` and `docs/architecture.md`. (2) The publication-year upper bound ("next year") needs the current time. Following the ddd skill (Phase 2 judgment call 3), `Book.addToCatalogue` and `restore` take a `java.time.Clock`, `CatalogueFlow` passes its injected `Clock`, and spec 02 creates the `Clock` bean (`Clock.systemUTC()`) in a root-package config class, the place the standard reserves for it; spec 04 then reuses that bean instead of creating it. Tests use a fixed clock.
- 2026-11-06 Phase 1: bootstrap committed as 1cd9170 and pushed to origin/factory. GitHub default branch stays main. Skills gate passed: hexagonal-slice, clean-hexagonal-onion, ddd-building-blocks and layer-testing all appear in the session and load.
