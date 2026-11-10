---
name: qa-docs-checker-specs
description: Strategy traps for docs-site specs whose oracle is a home-made checker (Docusaurus guide, spec 11): checker mutation probes, coverage-manifest gaming, plan-parser witnesses, hex false positives, font-loading evidence, generated folders before their ignore rule, regex MDX scanner construct lists and fail directions, self-tests that read live content, axe preload noise on file://, shape-vs-count rules, built meta descriptions, substring command entries, branch lag, derived scenario prompts, print margins, designated-source ranges; read before the strategy of any docs or checker task
metadata:
  type: feedback
---

When a spec's acceptance criteria are graded by a checker the team writes itself (structure, coverage, hygiene,
contrast), the checker is production code AND the oracle for every later task. Tier the checker tasks critical.

1. **Input probes are not enough.** "An em dash in index makes check fail" proves wiring, not the rule's edges. Add
   mutation probes on the checker source (rule returns no errors, comparator ignores order, count guard removed,
   whole-file instead of section-scoped search) that `node --test` must kill. Also prove test discovery: a throwaway
   failing test in the discovery pattern must turn `npm run check` red (catches `;` chaining and zero-test globs).
   That probe proves discovery, not a vacuity guard: `node --test` exits 0 with `tests 0`. Put a criterion on the
   harness task: check fails on zero executed tests (witnesses: test files deleted, emptied, glob to nothing).
   Lockfiles hold third-party names (`postcss-lab-function`), so a word ban must exempt the top-level lockfile by
   exact path.
2. **Freeze the oracle.** Content tasks hash every checker file against the last gated baseline (a hash chain), not
   only a fileScope rule. A later fix cycle makes old hash files stale: rebase on `git show <spec-base>:<f>` (spec 14).
   A dropped feature may name an artefact the site never had (spec 11 D16): grep the checker for a pin and tell
   computed counts from fixture expectations. A pack-derived allow-list inherits pack text the decision left stale,
   so the dropped feature passes: pin it after the freeze.
3. **Coverage-manifest gaming.** Name these witnesses: snippet only inside `{/* */}` or `<!-- -->` must not count; a
   command only in prose inline code must not count when `kind: command`; a snippet that exists in the source but in
   another section must fail; a pack line plus an appended command must fail (whole-line, not `includes`). Guard
   silent exclusion with an independent literal sweep (extract every code span and fenced line of the source ranges,
   diff against manifest plus manual rows). List snippets shorter than 8 chars: they match anything.
   A source-text manifest forces Markdown-exact wording (a link, emphasis or backticks inside a snippet break it):
   hand the content implementers the forced forms at the freeze checkpoint. A rendered cross-check of the same
   manifest must delete whitespace, backticks and `|` on both sides (Docusaurus code lines are `.token-line` spans
   with `<br>`: no newline in textContent; tables render no pipe). Facilitator-only ranges can overlap lines the
   manifest requires: absence greps name facilitator-only phrases. Optional quotes vs "anchored by the manifest"
   criteria conflict after the freeze: settle them at the checkpoint.
4. **Plan-parser witnesses.** Reword only the continuation line of a wrapped source bullet or Goal (kills first-line
   parsing); drop backticks (kills markup-stripping normalisation); a heading with two parentheticals
   (`(LSP + MCP) (20 min)`) needs the last one; an in-section `(8 min)` must not be accepted as the level time; the
   source may quote the anchor phrase elsewhere (a table cell), so a count guard needs a sixth-block probe.
5. **H1 and heading rules** must skip fenced code: bash `# comment` lines are not headings.
6. **Hex colour rules** hit Markdown anchors: `#add-book-reviews` matches `#[0-9a-f]{3}\b` because `-` is a word
   boundary. Witnesses: `#add-...`, `#bad`, `#cafe`, `{#deadbeef}`. "Colour value" also covers CSS named colours.
