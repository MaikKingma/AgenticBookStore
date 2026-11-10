#!/usr/bin/env bash
# Harness for the Level 3 hooks (spec 10, task T8).
#
# Runs the staged hooks of level-3/files against a stub ./mvnw (tests/hooks-fixtures/mvnw-stub.sh) in temp git
# repositories, with the stdin fixtures of tests/hooks-fixtures/stdin:
#   - tests-before-commit.sh and architecture-check-on-stop.sh, started as files through their shebang;
#   - .githooks/pre-commit through a real `git commit` with core.hooksPath .githooks.
# Each hook case checks: exit code, the stub's argv, the git calls, the first stderr line, the tail (number of lines,
# first and last line), stdout, and that the hook left no temp file behind. Every row must equal the expected row.
#
# Offline. Needs bash 3.2 or later and git.
# Locates the staged files relative to its own path, so a copy of the staging folder can be tested (and mutated).
#
# Usage: bash tests/test-hooks.sh
# Env:   TMPDIR       parent of the temp folder; every temp path is made from a template under it
#        KEEP_TMP=1   keep the temp folder and print its path
#
# Exit codes as the hook scripts set them: 0 lets the tool call go ahead (PreToolUse) or Claude finish (Stop), 2 blocks
# the tool call or keeps Claude working, 1 means the project folder cannot be entered. What Claude Code does with a
# code other than 0 and 2 is decided by the hook handler, not by the script: the Level 3 commit hook handler sets
# "onFailure": "block", so Claude Code 2.1.295 or later blocks the commit for such a code as well (and when the hook
# cannot start or times out); the Stop handler has no onFailure. Sources: records/windows-hooks.md.
#
# Two stand-ins keep the runs inside the temp folder: a `mktemp` shim (macOS mktemp without a template ignores
# TMPDIR) and a `git` shim that logs calls and injects failures (tests/hooks-fixtures/).
set -euo pipefail

TEST_DIR=$(cd "$(dirname "$0")" && pwd)
S_DIR=$(cd "$TEST_DIR/.." && pwd)
HOOKS_DIR="$S_DIR/level-3/files/dot-claude/hooks"
GITHOOK="$S_DIR/level-3/files/dot-githooks/pre-commit"
FIX="$TEST_DIR/hooks-fixtures"
STDIN_DIR="$FIX/stdin"

REAL_GIT=$(command -v git)
REAL_MKTEMP=$(command -v mktemp)
export REAL_GIT REAL_MKTEMP

TMP_ROOT=$(mktemp -d "${TMPDIR:-/tmp}/test-hooks.XXXXXX")
cleanup() {
  local rc=$?
  # No errexit here: a failing cleanup step must not hide the result.
  set +e
  if [ "${KEEP_TMP:-0}" = 1 ]; then
    echo "kept: $TMP_ROOT"
  else
    chmod -R u+w "$TMP_ROOT"
    rm -rf "$TMP_ROOT" || echo "warning: could not remove $TMP_ROOT" >&2
  fi
  exit "$rc"
}
trap cleanup EXIT

# Isolate git from the user's and the system's configuration.
export HOME="$TMP_ROOT/home"
mkdir -p "$HOME"
export GIT_CONFIG_GLOBAL="$HOME/.gitconfig"
export GIT_CONFIG_NOSYSTEM=1
unset GIT_DIR GIT_WORK_TREE GIT_INDEX_FILE GIT_OBJECT_DIRECTORY GIT_ALTERNATE_OBJECT_DIRECTORIES GIT_CEILING_DIRECTORIES
unset GIT_AUTHOR_NAME GIT_AUTHOR_EMAIL GIT_AUTHOR_DATE GIT_COMMITTER_NAME GIT_COMMITTER_EMAIL GIT_COMMITTER_DATE
unset CLAUDE_PROJECT_DIR STUB_MODE STUB_ARGV_FILE GIT_ARGV_FILE GIT_SHIM_MISSING GIT_SHIM_FAIL MKTEMP_LOG
"$REAL_GIT" config --global user.name "Hook Test"
"$REAL_GIT" config --global user.email "hook-test@example.invalid"
"$REAL_GIT" config --global init.defaultBranch main
"$REAL_GIT" config --global commit.gpgsign false
"$REAL_GIT" config --global core.autocrlf false

# Shims, first on PATH for the hook runs.
SHIM_BIN="$TMP_ROOT/bin"
mkdir -p "$SHIM_BIN"
cp "$FIX/git-shim.sh" "$SHIM_BIN/git"
cp "$FIX/mktemp-shim.sh" "$SHIM_BIN/mktemp"
chmod 755 "$SHIM_BIN/git" "$SHIM_BIN/mktemp"

