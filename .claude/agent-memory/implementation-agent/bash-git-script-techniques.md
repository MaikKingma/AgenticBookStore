---
name: bash-git-script-techniques
description: Techniques and traps for bash scripts that move git refs (publish script) and their hermetic bash tests, bash 3.2 and bash 5
metadata:
  type: reference
---
See also the senior researcher's note `.claude/agent-memory/senior-research-agent/shell-script-git-research-techniques.md`
(three-shell matrix, hermetic env vars, forced identity failure).

- **Run the script under test with `"$BASH" "$SCRIPT"`, never `bash "$SCRIPT"`:** this Mac has Homebrew bash 5 first on PATH,
  so `/bin/bash test.sh` otherwise tests on bash 5 and a "green on 3.2" claim is false. Prove it with a PATH shim `bash` that
  logs invocations (expect 0) and print `$BASH_VERSION` in the summary. Bash-4 mutants to check the guard: `${x,,}` and an
  empty `"${a[@]}"` under `set -u`. A Linux bash 5 run needs no new image: `postgres:18.6` plus `apt-get install -y git`.
- **Pin git flags that override config:** a flag like `status --untracked-files=normal` needs a test that sets the config it
  overrides (`status.showUntrackedFiles no`), or dropping the flag survives.
- **Worktree guard refuses `bash -c '...'`:** put waits, loops and container runs in `.claude/comms/tmp/<id>-*.sh`.
- **Date-varied idempotency:** set `GIT_AUTHOR_DATE`/`GIT_COMMITTER_DATE` per script run, an hour apart. Pinned dates make an
  "always commit-tree" mutant produce the same id and survive.
- **`--force-with-lease=<ref>:<sha>` works when the sha is not in the local object store**; read remote values with
  `git ls-remote` so no local ref moves (dry-run safe, no fetch).
- **Atomic witness:** an `update` hook in the bare remote that rejects the tag; **lease witness:** a PATH shim `git` that moves
  the remote main when it sees `push`, and logs the push argv for exact flag assertions.
- **Precedence between two valid candidates needs a case where both exist and differ** (remote vs local baseline: a local
  `commit-tree` of the same tree with another date), in BOTH date orders (a "the older/newer one wins" mutant passes whichever
  order you picked), with a sanity check that the dates really are in that order, compared against the fixed expected id, not a
  live ref the mutant may have moved.
- **Mutants on copies:** `perl -0pe` (multi-line, same on BSD and GNU) into a scratch copy, run the test with its own TMPDIR. A
  mutant can abort the test in setup (`set -e`); an EXIT trap printing "aborted" keeps that legible. A negative control must not
  abort either: re-adding a file with identical content leaves nothing to commit, so give it different content.
- **Look-alikes for a prefix mutant (`"$p*"` pathspec):** names extending the whole listed path (`guide.yml.disabled`);
  `guide.yaml` is no prefix extension and survives. **shellcheck SC2016** fires on `printf '%s' '[ "$1" = x ]'`: write hook files
  with a quoted heredoc; `ci.yml` already has an SC2016 info: compare against the base before claiming a new finding.
