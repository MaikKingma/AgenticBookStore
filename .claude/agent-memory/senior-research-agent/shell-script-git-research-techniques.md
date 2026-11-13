---
name: shell-script-git-research-techniques
description: How to verify bash scripts that manipulate git refs portably (macOS bash 3.2 vs Linux GNU) without touching the real remote
applies-to:
  - scripts/**
  - "*.sh"
metadata:
  type: reference
---

Techniques that worked for researching git-ref scripts (publish/strip/tag) in this repo:

- **Three-shell matrix without CI:** `/bin/bash` (3.2, BSD userland), `/opt/homebrew/bin/bash` (5.x), and GNU/Linux
  via the already-pulled `postgres:18.6` image (Debian): `docker run --rm --entrypoint bash -v <scratch>:/proto:ro
  postgres:18.6 /proto/in-container.sh` with `apt-get install -y git` inside. Wrap in `with-budget.sh`.
- **Lint without installs:** `rhysd/actionlint` image; pass workflow file paths explicitly (it needs a git repo
  otherwise), flag is `-no-color`. Same image has `shellcheck` (`--entrypoint shellcheck`).
- **Hermetic git in tests:** `GIT_CONFIG_GLOBAL=/dev/null GIT_CONFIG_NOSYSTEM=1`, identity via `GIT_AUTHOR_*` /
  `GIT_COMMITTER_*`, `GIT_CEILING_DIRECTORIES=<tmp root>`, local bare repo as `origin`. Force a mid-run failure
  deterministically with `GIT_CONFIG_COUNT=1 GIT_CONFIG_KEY_0=user.useConfigOnly GIT_CONFIG_VALUE_0=true` and no identity.
- **Traps confirmed:** bash 3.2 `"${arr[@]}"` on an empty array under `set -u` is an unbound-variable error;
  `git ls-tree -- <path>` is cwd-relative (from a subdir it silently matches nothing); `git commit-tree` ignores
  `commit.gpgSign`; `--atomic` + `--force-with-lease=<ref>:<sha>` rejects both refs on one stale lease; an empty
  lease value means "ref must not exist". The repo path contains a space: fixtures should use spaced paths.
- Remote facts (protection, rulesets, default branch) are read-only via `gh api repos/<o>/<r>/branches/<b>` and
  `.../rulesets`; never push to check them.
