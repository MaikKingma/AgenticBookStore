# References

- Spec: `docs/spec-pack/specs/04-orders.md` (use case step 0; domain model `EmailAddress`).
- Spec 12: `docs/spec-pack/specs/12-storefront.md` (400 `validation-failed` with field `customerEmail`; quantity 1 to 10 limited in the UI).
- Spec 04 folder: `agent-os/specs/2026-11-07-1724-04-orders/` (decision O2, superseded here for the place-order constraints).
- Spec 04 run ledger (archived): `.claude/comms/archive/2026-11-08-04-orders/mailboxes/orchestrator/outbox/run-04-orders.md`.
- Code: `bookstore/src/main/java/nl/openvalue/bookstore/command/OrderLinePayload.java`, `PlaceOrderPayload.java`; `domaininteraction/OrderFlow.java` (`mergedQuantities`, `translatingDomainExceptions`); `domain/OrderLine.java` (`quantityOf`); `domain/EmailAddress.java`.
- Precedent: spec 02 review fix (payload regex let negative prices reach the domain's 422), `docs/spec-pack/orchestrator/progress.md` decision log.
