# Measurements: ci-performance

All durations in seconds, from `gh run view <id> --attempt <a> --json jobs` (job and step startedAt/completedAt,
1 s resolution). Runner image ubuntu-26.04 (20261101.186.1), Docker 29.4.2 with the containerd snapshotter.

## Baseline (factory 05be36c and earlier, before this spec)

From 88 successful runs of 2026-11-07 and 2026-11-08 (research by res-1):

| metric | median | range |
|---|---|---|
| verify job, warm Maven cache (41 pull_request runs) | 52 | 41 to 62 |
| verify job, cold Maven cache (push runs) | 62 | 52 to 71 |
| verify build step, warm | 45 to 47 | |
| windows-build job, warm / cold | 41 / 53 | 31 to 76 / 37 to 64 |
| `postgres:18.6` pull by Testcontainers | about 4 | 3.2 to 4.9 |
| `testcontainers/ryuk:0.14.0` pull | about 1 | 0.4 to 2.3 |
| push runs with a cold Maven cache | 28 of 44 | |
| workflow runs per task PR (pushes, PR runs, spec-branch push) | about 5 | 2 pure duplicates |

Repository caches at baseline: 188 entries, about 10.4 GB across 47 refs (GitHub evicts above 10 GB).

## Image experiment (run 42480752749, throwaway branch, never merged)

Jobs in one run, each on a fresh VM: `img-pull` (plain pulls), `img-cache` (actions/cache tarball plus `docker load`),
`img-bg` (background pre-pull plus `TESTCONTAINERS_RYUK_DISABLED=true`, full verify), next to the unchanged `verify`.

| attempt | pull postgres + ryuk | cache restore + load | cache miss extra (save tar + upload) | verify build step | img-bg build step |
|---|---|---|---|---|---|
| 1 (cold) | 4 + 1 | miss (1) | 5 + 3 | 45 | 42 |
| 2 | 3 + 1 | 7 + 5 | | 40 | 32 |
| 3 | 3 + 1 | 2 + 4 | | 52 | 34 |
| 4 | 4 + 1 | 2 + 3 | | 43 | 36 |
| 5 | 4 + 1 | 2 + 6 | | 45 | 40 |
| median | 5 | 7 (warm) | +9 on a cold run | 45 | 36 |

- A warm image cache is about 2 s slower than a plain pull, and a cold run pays about 9 s more. No image cache.
- Background pre-pull plus Ryuk off: build step median 36 against 45 in the same runs (paired saving median 7 s);
  job total median 44 against 53. The `img-bg` Maven log has no Testcontainers pull and no Ryuk line.
- Docker Hub rate-limit probe (manifest HEAD, consumes no pull): `ratelimit-limit: 100;w=3600`,
  `ratelimit-remaining: 100;w=3600` on every attempt; each runner has its own address.
- Pinned image digest observed: `postgres@sha256:e3d24f6906d0760bf4da0dae872415391ededefad23fd471c47dc53b0ddf3c42`.

## After (filled per task from the task PR runs)

See plan.md task rows and the spec PR body; the final before/after table is added here at the end of the cycle.
