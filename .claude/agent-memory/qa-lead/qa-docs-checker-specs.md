---
name: qa-docs-checker-specs
description: Strategy traps for docs-site specs whose oracle is a home-made checker (Docusaurus guide, spec 11): checker mutation probes, coverage-manifest gaming, plan-parser witnesses, hex false positives, font-loading evidence, generated folders before their ignore rule; read before the strategy of any docs or checker task
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
2. **Freeze the oracle.** Content tasks must not touch the checker: hash every checker file against the last gated
   baseline in each content task (a hash chain), not only a fileScope rule.
3. **Coverage-manifest gaming.** Name these witnesses: snippet only inside `{/* */}` or `<!-- -->` must not count; a
   command only in prose inline code must not count when `kind: command`; a snippet that exists in the source but in
   another section must fail; a pack line plus an appended command must fail (whole-line, not `includes`). Guard
   silent exclusion with an independent literal sweep (extract every code span and fenced line of the source ranges,
   diff against manifest plus manual rows). List snippets shorter than 8 chars: they match anything.
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
11. **Self-hosted static sites in Docusaurus**: the hash-router build skips SSG and link checks (exit 0 on broken
    links and crashing pages), so only the browser build is evidence; a missing `.md` link failing the hash build too
    proves `markdown.hooks.onBrokenMarkdownLinks: 'throw'` survived a config spread. Pointer: res-1 report copied
    into the spec 11 folder.
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

See also [[qa-directive-traps]] (snapshots, probe reasons, scratch names) and [[qa-ci-config-specs]] (workflow tasks).
