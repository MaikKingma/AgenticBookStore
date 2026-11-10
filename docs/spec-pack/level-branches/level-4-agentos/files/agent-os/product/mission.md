# Product Mission

## Problem

Workshops about AI coding agents usually run on toy repositories. Participants watch an agent succeed on a
greenfield todo app and then fail on their first real codebase. Harness engineering only makes sense when there is
something to protect: an architecture, conventions, a test suite, external integrations.

## Target Users

1. **Workshop participants** (J-Fall 2026 pre-conference workshop, beginner level, Java developers). They receive
   a working, tested, *brownfield* codebase with no agent configuration at all, and build up a harness around it
   over Levels 0 to 4.
2. **The facilitator** (Maik). Needs reproducible starting points per level, reference solutions, and an app whose
   architecture an unconfigured agent will predictably violate.

## Solution

A small but realistic online bookstore, built with Spring Boot 4 on Java 25, structured with the Clean Hexagonal
Onion architecture (see `docs/architecture.md`), plus a separate distributor service that the bookstore
integrates with over REST and that the coding agent can query through our own MCP server.

The codebase has:
- A real domain with invariants (catalogue, inventory, orders, restocking), so "where does this logic go?" matters.
- A strict, test-enforced architecture (ArchUnit), so harness work has a referee.
- An anti-corruption layer and an event-driven process adapter, so agents meet non-trivial patterns.
- A Spring AI assistant, so participants see an AI feature inside the app they are building with an AI agent.
- An MCP server we built ourselves (distributor catalog), so participants see both sides of MCP.
