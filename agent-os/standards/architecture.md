# Architecture standard (pointer)

The architecture standard for this repository is `docs/spec-pack/product/architecture-standard.md`.
Read that file; it wins over every skill in `.claude/skills/`.

- Bookstore code is written through the `hexagonal-slice` skill, which pulls in `clean-hexagonal-onion`,
  `ddd-building-blocks` and `layer-testing`.
- The referee is `./mvnw clean verify` from the repo root.