HDR_TBC='Commit blocked: ./mvnw verify failed. Fix the cause (do not weaken tests), then commit again.'
HDR_ACS='ArchitectureTest fails. Fix the layering before you finish:'
ARGV_TBC='[-q][verify]'
ARGV_ACS='[-q][-pl][bookstore][test][-Dtest=ArchitectureTest][-Dsurefire.failIfNoSpecifiedTests=false]'
G_DIFF='[diff][--quiet][HEAD][--][*.java]'
G_LS='[ls-files][--others][--exclude-standard][--][*.java]'
G_BOTH="$G_DIFF ; $G_LS"
# Glob for git-shim.sh that fails only the ls-files call ("[[]" is a literal "[").
FAIL_LS='[[]ls-files]*'

# ---------------------------------------------------------------------------------------------------------------
# Reporting
# ---------------------------------------------------------------------------------------------------------------

CASE_NO=0
FAILED=0
FAILED_NAMES=""
TABLE="$TMP_ROOT/table.tsv"
: > "$TABLE"

ok() {
  CASE_NO=$((CASE_NO + 1))
  echo "ok $CASE_NO - $1"
}

not_ok() { # name detail-file
  CASE_NO=$((CASE_NO + 1))
  echo "not ok $CASE_NO - $1"
  sed 's/^/    # /' "$2"
  FAILED=$((FAILED + 1))
  FAILED_NAMES="$FAILED_NAMES
  - $1"
}

# ---------------------------------------------------------------------------------------------------------------
# Repository states
# ---------------------------------------------------------------------------------------------------------------

# base_repo <dir>: a committed repository with the stub mvnw, one Java file, one Markdown file and a .gitignore.
base_repo() {
  local d=$1
  mkdir -p "$d/src"
  "$REAL_GIT" init -q "$d"
  cp "$FIX/mvnw-stub.sh" "$d/mvnw"
  chmod 755 "$d/mvnw"
  printf 'class Foo {}\n' > "$d/src/Foo.java"
  printf '# Readme\n' > "$d/README.md"
  printf 'Ignored.java\n' > "$d/.gitignore"
  "$REAL_GIT" -C "$d" add -A
  "$REAL_GIT" -C "$d" commit -q -m base
}

# make_state <state> <dir>
make_state() {
  local d=$2
  case "$1" in
    clean) base_repo "$d" ;;
    java-change) base_repo "$d"; printf 'class Foo { int x; }\n' > "$d/src/Foo.java" ;;
    md-change) base_repo "$d"; printf '# Readme\nmore\n' > "$d/README.md" ;;
    untracked-java) base_repo "$d"; printf 'class New {}\n' > "$d/src/New.java" ;;
    ignored-java) base_repo "$d"; printf 'class Ignored {}\n' > "$d/Ignored.java" ;;
    staged-java) base_repo "$d"; printf 'class New {}\n' > "$d/src/New.java"; "$REAL_GIT" -C "$d" add src/New.java ;;
    deleted-java) base_repo "$d"; rm "$d/src/Foo.java" ;;
    mvnw-missing) base_repo "$d"; printf 'class Foo { int x; }\n' > "$d/src/Foo.java"; rm "$d/mvnw" ;;
    mvnw-not-executable) base_repo "$d"; printf 'class Foo { int x; }\n' > "$d/src/Foo.java"; chmod 644 "$d/mvnw" ;;
    untracked-java-no-mvnw) base_repo "$d"; printf 'class New {}\n' > "$d/src/New.java"; rm "$d/mvnw" ;;
    not-a-repo)
      mkdir -p "$d/src"
      cp "$FIX/mvnw-stub.sh" "$d/mvnw"
      chmod 755 "$d/mvnw"
      printf 'class Foo {}\n' > "$d/src/Foo.java"
      ;;
    no-commit)
      "$REAL_GIT" init -q "$d"
      cp "$FIX/mvnw-stub.sh" "$d/mvnw"
      chmod 755 "$d/mvnw"
      ;;
    *) echo "unknown state $1" >&2; return 1 ;;
  esac
}

# ---------------------------------------------------------------------------------------------------------------
# Running one hook
# ---------------------------------------------------------------------------------------------------------------

