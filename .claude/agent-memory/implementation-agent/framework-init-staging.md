---
name: framework-init-staging
description: Staging a generator-made framework (Spec Kit 1.3.1 via uvx) under HR1 isolation, its non-determinism, and evidence tricks for a self-consistent record
metadata:
  type: reference
---

- **Isolated run**: `env -i HOME=<h> PATH=<python dir>:/opt/homebrew/bin:/usr/bin:/bin XDG_{CONFIG,CACHE,DATA,STATE}_HOME UV_CACHE_DIR
  UV_TOOL_DIR` (all in the scratchpad) and `uvx --from specify-cli==1.3.1 specify init ... < /dev/null`. `env -i` keeps tokens out.
  Python 3.11+ must be on that PATH, else uv downloads a managed Python. The init needs the network only for the PyPI download.
- **`uv tool install` writes** (proved in a second isolated HOME with only HOME and XDG set): tool env `$XDG_DATA_HOME/uv/tools/<name>`,
  executable `$XDG_DATA_HOME/../bin`, cache `$XDG_CACHE_HOME/uv`. `uv tool uninstall` keeps the cache; `uv cache clean <pkg>` removes
  only that package's entries.
- **Spec Kit 1.3.1 init is not byte-deterministic**: `installed_at`/`updated_at` in `.specify/integrations/*.manifest.json` and
  `.specify/workflows/workflow-registry.json` (which also lacks a final newline). `constitution.md` equals the template after init.
- **`/speckit-constitution` amends an existing constitution**, so a row comparing a fresh run with a staged one first restores the
  template (`git show <start>:.specify/templates/constitution-template.md`).
- **Hand-written constitution check**: python3 -I with a trace TSV (fragment, source file, line, source fragment) plus mutants (placeholder
  back, key term removed, forbidden word allowed, untraced rule, shifted trace line), red first on the template.
- **Evidence must not match its own greps**: build leak and branch-name patterns from split strings in the script, describe them in prose.
  ASCII check that ignores tabs: `LC_ALL=C grep -E '[^[:print:][:space:]]'`; tool box art through a small asciify script, emoji in
  `git grep` output through `sed 's/[^ -~]/?/g'`.
- **Row dry runs** need a scratch repo that really has the branch name (`git branch start/level-4-speckit HEAD` in a clone of the
  built sandbox); label stand-in files and commits "not a Claude result".
- The Level 3 allow list has no Spec Kit script entry (skills prompt); generated `tasks-template.md` calls tests OPTIONAL.
- `specify init` needs a `claude` program on PATH or `--ignore-agent-tools` (else exit 1, nothing written); a stub proves it only looks it up.
  Rework traps: plain `git status --short` folds untracked folders (10 lines), count with `--untracked-files=all`; a regenerated evidence log may differ from the QA hash when inputs moved, so keep a pre-edit copy and pin refs.
