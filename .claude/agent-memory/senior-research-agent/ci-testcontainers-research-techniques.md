---
name: ci-testcontainers-research-techniques
description: Techniques and traps for researching CI Docker image cost, Ryuk and registry questions for the Testcontainers suite on GitHub-hosted runners
applies-to:
  - .github/workflows/*.yml
  - bookstore/src/test/**
metadata:
  type: reference
---

Techniques that worked:
- Real image cost per run: `gh run view <id> --repo <owner/repo> --log`, then grep for the Testcontainers lines
  `pull took`, `Pull complete`, `started in PT`. They give exact pull and start seconds without any experiment. Put the
  loop in a script (shell guard rules).
- Image identity check without pulling: get an anonymous token from auth.docker.io, then `curl -sI` manifest HEADs on
  registry-1.docker.io and mirror.gcr.io and compare `docker-content-digest`. A HEAD does not consume a pull, and the
  Hub response carries the `ratelimit-*` headers.
- Library source at the pinned tag: `gh api -H "Accept: application/vnd.github.raw+json" repos/<o>/<r>/contents/<path>?ref=<tag>`.
- Plugin thread safety for `-T`: unzip `META-INF/maven/plugin.xml` from the jars in the local ~/.m2 and read `threadSafe`.

Traps:
- In Testcontainers 2.0.5, `TESTCONTAINERS_RYUK_DISABLED` is read from the environment only (ResourceReaper), never
  from testcontainers.properties.
- GitHub cache scoping: a run restores only from its own branch, the default branch (`main`) or a PR base branch. With
  one branch per task, the first push run on each branch is always cold. Weigh this in any cache cost/benefit.
- The Docker Hub docs and the live headers disagreed on the anonymous window (6 h vs `w=3600`). Quote both, and rely on
  the GitHub limits page for the hosted-runner exemption.
- mirror.gcr.io referenced directly (image-name prefix) has no fallback to Docker Hub. Only daemon `registry-mirrors`
  falls back.