# Per run folder <run>: rc, stdout, stderr, mvnw-argv, git-argv, mktemp-log, tmp/ (the hook's TMPDIR).
# Project folder: "<case>/project dir" (a space proves the quoting). The hook runs from <case> when
# CLAUDE_PROJECT_DIR is set, so a hook that does not change into it finds no mvnw.

# where_to_run <env> <case> <proj>: prints the working folder.
where_to_run() {
  case "$1" in
    unset | empty) printf '%s' "$3" ;;
    *) printf '%s' "$2" ;;
  esac
}

# run_sh <script> <case> <run> <fixture> <stub> <env> <git> <tmp>
run_sh() {
  local script=$1 c=$2 run=$3 fixture=$4 stub=$5 envmode=$6 gitmode=$7 tmpmode=$8 proj rc cwd
  proj="$c/project dir"
  mkdir -p "$run"
  [ "$tmpmode" = missing ] || mkdir -p "$run/tmp"
  cwd=$(where_to_run "$envmode" "$c" "$proj")
  set +e
  (
    set -e
    cd "$cwd"
    export STUB_MODE="$stub" STUB_ARGV_FILE="$run/mvnw-argv" GIT_ARGV_FILE="$run/git-argv"
    export MKTEMP_LOG="$run/mktemp-log" TMPDIR="$run/tmp"
    case "$envmode" in
      set) export CLAUDE_PROJECT_DIR="$proj" ;;
      unset) unset CLAUDE_PROJECT_DIR ;;
      empty) export CLAUDE_PROJECT_DIR= ;;
      missing) export CLAUDE_PROJECT_DIR="$c/no such dir" ;;
    esac
    case "$gitmode" in
      missing) export GIT_SHIM_MISSING=1 ;;
      fail-ls) export GIT_SHIM_FAIL="$FAIL_LS" ;;
    esac
    PATH="$SHIM_BIN:$PATH"
    exec "$script" < "$fixture" > "$run/stdout" 2> "$run/stderr"
  )
  rc=$?
  set -e
  echo "$rc" > "$run/rc"
}

# joined <file>: the lines of a call log joined by " ; ", or "-" when the file is missing or empty (no call).
joined() {
  if [ -s "$1" ]; then
    awk 'NR > 1 { printf " ; " } { printf "%s", $0 } END { print "" }' "$1"
  else
    echo -
  fi
}

# row <id> <run>: the result row of one run, tab separated:
# id, exit, mvnw argv, git calls, stderr line 1, tail lines, tail first, tail last, diag, tmp, stdout bytes.
# stderr is read as hook lines (the two headers), stub lines ("stub-mvnw: ...") and empty lines; any other line is
# a diagnostic of the shell (diag yes), whose wording the row does not pin. In a row with diag no, "stderr line 1" is
# the literal first line of stderr.
row() {
  local id=$1 run=$2 argv gitc kept n line1 tfirst tlast diag tmpstate outb
  argv=$(joined "$run/mvnw-argv")
  gitc=$(joined "$run/git-argv")
  kept="$run/stderr-kept"
  awk -v h1="$HDR_TBC" -v h2="$HDR_ACS" '$0 == h1 || $0 == h2 || $0 == "" || /^stub-mvnw: / { print }' \
    "$run/stderr" > "$kept"
  if awk -v h1="$HDR_TBC" -v h2="$HDR_ACS" '!($0 == h1 || $0 == h2 || $0 == "" || /^stub-mvnw: /) { f = 1 } END { exit f ? 0 : 1 }' "$run/stderr"; then
    diag=yes
  else
    diag=no
  fi
  n=$(wc -l < "$kept" | tr -d ' ')
  if [ "$n" -eq 0 ]; then
    line1=- tfirst=- tlast=-
    n=1
  else
    line1=$(sed -n '1p' "$kept")
    if [ "$n" -ge 2 ]; then tfirst=$(sed -n '2p' "$kept"); tlast=$(sed -n '$p' "$kept"); else tfirst=- tlast=-; fi
  fi
  [ -n "$line1" ] || line1='(empty)'
  [ -n "$tfirst" ] || tfirst='(empty)'
  [ -n "$tlast" ] || tlast='(empty)'
  if [ -d "$run/tmp" ] && [ -n "$(ls -A "$run/tmp")" ]; then tmpstate=left; else tmpstate=clean; fi
  outb=$(wc -c < "$run/stdout" | tr -d ' ')
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$id" "$(cat "$run/rc")" "$argv" "$gitc" "$line1" \
    "$((n - 1))" "$tfirst" "$tlast" "$diag" "$tmpstate" "$outb"
}

