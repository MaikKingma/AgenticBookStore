<!--
Sync Impact Report
Version change: none -> 1.0.0 (initial ratification)
Modified principles: none (five principles added)
Added sections: Core Principles I to V, Technology Constraints, Development Workflow, Governance
Removed sections: none
Follow-up TODOs: none
-->
# Agentic Bookstore Constitution

## Core Principles

### I. Onion Layers
- Code follows docs/architecture.md.
- `ArchitectureTest` MUST stay green and its rules MUST NOT be weakened.
- The layer rules are enforced by ArchUnit, never by convention alone.

### II. Tests First, Per Layer
- Tests MUST come first, per layer.
- Every new behaviour has a test in the layer where the behaviour lives.
- There MUST be one test per acceptance scenario; every requirement scenario in a spec maps to at least one test.

### III. Exact Money
- Money is exact: it is always `Money` (`BigDecimal`, scale 2, HALF_EVEN), never floating point.
- Never use `double` or `float` for money.

### IV. Schema Through New Migrations
- Schema changes MUST go only through new Flyway migrations in `bookstore/src/main/resources/db/migration`.
- An existing migration is never edited.

### V. Small, Verified Changes
- Changes MUST be small and scoped: stay inside the scope of the request.
- `./mvnw verify` MUST pass before every commit; always use the Maven wrapper, never `mvn`.

## Technology Constraints
- The build is Java 25 and Spring Boot 4.1.2 through the Maven wrapper (`./mvnw`), in the modules `frontend`, `bookstore` and `distributor`.
- PostgreSQL is the only database: no H2, no embedded or in-memory database, tests included.
- No Lombok and no MapStruct: records and small hand-written mappers.

## Development Workflow
- The definition of done is `./mvnw verify` green, including `ArchitectureTest`; show the last lines of the output.
- Never say tests pass without having run them in this session.
- Commits are small and use conventional commit messages (feat:, fix:, test:, refactor:, docs:).

## Governance
- Compliance is checked by `ArchitectureTest` (principle I) and `./mvnw verify` (principle V).
- The constitution is amended with `/speckit-constitution`; each amendment bumps the version by semantic versioning: MAJOR for backward incompatible removals or redefinitions of principles, MINOR for a new or materially expanded principle or section, PATCH for clarifications.

**Version**: 1.0.0 | **Ratified**: 2026-11-10 | **Last Amended**: 2026-11-10
