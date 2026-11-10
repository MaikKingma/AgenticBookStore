#!/bin/sh
# Host "mktemp" for the .sh hook runs in tests/test-hooks.sh, first on PATH. macOS mktemp without a template ignores
# TMPDIR, so the verbatim `mktemp` of tests-before-commit.sh would write outside the test folder. A call without
# arguments makes the file from a template under $TMPDIR instead (it fails like mktemp when that folder is missing)
# and appends the path to $MKTEMP_LOG (when set), so the harness can check that the hook removed it.
if [ "$#" -ne 0 ]; then
  exec "$REAL_MKTEMP" "$@"
fi
p=$("$REAL_MKTEMP" "${TMPDIR:-/tmp}/hooklog.XXXXXX") || exit 1
if [ -n "${MKTEMP_LOG:-}" ]; then
  printf '%s\n' "$p" >> "$MKTEMP_LOG"
fi
printf '%s\n' "$p"