# hc <id> <hook tbc|acs> <state> <stdin> <stub> <env> <git> <tmp> -- <exit> <argv> <git> <line1> <tail> <first>
#    <last> <diag> <tmp> <stdout>
# Runs one case and checks its row against the expected row.
hc() {
  local id=$1 hook=$2 state=$3 stdin=$4 stub=$5 envmode=$6 gitmode=$7 tmpmode=$8 c fixture expected got detail
  local logs=0
  shift 9
  expected=$(printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s' "$id" "$@")
  # The commit hook reaches its mktemp when it blocks, or when it lets the commit through after a build.
  if [ "$hook" = tbc ] && [ "$tmpmode" = ok ] && { [ "$1" = 2 ] || { [ "$1" = 0 ] && [ "$2" != - ]; }; }; then
    logs=1
  fi
  c="$TMP_ROOT/cases/$id"
  make_state "$state" "$c/project dir" > /dev/null
  if [ "$stdin" = - ]; then fixture=/dev/null; else fixture="$STDIN_DIR/$stdin"; fi
  case "$hook" in
    tbc) run_sh "$HOOKS_DIR/tests-before-commit.sh" "$c" "$c/run" "$fixture" "$stub" "$envmode" "$gitmode" "$tmpmode" ;;
    acs) run_sh "$HOOKS_DIR/architecture-check-on-stop.sh" "$c" "$c/run" "$fixture" "$stub" "$envmode" "$gitmode" "$tmpmode" ;;
  esac
  got=$(row "$id" "$c/run")
  printf '%s\n' "$got" >> "$TABLE"
  detail="$c/run/detail"
  if [ "$got" = "$expected" ]; then
    if check_temp "$c/run" "$detail" "$logs"; then
      ok "$id"
    else
      not_ok "$id" "$detail"
    fi
  else
    {
      echo "expected: $expected" | tr '\t' '|'
      echo "got:      $got" | tr '\t' '|'
      echo "stderr:"
      sed 's/^/  /' "$c/run/stderr"
    } > "$detail"
    not_ok "$id" "$detail"
  fi
}

# check_temp <run> <detail> <expected log count>: the commit hook made exactly that many logs with mktemp, through
# the shim under the run's TMPDIR (never the system temp folder), and removed each one.
check_temp() {
  local run=$1 p n=0
  : > "$2"
  [ ! -f "$run/mktemp-log" ] || n=$(wc -l < "$run/mktemp-log" | tr -d ' ')
  if [ "$n" -ne "$3" ]; then
    echo "expected $3 temp log(s) made through the mktemp shim, found $n" >> "$2"
    return 1
  fi
  [ -f "$run/mktemp-log" ] || return 0
  while IFS= read -r p; do
    case "$p" in
      "$run/tmp/"*) ;;
      *) echo "log made outside the run's TMPDIR: $p" >> "$2"; return 1 ;;
    esac
    if [ -e "$p" ]; then echo "log not removed: $p" >> "$2"; return 1; fi
  done < "$run/mktemp-log"
}

# ---------------------------------------------------------------------------------------------------------------
# tests-before-commit.sh
# ---------------------------------------------------------------------------------------------------------------

R1='stub-mvnw: red line 1 of 5'
R5='stub-mvnw: red line 5 of 5'
L11='stub-mvnw: red line 11 of 50'
L21='stub-mvnw: red line 21 of 50'
L50='stub-mvnw: red line 50 of 50'
E1='stub-mvnw: red line 1 of 3'
E3='stub-mvnw: red line 3 of 3'
B1='stub-mvnw: red line 1 of 2'
B2='stub-mvnw: red line 2 of 2'

echo "# tests-before-commit"
hc tbc-commit-green tbc clean pre-git-commit.json green set normal ok -- \
  0 "$ARGV_TBC" - - 0 - - no clean 0
hc tbc-commit-green-long-output tbc clean pre-git-commit.json green-long set normal ok -- \
  0 "$ARGV_TBC" - - 0 - - no clean 0
hc tbc-commit-red tbc clean pre-git-commit.json red set normal ok -- \
  2 "$ARGV_TBC" - "$HDR_TBC" 5 "$R1" "$R5" no clean 0
hc tbc-commit-red-long-output-tail-40 tbc clean pre-git-commit.json red-long set normal ok -- \
  2 "$ARGV_TBC" - "$HDR_TBC" 40 "$L11" "$L50" no clean 0
hc tbc-commit-red-output-on-stderr tbc clean pre-git-commit.json red-stderr set normal ok -- \
  2 "$ARGV_TBC" - "$HDR_TBC" 3 "$E1" "$E3" no clean 0
