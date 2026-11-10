---
agent: res-2
task: res-2
status: completed
updated: 2026-11-08T20:08:05Z
---

## TL;DR

Use a three-job `guide.yml` (`build`, `deploy`, `release`) with SHA pins below. The public gate is a repository variable: `if: github.event_name == 'push' && vars.GUIDE_DEPLOY == 'true'` on `deploy` and `release` and on the Pages-artifact upload step. The owner sets `GUIDE_DEPLOY=true` as his approval. Until then a push to factory only builds and the two jobs show as skipped (green). The full YAML below lints clean with actionlint 1.7.12 run from a pinned docker image. No required checks exist, so path filters cannot block a merge. `configure-pages` is not needed and must not be used.

## Key findings

### 1. Pins (verified with `gh api repos/<r>/releases/latest` and `git/ref/tags/<tag>`; every tag was a lightweight tag, object type `commit`, so no dereference was needed)

| Action | Tag | Commit SHA |
|---|---|---|
| actions/checkout (same as ci.yml:30) | v7.0.1 | 3d3c42e5aac5ba805825da76410c181273ba90b1 |
| actions/setup-node | v7.1.0 | 949feb2413d6458794dcd2491c4babbbce0c15c1 |
| actions/upload-pages-artifact | v5.0.0 | fc324d3547104276b827a68afc52ff2a11cc49c9 |
| actions/deploy-pages | v5.0.1 | 368f82528645a54fb793d4d04e342629a3f51346 |
| actions/upload-artifact (same as ci.yml:52) | v7.0.2 | cf430e030ddbb5b0abf93d22962f4752f3646cd9 |
| actions/download-artifact (new, release job) | v8.0.2 | 9000827ccba6bdab643e8b6fd33ac0654aef8333 |
| actions/configure-pages (NOT used) | v6.0.0 | 45bfe0192ca1faeb007ade9deae92b16b8254a0d |

- setup-node v7.1.0 and deploy-pages v5.0.1 run on `node24` (their action.yml). setup-node supports `cache: npm` and `cache-dependency-path` (action.yml at that SHA).
- upload-pages-artifact v5.0.0 inputs: `name` (default `github-pages`), `path`, `retention-days` (default 1), `include-hidden-files` (default false: dotfiles are excluded from the tar). It uses upload-artifact v7.0.0 internally. A Docusaurus build needs no dotfiles for an Actions deployment.
- configure-pages is not needed: it only extracts Pages metadata for static-site generators and can enable Pages (which we must never do). Docusaurus takes `url` and `baseUrl` from its own config. Do not add it.
- Release upload uses the preinstalled `gh` CLI: `gh release view workshop-2026 || gh release create workshop-2026 --target "$GITHUB_SHA" ...`, then `gh release upload workshop-2026 <zip> --clobber`. Needs `permissions: contents: write` and `GH_TOKEN: ${{ github.token }}` (the `GITHUB_TOKEN`); `GH_REPO` removes the need for a checkout. `--target` matters: without it a new tag points at the default branch (main) HEAD. (`gh` behaviour is from my knowledge, not run; I did not create a release.)
- The release job needs the zip from the build job, hence `download-artifact` (extra pin). Alternative: do the release in the `build` job, which would give a PR-reachable job `contents: write`; rejected for least privilege.

### 3. Public deploy gate

- State of the repo now (read-only checks): Pages is not enabled (`gh api repos/MaikKingma/AgenticBookStore/pages` 404), no environments, no releases, no repository variables, repo is public, default branch `main`.
- Unguarded push to factory with Pages disabled: `deploy-pages` reads the artifact, calls `POST /repos/{owner}/{repo}/pages/deployments`, gets 404 and throws `Failed to create deployment (status: 404) ... Ensure GitHub Pages has been enabled: <repo>/settings/pages`. The step and the workflow run go red (verified in `src/internal/deployment.js` and `api-client.js` at the v5.0.1 SHA). It deploys nothing, but it is a red run on factory.
- Option (a) variable guard: fails closed (an unset variable is an empty string, so the condition is false), no settings change by us, the owner approves by creating one variable, a skipped job is grey not red, and the same variable gates the release. Pages must still be enabled by the owner (Settings, Pages, source GitHub Actions) before the first real deploy.
- Option (b) environment protection: needs settings, and the `github-pages` environment does not exist yet. Not available to us. Medium confidence (docs, not verified here): when the owner enables Pages with source GitHub Actions the default environment may restrict deployments to the default branch `main`, so deploying from `factory` would be rejected until he adds `factory` to the environment's branch policy. The spec says nothing deploys from main, so the owner should expect this one-time settings step. (I did not confirm that a skipped job never creates the environment; unverified.)
- Option (c) `workflow_dispatch` only: strongest gate but contradicts the requirement "pushes to factory build and deploy automatically". Can be combined with (a) later by the owner if he wants a manual step.
- Recommendation: (a). Use one variable for both public effects, or two (`GUIDE_DEPLOY_PAGES`, `GUIDE_DEPLOY_RELEASE`) if the owner wants them separate; one is simpler. The workflow header comment must name the variable.
- Caveat: anyone with write access can set the variable; fine, the owner is the approver. The guard lives in the workflow on factory, so only a merged PR can change it.

