---
name: slice-pattern-decisions
description: Rationale for the cross-slice design choices set at spec 02 (catalogue) that specs 03, 04, 06, 08 must mirror; use when shaping any later bookstore slice
metadata:
  type: project
---

Status: proposed in the spec 02 strategy (2026-11-07). Verify what landed in the spec 02 plan under
`agent-os/specs/` and in the code before advising; delete or fix any bullet the code contradicts.

- **Mappers are static final classes** (`BookDomainMapper.toDomain(dto, clock)`, `BookJPAMapper`). **Why:** the
  standard allows only `@Service`, `@Transactional`, `ApplicationEventPublisher` and the Logger in `domaininteraction`,
  so a `@Component` mapper there breaks the standard (ArchUnit would not catch it). The ddd skill's sketch comment
  "constructor injection: ports, mappers" is illustrative, not a mandate.
- **Price crosses the flow as `BigDecimal`; payload keeps `String price` with `@NotBlank` plus a decimal `@Pattern`,
  the command adapter converts.** **Why:** the standard's example and spec 06's `importFromDistributor(String,
  BigDecimal)` fix BigDecimal as the pattern; a `BigDecimal` payload component would give Boot's empty-body 400.
- **400 is presence and wire format only; every business range is a domain rule and a 422 with its own code.**
  **Why:** one source of truth for rules (the Lab 1 discount lands in the domain), no `@Size` duplicates of domain
  ranges in payloads.
- **Port shape:** `findById` throws the nested NotFound, natural-key finders return `Optional`, `save(DTO)` upserts by
  id onto the managed entity with `saveAndFlush`, DTOs carry no version. **Why:** `@Version` is honoured inside the
  flow's transaction without leaking persistence state into the domain; flushing in the adapter lets it translate
  unique and lock failures into port exceptions.
- **Four use-case exceptions, no common base; `details` always present.** **Why:** the standard names four; a base
  type would let a handler catch everything and hide a missing mapping. Handlers are duplicated per adapter by design
  (rule 3 forbids a shared helper package).
- **No event machinery before a spec needs it** (spec 03 brings `DomainEvent`). **Why:** the slice skill forbids
  speculative methods.

Related: [[strategy-constraints]], [[test-context-cost-model]].
