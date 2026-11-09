# Level 1 record: consistency check (decision D14)

Branch `solution/level-1`, five staged files (`level-1/manifest.tsv`). No Claude Code session was run for this record
(decision D14, plan amendment A2). Nothing here claims a Claude result. One `claude -p` auth probe did start before D14
reached the spec branch; it answered nothing (see "Scope guard"). The two Level 1 acceptance scenarios that need
a session are rows of `rehearsal-checklist.md`, section "Level 1", for Maik's rehearsal on 2026-11-10.

## Verdict

| Check | Result |
|---|---|
| Every token of the five staged files resolves on `origin/factory` or is a listed spec 09 arrival | 50 rows: 46 consistent, 3 arrives-with-spec-09, 1 mismatch-pack-or-guide, 0 mismatch-own-file |
| Staged files spelled the same as pack spec 10, the workshop plan and the guide | yes, except one pack sentence (PF-T3-1) |
| `CLAUDE.md` has no `@` import | proven (P1) |
| Each rule's `paths` matches only its intended folder | proven on the level-1 sandbox tree, negative controls fail as they must |
| `CLAUDE.md` at most 60 lines | 29 lines |
| `./mvnw clean verify` green on the level-1 sandbox | green, 1314 bookstore tests and 190 distributor tests, `ArchitectureTest` 22 tests |

No row is a mismatch in the task's own files, so there is no FAIL to report.

## Inputs and shas

| Input | Value |
|---|---|
| Code base | `origin/factory` at `ae2a5f26ef752f68a49d6d44ce2e860ffe6a57cc` |
| Staged files and T1 scripts | spec branch tip `0f07e919f05c49392895779a49b27bab27c39f59` (`level-1/files/**`, `scripts/**`, unmodified) |
| Level-0 stand-in (T1 `simulate-level-0.sh`) | `3b34237764412495519afd8fc1c28fc479a8029c` |
| Level-1 commit (T1 `build-level.sh`) | `fcff79f61d83f9e46e915b215fa9173e4f973a25`, adds exactly the five staged paths |
| Guide sources | `origin/sdd/11-workshop-guide/spec` at `921e18b0427c2e5d46c0784afc99f8a30b8b3f73`, read-only, `guide/docs/**` |
| Pack text | `docs/spec-pack/specs/10-harness-reference-solutions.md` (Level 1 block, lines 23 to 119), `docs/spec-pack/specs/09-workshop-baseline.md`, `docs/spec-pack/workshop/workshop-plan.md` |

Line numbers below: "staged" cites the target path with the line of the staged file (the same line as in the
sandbox). Pack spec 10 lines are lines of `10-harness-reference-solutions.md`. Plan means `workshop-plan.md`. Guide
paths are under `guide/docs/`. Factory cites are `path:line` on the recorded sha. Raw outputs:
`evidence/level-1/resolution-on-factory.txt` and `evidence/level-1/spelling-in-pack-plan-guide.txt`.

## Arrivals with spec 09 (FF-T2-a)

`docs/architecture.md` (named by `CLAUDE.md`) and `db/seed/R__seed_demo_data.sql` (named by
`.claude/rules/data-and-migrations.md` line 10) are absent on the simulated base and arrive with spec 09
(`09-workshop-baseline.md` lines 12, 21 to 23, 48). Both must be present on the real `level-0-start`. Resumed-run
checks, each must print a path:

```
git ls-tree level-0-start -- docs/architecture.md
git ls-tree level-0-start -- bookstore/src/main/resources/db/seed/R__seed_demo_data.sql
git grep -n "classpath:db/seed" level-0-start -- bookstore/src/main/resources/application.yml
```

Today the tag does not exist locally: `git ls-tree level-0-start` answers `fatal: Not a valid object name
level-0-start`, so none of the three checks can pass yet. The factory side, from the recorded sha:
`git ls-tree --name-only ae2a5f2 -- docs/architecture.md` prints nothing, and the same for the `db/seed` folder. A
Level 0 run on the simulated base that tries to read either file and finds it missing is a side effect of the base,
not a run failure.

## Consistency table

Status values: consistent, mismatch-own-file (would be a FAIL), mismatch-pack-or-guide (product finding for Maik),
arrives-with-spec-09.

### `CLAUDE.md`