### 4. Triggers

- `pull_request: paths: [guide/**, .github/workflows/guide.yml]` and `push: branches: [factory], paths: [same]`. ci.yml keeps its unfiltered `pull_request`, so `verify` and `windows-build` still run on guide PRs (fine, a few minutes of extra CI; Windows job needs nothing from guide/).
- No blocking: `gh api repos/MaikKingma/AgenticBookStore/branches/factory/protection` returns 404 "Branch not protected" and `gh api .../rulesets` returns `[]`. With no required checks a path-filtered workflow that does not run cannot block a merge. (If the owner ever requires `build`, a filtered-out run would stay "pending"; not applicable.)
- Spec PRs from `sdd/11-workshop-guide/spec` into factory run the PR build, so QA sees the build go green before merge. guide.yml deliberately does not add `sdd/*/spec` to its push filter.
- "Nothing deploys from main": the push trigger names only `factory`; a PR event never satisfies the deploy `if`.

### 5. actionlint

- `which actionlint go shellcheck`: none installed. Docker 29.8.2 is available. Runnable without a global install: `docker run --rm -v "<dir>:/repo" -w /repo rhysd/actionlint:1.7.12@sha256:b1934ee5f1c509618f2508e6eb47ee0d3520686341fec936f3b79331f9315667 .github/workflows/guide.yml` (digest is the multi-arch index digest of tag 1.7.12, latest release v1.7.12, verified with `docker buildx imagetools inspect`). Pass the file explicitly (without it actionlint errors "no project was found" outside a git repo); the image bundles shellcheck. For an absolute path to the real repo, mount `.github/workflows` read-only and pass `/wf/guide.yml`.
- The draft below: exit 0, no findings. Existing ci.yml: one pre-existing info `SC2016` at line 28 (the nohup line, single quotes around `$(date ...)` is intentional). QA should expect that on ci.yml and not on guide.yml.

### 6. ci-performance follow-up folded in (`agent-os/specs/2026-11-08-1217-ci-performance/shape.md`, "Follow-ups", spec 11 bullet; standards.md has no CI-workflow standard proposal, only the GitHub SHA-pinning guidance)

All applied: `setup-node` with `cache: npm` and `cache-dependency-path: guide/package-lock.json`; same concurrency group pattern; SHA pinning; deploy job group `pages` with no cancel; push filter matches this workflow. Related: the shape doc lists Dependabot for pin upkeep as an owner decision (not done here).

## Proposed `.github/workflows/guide.yml` (linted, ASCII only; the file is at the scratchpad path below)

Scratchpad copy: `/private/tmp/claude-501/-Users-maikkingma-OpenValue-AI-Conferences-AgenticBookStore/cb0898b6-6f7b-44fd-8024-5110d0f772e2/scratchpad/res-2/.github/workflows/guide.yml`

