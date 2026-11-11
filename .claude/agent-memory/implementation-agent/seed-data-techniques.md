---
name: seed-data-techniques
description: Loading and guarding JSON seed files with Jackson 3 in the distributor; keeping each validation guard in one place so QA removal probes fail; CRLF-neutral file checks
metadata:
  type: reference
---
- Jackson 3 (`tools.jackson.*`, Boot 4): to name the offending key in a strict-read error, read the file as a tree
  (`jsonMapper.readTree(in)`), take `node.path("ean").asString("(no ean)")`, then convert each element with
  `jsonMapper.readerFor(T.class).with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(node)` and wrap the
  `JacksonException` (`getOriginalMessage()` keeps the property name). `asText()` is deprecated-final; use `asString`.
- Prove the per-call strict reader with a test mapper whose feature is DISABLED, so the mapper default cannot decide.
- Keep each validation guard in exactly one class. A second, accidental guard downstream (for example
  `Collectors.toUnmodifiableMap` throwing on duplicate keys in the catalogue constructor) makes QA's "guard removed"
  probe survive, which QA reports as a missing test. Use `putIfAbsent` plus `Map.copyOf` in the downstream class.
- Line-based seed files stay CRLF-neutral when tests use `String.lines()` plus `endsWith("\n")` and never check for CR.
- SQL seed census on the shared Testcontainers container without a new Spring context: extend `IntegrationTestBase`,
  autowire the `PostgreSQLContainer` bean, `CREATE DATABASE seed_x` per test class, plain Flyway with the locations read
  from the main application.yml via `YamlPropertySourceLoader`. Write the locations as ONE comma separated scalar, not a
  YAML list (`getProperty` of a list key is null). Idempotency proof = delete the `version IS NULL` history row,
  migrate again, assert `migrationsExecuted == 1` and compare the ordered table contents.
- "Keeps developer edits" rerun test: UPDATE at least one column in EVERY seeded table (ship an order: order row plus its
  stock rows; line quantity and price; book price; author name; stock flag and level), assert counts, then ONE equality
  over all tables. A deletion check is a separate test. Edits to a subset of tables let per-column `DO UPDATE SET`
  mutants survive. Pin ASCII-only SQL with a byte scan of the classpath resource.
- Growing a seed: prove "old lines unchanged" with an ordered-subsequence script plus two negative-control copies. Anchor
  count greps with `^` (`grep -c '@Test'` counts `@TestInstance`).