| # | Token | Named at (staged) | Resolves on factory ae2a5f2, or command output | Pack spec 10 / Plan / Guide | Status |
|---|---|---|---|---|---|
| C1 | `Spring Boot 4.1 / Java 25` | CLAUDE.md:3 | `pom.xml:7-8` spring-boot-starter-parent 4.1.2; `pom.xml:24` java.version 25 | spec 10 :29 / plan :52 (`java -version`, 25) / guide `00-setup-check.md:16` (Temurin 25) | consistent |
| C2 | modules `bookstore`, `distributor`, `frontend` | CLAUDE.md:3-4 | `pom.xml:18-20` the three modules | spec 10 :29-30 / not named / not named in the Level 1 page; `reference/architecture.md` names packages of the bookstore only | consistent |
| C3 | `/shop/`, frontend built by Maven | CLAUDE.md:4 | `frontend/vite.config.ts:10` `base: '/shop/'`; `StorefrontConfig.java:27-29,36-37`; frontend is a Maven module (C2); in the sandbox verify the `frontend` module is SUCCESS | spec 10 :30 / not named / not named | consistent |
| C4 | `./mvnw verify` | CLAUDE.md:7 | `mvnw` at the factory root; sandbox run green (P6) | spec 10 :33 / plan :141 / guide `02-level-1-context-engineering.mdx:49` | consistent |
| C5 | `mvnw.cmd verify` (Windows) | CLAUDE.md:7 | `mvnw.cmd` at the factory root; not executed, no Windows host (rehearsal row R1-3) | spec 10 :33 only / not named / not named | consistent |
| C6 | Docker must be running (Testcontainers) | CLAUDE.md:7 | `bookstore/pom.xml:84,89` testcontainers; `PostgresTestConfig.java:16`; verify green with Docker (P6) | spec 10 :33 / plan :53 (`docker info`) / guide `00-setup-check.md:18` | consistent |
| C7 | wrapper only, never `mvn` | CLAUDE.md:8 | `mvnw`, `mvnw.cmd` and `.mvn/` at the factory root | spec 10 :34 / plan :126 (`mvn` instead of `./mvnw` is an expected Level 0 finding) / not named | consistent |
| C8 | `./mvnw -pl bookstore test -Dtest=BookTest` | CLAUDE.md:9 | `bookstore/src/test/java/nl/openvalue/bookstore/domain/BookTest.java:17`; run in the sandbox: 38 tests, BUILD SUCCESS (P7) | spec 10 :35 / plan :141 / guide `02-level-1-context-engineering.mdx:50`, same text | consistent |
| C9 | `./mvnw -pl bookstore spring-boot:run` (port 8080) | CLAUDE.md:10 | `bookstore/pom.xml:134` spring-boot-maven-plugin; `./mvnw -pl bookstore spring-boot:help -Dgoal=run` prints the goal and BUILD SUCCESS; `bookstore/src/main/resources/application.yml:37` `port: 8080` (address 127.0.0.1, line 36); app not started by this check | spec 10 :36, spec 09 :39-40 / not named / not named for the bookstore | consistent |
| C10 | `./mvnw -pl distributor spring-boot:run` (port 8090) | CLAUDE.md:10 | `distributor/pom.xml:43`; `spring-boot:help -Dgoal=run` for `distributor` prints the goal and BUILD SUCCESS; `distributor/src/main/resources/application.yml:17` `port: 8090` | spec 10 :36 / plan :205 / guide `03-level-2-give-your-agent-eyes.mdx:116`, `reference/troubleshooting.md:66-69` | consistent |
| C11 | `ArchitectureTest`, definition of done (show last lines, never claim without running) | CLAUDE.md:13-14 | `bookstore/src/test/java/nl/openvalue/bookstore/ArchitectureTest.java:28`; 22 tests green in the sandbox verify | spec 10 :39-40 / plan :142 / guide `02-level-1-context-engineering.mdx:53`, `reference/architecture.md:70` | consistent |
| C12 | `docs/architecture.md` | CLAUDE.md:18 | absent on factory (`git ls-tree` prints nothing); the pack source is `docs/spec-pack/product/architecture-standard.md` | spec 10 :44 / plan :145 / guide `02-level-1-context-engineering.mdx:55`, `reference/architecture.md:8`; spec 09 :12, :48 | arrives-with-spec-09 |
| C13 | plain path, no `@` import | CLAUDE.md:18 | P1: `grep -n '@' CLAUDE.md` finds nothing (exit 1); P2: line 18 is the only mention | spec 10 :44 and plan :145-147 and guide `02-level-1-context-engineering.mdx:55-57` all say plain path; spec 09 :49 says "Level 1 imports it from `CLAUDE.md`" | mismatch-pack-or-guide (PF-T3-1, the staged file is right) |
| C14 | inbound adapters `command`, `query`, `process`, `assistant`; `domaininteraction` | CLAUDE.md:18-19 | all eight package folders under `bookstore/src/main/java/nl/openvalue/bookstore/`; `ArchitectureTest.java:60-65` (rule 2), `:67-95` (rule 3) | spec 10 :44-45 / plan :144 / guide `reference/architecture.md:18,32,44-46` | consistent |
| C15 | "Only flows touch domain objects" | CLAUDE.md:19 | `ArchitectureTest.java:51-58` rule 1; its one exception (process may reference `*Event` classes) is `:54-56` | spec 10 :45 / not named / guide `reference/architecture.md:44,52` names the exception | consistent (note N1) |
| C16 | `domain` plain Java, no Spring, JPA, Jackson, Lombok | CLAUDE.md:20 | `ArchitectureTest.java:97-102` `onlyDependOnClassesThat().resideInAnyPackage("java..", DOMAIN)`; `:104-109` persistence types only in `data` | spec 10 :46 / plan :143 / guide `02-level-1-context-engineering.mdx:54`, `reference/architecture.md:47` | consistent |
| C17 | `Money`, BigDecimal, scale 2, HALF_EVEN, never double or float | CLAUDE.md:21 | `domain/Money.java:11` record with `BigDecimal amount`; `:13` `SCALE = 2`; `:27` `setScale(SCALE, RoundingMode.HALF_EVEN)`; `ArchitectureTest.java:141-155` bans double and float in `domain`, `domaininteraction`, `command`, `query` | spec 10 :47 / plan :143 (`Money` (BigDecimal), never double) / guide `02-level-1-context-engineering.mdx:54`; scale 2 and HALF_EVEN are named only in spec 10 | consistent (note N2) |
| C18 | new Flyway migration in `bookstore/src/main/resources/db/migration` | CLAUDE.md:22-23 | the folder holds `V0__baseline.sql` to `V4__add_pending_restock.sql`; `ddl-auto: validate` at `application.yml:12` | spec 10 :48 / plan :144 / guide `02-level-1-context-engineering.mdx:54` (folder not named) | consistent |
| C19 | no Lombok, no MapStruct; "records and small hand-written mappers" | CLAUDE.md:24 | `git grep -i -E "lombok\|mapstruct"` over the three poms and `bookstore/src`, `distributor/src`: no hit; records: `domaininteraction/BookDTO.java:13`, `OrderDTO.java:14`, `StockItemDTO.java:12`; hand-written mappers: `BookDomainMapper.java:14`, `OrderDomainMapper.java:14`, `StockItemDomainMapper.java:12` (final classes), `data/BookJPAMapper.java:6`, `OrderJPAMapper.java:11`, `StockItemJPAMapper.java:10` | spec 10 :50 / plan :144 (Lombok; MapStruct not named) / guide `02-level-1-context-engineering.mdx:54` | consistent |
| C20 | `DistributorAppService` port | CLAUDE.md:25 | `domaininteraction/DistributorAppService.java:9`; implemented by `acl/DistributorAppServiceImpl.java:41`; used in main code only by `CatalogueFlow` and `InventoryFlow`; RestClient only in `acl` (`ArchitectureTest.java:111-116`) | spec 10 :51 / plan :212 (`DistributorAppServiceImpl`) / guide `reference/architecture.md:32,38` | consistent |
| C21 | conventional commit messages (feat:, fix:, test:, refactor:, docs:) | CLAUDE.md:29 | factory history, last 60 subjects: docs 31, chore 7, feat 5, fix 4, test 2 (others are merge commits). `chore:` is in use in the repo history but is not in the rule's list; that is a note, not a mismatch (the rule names the prefixes the workshop wants, it does not claim to be the full set the history uses). `refactor:` has no use in the last 60 | spec 10 :55 / not named / not named | consistent |
| C22 | at most 60 lines | CLAUDE.md:1-29 | `wc -l CLAUDE.md` prints 29 (P4) | spec 10 :25 / plan :140,:163 / guide `02-level-1-context-engineering.mdx:44,107` | consistent |
| C23 | the five rules the plan lists (domain pure Java, money, new migration, inbound adapters only to flows, no Lombok) | CLAUDE.md:18-24 | all five present as C14 to C19 | spec 10 :44-50 / plan :143-144 / guide `02-level-1-context-engineering.mdx:54` | consistent |
| C24 | `PostgreSQL in Docker` (module `bookstore`) | CLAUDE.md:3 | `bookstore/compose.yaml:2-3` service `postgres:` with `image: 'postgres:18.6'`; Testcontainers PostgreSQL for tests (C6); `application.yml:6` comment says spring-boot:run starts `compose.yaml` | spec 10 :29 (same words) / plan :56, :82 ("the PostgreSQL image", no "in Docker") / guide `00-setup-check.md:84` ("the PostgreSQL image"); spec 09 :39-40 and :83 say "PostgreSQL through Docker" (same fact, other words) | consistent |
| C25 | `React storefront` (module `frontend`) | CLAUDE.md:3-4 | `frontend/package.json:13-14` `react` and `react-dom` 19.3.1; `frontend/src/main.tsx:1-2,8` imports React and calls `createRoot`; module listed at `pom.xml:18` (C2) | spec 10 :29-30 (same words) / plan :89 ("a React storefront") / guide `01-level-0-break-the-agent.mdx:26` ("a React storefront in `frontend/`"); spec 09 does not name it | consistent |

