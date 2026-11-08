---
agent: res-1
task: res-1
status: completed
spec: (not yet written) 11-workshop-guide
updated: 2026-11-08T20:37:40Z
---

## TL;DR

Use one `docusaurus.config.ts` for the GitHub Pages build (browser router, `url: 'https://maikkingma.github.io'`,
`baseUrl: '/AgenticBookStore/'`, `trailingSlash: false`). Add a small `docusaurus.offline.config.ts` that spreads it
and overrides only `baseUrl: '/'` and `future.experimental_router: 'hash'`. Build them with `docusaurus build` (output
`build/`) and `docusaurus build --config docusaurus.offline.config.ts --out-dir build-offline`. I tested this on the
pinned Docusaurus 3.10.2 and Node 26.13.0. I zipped the offline bundle, extracted it into a path with spaces and
opened it from file:// in Chromium, Firefox and WebKit. All pages, images, code blocks, copy buttons, tabs, task
lists, navigation, deep links and anchors worked, with no horizontal page scroll at 720 px and no custom CSS. Two
traps change the plan:
(1) The hash build skips static generation, so a broken link or a crashing page still builds with exit 0. The
browser-router build must therefore be the CI gate, and the offline zip is built only after it passes.
(2) The official TS template turns on `future.v4: true`, which makes the spec's `:::info Sync moment` render as plain
text with no error. Pages must use `:::info[Sync moment]` and `:::tip[Checkpoint]`.
Directive D7 results:
- Self-hosted Montserrat woff2 in `src/css/fonts/`, referenced as `url('./fonts/...')` from `custom.css`, is bundled
  and loads on file:// in Chromium, Firefox and WebKit, with no CORS or font errors.
- `colorMode.disableSwitch` and `respectPrefersColorScheme` behave as documented.
- New risk: in Playwright WebKit (the Safari engine), the first file:// load of the offline bundle sometimes renders
  the navbar and sidebar but an empty article: 2 of 20 to 8 of 15 loads, with or without the font. It never happens
  over HTTP (0 of 15), and a reload or a sidebar click recovers it. I have not checked real Safari.

Senior assessment: adopt the two-config, two-build setup with the Pages build as the gate, the bracket admonition
syntax, fonts in `src/css/fonts/` and no search plugin. Confidence: high for Chromium and Firefox. For Safari, add a
"blank page: reload, or open the offline copy in Chrome or Firefox" line to the guide and do a manual real-Safari
check. memory: written in worktree.

## Key findings

1. **The hash router is `future.experimental_router: 'hash'` in 3.10.2, and it makes file:// work.** The config
   check accepts `'browser' | 'hash'` and defaults to browser (`node_modules/@docusaurus/core/lib/server/configValidation.js:91,283`).
   In hash mode:
   - The client uses `HashRouter` (`lib/client/clientEntry.js:17`).
   - Webpack/rspack `publicPath` is `'auto'` (`lib/webpack/base.js:137`).
   - `useBaseUrl` returns `./path` (`lib/client/exports/useBaseUrl.js:19`).
   - The output is a single `index.html` with `./assets/...` references.

   Reproduced in Chromium, Firefox and WebKit, both from the build folder and after a zip/unzip round trip into
   `USB Kit/workshop guide/`. Every functional check passed in all three engines. Evidence:
   `/private/tmp/claude-501/-Users-maikkingma-OpenValue-AI-Conferences-AgenticBookStore/cb0898b6-6f7b-44fd-8024-5110d0f772e2/scratchpad/res-1-browser-matrix.log`
   and `/private/tmp/claude-501/-Users-maikkingma-OpenValue-AI-Conferences-AgenticBookStore/cb0898b6-6f7b-44fd-8024-5110d0f772e2/scratchpad/res-1-zip-roundtrip.log`.
   Offline URLs look like `index.html#/level-0-break-the-agent`. Cross-page anchors (`#/reference/troubleshooting#jdtls-not-starting`),
   reload, unknown routes (the client shows "Page Not Found") and the navbar logo all work.
