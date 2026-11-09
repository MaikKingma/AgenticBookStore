# References

- `bookstore/src/main/java/nl/openvalue/bookstore/acl/DistributorAppServiceImpl.java`: `toTitleDTO` (line 142, the
  pass-through of `ean`), `toRestockDTO` (line 159, status), `genreOf` (line 172, subject code), `dateOf` (line 200),
  `translate`, `failed`, `unusable`, `unavailable` (lines 226-246).
- `bookstore/src/main/java/nl/openvalue/bookstore/domaininteraction/CatalogueFlow.java`: `importFromDistributor`
  (lines 82-97).
- `bookstore/src/main/java/nl/openvalue/bookstore/domaininteraction/DistributorAppService.java` and
  `UnavailableException.java`: the port exceptions.
- Tests: `bookstore/src/test/java/nl/openvalue/bookstore/acl/DistributorAppServiceImplTest.java` (contract helpers
  `contract`, `variant`, `stubTitle`, `stubTitleBody`, `expectRestockRequest`, `CapturedOutput`),
  `bookstore/src/test/java/nl/openvalue/bookstore/domaininteraction/CatalogueFlowTest.java`,
  `bookstore/src/test/java/nl/openvalue/bookstore/DistributorImportIntegrationTest.java`; captured contract files in
  `bookstore/src/test/resources/contracts/distributor/` (read-only for this cycle).
- Prior cycles: `agent-os/specs/2026-11-08-2031-06-distributor-acl/` (spec 06), and the bugfix layout of
  `agent-os/specs/2026-11-08-1712-fix-05-distributor-review/`.
