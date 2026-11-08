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
- Prove the per-call strict reader by building the test's mapper with the feature DISABLED, so the mapper default
  cannot decide the outcome (QA ledger G4 pattern).
- Keep each validation guard in exactly one class. A second, accidental guard downstream (for example
  `Collectors.toUnmodifiableMap` throwing on duplicate keys in the catalogue constructor) makes QA's "guard removed"
  probe survive, which QA reports as a missing test. Use `putIfAbsent` plus `Map.copyOf` in the downstream class.
- Line-based seed files stay CRLF-neutral when tests use `String.lines()` plus `endsWith("\n")` and never check for CR.
- Data-as-test: write the seed guard test first (counts, checksums, consumer invariants), then author the data until
  it passes; generate check digits with a scratchpad program, never committed.
