## QA strategy

### Risk tiers and models

| id | tier | qaModel | why |
|---|---|---|---|
| T1 | elevated | sonnet | new pattern (wrapped theme component), user-facing navbar in two builds, two new checker rules plus wiring edits in `check.mjs`, `lib/site.mjs`, `rules/built.mjs`, `check-built.mjs` (the shared oracle path), a binary asset whose provenance must be reproducible |
| T2 | critical | opus | changes the contrast oracle (`rules/contrast.mjs`: new 3:1 kind) and edits a REQUIRED-pinned text pair; the AA claim for the current crumb rests on rendered evidence (computed size and weight, axe in three engines) that no CI step re-checks; new interactive links with five states |
| T3 | elevated | sonnet | site-wide palette swap with brand-rule (oracle) edits and pinned-test retargets; pattern-following, but lens 4 on AC2 (text on blue) because Infima renders bare `--ifm-color-primary` as text in places no pair names |

Implementers: sonnet (owner policy); opus from rework round 2 per spec 11 precedent if the orchestrator wants it.

### Audit of the orchestrator's shaping decisions

- **D-S1 (exact `#3c7ec9` fill, white label as large text, kind `large`).** WCAG 2.x AA for large text (14 pt bold =
  18.67 px at weight 700, or 18 pt = 24 px) is 3:1, and white on `#3C7EC9` is 4.18:1, so the design is AA-conformant
  IF the label really is large text everywhere it renders. The static checker cannot see font size, so as written the
  guard degrades from "computed 4.5" to "a name in a list". Honest only with all of G-2 (CI pin of size and weight,
  exact allow-list, band witnesses, rendered computed style and axe `passes` at every width, engine, build and mode).
  It also changes the existing REQUIRED entry `['level progress current label', ..., kind: 'text']`
  (`contrast-pairs.test.mjs:39,45`) from 4.5 to 3.0: a pinned check is relaxed, which the owner's standing rule
  forbids. Not a reason to stop: raise it to the owner (OD-1) with the reversible alternative (fill `#3371B8`, 5.01:1,
  normal-size label, pair stays `text`). Bar raised: G-2.
- **D-S2 (crumbs become links).** Agree; the spec requires links, "as today" is a pack slip (residual for the owner,
  no decision needed). Bar raised: links must resolve in both builds (Pages by the built rule, offline by rig clicks)
  and must show a visible focus indicator that a chevron `clip-path` does not clip (G-11).
- **D-S3 (light navbar in light mode, slate in dark).** Agree; forced by spec item 5 (slate logo for light mode).
  Visible change from D7's navy bar: disclose in the spec PR. Bar raised: the rendered logo source per mode is
  asserted by file name (kills a swapped light/dark asset), and no horizontal scroll at 375/390 px with logo + title.
- **D-S4 (BRAND to D18, teal and mist leave, swap tests kept).** Agree, with G-12: sage's fate must be stated (kept =
  restriction stays; dropped = its restricted-rule tests retargeted, never deleted); every brand/site test case is
  retargeted literal for literal with an old -> new title list; node:test titles subset rule holds except that list.
- **D-S5 (spec 11 checker freeze ended).** Agree, but "ended" must not mean "unguarded": C0-7 replaces it with a
  per-task regime (every `guide/scripts/**` file outside the task's fileScope hash-equal to the previous gate, the
  spec base for T1; edits inside fileScope listed with reason; test files insertion-only except listed retargets).
  Trap (G-8): the spec 11 `t6..t13-hashes.txt` files no longer match after fix-11 changed five docs pages and several
  scripts files; never use them. qa-final records the new baseline from committed bytes (`git show <tip>:<f>`).
- **D-S6 (logo provenance check).** Agree; widen (G-3): every file under `guide/static/img/**`, recursively, any
  extension (an SVG or JPEG logo later would otherwise bypass it); name and hash on the same README line, with a
  hash-swap witness; the ok line states the exact file count.
- **D-S7 (screenshots in `visuals/`).** Agree; G-5: shoot Level 2 (`current={2}`), the only page that shows done,
  current and upcoming crumbs at once; shots taken after the last CSS change of T3 and viewed by the verifier.
- **D-S8 (wrap `Navbar/Logo`), D-S9 (built-HTML rules).** Agree. D-S9 adds G-7: the entry-point tests must run in
  `npm run check` without a `guide/build` (CI runs check before build) and must not self-skip.

