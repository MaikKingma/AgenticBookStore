---
bug: fix-06-distributor-review
severity: S3
path: lite
status: open
closes: [spec 06 review finding: ean not compared with the requested ISBN, finding: cause dropped, finding: CR/LF in logged distributor values]
---
# Bug: a distributor answer for another title is imported under the requested ISBN

## Report
- Observed: `POST /books/commands/import-from-distributor` with ISBN A stores a book under ISBN A with the title,
  authors, genre, publication year and recommended price of whatever title the distributor answered with. When the
  answer's `ean` is B, the catalogue gets ISBN A with B's data. Two further review findings in the same adapter: the
  original exception is dropped when the adapter turns a failure into `DistributorUnavailableException`, and
  distributor-supplied strings (`status`, date strings, subject code) are logged verbatim, so a CR or LF in a body can
  forge log lines.
- Expected: spec 06 maps the distributor's `ean` to the isbn and says "The system SHALL add a distributor title to
  the catalogue using translated data". An answer whose `ean` is not the requested ISBN is not data about the
  requested title: it is an unusable answer. The import answers 503 `distributor-unavailable` and stores nothing.
- Environment: `factory` at 824af83 (spec 06 merged), tip b12443f; reproduced in this worktree on Java 25,
  PostgreSQL 18.6 via Testcontainers.
- First seen / last known good: spec 06 review by the main session, 2026-11-08 -> regression window: not a regression
  (present since spec 06 introduced the import).
- Frequency: always, for any answer whose `ean` differs from the request.
- Impact: wrong catalogue data would be stored. No data is wrong today: every distributor seed title answers with its
  own EAN. S3.
- Related: `docs/spec-pack/specs/06-distributor-acl.md`, `agent-os/specs/2026-11-08-2031-06-distributor-acl/`.

## Reproduction
- Status: reproduced
- Recipe:
  1. On `factory` b12443f run
     `./mvnw -pl bookstore -am test -Dtest=DistributorImportIntegrationTest#importWithAPriceAboveTheRecommendedOneAndAHyphenatedIsbnStoresTheOverrideAndTheNormalisedRequestIsbn+CatalogueFlowTest#importsATitleWithTheRecommendedPriceBuiltFromTheRequestedIsbn -Dsurefire.failIfNoSpecifiedTests=false`.
  2. Both pass. The first stubs the port to answer a request for 9780141439518 with a title whose isbn is
     9789900010050 ("The Last Man", 1826, SCIENCE_FICTION) and asserts that 9780141439518 is stored as "The Last Man".
     The second does the same at the flow layer.
  3. In `DistributorAppServiceImpl.toTitleDTO` no line compares `title.ean()` with the requested `isbn`; no ACL test
     sends a body whose `ean` differs from the request (every `stubTitle(ean, file)` pairs a request with its own
     captured file).
- Evidence: both tests green on the factory tip (orchestrator run 2026-11-09T03:12Z); referee baseline
  `./mvnw clean verify` green in 17 s, bookstore 931 tests, distributor 190 tests.
- Regression-test layers: acl contract test with `MockRestServiceServer` (T1), flow test with mocked ports and the
  end-to-end integration test (T2), acl contract tests with `CapturedOutput` (T3), per the layer-testing skill.
- Test files: `bookstore/src/test/java/nl/openvalue/bookstore/acl/DistributorAppServiceImplTest.java`,
  `bookstore/src/test/java/nl/openvalue/bookstore/domaininteraction/CatalogueFlowTest.java`,
  `bookstore/src/test/java/nl/openvalue/bookstore/DistributorImportIntegrationTest.java`; run with
  `./mvnw -pl bookstore test -Dtest='<Class>#<method>' -Dsurefire.failIfNoSpecifiedTests=false`.

## Root cause
- Causal chain: a foreign title is stored under the requested ISBN -> `CatalogueFlow.importFromDistributor` builds
  the book from the requested `Isbn` and never reads `DistributorTitleDTO.isbn`
  (`bookstore/src/main/java/nl/openvalue/bookstore/domaininteraction/CatalogueFlow.java:92`) -> **the ACL passes
  the distributor's `ean` through without comparing it to the ISBN it asked for
  (`bookstore/src/main/java/nl/openvalue/bookstore/acl/DistributorAppServiceImpl.java:142`)** -> the gates missed it
  because no contract test sends an `ean` that differs from the request, and two tests pinned the wrong behaviour as
  intended (`DistributorImportIntegrationTest.java:83-104`, `CatalogueFlowTest.java:462-480`).
