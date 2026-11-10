# Spec 11: Workshop guide site (Docusaurus)

> Folder: `guide/` in `MaikKingma/AgenticBookStore` · Depends on: 10 (for branch names) · Capability: `workshop-guide`

## Why
Participants work at different speeds. A self-paced guide lets fast people move on and slow people catch up, while
the facilitator's sync moments keep the room together. The guide must work on a second screen, survive bad wifi,
and stay online after the workshop as take-home material.

## What changes
- A Docusaurus 3 site in `guide/` (classic preset, docs-only mode, no blog) deployed to GitHub Pages of this
  repository. The version is pinned in `product/tech-stack.md`. `guide/` is not a Maven module: it has its own
  `package.json`, and CI installs the pinned Node.js with `actions/setup-node`. Participants never need Node.js.
- Content generated from `workshop/workshop-plan.md`, `workshop/sdd-framework-guide.md` and
  `workshop/level-4-feature-briefs.md` of this pack. Those files stay the source of truth; the site restructures them
  for participants and removes facilitator-only notes (expected findings, risk table, rehearsal notes).
- An offline bundle: the built static site as a zip attached to a GitHub release, and on the USB kit.

## Out of scope
- Search services that need an API key (use the local search plugin or none), comments, analytics, i18n.
- Accounts, a backend, or stored progress: the level progress indicator and the "Level cleared when" checklists are
  static content.

## Information architecture
```
guide/docs/
  index.md                                  # Welcome, agenda, the five levels, how to use this guide
  00-setup-check.md                         # Prerequisites (mirrors the participant email), setup check, per-OS jdtls install
  01-level-0-break-the-agent.mdx            # Level 0: Break the agent
  02-level-1-context-engineering.mdx        # Level 1: Context engineering
  03-level-2-give-your-agent-eyes.mdx       # Level 2: Give your agent eyes (LSP + MCP); Part A LSP, Part B MCP, stretch
  04-level-3-behavioral-control.mdx         # Level 3: Behavioral control; Parts A to C, optional Stop hook, stretch
  05-level-4-spec-driven-development.mdx    # Level 4: Spec-driven development; track tabs, feature briefs
  06-wrap-up.md                             # Who cleared which level, recap map, Anthropic Academy links, next steps
  reference/
    cheat-sheet.md              # Claude Code commands and files used today (one page, printable)
    architecture.md             # The onion, for participants
    troubleshooting.md          # Windows hooks, jdtls not starting, usage limits, MCP not connecting, ANTHROPIC_API_KEY pitfall
guide/src/components/
  LevelProgress.tsx             # Static level progress indicator, levels 0 to 4, current level highlighted
```
One page per level. Level 3 shows its parts as sections of one page. Level 4 shows the three tracks as Docusaurus
tabs and the feature briefs as a section of the same page.

## Page conventions
- Every level page is titled "Level N: name" with the names of the workshop plan: Level 0: Break the agent;
  Level 1: Context engineering; Level 2: Give your agent eyes (LSP + MCP); Level 3: Behavioral control;
  Level 4: Spec-driven development.
- Every level page starts with the level progress indicator (`<LevelProgress current={N} />`: levels 0 to 4, the
  current one highlighted), then a box: **Goal**, **Start branch**, **Catch-up**:
  the level's command from the workshop plan ("Someone is far behind": `git switch -C level-N level-0-start` for
  Levels 0 and 1, `git switch <branch>` from Level 2 on, each after `git stash -u`), **Time**.
- Every level page ends with a "Level cleared when" checklist (a Markdown task list) copied from the workshop plan.
- Sync moments are marked with an admonition `:::info Sync moment` that says "Look up, Maik demos this now".
- Checkpoints are `:::tip Checkpoint` with the exact command and the expected output.
- Every command is in a code block with a copy button; OS-specific commands use Docusaurus tabs
  (macOS/Linux, Windows PowerShell). Hook script tabs: bash and PowerShell.