hc tbc-commit-red-no-output tbc clean pre-git-commit.json red-silent set normal ok -- \
  2 "$ARGV_TBC" - "$HDR_TBC" 0 - - no clean 0
hc tbc-commit-red-exit-3 tbc clean pre-git-commit.json red-3 set normal ok -- \
  2 "$ARGV_TBC" - "$HDR_TBC" 5 "$R1" "$R5" no clean 0
# The log keeps trailing empty lines, so the tail shows them.
hc tbc-commit-red-trailing-empty-lines tbc clean pre-git-commit.json red-blank set normal ok -- \
  2 "$ARGV_TBC" - "$HDR_TBC" 4 "$B1" '(empty)' no clean 0
hc tbc-other-command tbc clean pre-git-status.json red set normal ok -- \
  0 - - - 0 - - no clean 0
hc tbc-escaped-quotes-after-commit tbc clean pre-escaped-quotes-after-commit.json red set normal ok -- \
  2 "$ARGV_TBC" - "$HDR_TBC" 5 "$R1" "$R5" no clean 0
# Pack behaviour, not a harness choice: an escaped quote before `git commit` ends the [^"]* of the grep, so the hook
# exits 0 and the call goes ahead without a build (product finding PF-T8-1 in records/windows-hooks.md).
hc tbc-escaped-quotes-before-commit tbc clean pre-escaped-quotes-before-commit.json red set normal ok -- \
  0 - - - 0 - - no clean 0
hc tbc-spaces-in-json-and-command tbc clean pre-spaced.json red set normal ok -- \
  2 "$ARGV_TBC" - "$HDR_TBC" 5 "$R1" "$R5" no clean 0
hc tbc-pretty-printed-json tbc clean pre-pretty.json red set normal ok -- \
  2 "$ARGV_TBC" - "$HDR_TBC" 5 "$R1" "$R5" no clean 0
# A tab is JSON whitespace too: [[:space:]] of the grep matches it.
hc tbc-tab-after-command-colon tbc clean pre-tab-after-colon.json red set normal ok -- \
  2 "$ARGV_TBC" - "$HDR_TBC" 5 "$R1" "$R5" no clean 0
# Each JSON whitespace member at each side of the colon. A space and a tab on either side, a CR on either side (a lone
# CR, no LF) and a mixed run on both sides all match. An LF before the colon (and after it, the two-lines case below)
# ends the line first, and grep matches line by line, so it does not match. VT and FF are in [[:space:]] but are not
# JSON whitespace, and a raw control byte inside the command string is not valid JSON, so no stdin carries them.
hc tbc-tab-before-command-colon tbc clean pre-tab-before-colon.json red set normal ok -- \
  2 "$ARGV_TBC" - "$HDR_TBC" 5 "$R1" "$R5" no clean 0
hc tbc-cr-after-command-colon tbc clean pre-cr-after-colon.json red set normal ok -- \
  2 "$ARGV_TBC" - "$HDR_TBC" 5 "$R1" "$R5" no clean 0
hc tbc-cr-before-command-colon tbc clean pre-cr-before-colon.json red set normal ok -- \
  2 "$ARGV_TBC" - "$HDR_TBC" 5 "$R1" "$R5" no clean 0
hc tbc-mixed-whitespace-runs-around-command-colon tbc clean pre-mixed-whitespace-runs.json red set normal ok -- \
  2 "$ARGV_TBC" - "$HDR_TBC" 5 "$R1" "$R5" no clean 0
hc tbc-newline-before-command-colon tbc clean pre-newline-before-colon.json red set normal ok -- \
  0 - - - 0 - - no clean 0
hc tbc-key-and-value-on-two-lines tbc clean pre-split-lines.json red set normal ok -- \
  0 - - - 0 - - no clean 0
# Not valid JSON (a raw newline inside a string): pins that the match works line by line, as grep does.
hc tbc-raw-newline-in-command-string tbc clean pre-raw-newline-in-command.json red set normal ok -- \
  0 - - - 0 - - no clean 0
hc tbc-uppercase-command tbc clean pre-uppercase.json red set normal ok -- \
  0 - - - 0 - - no clean 0
hc tbc-git-commit-only-in-description tbc clean pre-description-only.json red set normal ok -- \
  0 - - - 0 - - no clean 0
# A chained command: the [^"]* before git reaches a git commit later in the command string.
hc tbc-chained-git-add-and-git-commit tbc clean pre-chained-add-and-commit.json red set normal ok -- \
  2 "$ARGV_TBC" - "$HDR_TBC" 5 "$R1" "$R5" no clean 0
