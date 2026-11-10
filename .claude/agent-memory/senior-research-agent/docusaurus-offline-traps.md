---
name: docusaurus-offline-traps
description: Docusaurus 3.10 traps for file:// offline builds, broken-link gating and the future.v4 flag, plus how to verify them in a browser
metadata:
  type: reference
---

Traps confirmed on Docusaurus 3.10.2 (verify again if the pin moves):

- `future.experimental_router: 'hash'` skips SSG, so `collectedData` is empty: the broken-link and broken-anchor checker sees
  nothing, and MDX runtime errors (`{name}` in prose) only show up as "This page crashed" in the browser. Rule: the
  browser-router build is the gate; build the hash bundle only after it passes.
- With the hash router, `baseUrl` must be `/`. A project baseUrl renders "Page Not Found" at `#/`.
- The create-docusaurus template sets `future.v4: true`, which turns off `markdown.mdx1Compat.admonitions`. Then
  `:::info Title` renders as literal text with no error. Use `:::info[Title]`.
- `markdown.hooks.onBrokenMarkdownLinks` defaults to `warn`. `.md` files are parsed as MDX (`format: 'mdx'`).
- WebKit has no `rel=prefetch`, so Docusaurus prefetches over XHR. Under file:// that logs harmless cross-origin console errors.
- Playwright WebKit first file:// load of a hash build intermittently leaves `<main>` empty (navbar and sidebar
  render). It is not caused by prefetch (an XHR stub still flakes) and never happens over HTTP; reload recovers.
  Measure with a fresh browser process per run: reused contexts hide it (0/20).
- CSS `url()` to fonts: both `src/css/fonts` with a relative url and `/fonts/...` from `static/` get bundled into
  `assets/fonts` with per-build paths. `static/` also ships an unhashed duplicate.

Technique: in a scratch dir, a Playwright script (chromium/firefox/webkit) that opens `pathToFileURL(index.html)` at a
720 px viewport and checks h1, sidebar navigation, images (force `loading=eager` first), tabs groupId sync, admonition
headings, copy buttons and `scrollWidth <= clientWidth`. For the Pages build, use a tiny Node server that serves under
`/<repo>/` and maps extensionless paths to `.html`. The `--config <file>` override that imports the main config is cross-platform.
Bash guard quirk: a command line containing the word "hash" is refused; grep for `HashRouter` or `experimental_router` instead.
