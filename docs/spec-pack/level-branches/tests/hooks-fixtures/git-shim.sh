#!/bin/sh
# Host "git" for the .sh hook runs in tests/test-hooks.sh, first on PATH. Runs the real git ($REAL_GIT) unless told
# otherwise, and appends the call to $GIT_ARGV_FILE (when set), every argument in brackets.
# GIT_SHIM_MISSING=1   behave like a missing git: no log line, bash's message, exit 127
# GIT_SHIM_FAIL=<glob> fail the call whose bracketed argument line matches the glob, exit 1
if [ "${GIT_SHIM_MISSING:-0}" = 1 ]; then
  echo "git: command not found" >&2
  exit 127
fi
line=""
for a in "$@"; do
  line="$line[$a]"
done
if [ -n "${GIT_ARGV_FILE:-}" ]; then
  printf '%s\n' "$line" >> "$GIT_ARGV_FILE"
fi
pat=${GIT_SHIM_FAIL:-}
if [ -n "$pat" ]; then
  case "$line" in
    $pat) echo "fatal: injected failure: $line" >&2; exit 1 ;;
  esac
fi
exec "$REAL_GIT" "$@"
