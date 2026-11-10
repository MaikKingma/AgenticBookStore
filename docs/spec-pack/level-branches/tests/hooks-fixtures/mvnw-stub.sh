#!/bin/sh
# Stand-in for ./mvnw in tests/test-hooks.sh. The harness copies it into a temp repository as "mvnw".
# Appends one line to $STUB_ARGV_FILE per call: every argument in brackets, so a split argument shows.
# STUB_MODE: green        1 line on stdout, exit 0
#            green-long  50 lines on stdout, exit 0
#            red          5 lines on stdout, exit 1
#            red-long    50 lines on stdout, exit 1
#            red-stderr   3 lines on stderr, exit 1
#            red-silent   no output, exit 1
#            red-3        5 lines on stdout, exit 3
#            red-blank    2 lines and then 2 empty lines on stdout, exit 1
if [ -z "${STUB_ARGV_FILE:-}" ] || [ -z "${STUB_MODE:-}" ]; then
  echo "stub-mvnw: STUB_ARGV_FILE and STUB_MODE must be set" >&2
  exit 99
fi
line=""
for a in "$@"; do
  line="$line[$a]"
done
printf '%s\n' "$line" >> "$STUB_ARGV_FILE"
lines() { # count label stream
  i=1
  while [ "$i" -le "$1" ]; do
    if [ "$3" = err ]; then
      echo "stub-mvnw: $2 line $i of $1" >&2
    else
      echo "stub-mvnw: $2 line $i of $1"
    fi
    i=$((i + 1))
  done
}
case "$STUB_MODE" in
  green) lines 1 green out; exit 0 ;;
  green-long) lines 50 green out; exit 0 ;;
  red) lines 5 red out; exit 1 ;;
  red-long) lines 50 red out; exit 1 ;;
  red-stderr) lines 3 red err; exit 1 ;;
  red-silent) exit 1 ;;
  red-3) lines 5 red out; exit 3 ;;
  red-blank) lines 2 red out; echo; echo; exit 1 ;;
  *) echo "stub-mvnw: unknown STUB_MODE $STUB_MODE" >&2; exit 99 ;;
esac