```yaml
name: Guide

# Builds the workshop guide (Docusaurus, folder guide/).
# Pull requests only build. Pushes to factory build and, once the owner sets the repository variable
# GUIDE_DEPLOY to the string true, deploy to GitHub Pages and attach the offline zip to the release workshop-2026.
# Without that variable nothing is published and the deploy and release jobs are skipped.
on:
  push:
    branches:
      - factory
    paths:
      - 'guide/**'
      - '.github/workflows/guide.yml'
  pull_request:
    paths:
      - 'guide/**'
      - '.github/workflows/guide.yml'

concurrency:
  group: ${{ github.workflow }}-${{ github.event.pull_request.number || github.sha }}
  cancel-in-progress: ${{ github.event_name == 'pull_request' }}

permissions:
  contents: read

jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 10
    defaults:
      run:
        working-directory: guide
    steps:
      - uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1
      - uses: actions/setup-node@949feb2413d6458794dcd2491c4babbbce0c15c1 # v7.1.0
        with:
          node-version: '26.13.0'
          cache: npm
          cache-dependency-path: guide/package-lock.json
      - name: Install dependencies
        timeout-minutes: 3
        run: npm ci
      - name: Build site
        # Must stay below the job timeout-minutes (10). Broken links fail this step.
        timeout-minutes: 5
        run: npm run build
      - name: Build offline site
        timeout-minutes: 5
        run: npm run build:offline
      - name: Zip offline site
        run: zip -r -q ../workshop-guide-offline.zip build-offline
      - name: Upload offline zip
        uses: actions/upload-artifact@cf430e030ddbb5b0abf93d22962f4752f3646cd9 # v7.0.2
        with:
          name: workshop-guide-offline
          path: workshop-guide-offline.zip
          retention-days: 14
          if-no-files-found: error
      - name: Upload Pages artifact
        if: ${{ github.event_name == 'push' && vars.GUIDE_DEPLOY == 'true' }}
        uses: actions/upload-pages-artifact@fc324d3547104276b827a68afc52ff2a11cc49c9 # v5.0.0
        with:
          path: guide/build

  deploy:
    needs: build
    if: ${{ github.event_name == 'push' && vars.GUIDE_DEPLOY == 'true' }}
    runs-on: ubuntu-latest
    timeout-minutes: 15
    permissions:
      pages: write
      id-token: write
    concurrency:
      group: pages
      cancel-in-progress: false
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    steps:
      - name: Deploy to GitHub Pages
        id: deployment
        # deploy-pages gives up after 10 minutes by default, below the job timeout.
        timeout-minutes: 12
        uses: actions/deploy-pages@368f82528645a54fb793d4d04e342629a3f51346 # v5.0.1

  release:
    needs: build
    if: ${{ github.event_name == 'push' && vars.GUIDE_DEPLOY == 'true' }}
    runs-on: ubuntu-latest
    timeout-minutes: 5
    permissions:
      contents: write
    env:
      GH_TOKEN: ${{ github.token }}
      GH_REPO: ${{ github.repository }}
    steps:
      - name: Download offline zip
        uses: actions/download-artifact@9000827ccba6bdab643e8b6fd33ac0654aef8333 # v8.0.2
        with:
          name: workshop-guide-offline
      - name: Create the release if it is missing
        run: |
          if ! gh release view workshop-2026 > /dev/null 2>&1; then
            gh release create workshop-2026 --target "$GITHUB_SHA" --title "Workshop 2026" --notes "Offline copy of the workshop guide."
          fi
      - name: Attach the offline zip
        timeout-minutes: 3
        run: gh release upload workshop-2026 workshop-guide-offline.zip --clobber
```

## Implications for the decision

- Assumptions baked into the draft that the Docusaurus researcher or the spec must confirm: `guide/package.json` has scripts `build` (writes `guide/build/`) and `build:offline` (writes `guide/build-offline/`); the offline build uses relative or a different `baseUrl` than the Pages build; package-lock.json is committed. Rename in the YAML if the names differ. `setup-node` fails its cache step if `guide/package-lock.json` is missing.
- Acceptance criteria candidates: (1) actionlint 1.7.12 (pinned digest) exits 0 on guide.yml; (2) grep shows every `uses:` line is a 40-hex SHA with a `# vX.Y.Z` comment; (3) every job has `timeout-minutes` and each long step has a smaller one; (4) top-level `permissions: contents: read`, `pages: write` and `id-token: write` only on `deploy`, `contents: write` only on `release`; (5) `deploy` and `release` carry the `vars.GUIDE_DEPLOY == 'true'` and `push` condition; (6) no `workflow_dispatch`, no `configure-pages`, no secrets; (7) ASCII only; (8) the spec PR's own run shows `build` green and `deploy`/`release` not present as run (PR event) and no Pages or release created.
- Local QA cannot execute the workflow (and we must not dispatch it); lint plus the PR run of `build` is the evidence. The deploy and release jobs cannot be proven by us before the owner opens the gate: say so in the spec.
- Owner hand-over steps to put in the spec or handoff doc: enable Pages with source GitHub Actions; allow `factory` in the `github-pages` environment branch policy if it is restricted to main; set repository variable `GUIDE_DEPLOY=true`.
- Trade-off of single `build` job with Pages artifact conditional: PR runs build the offline zip and site but never upload a Pages artifact.
- Pages artifact limit: 1 GB (deploy-pages source), not a concern for a docs site.

## Sources

- Verified: `.github/workflows/ci.yml`; `agent-os/specs/2026-11-08-1217-ci-performance/shape.md` and `standards.md`; `gh api` releases and tag refs for the six actions plus download-artifact; action.yml of upload-pages-artifact, deploy-pages, setup-node at the pinned SHAs; deploy-pages `src/internal/deployment.js` and `api-client.js` at the pinned SHA; read-only `gh api` calls for protection, rulesets, pages, environments, releases, variables, repo metadata; actionlint run via docker on the draft and on ci.yml.
- Unverified (knowledge, medium confidence): default `github-pages` environment branch restriction; `gh release create/upload` flag behaviour; `zip` present on ubuntu-latest; whether a skipped job creates the environment.
- Not fetched: the GitHub Pages custom-workflow docs and the Docusaurus deployment page (the pinned action sources were used instead).

## Open questions / gaps

- Names of the npm scripts and output folders (Docusaurus researcher topic).
- One variable or two for the gate (owner preference).
- Whether the owner wants the release created as draft (add `--draft` to `gh release create`; then upload works the same and nothing is public until he publishes). Worth offering as an additional guard.
- ci.yml has a pre-existing actionlint SC2016 info; out of scope.
- Comms: the mailbox directory for res-2 did not exist, created by this run.