- Root cause (one sentence): the translation at `DistributorAppServiceImpl.java:142` treats the answer's `ean` as
  data instead of as the identity of the answer, so an answer about another title is translated as if it were about
  the requested one, and the flow at `CatalogueFlow.java:92` ignores the translated isbn.
- Contributing factors: the spec 06 translation table states the mapping `ean` -> isbn but not that it must equal the
  request; the flow Javadoc documents "built from the requested ISBN" as intended.
- Review findings in the same adapter (not causes of the defect, fixed in T3):
  - `DistributorAppServiceImpl.java:234-246`: `failed` and `translate` build the exception without the caught one,
    so the stack trace and the root cause are lost (the parse failures in `priceOf`, `quantityOf` and `dateOf` too).
  - `DistributorAppServiceImpl.java:159`, `:172`, `:200`: distributor-supplied `status`, subject code and date strings
    go verbatim into WARN lines; a CR or LF in them starts a forged log line.
- Introduced by: spec 06 (`agent-os/specs/2026-11-08-2031-06-distributor-acl/`), intended: translate the
  distributor's model at the border.

## Blast radius
- [x] Same pattern elsewhere: `grep -n "ean" DistributorAppServiceImpl.java` -> the restock answer echoes `ean`
  (captured `restock-*-response.json`), and `RestockPayload` does not read it, so a restock confirmation for another
  EAN would be accepted. deferred: `RestockConfirmationDTO` carries no ISBN and writes no catalogue data; spec 07
  builds the restock flow on `requestRestock` in parallel right now; listed for the owner.
- [x] Other callers: `grep -rn "findTitle(" bookstore/src/main/java` -> only `CatalogueFlow.importFromDistributor`.
  task T2 (defence in depth in the flow).
- [x] Persisted data already wrong: none; every distributor seed title answers with its own EAN, and no import has run
  against a misbehaving distributor. No backfill.
- [x] Artifacts produced from wrong data: none.
- [x] Tests that encode the wrong assumption: `CatalogueFlowTest` line 464 (foreign isbn, asserts the book is saved),
  line 484 and line 793 (DTO isbn differs from the request), the `lastMan` helper at line 931 (isbn 9789900010050,
  stubbed under 9780141439518 in about twenty tests); `DistributorImportIntegrationTest` line 85. task T2 (align the
  stubbed isbn with the request; add the rejection tests). ACL contract tests: every stub answers with its own EAN,
  nothing to change.
- [x] Docs: the `CatalogueFlow.importFromDistributor` Javadoc step 5 and the port's `findTitle` Javadoc. task T2.
  The pack spec's translation table: follow-up (Prevention).
- [x] Sibling environments: the distributor seed data answers with its own EAN for every title; `http/catalogue.http`
  imports seed titles only. Nothing to change.

## Fix strategy
| option | viable? | trade-off |
|---|---|---|
| (a) fix at root cause | yes | the ACL rejects a foreign `ean` with its existing "is unusable" WARN; the flow refuses a DTO for another ISBN (defence in depth) |
| (b) revert | no | nothing to revert; the import is spec 06's feature |
| (c) guarded workaround + follow-up | no | no pressure, S3 |

Chosen: (a), decided by the main session (owner-delegated gates): an answer whose `ean` does not equal the requested
ISBN is an unusable distributor answer; the import answers 503 `distributor-unavailable` and stores nothing.

## Prevention
- Gap: the ACL contract tests only ever pair a request with its own captured answer; the pack spec's translation
  table maps `ean` without saying it identifies the answer; the spec 06 review checklist had no "identity fields are
  compared with the request" item.
- Action: follow-up for the owner: add "an answer's identity field must equal the request; otherwise the answer is
  unusable" to the pack spec 06 translation rules or the architecture standard's acl section, and a contract test with
  a foreign identity to the layer-testing skill's acl checklist. The restock `ean` echo (Blast radius) belongs to the
  same follow-up.

## Verification
- Regression tests red on unfixed -> green on fixed (impl `## Red evidence`, QA `## Negative control`)
- Blast-radius items closed or deferred
- `./mvnw clean verify` green within 60 s
