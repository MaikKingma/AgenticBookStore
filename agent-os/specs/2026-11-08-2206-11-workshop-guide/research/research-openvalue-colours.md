---
agent: res-4
task: res-4
status: completed
updated: 2026-11-08T12:00:00Z
---

## TL;DR
Brand blue is #3d7dc9 in the keynote deck (the design system source; public site openvalue.eu uses a duller #4273b5). Dark ground #1d252d, deep teal #014046, light tints #f1f5f6 and #cbd9da, mid grey-teal #809fa2. Font is Montserrat for text (SIL OFL on Google Fonts) and JetBrains Mono for code (OFL); the outline states both. #3d7dc9 fails AA for text on white (4.22). Use #2e64a5 as light-mode primary (6.03 on white) and #6598d4 in dark mode (5.16 on #1d252d). Recommend system font stack unless you bundle Montserrat woff2 files.

## Key findings
- Deck PDF colour operators (python3 -I zlib scan of content streams, usage counts): #3d7dc9 (168), #1d252d (146), #ffffff (140), #cbd9da (110), #014046 (92), #f1f5f6 (54), #809fa2 (42), #000000 (20), #1d242c (10). Role mapping is my inference from frequency, not documented: #3d7dc9 blue accent, #1d252d charcoal ground/text, #014046 deep teal ground, #f1f5f6 light ground, #cbd9da borders/light text on dark, #809fa2 muted text.
- Deck embeds only JetBrainsMono-Regular and SemiBold (BaseFont scan). Outline line 3 (keynote-outline.md): "Built in the OpenValue design system (Montserrat, OpenValue blue and charcoal grounds, logo bottom-right). Code on slides uses JetBrains Mono." Montserrat is not embedded in the PDF (likely outlined or in object streams; unverified).
- Public site: https://www.openvalue.eu/ (openvalue.nl 301-redirects there), css/main.<hash>.css contains only #4273b5 and #2b343c (and #222); no font-family declarations found. Logo: homepage references ./shared/images/openvalue-tt-white.png; no usage permission found, so recommend no logo.
- Contrast (WCAG, computed): #3d7dc9 on white 4.22 (fails AA 4.5 for normal text), on #1d252d 3.67 (fails). #4273b5 on white 4.83.
- Shade set by HSL lightness +/-10/15/30 points (approximation of the Docusaurus generator; the official one at docusaurus.io/docs/styling-layout is an online tool, not re-run):
  - Base #3d7dc9: dark #2e64a5, darker #285891, darkest #183455, light #6598d4, lighter #79a5da, lightest #b5cdea.
- Proposed light mode: primary #2e64a5 (white 6.03); dark #285891 (7.25); darker/darkest from a set generated from #2e64a5 (run the Docusaurus palette tool on it). Note #285891 values above are from the #3d7dc9 set.
- Proposed dark mode: primary #6598d4 (5.16 on #1d252d), lighter #79a5da (6.06). Dark background #1d252d (docusaurus --ifm-background-color), surface #014046 optional.

## Implications for the decision
- Option A: keep brand #3d7dc9 for large text, buttons with white text (4.22 passes only for large text) and links in dark mode; adjust links to #2e64a5 for accessibility.
- Option B (recommended by numbers): light #2e64a5, dark #6598d4. Brand-adjacent, passes AA.
- Fonts: Montserrat (OFL 1.1) and JetBrains Mono (OFL 1.1) both allow bundling; woff2 files must be copied into guide/static/fonts with the licence text. Otherwise system stack: `system-ui, -apple-system, "Segoe UI", Roboto, sans-serif` and `ui-monospace, "SF Mono", Menlo, Consolas, monospace`. Licence statements are from my knowledge of Google Fonts, not re-verified this run.

## Sources
- Keynote PDF (verified, scanned); keynote-outline.md line 3 (verified); https://www.openvalue.eu/ and its main.css (verified, curl); WCAG maths computed locally (verified).

## Open questions / gaps
- No official design-system document found; roles of colours are inferred. Ask the brand owner if #3d7dc9 vs #4273b5 is canonical.
- Mailbox dir res-4/ did not exist at start; I created the outbox via Write. Inbox not polled (absent).
- Shade values are an HSL approximation; rerun the Docusaurus palette generator on the final primary.