### Owner decisions (to surface in the spec PR; not blocking Gate 1 for T1)

- **OD-1 (before T2 starts, or explicitly as a spec PR decision).** Keeping the deck blue exact forces two existing
  text pairs to change kind: `level progress current label` text -> `large` (T2, REQUIRED-pinned) and `primary`
  text -> `nontext` (T3, G-1). Both are WCAG-correct with the G-1/G-2 compensating guards, and the new restricted-text
  rule makes `#3C7EC9` impossible in any `text` pair, but literally they relax pinned checks, which the owner's rule
  forbids. Recommendation: proceed (reversible: one fill value and one pair kind), and list both in the spec PR as
  "decisions for Maik" with the alternative (current fill `#3371B8`, 5.01:1). If the orchestrator reads its
  delegated gate authority as not covering a relaxed pinned check, ask before T2.
- OD-2 (information only): at 32 px displayed height the "TECH TRIBES" line is about 10 px tall (res-3). A design
  call; the Level 2 screenshots show it.

### Gate 1 findings: untestable, contradictory or under-guarded criteria, with rewrites

- **G-1 (T3 AC1 + AC2 vs `rules/contrast-pairs.mjs:47`).** `text('primary', '--ifm-color-primary',
  '--ifm-background-color')` fails at 4.18 (light) and 3.74 (dark) once primary is `#3C7EC9`, and AC2's extended
  restricted rule rejects it outright. The table is silent. Rewrite (T3): "The pair `primary` keeps its name and
  variables and becomes `nontext` (primary as an accent, 3:1). A new text pair `primary text` names the variable the
  site uses for primary-coloured text in each mode. A primary-as-text census proves no text renders in bare primary:
  from the built CSS, every declaration whose `color` (or a `--ifm-*-color*` default) resolves to bare
  `var(--ifm-color-primary)` is listed with its disposition (overridden in custom.css to a shade with a text pair,
  element absent from all 11 pages by a rig DOM count, or non-text); plus a rendered walk of every text node on the
  11 pages in both modes, including hovered and active states of links, sidebar, TOC active entry, tabs, breadcrumbs
  and pagination, finds no text in `rgb(60, 126, 201)` and no text on that background below large size, except the
  current crumb label." Infima's TOC active entry is the likely leak; axe only sees it when the scroll position makes
  an entry active.
- **G-2 (T2 AC4: the `large` kind).** Add: (a) `MINIMUMS.large = 3` pinned by band witnesses in `contrast.test.mjs`
  (a `large` pair in [3.00, 3.05) passes, one in (2.95, 3.00) fails; `text` keeps the `#767676`/`#777777` band);
  (b) `contrast-pairs.test.mjs` asserts the exact set of `large` pair names deep-equals
  `['level progress current label']`, and the REQUIRED change for that pair is the only REQUIRED edit; (c) a
  node:test in `level-progress.test.mjs` reads `LevelProgress.module.css` and asserts that the rule styling the
  current crumb's label declares `font-weight: 700` and a font size of at least 18.67 px (rem at the 16 px root), and
  that no `@media` block in the module lowers either (a CI-visible guard for what the exemption assumes; cascade-blind
  by design, so (d) is still needed); (d) rendered: computed `fontWeight >= 700` and `fontSize >= 18.67px`, Montserrat
  700 FontFace `loaded`, no `transform` or `zoom` on the label or its ancestors, at 375, 390, 720 and 1280 px, three
  engines, both builds, both modes, all five level pages; axe `color-contrast` lists every crumb label node in
  `passes` (never in `incomplete`); quote axe's measured ratio for the current label.
- **G-3 (T1 AC2, D-S6).** Rewrite: "fails when any file under `guide/static/img/` (recursive, any extension) has no
  README line holding both its file name and its sha256"; ok line `N images` exact. Witnesses: row removed, one hex
  digit changed, the two rows' hashes swapped, name and hash on different lines, a third image (`img/sub/x.svg`,
  `img/X.PNG`), README missing.
- **G-4 (T3 AC3, AC2 "every background they sit on").** "Close slate shades", "slate for headings where it helps",
  "admonition accents use the blue family" are not falsifiable. Rewrite: dark `--ifm-background-color`,
  `--ifm-navbar-background-color` and the sidebar background resolve to `#1D242C`; every dark surface variable (code,
  Prism, the five admonition contrast backgrounds, table stripe, tabs hover, the observation card and checklist
  surfaces) resolves to a named `--ov-slate-*` token declared in custom.css and listed in the README Brand section;
  light `--ifm-heading-color` resolves to slate `#1D242C`; every bordered admonition's `-dark` variable resolves to
  `#3C7EC9`, one of its six shades, or a named neutral, as the README table states. The surfaces are enumerated in the
  T3 outbox, each with its text and link pairs (links on every surface: a pair or a rendered measurement).