### `.claude/rules/domain.md`

| # | Token | Named at (staged) | Resolves on factory ae2a5f2, or command output | Pack spec 10 / Plan / Guide | Status |
|---|---|---|---|---|---|
| D1 | `paths: "bookstore/src/main/java/**/domain/**"` | domain.md:3 | glob test: 43 files, exactly the `domain` package folder (`evidence/level-1/glob-test-output.txt`) | spec 10 :62 / plan :149 / guide `02-level-1-context-engineering.mdx:63` | consistent |
| D2 | private constructor, static factory, `restore(...)` | domain.md:6 | `Book.java:29,45,52`; `Order.java:30,45,54`; `StockItem.java:34,54,62` (private constructor, `addToCatalogue`/`place`/`openFor`, `restore`) | spec 10 :65 / not named / not named | consistent |
| D3 | no setters | domain.md:7 | `git grep -E "void set[A-Z]"` in the domain package: no hit | spec 10 :66 / not named / not named | consistent |
| D4 | unchecked exception from the `domain` package | domain.md:8 | 22 exception classes in `domain`, each `extends RuntimeException` (for example `InsufficientStockException.java:4`) | spec 10 :67 / not named / not named | consistent |
| D5 | domain events registered in the aggregate, flows publish after saving via `pullDomainEvents()` | domain.md:9 | `Order.java:28,74`, `StockItem.java:32,146`; `OrderFlow.java:153-162` saves (153, 155), then pulls and publishes (157, 161); `InventoryFlow.java:60-61,75-76,168-169` save, then pull | spec 10 :68 / not named / not named | consistent (`Book` raises no events, the rule says "register domain events", it does not require every aggregate to have one) |
| D6 | value objects are records that validate in the compact constructor | domain.md:10 | `Isbn.java:11`, `Quantity.java:9`, `Title.java:8`, `EmailAddress.java:16`, `Money.java:16` | spec 10 :69 / not named / not named | consistent |
| D7 | domain tests plain JUnit and AssertJ, no Spring context | domain.md:11 | `BookTest.java:3-15` imports only AssertJ, JUnit and `java.*`; no Spring import | spec 10 :70 / not named / not named | consistent |

