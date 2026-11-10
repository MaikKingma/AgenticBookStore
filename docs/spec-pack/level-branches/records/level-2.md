# Level 2 record: consistency check (decision D14)

Branch `solution/level-2`: the Level 1 chain plus six manifest rows (`level-2/manifest.tsv`): `.mcp.json`,
`.claude/settings.json` (the plugin declaration), `docs/lsp-setup.md`, the patch of `DistributorAppServiceImplTest.java`,
the contract file `title-9789900010395.json` and the patch of the contract folder's `README.md`. No Claude Code session
was run for this record (decision D14, plan amendment A2). No `claude` command, no login, no `brew` or `yay` install
was run. Every `claude ...` string below is quoted text from a staged file, the pack or the guide, not a command that
was run. Nothing here claims a Claude result. The scenarios that need a session, and the macOS LSP rehearsal, are rows of
`rehearsal-checklist.md`, section "Level 2", for Maik's rehearsal on 2026-11-10.

## Verdict

| Check | Result |
|---|---|
| Every token of the staged Level 2 files and of the Level 2 scenarios resolves on `origin/factory`, on the level-2 sandbox or in a public source | 55 rows: 45 consistent, 3 mismatch-pack-or-guide, 7 rehearsal-only, 0 mismatch-own-file |
| Staged files spelled the same as pack spec 10, the workshop plan and the guide | yes, except the JDK floor (staged: 21; spec 10, plan and guide: 17), the `claude mcp add` URL form (literal URL against the staged variable; pack spec 05 line 159 also leaves out `--scope project`) and one pack sentence about how the contract test was built |
| Staged `.mcp.json` and plugin declaration equal the pack blocks | proven by `cmp` with controls (`evidence/level-2/pack-blocks-vs-staged.txt`) |
| `distributor` starts on a free port and `tools/list` on `/mcp` shows exactly the four tools | proven by curl (P1) |
| The default URL in `.mcp.json` equals the distributor's default port and path | proven (P2): port 8090 in `application.yml`, path `/mcp` is the Spring AI default and answers on the free-port run |
| `./mvnw clean verify` green on the level-2 sandbox | green, 1349 bookstore tests, 190 distributor tests, `ArchitectureTest` 22 tests, `DistributorAppServiceImplTest` 131 tests including the T5 test (P3) |
| `docs/lsp-setup.md` at most 40 lines | 38 lines, ASCII, no em dash, no `ENABLE_LSP_TOOL` |

No row is a mismatch in the task's own files, so there is no FAIL to report. Three rows are product findings
(PF-T6-1, PF-T6-2, PF-T6-3 below); a fourth finding, PF-QA-T6-1, adds a spelling in pack spec 05 to row M11.

## Inputs and shas

| Input | Value |
|---|---|
| Code base | `origin/factory` at `2ab37a71cf423ac3af30c21cf09c77aff4ec5a68` (the sha the sandbox was built from; `origin/factory` moved to `3916b0b` during the work, the only difference is `docs/spec-pack/orchestrator/progress.md`) |
| Staged files and T1 scripts | spec branch tip `ca194f463a55c0a107c9b5f2c51fab01986d9697` (`level-2/**`, `scripts/**`, unmodified) |
| Level-0 stand-in (T1 `simulate-level-0.sh`) | `63390c2aacddf4f7aec15bd4c13020e22dc2bf1c` |
| Level-1 commit, level-2 commit (T1 `build-level.sh level-2`) | `ef7a42f`, `0c4b31c`; the level-2 commit adds `.claude/settings.json`, `.mcp.json`, `docs/lsp-setup.md`, the contract file and changes the test and the README |
| Guide sources | `origin/sdd/11-workshop-guide/spec` at `fb958b3e59e325e2fe27cae12b18a22750fdf90e`, read-only, `guide/docs/**` |
| Pack text | `docs/spec-pack/specs/10-harness-reference-solutions.md` (Level 2 block, lines 123 to 215), `docs/spec-pack/specs/11-workshop-guide-site.md`, `docs/spec-pack/workshop/workshop-plan.md` (setup check lines 43 to 83, Level 2 lines 175 to 224) |
| Public sources | read with curl on 2026-11-10 (marketplace file, Homebrew formula, AUR package, Claude Code docs, upstream `jdtls.py` at tag v1.63.0), `evidence/level-2/external-sources.txt` |

Line numbers below: "staged" cites the target path with the line of the staged file (the same line as in the sandbox).
"spec 10" means lines of `10-harness-reference-solutions.md`, "plan" means `workshop-plan.md`, "spec 11" means
`11-workshop-guide-site.md`. Guide paths are under `guide/docs/`; the Level 2 page is `03-level-2-give-your-agent-eyes.mdx`
(written "03" below), the setup page is `00-setup-check.md` ("00"). Factory cites are `path:line` on the recorded sha.
Raw outputs: `evidence/level-2/resolution-on-factory.txt`, `evidence/level-2/spelling-in-pack-plan-guide.txt`,
`evidence/level-2/external-sources.txt`, `evidence/level-2/mcp-endpoint-default.txt`.

Status values: consistent, mismatch-own-file (would be a FAIL), mismatch-pack-or-guide (product finding for Maik),
rehearsal-only (cannot be resolved without a Claude Code session).

HR2 note: the two plugin settings keys are the marketplaces key `extraKnownMarketplaces` and the enabled plugins key
`enabledPlugins`. They were read with the file reader only, never named in a shell command. The pack block and the staged
file were compared with `cmp` on line ranges of the pack file (no key name in the command).

## Consistency table

### `.mcp.json` and the distributor

