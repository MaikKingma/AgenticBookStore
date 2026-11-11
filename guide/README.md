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
- Brand and fonts: the deck palette values (decision D18), the primary colour of each mode, the dark surfaces, the
  admonition borders, the font stack, the three font faces and their provenance, the licence file, and no font CDN or
  font package anywhere in `guide/`.
- Logo provenance (rule `logo`): every file under `static/img/` has a README line with its name and sha256 (see Logo and
  provenance below).

The checker builds its forbidden characters from code points (`String.fromCodePoint(0x2014)`) because the editor tools
decode a typed backslash-u escape into the literal character.

## Brand

The guide uses the palette of the keynote deck "Shift Left, Look Right" (decision D18). It replaced the D7 teal palette
for the guide only; the OpenBook storefront keeps D7. All colour values live in `src/css/custom.css` and nowhere else.

- Deck colours: deck blue `#3C7EC9` (`--ov-blue`) and slate `#1D242C` (`--ov-slate`, the dark background and the colour
  of the slate logo file). The D7 neutrals still in use are sage `#809FA2` (`--ov-sage`, decorative only), snow
  `#F1F5F6` (`--ov-snow`) and white `#FFFFFF` (`--ov-white`). D7 navy, teal and mist are gone from the guide.
- Deck blue is 4.18:1 on white and 3.74:1 on slate, below the 4.5:1 of text. So it is a non-text accent only (navbar
  accent line, admonition borders, outlines and bars), and text and filled controls use an AA shade of it. The primary
  colour (`--ifm-color-primary`, the colour Infima paints links, the table of contents entry and buttons in) is `#3371B8`
  in light mode (5.01:1 on white) and `#79A7DA` in dark mode (6.23:1 on slate, 5.10:1 on the raised surface
  `--ov-slate-800`). Each mode has its six shade variables (`-dark`, `-darker`, `-darkest`, `-light`, `-lighter`,
  `-lightest`), the Docusaurus ratios of its primary, ordered from dark to light:

  | Variable | Light mode | Dark mode |
  |---|---|---|
  | `--ifm-color-primary-darkest` | `#244F81` | `#3373BA` |
  | `--ifm-color-primary-darker` | `#2B609C` | `#518DCF` |
  | `--ifm-color-primary-dark` | `#2E66A6` | `#5E96D3` |
  | `--ifm-color-primary` | `#3371B8` | `#79A7DA` |
  | `--ifm-color-primary-light` | `#3A7CC8` | `#94B8E1` |
  | `--ifm-color-primary-lighter` | `#4383CB` | `#A1C1E5` |
  | `--ifm-color-primary-lightest` | `#5F95D3` | `#C9DBF0` |

- Light mode: white page, slate text and headings (15.66:1), code blocks and inline code on snow, links in the primary
  (underlined in page text), a light navbar with slate text and a deck blue accent line. The active entry of the sidebar
  menu, the breadcrumbs and the tabs use `--ifm-color-primary-dark` (4.91:1 on the hover tint `--ov-slate-100`).
- Dark mode: the page, the navbar and the sidebar are slate `#1D242C`; every other surface is a named slate token (list
  below), text is white (15.66:1) and links are the primary. Both modes pass every contrast pair, so the colour mode
  switch stays on. If a dark pair ever fails, fix the pair or set `colorMode.disableSwitch: true` (and
  `respectPrefersColorScheme: false`) and delete the dark block; the checker verifies that combination too.
- The slate scale is slate mixed with white (100 to 500) or black (900); 700 and 800 are the raised dark surfaces:

  | Token | Value | Used for |
  |---|---|---|
  | `--ov-slate-100` | `#E6EBF0` | light mode hover tint, navbar hover and mobile menu, highlighted code line, warning admonition, note, tip and info code tints |
  | `--ov-slate-200` | `#CFD7DF` | light mode borders and code border; dark mode light grey text (emphasis 600 and 700) and scrollbar hover |
  | `--ov-slate-500` | `#4B5966` | light mode mid-dark neutral text (emphasis 600 and 700) and scrollbar hover |
  | `--ov-slate-700` | `#323E49` | dark mode navbar hover and mobile menu, highlighted code line, emphasis 200; light mode danger inline code tint |
  | `--ov-slate-800` | `#2A333C` | dark mode inline code, table stripe, hover tint, admonitions, upcoming crumb fill, emphasis 100, danger code tint |
  | `--ov-slate-900` | `#171C22` | dark mode code blocks (also the observation card), danger admonition, code tints |

- Dark surfaces, one token each: code blocks (`--prism-background-color`) and the observation card of Level 0, which is
  a code block: `--ov-slate-900`; inline code, table stripe, tabs and menu hover (`--ifm-hover-overlay`) and the note,
  tip, info and warning admonitions: `--ov-slate-800`; the highlighted code line: `--ov-slate-700`; the danger
  admonition: `--ov-slate-900`. A checklist sits on the page (`--ov-slate` in dark mode). The checker fails when one of
  these is not a `var(--ov-slate-<step>)` in the dark block, and when a slate token is missing from this section.
- Admonition borders (the `-dark` variable of each type; the exact deck blue, a shade of the primary or a named neutral):

  | Type | Light mode | Dark mode |
  |---|---|---|
  | note | sage (decorative, no pair) | sage |
  | tip | `--ifm-color-primary-darkest` `#244F81` | `--ifm-color-primary-lighter` `#A1C1E5` |
  | info | deck blue `#3C7EC9` (3.81:1 on snow) | `--ifm-color-primary` `#79A7DA` |
  | warning and caution | slate `#1D242C` | white `#FFFFFF` |
  | danger | deck blue `#3C7EC9` (3.74:1 on slate) | `--ifm-color-primary-light` `#94B8E1` |

