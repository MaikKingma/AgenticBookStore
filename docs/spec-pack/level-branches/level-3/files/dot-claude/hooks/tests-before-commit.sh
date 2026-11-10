#!/usr/bin/env bash
# PreToolUse hook: block `git commit` unless the full build is green.
# Exit code 2 blocks the tool call and feeds stderr back to Claude. Any other non-zero code does NOT block.
input="$(cat)"
# The "if" filter in settings.json is best-effort, so check the command ourselves too.
if ! printf '%s' "$input" | grep -Eq '"command"[[:space:]]*:[[:space:]]*"[^"]*git[[:space:]]+commit'; then
  exit 0
fi
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 1
log="$(mktemp)"
if ./mvnw -q verify >"$log" 2>&1; then
  rm -f "$log"
  exit 0
fi
{
  echo "Commit blocked: ./mvnw verify failed. Fix the cause (do not weaken tests), then commit again."
  tail -n 40 "$log"
} >&2
rm -f "$log"
exit 2
