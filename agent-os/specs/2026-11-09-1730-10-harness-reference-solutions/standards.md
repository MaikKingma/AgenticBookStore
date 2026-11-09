# Standards for spec 10

- `docs/spec-pack/product/architecture-standard.md` (via `agent-os/standards/architecture.md`): applies to the two
  exercise references (the multi-role ACL contract test, cancel order), which follow the Clean Hexagonal Onion layers,
  naming suffixes and test types. Bookstore Java goes through the `hexagonal-slice` skill.
- `docs/spec-pack/product/tech-stack.md`: version pins for the Level 4 tools (OpenSpec 1.16.0, Spec Kit 1.3.1,
  Agent OS main) and the build (Maven wrapper, Java 25).
- Repository rules (`CLAUDE.md` of factory): referee `./mvnw clean verify` within 60 s; never weaken a test or an
  ArchUnit rule; plain English docs without em dashes; product files ASCII only; `BOOKSTORE_AI_API_KEY` only.
