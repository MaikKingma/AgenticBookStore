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

## After (qa-final QF-8 table, copied verbatim from `.claude/comms/mailboxes/qa-final/outbox/task-final-verdict.md`)

Produced by one script (`qa-final-cip-analyse.py`) from saved run JSON and job logs; every cell names its run, attempt
and job. The baseline rows above (research prose, a different run set) are superseded for comparisons by the
same-script baseline rows below.

Job and step durations come from `startedAt`/`completedAt` at 1 s resolution. Maven Total time is the last `Total time:` line. The starter class is the `Starting <X> using Java` line before the first
`Creating container for image: postgres:18.6`, and its value is that class's Surefire `Time elapsed`. The runner image is the `Image:`/`Version:` pair in "Set up job".

**Runs per task PR push**
| state | runs | evidence |
|---|---|---|
| before | 2 | push 42478894392 plus pull_request 42478908987, both on sha b098e19, branch `sdd/05-distributor/T2`, both success |
| after, PR #47 final head decf800 | 1 | 42483154444 pull_request, success, attempt 1 (`gh run list --commit decf80074f31b724c6690143aefbdaada6e763fb`) |
| after, PR #48 final head 2c7e412 | 1 | 42486800066 pull_request, success, attempt 1 (`--commit 2c7e41201748a4e10488646d5ab85b22bcaabea7`) |
| after, PR #51 final head 1ffd907 | 1 | 42488669087 pull_request, success, attempt 1 (`--commit 1ffd9071f08b5f212ae1bac84c9de3ae680a3c70`) |
| spec tip d7e11a5 (accepted G7 cost: a spec-branch push with an open spec PR runs twice) | 2 | push 42488779335 plus pull_request 42488783421 |

**Verify job timings**
| row | run / attempt / job | event | runner image | per-module totals | verify job | build step | Maven Total time | starter class Time elapsed | report step |
|---|---|---|---|---|---|---|---|---|---|
| baseline 42474529317 | 42474529317 / 1 / 128712764531 | push | ubuntu-26.04 20261101.186.1 | bookstore 684 + distributor 3, Skipped 0 | 53 s | 47 s | 43.489 s | DatabaseCleanerIntegrationTest 18.11 s | no step |
| baseline 42478926587 | 42478926587 / 1 / 128726383100 | pull_request | ubuntu-26.04 20261101.186.1 | bookstore 667 + distributor 86, Skipped 0 | 52 s | 48 s | 45.35 s | DatabaseCleanerIntegrationTest 17.06 s | no step |
| baseline 42476894930 | 42476894930 / 1 / 128720144349 | pull_request | ubuntu-26.04 20261101.186.1 | bookstore 667 + distributor 37, Skipped 0 | 62 s | 55 s | 51.697 s | DatabaseCleanerIntegrationTest 20.35 s | no step |
| baseline 42476879143 | 42476879143 / 1 / 128720096995 | pull_request | ubuntu-26.04 20261101.186.1 | bookstore 667 + distributor 37, Skipped 0 | 58 s | 50 s | 47.239 s | DatabaseCleanerIntegrationTest 16.47 s | no step |
| baseline 42473951976 | 42473951976 / 1 / 128710973928 | pull_request | ubuntu-26.04 20261101.186.1 | bookstore 667 + distributor 3, Skipped 0 | 59 s | 50 s | 48.123 s | DatabaseCleanerIntegrationTest 16.64 s | no step |
| T2 green (same suite) | 42483525266 / 1 / 128740412633 | pull_request | ubuntu-26.04 20261101.186.1 | bookstore 684 + distributor 3, Skipped 0 | 59 s | 46 s | 44.562 s | DatabaseCleanerIntegrationTest 16.16 s | 0 s |
| T3 attempt 1 | 42487157571 / 1 / 128751485930 | pull_request | ubuntu-26.04 20261101.186.1 | bookstore 684 + distributor 3, Skipped 0 | 49 s | 42 s | 39.645 s | DatabaseCleanerIntegrationTest 10.58 s | 0 s |
| T3 attempt 2 | 42487157571 / 2 / 128751763383 | pull_request | ubuntu-26.04 20261101.186.1 | bookstore 684 + distributor 3, Skipped 0 | 53 s | 43 s | 40.492 s | DatabaseCleanerIntegrationTest 11.38 s | 0 s |
| T3 attempt 3 | 42487157571 / 3 / 128752066219 | pull_request | ubuntu-26.04 20261101.186.1 | bookstore 684 + distributor 3, Skipped 0 | 50 s | 39 s | 35.755 s | DatabaseCleanerIntegrationTest 9.609 s | 1 s |
| tip push | 42488779335 / 1 / 128756352608 | push | ubuntu-26.04 20261101.186.1 | bookstore 684 + distributor 3, Skipped 0 | 55 s | 48 s | 44.075 s | DatabaseCleanerIntegrationTest 10.95 s | 0 s |
| tip pull_request | 42488783421 / 1 / 128756365205 | pull_request | ubuntu-26.04 20261101.186.1 | bookstore 684 + distributor 3, Skipped 0 | 40 s | 32 s | 30.827 s | DatabaseCleanerIntegrationTest 10.65 s | 0 s |
| **Baseline (5 warm runs, mixed suites, Ryuk on, no pre-pull)** | n=5 | | ubuntu-26.04 20261101.186.1 (all) | | median 58 s (range 52 to 62) | median 50 s (range 47 to 55) | median 47.239 s (range 43.489 to 51.697) | median 17.06 s (range 16.47 to 20.35) | n/a |
| **T2 green, same suite (684+3, Ryuk on, no pre-pull)** | n=1 | | ubuntu-26.04 20261101.186.1 | | 59 s | 46 s | 44.562 s | 16.16 s | 0 s |
| **T3 head 4b0d516, attempts 1 to 3** | n=3 | | ubuntu-26.04 20261101.186.1 (all) | | median 50 s (range 49 to 53) | median 42 s (range 39 to 43) | median 39.645 s (range 35.755 to 40.492) | median 10.58 s (range 9.609 to 11.38) | median 0 s (range 0 to 1) |
| **Spec tip d7e11a5 (push + pull_request)** | n=2 | | ubuntu-26.04 20261101.186.1 (both) | | median 47.5 s (range 40 to 55) | median 40 s (range 32 to 48) | median 37.451 s (range 30.827 to 44.075) | median 10.8 s (range 10.65 to 10.95) | median 0 s (range 0 to 0) |
| **After pooled: T3 attempts + tip jobs** | n=5 | | ubuntu-26.04 20261101.186.1 (all) | | median 50 s (range 40 to 55) | median 42 s (range 32 to 48) | median 39.645 s (range 30.827 to 44.075) | median 10.65 s (range 9.609 to 11.38) | median 0 s (range 0 to 1) |

