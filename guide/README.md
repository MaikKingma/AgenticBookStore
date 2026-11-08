# Workshop guide site

This folder holds the participant guide for the workshop "Crafting your Agentic Harness". It is a Docusaurus 3.10.2
site (classic preset, docs only, TypeScript config, no blog, no search). It is not a Maven module: the referee
`./mvnw clean verify` ignores it. Participants never need Node.js. Node.js is only for people who work on this folder
and for CI.

## Requirements

- Node.js 26.13.0 (the pinned version, also used by CI) and the npm that ships with it.
- Install with `npm ci` in this folder. Use `npm install` only to change `package.json`, and then commit the lockfile.
- Never run `npm audit fix` and never add a dependency. A check fails on any change to the dependency list.

## Commands

Run all of them in `guide/`.

| Command | What it does |
|---|---|
| `npm start` | Dev server with live reload (Pages settings, so open the URL it prints). |
| `npm run typecheck` | `tsc` over the config files and components. |
| `npm run check` | The node:test self-tests of the checker, then the source rules. Needs no build. |
| `npm run build` | The Pages build into `build/`. This is the gate: it renders every page and fails on a broken link or anchor. |
| `npm run check:built` | Scans `build/**/*.html` for a literal `:::` (an admonition that was not parsed). Fails when `build/` is missing or holds no html file. |
| `npm run build:offline` | The offline build into `build-offline/` (see below). Run it only after `npm run build` passed. |

The order in CI is `npm ci`, `check`, `typecheck`, `build`, `check:built`, `build:offline`.

## Two configs, two builds

- `docusaurus.config.ts` is the GitHub Pages build: browser router, `baseUrl: '/AgenticBookStore/'`,
  `trailingSlash: false`, and `throw` for broken links, anchors and Markdown links.
- `docusaurus.offline.config.ts` spreads the first config and overrides only `baseUrl: '/'` and the hash router
  (`future.experimental_router: 'hash'`). Its output is one `index.html` with relative assets, so the unzipped folder
  opens from `file://` without a server.

The hash build skips static rendering and checks no links, so a broken link or a crashing page still builds with exit
code 0. That is why `npm run build` is the gate and the offline build is only a repackaging of what the gate proved.

Keep `future.v4: true` in the config. Without it the Docusaurus 3 title syntax of admonitions fails silently.

## The checker

`npm run check` runs the self-tests in `scripts/*.test.mjs` (Node's built-in `node:test`) and then
`node scripts/check.mjs`, which prints one line per rule and the pages that still carry a skeleton marker. The checker
has no dependencies and reads source files only.

Rules in place:

- ASCII only: every text file under `guide/` (except `node_modules`, `build`, `build-offline`, `.docusaurus` and font
  binaries) must contain 7-bit ASCII only, with a dedicated message for the em dash. `scripts/` is scanned too.
- No forbidden word: the guide says Level, never the old term. It scans the same files except `scripts/` and the
  lockfile, which lists third-party package names.
- Dependency allowlist: the key sets of `dependencies` and `devDependencies` equal the official template's, and the six
  `@docusaurus/*` packages are pinned to exactly 3.10.2.

The checker builds its forbidden characters from code points (`String.fromCodePoint(0x2014)`) because the editor tools
decode a typed backslash-u escape into the literal character.

## Writing pages

- Admonitions use the bracket form: `:::info[Sync moment]` and `:::tip[Checkpoint]`. The space form renders as plain text.
- Put placeholders such as `<branch>` or `{name}` in inline code. Raw ones break the build.
- Use Markdown image syntax only, never a raw `<img>` tag.
- Quote a YAML title that contains a colon.

## Hand-over: publishing the site (owner only)

The workflow `.github/workflows/guide.yml` builds on every pull request that touches `guide/**`. The deploy to GitHub
Pages and the release asset `workshop-2026` run only on a push to `factory` and only when the repository variable
`GUIDE_DEPLOY` is `true`. Nobody but the owner flips it. The steps:

1. In the repository settings, enable Pages with source "GitHub Actions".
2. If the `github-pages` environment restricts branches, allow `factory`.
3. Set the repository variable `GUIDE_DEPLOY` to `true`.

Until then a push to `factory` builds the site and skips deploy and release.