7. **Contrast checkers**: recompute every ratio independently; witnesses `#767676`/`#777777` on white straddle 4.5;
   a saturated blue pair catches swapped R/B coefficients; a `var()` chain two deep; dark values falling back to
   `:root`. Hover states are invisible to axe: hover in Playwright and compute the ratio.
8. **Font-loading evidence**: `document.fonts.check()` returns true when no face matches (vacuous). Use
   `document.fonts.load('<w> 16px Family')` then FontFace `status === 'loaded'`, with a negative control (one woff2
   deleted in a build copy shows `error`). Faces load lazily, so unused weights stay `unloaded` without `load()`.
9. **"Types used" criteria drift.** A rule keyed to what pages use today (admonition types with contrast pairs) goes
   stale when a later task adds a type. Rewrite to a closed set at strategy time.
10. **Generated folders before their ignore rule.** If task N creates a build folder and the ignore line arrives in a
    later task, the orchestrator's `git add` can sweep it in. Check every folder a task generates against the ignore
    file AT that task (`git check-ignore -v`), and move the line forward if needed.
11. **Docusaurus link checks**: the hash-router build skips SSG and link checks (exit 0 on broken links), so only the
    browser build is evidence (a `.md` link failing both proves `onBrokenMarkdownLinks: 'throw'` survived a spread).
    External links are never checked: inventory them at strategy time; refs a later publish creates get a deploy-gate
    check, every other URL one request in qa-final (spec 11 R-G2, D17).
12. **Docusaurus markup traps for a page grammar**: `{#id}` heading ids do not compile under `future.v4` (use
    `{/* #id */}`, spec 11 plan.md A3); `- [ ]` task lists render bare disabled checkboxes that fail axe `label`
    (critical). At strategy time say whether "axe" means color-contrast only or the full ruleset, and give every
    framework violation an owning task (a local rehype plugin needs no dependency, but sits in the config, outside
    content and checker scopes). Line-level H1 rules miss JSX `<h1>` and H1s nested in a blockquote or list: pair
    them with a rendered `h1` count per page.
13. **Rehype label plugins and built-HTML guards** (spec 11 T4a): a text walk over `element` nodes drops the text
    of inline MDX JSX (`mdxJsxTextElement`); name the witness at strategy time (`<kbd>` in an item) or judge the gap
    on reachability through other frozen rules (checklists pinned to pack text). A built-HTML guard passes
    vacuously with zero matches and a wrong default dir; when earlier CLI tests and `package.json` are pinned, an
    in-checker floor breaks them, so require a quoted exact tail line (`N html files, M checkboxes`) in every later
    verifier instead.
14. **Regex scanners of MDX** (spec 11 T5, four FAIL rounds): each fresh-probe round found one new construct where the
    scanner and MDX disagree about code, comments or tags. Neither a false span NOR a missed span is safe: a false span
    blanks or hides text, a missed span hands its literal `</TabItem>` or `{/*` to the tag reader and comment blanker
    (item ends early, rest of page blanked: fail-open). "End the paragraph early" is NOT fail-closed (refuted by builds).
    CommonMark continues a paragraph at an ordered marker other than 1, an empty item, a `<tag>` or `{expr}` followed by
    text, a `|` line without a delimiter row, `  >` in a list item: never list those as paragraph ends. Cheap nets that
    match MDX: an orphan closing tag and an unclosed or nested comment opener never build, so report them. Even so a
    closed construct list may not terminate on a regex model; for a rule whose truth is the rendered page, check the
    build in CI (`check:built`) at strategy time instead of hardening the source model. Source checkers can never stop
    deliberate hiding (ESM lines, attribute text, CSS): pair rule (b)-type coverage with a rendered textContent
    cross-check in the content verifiers. Before sending a contract as a Required Action, read the pinned tests it
    touches, and trace every consumer before calling an over-approximation fail-closed. Swapping in the MDX parser
    breaks a "dependency free" checker rule and a dependency key-set test: a plan decision, never a last-round fix.
