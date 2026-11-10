#!/usr/bin/env bash
# Stop hook: when Java files changed, run only the fast ArchitectureTest before Claude may finish.
input="$(cat)"
if printf '%s' "$input" | grep -Eq '"stop_hook_active"[[:space:]]*:[[:space:]]*true'; then
  exit 0   # we already blocked once in this turn; avoid loops
fi
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 1
if git diff --quiet HEAD -- '*.java' && [ -z "$(git ls-files --others --exclude-standard -- '*.java')" ]; then
  exit 0   # no Java changes
fi
if ! out="$(./mvnw -q -pl bookstore test -Dtest=ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1)"; then
  { echo "ArchitectureTest fails. Fix the layering before you finish:"; printf '%s\n' "$out" | tail -n 30; } >&2
  exit 2
fi
exit 0