### `.claude/rules/data-and-migrations.md`

| # | Token | Named at (staged) | Resolves on factory ae2a5f2, or command output | Pack spec 10 / Plan / Guide | Status |
|---|---|---|---|---|---|
| M1 | `paths` `bookstore/src/main/java/**/data/**`, `bookstore/src/main/resources/db/**` | data-and-migrations.md:3-4 | glob test: 19 files, exactly the `data` package folder plus `db/` | spec 10 :77-78 / plan :150 / guide `02-level-1-context-engineering.mdx:64` | consistent |
| M2 | `*DataServiceImpl` implements a `*DataService` port and speaks DTOs | data-and-migrations.md:7 | `data/BookDataServiceImpl.java:21` implements `BookDataService` (port in `domaininteraction`), takes and returns `BookDTO`; same for `Order` and `StockItem`; `ArchitectureTest.java:176-177` suffix rule | spec 10 :81 / not named / guide `reference/architecture.md:64` | consistent |
| M3 | `*JPA` entities never leave `data` | data-and-migrations.md:7 | `BookJPA`, `OrderJPA`, `OrderLineJPA`, `StockItemJPA` in `data/`; `git grep -l -E "(Book\|Order\|OrderLine\|StockItem)JPA" bookstore/src/main`: 13 files, all under `data/` | spec 10 :81 / not named / guide `reference/architecture.md:48,64` | consistent |
| M4 | `V{n}__` numbering and `V{n}__{snake_case_description}.sql` | data-and-migrations.md:8 | files `V0__baseline.sql`, `V1__create_catalogue.sql`, `V2__create_inventory.sql`, `V3__create_orders.sql`, `V4__add_pending_restock.sql`; highest is V4, so the rule's next number is V5 | spec 10 :82 / not named / not named | consistent |
| M5 | folder `db/migration` | data-and-migrations.md:8 | same folder as C18 | spec 10 :82 / not named / not named | consistent |
| M6 | `ddl-auto=validate` | data-and-migrations.md:9 | `application.yml:12` `ddl-auto: validate` (yaml spelling of the property) | spec 10 :83 / plan :125 `ddl-auto=validate` / not named | consistent |
| M7 | seed file `db/seed/R__seed_demo_data.sql` | data-and-migrations.md:10 | absent on factory (no `db/seed` folder under `bookstore/src/main/resources`) | spec 10 :84 / not named / not named; spec 09 :23 same path | arrives-with-spec-09 |
| M8 | seed "is not loaded in tests" | data-and-migrations.md:10-11 | factory `application.yml:14-15` has `flyway: enabled: true` with no `locations`; `application-test.yml` has only the AI key; the location split is a spec 09 change | spec 10 :84 / not named / not named; spec 09 :21-22 | arrives-with-spec-09 |
| M9 | `numeric(10,2)` plus `varchar(3)` currency column | data-and-migrations.md:12 | `V1__create_catalogue.sql:8-9` (`price_amount numeric(10,2)`, `price_currency varchar(3)`); `V3__create_orders.sql:21-22` | spec 10 :86 / not named / not named | consistent |