- **G-5 (T3 AC6, D-S7).** Matrix: 11 pages x light/dark x 1280/390 px x Chromium/Firefox/WebKit x Pages build and
  offline zip over `file://` (network aborted): axe full rule set and `color-contrast` 0 violations with `incomplete`
  listed and dispositioned; no horizontal scroll; console per spec 11 FF-5/FF-37 only. Screenshots: the Level 2 page
  (all three crumb states), light and dark, 1280 and 390 px, Chromium, Pages build.
- **G-6 (T2 AC3).** "Muted" and "wraps" made measurable: upcoming fill and label values differ from current and done
  values and the label is a 4.5 `text` pair; at 375 px the crumbs occupy at least two rows (distinct `li` tops) and
  every `li` rect lies inside the nav rect; at 1280 px one row; no horizontal page scroll at 375 and 720 px
  (`documentElement` and `body`).
- **G-7 (T1 AC6, T2 AC5: "one test through the entry point").** `check-built.mjs` defaults to `../build`, which does
  not exist when CI runs `npm run check`. The entry-point tests must exercise the default-dir switch (the code path
  that enables the site rules) on fixture builds, must not read `guide/build`, and must not skip: `npm run check` with
  `guide/build` moved aside gives the same test count. A mutant that disables the switch in `check-built.mjs` must be
  killed by a test, not only by the real `check:built` tail line.
- **G-8 (all tasks).** Docs-untouched proof is `git diff --stat <spec-base> -- guide/docs` empty plus an empty
  `git status --porcelain -- guide/docs`; never `shasum -c` against spec 11's `t6..t13-hashes.txt` or
  `t11-freeze-baseline-hashes.txt` (stale after fix-11). Script baselines come from `git show <spec-base>:<path>`.
