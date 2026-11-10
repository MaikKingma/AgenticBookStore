# Shape: spec 14 guide branding

Source of truth: `docs/spec-pack/specs/14-guide-branding.md` (decision D18). This file restates it for the cycle and
records the shaping decisions; it changes no requirement.

## Scope
The Docusaurus guide (`guide/`, module `guide`) only:
1. Navbar left: the OpenValue logo (extracted from the keynote deck "Shift Left, Look Right", page 1) in front of the
   title. The logo opens https://openvalue.eu in a new tab; the title still goes to the guide home page.
2. Navbar right: a GitHub link to https://github.com/MaikKingma/AgenticBookStore and "Follow on LinkedIn" to
   https://www.linkedin.com/in/maik-kingma/, both in a new tab with `rel="noopener noreferrer"`.
3. `LevelProgress` becomes a chevron breadcrumb: done levels green with a check mark, the current level solid deck blue
   with white text and `aria-current="step"`, upcoming levels muted; every crumb links to its level page.
4. Deck colours: primary deck blue `#3c7ec9` in both modes, slate `#1d242c` backgrounds in dark mode, coherent across
   all 11 pages. Montserrat stays.
5. Logo asset: a white and a slate transparent PNG with recorded provenance.

Out of scope: the storefront (`frontend/`, D7), the bookstore, page text, level content, the frozen command checker.

## Shaping decisions (self-accepted by the orchestrator, owner delegated the gates)
- D-S1 AA and the exact deck blue. `#3c7ec9` is 4.18:1 against white (white text on it, or it as text on white) and
  3.74:1 on slate. The primary variable is `#3c7ec9` in both modes; text in blue uses a derived shade that reaches
  4.5:1 (darker in light mode, lighter in dark mode), as the pack allows ("shades derived from it"). The current crumb
  keeps the owner's exact fill with a white label rendered as WCAG AA large text (bold, at least 18.67 px, 3:1); the
  checker gains a contrast kind `large` that a test restricts to named pairs. Alternative kept open for the owner:
  fill the current crumb with the derived `#3371B8` (5.01:1) at normal label size.
- D-S2 The crumbs become links. The pack says "as today", but today the crumbs are plain list items; the requirement
  "every crumb links to its level page" stands. Pack wording correction is the owner's (residual).
- D-S3 The light-mode navbar becomes light, so the slate logo for light mode is visible; the dark-mode navbar is
  slate. Part of T1.
- D-S4 The brand rule moves to the D18 palette; teal `#014046` and mist leave the guide; the negative swap tests stay.
- D-S5 The spec 11 checker freeze ended with spec 11. Every edited `guide/scripts/**` file is named with its reason;
  a guard may move or grow, never shrink. qa-final records the new `guide/scripts/**` hash baseline in this folder.
- D-S6 Logo provenance is enforced by a check (README row with sha256), like the fonts.
- D-S7 The four required screenshots are committed under `visuals/` and linked from the spec PR body.
- D-S8 The navbar logo uses a wrapped theme component (`guide/src/theme/Navbar/Logo`): Docusaurus config alone puts
  logo and title in one link.
- D-S9 The `LevelProgress` and navbar tests are built-HTML rules with node:test fixtures; a source-level `.tsx` render
  needs a new dependency.

## Research
- res-1 guide checks: `.claude/comms/mailboxes/res-1/outbox/research-guide-checks.md` (session-local).
- res-2 Docusaurus navbar and palette: `.claude/comms/mailboxes/res-2/outbox/research-docusaurus-navbar-palette.md`.
- res-3 logo extraction: `.claude/comms/mailboxes/res-3/outbox/research-logo-extraction.md`.
Three sonnet researchers; no tech lead (three tasks in one module).