### `.claude/rules/testing.md`

| # | Token | Named at (staged) | Resolves on factory ae2a5f2, or command output | Pack spec 10 / Plan / Guide | Status |
|---|---|---|---|---|---|
| T1 | `paths: "**/src/test/**"` | testing.md:3 | glob test: 152 files, exactly the files below a `src/test` folder, in `bookstore`, `distributor` and `frontend` | spec 10 :93 / plan :151 / guide `02-level-1-context-engineering.mdx:65` | consistent (note N3) |
| T2 | one test class per production class, same package, suffix `Test`, end-to-end `IntegrationTest` | testing.md:6 | `BookTest`, `OrderFlowTest`; nine `*IntegrationTest` classes such as `CatalogueIntegrationTest.java`; same-package pairs: `domain/Book.java` and `domain/BookTest.java`, `domaininteraction/OrderFlow.java` and `OrderFlowTest.java`, `acl/DistributorAppServiceImpl.java` and `DistributorAppServiceImplTest.java`; a convention, not exhaustive (`acl/DistributorProperties.java` has no test of its own) | spec 10 :96 / not named / not named | consistent |
| T3 | `@WebMvcTest` with `@MockitoBean` flows | testing.md:7 | `@WebMvcTest` in 7 classes (`BookCommandsTest`, `BookQueriesTest`, and five more); `@MockitoBean` in 8 files | spec 10 :97 / not named / not named | consistent |
| T4 | `@DataJpaTest` on shared Testcontainers PostgreSQL, `@Import(PostgresTestConfig.class)`, never an embedded database | testing.md:7-8 | `@DataJpaTest` in 3 classes; `data/BookDataServiceImplTest.java:34` `@Import({PostgresTestConfig.class, BookDataServiceImpl.class})`; `PostgresTestConfig.java:16`; no h2 or hsqldb in `bookstore/pom.xml` | spec 10 :97-98 / not named / not named | consistent (the code lists a second class in the same annotation, the rule shows only the config) |
| T5 | `MockRestServiceServer` for the ACL | testing.md:8 | `acl/DistributorAppServiceImplTest.java` | spec 10 :98,:181 / not named / not named | consistent |
| T6 | `ScriptedChatModel` | testing.md:9 | `assistant/ScriptedChatModel.java:24` in the test tree; used by `AssistantIntegrationTest`, `AssistantConfigTest` and two more | spec 10 :99 / not named / not named | consistent |
| T7 | test named after behaviour: `rejectsOrderWhenStockIsInsufficient` | testing.md:10 | illustrative only, no test of that name exists; style matches `OrderFlowTest.java:121` `placeOrderSavesTheOrderWithSnapshotsAndReturnsItsId` | spec 10 :100 / not named / not named | consistent |
| T8 | never weaken or delete an `ArchitectureTest` rule | testing.md:11 | `ArchitectureTest.java` 22 tests, green in the sandbox verify | spec 10 :101 / plan :142 / guide `reference/architecture.md:70` | consistent |

### `CLAUDE.local.md.example`

| # | Token | Named at (staged) | Resolves on factory ae2a5f2, or command output | Pack spec 10 / Plan / Guide | Status |
|---|---|---|---|---|---|
| L1 | the real `CLAUDE.local.md` is git-ignored | CLAUDE.local.md.example:1 | `.gitignore:29` `CLAUDE.local.md`; in the sandbox `git check-ignore -v CLAUDE.local.md` prints `.gitignore:29:CLAUDE.local.md` | spec 10 :104-108 / plan :153 / guide `02-level-1-context-engineering.mdx:81` | consistent |

Row count: 25 + 7 + 9 + 8 + 1 = 50 rows: 46 consistent, 3 arrives-with-spec-09 (C12, M7, M8), 1
mismatch-pack-or-guide (C13), 0 mismatch-own-file. (Rework round 1 added C24 and C25, which the first round had
missed, and widened C19, C21 and T2; the other rows are unchanged.)

### Notes on rows

- N1 (C15): the staged text says "Only flows touch domain objects". The ArchUnit rule has one documented exception
  (rule 9: `process` may reference `*Event` classes). The guide page names it; `CLAUDE.md` does not. This is the
  pack's own wording, not a defect, and the line stays short on purpose.
- N2 (C17): the ArchUnit double and float ban covers `domain`, `domaininteraction`, `command` and `query` only. The
  `data`, `acl` and `assistant` packages are not covered; the staged rule is guidance beyond that scope.