- **G-9 (T1 AC5, T2 AC4, T3 AC2: the checker's resolution model).** `rules/contrast.mjs` reads only top-level
  `:root` and `html[data-theme='dark']` and ignores `@media` and scoped selectors (custom.css already re-maps menu
  variables inside `.navbar`). Add to each colour task: every variable a pair names is declared only in those two
  blocks, or each other declaration is listed with a disposition; and a rendered agreement check: for every pair the
  task adds or re-points, the computed colours of a rendered element equal the checker's resolved hex in that mode.
- **G-10 (T1 AC3, AC7).** Add: in light mode the visible logo `img` src ends `openvalue-logo-dark.png` and in dark
  mode `openvalue-logo-light.png` (kills swapped sources); rendered height 32 px; `naturalWidth` 422; the hidden
  variant is not displayed; GitHub and LinkedIn links present with their attributes in the opened mobile sidebar at
  390 px; no horizontal scroll at 375 and 390 px on index and a level page in T1 itself.
- **G-11 (T2 AC2, links).** Add: each crumb link shows a visible keyboard focus indicator (WCAG 2.4.7) that is not
  clipped by the chevron shape (an outline on a `clip-path` element is clipped), with 3:1 against its adjacent colour.
- **G-12 (D-S4, T3 AC1).** Add: sage kept or dropped explicitly; every existing case of `brand.test.mjs` and
  `site.test.mjs` retargeted literal for literal with an old -> new title list in the outbox, none deleted; the
  restricted-rule tests keep one case per restricted colour per role (fg, bg) for each colour still restricted.

### C0: common directive for every verifier of this spec (in addition to the task card and its block)

C0-1 Scratch and evidence. SP = the session scratchpad
(`/private/tmp/claude-501/-Users-maikkingma-OpenValue-AI-Conferences-AgenticBookStore/cb0898b6-6f7b-44fd-8024-5110d0f772e2/scratchpad`).
Evidence files `s14-t<N>-qa-r<k>-*` (implementers `s14-t<N>-impl-*`, qa-final `s14-final-qa-r<k>-*`), cited by
absolute path. Helper scripts `.claude/comms/tmp/s14-t<N>-qa-*.sh|.mjs|.py`. Browser rig: reuse the install in
`SP/s11-guide-verify/` (quote the Playwright and axe-core versions); new scripts in that folder named `s14-...mjs`;
never edit an `s11-*` file; derive from `s11-final-qa-r3-render.mjs` by copy and quote the `diff`. Every round
writes `s14-t<N>-qa-r<k>-hashes.txt` with repo-relative `shasum -a 256` lines only, one per fileScope file present
(PNGs included). Every temporary directory: `mktemp -d "$TMPDIR/s14-t<N>-<role>-XXXXXX"` inside a script file.

C0-2 Snapshot and restore. Never claim an empty `git status`. At snap-before record `git status --porcelain` and the
hashes of every probe-touched file and `cp` each to `SP/s14-t<N>-qa-<File>.orig`; restore every probe by `cp`, then
`shasum` and quote the match before the next probe; snap-after equals snap-before. No git write of any kind (no
checkout, restore, stash, worktree add); base bytes via `git show <spec-base>:<path>` or `git archive -o`.

C0-3 Toolchain. Every npm and node command runs with
`PATH=<SP>/node-v26.13.0-darwin-arm64/bin:/usr/bin:/bin`; quote `node --version` (v26.13.0) and `npm --version`.
Run `npm ci` in `guide/` yourself; never `npm install` or `npm audit fix`; no new dependency. Long runs through
`bash ~/.claude/scripts/with-budget.sh <s> ...` (npm ci 300, each build 300, referee 600).

C0-4 Gates, all exit 0 with the tail quoted: `npm run check`, `npm run typecheck`, `npm run build`,
`npm run check:built`, `npm run build:offline` in `guide/`; referee
`env -u SPRING_AI_ANTHROPIC_API_KEY -u BOOKSTORE_AI_API_KEY -u SPRING_AI_MODEL_CHAT ./mvnw clean verify` from the
repo root with `JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=<SP>/jtmp` (Tests run totals, BUILD SUCCESS, duration). Run
`typecheck` again after both builds exist. Never `spring-boot:run` or `docker compose`.

C0-5 Warning inventory. Grep `warn|deprecat|experimental|error` (case-insensitive) over the npm ci, check,
typecheck, both builds and check:built logs. Accepted at spec 11 FF-4 counts only: npm ci uuid@8.3.2 deprecation 1,
install-scripts notice 6, audit summary 1; `build` localStorage ExperimentalWarning 2; `build:offline` sitemap
disabled 1; check, typecheck, check:built 0. Any other line (a swizzle or `@theme-original` notice included) is
fixable here (FAIL), owned by a named criterion, or unowned (flag it).

C0-6 Checker tests and no regression. Quote node:test `# tests N`, `# pass N`, `# fail 0`; N equals your grep count
of `test(`/`it(` in `guide/scripts/**` (command quoted). N is at least the previous gate's N (T1: the spec base,
from `git archive -o <SP>/s14-t1-qa-base.tar <spec-base> guide`, extracted under `$TMPDIR`, `node scripts/check.mjs`
there, which needs no node_modules). Test titles: previous gate's titles minus current titles (`comm -23`) is empty
except the renames the task lists old -> new. One throwaway failing test file in the discovery pattern makes
`npm run check` exit non-zero; remove it and re-hash. `diff` the check.mjs output against the previous gate: only the
contrast pair lines, the tests line and the task's new rule lines may differ; `structure (11 pages`,
`coverage (anchored 304/304, 15 manual rows, 0 entries pending)` and the content lines are byte-identical.

C0-7 Scripts regime (replaces spec 11 C0-12; D-S5). Every `guide/scripts/**` file outside the task's fileScope is
hash-equal to the previous gate (T1: the spec base). Inside the fileScope each changed file is listed with its
reason. Edited test files are insertion-only except retargets the task lists old -> new. `MINIMUM_PAIR_COUNT` equals
the live pair count and is never lower than before; no pair is removed or renamed (`comm -23` of pair names vs the
previous gate, empty); REQUIRED entries are unchanged except the one a criterion names. A change to `check.mjs` or
`lib/site.mjs` re-runs the wiring mutant: the new rule's call removed makes a `runChecks` test fail.

C0-8 Docs untouched (G-8). `git diff --stat <spec-base> -- guide/docs` prints nothing and
`git status --porcelain -- guide/docs` prints nothing.

