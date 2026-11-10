# Level 0 rehearsal record (template, facilitator only)

Pack spec 09, requirement "Level 0 produces observable failures", scenario "Rehearsal (facilitator, manual)". This
file is a template. No rehearsal has been run by the factory. The facilitator fills in a copy after publishing `main`
and tagging `level-0-start` (pack task 4), and keeps it with the facilitator notes. This folder never reaches `main`.

## Setup (fill in once)

- Date:
- Facilitator:
- Commit of `level-0-start` (`git rev-parse level-0-start`):
- Fresh clone command used: `git clone --branch level-0-start <repo url> level0-rehearsal`
- No user-level `~/.claude/CLAUDE.md` present (yes or no):
- Claude Code version (`claude --version`):
- Model: `/model sonnet` (what participants use)
- Permission mode: auto (the default for a new session; leave it, as in the workshop)
- `./mvnw verify` green on the fresh clone before the run (yes or no, time):

## Prompt

Level 0 prompt from the workshop plan, pasted literally, no hints:

> Add discounts to the bookstore. A book can get a discount percentage. Show the discounted price when people
> search the catalogue.

Answers to the agent's questions: only "you decide". Reset between runs: a fresh clone or
`git restore . && git clean -fd`, and a fresh session.

## Bar

Every run shows at least three of these failure modes, each with evidence (the failing test, the ArchUnit output or
the wrong file). This is the same bar as "Level cleared when" for Level 0.

| Code | Failure mode |
|---|---|
| F1 | ArchUnit failure (`ArchitectureTest` red) |
| F2 | Failing or missing tests |
| F3 | Missing migration (new column without a Flyway migration, an edited existing migration, or reliance on Hibernate DDL) |
| F4 | `double` or `float` for money |
| F5 | Logic outside the domain (controller, JPA entity, flow) |
| F6 | Unverified "tests pass" claim |

## Run 1

- Session start and end time:
- Agent's final claim (quote):
- `./mvnw verify` result after the run (exit code, failing tests):
- `git diff --stat`:

| Code | Seen (yes or no) | Evidence (failing test name, ArchUnit output line, or file and line) | Debrief column (didn't know, didn't care, didn't check) |
|---|---|---|---|
| F1 | | | |
| F2 | | | |
| F3 | | | |
| F4 | | | |
| F5 | | | |
| F6 | | | |

- Failure modes with evidence (count):
- One line on what went right:

## Run 2

Same fields and table as run 1.

## Run 3

Same fields and table as run 1.

## Outcome

- Runs with at least three failure modes with evidence: of 3
- Bar met (every run at least three): yes or no
- If no: rehearse again with the fallback prompts below and record those runs the same way.

Fallback prompts (more ambiguous, same area), from pack spec 09:

1. "Black Friday is coming. Make books cheaper for a weekend and show it in search."
2. "Marketing wants a 'sale' badge and a crossed-out old price in the catalogue API."

- Prompt used in the workshop:
- Notes for the keynote and the debrief:
