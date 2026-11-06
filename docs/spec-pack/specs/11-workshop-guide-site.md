# Spec 11: Workshop guide site (Docusaurus)

> Repository: `agentic-harness-workshop` · Depends on: 10 (for branch names) · Capability: `workshop-guide`

## Why
Participants work at different speeds. A self-paced guide lets fast people move on and slow people catch up, while
the facilitator's sync moments keep the room together. The guide must work on a second screen, survive bad wifi,
and stay online after the workshop as take-home material.

## What changes
- A Docusaurus 3 site (classic preset, docs-only mode, no blog) deployed to GitHub Pages.
- Content generated from `workshop/workshop-plan.md`, `workshop/sdd-framework-guide.md` and
  `workshop/lab-4-feature-briefs.md` of this pack. Those files stay the source of truth; the site restructures them
  for participants and removes facilitator-only notes (expected findings, risk table, rehearsal notes).
- An offline bundle: the built static site as a zip attached to a GitHub release, and on the USB kit.

## Out of scope
- Search services that need an API key (use the local search plugin or none), comments, analytics, i18n.

## Information architecture
```
docs/
  index.md                      # Welcome, agenda, what you will build, how to use this guide
  00-before-you-start.md        # Prerequisites (mirrors the participant email), Lab 0 setup check
  01-lab-break-the-agent.md
  02-lab-context-engineering.md
  03-lab-behavioural-controls/
    index.md                    # Overview and catch-up
    a-permissions.md
    b-hooks.md
    c-skills-and-commands.md
    d-mcp.md
    e-stretch-goals.md
  04-lab-spec-driven/
    index.md                    # Choose a track and a brief
    track-a-openspec.md
    track-b-speckit.md
    track-c-agentos.md
    feature-briefs.md
  05-wrap-up.md                 # Recap map, Anthropic Academy links, next steps
  reference/
    cheat-sheet.md              # Claude Code commands and files used today (one page, printable)
    architecture.md             # The onion, for participants
    troubleshooting.md          # Windows hooks, usage limits, MCP not connecting, ANTHROPIC_API_KEY pitfall
```

## Page conventions
- Every lab page starts with a box: **Goal**, **Start branch**, **Catch-up**: `git stash -u && git switch <branch>`,
  **Time**.
- Sync moments are marked with an admonition `:::info Sync moment` that says "Look up, Maik demos this now".
- Checkpoints are `:::tip Checkpoint` with the exact command and the expected output.
- Every command is in a code block with a copy button; OS-specific commands use Docusaurus tabs
  (macOS/Linux, Windows PowerShell). Hook script tabs: bash and PowerShell.
- Lab 1 includes the observation card as a fill-in table participants copy into `notes/lab-1.md` (git-ignored).
- No em dashes in any page.

## Requirements

### Requirement: Complete lab coverage
The site SHALL contain every step, command and checkpoint of Labs 0 to 4 from the workshop plan, and the three SDD tracks.

#### Scenario: Content check
- **GIVEN** the workshop plan
- **WHEN** a reviewer compares each numbered step in sections 3 to 8 with the site
- **THEN** every step and every command appears on the site, and no facilitator-only section does

### Requirement: Works offline
The guide SHALL be usable without network access.

#### Scenario: Offline bundle
- **GIVEN** the release zip extracted on a laptop without network
- **WHEN** the participant opens it with `npx serve` or by opening `index.html` (configure the build for relative paths, *verify* which works with the chosen Docusaurus version)
- **THEN** all pages, images and code blocks render

### Requirement: Readable next to a terminal
The guide SHALL be readable at half-screen width on a 13-inch laptop.

#### Scenario: Narrow viewport
- **WHEN** a page is viewed at 720 px width
- **THEN** there is no horizontal page scroll and code blocks scroll on their own

### Requirement: Deployed automatically
Pushes to `main` SHALL build and deploy the site to GitHub Pages; pull requests SHALL build without deploying.

#### Scenario: Broken link
- **GIVEN** a page links to a missing page
- **WHEN** the CI build runs
- **THEN** the build fails (`onBrokenLinks: 'throw'`)

## Tasks
1. Scaffold Docusaurus 3 (classic, TypeScript config), docs-only, OpenValue AI colours from the OpenValue design system.
2. Write `index.md`, `00-before-you-start.md`, `reference/*`.
3. Write Lab 1 to Lab 4 pages and the SDD tracks from the pack.
4. Tabs for OS-specific commands, admonitions for sync moments and checkpoints.
5. GitHub Actions: build on PR, deploy on main, attach zip to release `workshop-2026`.
6. Review pass: run every command in the guide on a clean machine against the bookstore repository.

## Definition of done
- Site live, offline zip available, every command executed once on a clean machine, no broken links.