- N3 (T1): `**/src/test/**` also matches the `distributor` and `frontend` tests, whose rule text (Spring slices,
  `PostgresTestConfig`) is about the bookstore. This is how the pack spells the glob (guide the same). The rule loads
  when Claude touches any test there. Not a mismatch, listed so Maik knows it when rehearsing.
- The ArchUnit tests import `DoNotIncludeTests`, so the `archfixtures/domain` and `archfixtures/data` folders in the
  bookstore test tree do not matter for the rules and do not match the `domain.md` or `data-and-migrations.md` globs
  (glob test, "named non-matches").

### Inventory re-check (rework round 1)

After the QA verdict, every sentence of the five staged files was read again against the table, not only the backtick
spans. The first round had missed two unquoted nouns in `CLAUDE.md:3` (`PostgreSQL in Docker`, `React storefront`, now
C24 and C25). Tokens checked, with the row that holds each:

- `CLAUDE.md`: title `Agentic Bookstore` (a name, root `pom.xml` artifact `agenticbookstore-parent`, no claim; no row),
  `Spring Boot 4.1`, `Java 25` (C1), `online bookstore`, `fake book distributor`, modules `bookstore`, `distributor`,
  `frontend` (C2), `PostgreSQL in Docker` (C24), `React storefront` (C25), `built by Maven`, `/shop/` (C3),
  `./mvnw verify`, `mvnw.cmd verify`, Docker and Testcontainers, `mvn` (C4 to C7), `-Dtest=BookTest` (C8),
  both `spring-boot:run` lines and ports 8080 and 8090 (C9, C10), `ArchitectureTest`, last lines of output (C11),
  `docs/architecture.md` (C12, C13), the four inbound adapters, `domaininteraction`, flows (C14, C15), `domain` imports
  (C16), `Money`, BigDecimal, scale 2, HALF_EVEN, double and float (C17), Flyway migration folder and "never edit an
  existing migration" (C18; the second clause is guidance, Flyway checksum validation is the mechanism and
  `application.yml:14-15` does not switch it off), Lombok, MapStruct, records, hand-written mappers (C19),
  `DistributorAppService` (C20), commit prefixes (C21), 60 lines (C22), the five rules (C23).
- Instructions with no resolvable token, so no row: "Show the last lines of the output", "Never say tests pass without
  having run them in this session", "Every new behaviour has a test in the layer where the behaviour lives", "Stay inside
  the scope of the request. Propose unrelated refactors instead of doing them", "Small commits". They are agent
  behaviour for the rehearsal, not facts about the repository.
- `domain.md`: `paths` glob, aggregates, private constructor, static factory, `restore(...)`, behaviour methods, no
  setters, unchecked exception, domain events, `pullDomainEvents()`, flows, value-object records, compact constructor,
  JUnit, AssertJ, no Spring context (D1 to D7).
- `data-and-migrations.md`: both `paths` globs, `*DataServiceImpl`, `*DataService`, DTOs, `*JPA`, `data`, `V{n}__`,
  `db/migration`, snake_case file names, Hibernate `ddl-auto=validate`, `R__seed_demo_data.sql` (repeatable, idempotent,
  not loaded in tests), `numeric(10,2)`, `varchar(3)` (M1 to M9; "repeatable, idempotent" is a property of the seed file
  that arrives with spec 09, so it sits in M7). "Do not add test data there" is guidance.
- `testing.md`: `paths` glob, one test class per production class in the same package, `Test`, `IntegrationTest`,
  `@WebMvcTest`, `@MockitoBean`, `@DataJpaTest`, shared Testcontainers PostgreSQL, `@Import(PostgresTestConfig.class)`,
  embedded database, `MockRestServiceServer`, real distributor and real LLM, `ScriptedChatModel`, the example test name,
  `ArchitectureTest` (T1 to T8). "Never call the real distributor or a real LLM" is guidance; the test profile and
  `ScriptedChatModel` are the mechanism (T6, M8).
- `CLAUDE.local.md.example`: `CLAUDE.local.md` git-ignored, "copy to CLAUDE.local.md, never commit" (L1); the three
  bullets (plan in three bullets, IntelliJ, paths from the repository root) are personal preferences, no repository fact.

## Product findings for Maik

- PF-T3-1: `09-workshop-baseline.md` line 49 says "Level 1 imports it from `CLAUDE.md`" (`docs/architecture.md`). Pack
  spec 10 line 44, workshop plan lines 145 to 147 and the guide (`02-level-1-context-engineering.mdx` lines 55 to 57)
  all use a plain path and teach that an import (`@docs/architecture.md`) is the thing to avoid. The staged `CLAUDE.md`
  follows spec 10. Suggested fix: reword spec 09 to "Level 1 points to it by path from `CLAUDE.md`". Wording only, no
  file change in spec 10 scope.