**Cold Maven cache share**
| run set | cold | evidence |
|---|---|---|
| this cycle: `gh run list --workflow CI --created '>=2026-11-08T12:40:00Z' --limit 200` (29 runs), filtered to headBranch containing `ci-performance` and conclusion not `cancelled`, every attempt, verify job only: 18 verify jobs | 1 of 18 | setup-java line `maven cache is not found` only in run 42481683660 / a1 / job 128734792165 (push, `sdd/ci-performance/spec`, aa167b3, the first push of the spec branch; it then logs `Cache saved with the key: setup-java-Linux-x64-maven-5fd9fa18...`). The other 17 show `Cache restored from key: setup-java-Linux-x64-maven-5fd9fa18...` (`cip-final-qa-cachescan.txt`) |
| baseline "28 of 44 push runs" | research figure, not recomputed | res-1 scanned "the latest 120 runs" at research time, a set that cannot be restated now. Its script also greps `Cache not found`, which is not the setup-java text (`maven cache is not found`). No conclusion is drawn from comparing the two |

**Gated statement (the only one):** the starter-class median after (pooled n=5: 10.65 s; tip alone n=2: 10.8 s) is
lower than the same-script baseline median (17.06 s). Every after value (max 11.38 s) is below every baseline value
(min 16.47 s), and below the same-suite T2 green value 16.16 s. Report step at most 1 s against the 10 s gate (T2 AC7).
**Not gated:** with n of 1 to 5 per row, no significance is claimed for job, build or Maven durations. The job-duration
drop against the mixed-suite baseline (58 s -> 50 s median) is not attributed wholly to this spec. Baseline 42474529317
(push on factory 05be36c, the PR base) and T2 green 42483525266 are the two same-suite (684 + 3) Ryuk-on rows (53 s and
59 s job).