# git and commit without whitespace between them, inside a word, is not a git commit.
hc tbc-gitcommit-inside-a-word tbc clean pre-gitcommit-inside-a-word.json red set normal ok -- \
  0 - - - 0 - - no clean 0
hc tbc-empty-stdin tbc clean - red set normal ok -- \
  0 - - - 0 - - no clean 0
hc tbc-project-dir-unset tbc clean pre-git-commit.json red unset normal ok -- \
  2 "$ARGV_TBC" - "$HDR_TBC" 5 "$R1" "$R5" no clean 0
hc tbc-project-dir-empty tbc clean pre-git-commit.json red empty normal ok -- \
  2 "$ARGV_TBC" - "$HDR_TBC" 5 "$R1" "$R5" no clean 0
# Could not enter the project folder: exit 1 from the script, a diagnostic, no build.
hc tbc-could-not-cd tbc clean pre-git-commit.json red missing normal ok -- \
  1 - - - 0 - - yes clean 0
# Could not start ./mvnw: blocked (exit 2) with the header; the shell's message is in the tail.
hc tbc-could-not-start-mvnw-missing tbc mvnw-missing pre-git-commit.json red set normal ok -- \
  2 - - "$HDR_TBC" 0 - - yes clean 0
hc tbc-could-not-start-mvnw-not-executable tbc mvnw-not-executable pre-git-commit.json red set normal ok -- \
  2 - - "$HDR_TBC" 0 - - yes clean 0
# Could not make the temp log: the build does not run and the commit stays blocked.
hc tbc-could-not-make-temp-log tbc clean pre-git-commit.json green set normal missing -- \
  2 - - "$HDR_TBC" 0 - - yes clean 0

# ---------------------------------------------------------------------------------------------------------------
# architecture-check-on-stop.sh
# ---------------------------------------------------------------------------------------------------------------

echo "# architecture-check-on-stop"
hc acs-java-change-green acs java-change stop-active-false.json green set normal ok -- \
  0 "$ARGV_ACS" "$G_DIFF" - 0 - - no clean 0
hc acs-java-change-red acs java-change stop-active-false.json red set normal ok -- \
  2 "$ARGV_ACS" "$G_DIFF" "$HDR_ACS" 5 "$R1" "$R5" no clean 0
hc acs-java-change-red-long-output-tail-30 acs java-change stop-active-false.json red-long set normal ok -- \
  2 "$ARGV_ACS" "$G_DIFF" "$HDR_ACS" 30 "$L21" "$L50" no clean 0
hc acs-java-change-red-output-on-stderr acs java-change stop-active-false.json red-stderr set normal ok -- \
  2 "$ARGV_ACS" "$G_DIFF" "$HDR_ACS" 3 "$E1" "$E3" no clean 0
hc acs-java-change-red-no-output acs java-change stop-active-false.json red-silent set normal ok -- \
  2 "$ARGV_ACS" "$G_DIFF" "$HDR_ACS" 1 '(empty)' '(empty)' no clean 0
hc acs-java-change-red-exit-3 acs java-change stop-active-false.json red-3 set normal ok -- \
  2 "$ARGV_ACS" "$G_DIFF" "$HDR_ACS" 5 "$R1" "$R5" no clean 0
# "$(...)" drops trailing empty lines of the output before the tail.
hc acs-java-change-red-trailing-empty-lines acs java-change stop-active-false.json red-blank set normal ok -- \
  2 "$ARGV_ACS" "$G_DIFF" "$HDR_ACS" 2 "$B1" "$B2" no clean 0
hc acs-no-java-change acs md-change stop-active-false.json red set normal ok -- \
  0 - "$G_BOTH" - 0 - - no clean 0
hc acs-clean-tree acs clean stop-active-false.json red set normal ok -- \
  0 - "$G_BOTH" - 0 - - no clean 0
hc acs-untracked-java-file acs untracked-java stop-active-false.json red set normal ok -- \
  2 "$ARGV_ACS" "$G_BOTH" "$HDR_ACS" 5 "$R1" "$R5" no clean 0
hc acs-ignored-java-file acs ignored-java stop-active-false.json red set normal ok -- \
  0 - "$G_BOTH" - 0 - - no clean 0
hc acs-staged-java-file acs staged-java stop-active-false.json red set normal ok -- \
  2 "$ARGV_ACS" "$G_DIFF" "$HDR_ACS" 5 "$R1" "$R5" no clean 0