- PF-T3-2 (information, no fix needed): `mvnw.cmd verify` appears only in `CLAUDE.md` and pack spec 10; neither the
  guide nor the plan names a Windows build command, and the setup check sends Windows users to WSL for `jdtls` only.
  The command resolves (the file exists) but cannot be executed here. It is a row of the rehearsal checklist.

## Zero-model proofs

All run without any Claude process, on the level-1 sandbox built with the T1 scripts. Raw output:
`evidence/level-1/zero-model-proofs.txt`, `evidence/level-1/glob-test-output.txt`, script
`evidence/level-1/glob-test.py.txt`.

### P1 and P3: no `@` import

```
$ grep -n '@' CLAUDE.md
exit: 1
$ grep -rn -E '(^|[[:space:]])@[A-Za-z./]' CLAUDE.md .claude/rules CLAUDE.local.md.example
exit: 1
```

The only `@` characters in the staged files are annotation names inside backtick code spans in
`.claude/rules/testing.md` lines 7 and 8 (`@WebMvcTest`, `@MockitoBean`, `@DataJpaTest`, `@Import(...)`); with code
spans removed, `grep -c '@'` over the rule files prints 0. `CLAUDE.md` names `docs/architecture.md` once, as a plain
path on line 18.

### P4: line count

```
$ wc -l CLAUDE.md
      29 CLAUDE.md
```

### Glob test

`python3 -I glob-test.py.txt <sandbox>` reads the `paths` from the three built rule files and matches them over all
421 tracked files of the sandbox. Claude Code reads a `paths` entry as a glob over the path relative to the project
root: `*` and `?` stay inside one path segment, `**` as a whole segment matches zero or more directories, a trailing
`/**` matches every file below. `fnmatch` is not used (its `*` crosses `/`). Three independent implementations must
agree on every pattern and file: a regex translator, `pathlib.PurePosixPath.full_match` (Python 3.13) and git's
`:(glob)` pathspec. The test could not run Claude Code itself, so the semantics above are the one assumption; the
rehearsal row R1-1 checks the real behaviour.

| Rule | `paths` | Intended folder | Matched | Outside the folder | Intended but missed |
|---|---|---|---|---|---|
| `domain.md` | `bookstore/src/main/java/**/domain/**` | package folder `domain` | 43 | 0 | 0 |
| `data-and-migrations.md` | `bookstore/src/main/java/**/data/**`, `bookstore/src/main/resources/db/**` | package folder `data` plus `db/` | 19 | 0 | 0 |
| `testing.md` | `**/src/test/**` | any `src/test` folder | 152 | 0 | 0 |

Sample matches: `domain/Book.java`, `domain/package-info.java`; `data/BookJPA.java`,
`db/migration/V4__add_pending_restock.sql`; `bookstore/src/test/java/.../BookTest.java`,
`distributor/src/test/java/.../TitlesApiTest.java`, `frontend/src/test/fetchMock.ts`.
Sample non-matches (no rule at all): `CLAUDE.md`, `pom.xml`, `bookstore/src/main/resources/application.yml`,
`.../bookstore/domaininteraction/OrderFlow.java` (a longer segment that starts with `domain`),
`.../command/OrderCommands.java`, `.../query/BookQueries.java`, `.../acl/DistributorAppServiceImpl.java`,
`frontend/src/main.tsx`. Traps that match only the testing rule:
`bookstore/src/test/java/nl/openvalue/archfixtures/domain/BookEntity.java` and
`.../archfixtures/data/BookDataServiceImpl.java` (a `domain` or `data` folder in the test tree). The three rules share
no file.

Negative controls, each must fail the check for its rule (10 of 10 failed as they must):

| Rule | Control glob | Result |
|---|---|---|
| `domain.md` | `bookstore/src/main/java/**` (widened) | 86 files outside the folder |
| `domain.md` | `bookstore/src/main/java/**/domain*/**` (widened) | 24 outside (`domaininteraction`) |
| `domain.md` | `**/domain/**` (no main anchor) | 18 outside (test-tree `domain` folders) |
| `domain.md` | `bookstore/src/main/java/nl/openvalue/bookstore/domain/Book*` (narrowed) | 39 missed |
| `data-and-migrations.md` | `bookstore/src/main/**` (widened) | 118 outside |
| `data-and-migrations.md` | `**/data/**` (no main anchor) | 5 outside, 5 missed |
| `data-and-migrations.md` | `bookstore/src/main/resources/db/migration/V1*` (narrowed) | 18 missed |
| `testing.md` | `**/src/**` (widened) | 237 outside |
| `testing.md` | `**` (widened) | 269 outside |
| `testing.md` | `bookstore/src/test/**` (narrowed) | 18 missed |

Result line of the script: `RESULT: PASS (3 rules matched only their intended folders; all controls failed as
expected; A, B and C agree)`.

