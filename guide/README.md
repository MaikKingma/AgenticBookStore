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
| `npm run check` | The node:test self-tests of the checker, then the source rules (hygiene, colours, contrast, brand, fonts, zero-test guard). Needs no build. |
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
- Zero-test guard: `node --test` exits 0 when it runs nothing (a glob that matches no file, or emptied test files). The
  `tests` rule reads the patterns of the `node --test` command in the `check` script of `package.json`, and fails when a
  pattern matches no file or when the matched files register no test case. Keep the `&&` chain in that script.
- Colours (rule `colors`): no hex value, colour function or CSS named colour in a property value of any stylesheet
  other than `src/css/custom.css`, and none in a `style` prop of a `.tsx`, `.jsx`, `.mdx` or `.md` file. `inherit`,
  `currentColor`, `transparent` and `var(--...)` are fine. Markdown anchors such as `[x](#cafe)` are not colours, and a
  name followed by `(` is a function, so `tan(45deg)` passes while `tan` alone is the colour.
  What the rule scans: the property values of every `.css` file except `custom.css`, and the `style` props (JSX object or
  HTML attribute) of every `.tsx`, `.jsx`, `.mdx` and `.md` file. What it does not scan, so review these by eye: `.ts`
  files such as `docusaurus.config.ts`, a colour held in a `const` and referenced from a style prop, the SVG `fill` and
  `stroke` attributes, an inline `<style>` element, and the system colours (`Canvas`, `LinkText` and so on).
- Contrast: every pair in `scripts/rules/contrast-pairs.mjs` is checked in light and dark mode (see Brand below) and
  printed with its ratio: 4.5 or more for text, 3.0 or more for non-text accents. The table is pinned by
  `scripts/contrast-pairs.test.mjs` (the required pairs by name and a minimum count), so a pair can be added but not
  dropped quietly.
- Brand and fonts: the seven brand values, the primary colours, the font stack, the three font faces and their
  provenance, the licence file, and no font CDN or font package anywhere in `guide/`.

The checker builds its forbidden characters from code points (`String.fromCodePoint(0x2014)`) because the editor tools
decode a typed backslash-u escape into the literal character.

## Brand

The guide uses the OpenValue 2026 brand (decision D7), the same as the OpenBook storefront. All colour values live in
`src/css/custom.css` and nowhere else.

- Seven brand values: navy `#1D252D`, blue `#3D7DC9`, teal `#014046`, sage `#809FA2`, mist `#CBD9DA`, snow `#F1F5F6`,
  white `#FFFFFF`. In the stylesheet they are `--ov-navy` and so on. Everything else maps them onto the Infima variables.
- Light mode: teal primary, navy text and headings, white page, code blocks on snow, navbar navy with white text and a
  blue accent line. Dark mode: navy page, mist primary (10.7:1), white text. Both modes pass every contrast pair, so the
  colour mode switch stays on. If a dark pair ever fails, fix the pair or set `colorMode.disableSwitch: true` (and
  `respectPrefersColorScheme: false`) and delete the dark block; the checker verifies that combination too.
- Blue is 4.2:1 on white: use it for large or bold text and non-text accents only. Sage is 2.8:1 on white: decorative
  only, never text. The checker fails on a text pair that uses either.
- A contrast pair is a foreground variable and a background variable, by name, in `scripts/rules/contrast-pairs.mjs`.
  Declare the variables in `custom.css` with a hex value or one `var()` reference, and add a pair when a component gets
  a new text colour. Every Infima admonition type (note, tip, info, warning, danger, and caution as an alias of warning)
  has its pairs, so any of them may be used on a page.
- The Prism theme in `docusaurus.config.ts` is empty on purpose. Code colours are the `--prism-*` and `--guide-code-*`
  variables of `custom.css`.
- `LevelProgress` colours are the `--guide-level-*` variables of `custom.css`.

### Fonts and provenance