15. **Frozen self-tests must not read the content tree** (spec 11 A10/T5c): "the real guide passes" tests that read
    live `docs/` freeze the content with the checker, so the first content task fails them. Before the freeze
    baseline, grep the checker tests for reads of the content dir and move them to a committed byte-equal snapshot
    (compare the tree ids). A later flag-removal task (marker ban) then collides with the snapshot: make it an
    opt-in the CLI sets on its default root, scope it to `<root>/docs` (fixtures carry markers), and let only the
    CLI copy tests change.
16. **Semantic source reviews of content pages** (spec 11 T6): verifiers search only the main source file and flag
    sourced clauses as embellishments; the content spec itself and app specs or product docs hold sources too. Direct
    a whole-pack search before any "embellished" mark; a usage instruction derived from a cited mechanism is faithful,
    a tool-behaviour claim never is. Facilitator-only ranges overlap participant facts: bar facilitator-only content,
    accept a fact a participant line repeats (cite both); a range that is one page's designated source with named
    exclusions (risk table -> troubleshooting) is ruled by its exclusion list, not its label. Cross-page link texts
    quoting another task's headings: heading-keep rule plus a rendered text check. Print: `page.pdf` is margin 0, a
    dialog about 10 mm; check both, with a pre-trim control build. A trim-for-fit card names what may not drop
    (optional/stretch, pack facts), not only tokens; its oracle is a word-sequence diff vs the previous bytes with
    bullet markers and `; ` joins stripped. A literal ban needs replacement wording that keeps the referent (T12).

17. **axe on `file://`** (spec 11 T4a to T7): axe-core's CSSOM preload XHR fails, logging `Couldn't load preload
    assets:` plus a Chromium CORS error and `net::ERR_FAILED`, one triple per run. Accept it only in the axe phase with
    phase-tagged one-per-run counts, never in Firefox, Pages or dev mode; full-axe evidence includes the http build.
18. **Shape rules are not count rules**: a frozen structure rule that validates a Checkpoint's shape passes a page
    with none (a retitled tip escapes). Put per-page block counts into the content verifiers' rendered assertions,
    with one must-fail retitle mutant. Before sending an anchor or markup form in a directive, read the skeleton and
    the checker constant that pins it (spec 11 FF-36: bare `## Level cleared when`, no comment anchor). Run any author
    alphabet over the grammar's pinned strings first (T10: a colon in exact track labels); exempt exact strings, never a
    character. A count of 0 is valid when every candidate is a Gaps row or barred by the grammar; pin it by add-one.
19. **Framework-derived and repo-relative text** (spec 11 T8): Docusaurus builds `<meta name="description">` from the
    first paragraph and dropped the span between two colons of a pinned Goal line; no source rule sees it. Grep the
    built descriptions of every page at strategy time; the fix is a quoted `description:` frontmatter (checkers that
    skip frontmatter accept it). Manifest command entries are substring matches (`which jdtls-lsp` passes): ask
    content verifiers for whole-line command equality. A class a pack prompt names may be missing only because the
    spec branch lags the integration branch: check `origin/factory` before calling the pack wrong.
20. **Derived prompts for pack scenarios** (spec 11 T9 `.env`): when the pack gives GIVEN/WHEN/THEN but no prompt
    words, a fenced prompt is a valid forced form only if it restates the WHEN clause and adds no output, path, tool
    or flag, and the Expected line is the THEN clause verbatim. A GIVEN no page provides is stated as a precondition
    (owner note), never an invented setup step. Check deny-rule scenarios against the tool the rule names (a `Read()`
    deny does not cover a Bash read): a rehearsal item, not a page finding.

See also [[qa-directive-traps]] (snapshots, probe reasons, scratch names) and [[qa-ci-config-specs]] (workflow tasks).
