---
name: slice-pattern-decisions
description: Rationale for the cross-slice design choices set at spec 02 (catalogue) and extended at spec 03 (inventory) that later bookstore slices mirror; use when shaping any bookstore slice
applies-to:
  - bookstore/src/main/java/nl/openvalue/bookstore/**
  - bookstore/src/main/resources/db/migration/**
metadata:
  type: project
---

Status: the spec 02 and spec 03 bullets landed in code (checked 2026-11-07 against `CatalogueFlow`, `InventoryFlow`,
`StockItemDTO`, `QueryExceptionHandler`); the spec 04 bullets are proposals in the spec 04 plan under `agent-os/specs/`,
so verify them there and in the code before relying on them.

- **Mappers are static final classes** (`BookDomainMapper.toDomain(dto, clock)`, `BookJPAMapper`). **Why:** the
  standard allows only `@Service`, `@Transactional`, `ApplicationEventPublisher` and the Logger in `domaininteraction`,
  so a `@Component` mapper there breaks the standard (ArchUnit would not catch it). The ddd skill's sketch comment
  "constructor injection: ports, mappers" is illustrative, not a mandate.
- **Price crosses the flow as `BigDecimal`; payload keeps `String price` with `@NotBlank` plus a decimal `@Pattern`,
  the command adapter converts.** **Why:** the standard's example and spec 06's `importFromDistributor(String,
  BigDecimal)` fix BigDecimal as the pattern; a `BigDecimal` payload component would give Boot's empty-body 400.
- **400 is presence and wire format; a business range is a domain rule and a 422 with its own code.** Exception: when a
  spec scenario names 400 for a range (spec 03 quantities), the payload repeats the range (`@NotNull Integer` plus
  `@Min`/`@Max`) and the domain keeps the rule as an unreachable 422. **Why:** one source of truth where the spec allows
  it; the standard itself does not forbid payload ranges. **Trap:** the domain rule must accept everything the payload
  accepts, with no `strip()` and no blank check in the domain, so the payload stays stricter and the 422 stays
  unreachable over HTTP (hibernate-validator 9.1.5 `NotBlankValidator` calls `String.isBlank()`, so any whitespace
  such as U+2003 is already a 400; verified with `javap -c` in spec 03 qa-final).
- **Port shape:** `findById` throws the nested NotFound, natural-key finders return `Optional`, `save(DTO)` upserts by
  id onto the managed entity with `saveAndFlush`, DTOs carry no version. **Why:** `@Version` is honoured inside the
  flow's transaction without leaking persistence state into the domain; flushing in the adapter lets it translate
  unique and lock failures into port exceptions, and makes failures surface before the flow publishes events.
- **Four use-case exceptions, no common base; `details` always present.** **Why:** the standard names four; a base
  type would let a handler catch everything and hide a missing mapping. Handlers are duplicated per adapter by design
  (rule 3 forbids a shared helper package).
- **Every domain exception a flow call can throw is translated, `restore` included** (spec 02 QA failed call sites
  whose `toDomain` could leak). One private static translator per flow, parameterised by the field name when the same
  value object feeds different payload fields.
- **Events (spec 03):** the flow saves, then publishes `pullDomainEvents()` inside its transaction. A flow that only
  opens an aggregate whose factory raises nothing takes no publisher; a domain test "factory registers no event" is the
  guard that forces a revisit if that changes.
- **Derived read values reach adapters as DTO accessor methods** (spec 03 `StockItemDTO.available()`, `lowStock()`),
  pinned against the aggregate at the threshold. **Why:** adapters may not touch `domain`; otherwise every view copies
  the rule. Presentation-only values (`inStock`) stay in the view.
- **A request-parameter 400 in a query adapter** is a nested exception in the throwing controller plus one handler in
  the bean-validation shape. **Why:** tech-stack limits Jakarta Validation to payloads, and `params = "x=y"` mappings
  fail during handler lookup, where a package-scoped advice never runs.

- **Multi-aggregate flows (spec 04):** every check before the first write; one private save-and-publish helper shared
  by the use cases (root first, then the other aggregates sorted by id, then all pulled events). **Why:** sorted saves
  avoid a probed 1 s deadlock between concurrent orders, and a shared helper lets the use case whose events are
  observable prove the one whose events are empty today. A state check on the root (`ship`) runs before any other
  aggregate is read.
- **Payload subset for normalised strings (spec 04 e-mail):** the domain uses `trim()` (removes only chars at or below
  U+0020), the payload pattern excludes `\x00-\x20` everywhere; `strip()` or `\s` in the payload breaks the subset.
  Length is checked after lower-casing (U+0130 grows), which leaves one accepted 422 edge. **Why:** keeps the 422
  unreachable without a second validator.
- **Snapshots copied from another aggregate are plain strings** (order line isbn, title); money stays `Money`.
  **Why:** re-validating a validated copy only adds untranslatable restore failures. Instants go to views as
  `Instant.toString()` strings, truncated to micros in the domain (PostgreSQL rounds nanos; Linux clocks give nanos).

Spec 07 proposals (lead-1 strategy outbox, 2026-11-09; verify in the spec 07 plan and code):
- **Arity changes:** an aggregate `restore` stays one total factory (update its call sites); a DTO may keep an older
  arity as a documented convenience constructor when the sites are many and spread over unrelated tests. **Why:** a
  `restore` overload has no production caller and invites state loss; round-trip tests prove production uses the
  full form.
- **Invariant plus pre-check:** a business rule stays a domain exception even when the flow pre-checks it before a
  remote call; the unreachable exception is not translated (comment at the call). An unusable port answer that fails
  value-object validation becomes the port's "unavailable" exception in the flow.
- **Process layer:** no catch in processor or delegate; the flow catches named port exceptions around the port call
  only. **Why:** a caught exception that crossed the flow proxy already marked the transaction rollback-only.
- **Dates in views are `LocalDate.toString()` strings**, like instants, so no Jackson date setting matters.

Spec 08 proposals (lead-1 strategy outbox, 2026-11-09; verify in the spec 08 plan and code):
- **A limit configured in properties is checked in code**, a nested controller exception mapped to 400
  `validation-failed`; a `@Size` constant would leave the property without effect.
- **LLM tools take ids as `String` and parse inside**, so a hallucinated id reaches our curated error text instead of
  failing in the framework's argument conversion. Tool error texts: use-case details verbatim, fixed texts for tool
  input, a generic "<tool> failed for a technical reason." for anything else (trace to the log only).
- **Request order in a guarded adapter:** validation, then offline, then rate limit, then the model; rejected requests
  consume no slot and touch no memory.

Related: [[strategy-constraints]], [[test-context-cost-model]].