- Buttons are the primary with the inverse text colour: white on `#3371B8` in light mode (5.01:1), slate on `#79A7DA` in
  dark mode (6.23:1); the hover uses `--ifm-color-primary-dark`. The `LevelProgress` colours are the `--guide-level-*`
  variables (current crumb `#3371B8` with a white label, done crumbs green, upcoming crumbs muted).
- The checker (rules `brand` and `contrast`) fails on: a primary other than the two AA shades above (the exact deck blue
  included), shades out of order, a dark background that is not slate, a dark surface that is not a named slate token, a
  light heading colour that is not slate, an admonition border outside the sets above, and any text pair that uses deck
  blue or sage as its colour or its background. Deck blue may only appear in non-text pairs.
- A contrast pair is a foreground variable and a background variable, by name, in `scripts/rules/contrast-pairs.mjs`.
  Declare the variables in `custom.css` with a hex value or one `var()` reference, and add a pair when a component gets
  a new text colour. Every Infima admonition type (note, tip, info, warning, danger, and caution as an alias of warning)
  has its pairs, so any of them may be used on a page.
- The Prism theme in `docusaurus.config.ts` is empty on purpose. Code colours are the `--prism-*` and `--guide-code-*`
  variables of `custom.css`.

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

### Logo and provenance

The navbar shows the OpenValue wordmark in front of the title (decision D18). The two files in `static/img/` come from the
keynote deck "Shift Left, Look Right" (`Shift Left, Look Right.pdf`, kept outside this repository; the PDF never enters it).
The wordmark on page 1 is an embedded raster image (image XObject `/Im4`, 422x77 pixels, white) with a separate 8-bit soft
mask that already is the anti-aliased alpha channel. A short Python 3 script with the standard library only (`re`, `zlib`,
`struct`) inflates both streams and writes two RGBA PNGs at the native 422x77 size: the colour channels are one constant
colour (white for the light file, slate `#1D242C` for the dark file) and the alpha channel is the soft mask, byte for byte,
so the two files have identical alpha and alpha 0 in all four corners. There is no rendering, cropping, keying or
resampling. The deck holds no larger copy of the wordmark, so 422x77 is the ceiling.

Mode: the slate file shows in light mode (light navbar), the white file in dark mode (slate navbar). The navbar displays
the logo at a height of 32 pixels.

| File in `static/img/` | Source deck | Page | Image object | How it was made | File sha256 |
|---|---|---|---|---|---|
| openvalue-logo-light.png | Shift Left, Look Right.pdf | page 1 | /Im4 with soft mask | recoloured white (255,255,255), no resampling | a822369154fc67fb17bcce84ddd262fa3ecf6fa1daadddbc9e1254ca8568c472 |
| openvalue-logo-dark.png | Shift Left, Look Right.pdf | page 1 | /Im4 with soft mask | recoloured slate (29,36,44), no resampling | 7609e6dcb361444a8ad8ab243bc9bc4891a36e712f20e9c5166f8192afb56933 |

The checker (rule `logo`) fails when any file under `static/img/`, in any folder and with any extension, has no README line
that holds both its name and its sha256 (the ok line states the number of images). Check by hand with
`shasum -a 256 static/img/*.png`. To replace a logo, redo the steps above and update the table.

## Table regions

Docusaurus renders a Markdown table as a box that scrolls when the table is wider than the page, and that box cannot take
keyboard focus (axe `scrollable-region-focusable`, WCAG 2.1.1). At 390 px wide most tables are wider than the page (axe
flagged the three that hold no link), so a keyboard user could not scroll them. The rehype plugin `src/plugins/rehype-table-regions.mjs` fixes it for every table
of every page, with no change to a page:

- It wraps each table in a `div` with `tabindex="0"`, `role="region"`, an `aria-label` and the class
  `guide-table-region`. The role is on the wrapper and never on the table, so the table keeps its table semantics.
- The label is the text of the nearest heading before the table (inline code, emphasis and `kbd` keep their words, the MDX
  id comment of `## Title {/* #id */}` is dropped). Several tables under one heading are numbered ("Packages (table 1 of
  2)"), a table before any heading is called "Table", and a label already used on the page gets a count ("Notes, 2").
  Every label is unique on its page, because two regions with one name are an axe `landmark-unique` violation.
- It is registered in `docusaurus.config.ts` as `rehypePlugins: [rehypeTaskListLabels, rehypeTableRegions]`; the offline
  config spreads that config. `scripts/task-list-labels.test.mjs` pins the whole list, so dropping either plugin fails
  `npm run check`. The plugin imports nothing from `node_modules` (a self-test reads its import lines).
- `src/css/custom.css` (block "Table regions"): the region scrolls (`overflow-x: auto`) and the table inside is
  `display: table`, so there is one scrollbar. The focus ring is the one of the level progress breadcrumb
  (`--guide-level-focus`, 3 px solid, 2 px outside the box, on `:focus-visible`), and `scroll-margin-top` keeps a focused
  region below the sticky navbar.
- `npm run check:built` runs the rule `scripts/rules/table-regions-built.mjs` on every table inside an `article`: its
  parent must be a `div` with `tabindex="0"`, `role="region"` and a non-blank `aria-label`, it must be the only element in
  that region, and no two names on one page may be equal (case and spacing ignored). The ok line gives the number of
  tables and pages. The rule reads the Pages build. The offline build is rendered in the browser, so its html holds no
  table and the rule prints no line for it; the rendered sweep (axe and keyboard, three engines, both builds) is the proof
  there.

Write tables as plain Markdown and add no wrapper of your own.

## Writing pages

- Admonitions use the bracket form: `:::note[At a glance]` and `:::tip[Checkpoint]`. The space form renders as plain text.
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