C0-9 Probes fail for the stated reason. Quote the failing assertion or rule message (rule, file, line) per probe; a
syntax, import or build error is no evidence. Checker mutants are applied by you, each in its own run with its own log.

C0-10 Rendered checks. Pages build `docusaurus serve --port <P> --no-open` (T1 3341, T2 3342, T3 3343, qa-final
3349; implementers 3351 to 3353) after an `lsof -iTCP:<P> -sTCP:LISTEN` preflight, stopped by PID, port free
afterwards. Offline build over `file://` from a path with a space, every non-`file:` request aborted by
`context.route` and counted (expected 0, plus one control fetch that moves the counter). A brand-new browser context
per build and mode. Assert `document.documentElement.dataset.theme` equals the intended mode before every measurement
(a failed switch makes a "dark" run measure light). Console accounting per phase: only spec 11 FF-5 (WebKit offline
CORS pair) and FF-37 (axe preload triple on `file://`, axe phase only). Dev-mode walk for new React components (T1,
T2): `docusaurus start --port 344<N> --no-open`, console clean (no `validateDOMNesting`, hydration or key warnings).

C0-11 Contrast honesty. (a) Recompute every printed pair ratio yourself (WCAG 2.x). (b) Rendered agreement (G-9):
for every pair the task adds or re-points, the computed colours of a rendered element equal the checker's resolved
hex in that mode. (c) Every variable a pair names is declared only in top-level `:root` or
`html[data-theme='dark']`, or each other declaration is listed with a disposition. (d) axe: report `violations` and
`incomplete` for every run; any node of the task's elements in `incomplete` is a finding unless you computed its
ratio yourself from computed styles. (e) Hover, focus and active states measured after `locator.hover()` and
`locator.focus()`; axe does not see them.

C0-12 Scope. Product files changed (`git diff --name-only <pr-base>...HEAD` plus the working tree, minus `.claude/`
and `agent-os/`; T3 also `agent-os/specs/2026-11-10-1610-14-guide-branding/visuals/**` in scope) are a subset of
the fileScope. Quote the list.

C0-13 Evidence timing by `stat` mtimes (impl artifact, last product file, your gate logs), never frontmatter.

C0-14 Hard limits. Never push, dispatch a workflow, enable Pages, set a variable, create a release or tag, write git,
or add a dependency. The deck PDF is read-only and never copied into the repo. No absolute home path in any committed
file (`git grep -n '/Users/' -- guide` prints nothing).

C0-15 Hygiene. `git grep --no-index -n -P '[^\x00-\x7F]' -- <fileScope text files>` prints nothing (with a positive
control file that prints); no em dash; no `lab` word; check marks and other glyphs only as escapes (verify with
`od -c` on the line, never by reading).

C0-16 Decisions. Report each shaping decision D-S1 to D-S9 and each Gate 1 finding the task implements as met or
disclosed, with the evidence line.

### Task directives (qaDirective, in addition to all of C0)

**T1 (elevated, sonnet). Logo asset and navbar.**
1. Provenance by independent re-run: run `python3 -I .claude/comms/tmp/res-3-extract.py "<deck>" <mktemp dir>`
   (deck read-only) and `cmp` each PNG with the committed one. If bytes differ, decode both with your own stdlib
   decoder and compare the RGBA arrays. From your own decode prove AC1: IHDR 422x77, bit depth 8, colour type 6;
   four corner alphas 0; the two alpha channels byte-identical; RGB of every pixel with alpha > 0 exactly
   (255,255,255) for light and (29,36,44) for dark (say how alpha-0 pixels are treated). Your sha256 equals the
   README rows. No `.pdf` among tracked or untracked files; README names the deck file and page 1, never a path.
2. Logo rule (G-3) witnesses, each makes `npm run check` fail with the rule message: README row removed; one hex digit
   changed; the two rows' hashes swapped; name and hash on separate lines; `static/img/sub/x.svg` added;
   `static/img/X.PNG` added; README missing. Mutants (node:test fails): the rule returns no errors; recursion removed;
   the extension filter narrowed to `.png`; the call removed from `lib/site.mjs` or `check.mjs`. Real ok line quotes
   `2 images`.
