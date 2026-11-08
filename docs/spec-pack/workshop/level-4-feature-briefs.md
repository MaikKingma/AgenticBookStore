# Level 4 Feature Briefs

Pick one. Each brief is written the way a product owner would ask for it: incomplete on purpose. Turning it into a
precise, testable spec is the point of the level. Facilitator notes at the bottom.

## Brief A: Reviews and ratings (recommended for most people)
> "Customers keep asking which books are actually good. Let them leave a rating from 1 to 5 stars and a short review
> for books they bought. Show the average rating in the catalogue search."

Questions your spec should answer: who may review (only customers with a shipped order containing the book?), one
review per customer per book or edits allowed, review length, how the average is rounded and shown when there are no
reviews, whether discontinued books keep their reviews.

## Brief B: Wishlist with back-in-stock notice
> "When a book is out of stock, customers should be able to put it on a wishlist, and we tell them when it is back."

Questions your spec should answer: identify customers by e-mail like orders do?, what "tell them" means here (no real
e-mail: record a `NotificationRequested` entry that a query can list), what happens when stock comes back while a
restock is pending, limits per customer. Touches the `process` layer: a new domain event when stock becomes
available again.

## Brief C: Assistant recommends from the distributor (advanced)
> "When we don't have what a customer asks for, the assistant should suggest titles from our distributor that we could
> order for them."

Questions your spec should answer: how the assistant reaches the distributor, how suggestions are clearly marked
as "not in stock, can be ordered", what the assistant must never promise (delivery dates?), how tests stay offline.

Two routes, and your spec must pick one explicitly:
1. **Within the architecture (recommended):** add a `searchTitles` method to the `DistributorAppService` port,
   implement it over REST in `acl`, expose it to the model through a new tool that calls a flow. Tests: scripted chat
   model plus a mocked port.
2. **Via MCP:** use Spring AI's MCP client against our distributor MCP server. This breaks two rules on purpose
   (outbound integration only through `acl`; `CLAUDE.md` says the distributor is reached only through the port), so
   your spec must include an architecture change: update `docs/architecture.md`, `CLAUDE.md` and the ArchUnit rule
   with a documented exception. That is a legitimate outcome of a spec, as long as it is explicit.

---

## Facilitator notes
- A fits every track in 35 minutes. B is fine for OpenSpec and Agent OS, tight for Spec Kit. C is for people who
  finished early or know Spring AI. Route 2 is a great debrief story: the spec changed the standard, and the harness
  (ArchUnit, CLAUDE.md) was updated in the same commit instead of being silently violated.
- Good specs for A name the "verified buyer" rule explicitly; weak ones let anyone review anything. Use that in the debrief.
- Ask every participant to show the scenario-to-test mapping before they `/ship`.