hc acs-deleted-java-file acs deleted-java stop-active-false.json red set normal ok -- \
  2 "$ARGV_ACS" "$G_DIFF" "$HDR_ACS" 5 "$R1" "$R5" no clean 0
hc acs-stop-hook-active-true acs java-change stop-active-true.json red set normal ok -- \
  0 - - - 0 - - no clean 0
hc acs-stop-hook-active-true-with-spaces acs java-change stop-active-true-spaced.json red set normal ok -- \
  0 - - - 0 - - no clean 0
hc acs-stop-hook-active-true-with-a-tab acs java-change stop-active-true-tab.json red set normal ok -- \
  0 - - - 0 - - no clean 0
# The same whitespace members as for the commit hook; an LF before the colon misses the loop guard, as on two lines.
hc acs-stop-hook-active-true-tab-before-colon acs java-change stop-active-true-tab-before-colon.json red set normal ok -- \
  0 - - - 0 - - no clean 0
hc acs-stop-hook-active-true-cr-after-colon acs java-change stop-active-true-cr-after-colon.json red set normal ok -- \
  0 - - - 0 - - no clean 0
hc acs-stop-hook-active-true-cr-before-colon acs java-change stop-active-true-cr-before-colon.json red set normal ok -- \
  0 - - - 0 - - no clean 0
hc acs-stop-hook-active-true-mixed-whitespace-runs acs java-change stop-active-true-mixed-whitespace-runs.json red set normal ok -- \
  0 - - - 0 - - no clean 0
hc acs-stop-hook-active-newline-before-colon acs java-change stop-active-true-newline-before-colon.json red set normal ok -- \
  2 "$ARGV_ACS" "$G_DIFF" "$HDR_ACS" 5 "$R1" "$R5" no clean 0
hc acs-stop-hook-active-string acs java-change stop-active-string.json red set normal ok -- \
  2 "$ARGV_ACS" "$G_DIFF" "$HDR_ACS" 5 "$R1" "$R5" no clean 0
hc acs-stop-hook-active-uppercase-key acs java-change stop-active-uppercase-key.json red set normal ok -- \
  2 "$ARGV_ACS" "$G_DIFF" "$HDR_ACS" 5 "$R1" "$R5" no clean 0
hc acs-stop-hook-active-on-two-lines acs java-change stop-active-split-lines.json red set normal ok -- \
  2 "$ARGV_ACS" "$G_DIFF" "$HDR_ACS" 5 "$R1" "$R5" no clean 0
hc acs-empty-stdin acs java-change - red set normal ok -- \
  2 "$ARGV_ACS" "$G_DIFF" "$HDR_ACS" 5 "$R1" "$R5" no clean 0
hc acs-project-dir-unset acs java-change stop-active-false.json red unset normal ok -- \
  2 "$ARGV_ACS" "$G_DIFF" "$HDR_ACS" 5 "$R1" "$R5" no clean 0
hc acs-project-dir-empty acs java-change stop-active-false.json red empty normal ok -- \
  2 "$ARGV_ACS" "$G_DIFF" "$HDR_ACS" 5 "$R1" "$R5" no clean 0
# Could not enter the project folder: exit 1 from the script; the Stop handler has no onFailure.
hc acs-could-not-cd acs java-change stop-active-false.json red missing normal ok -- \
  1 - - - 0 - - yes clean 0
# git cannot answer (no repository, no commit, no git): counted as a Java change, so the check runs.
hc acs-git-diff-fails-not-a-repo acs not-a-repo stop-active-false.json red set normal ok -- \
  2 "$ARGV_ACS" "$G_DIFF" "$HDR_ACS" 5 "$R1" "$R5" yes clean 0
hc acs-git-diff-fails-no-commit acs no-commit stop-active-false.json red set normal ok -- \
  2 "$ARGV_ACS" "$G_DIFF" "$HDR_ACS" 5 "$R1" "$R5" yes clean 0
hc acs-could-not-start-git acs java-change stop-active-false.json red set missing ok -- \
  2 "$ARGV_ACS" - "$HDR_ACS" 5 "$R1" "$R5" yes clean 0
# git ls-files fails: its empty output reads as "no untracked Java file" (pack behaviour).
hc acs-git-ls-files-fails acs untracked-java stop-active-false.json red set fail-ls ok -- \
  0 - "$G_BOTH" - 0 - - yes clean 0
hc acs-could-not-start-mvnw-missing acs mvnw-missing stop-active-false.json red set normal ok -- \
  2 - "$G_DIFF" "$HDR_ACS" 0 - - yes clean 0
