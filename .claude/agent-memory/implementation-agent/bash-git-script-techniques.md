---
name: bash-git-script-techniques
description: Techniques and traps for bash scripts that move git refs (publish script) and their hermetic bash tests, bash 3.2 and bash 5
metadata:
  type: reference
---

See also the senior researcher's note `.claude/agent-memory/senior-research-agent/shell-script-git-research-techniques.md`
(three-shell matrix, hermetic env vars, forced identity failure).

- **Worktree guard refuses `bash -c '...'`** in Bash calls (it cannot prove no git runs). Put waits, loops and
  container runs in `.claude/comms/tmp/<id>-*.sh` and run `bash <file>`.
- **Date-varied idempotency:** set `GIT_AUTHOR_DATE`/`GIT_COMMITTER_DATE` per script run, an hour apart. Pinned
  dates make an "always commit-tree" mutant produce the same id and survive.
- **`--force-with-lease=<ref>:<sha>` works when the sha is not in the local object store** (remote main from an
  unrelated root); read remote values with `git ls-remote` so no local ref moves (dry-run safe, no fetch).
- **Atomic witness:** an `update` hook in the bare remote that rejects the tag; **lease witness:** a PATH shim `git`
  that moves the remote main when it sees `push`, and logs the push argv for exact flag assertions.
- **Mutants on copies:** `perl -0pe` (multi-line, same on BSD and GNU) into a scratch copy, run the test with its own
  TMPDIR. A mutant can abort the test in setup (`set -e`); an EXIT trap that prints "aborted" keeps that legible.
- **shellcheck SC2016** fires on `printf '%s' '[ "$1" = x ]'`; write hook files with a quoted heredoc instead.
- **actionlint on `ci.yml`** reports a pre-existing SC2016 info on the pre-pull step (line 28); compare against the
  base file before claiming a new finding.
- Linux bash 5 run without new images: `postgres:18.6` (Debian) plus `apt-get install -y git` in the container.
