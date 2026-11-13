---
name: gh-actions-measurement-technique
description: How to measure CI baselines with gh (step times, log greps, cache scoping) and the traps hit
applies-to:
  - .github/workflows/*.yml
metadata:
  type: reference
---

- `gh run view <id> --json jobs` gives step startedAt/completedAt; job objects have no createdAt (use run createdAt for queue time).
- `gh run view <run> --job <jobId> --log > file` from a script, then grep the file; the output is 300+ KB (Hibernate SQL), never print it.
- Testcontainers logs "Image X pull took PTnS"; setup-java logs "Cache hit for", "maven cache is not found", "Cache saved", "Unable to reserve cache".
- `gh cache list --json key,ref,sizeInBytes` shows cache scoping: identical keys per branch mean branch-scoped duplicates and no default-branch seed.
- Workflow lint without installs: `docker run --rm -v "<dir>:/repo" -w /repo rhysd/actionlint:<tag>@sha256:<index digest> .github/workflows/x.yml` (pass the file; no .git means "no project found"). Get the digest with `docker buildx imagetools inspect`. Read-only checks: `gh api .../branches/<b>/protection`, `.../rulesets`, `.../pages`, `.../actions/variables`. Action source at a pinned SHA: `gh api "repos/o/r/contents/path?ref=<sha>" -H "Accept: application/vnd.github.raw"` (quote the URL; plain `--jq .content` returns base64).
- Bash guard: scripts in `.claude/comms/tmp/`, one simple command per call; a quoted absolute path with spaces works, backslash-escaped spaces are refused.