# The same with only an untracked Java file: both git calls succeed before the wrapper cannot start.
hc acs-could-not-start-mvnw-untracked-java-file acs untracked-java-no-mvnw stop-active-false.json red set normal ok -- \
  2 - "$G_BOTH" "$HDR_ACS" 0 - - yes clean 0

# ---------------------------------------------------------------------------------------------------------------
# .githooks/pre-commit through git commit
# ---------------------------------------------------------------------------------------------------------------

echo "# .githooks/pre-commit"

# gh_repo <dir> <activate yes|no>
gh_repo() {
  base_repo "$1"
  mkdir -p "$1/.githooks"
  cp -p "$GITHOOK" "$1/.githooks/pre-commit"
  "$REAL_GIT" -C "$1" add .githooks/pre-commit
  "$REAL_GIT" -C "$1" commit -q -m hooks
  if [ "$2" = yes ]; then "$REAL_GIT" -C "$1" config core.hooksPath .githooks; fi
  printf 'class Foo { int y; }\n' > "$1/src/Foo.java"
  "$REAL_GIT" -C "$1" add src/Foo.java
}

# gc <id> <activate> <stub> <commit-flag|-> -- <git-exit> <argv> <commits-added> <stderr-first|-> <stderr-last|->
#    <stdout bytes>
# git runs hooks with their stdout sent to stderr, so both hook lines arrive on the stderr of `git commit`.
gc() {
  local id=$1 activate=$2 stub=$3 flag=$4 d run rc before after got expected argv errf errl outb
  shift 5
  expected=$(printf '%s\t%s\t%s\t%s\t%s\t%s' "$@")
  d="$TMP_ROOT/cases/$id/repo"
  run="$TMP_ROOT/cases/$id/run"
  mkdir -p "$run"
  gh_repo "$d" "$activate" > /dev/null
  before=$("$REAL_GIT" -C "$d" rev-list --count HEAD)
  set +e
  if [ "$flag" = - ]; then
    STUB_MODE=$stub STUB_ARGV_FILE="$run/mvnw-argv" "$REAL_GIT" -C "$d" commit -q -m test > "$run/stdout" 2> "$run/stderr"
  else
    STUB_MODE=$stub STUB_ARGV_FILE="$run/mvnw-argv" "$REAL_GIT" -C "$d" commit -q "$flag" -m test > "$run/stdout" 2> "$run/stderr"
  fi
  rc=$?
  set -e
  after=$("$REAL_GIT" -C "$d" rev-list --count HEAD)
  argv=$(joined "$run/mvnw-argv")
  if [ -s "$run/stderr" ]; then errf=$(sed -n '1p' "$run/stderr"); errl=$(sed -n '$p' "$run/stderr"); else errf=- errl=-; fi
  outb=$(wc -c < "$run/stdout" | tr -d ' ')
  got=$(printf '%s\t%s\t%s\t%s\t%s\t%s' "$rc" "$argv" "$((after - before))" "$errf" "$errl" "$outb")
  if [ "$got" = "$expected" ]; then
    ok "$id"
  else
    {
      echo "expected: $expected" | tr '\t' '|'
      echo "got:      $got" | tr '\t' '|'
    } > "$run/detail"
    not_ok "$id" "$run/detail"
  fi
}

gc githook-green-commit-goes-through yes green - -- \
  0 "$ARGV_TBC" 1 'pre-commit: ./mvnw -q verify' 'stub-mvnw: green line 1 of 1' 0
gc githook-red-commit-aborted yes red - -- \
  1 "$ARGV_TBC" 0 'pre-commit: ./mvnw -q verify' 'pre-commit: build failed, commit aborted' 0
gc githook-inactive-without-hooks-path no red - -- \
  0 - 1 - - 0
gc githook-skipped-by-no-verify yes red --no-verify -- \
  0 - 1 - - 0

# ---------------------------------------------------------------------------------------------------------------
# Result
# ---------------------------------------------------------------------------------------------------------------

echo "# result table (id, exit, mvnw argv, git calls, stderr line 1, tail lines, tail first, tail last, diag, tmp, stdout bytes)"
sed 's/^/#   /' "$TABLE"
echo "# bash: $BASH_VERSION"
echo "1..$CASE_NO"
if [ "$FAILED" -ne 0 ]; then
  echo "FAILED: $FAILED of $CASE_NO$FAILED_NAMES"
  exit 1
fi
echo "PASSED: $CASE_NO of $CASE_NO"