Montserrat is self-hosted: three woff2 faces (latin, weights 400, 600 and 700) in `src/css/fonts/`, loaded by
`@font-face` rules with `font-display: swap`. There is no font CDN and no npm font package, so the site works offline and
from the zip. The licence is `static/fonts/OFL.txt` (SIL Open Font License 1.1), which both builds copy to `fonts/OFL.txt`.
The code font stays the default monospace stack.

Source: the upstream release v7.222 of https://github.com/JulietaUla/Montserrat (static `fonts/webfonts` files, fetched
on 2026-11-08). Each file is subset to the latin range with fonttools 4.68.0 and brotli 1.2.1:

```
pyftsubset Montserrat-<Weight>.woff2 --flavor=woff2 --layout-features='*' --output-file=montserrat-latin-<weight>.woff2 --unicodes="U+0000-00FF,U+0131,U+0152-0153,U+02BB-02BC,U+02C6,U+02DA,U+02DC,U+0304,U+0308,U+0329,U+2000-206F,U+20AC,U+2122,U+2191,U+2193,U+2212,U+2215,U+FEFF,U+FFFD"
```

| File in `src/css/fonts/` | Upstream source file | Source sha256 | File sha256 |
|---|---|---|---|
| montserrat-latin-400.woff2 | https://raw.githubusercontent.com/JulietaUla/Montserrat/v7.222/fonts/webfonts/Montserrat-Regular.woff2 | c2b2cb00987bfe82494d234b5de4a36d217d20536e37e1b98423cc64efefca43 | 7d8a3ca3633130881ee6a8bacd35b6eeac646a04b98e238d5b433c316e480ee7 |
| montserrat-latin-600.woff2 | https://raw.githubusercontent.com/JulietaUla/Montserrat/v7.222/fonts/webfonts/Montserrat-SemiBold.woff2 | 12e1a9890320276b7c7c6fe95ccf06b7619ded7644e5decc56ff2c7449145301 | c4bf29b66d638a468fb7142c965e85f75d5a995f3d47d883a439e3f75b208624 |
| montserrat-latin-700.woff2 | https://raw.githubusercontent.com/JulietaUla/Montserrat/v7.222/fonts/webfonts/Montserrat-Bold.woff2 | 294653dc1466dcda027c8ff4d80f7bc8fb074fc0daacab9afde68c1f7646bb1d | 8d08a32f18f52f4fc926392328ca811ec3b587e52a490f033d9a09a089897197 |
| (licence) static/fonts/OFL.txt | https://raw.githubusercontent.com/JulietaUla/Montserrat/v7.222/OFL.txt | 41f82bb4d24b304f30f7136bc47abdd083782e4265c984160f5649d1e78ea49c | 41f82bb4d24b304f30f7136bc47abdd083782e4265c984160f5649d1e78ea49c |

The checker recomputes the sha256 of every woff2 in `src/css/fonts/` and fails when the last column of this table does
not hold it. Check by hand with `shasum -a 256 src/css/fonts/*.woff2`. Never fetch a font through an npm package or a
font CDN; replace a file by redoing the steps above and updating the table.

## Writing pages

- Admonitions use the bracket form: `:::info[Sync moment]` and `:::tip[Checkpoint]`. The space form renders as plain text.
- Put placeholders such as `<branch>` or `{name}` in inline code. Raw ones break the build.
- Use Markdown image syntax only, never a raw `<img>` tag.
- Quote a YAML title that contains a colon.
- No colour values in pages, components or other stylesheets: no `style={{color: ...}}`, no `style="..."` with a colour.
  Use the variables of `custom.css`.

## Hand-over: publishing the site (owner only)

The workflow `.github/workflows/guide.yml` builds on every pull request that touches `guide/**`. The deploy to GitHub
Pages and the release asset `workshop-2026` run only on a push to `factory` and only when the repository variable
`GUIDE_DEPLOY` is `true`. Nobody but the owner flips it. The steps:

1. In the repository settings, enable Pages with source "GitHub Actions".
2. If the `github-pages` environment restricts branches, allow `factory`.
3. Set the repository variable `GUIDE_DEPLOY` to `true`.

Until then a push to `factory` builds the site and skips deploy and release.