- Level 0 includes the observation card as a fill-in table participants copy into `notes/level-0.md` (git-ignored):
  one row per failure mode with the columns **Failure mode**, **Evidence** (the failing test, the ArchUnit output or
  the wrong file) and **Column** (didn't know, didn't care, didn't check), at least three rows, plus one line on
  what went right.
- The setup page lists the `jdtls` install per OS in tabs (macOS: `brew install jdtls`; Arch Linux: `yay -S jdtls`;
  other Linux: a JDT LS release with a `jdtls` wrapper script on the `PATH`; Windows: through WSL) and the check
  `which jdtls` (PowerShell: `Get-Command jdtls`). It does not install the plugin.
- The Level 2 page holds the plugin install `/plugin install jdtls-lsp@claude-plugins-official` in Part A; Levels 0
  and 1 run without the plugin.
- No em dashes in any page.

## Visual design (decision D7, Maik 2026-11-08)
The guide uses the same OpenValue 2026 brand as the OpenBook storefront (spec 12, "Visual design"), so the shop and
the guide look like one product.

- Colours: the seven brand values of spec 12 (`#1D252D` navy, `#3D7DC9` blue, `#014046` teal, `#809FA2` sage,
  `#CBD9DA` mist, `#F1F5F6` snow, `#FFFFFF` white), mapped onto Docusaurus' Infima variables in
  `guide/src/css/custom.css` and nowhere else:
  - light mode: `--ifm-color-primary` teal `#014046` (with the six generated shades), text and headings navy, page
    background white, code blocks and the progress indicator's inactive levels on snow or mist, the current level
    and the navbar accent in blue (large or bold text only, it is 4.2:1 on white), navbar navy with white text;
  - dark mode: background navy, primary mist `#CBD9DA` (10.7:1 on navy), text white. If a pair in dark mode cannot
    meet AA, switch dark mode off (`colorMode.disableSwitch`) instead of using an off-brand colour.
  Every text and background pair meets WCAG 2.2 AA (4.5:1 for normal text).
- Typography: Montserrat, self-hosted woff2 (latin, 400, 600, 700) with its SIL Open Font License file, loaded with
  `@font-face` and `font-display: swap`; `--ifm-font-family-base` is
  `Montserrat, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`. Code stays in the default monospace stack.
  No font CDN: the guide must work offline and from the zip.
- The level progress indicator uses the brand: five steps 0 to 4, the current level filled teal (light) or mist
  (dark) with a visible label, the others outlined in sage with navy labels.
- No OpenValue logo is required; the navbar title stays the guide's title. No new dependency beyond the pinned
  Docusaurus packages.

## Requirements

### Requirement: Complete level coverage
The site SHALL contain every step, command, checkpoint and "Level cleared when" check of the setup check and Levels 0
to 4 from the workshop plan, and the three SDD tracks.

#### Scenario: Content check
- **GIVEN** the workshop plan
- **WHEN** a reviewer compares each numbered step in sections 3 to 9 with the site
- **THEN** every step and every command appears on the site, and no facilitator-only section does

### Requirement: Level pages
Every level page SHALL show its place in the level sequence and end with its "Level cleared when" checklist.

#### Scenario: Level page structure
- **GIVEN** the built site
- **WHEN** a reviewer opens each of the five level pages
- **THEN** the title is "Level N: name", the level progress indicator at the top shows levels 0 to 4 with level N
  highlighted, and the page ends with a "Level cleared when" checklist identical to the workshop plan

### Requirement: Works offline
The guide SHALL be usable without network access.

#### Scenario: Offline bundle
- **GIVEN** the release zip extracted on a laptop without network
- **WHEN** the participant opens `index.html` from the extracted folder in a browser (configure the build for relative paths; *verify* with the pinned Docusaurus version; participants have no Node.js, so no `npx serve`)
- **THEN** all pages, images and code blocks render

### Requirement: Readable next to a terminal
The guide SHALL be readable at half-screen width on a 13-inch laptop.

#### Scenario: Narrow viewport
- **WHEN** a page is viewed at 720 px width
- **THEN** there is no horizontal page scroll and code blocks scroll on their own

### Requirement: OpenValue brand
The guide SHALL use the OpenValue 2026 colours and Montserrat described under "Visual design".

#### Scenario: Brand in one place
- **GIVEN** the guide source
- **THEN** `guide/src/css/custom.css` sets the Infima primary colour to `#014046` for light mode and holds every brand
  colour value; no other stylesheet or component hard-codes a colour value

#### Scenario: Readable contrast
- **GIVEN** the text and background pairs of both colour modes (or light mode only, when dark mode is switched off)
- **THEN** each pair has a contrast ratio of at least 4.5:1, checked by a script in `guide/` that the CI build runs

### Requirement: Deployed automatically
Pushes to `factory` that touch `guide/**` SHALL build and deploy the site to GitHub Pages; pull requests that touch `guide/**` SHALL build without deploying. Nothing deploys from `main`: it is published by spec 09 before `guide/` exists, and a later republish would only carry `guide/` as a plain folder. Guide fixes never require a republish of `main` (decision of 2026-11-06).

#### Scenario: Broken link
- **GIVEN** a page links to a missing page
- **WHEN** the CI build runs
- **THEN** the build fails (`onBrokenLinks: 'throw'`)

## Tasks
1. Scaffold Docusaurus (pinned version, classic, TypeScript config) in `guide/`, docs-only, with the OpenValue 2026
   brand of "Visual design" (Infima variables, Montserrat, dark mode rule). Add `guide/.docusaurus/` and `guide/build/` to the root `.gitignore`.
2. Write `index.md`, `00-setup-check.md`, `reference/*`, and the `LevelProgress` component.
3. Write the Level 0 to Level 4 pages and the SDD tracks from the pack.
4. Tabs for OS-specific commands, admonitions for sync moments and checkpoints.
5. GitHub Actions workflow `guide.yml` with `paths: [guide/**]`: build on PR, deploy on push to `factory`,
   attach zip to release `workshop-2026`.
6. Review pass: run every command in the guide on a clean machine against `main` of this repository.

## Definition of done
- Site live, offline zip available, every command executed once on a clean machine, no broken links.