3. Navbar rule witnesses, each makes its self-test fail: wrong href (and `https://openvalue.eu/`,
   `http://openvalue.eu`); no target; `target="_self"`; logo `rel="noopener"` only and `rel="noreferrer"` only;
   GitHub `rel="noopener-noreferrer"` (one token: kills substring matching); empty accessible name (`aria-label=""`,
   `alt=""`); logo inside the title link; title link with a target; title link href not the base URL; GitHub missing;
   LinkedIn missing; a page with no navbar; a build with zero HTML files. Mutants: the rule returns no errors; the
   default-dir switch in `check-built.mjs` disabled (G-7: a test must kill it); the rule wired unconditionally into
   `scanBuilt` makes the existing `built*.test.mjs` fail (proves isolation), then restore. `built.test.mjs`,
   `built-checkbox.test.mjs`, `built-powershell.test.mjs` hash-equal to the spec base and green. Real `check:built`
   tail line quoted exactly, its page count equal to `find guide/build -name '*.html' | wc -l`.
4. G-7: move `guide/build` to SP (snapshot rules), run `npm run check`: the same test count, no skip; move it back.
5. Rendered (3 engines x 2 builds x 2 modes, index and one level page): G-10 items; logo link
   `href`/`target`/`rel` and accessible name; title link goes home (click from a level page: Pages lands on the base
   URL, offline on `#/`); no horizontal scroll at 375, 390 and 720 px; Tab order logo link then title link with a
   visible focus indicator; axe full and `color-contrast` at 1280 and 390 px with `incomplete` listed; navbar
   text, hover (after hover), active and background computed colours equal the checker's hex (C0-11 b).
6. Config: `navbar.logo` is not set (the original renders no image in the title link); no `/AgenticBookStore/`
   literal under `src/theme/`; `task-list-labels.test.mjs` unchanged and green; `docusaurus.offline.config.ts`,
   `package.json` and the lockfile hash-equal to the spec base.
7. Contrast: the navbar pairs in both modes, recomputed; `MINIMUM_PAIR_COUNT` equals the live count.

**T2 (critical, opus). Breadcrumb. Lens 4 depth on every criterion.**
1. Pair table diff vs the T1 gate: only additions plus the one REQUIRED kind change (`level progress current label`
   text -> large), listed old -> new; the `--guide-level-current-*` and `--guide-level-inactive-*` names kept; a
   table state -> pairs: done label (text), done check mark vs done fill (nontext), upcoming label (text), current
   label (large), current fill vs page (nontext, existing), every state-carrying outline or separator (nontext).
2. `large` kind (G-2 a, b), each must make node:test fail: `MINIMUMS.large` 2.9; `MINIMUMS.large` 3.1; the band
   witnesses missing; a second pair (`links`) switched to `large` (allow-list); the allow-list check loosened to
   `includes` while a second name is added; `text` threshold 4.4. Control: the current label pair reverted to `text`
   makes `npm run check` fail at 4.18 (proves the exemption is load-bearing and the ratio is real).
3. Large-ness (G-2 c, d): static mutants `font-weight: 600`, `font-size: 1.125rem` (18 px), and an added
   `@media (max-width: 996px)` that lowers the size each fail the CSS pin test; the rendered matrix of G-2 d with
   values quoted per width, engine, build and mode; axe `passes` contains every crumb label node, and the current
   label's measured ratio is quoted.
4. Built rule witnesses (AC5 list plus): `aria-current` on the `li` instead of the link; `aria-current` on a done
   crumb as well; `data-state` contradicting the position; check mark without `aria-hidden`; done crumb without its
   "completed" text; a `Level 5` crumb; a Level progress nav on a non-level page; a level page missing from the
   build; the Level 3 page showing `current` 2. Mutants: the rule returns no errors; `<=` for `<` in the done test
   (killed by the current-2 fixture); the file -> level mapping off by one; the default-dir switch disabled (G-7).
   Real tail line quoted with `5 level pages`.
5. Links: every crumb `href` in the Pages build names an existing built file of that level; offline: click each of
   the five crumbs on the Level 2 page and quote the resulting h1; no `/AgenticBookStore/` literal in the TSX;
   exactly one `aria-current="step"` per level page, on the current crumb's `a`.
6. Layout (G-6) at 375, 720 and 1280 px with numbers quoted; focus (G-11): Tab through the five crumbs, each focus
   indicator computed (outline or box-shadow not `none`), not clipped (screenshot crop per crumb), 3:1 vs adjacent.
7. Component guard: `current={5}`, `{-1}`, `{1.5}` on one level page (C0-2 restore) each make `npm run build` fail;
   structure rule files hash-equal to the T1 gate; the five pages unchanged (C0-8).
