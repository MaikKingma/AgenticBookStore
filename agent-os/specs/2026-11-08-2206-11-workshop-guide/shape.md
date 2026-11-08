# Shape: spec 11, workshop guide site

Source of truth: `docs/spec-pack/specs/11-workshop-guide-site.md` as on factory 52c4f6b (including the D7 section
"Visual design" and the requirement "OpenValue brand"). Shaping restates the pack spec and regroups its tasks; it never
changes a requirement, scenario, path, name or version.

## Scope

- A Docusaurus 3.10.2 site in `guide/` (classic preset, docs-only, TypeScript config, no blog), with its own
  `package.json` and `package-lock.json`. `guide/` is not a Maven module; the referee `./mvnw clean verify` is
  unchanged. Participants never need Node.js; Node.js 26.13.0 is used only for working on `guide/` and in CI.
- Pages per the spec's information architecture: `index.md`, `00-setup-check.md`, five level pages as `.mdx`,
  `06-wrap-up.md`, `reference/cheat-sheet.md`, `reference/architecture.md`, `reference/troubleshooting.md`, and the
  static component `src/components/LevelProgress.tsx`.
- The OpenValue 2026 brand (D7) in `guide/src/css/custom.css` only, self-hosted Montserrat woff2 with its OFL licence,
  and a contrast script that the CI build runs.
- `.github/workflows/guide.yml`: PRs touching `guide/**` build (and upload the offline zip as a workflow artifact);
  pushes to `factory` build, deploy to GitHub Pages and attach the zip to release `workshop-2026`, behind the owner's
  deploy gate (below).

## Not in this cycle

- Spec 11 task 6 (run every command in the guide on a clean machine against `main`): `main` gets the app only through
  spec 09 and the level branches only through spec 10. It is follow-up F1.
- "Site live" and "offline zip available" of the definition of done: they wait for Maik's public deploy gate (F12).
- Everything the follow-up list F1 to F13 in `strategy.md` names (content that exists only after specs 09 and 10).

## Decisions (orchestrator, self-accepted; full log in the run ledger and the spec PR body)

1. Two configs, two builds (research/research-docusaurus-offline-config.md): `docusaurus.config.ts` for Pages
   (browser router, `url: https://maikkingma.github.io`, `baseUrl: /AgenticBookStore/`, `onBrokenLinks`,
   `onBrokenAnchors` and `markdown.hooks.onBrokenMarkdownLinks` all `throw`); `docusaurus.offline.config.ts`
   (the same plus `baseUrl: '/'` and `future.experimental_router: 'hash'`). `npm run build` is the gate, because the
   hash build skips static rendering and checks no links. The offline zip is built after the gate, `index.html` at
   the zip root. Verified on 3.10.2 and Node 26.13.0 from `file://` in Chromium, Firefox and WebKit.
2. Admonitions use the Docusaurus 3 title syntax `:::info[Sync moment]` and `:::tip[Checkpoint]`. With the official
   template's `future.v4: true` the spec's space form renders as plain text; the rendered result (an info admonition
   titled "Sync moment", a tip titled "Checkpoint") is what the page conventions ask for.
3. No search plugin (allowed by the spec: no first-party local search exists, the hosted one needs an API key, and
   community plugins would be a new dependency). No logo.
4. Public deploy gate (Maik, mandatory): the deploy job, the release job and the Pages artifact upload run only when
   `github.event_name == 'push' && vars.GUIDE_DEPLOY == 'true'`. The repository variable `GUIDE_DEPLOY=true` is Maik's
   approval. Until he sets it, a factory push builds and the deploy and release jobs are skipped (grey, not red). No
   agent enables Pages, runs a deploy, creates a release or a `gh-pages` branch. Removing the one `if:` restores the
   unconditional behaviour of the requirement.
5. Brand per D7: light primary teal `#014046`, navy text, white background, snow or mist surfaces, blue only as a
   non-text or large-text accent, navy navbar with white text; dark mode navy background, mist primary, white text,
   or `colorMode.disableSwitch` if any pair fails AA. The current level in `LevelProgress` is a teal (light) or mist
   (dark) fill with a contrasting label; blue is a non-text accent only, because every text pair must reach 4.5:1.
   Fonts live in `guide/src/css/fonts/` (bundled into the build); the OFL licence ships as
   `guide/static/fonts/OFL.txt`. Font files are downloaded from the upstream Montserrat release or Google Fonts with
   URL and sha256 recorded, never through an npm package.
6. Header box per level page: Goal, Start branch, Catch-up (`git stash -u && git switch <branch>`, the branch to use
   when behind at the start of the level: L0 and L1 `main`, L2 `solution/level-1`, L3 `solution/level-2`, L4
   `start/level-4-<track>`; roadmap catch-up rule), Time, and a Compare line `solution/level-N` for L1 to L3, because
   the plan names those branches "catch-up / compare".
7. Windows: PowerShell tabs only where the pack states the Windows command (plan line 344: Claude Code native, bash
   hooks through Git Bash or WSL2, `.ps1` hook variants on `solution/level-3`, jdtls through WSL). Missing variants are
   follow-ups (F2, F9), never invented. The bash hook scripts may be quoted from spec 10 lines 273-314.
8. Solution branches are linked, their file texts are not printed (except what the plan itself shows). The setup page
   is built from the plan (lines 45-47, 59-66) and spec 09 lines 38-39, because the pack contains no participant
   email (F13).
9. The guide's checker (`npm run check`) reads `docs/spec-pack/workshop/workshop-plan.md` at check time, so the
   "Level cleared when" lists, Goals and minutes cannot drift. Safe because `guide/` lives on `factory`, where the
   pack exists; the path filter of `guide.yml` stays as the spec has it (a pack-only push does not redeploy).
10. Implementation order T1 to T11, one task at a time (strategy.md, approach A: checker first, skeleton pages, then
    content). All tasks are module `guide` except T2 (`root`, the workflow).

## Requirement to task map

| Requirement / scenario | Tasks | Mechanical evidence |
|---|---|---|
| Complete level coverage / Content check | T5 manifest, T6 to T10 content, T11 | `npm run check` coverage N/N, res-3 section 9 table ticked by verifiers |
| Level pages / Level page structure | T4 skeletons and checks, T7 to T10 | checker structure rules against the plan, rendered H1 and indicator |
| Works offline / Offline bundle | T1 configs, T2 zip, T3 fonts, T11 site-wide pass | `file://` checks in three engines on the CI zip |
| Readable next to a terminal / Narrow viewport | T1, T3, T11 | Playwright at 720 px: no page scroll, code blocks scroll |
| OpenValue brand / Brand in one place, Readable contrast | T3, T4 component | checker colour rule and contrast script in CI |
| Deployed automatically / Broken link | T1 config, T2 workflow | build fails on a missing page link; PR runs build, deploy skipped |

## Follow-ups (after spec-10-done, before the spec PR merges)

F1 to F13 as listed at the end of `strategy.md`: clean-machine review (spec 11 task 6), PowerShell hook scripts,
`docs/lsp-setup.md`, the plugin-declaration sentence, links on `main`, `notes/` in main's `.gitignore`, solution branch
links, the `start/level-4-*` paths and the OpenSpec CLI, Windows variants the pack does not state, a real Safari check,
the "under a minute" claim after spec 12, the owner's deploy gate, the missing participant email.