### Build

```
$ ./mvnw clean verify          (sandbox HEAD fcff79f, 2026-11-09, 22:26:11Z to 22:26:39Z)
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0 ... in nl.openvalue.bookstore.ArchitectureTest
[INFO] Tests run: 1314, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 190, Failures: 0, Errors: 0, Skipped: 0
[INFO] agenticbookstore-parent ............................ SUCCESS [  0.040 s]
[INFO] frontend ........................................... SUCCESS [  7.890 s]
[INFO] bookstore .......................................... SUCCESS [ 14.724 s]
[INFO] distributor ........................................ SUCCESS [  4.397 s]
[INFO] BUILD SUCCESS
[INFO] Total time:  27.211 s
$ ./mvnw -pl bookstore test -Dtest=BookTest
[INFO] Tests run: 38, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.072 s -- in nl.openvalue.bookstore.domain.BookTest
[INFO] BUILD SUCCESS
```

Environment: the run set `TMPDIR` and `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=...` to the scratchpad, the sandbox lives in
the scratchpad, the shared `~/.m2` cache and Docker were used (J10).

### Scope guard

One `claude` command did run for this task, once, before decision D14 and plan amendment A2 reached the spec branch
(D14 arrived with the commits of 2026-11-09 22:21:55Z and 22:22:54Z; the probe ran at 21:51:20Z, about half an hour
earlier). It was an auth probe, not a check of any claim in this record:

- Command shape: `claude -p "say ok" --model sonnet --output-format json --setting-sources project,local --max-turns 1`,
  started from an empty throwaway folder (no repository), with `HOME` and the `XDG_*` folders set to a folder in the
  scratchpad, no API key variable set (the script unsets it), and `ENABLE_CLAUDEAI_MCP_SERVERS=false`. Script: `$SP/t3/auth-probe.sh`.
- Result: `Not logged in` (`Please run /login`), `is_error` true, `terminal_reason` `api_error`, cost 0, zero tokens, 42 ms.
  No model answered. Nothing in this record depends on it.
- Where it lies: output `$SP/t3/out/auth-probe.json` (empty `auth-probe.err` beside it); the probe's session transcript
  is a `.jsonl` file below `$SP/t3/home-probe/` (the isolated HOME). These scratch files are kept as they are and are
  not part of the evidence (`evidence/level-1/` holds no transcript).
- Guard snapshots, taken read-only before the probe (21:51:05Z) and after it (21:51:31Z): the sha-256 of
  `~/.claude/settings.json`, the listing of `~/.claude/projects`, and the listings of `/tmp`, `/private/tmp` and the
  user temp folder are identical, so the probe wrote nothing outside the scratchpad.

No `claude` command was run for the Level 1 consistency check, the proofs or the checklist after D14, and no Claude
output supports any statement here. A third snapshot after the T3 work (22:38:10Z) shows the same settings sha-256 and
the same `~/.claude/projects` listing. Its user temp folder listing gained five entries that the first snapshot does not
have (`BlobRegistryFiles-pRvXoGrH`, two `jlinenative-*` library files, `tomcat-docbase.8080.*` and `tomcat.8080.*`).
That folder is shared with every other process of the session; the T3 build ran with its Java temp folder in the
scratchpad and on random ports (never 8080), so the origin of those five entries is not established here. Everything
this task wrote lies in the worktree (`docs/spec-pack/level-branches/records/`) and the session scratchpad (`$SP/t3/`,
with the sandbox kept there).

## Not proven here (listed for Maik, see `rehearsal-checklist.md`, section "Level 1")

- "Rules load on demand" (what `/context` or `/memory` shows at the start and after a domain edit): needs a Claude Code
  session. R1-1.
- "Level 0 task with context" (three fresh sessions, at least two green with a `Money`-based discount in the domain and
  a new migration): needs Claude Code sessions. R1-2.
- Whether the real Claude Code `paths` matching agrees with the glob semantics assumed above, and whether a rule loads
  on a read or only on an edit of a matching file. R1-1.
- `mvnw.cmd verify` on Windows. R1-3.
- The spec 09 arrivals (C12, M7, M8) on the real `level-0-start`: the three resumed-run commands above.

## Evidence index (`evidence/level-1/`)

| File | Content |
|---|---|
| `sandbox-build.txt` | T1 script output, shas, five added paths, check-ignore, `spring-boot:help` for both modules |
| `zero-model-proofs.txt` | P1 to P7: no `@`, plain path, line count, five paths, verify tail, `BookTest` run |
| `glob-test.py.txt` | the glob test (run with `python3 -I`) |
| `glob-test-output.txt` | its output |
| `resolution-on-factory.txt` | `git grep` and `git ls-tree` results on `ae2a5f2` |
| `spelling-in-pack-plan-guide.txt` | per token: lines in pack spec 10, the workshop plan and the guide |