8. Rig LevelProgress check rewritten for the new markup (AC6): `diff` vs the s11 script quoted; runs in 3 engines on
   both builds on all five level pages; dev-mode walk on the five level pages, console clean.

**T3 (elevated, sonnet). Deck colours and visual sweep. Lens 4 on AC2.**
1. Brand rule: `BRAND` deep-equals the D18 values plus the neutrals kept (list them, sage stated, G-12); `PRIMARY`
   `#3C7EC9` in both modes. Must fail: light primary restored to `#014046`; dark primary `#CBD9DA`; primary set to
   the shade `#3371B8` (kills "any blue" matching). Retarget list old -> new for `brand.test.mjs` and `site.test.mjs`.
2. Restricted rule: `#3C7EC9` (any case) as fg and as bg of a `text` pair each fail; the `nontext` and named `large`
   pairs pass; mutant dropping the bg check fails a test. State whether D7 blue `#3D7DC9` is still restricted.
3. G-1: the `primary` pair disposition, the `primary text` pair, the primary-as-text census from the built CSS with a
   disposition per declaration, and the rendered walk with states (TOC active entry forced by scrolling, sidebar
   active, tabs active, breadcrumb, link hover, pagination hover).
4. Shades: the six values recomputed by the Docusaurus HSL ratios with your own script, controlled by reproducing the
   old teal shades of the spec base exactly.
5. Surfaces (G-4): the enumerated surface list, each with text and link pairs or a rendered link measurement >= 4.5
   in both modes; dark page, navbar and sidebar computed backgrounds `rgb(29, 36, 44)`.
6. Teal and mist gone: `git grep -n -i -E 'ov-teal|014046|ov-mist|cbd9da' -- guide/src` prints nothing (control: the
   same grep at the spec base prints the base lines); each base `ov-teal` use listed with its new value.
7. README Brand claims: every hex and ratio in the section equals custom.css and the checker output (script, quoted).
8. Sweep (G-5): your own independent run of the full matrix, never the implementer's record alone; the sweep record
   in `visuals/` agrees with your counts.
9. Screenshots (G-5): view each PNG; all three crumb states visible, the right logo per mode, no clipping; mtimes
   after the last CSS change; sha256 recorded.
10. Re-run on the final palette: T1 G-10 logo source per mode and navbar agreement, T2 large-ness and axe `passes`
    for crumb labels, both modes, Chromium both builds at least.
11. Each `src/components/*.module.css` edit listed; `var()` only.

### qa-final focus

1. Integrity: per-task hash baselines vs committed tip bytes; the union of baseline paths equals the spec diff's
   product files; scripts files changed across the spec listed with reasons; record the new `guide/scripts/**`
   baseline (`checker-baseline-hashes.txt`) from `git show <tip>:<f>`.
2. Checker no-regression across the spec: node:test titles at the spec base minus tip titles equals the union of the
   tasks' rename lists; totals monotone; `MINIMUM_PAIR_COUNT` equals the live count; the `large` set is exactly one
   name; check.mjs non-colour lines byte-identical to the base.
3. Palette end state: every pair in both modes recomputed; rendered agreement for navbar, links, crumbs, body,
   admonitions; primary-as-text census re-run on the tip build.
4. Cross-task: the T1 navbar and T2 crumbs under the T3 palette (logo source per mode, large-ness and axe `passes`,
   focus indicators), full G-5 matrix on the tip.
5. The CI artifact: download the offline zip of the spec PR's `guide.yml` run (bound by `gh run list --commit <full
   sha>`), unzip into a path with a space, run the rig on it (logo, colours, crumbs, external links).
6. External links, one request each: `https://openvalue.eu`, the GitHub repo, the LinkedIn profile (LinkedIn may
   answer 999 to automated clients: record it, a 404 is a finding).
7. Docs untouched over the whole spec (C0-8); README claims census; screenshots taken on the final CSS.
8. CI green on the spec PR head (guide.yml build, ci.yml jobs), PR readiness (base ancestor, no gitlinks).

### Follow-ups outside this cycle's QA scope

- F1: the logo recipe script lives only in `.claude/comms/tmp/`; reproduction needs the owner's local deck. README
  prose is the only durable record (residual).
- F2: the large-text exemption is CI-guarded only by the source pin (G-2 c); its rendered proof is local (the rig is
  not in CI). Residual for Gate 2.
- F3: pack wording "as today" for crumb links (D-S2) is wrong; owner residual.