2. **The hash build needs `baseUrl: '/'`.** With the hash router and `baseUrl: '/AgenticBookStore/'`, `#/` shows
   "Page Not Found" because the routes are registered under the base path. Reproduced with the probe config
   `res-1-hashbase.config.ts`. So an override is required, and one config cannot serve both builds unchanged.
3. **The hash build does not check links and does not render pages at build time.** In
   `lib/ssg/ssgExecutor.js:131-137` the hash branch returns `collectedData: {}`, and `buildLocale.js:79` skips the
   server bundle. The broken-link checker therefore has nothing to check. Reproduced:
   - A broken `.md` link with the default hook: the browser build exits 1 ("Docusaurus found broken links!"). The
     hash build exits 0 (`res-1-hashwarn.config.ts`).
   - `{title}` in prose: the browser build exits 1 during static generation (`ReferenceError: title is not defined`).
     The hash build exits 0, and the page shows "This page crashed" in the browser.
4. **Broken links fail the browser build with exit 1.** Each case below was reproduced on the pinned version:
   - Route link `[x](/nowhere)`: `onBrokenLinks: 'throw'`.
   - Missing anchor `troubleshooting.md#no-such-heading`: needs `onBrokenAnchors: 'throw'`, because the default is `warn`.
   - Missing `.md` file: caught by `onBrokenLinks` at the end of the build even with the default hook. With
     `markdown.hooks.onBrokenMarkdownLinks: 'throw'` it fails earlier and also in the hash build.

   The top-level `onBrokenMarkdownLinks` is deprecated in 3.10 and moved to `markdown.hooks.onBrokenMarkdownLinks`
   (default `'warn'`). Its sibling `onBrokenMarkdownImages` defaults to `'throw'`
   (`configValidation.js:91-95,412-417,456-462`; https://docusaurus.io/docs/api/docusaurus-config).
5. **`future.v4: true` from the template turns off the `:::type Title` admonition syntax, silently.**
   `DEFAULT_FUTURE_V4_CONFIG_TRUE.mdx1CompatDisabledByDefault = true` (`configValidation.js:76-82`). The docs say
   mdx1Compat "Defaults to all false when future.v4.mdx1CompatDisabledByDefault is enabled". In the build output,
   `:::info Sync moment` came out as `<p>:::info Sync moment</p>` and `<p>:::</p>`, with no admonition and no error.
   Two fixes, both verified: `:::info[Sync moment]` (MDX v3 syntax, forward compatible), or
   `markdown.mdx1Compat.admonitions: true`.
6. **Tabs, task lists, copy button and the TSX component work with no extra setup.**
   - `@theme/Tabs` and `@theme/TabItem` with `groupId="os"`: picking "Windows PowerShell" in one block switches
     both blocks on the page.
   - Markdown task lists render as `<input type=checkbox disabled>`, and `[x]` renders as checked.
   - The classic theme shows a copy button on every code block by default: 5 buttons for 5 visible code blocks.
   - `import LevelProgress from '@site/src/components/LevelProgress'` with `<LevelProgress current={0} />` renders.
     A CSS module works too, and `tsc` passes. All of this held in both builds and all three engines.
7. **MDX pitfalls (verified).** `.md` files are also parsed as MDX (`markdown.format: 'mdx'` is the default).
   - Raw `<branch>` in prose fails the build loudly ("Expected a closing tag").
   - Raw `{title}` fails only at static generation, so only in the browser build (see finding 3).
   - Safe forms: inline code (`` `git switch <branch>` ``, `` `List<Order>` ``), `&lt;`, and `\{`.
   - YAML titles that contain a colon must be quoted: `title: "Level 0: Break the agent"`.
8. **At 720 px nothing needs to change.** `scrollWidth == clientWidth == 720` on every page in all three engines.
   - A long command makes `pre` scroll on its own (`overflow-x: auto`, scrollWidth 1423 > clientWidth 688).
   - Infima tables are `display: block; overflow-x: auto`. Wrapping text simply wraps, and a token that cannot wrap
     scrolls inside the table. The page never scrolls sideways.
   - At 720 px the docs sidebar collapses into the hamburger menu, and navigation through it works.
   - `src/css/custom.css` needs only the colours.
9. **Other things that break or change under file://.**
   - Raw JSX `<img src="/img/x.svg">` breaks in both builds: it resolves to `file:///img/...`, or to the domain root
     on Pages. The build does not catch it. Markdown image syntax (`![](/img/x.svg)` or a co-located `./img/x.svg`)
     and `useBaseUrl()` work in both builds.
   - The sitemap plugin disables itself in the hash build with a warning. That is harmless, so leave it on for Pages.
   - The hash build emits no `404.html`. Unknown routes still show the client-side "Page Not Found".
   - The default theme has no web fonts (system font stack), so no network requests fail.
   - In WebKit (Safari engine), Docusaurus prefetches over XHR because WebKit lacks `rel=prefetch`
     (`lib/client/prefetch.js`). File:// blocks that, so the console logs "Cross origin requests are only supported
     for HTTP". The error is caught, and pages still load through script tags. It is cosmetic only.
   - `trailingSlash` has no effect on the hash build. For Pages, set `false`, as the docs recommend for GitHub Pages
     (`true` or `false`, never undefined). Output is then `setup-check.html`. My local GitHub Pages emulation served
     extensionless URLs fine.
10. **Package and install facts.**
    - Exact `create-docusaurus@3.10.2 classic --typescript` package.json:
      - dependencies: `@docusaurus/core 3.10.2`, `@docusaurus/faster 3.10.2`, `@docusaurus/preset-classic 3.10.2`,
        `@mdx-js/react ^3.0.0`, `clsx ^2.0.0`, `prism-react-renderer ^2.3.0`, `react ^19.0.0`, `react-dom ^19.0.0`.
      - devDependencies: `@docusaurus/module-type-aliases 3.10.2`, `@docusaurus/tsconfig 3.10.2`,
        `@docusaurus/types 3.10.2`, `@types/react ^19.0.0`, `typescript ~6.0.2`.
      - `engines.node: ">=20.0"`, plus a `browserslist` block.
      - `@docusaurus/faster` (rspack, swc, lightningcss) is part of the official template. With `future.v4: true` it
        becomes the default bundler, so it is not a new dependency.
    - `npm ci` against the generated `package-lock.json` (lockfileVersion 3, npm 11.20.0) works:
      - 1301 packages in 5.3 s on warm cache, node_modules 443 MB.
      - The lockfile lists native bindings for linux-x64-gnu, win32, darwin and others, so a lockfile made on macOS
        installs on ubuntu CI.
      - npm 11.20 warns that it did not run install scripts for `@swc/core`, `core-js` and `fsevents` (allowScripts),
        and the build still works.
      - `npm audit` reports 25 advisories (15 critical) inside template dependencies.
    - Build time for this small prototype: browser build about 2.8 s cold, hash build about 1.2 s; `tsc` takes 2.4 s.
      The zipped offline bundle is 286 KB (820 KB extracted).
    - Node 26 prints a harmless `ExperimentalWarning: localStorage is not available` during the browser build.

11. **Self-hosted font (directive D7), verified on the pinned version.** I used real Montserrat latin 400, 600 and
    700 woff2 files, copied from `@fontsource/montserrat` into the prototype as probe material only (it is not a
    proposed dependency). Two placements:
    - `src/css/fonts/*.woff2` with `src: url('./fonts/montserrat-latin-400-normal.woff2') format('woff2')`: the
      bundler emits `assets/fonts/<name>-<hash>.woff2`. The CSS url is rewritten to `../../assets/fonts/...` in the
      offline build and to `/AgenticBookStore/assets/fonts/...` in the Pages build.
    - `static/fonts/*.woff2` with `url('/fonts/...')`: Docusaurus also resolves this absolute URL against `static/`
      and bundles it the same way. It does not break, but the build then ships the file twice: hashed in
      `assets/fonts/` and unhashed in `fonts/`.

    Result: every face reported `loaded` (`document.fonts`) and the body computed to Montserrat on file:// in
    Chromium, Firefox and WebKit, with no font, CORS or woff errors. Firefox's file:// same-directory rule is
    satisfied because the fonts sit below the `index.html` folder.

    Recommendation: put the fonts in `src/css/fonts/` next to `custom.css`, with relative `url('./fonts/...')`,
    `font-display: swap` and `format('woff2')`. Set `--ifm-font-family-base` (and optionally
    `--ifm-heading-font-family`) in `:root`.

    Evidence: `/private/tmp/claude-501/-Users-maikkingma-OpenValue-AI-Conferences-AgenticBookStore/cb0898b6-6f7b-44fd-8024-5110d0f772e2/scratchpad/res-1-font-color.log`
    and `.../scratchpad/res-1-font-color2.log`.
12. **Colour mode works as documented in all three engines on file://.** Tested with a 1200 px viewport and an
    emulated system scheme:
    - Default (switch on, `respectPrefersColorScheme: true`): the theme follows the system (light gives light, dark
      gives dark). The toggle has three states in 3.10 (system, then light, then dark, via `data-theme-choice`), and
      the choice survives a reload, because localStorage works on file:// in Chromium, Firefox and WebKit.
    - `disableSwitch: true, respectPrefersColorScheme: false, defaultMode: 'light'`: no toggle, and the theme stays
      light even when the system is dark.
    - `disableSwitch: true, respectPrefersColorScheme: true`: no toggle, and the theme follows the system.

    At 720 px the toggle sits inside the mobile menu, not in the navbar.
13. **WebKit file:// first-load flake (found while doing D7).** With a fresh Playwright WebKit process, the first
    load of `index.html` sometimes renders the navbar and docs sidebar but leaves `<main>` as an empty container. It
    does not recover within about 28 s.
    - Measured on the offline build: 4 of 10, 8 of 15 (at 720 px), 9 of 25 and 2 of 12.
    - Without any `@font-face`: 2 of 20.
    - With an XHR stub that removes the prefetch noise: 3 of 12. Prefetch is therefore not the cause.
    - The same hash build served over HTTP: 0 of 15. Chromium and Firefox: never seen in any run.
    - In 2 of 2 failures, a sidebar click and a reload both rendered the page.

    Root cause not found. `clientEntry.js` preloads `window.location.pathname` (the file path, not the hash route),
    so the content chunk is fetched lazily during render. Some WebKit file:// script-load race is plausible but
    unproven. Real Safari is not verified.

    Evidence: `.../scratchpad/res-1-webkit-flake2.log` to `.../scratchpad/res-1-webkit-flake5.log`.

## System model

Content (`docs/*.md|mdx`) goes through the MDX loader, then the client bundle (rspack via `@docusaurus/faster`):
- Browser router: server bundle, then static generation of one HTML file per route, then the broken link/anchor
  checker, then `build/` (Pages artifact).
- Hash router: no server bundle and no static generation. The output is one `index.html` plus relative assets in
  `build-offline/` (the zip). The link checker runs on an empty set.

Invariants:
- The Pages build is the only one that proves links and page rendering.
- The offline build is only a repackaging of content the Pages build already proved.
- Offline needs `baseUrl: '/'`. Pages needs `/AgenticBookStore/`.

## Hypotheses considered

| Hypothesis | Verdict | Evidence |
| --- | --- | --- |
| H1: hash router plus baseUrl switch makes file:// work | Survived, refined | Works in 3 engines and after a zip round trip. baseUrl must be `/`, not just "relative". |
| H2: some assets break under file:// even with hash | Partly survived | Only raw JSX `<img src="/...">` (also broken on Pages) and cosmetic WebKit prefetch errors. Fonts, Markdown images, co-located images, logo and favicon are fine. |
| H3: one hash build can serve both Pages and offline | Rejected | The hash build skips static generation, so there is no link check and no build-time render errors. Pages would also lose real URLs and static HTML. |
| H4: `onBrokenLinks: 'throw'` gates broken doc links | Survived only for the browser build | Exit 1 in the browser build, exit 0 in the hash build. Anchors need `onBrokenAnchors: 'throw'`. |
| H5: 720 px is fine with defaults | Survived | Measured in 3 engines: no page scroll; code blocks and tables scroll on their own. |
| H6 (found): the spec's `:::info Sync moment` syntax works | Rejected under the template default | `future.v4: true` disables it silently. |

## Implications for the decision

Recommended `guide/docusaurus.config.ts` (verified shape, colours and content aside):

```ts
const config: Config = {
  title: '...', favicon: 'img/favicon.ico',
  future: {v4: true},
  url: 'https://maikkingma.github.io',
  baseUrl: '/AgenticBookStore/',
  organizationName: 'MaikKingma', projectName: 'AgenticBookStore',
  trailingSlash: false,
  onBrokenLinks: 'throw',
  onBrokenAnchors: 'throw',
  markdown: {hooks: {onBrokenMarkdownLinks: 'throw', onBrokenMarkdownImages: 'throw'}},
  i18n: {defaultLocale: 'en', locales: ['en']},
  presets: [['classic', {
    docs: {routeBasePath: '/', sidebarPath: './sidebars.ts'},
    blog: false,
    theme: {customCss: './src/css/custom.css'},
  } satisfies Preset.Options]],
  themeConfig: {/* navbar with docSidebar, no blog link; prism additionalLanguages: ['bash','powershell','java','json'] */},
};
```

`guide/docusaurus.offline.config.ts`:

```ts
import config from './docusaurus.config';
export default {...config, baseUrl: '/', future: {...config.future, experimental_router: 'hash'}} satisfies Config;
```

Docs-only mode also needs:
- `docs/index.md` with `slug: /`.
- No `src/pages/index.tsx`: delete the template's `src/pages`, `blog/` and `HomepageFeatures`.
- Delete the template's navbar `/blog` link and footer links, or the link checker fails the build.

Number prefixes are stripped from ids and URLs: `00-setup-check.md` becomes `/setup-check`, and
`01-level-0-break-the-agent.mdx` becomes `/level-0-break-the-agent`. In-page links should use the file path with its
extension (`./00-setup-check.md`).

package.json scripts (all cross-platform, no `cross-env`):
- `"build": "docusaurus build"`: output `build/`. This is the gate and the Pages artifact.
- `"build:offline": "docusaurus build --config docusaurus.offline.config.ts --out-dir build-offline"`: output `build-offline/`.
- `"typecheck": "tsc"`. Keep the template script.
- `"check": "npm run typecheck && npm run build && npm run build:offline"`.

CI order: `npm ci`, then `npm run typecheck`, then `npm run build` (fails on broken links, anchors or render
errors), then `npm run build:offline`. Then:
- Upload `build/` as the Pages artifact.
- Zip the *contents* of `build-offline/` so `index.html` is at the zip root.
- PRs run the same steps without deploying.

`.gitignore`: the spec names `guide/.docusaurus/` and `guide/build/`. Also add `guide/build-offline/` and
`guide/node_modules/`. The template's own `guide/.gitignore` covers `/build`, `.docusaurus` and `/node_modules`, but
not `build-offline`.

Search: recommend none. There is no first-party local search plugin; the official integration is Algolia DocSearch,
which needs an API key and a crawler. Local search plugins are community packages, which would break the "no new
dependencies" rule. Browser find (Ctrl+F or Cmd+F) works offline.

## Senior assessment

Adopt:
- The two-config, two-build setup above, with the browser build as the gate.
- `:::info[Sync moment]` and `:::tip[Checkpoint]` in pages. Reword the spec's page conventions to that syntax rather
  than turning the MDX 1 compatibility back on: it is forward compatible with v4 and fails loudly if misspelled.
- `onBrokenAnchors: 'throw'` and `markdown.hooks.onBrokenMarkdownLinks: 'throw'` alongside `onBrokenLinks: 'throw'`.
- No search.
- Markdown image syntax only, never raw `<img src="/...">`.

Confidence: high for Chromium, Firefox and WebKit on macOS. Medium-high for real Safari and Windows browsers, which
I could not test, though relative-path classic scripts are the most portable form.

Assumptions: participants open `index.html` with a double-click and the zip keeps the folder structure.

What would change this: Docusaurus moving the hash router out of `experimental_`, a pin bump (re-run the probe), or a
real-Safari failure. The fallback for Safari would be telling participants to use Chrome or Firefox for the offline copy.

## Risks and second-order effects

- If CI ever builds only the offline variant (for example "just make the zip"), broken links and crashing pages ship
  silently. Make the Pages build a hard prerequisite of the zip step in the same job.
- Spec scenario wording: "Broken link, the CI build fails" holds only for `npm run build`. The QA check should run
  that command, not `build:offline`.
- Content written from the workshop plan will contain `<branch>`, `<placeholder>` and `{...}` in prose. Each one
  either breaks the build loudly or, with braces, only in the browser build. Authors must wrap them in inline code.
- Optional cheap guard: after `npm run build`, fail if the output contains `<p>:::` (a misspelled admonition). For
  example, a one-line `node -e` script in `check`. I did not implement it.
- The facilitator's own Safari may show console noise offline. That is harmless.
- `npm audit` noise (15 critical advisories in template dependencies) may trip any repo-wide audit gate if one
  exists. Not investigated.
- Node 26 `ExperimentalWarning` lines in the build log are expected. Do not treat them as failures.
- WebKit/Safari offline first load can come up blank (finding 13). Mitigations that need no code:
  - A troubleshooting line: "Offline copy shows an empty page in Safari: reload (Cmd+R), or open it in Chrome or
    Firefox".
  - Mention it where the offline zip is introduced.
  - A manual real-Safari check during the task 6 review pass on a clean machine.

  This does not affect the Pages site, which is static HTML served over HTTP.

## Sources

- Verified by reproduction: prototype at
  `/private/tmp/claude-501/-Users-maikkingma-OpenValue-AI-Conferences-AgenticBookStore/cb0898b6-6f7b-44fd-8024-5110d0f772e2/scratchpad/res-1-proto/site/`
  (create-docusaurus 3.10.2, Node 26.13.0, npm 11.20.0). Browser check script: `.../scratchpad/res-1-pw/res-1-check.mjs`
  (Playwright, chromium-headless-shell, firefox, webkit). Logs: `.../scratchpad/res-1-browser-matrix.log` and
  `.../scratchpad/res-1-zip-roundtrip.log`.
- Verified by reading the installed source: `@docusaurus/core@3.10.2` files `lib/server/configValidation.js`,
  `lib/ssg/ssgExecutor.js`, `lib/commands/build/buildLocale.js`, `lib/client/exports/useBaseUrl.js`,
  `lib/client/clientEntry.js`, `lib/webpack/base.js`, `lib/client/prefetch.js`.
- Docs (current site, consistent with 3.10.2 behaviour): https://docusaurus.io/docs/api/docusaurus-config
  (experimental_router, markdown.hooks, mdx1Compat, trailingSlash) and https://docusaurus.io/docs/deployment
  (GitHub Pages trailingSlash recommendation, url and baseUrl for project sites).

## Open questions / gaps

- Whether real Safari shows the WebKit first-load flake from finding 13 is unknown, and so is its root cause.
  Resolving it would take a manual Safari double-click test (10 tries), or enabling safaridriver for a scripted run.
- Real Safari, and Chrome or Edge on Windows opening `file:///C:/...`, were not tested. A one-time manual
  double-click test on the USB kit laptop types would settle it.
- Real GitHub Pages behaviour (extensionless URLs mapping to `.html` with `trailingSlash: false`) is verified only
  through the docs and a local emulation. The first deploy settles it.
- `engines.node`: the template says `>=20.0`. Whether to tighten it to the 26.13.0 pin, or to add `.nvmrc` for
  `actions/setup-node` `node-version-file`, is a choice for the orchestrator or res-2 (workflow).
- Whether the npm 11.20 allowScripts warnings matter in CI (the build works without the scripts) is not investigated further.