| # | Token | Named at (staged) | Resolves on factory 2ab37a7, sandbox, or command output | Pack spec 10 / Plan / Guide | Status |
|---|---|---|---|---|---|
| M1 | server name `distributor` | .mcp.json:3 | the key is the client's label; the module is `spring.application.name: distributor` (`distributor/src/main/resources/application.yml:3`); the server announces itself as `distributor-catalog` (`application.yml:7`, curl `initialize` answer `serverInfo`, P1) | spec 10 :132, :209 / plan :183, :205, :207 / guide 03 :33, :124, :135, :145, `reference/cheat-sheet.md:14`, `reference/troubleshooting.md:83` | consistent (note N1) |
| M2 | transport `http` | .mcp.json:4 | `application.yml:10` `protocol: STREAMABLE`; curl: JSON-RPC by POST, `Mcp-Session-Id` header, answers as JSON and as `text/event-stream` (P1) | spec 10 :133 / plan :207 `--transport http` / guide 03 :124, cheat-sheet :14 | consistent |
| M3 | variable `DISTRIBUTOR_MCP_URL` with default, syntax `${VAR:-default}` | .mcp.json:5 | not read by any factory file except pack spec 10 line 134 (`git grep` on the sha); the expansion is done by Claude Code, whose docs say "`${VAR:-default}`: expands to the value of VAR if VAR is set, or to default if it is unset" (`external-sources.txt` section 4); P2 shows the variable set and unset resolving to the two URLs | spec 10 :134 only / not named / not named (PF-T4-2) | consistent (runtime expansion is part of rehearsal row R2-1) |
| M4 | default URL `http://localhost:8090/mcp` | .mcp.json:5 | host and port: `application.yml:17` `port: 8090`, `:16` `address: 127.0.0.1`; path: Spring AI default `/mcp` (`mcp-endpoint-default.txt`: field `mcpEndpoint`, constructor default `/mcp`, property `spring.ai.mcp.server.streamable-http.mcp-endpoint` not set in `application.yml`), `distributor/src/test/java/nl/openvalue/distributor/mcp/McpServerIntegrationTest.java:64` `.endpoint("/mcp")`; the free-port run answered on `/mcp` and answered 404 on `/mcpx` (P1, P2) | spec 10 :134 / plan :207 / guide 03 :124, cheat-sheet :14 | consistent |
| M5 | port 8090 | .mcp.json:5 | `application.yml:17`; the jar carries the same `port: 8090` (P2); Level 1 record row C10 | spec 10 :36, :134 / plan :207 / guide 03 :124, `reference/troubleshooting.md:66` | consistent |
| M6 | host `localhost` against `address: 127.0.0.1` | .mcp.json:5 | the free-port curl used `http://localhost:<port>/mcp` and got 200 here; the server binds only 127.0.0.1 (`application.yml:16`), so a machine whose `localhost` resolves to `::1` first depends on the client trying IPv4 next | spec 10 :134 / plan :207 / guide 03 :124 all `localhost` | consistent (note N2; rehearsal row R2-1 shows it on Maik's machine) |
| M7 | exactly the four tools | pack-only (spec 10 :209) | `CatalogMcpTools.java:38 search_titles`, `:66 get_title`, `:81 list_subjects`, `:91 check_availability`; four `@McpTool(name` declarations, none other; curl `tools/list` returns exactly those four (P1, negative controls bite) | spec 10 :209 `4 tools`, :182 names `get_title` / plan :209-210 not named / guide 03 :135 `4 tools`, :159 names all four | consistent |
| M8 | `/mcp` shows `distributor` connected with 4 tools | pack-only | the server side is proven by M7 and P1; what Claude Code's `/mcp` shows needs a session | spec 10 :209 / plan :220 (`/mcp` shows the distributor connected) / guide 03 :135, :174 | rehearsal-only (R2-1) |
| M9 | `mcp__distributor__*` tool prefix | pack-only | derived by Claude Code from the key `distributor` (M1); not checkable without a session | plan :210 / guide 03 :145 | rehearsal-only (R2-1) |
| M10 | `distributor/src/main/java/.../mcp/CatalogMcpTools.java` | pack-only | the file exists on the sha (`git ls-tree`), see M7 | plan :214 (with `...`) / guide 03 :159 (full path) | consistent |
| M11 | `claude mcp add --transport http --scope project distributor http://localhost:8090/mcp` | not in the staged files | name `distributor`, transport `http`, URL (the default of M4) all agree with `.mcp.json`; the command writes a literal URL, the staged file carries `${DISTRIBUTOR_MCP_URL:-...}`; on `solution/level-2` the file and the server already exist; the CLI flags cannot be checked without running `claude` | plan :207 / guide 03 :124, cheat-sheet :14 / spec 10 not named; pack spec 05 :159 spells the command without `--scope project` (`spelling-in-pack-plan-guide.txt`, r1 addition) | mismatch-pack-or-guide (PF-T6-2, information; PF-QA-T6-1) |
| M12 | `./mvnw -pl distributor spring-boot:run` and `java -jar distributor.jar` | pack-only | `spring-boot:help -Dgoal=run` for `distributor` prints the goal and BUILD SUCCESS in the sandbox (resolution file S7); `distributor/target/distributor.jar` exists after the build and starts (P1) | plan :205 / guide 03 :116, :119, `reference/troubleshooting.md:69` | consistent |
| M13 | stdio fallback `claude mcp add distributor -- java -jar <path>/distributor/target/distributor.jar --spring.profiles.active=stdio` | pack-only (guide) | `distributor/src/main/resources/application-stdio.yml` exists on the sha; the jar name is M12 | guide `reference/troubleshooting.md:83` / not in plan or spec 10 | consistent |
| M14 | top-level key `mcpServers` | .mcp.json:2 | the name of the key under which Claude Code reads project MCP servers: the Claude Code docs page `https://code.claude.com/docs/en/mcp` (curl GET, HTTP 200, 2026-11-10) uses `mcpServers` for the file; the staged file equals the pack block (`cmp`, P4) and, loaded with `python3 -I` and `json.load` in this round, has exactly one top-level key, `mcpServers`, holding exactly one server, `distributor` | spec 10 :131 only / not named / not named; spec 05 not named (`spelling-in-pack-plan-guide.txt`, section `mcpServers`) | consistent |

### `.claude/settings.json` (plugin declaration) and the plugin

| # | Token | Named at (staged) | Resolves on factory 2ab37a7, sandbox, or command output | Pack spec 10 / Plan / Guide | Status |
|---|---|---|---|---|---|
| S1 | marketplace name `claude-plugins-official` | settings.json:3 | `.name` of the marketplace file of the named repository is `claude-plugins-official` (`external-sources.txt` section 1, HTTP 200) | spec 10 :144, :149 / plan :183, :191 / guide 03 :56 | consistent |
| S2 | source `github`, repo `anthropics/claude-plugins-official` | settings.json:4 | the repository's `.claude-plugin/marketplace.json` answers HTTP 200 (S1); the marketplace holds 317 plugins | spec 10 :145, :172 / plan :192 / guide 03 :62, cheat-sheet :13 | consistent |
| S3 | plugin id `jdtls-lsp@claude-plugins-official` (plugin name at marketplace name) | settings.json:8 | the marketplace lists a plugin named `jdtls-lsp` (version 1.0.0, description "Java language server (Eclipse JDT.LS) for code intelligence") next to the name of S1 | spec 10 :149, :162, :171 / plan :183, :191 / guide 03 :33, :56, cheat-sheet :13; spec 11 :64 | consistent |
| S4 | plugin path `./plugins/jdtls-lsp` | not in the staged files (pack prose) | the marketplace entry has `"source":"./plugins/jdtls-lsp"` exactly | spec 10 :158-159 / not named / not named | consistent |
| S5 | the plugin starts the binary `jdtls` for `.java` | pack-only | the entry has `lspServers.jdtls.command` `jdtls`, `extensionToLanguage` `.java`, `startupTimeout` 120000 | spec 10 :165, :168 / plan :187, :190 / guide 03 :41, :46 | consistent (notes N3 and N7) |
| S6 | the declaration block, as the pack gives it | settings.json:1-10 | `cmp` of pack lines 142 to 151 with the staged file: identical; one line short: differs (`pack-blocks-vs-staged.txt`); both level-2 JSON files parse; the sandbox copy equals the staged file; the file has exactly the two keys of the declaration | spec 10 :142-152, :223 (level 3 "keep its ... keys") / plan :193, :237 (the declaration lives in `.claude/settings.json`) / guide 03 :65 | consistent |
| S7 | Claude Code honours the declaration after the trust dialog and loads `jdtls-lsp` without a manual install | settings.json; lsp-setup.md:25-26 | the pack cites the official docs (spec 10 :153-160); what a real Claude Code version does needs a fresh clone and a session | spec 10 :157-160, :199-203 / plan :193 / guide 03 :65 | rehearsal-only (R2-5) |
| S8 | fallback if the declaration does not load: remove both plugin keys and document only the install command | settings.json; lsp-setup.md:22-26 | FF-T4-a: the fallback must also remove lsp-setup.md lines 25 and 26 (the plugin lines: "On this branch the plugin is also declared ... after you accept the workspace trust dialog.") and the same two keys from the level-3 settings file staged by T7; that file is not staged yet on this branch (`ls level-3` finds nothing), so T7 and T10 carry it | spec 10 :160-162, :202-203, :223 / not named / not named | rehearsal-only (R2-5, T7) |
| S9 | the three docs URLs the pack cites for the declaration: `https://code.claude.com/docs/en/settings-reference#enabledplugins`, `https://code.claude.com/docs/en/settings-reference#extraknownmarketplaces`, `https://code.claude.com/docs/en/plugins/org#require-plugins-per-repository` | not in the staged files (pack prose) | curl GET on 2026-11-10: the two pages answer HTTP 200 and each of the three anchor ids exists once in its page (`spelling-in-pack-plan-guide.txt`, r1 addition). Read as a citation of the pack, not as proof that Claude Code honours the declaration (that is S7) | spec 10 :154-156 only (the plan, the guide, the staged settings file and `docs/lsp-setup.md` do not cite them) / not named / not named | consistent |

### `docs/lsp-setup.md`

| # | Token | Named at (staged) | Resolves on factory 2ab37a7, sandbox, or command output | Pack spec 10 / Plan / Guide | Status |
|---|---|---|---|---|---|
| L1 | built-in read-only LSP tool: go to definition, find references, type info, document and workspace symbols, implementations, call hierarchy; diagnostics after every edit | lsp-setup.md:5-6 | the docs page for code intelligence describes the plugin table and the diagnostics line (`external-sources.txt` section 4); the capability list is the pack's wording | spec 10 :165-167 / plan :187-189 / guide 03 :41 | consistent |
| L2 | no enable flag; `ENABLE_LSP_TOOL` absent | lsp-setup.md:7 | `grep -c ENABLE_LSP_TOOL` prints 0 on the sandbox file | spec 10 :167 ("No enable flag exists or is needed") / not named / not named | consistent |
| L3 | JDK floor: "JDK 21 or later (the upstream `jdtls` wrapper refuses older Java)" | lsp-setup.md:11 | upstream wrapper `jdtls.py` at tag v1.63.0, lines 39-40: `if java_major_version < 21:` then `raise Exception("jdtls requires at least Java 21")`; the AUR package `jdtls` 1.63.0-1 depends on `java-runtime>=21`; the Homebrew formula depends on the unversioned `openjdk` (`external-sources.txt` sections 2, 3 and the wrapper section) | spec 10 :168 says "JDK 17 or later"; plan :59 says "it needs JDK 17 or later, participants have 25"; guide 00 :92 says "`jdtls` needs JDK 17 or later, and you have 25" | mismatch-pack-or-guide (PF-T6-1, the staged file is right; harmless for Java 25) |
| L4 | `brew install jdtls` | lsp-setup.md:13 | the Homebrew formula `jdtls` exists, stable 1.63.0 (`external-sources.txt` section 2); not installed by this check | spec 10 :168 / plan :63 / guide 00 :98; spec 11 :61 | consistent |
| L5 | `yay -S jdtls` | lsp-setup.md:14 | the AUR package `jdtls` exists, version 1.63.0-1 (section 3); not installed | spec 10 :169 / plan :64 / guide 00 :105 | consistent |
| L6 | other Linux: extract a JDT LS release, put a `jdtls` wrapper script on the `PATH` | lsp-setup.md:15 | the wrapper script exists upstream (`org.eclipse.jdt.ls.product/scripts/jdtls.py`, HTTP 200) | spec 10 :169 / plan :65 / guide 00 :111; spec 11 :61 | consistent |
| L7 | Windows: use WSL | lsp-setup.md:16 | not checkable here (decision D8: Linux and WSL are Maik's manual check) | spec 10 :169 / plan :66 / guide 00 :115, `reference/troubleshooting.md:42` | rehearsal-only (R2-7) |
| L8 | `which jdtls`, PowerShell `Get-Command jdtls` | lsp-setup.md:18 | `jdtls` is not on the PATH of this check (information, S8 of the resolution file); the commands are spelled the same everywhere | spec 10 :170 / plan :54, :190 / guide 00 :59, :66, :161, 03 :46, `reference/troubleshooting.md:29,36`; spec 11 :63 | consistent |
| L9 | `/plugin install jdtls-lsp@claude-plugins-official` | lsp-setup.md:22 | id as S3; running the slash command needs a session | spec 10 :162, :171 / plan :183, :191 / guide 03 :33, :56, cheat-sheet :13; spec 11 :64 | consistent |
| L10 | `/plugin marketplace add anthropics/claude-plugins-official` | lsp-setup.md:23 | repo as S2 | spec 10 :172 / plan :192 / guide 03 :62, cheat-sheet :13 | consistent |
| L11 | the **Installed** tab of `/plugin` | not in the staged file | needs a session | plan :183, :194 / guide 03 :33, :65, cheat-sheet :12 / spec 10 :202 says only "`/plugin` lists `jdtls-lsp` as enabled" | rehearsal-only (R2-4, R2-5) |
| L12 | `Found N new diagnostic issues ...` | lsp-setup.md:31 | the docs page prints "Found N new diagnostic issues in M files (ctrl+o to expand)" under the edit (`external-sources.txt` section 4); the staged text shortens it with "..." | spec 10 :174 / plan :198 / guide 03 :87 (all the same shortened form) | consistent |
| L13 | deliberate error "assign a `String` to an `int`" (a different error from the guide's) | lsp-setup.md:30 | free text for humans; the guide's error is K2 | spec 10 :174 ("a deliberate type error in a `.java` file") / plan :197 / guide 03 :84 | consistent (note N4) |
| L14 | `/plugin` tab **Errors** | lsp-setup.md:35 | the docs page says "run /plugin and open the Errors tab" (section 4) | spec 10 :175 / plan :200 / guide 03 :101, `reference/troubleshooting.md:44` | consistent |
| L15 | `claude --debug` | lsp-setup.md:35 | a CLI flag, cannot be checked without running `claude` | spec 10 :175 / plan :200 / guide 03 :104, `reference/troubleshooting.md:47`, cheat-sheet :14 | rehearsal-only (R2-3) |
| L16 | error text `Executable not found in $PATH: "jdtls"` | lsp-setup.md:36 | the docs page shows `Executable not found in $PATH: "<binary>"` in the Errors tab; the binary of the plugin is `jdtls` (S5), so the text for this plugin is exactly the staged string; the live text needs a PATH without `jdtls` and a session | spec 10 :176 / plan :201 / guide 03 :107, `reference/troubleshooting.md:50` | consistent (live text: optional control in R2-3) |
| L17 | link https://code.claude.com/docs/en/plugins/code-intelligence | lsp-setup.md:38 | HTTP 200 on 2026-11-10 (section 4) | spec 10 :177 / not named / not named | consistent |
| L18 | at most 40 lines, ASCII, no em dash | whole file | `wc -l` prints 38; non-printable bytes 0; em or en dash 0 (resolution file S2) | spec 10 :164 ("at most 40 lines") / spec 11 :66 (no em dashes) | consistent |

### Code tokens named by the Level 2 scenarios and the guide

| # | Token | Named at | Resolves on factory 2ab37a7 | Pack spec 10 / Plan / Guide | Status |
|---|---|---|---|---|---|
| K1 | `Money.of`, used in at least two layers | pack scenario "Find references across layers" | definitions `bookstore/src/main/java/nl/openvalue/bookstore/domain/Money.java:34, :45, :49` (three overloads: `String`, `BigDecimal`, `BigDecimal` plus currency). Layer `domain`: `Book.java:16`, `Book.java:17`, `Order.java:108`. Layer `domaininteraction` (flows and mappers): `BookDomainMapper.java:27`, `CatalogueFlow.java:60, :86, :100, :110`, `OrderDomainMapper.java:26, :44`. Tests: `BookTest`, `MoneyTest`, `OrderTest`, `BookDomainMapperTest`, `CatalogueFlowTest`, `OrderDomainMapperTest`, `OrderFlowTest` (seven files). No call in `command`, `query`, `data`, `acl`, `assistant`. No method reference `Money::of` | spec 10 :196-197 ("at least two layers") / plan :195-196 ("spans several layers (domain, flows and mappers, tests)") / guide 03 :72, :75 | consistent (two main layers plus tests; note N5 on overloads) |
| K2 | `Book.java`, `price()` returning `Money` (the guide's type-error step) | pack scenario "Type error is reported and fixed" | `domain/Book.java:91` `public Money price() {`, body `return price;` at line 92, field `private Money price;` at line 25; `Book.java` is the only file of that name on the sha; callers that a `String` return type would break: `domaininteraction/BookDTO.java:23` (`book.price().amount()` and `.currency()`), `BookTest` (12 lines with `price()`, `git grep -c` on the sha), `BookDomainMapperTest.java:70` (one more line, 13 test lines in all) | spec 10 :189-192 (a type error in a bookstore domain class) / plan :197 / guide 03 :84 | consistent |
| K3 | the T5 test `keepsOnlyTheAuthorOfATitleWithATranslatorAndAnIllustrator` in `DistributorAppServiceImplTest` | pack scenario "ACL contract test from real MCP data" | sandbox `DistributorAppServiceImplTest.java:274-275` (patch row; the assertions are lines 280 to 282), Surefire XML: `testcase name="keepsOnlyTheAuthorOfATitleWithATranslatorAndAnIllustrator"`, suite 131 tests, 0 failures, 0 errors, 0 skipped (P3); asserts `containsExactly("Dante Alighieri")` and `doesNotContain("Henry Wadsworth Longfellow", "Gustave Dore")` (the added test, patch hunk `@@ -271,6 +271,17 @@`) | spec 10 :180-183, :213 ("asserts that only authors are imported") / plan :211-213 / guide 03 :152 | consistent (note N6) |
| K4 | contract file `title-9789900010395.json` under `bookstore/src/test/resources/contracts/distributor/` | spec 10 :181 | contents: ean 9789900010395, "The Divine Comedy: Inferno", roles AUTHOR, TRANSLATOR, ILLUSTRATOR, subject POE000000, 3499 cents, IN_STOCK, 1867-01-01; README row line 34 states the same values; byte identity of the file with the REST body and the MCP `get_title` text is T5's evidence (`evidence/level-2/acl/capture-and-byte-identity.txt`) | spec 10 :181-182 / plan :212 ("that exact payload shape") / guide 03 :152 | consistent |
| K5 | README claims of the contract folder | README.md:7, :9, :16, :34 | line 16 says the file "was captured on 2026-11-10 with curl from a distributor on a free port: the REST body and the MCP `get_title` text were identical bytes"; the T5 capture shows `Date: Mon, 09 Nov 2026 23:31:04 GMT`, which is 2026-11-10 00:31 in Europe/Amsterdam (UTC+1), the time zone the README names in line 7; `http/distributor.http` named in line 9 exists on the sha | not in spec 10 / plan / guide | consistent |
| K6 | `DistributorAppServiceImpl`, `MockRestServiceServer` | pack text | `bookstore/src/main/java/nl/openvalue/bookstore/acl/DistributorAppServiceImpl.java` exists; `DistributorAppServiceImplTest.java:56, :78, :84` use `MockRestServiceServer` | spec 10 :180-181 / plan :212 / guide 03 :152, `reference/architecture.md:38` | consistent |
| K7 | stretch tool `list_new_releases(days)` | pack text | no hit on the sha (absent on purpose, participants add it) | plan :216 / guide 03 :163 | consistent |
| K8 | spec 10 sentences "whose shape was taken from the distributor MCP" and "Built in Claude Code with the distributor MCP connected" | pack text | under D14 and amendment A2 the payload was captured with curl, over REST and over MCP `tools/call get_title`, and the contract file equals both; the README says curl; the pack sentence "Built in Claude Code" is no longer true | spec 10 :182-183 / plan :211-213 (a workshop prompt, still true as a participant task) / guide 03 :152 (same) | mismatch-pack-or-guide (PF-T6-3, wording only) |
| K9 | branch name `solution/level-2` and its content list ("`solution/level-1` plus `.mcp.json`, the MCP-assisted ACL contract test, the LSP setup note and the plugin declaration") | spec 10 :123-126 | the six manifest rows are exactly those five items; the branch itself is cut later by the README procedure; the sandbox is its stand-in | spec 10 :123, :186, :190, :195, :200, :212 / plan :180, :193 / guide 03 :25, :169 | consistent |
| K10 | the GIVEN "the distributor is running" | spec 10 :207 | the jar starts in about one second and serves `/actuator/health`, `/mcp` (P1) | spec 10 :207 / plan :205 / guide 03 :113-119 | consistent |
| K11 | the ten JSON keys of the contract file `title-9789900010395.json`: `ean`, `titleName`, `contributors` (with `name`, `role`), `subjectCodes`, `format`, `listPriceCents`, `currency`, `availability`, `warehouseQuantity`, `releaseDate` | the contract file, line 1 | `distributor/src/main/java/nl/openvalue/distributor/catalog/Title.java:10-19` is the record of exactly these ten components in this order (its Javadoc says "exactly the ten keys"); `Contributor.java:3` `record Contributor(String name, ContributorRole role)`; the bookstore side reads the same names: `DistributorAppServiceImpl.java:135-137` (`titleName`, `contributors`, `listPriceCents`), `:291-292` (`TitlePayload`), `:144` `"AUTHOR".equals(contributor.role())`; `CatalogMcpTools.java:42-44, :69-71` name them in the tool descriptions | spec 05 :33-41 shows the same keys (`spelling-in-pack-plan-guide.txt`, r1 addition); spec 10, plan and guide do not list them | consistent |
| K12 | the enum values in the contract file: `HARDCOVER` (format), `EUR` (currency), `IN_STOCK` (availability), `AUTHOR`, `TRANSLATOR`, `ILLUSTRATOR` (roles) | the contract file, line 1 | `catalog/Format.java:4` `HARDCOVER`; `catalog/Availability.java:4` `IN_STOCK`; `catalog/ContributorRole.java:4-7` `AUTHOR`, `EDITOR`, `TRANSLATOR`, `ILLUSTRATOR` (the enum has four roles; the contract uses three, see N6; spec 05 :44 and `CatalogMcpTools.java:69` name them too); `EUR`: `distributor/src/main/resources/catalog/titles.json` holds the currency `"EUR"` on 150 lines (`git grep -c` on the sha); the same file carries `HARDCOVER` as the format on 32 lines | spec 05 :38 `"currency": "EUR"`, :44 `format: HARDCOVER, PAPERBACK, EBOOK`; spec 10, plan and guide do not name them | consistent |
| K13 | identifiers in the added lines of the test patch: `stubTitle`, `findTitle`, `DistributorTitleDTO`, `title()` and `authors()` of that record, `orElseThrow`, `assertThat`, `isEqualTo`, `containsExactly`, `doesNotContain` | `DistributorAppServiceImplTest.java.patch` lines 9-18 | `DistributorAppServiceImplTest.java:915` `private void stubTitle(String ean, String contractFile)` (on the sha); `DistributorAppService.java:20` `Optional<DistributorTitleDTO> findTitle(String isbn)`, implemented at `DistributorAppServiceImpl.java:90`; `DistributorTitleDTO.java:12` `record DistributorTitleDTO(String isbn, String title, List<String> authors, ...)`; `orElseThrow` is `java.util.Optional`, `assertThat`, `isEqualTo`, `containsExactly`, `doesNotContain` are AssertJ methods the test class already uses; the patched class compiles and passes on the sandbox (P3: 131 tests, 0 failures) | spec 10 :180-183 describes the test in words / plan :211-213 / guide 03 :152 | consistent |
| K14 | tokens of the README patch and of `manifest.tsv`: `unavailable-503.json`, `--distributor.chaos.failure-rate=1.0`, `GET /api/titles/{ean}`, `http/distributor.http`, the capture dates 2026-11-08 and 2026-11-10, the two base blob ids of the patches (`60d9d896...` for the test, `52a2b6b7...` for the README), the modes `644`, the six staged and target paths (staged: `files/dot-mcp.json`, `files/dot-claude/settings.json`, `files/docs/lsp-setup.md`, `patches/.../DistributorAppServiceImplTest.java.patch`, `files/.../title-9789900010395.json`, `patches/.../README.md.patch`), the diff headers `a/<target>` and `b/<target>` with the `index` lines (`60d9d89..4923e01`, `52a2b6b..9458432`), the groups `config` and `exercise`. The unchanged context lines of the two patches (the pre-existing body of the README and of the test class, for example the table rows of the other title files, the years, `FIC...` prefixes, `outOfPrintTitleIsNotOrderable`, `CONFIRMED`, `BACKORDERED`) are not part of this change and are not tokens here | `README.md.patch` lines 9-37; `manifest.tsv` lines 2-7 | `bookstore/src/test/resources/contracts/distributor/unavailable-503.json` exists on the sha; the property: `ChaosProperties.java:22` names `distributor.chaos.failure-rate`, `application.yml:22` `failure-rate: 0.0`; route: `TitlesController.java:17` `@RequestMapping("/api")` and `:44` `@GetMapping("/titles/{ean}")`; `git rev-parse 2ab37a7:<path>` for the test file prints `60d9d896ed4a9e5fb16e9c94a4547c01eca5c3a7` and for the README `52a2b6b7b73edaafca03da2e8f6c4ae07ffabe3b`, the two blob ids of the manifest; the post-image blobs of the `index` lines equal the blobs of the two files in the level-2 sandbox commit (`git rev-parse HEAD:<path>` prints `4923e01a674d8c7d214a94b3518a6eb80afa5a7d` and `9458432417e2162f0e2c96427ab5a9fc50facf3b`); `http/distributor.http` and the capture dates are K5; the six rows applied cleanly on the sandbox (P0: six `name-status` rows, patches applied) | not in spec 10, plan or guide | consistent |

Row count: 14 + 9 + 18 + 14 = 55 rows: 45 consistent, 3 mismatch-pack-or-guide (M11, L3, K8), 7 rehearsal-only (M8, M9,
S7, S8, L7, L11, L15), 0 mismatch-own-file. The counts come from a `grep` and `awk` pass over the last column of the
table rows of this file (r1 run, same method as r0).

### Notes on rows

- N1 (M1): the key `distributor` in `.mcp.json` is what Claude Code shows in `/mcp` and uses in `mcp__distributor__*`.
  The server's own name in the `initialize` answer is `distributor-catalog`. Neither document claims they are equal.
- N2 (M6): `localhost` in the URL, `127.0.0.1` in the server. It worked here with curl. On a machine where `localhost`
  resolves to `::1` first, a client that does not try IPv4 afterwards would fail to connect. Row R2-1 records what
  Maik's machine does; the staged files follow the pack.
- N3 (S5): the plugin entry has `startupTimeout` 120000 (milliseconds). The first start of `jdtls` on a large Maven
  multi-module build may approach that value; the rehearsal row R2-4 records the time to the first answer.
- N4 (L13): `docs/lsp-setup.md` suggests assigning a `String` to an `int`, the guide suggests changing `price()` to
  return `String`. Both are deliberate type errors and both are fine; they are different examples on purpose.
- N5 (K1): `Money.of` has three overloads. A references query started on one overload lists only that overload's
  callers. The reference counts for the rehearsal row, per overload, are in `resolution-on-factory.txt` (R5): ten main
  call sites in five files (three in `domain`, seven in `domaininteraction`). A query by name over the workspace should
  list all ten plus the test sites.
- N6 (K3): the contract has no EDITOR contributor, so a filter that keeps AUTHOR and EDITOR, or one that drops only
  TRANSLATOR and ILLUSTRATOR, also passes this one test (follow-up FU-T5-1, decision J14, unchanged by this task).
- N7 (S5, L1): when the language server starts. The Claude Code docs (code intelligence page, step "Confirm the server
  starts", `claude-docs-pages.txt` r2 addition) say: "The language server starts the first time Claude edits a file
  with one of the plugin's extensions." The earlier research probe (res-1, finding F7) logged the start attempt at the
  first `.java` edit, not before. No source says that a read-only request such as find references starts the server.
  The workshop plan Part A step 3 and guide 03 line 72 put "Find all references to `Money.of`." before the first
  edit, so that order may find no running server. The pack and the staged files do not state a start trigger, so no
  row is a mismatch; the checklist rows R2-3 (branch "No server running yet") and R2-4 (Measure 1 start rule) record
  what happens, and the observation is a possible note for the guide.

## Product findings for Maik

- PF-T6-1 (confirms PF-T4-1): the JDK floor of `jdtls` is 21. Evidence: the upstream wrapper at v1.63.0 raises "jdtls
  requires at least Java 21" (`jdtls.py:39-40`), the AUR package depends on `java-runtime>=21`. Three texts still say
  17: `10-harness-reference-solutions.md` line 168, `workshop-plan.md` line 59, and the guide page `00-setup-check.md`
  line 92 (`origin/sdd/11-workshop-guide/spec`). `docs/lsp-setup.md` line 11 says 21 (decision J8). Suggested fix:
  "JDK 21 or later" in those three places. Participants on Temurin 25 are not affected.
- PF-T6-2 (confirms PF-T4-2): `workshop-plan.md` line 207, guide `03-level-2-give-your-agent-eyes.mdx` line 124 and
  `reference/cheat-sheet.md` line 14 tell participants to run `claude mcp add --transport http --scope project
  distributor http://localhost:8090/mcp`, which writes a literal URL into `.mcp.json`. The staged `.mcp.json` of
  `solution/level-2` carries `${DISTRIBUTOR_MCP_URL:-http://localhost:8090/mcp}`, and on that branch the server entry
  already exists. Name, transport and URL agree. Suggested guide note: on `solution/level-2` the file is already there.
  No file change in spec 10 scope.
- PF-T6-3: `10-harness-reference-solutions.md` lines 182 and 183 say the contract shape "was taken from the distributor
  MCP" and "Built in Claude Code with the distributor MCP connected". Under D14 the payload was captured with curl (REST
  and MCP `tools/call get_title`, byte-identical); the staged README says so. Suggested fix: reword the sentence to
  "captured with curl from the distributor, over REST and over the MCP tool". Wording only.
- PF-QA-T6-1 (found by the QA verifier in round r0, verified here): `docs/spec-pack/specs/05-distributor-service.md`
  line 159 says "the tester runs `claude mcp add --transport http distributor http://localhost:8090/mcp`" without
  `--scope project`. Without that flag the command adds the server at local scope and does not write `.mcp.json` (the
  Claude Code MCP docs page, read with curl: "Each command writes to local scope unless you add --scope project or
  --scope user."); the
  plan (line 207), guide page 03 (line 124) and the cheat sheet (line 14) carry `--scope project`. Same pack, two
  spellings (row M11). Suggested fix: add `--scope project` in spec 05 line 159. No file change in spec 10 scope.
- Information, no fix needed: the marketplace entry for `jdtls-lsp` declares `startupTimeout` 120000 (N3); the docs
  page prints the diagnostics line as "Found N new diagnostic issues in M files (ctrl+o to expand)", the pack and guide
  shorten it to "Found N new diagnostic issues ..."; the first `jdtls` start keys its workspace data on the folder name
  (R2-4).

## Zero-model proofs

All run without any Claude process, on the level-2 sandbox built with the T1 scripts. Environment: `TMPDIR` and
`JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=...` point into the session scratchpad; the sandbox lives in the scratchpad; the
shared `~/.m2` cache and Docker were used (J10). Evidence is masked while it is written (paths appear as `$SP`, `$WT`).

### P0: sandbox build (`evidence/level-2/sandbox-build.txt`)

```
$ bash docs/spec-pack/level-branches/scripts/simulate-level-0.sh origin/factory $SBX
simulated level-0: $SP/t6/tmp/sbx.y9Ncq8/level-2 at 63390c2aacddf4f7aec15bd4c13020e22dc2bf1c (from origin/factory 2ab37a71cf423ac3af30c21cf09c77aff4ec5a68)
$ bash docs/spec-pack/level-branches/scripts/build-level.sh level-2 $SBX
build-level: committed level-1 as ef7a42f: Level 1 solution: context engineering
apply-level: level-2: 6 rows applied (group: all); changes left uncommitted in $SP/t6/tmp/sbx.y9Ncq8/level-2
build-level: committed level-2 as 0c4b31c: Level 2 solution: give your agent eyes (LSP + MCP)
$ git diff --name-status HEAD~1 HEAD
A	.claude/settings.json
A	.mcp.json
M	bookstore/src/test/java/nl/openvalue/bookstore/acl/DistributorAppServiceImplTest.java
M	bookstore/src/test/resources/contracts/distributor/README.md
A	bookstore/src/test/resources/contracts/distributor/title-9789900010395.json
A	docs/lsp-setup.md
```

### P1: `initialize` and `tools/list` on `/mcp`, free port (`evidence/level-2/mcp-initialize-tools-list.txt`)

The distributor jar of the sandbox was started with `java -jar distributor/target/distributor.jar --server.port=<free>`
on a port taken from the operating system (never 8080 or 8090; those two were not opened or probed). Only that pid
was stopped, and the port was free again afterwards.

```
free port: 59098 (not 8080, not 8090)
Tomcat started on port 59098 (http) with context path '/'
Started DistributorApplication in 1.02 seconds (process running for 1.181)
with the variable set: http://localhost:59098/mcp
with the variable unset: http://localhost:8090/mcp   (not contacted: that port is the workshop port)

## 4. initialize          HTTP/1.1 200, Content-Type: application/json
serverInfo: {"name":"distributor-catalog","version":"1.0.0"}
protocolVersion: 2025-06-18
capabilities.tools: {"listChanged":true}

## 6. tools/list          HTTP/1.1 200, Content-Type: text/event-stream
tool count: 4
  "check_availability"  required: ean
  "get_title"  required: ean
  "list_subjects"  required: 
  "search_titles"  required: query
check: exactly the expected set
check: DIFFERS from the expected set     (negative control: one name misspelled)
check: DIFFERS from the expected set     (negative control: one name missing)

POST /mcpx -> HTTP 404
GET /actuator/health -> HTTP 200
stopped pid 96347 (own process only)
port 59098 is free again
```

The listing is the evidence file's content, shortened. An earlier run of the same script (ports 57927 and 58144) gave
the same answers; the evidence file holds the last run.

### P2: the default of `.mcp.json` against the distributor config

```
url spec: ${DISTRIBUTOR_MCP_URL:-http://localhost:8090/mcp}
env variable: DISTRIBUTOR_MCP_URL
default URL:  http://localhost:8090/mcp
default port: 8090
default path: /mcp
17:  port: 8090                        (distributor/src/main/resources/application.yml)
port check: default URL port equals distributor server.port
17:  port: 8090                        (the same file inside distributor/target/distributor.jar)
0                                      (mcp-endpoint or streamable-http in application.yml: not set, default applies)
```

The path default is read from the dependency's bytecode (`mcp-endpoint-default.txt`): property prefix
`spring.ai.mcp.server.streamable-http`, field `mcpEndpoint`, constructor default `/mcp`. P1 shows the path answers on the
free port and a different path answers 404. The jar was not started without `--server.port` because its default port
is the workshop port 8090.

### P3: build

```
$ ./mvnw clean verify          (sandbox HEAD 0c4b31c, 00:32:03Z to 00:32:33Z UTC, exit code 0)
[INFO] Tests run: 1349, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 190, Failures: 0, Errors: 0, Skipped: 0
[INFO] frontend ........................................... SUCCESS [  8.007 s]
[INFO] bookstore .......................................... SUCCESS [ 16.013 s]
[INFO] distributor ........................................ SUCCESS [  4.572 s]
[INFO] BUILD SUCCESS
[INFO] Total time:  28.785 s
[INFO] Tests run: 131, Failures: 0, Errors: 0, Skipped: 0 ... -- in nl.openvalue.bookstore.acl.DistributorAppServiceImplTest
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0 ... -- in nl.openvalue.bookstore.ArchitectureTest
```

The Surefire XML of that run names the T5 test and shows no failure, error or skipped element for it (resolution file
S5). The bookstore total is 1349 here against 1314 in the Level 1 record because that record was built from an earlier
factory sha.

### P4: pack blocks against the staged files

`cmp` of the pack's `.mcp.json` block (spec 10 lines 130 to 137) and plugin declaration block (lines 142 to 151) with the
staged files prints identical; the controls (one line short) differ (`pack-blocks-vs-staged.txt`).

### P5: public sources (`evidence/level-2/external-sources.txt`)

Read with curl, nothing installed: marketplace entry of `jdtls-lsp` (S1 to S5), Homebrew formula `jdtls` (L4), AUR
package `jdtls` (L5), the docs sentence for `${VAR:-default}` (M3), the code intelligence page (L12, L14, L16, L17),
the upstream wrapper `jdtls.py` v1.63.0: Java floor 21 (L3) and the workspace data folder
`~/Library/Caches/jdtls/jdtls-<sha1 of the folder's basename>` on macOS (used by row R2-4; the sha-1 command of the row
is shown to equal what the script computes). The wrapper uses that folder on Darwin only (line 89 and 90); on Linux it
is `~/.cache/jdtls/jdtls-<sha1 of the basename>` (line 91 `elif system == 'Linux' and 'HOME' in os.environ:`, line 92
`cachedir = Path(os.environ['HOME']) / '.cache'`, line 99 for the folder name), which row R2-7 uses for Linux and WSL.
The R2-7 command was run in round r1 against a scratch home folder and a folder named `l2-lsp-example`: it printed
`No such file or directory` for `~/.cache/jdtls/jdtls-56b7b190295d8dd20fbf72b592749b1f9d12aeae`, the same digest that
python `hashlib.sha1` gives for that name; with the folder created, the same expression lists it (control). It was run
on macOS (a `sha1sum` is present there), not on Linux or WSL; running it there is the row.

### Scope guard

No `claude` command was run for this task. A search of this record and the Level 2 evidence for strings of the form
`claude mcp add`, `claude plugin ...` and the like finds only quoted text: the staged, pack, plan and guide lines in the
spelling evidence (`claude mcp add` in plan line 207 and guide pages), table rows M11 and M13 and finding PF-T6-2 of this
record, and one sentence of the Claude Code docs in `claude-docs-pages.txt`. No evidence file holds a transcript or a
`.jsonl` stream. The
leak grep over the level-branches folder (patterns: the home folder prefix, the user name, the scratchpad folder
prefix, the API key prefix and two token names) prints nothing, and the same grep over a raw, unmasked evidence file
does hit (positive control, 4 lines). `ANTHROPIC_API_KEY`
was not set, read or written (the run scripts unset it). The scratch distributor process was stopped by its own pid.

## Not proven here (listed for Maik, see `rehearsal-checklist.md`, section "Level 2")

- "MCP is available": what `/mcp` shows, the project-server approval, the `mcp__distributor__*` names. R2-1.
- "Type error is reported and fixed". R2-2.
- "Find references across layers" (`Money.of`, no text search). R2-3.
- The macOS LSP rehearsal: first-index time on a cold cache, the multi-module result. R2-4.
- "Plugin loads from the project declaration", with the pack fallback and its two companion edits. R2-5.
- "ACL contract test from real MCP data" by `./mvnw verify` on the cut branch: green on the sandbox (P3); the row repeats it on the real branch. R2-6.
- Linux and Windows through WSL (decision D8). R2-7.
- The level-3 settings file staged by T7 must carry the same two plugin keys that the fallback would remove (S8).

## Evidence index (`evidence/level-2/`)

| File | Content |
|---|---|
| `sandbox-build.txt` | T1 script output, shas, the six changed paths |
| `sandbox-verify.txt` | `./mvnw clean verify` on the sandbox, totals and per-class lines |
| `mcp-initialize-tools-list.txt` | P1 and P2: free port, `initialize`, `tools/list`, controls, stop |
| `mcp-endpoint-default.txt` | the Spring AI default of the MCP endpoint property, from bytecode |
| `external-sources.txt` | marketplace entry, Homebrew formula, AUR package, one docs sentence and the code intelligence page, upstream wrapper `jdtls.py` v1.63.0 (Java floor, data folder, sha-1 command check) |
| `claude-docs-pages.txt` | Claude Code docs lines the rehearsal rows lean on: project MCP approval, `/plugin uninstall`, the **Installed** tab, the LSP tool, where auto memory lives and what it is keyed by, and (r2 addition at the end) the start trigger of the language server, "The language server starts the first time Claude edits a file with one of the plugin's extensions" (note N7; non-ASCII glyphs of the pages are shown as `???`, noted in the file's second line) |
| `resolution-on-factory.txt` | `git grep` and `git ls-tree` results on `2ab37a7` and checks on the sandbox |
| `spelling-in-pack-plan-guide.txt` | per token: lines in pack spec 10, the workshop plan, other pack text and the guide; the r1 addition at the end adds pack spec 05 to the search (all r0 tokens that it names, and the tokens of rows M14, S9, K11, K12), and the public docs status lines for M14 and S9 |
| `pack-blocks-vs-staged.txt` | `cmp` of the two pack JSON blocks with the staged files, with controls |
| `acl/**` | T5 evidence (unchanged by this task) |
