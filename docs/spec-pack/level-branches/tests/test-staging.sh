#!/usr/bin/env bash
# Self-test for the level-branches staging scripts (spec 10, task T1).
#
# Offline. Builds every repository it needs in a temp folder and never writes to the repository it lives in.
# Locates the scripts relative to its own path, so a copy of the staging folder can be tested (and mutated) anywhere.
#
# Usage: bash tests/test-staging.sh
# Env:   SPEC09_FILE  path to docs/spec-pack/specs/09-workshop-baseline.md (default: next to the staging folder)
#        KEEP_TMP=1   keep the temp folder and print its path
#        TMPDIR       parent of the temp folder
set -euo pipefail

TEST_DIR=$(cd "$(dirname "$0")" && pwd)
S_DIR=$(cd "$TEST_DIR/.." && pwd)
SPEC09_FILE=${SPEC09_FILE:-$S_DIR/../specs/09-workshop-baseline.md}

TMP_ROOT=$(mktemp -d "${TMPDIR:-/tmp}/test-staging.XXXXXX")
# A space in every work path proves the scripts quote their paths.
WORK="$TMP_ROOT/work dir"
mkdir -p "$WORK"

cleanup() {
  local rc=$?
  # No errexit here: a failing cleanup step must not hide the result or skip the next step.
  set +e
  if [ "${KEEP_TMP:-0}" = 1 ]; then
    echo "kept: $TMP_ROOT"
  else
    # Some cases make read-only files and folders.
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
git config --global user.name "Staging Test"
git config --global user.email "staging-test@example.invalid"
git config --global init.defaultBranch main
git config --global advice.detachedHead false
git config --global commit.gpgsign false
git config --global tag.gpgsign false
git config --global core.autocrlf false

TAB=$(printf '\t')
CR=$(printf '\r')

# ---------------------------------------------------------------------------------------------------------------
# Case runner and assertions
# ---------------------------------------------------------------------------------------------------------------

CASE_NO=0
FAILED=0
FAILED_NAMES=""

# run_case <name> <function>: runs the function in a subshell with errexit; prints ok / not ok.
run_case() {
  local name=$1 fn=$2 rc out
  CASE_NO=$((CASE_NO + 1))
  out="$TMP_ROOT/case-$CASE_NO.txt"
  set +e
  (
    set -e
    CASE_TMP="$WORK/case-$CASE_NO"
    mkdir -p "$CASE_TMP"
    "$fn"
  ) > "$out" 2>&1
  rc=$?
  set -e
  if [ "$rc" -eq 0 ]; then
    echo "ok $CASE_NO - $name"
  else
    echo "not ok $CASE_NO - $name"
    sed 's/^/    # /' "$out"
    FAILED=$((FAILED + 1))
    FAILED_NAMES="$FAILED_NAMES
  - $name"
  fi
}

die() {
  echo "ASSERTION FAILED: $*" >&2
  exit 1
}

# expect_ok <outfile> <cmd...>: the command must exit 0; its output goes to <outfile>.
expect_ok() {
  local of=$1 rc
  shift
  set +e
  "$@" > "$of" 2>&1
  rc=$?
  set -e
  if [ "$rc" -ne 0 ]; then
    sed 's/^/      /' "$of" >&2
    die "expected exit 0, got $rc: $*"
  fi
}

# expect_fail <outfile> <cmd...>: the command must exit non-zero; its output goes to <outfile>.
expect_fail() {
  local of=$1 rc
  shift
  set +e
  "$@" > "$of" 2>&1
  rc=$?
  set -e
  if [ "$rc" -eq 0 ]; then
    sed 's/^/      /' "$of" >&2
    die "expected a non-zero exit: $*"
  fi
}

# assert_grep <file> <fixed string>
assert_grep() {
  grep -F -q -- "$2" "$1" || { sed 's/^/      /' "$1" >&2; die "'$2' not found in output"; }
}

# assert_no_grep <file> <fixed string>
assert_no_grep() {
  if grep -F -q -- "$2" "$1"; then
    sed 's/^/      /' "$1" >&2
    die "'$2' found in output"
  fi
}

assert_eq() {
  [ "$1" = "$2" ] || die "$3: expected [$2], got [$1]"
}

assert_file() {
  [ -f "$1" ] || die "missing file: $1"
}

assert_absent() {
  if [ -e "$1" ] || [ -L "$1" ]; then
    die "path should not exist: $1"
  fi
}

# ref_state <repo>: every ref, every worktree, every tag, HEAD, the index and the status of a repository.
ref_state() {
  git -C "$1" for-each-ref --format='%(refname) %(objectname)'
  git -C "$1" worktree list --porcelain
  git -C "$1" tag --list
  git -C "$1" rev-parse HEAD
  git -C "$1" symbolic-ref -q HEAD || true
  git -C "$1" ls-files --stage
  git -C "$1" status --porcelain=v1 --untracked-files=all --ignored
}

# tree_state <dir>: every path, its type, exec bit and checksum, plus the git index and status when it is a repo.
tree_state() {
  (
    cd "$1"
    find . -path ./.git -prune -o -print | LC_ALL=C sort
    find . -path ./.git -prune -o -type f -perm -u+x -print | LC_ALL=C sort | sed 's/^/x /'
    find . -path ./.git -prune -o -type f -exec cksum {} + | LC_ALL=C sort
    if [ -d .git ]; then
      git rev-parse HEAD
      git ls-files --stage
      git status --porcelain=v1 --untracked-files=all
    fi
  )
}

commit_count() {
  git -C "$1" rev-list --count HEAD
}

# ---------------------------------------------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------------------------------------------

# spec09_list: the strip list as written in spec 09, "Publishing main", step 2 (top-level backticked paths only;
# the parenthesised remarks name sub-paths and are dropped).
spec09_list() {
  [ -f "$SPEC09_FILE" ] || die "spec 09 not found at $SPEC09_FILE (set SPEC09_FILE)"
  awk '/^## Publishing `main`/ { f = 1 } f && /^2\. / { g = 1 } g && /^3\. / { exit } g { print }' "$SPEC09_FILE" \
    | tr '\n' ' ' \
    | sed -e 's/([^)]*)//g' -e 's/.*removes these paths if present://' \
    | grep -o '`[^`]*`' \
    | tr -d '`'
}

# write_file <path> <content...>: writes the lines given, LF terminated, creating parent folders.
write_file() {
  local f=$1
  shift
  mkdir -p "$(dirname "$f")"
  printf '%s\n' "$@" > "$f"
}

foo_base_lines() {
  printf '%s\n' 'line 01' 'line 02' 'line 03' 'line 04' 'line 05' 'line 06' 'line 07' 'line 08' 'line 09' \
    'line 10' 'line 11' 'line 12' 'line 13' 'line 14'
}

# make_source <dir>: a factory-like source repo with every spec 09 strip path, control files and a tag.
make_source() {
  local d=$1 p
  mkdir -p "$d"
  git -C "$d" init -q
  write_file "$d/.gitattributes" '*.sh text eol=lf' 'mvnw text eol=lf' '.githooks/* text eol=lf' \
    '*.cmd text eol=crlf' '*.ps1 text eol=crlf'
  write_file "$d/.gitignore" 'target/' '*.log' 'notes/' 'CLAUDE.local.md'
  write_file "$d/README.md" 'readme'
  write_file "$d/docs/keep.md" 'kept'
  write_file "$d/docs/spec-pack-sibling.md" 'kept, not under docs/spec-pack/'
  write_file "$d/mvnw" '#!/bin/sh' 'echo mvnw'
  chmod 755 "$d/mvnw"
  foo_base_lines > "$d/foo.tmp"
  mkdir -p "$d/bookstore"
  mv "$d/foo.tmp" "$d/bookstore/Foo.java"
  write_file "$d/extra-strip/x.md" 'stripped only when the publish script lists it'
  spec09_list > "$TMP_ROOT/spec09.txt"
  while IFS= read -r p; do
    case $p in
      */) write_file "$d/${p}placeholder.txt" "strip me" ;;
      *) write_file "$d/$p" "strip me" ;;
    esac
  done < "$TMP_ROOT/spec09.txt"
  write_file "$d/.claude/skills/x/SKILL.md" 'nested'
  write_file "$d/agent-os/specs/p/plan.md" 'nested'
  git -C "$d" add --all --force
  git -C "$d" commit -q -m "source"
  git -C "$d" tag src-tag
}

SOURCE=""
# shared_source: one source repo for the read-only simulate cases.
shared_source() {
  SOURCE="$TMP_ROOT/shared source"
  if [ ! -d "$SOURCE/.git" ]; then
    make_source "$SOURCE"
    ref_state "$SOURCE" > "$TMP_ROOT/source-state-before.txt"
  fi
}

# fake_publish <source> <body>: commits scripts/publish-workshop-main.sh with the given body, tags it with-publish.
fake_publish() {
  local d=$1
  shift
  write_file "$d/scripts/publish-workshop-main.sh" '#!/usr/bin/env bash' "$@"
  chmod 755 "$d/scripts/publish-workshop-main.sh"
  git -C "$d" add --all
  git -C "$d" commit -q -m "fake publish script"
}

# ---------------------------------------------------------------------------------------------------------------
# simulate-level-0.sh
# ---------------------------------------------------------------------------------------------------------------

SIM="$S_DIR/scripts/simulate-level-0.sh"

case_sim_builtin_list_equals_spec09() {
  shared_source
  local out="$CASE_TMP/out"
  expect_ok "$CASE_TMP/run.txt" bash "$SIM" --repo "$SOURCE" src-tag "$out"
  assert_grep "$CASE_TMP/run.txt" "list: built-in"
  sed -n 's/^strip-path: //p' "$CASE_TMP/run.txt" > "$CASE_TMP/used.txt"
  [ -s "$CASE_TMP/used.txt" ] || die "no strip-path lines printed"
  spec09_list > "$CASE_TMP/spec.txt"
  [ "$(wc -l < "$CASE_TMP/spec.txt")" -ge 11 ] || die "spec 09 list extraction found fewer than 11 paths"
  diff "$CASE_TMP/spec.txt" "$CASE_TMP/used.txt" || die "built-in list differs from spec 09"
}

case_sim_strips_every_path_and_keeps_the_rest() {
  shared_source
  local out="$CASE_TMP/out" p
  expect_ok "$CASE_TMP/run.txt" bash "$SIM" --repo "$SOURCE" src-tag "$out"
  while IFS= read -r p; do
    assert_absent "$out/${p%/}"
  done < "$TMP_ROOT/spec09.txt"
  assert_file "$out/README.md"
  assert_file "$out/docs/keep.md"
  assert_file "$out/docs/spec-pack-sibling.md"
  assert_file "$out/bookstore/Foo.java"
  assert_file "$out/extra-strip/x.md"
  # The commit holds exactly the source tree minus the stripped paths.
  git -C "$SOURCE" ls-tree -r --name-only src-tag | while IFS= read -r p; do
    case $p in
      CLAUDE.md|CLAUDE.local.md|AGENTS.md|.claude/*|.mcp.json|openspec/*|.specify/*|specs/*|agent-os/*|.githooks/*|docs/spec-pack/*) ;;
      *) echo "$p" ;;
    esac
  done > "$CASE_TMP/expected.txt"
  git -C "$out" ls-files > "$CASE_TMP/actual.txt"
  diff "$CASE_TMP/expected.txt" "$CASE_TMP/actual.txt" || die "simulated tree differs from source minus strip list"
}

case_sim_single_reproducible_commit() {
  shared_source
  expect_ok "$CASE_TMP/run1.txt" bash "$SIM" --repo "$SOURCE" src-tag "$CASE_TMP/a"
  expect_ok "$CASE_TMP/run2.txt" bash "$SIM" --repo "$SOURCE" src-tag "$CASE_TMP/b"
  assert_eq "$(commit_count "$CASE_TMP/a")" 1 "commit count"
  assert_eq "$(git -C "$CASE_TMP/a" rev-parse HEAD)" "$(git -C "$CASE_TMP/b" rev-parse HEAD)" "commit sha of two runs"
  assert_eq "$(git -C "$CASE_TMP/a" status --porcelain)" "" "status of the simulated repo"
  assert_eq "$(git -C "$CASE_TMP/a" ls-files --stage mvnw | cut -c1-6)" 100755 "mode of mvnw"
}

case_sim_uses_list_paths_of_the_ref() {
  local src="$CASE_TMP/src" out="$CASE_TMP/out" p
  make_source "$src"
  # The fake lists the spec 09 paths plus one extra path; the extra path must be stripped too.
  fake_publish "$src" 'if [ "${1:-}" = --list-paths ]; then' \
    "  printf '%s\n' CLAUDE.md CLAUDE.local.md AGENTS.md .claude/ .mcp.json openspec/ .specify/ specs/ agent-os/ .githooks/ docs/spec-pack/ extra-strip/" \
    '  exit 0' 'fi' 'exit 1'
  expect_ok "$CASE_TMP/run.txt" bash "$SIM" --repo "$src" HEAD "$out"
  assert_grep "$CASE_TMP/run.txt" "list: scripts/publish-workshop-main.sh --list-paths"
  assert_grep "$CASE_TMP/run.txt" "strip-path: extra-strip/"
  assert_absent "$out/extra-strip"
  while IFS= read -r p; do
    assert_absent "$out/${p%/}"
  done < "$TMP_ROOT/spec09.txt"
  assert_file "$out/scripts/publish-workshop-main.sh"
  assert_file "$out/docs/keep.md"
}

case_sim_falls_back_when_list_paths_fails() {
  local src="$CASE_TMP/src" out="$CASE_TMP/out" p
  make_source "$src"
  fake_publish "$src" 'echo "unknown option: $1" >&2' 'exit 3'
  expect_ok "$CASE_TMP/run.txt" bash "$SIM" --repo "$src" HEAD "$out"
  assert_grep "$CASE_TMP/run.txt" "--list-paths failed (exit 3)"
  assert_grep "$CASE_TMP/run.txt" "list: built-in"
  assert_file "$out/extra-strip/x.md"
  while IFS= read -r p; do
    assert_absent "$out/${p%/}"
  done < "$TMP_ROOT/spec09.txt"
}

case_sim_reports_absent_publish_script() {
  shared_source
  expect_ok "$CASE_TMP/run.txt" bash "$SIM" --repo "$SOURCE" src-tag "$CASE_TMP/out"
  assert_grep "$CASE_TMP/run.txt" "no scripts/publish-workshop-main.sh in src-tag"
  assert_grep "$CASE_TMP/run.txt" "list: built-in"
}

# refuse_list <source> <out-dir> <printf command> <expected reason>: a ref whose publish script runs the printf
# command for --list-paths and exits 0 must be refused with the reason.
refuse_list() {
  local src=$1 out=$2 cmd=$3 reason=$4
  git -C "$src" rm -q -r --cached --ignore-unmatch scripts > /dev/null
  rm -rf "$src/scripts"
  fake_publish "$src" "$cmd" 'exit 0'
  expect_fail "$CASE_TMP/run.txt" bash "$SIM" --repo "$src" HEAD "$out"
  assert_grep "$CASE_TMP/run.txt" "simulate-level-0: error: scripts/publish-workshop-main.sh --list-paths"
  assert_grep "$CASE_TMP/run.txt" "$reason"
}

case_sim_refuses_unsafe_list_paths_output() {
  local src="$CASE_TMP/src" out="$CASE_TMP/out" empty="$CASE_TMP/empty-out"
  make_source "$src"
  refuse_list "$src" "$out" "printf '%s\\n' ../outside" "unsafe path ('..' path component): '../outside'"
  assert_absent "$out"
  refuse_list "$src" "$out" "printf '%s\\n' /abs/path" "unsafe path (absolute path): '/abs/path'"
  assert_absent "$out"
  refuse_list "$src" "$out" "printf '%s\\n' docs/../.." "unsafe path ('..' path component): 'docs/../..'"
  assert_absent "$out"
  refuse_list "$src" "$out" "printf '%s\\n' ./docs" "unsafe path ('.' path component): './docs'"
  assert_absent "$out"
  refuse_list "$src" "$out" "printf '%s\\n' docs//x" "unsafe path (empty path component): 'docs//x'"
  assert_absent "$out"
  # CR LF output: '.claude<CR>' must not pass as a path that strips nothing and keeps .claude/.
  refuse_list "$src" "$out" "printf '%s\\r\\n' CLAUDE.md .claude/" "unsafe path (carriage return in path)"
  assert_absent "$out"
  refuse_list "$src" "$out" "printf '\\n'" "exited 0 but printed no path"
  assert_absent "$out"
  # An existing empty out-dir stays, and stays empty.
  mkdir -p "$empty"
  refuse_list "$src" "$empty" "printf '%s\\r\\n' .claude/" "unsafe path (carriage return in path)"
  [ -d "$empty" ] || die "the existing out-dir was removed"
  assert_eq "$(ls -A "$empty")" "" "existing out-dir after a refusal"
}

case_sim_refuses_non_empty_out_dir() {
  shared_source
  write_file "$CASE_TMP/out/existing.txt" 'x'
  expect_fail "$CASE_TMP/run.txt" bash "$SIM" --repo "$SOURCE" src-tag "$CASE_TMP/out"
  assert_grep "$CASE_TMP/run.txt" "not empty"
  assert_file "$CASE_TMP/out/existing.txt"
}

case_sim_into_an_existing_empty_out_dir() {
  local empty="$CASE_TMP/empty" new="$CASE_TMP/new"
  shared_source
  mkdir -p "$empty"
  ls -di "$empty" > "$CASE_TMP/inode-before.txt"
  expect_ok "$CASE_TMP/run1.txt" bash "$SIM" --repo "$SOURCE" src-tag "$empty"
  ls -di "$empty" > "$CASE_TMP/inode-after.txt"
  diff "$CASE_TMP/inode-before.txt" "$CASE_TMP/inode-after.txt" || die "the existing out-dir was replaced"
  expect_ok "$CASE_TMP/run2.txt" bash "$SIM" --repo "$SOURCE" src-tag "$new"
  assert_eq "$(commit_count "$empty")" 1 "commit count"
  assert_eq "$(git -C "$empty" rev-parse HEAD)" "$(git -C "$new" rev-parse HEAD)" "commit in the existing out-dir"
  assert_eq "$(git -C "$empty" status --porcelain --untracked-files=all)" "" "status of the existing out-dir"
  assert_file "$empty/README.md"
}

case_sim_refuses_bad_arguments() {
  local plain="$CASE_TMP/plain" file="$CASE_TMP/out-file" link="$CASE_TMP/out-link" linked="$CASE_TMP/linked"
  shared_source
  mkdir -p "$plain"
  expect_fail "$CASE_TMP/run1.txt" bash "$SIM" --repo "$plain" src-tag "$CASE_TMP/out1"
  assert_grep "$CASE_TMP/run1.txt" "simulate-level-0: error: not a git work tree: $plain"
  assert_absent "$CASE_TMP/out1"
  assert_eq "$(ls -A "$plain")" "" "plain folder"
  write_file "$file" 'a file'
  expect_fail "$CASE_TMP/run2.txt" bash "$SIM" --repo "$SOURCE" src-tag "$file"
  assert_grep "$CASE_TMP/run2.txt" "out-dir exists and is not a folder: $file"
  assert_eq "$(cat "$file")" 'a file' "the file named as out-dir"
  mkdir -p "$linked"
  ln -s "$linked" "$link"
  expect_fail "$CASE_TMP/run3.txt" bash "$SIM" --repo "$SOURCE" src-tag "$link"
  assert_grep "$CASE_TMP/run3.txt" "out-dir exists and is not a folder: $link"
  assert_eq "$(ls -A "$linked")" "" "folder behind the link"
  expect_fail "$CASE_TMP/run4.txt" bash "$SIM" --repo "$SOURCE" src-tag "$CASE_TMP/missing/out"
  assert_grep "$CASE_TMP/run4.txt" "parent of out-dir does not exist: $CASE_TMP/missing/out"
  assert_absent "$CASE_TMP/missing"
  expect_fail "$CASE_TMP/run5.txt" bash "$SIM" --repo "$SOURCE" src-tag
  assert_grep "$CASE_TMP/run5.txt" "usage: simulate-level-0"
}

case_sim_refuses_out_dir_inside_source() {
  local src="$CASE_TMP/src"
  make_source "$src"
  ref_state "$src" > "$CASE_TMP/before.txt"
  expect_fail "$CASE_TMP/run.txt" bash "$SIM" --repo "$src" HEAD "$src/out"
  assert_grep "$CASE_TMP/run.txt" "outside the source repository"
  assert_absent "$src/out"
  ref_state "$src" > "$CASE_TMP/after.txt"
  diff "$CASE_TMP/before.txt" "$CASE_TMP/after.txt" || die "source repo changed"
}

case_sim_refuses_unknown_ref() {
  shared_source
  expect_fail "$CASE_TMP/run.txt" bash "$SIM" --repo "$SOURCE" no-such-ref "$CASE_TMP/out"
  assert_grep "$CASE_TMP/run.txt" "unknown ref: no-such-ref"
  assert_absent "$CASE_TMP/out"
}

case_sim_leaves_source_refs_untouched() {
  shared_source
  ref_state "$SOURCE" > "$CASE_TMP/after.txt"
  diff "$TMP_ROOT/source-state-before.txt" "$CASE_TMP/after.txt" || die "source repo refs, worktrees, tags or status changed"
}

# ---------------------------------------------------------------------------------------------------------------
# apply-level.sh
# ---------------------------------------------------------------------------------------------------------------

HEADER_ROW='# op|group|staged|target|mode|base'

# new_stage <dir>: a staging root with a copy of the scripts under test and no step folders.
new_stage() {
  mkdir -p "$1"
  cp -R "$S_DIR/scripts" "$1/scripts"
}

# manifest <stage> <step> <row...>: writes <step>/manifest.tsv; '|' in a row becomes a tab. Line 1 is a comment.
manifest() {
  local stage=$1 step=$2 r
  shift 2
  mkdir -p "$stage/$step"
  : > "$stage/$step/manifest.tsv"
  for r in "$HEADER_ROW" "$@"; do
    printf '%s\n' "$r" | tr '|' '\t' >> "$stage/$step/manifest.tsv"
  done
}

# staged <stage> <step> <path> <content...>
staged() {
  local stage=$1 step=$2 p=$3
  shift 3
  write_file "$stage/$step/$p" "$@"
}

# new_target <dir>: a small committed repo with existing.txt and bookstore/Foo.java at the patch base.
new_target() {
  local d=$1
  mkdir -p "$d/bookstore"
  git -C "$d" init -q
  write_file "$d/README.md" 'target readme'
  write_file "$d/existing.txt" 'existing content'
  foo_base_lines > "$d/bookstore/Foo.java"
  git -C "$d" add --all
  git -C "$d" commit -q -m "target base"
}

FOO_BASE_SHA=""
# foo_patch <out>: a git diff of bookstore/Foo.java that changes line 02; sets FOO_BASE_SHA.
foo_patch() {
  local r="$CASE_TMP/patch-repo"
  rm -rf "$r"
  mkdir -p "$r/bookstore"
  git -C "$r" init -q
  foo_base_lines > "$r/bookstore/Foo.java"
  git -C "$r" add --all
  git -C "$r" commit -q -m base
  sed 's/^line 02$/line 02 changed by the step/' "$r/bookstore/Foo.java" > "$r/foo.tmp"
  mv "$r/foo.tmp" "$r/bookstore/Foo.java"
  mkdir -p "$(dirname "$1")"
  git -C "$r" diff > "$1"
  FOO_BASE_SHA=$(git -C "$r" rev-parse HEAD:bookstore/Foo.java)
}

# commit_foo_variant <target> <sed expression>: commits an edited Foo.java on top of the base.
commit_foo_variant() {
  sed "$2" "$1/bookstore/Foo.java" > "$1/foo.tmp"
  mv "$1/foo.tmp" "$1/bookstore/Foo.java"
  git -C "$1" commit -q -am "edit Foo.java"
}

APPLY="scripts/apply-level.sh"

case_apply_add_decodes_paths_and_leaves_changes_uncommitted() {
  local st="$CASE_TMP/stage" t="$CASE_TMP/target" head
  new_stage "$st"
  staged "$st" level-1 files/dot-claude/rules/domain.md 'domain rules'
  staged "$st" level-1 files/CLAUDE.staged.md 'project memory'
  staged "$st" level-1 files/docs/dot-config/dot-hidden.txt 'nested dots'
  manifest "$st" level-1 \
    'add|config|files/dot-claude/rules/domain.md|.claude/rules/domain.md|644|-' \
    'add|config|files/CLAUDE.staged.md|CLAUDE.md|644|-' \
    'add|config|files/docs/dot-config/dot-hidden.txt|docs/.config/.hidden.txt|644|-'
  new_target "$t"
  head=$(git -C "$t" rev-parse HEAD)
  expect_ok "$CASE_TMP/run.txt" bash "$st/$APPLY" level-1 "$t"
  assert_eq "$(cat "$t/.claude/rules/domain.md")" 'domain rules' ".claude/rules/domain.md"
  assert_eq "$(cat "$t/CLAUDE.md")" 'project memory' "CLAUDE.md"
  assert_eq "$(cat "$t/docs/.config/.hidden.txt")" 'nested dots' "docs/.config/.hidden.txt"
  assert_absent "$t/dot-claude"
  assert_absent "$t/files"
  assert_absent "$t/CLAUDE.staged.md"
  assert_eq "$(git -C "$t" rev-parse HEAD)" "$head" "HEAD after apply"
  assert_eq "$(commit_count "$t")" 1 "commit count after apply"
  git -C "$t" status --porcelain --untracked-files=all > "$CASE_TMP/status.txt"
  assert_grep "$CASE_TMP/status.txt" "?? .claude/rules/domain.md"
  assert_grep "$CASE_TMP/status.txt" "?? CLAUDE.md"
  assert_grep "$CASE_TMP/status.txt" "?? docs/.config/.hidden.txt"
  assert_grep "$CASE_TMP/run.txt" "uncommitted"
}

case_apply_replace_overwrites_an_existing_target() {
  local st="$CASE_TMP/stage" t="$CASE_TMP/target"
  new_stage "$st"
  staged "$st" level-2 files/existing.txt 'replaced content'
  manifest "$st" level-2 'replace|config|files/existing.txt|existing.txt|644|-'
  new_target "$t"
  expect_ok "$CASE_TMP/run.txt" bash "$st/$APPLY" level-2 "$t"
  assert_eq "$(cat "$t/existing.txt")" 'replaced content' "existing.txt"
  assert_eq "$(git -C "$t" status --porcelain)" " M existing.txt" "status"
}

case_apply_sets_modes_from_the_manifest() {
  local st="$CASE_TMP/stage" t="$CASE_TMP/target"
  new_stage "$st"
  staged "$st" level-3 files/dot-claude/hooks/check.sh '#!/bin/sh' 'exit 0'
  chmod 644 "$st/level-3/files/dot-claude/hooks/check.sh"
  staged "$st" level-3 files/notes.txt 'plain'
  chmod 755 "$st/level-3/files/notes.txt"
  staged "$st" level-3 files/existing.txt '#!/bin/sh' 'echo now executable'
  manifest "$st" level-3 \
    'add|config|files/dot-claude/hooks/check.sh|.claude/hooks/check.sh|755|-' \
    'add|config|files/notes.txt|notes.txt|644|-' \
    'replace|config|files/existing.txt|existing.txt|755|-'
  new_target "$t"
  expect_ok "$CASE_TMP/run.txt" bash "$st/$APPLY" level-3 "$t"
  [ -x "$t/.claude/hooks/check.sh" ] || die ".claude/hooks/check.sh is not executable"
  [ ! -x "$t/notes.txt" ] || die "notes.txt is executable"
  [ -x "$t/existing.txt" ] || die "existing.txt is not executable"
  git -C "$t" add --all
  assert_eq "$(git -C "$t" ls-files --stage .claude/hooks/check.sh | cut -c1-6)" 100755 "index mode of check.sh"
  assert_eq "$(git -C "$t" ls-files --stage notes.txt | cut -c1-6)" 100644 "index mode of notes.txt"
  assert_eq "$(git -C "$t" ls-files --stage existing.txt | cut -c1-6)" 100755 "index mode of existing.txt"
}

case_apply_group_filter() {
  local st="$CASE_TMP/stage" g
  new_stage "$st"
  staged "$st" level-3 files/config.txt 'config row'
  staged "$st" level-3 files/exercise.txt 'exercise row'
  manifest "$st" level-3 \
    'add|config|files/config.txt|config.txt|644|-' \
    'add|exercise|files/exercise.txt|exercise.txt|644|-'
  new_target "$CASE_TMP/config"
  expect_ok "$CASE_TMP/run1.txt" bash "$st/$APPLY" --group config level-3 "$CASE_TMP/config"
  assert_file "$CASE_TMP/config/config.txt"
  assert_absent "$CASE_TMP/config/exercise.txt"
  new_target "$CASE_TMP/exercise"
  expect_ok "$CASE_TMP/run2.txt" bash "$st/$APPLY" --group exercise level-3 "$CASE_TMP/exercise"
  assert_file "$CASE_TMP/exercise/exercise.txt"
  assert_absent "$CASE_TMP/exercise/config.txt"
  new_target "$CASE_TMP/all"
  expect_ok "$CASE_TMP/run3.txt" bash "$st/$APPLY" level-3 "$CASE_TMP/all"
  assert_file "$CASE_TMP/all/exercise.txt"
  assert_file "$CASE_TMP/all/config.txt"
  for g in docs CONFIG ''; do
    new_target "$CASE_TMP/bad-$g"
    expect_fail "$CASE_TMP/run4.txt" bash "$st/$APPLY" --group "$g" level-3 "$CASE_TMP/bad-$g"
    assert_absent "$CASE_TMP/bad-$g/config.txt"
    assert_absent "$CASE_TMP/bad-$g/exercise.txt"
  done
}

case_apply_group_without_rows_applies_nothing() {
  local st="$CASE_TMP/stage" t="$CASE_TMP/target"
  new_stage "$st"
  staged "$st" level-1 files/config.txt 'config row'
  manifest "$st" level-1 'add|config|files/config.txt|config.txt|644|-'
  new_target "$t"
  tree_state "$t" > "$CASE_TMP/before.txt"
  expect_ok "$CASE_TMP/run.txt" bash "$st/$APPLY" --group exercise level-1 "$t"
  assert_grep "$CASE_TMP/run.txt" "apply-level: level-1: no rows in group exercise; nothing applied"
  tree_state "$t" > "$CASE_TMP/after.txt"
  diff "$CASE_TMP/before.txt" "$CASE_TMP/after.txt" || die "target changed by an apply without rows"
}

# refuse_case <step> <expected line> <expected reason> <row...>: the step must be refused, naming the manifest line
# and the reason, with the target tree unchanged. Line 2 is always a valid add row, so the case also proves that
# nothing is written before every row is checked.
# Env: REFUSE_STAGED    the second staged file (default files/x.md)
#      PRE_TARGET_FILE  a file committed on the target first
#      PRE_TARGET_HOOK  a function called with the target folder; what it creates is committed first
refuse_case() {
  local step=$1 line=$2 reason=$3 st="$CASE_TMP/stage" t="$CASE_TMP/target"
  shift 3
  rm -rf "$st" "$t"
  new_stage "$st"
  staged "$st" "$step" files/valid.txt 'valid'
  staged "$st" "$step" "${REFUSE_STAGED:-files/x.md}" 'x'
  manifest "$st" "$step" 'add|config|files/valid.txt|valid.txt|644|-' "$@"
  new_target "$t"
  if [ -n "${PRE_TARGET_FILE:-}" ]; then
    write_file "$t/$PRE_TARGET_FILE" 'already on the target'
    git -C "$t" add --all
    git -C "$t" commit -q -m "pre-existing $PRE_TARGET_FILE"
  fi
  if [ -n "${PRE_TARGET_HOOK:-}" ]; then
    "$PRE_TARGET_HOOK" "$t"
    git -C "$t" add --all
    git -C "$t" commit -q -m "pre-existing state"
  fi
  tree_state "$t" > "$CASE_TMP/before.txt"
  expect_fail "$CASE_TMP/run.txt" bash "$st/$APPLY" "$step" "$t"
  if [ -n "$line" ]; then
    assert_grep "$CASE_TMP/run.txt" "$step/manifest.tsv line $line:"
  fi
  assert_grep "$CASE_TMP/run.txt" "$reason"
  tree_state "$t" > "$CASE_TMP/after.txt"
  diff "$CASE_TMP/before.txt" "$CASE_TMP/after.txt" || die "target changed by a refused apply"
}

case_apply_add_refuses_an_existing_target() {
  PRE_TARGET_FILE=x.md refuse_case level-1 3 "add: target exists: x.md" 'add|config|files/x.md|x.md|644|-'
}

case_apply_replace_refuses_a_missing_target() {
  refuse_case level-1 3 "replace: target does not exist: x.md" 'replace|config|files/x.md|x.md|644|-'
}

case_apply_refuses_rule_breaking_staged_paths() {
  refuse_case level-1 3 "starts with a dot" 'add|config|files/.claude/x.md|.claude/x.md|644|-'
  refuse_case level-1 3 "must not be named CLAUDE.md" 'add|config|files/CLAUDE.md|CLAUDE.md|644|-'
  refuse_case level-1 3 "must not be named CLAUDE.local.md" 'add|config|files/CLAUDE.local.md|CLAUDE.local.md|644|-'
  refuse_case level-1 3 "must not be named AGENTS.md" 'add|config|files/AGENTS.md|AGENTS.md|644|-'
  refuse_case level-1 3 "must not be named CLAUDE.md" 'add|config|files/dot-claude/CLAUDE.md|.claude/CLAUDE.md|644|-'
  refuse_case level-1 3 "'..' path component" 'add|config|files/a/../x.md|x.md|644|-'
  refuse_case level-1 3 "does not decode to the target" 'add|config|files/x.md|y.md|644|-'
  refuse_case level-1 3 "does not decode to the target" 'add|config|files/dot-claude/x.md|claude/x.md|644|-'
  refuse_case level-1 3 "must start with files/" 'add|config|x.md|x.md|644|-'
  refuse_case level-1 3 "must start with patches/" 'patch|config|files/x.md|x.md|644|0123456789012345678901234567890123456789'
}

case_apply_refuses_unsafe_targets() {
  refuse_case level-1 3 "absolute path" 'add|config|files/x.md|/tmp/x.md|644|-'
  refuse_case level-1 3 "'..' path component" 'add|config|files/x.md|../x.md|644|-'
  refuse_case level-1 3 "'..' path component" 'add|config|files/x.md|docs/../../x.md|644|-'
  refuse_case level-1 3 "'.git' path component" 'add|config|files/dot-git/hooks/x.md|.git/hooks/x.md|644|-'
  refuse_case level-1 3 "empty path component" 'add|config|files/x.md|docs//x.md|644|-'
}

# link_to_existing <target>: link.txt, a symbolic link to existing.txt.
link_to_existing() {
  ln -s existing.txt "$1/link.txt"
}

case_apply_refuses_targets_that_are_not_regular_files() {
  local hex=0123456789012345678901234567890123456789
  REFUSE_STAGED=files/existing.txt/x.md refuse_case level-1 3 "target folder existing.txt is not a folder" \
    'add|config|files/existing.txt/x.md|existing.txt/x.md|644|-'
  REFUSE_STAGED=files/existing.txt/sub/x.md refuse_case level-1 3 "target folder existing.txt is not a folder" \
    'add|config|files/existing.txt/sub/x.md|existing.txt/sub/x.md|644|-'
  REFUSE_STAGED=files/bookstore refuse_case level-1 3 "replace: target is not a regular file: bookstore" \
    'replace|config|files/bookstore|bookstore|644|-'
  REFUSE_STAGED=patches/bookstore.patch refuse_case level-2 3 "patch: target is not a regular file: bookstore" \
    "patch|exercise|patches/bookstore.patch|bookstore|644|$hex"
  PRE_TARGET_HOOK=link_to_existing REFUSE_STAGED=files/link.txt refuse_case level-1 3 \
    "replace: target is not a regular file: link.txt" 'replace|config|files/link.txt|link.txt|644|-'
  assert_eq "$(cat "$CASE_TMP/target/existing.txt")" 'existing content' "existing.txt behind the link"
}

case_apply_refuses_malformed_rows() {
  refuse_case level-1 3 "unknown op 'copy'" 'copy|config|files/x.md|x.md|644|-'
  refuse_case level-1 3 "unknown group 'docs'" 'add|docs|files/x.md|x.md|644|-'
  refuse_case level-1 3 "mode must be 644 or 755" 'add|config|files/x.md|x.md|600|-'
  refuse_case level-1 3 "base must be '-'" 'add|config|files/x.md|x.md|644|abc'
  refuse_case level-1 3 "base must be a 40-hex blob sha" 'patch|exercise|patches/x.md.patch|x.md|644|-'
  refuse_case level-1 3 "expected 6 tab-separated fields" 'add|config|files/x.md|x.md|644'
  refuse_case level-1 3 "expected 6 tab-separated fields" 'add|config|files/x.md|x.md|644|-|extra'
  refuse_case level-1 3 "empty field" 'add|config||files/x.md|x.md|644'
  refuse_case level-1 3 "staged file is missing" 'add|config|files/missing.md|missing.md|644|-'
  refuse_case level-1 4 "duplicate target" 'add|config|files/x.md|x.md|644|-' 'replace|config|files/x.md|x.md|644|-'
  refuse_case level-1 3 "carriage return" "add|config|files/x.md|x.md|644|-$CR"
}

case_apply_refuses_unlisted_staged_files() {
  local st="$CASE_TMP/stage" t="$CASE_TMP/target"
  new_stage "$st"
  staged "$st" level-1 files/listed.txt 'listed'
  staged "$st" level-1 files/dot-claude/unlisted.md 'unlisted'
  manifest "$st" level-1 'add|config|files/listed.txt|listed.txt|644|-'
  new_target "$t"
  tree_state "$t" > "$CASE_TMP/before.txt"
  expect_fail "$CASE_TMP/run.txt" bash "$st/$APPLY" level-1 "$t"
  assert_grep "$CASE_TMP/run.txt" "files/dot-claude/unlisted.md is not named by any manifest row"
  tree_state "$t" > "$CASE_TMP/after.txt"
  diff "$CASE_TMP/before.txt" "$CASE_TMP/after.txt" || die "target changed"
}

case_apply_refuses_a_symlinked_target_folder() {
  local st="$CASE_TMP/stage" t="$CASE_TMP/target"
  new_stage "$st"
  staged "$st" level-1 files/link/x.md 'through a link'
  manifest "$st" level-1 'add|config|files/link/x.md|link/x.md|644|-'
  new_target "$t"
  mkdir -p "$CASE_TMP/elsewhere"
  ln -s "$CASE_TMP/elsewhere" "$t/link"
  expect_fail "$CASE_TMP/run.txt" bash "$st/$APPLY" level-1 "$t"
  assert_grep "$CASE_TMP/run.txt" "level-1/manifest.tsv line 2:"
  assert_grep "$CASE_TMP/run.txt" "symbolic link"
  assert_absent "$CASE_TMP/elsewhere/x.md"
}

case_apply_refuses_bad_arguments() {
  local st="$CASE_TMP/stage" t="$CASE_TMP/target"
  new_stage "$st"
  staged "$st" level-1 files/x.md 'x'
  manifest "$st" level-1 'add|config|files/x.md|x.md|644|-'
  new_target "$t"
  mkdir -p "$t/sub"
  expect_fail "$CASE_TMP/run1.txt" bash "$st/$APPLY" level-9 "$t"
  assert_grep "$CASE_TMP/run1.txt" "unknown step: level-9"
  expect_fail "$CASE_TMP/run2.txt" bash "$st/$APPLY" level-1 "$t/sub"
  assert_grep "$CASE_TMP/run2.txt" "top level"
  mkdir -p "$CASE_TMP/plain"
  expect_fail "$CASE_TMP/run3.txt" bash "$st/$APPLY" level-1 "$CASE_TMP/plain"
  assert_grep "$CASE_TMP/run3.txt" "not a git work tree"
  expect_fail "$CASE_TMP/run4.txt" bash "$st/$APPLY" level-2 "$t"
  assert_grep "$CASE_TMP/run4.txt" "level-2/manifest.tsv is missing"
  expect_fail "$CASE_TMP/run5.txt" bash "$st/$APPLY" level-1 "$CASE_TMP/missing"
  assert_grep "$CASE_TMP/run5.txt" "target is not a folder"
  expect_fail "$CASE_TMP/run6.txt" bash "$st/$APPLY" level-1
  assert_grep "$CASE_TMP/run6.txt" "usage: apply-level"
  assert_absent "$t/x.md"
  assert_absent "$t/sub/x.md"
  assert_absent "$CASE_TMP/missing"
}

case_apply_patch_on_its_base() {
  local st="$CASE_TMP/stage" t="$CASE_TMP/target"
  new_stage "$st"
  foo_patch "$st/level-2/patches/bookstore/Foo.java.patch"
  manifest "$st" level-2 "patch|exercise|patches/bookstore/Foo.java.patch|bookstore/Foo.java|644|$FOO_BASE_SHA"
  new_target "$t"
  expect_ok "$CASE_TMP/run.txt" bash "$st/$APPLY" level-2 "$t"
  assert_eq "$(sed -n 2p "$t/bookstore/Foo.java")" 'line 02 changed by the step' "patched line"
  assert_eq "$(commit_count "$t")" 1 "commit count"
  git -C "$t" status --porcelain > "$CASE_TMP/status.txt"
  assert_grep "$CASE_TMP/status.txt" "bookstore/Foo.java"
}

case_apply_patch_three_way_when_the_target_moved_on() {
  local st="$CASE_TMP/stage" t="$CASE_TMP/target"
  new_stage "$st"
  foo_patch "$st/level-2/patches/bookstore/Foo.java.patch"
  manifest "$st" level-2 "patch|exercise|patches/bookstore/Foo.java.patch|bookstore/Foo.java|644|$FOO_BASE_SHA"
  new_target "$t"
  # Line 05 is context of the hunk, so a plain apply fails; the change is two lines away, so 3-way merges cleanly.
  commit_foo_variant "$t" 's/^line 05$/line 05 changed on the target/'
  git -C "$t" apply --check "$st/level-2/patches/bookstore/Foo.java.patch" 2> /dev/null && die "fixture: plain apply should fail"
  expect_ok "$CASE_TMP/run.txt" bash "$st/$APPLY" level-2 "$t"
  assert_eq "$(sed -n 2p "$t/bookstore/Foo.java")" 'line 02 changed by the step' "patched line"
  assert_eq "$(sed -n 5p "$t/bookstore/Foo.java")" 'line 05 changed on the target' "target line kept"
  assert_no_grep "$t/bookstore/Foo.java" "<<<<<<<"
  assert_eq "$(commit_count "$t")" 2 "commit count"
}

case_apply_patch_refuses_a_missing_base_blob() {
  local st="$CASE_TMP/stage" t="$CASE_TMP/target"
  new_stage "$st"
  foo_patch "$st/level-2/patches/bookstore/Foo.java.patch"
  manifest "$st" level-2 "patch|exercise|patches/bookstore/Foo.java.patch|bookstore/Foo.java|644|$FOO_BASE_SHA"
  # A target that never held the base blob, edited far from the hunk: a plain apply would succeed.
  mkdir -p "$t/bookstore"
  git -C "$t" init -q
  foo_base_lines | sed 's/^line 12$/line 12 differs/' > "$t/bookstore/Foo.java"
  git -C "$t" add --all
  git -C "$t" commit -q -m "no base blob"
  git -C "$t" cat-file -e "$FOO_BASE_SHA" 2> /dev/null && die "fixture: base blob should be absent"
  git -C "$t" apply --check "$st/level-2/patches/bookstore/Foo.java.patch" || die "fixture: plain apply should work"
  tree_state "$t" > "$CASE_TMP/before.txt"
  expect_fail "$CASE_TMP/run.txt" bash "$st/$APPLY" level-2 "$t"
  assert_grep "$CASE_TMP/run.txt" "level-2/manifest.tsv line 2:"
  assert_grep "$CASE_TMP/run.txt" "base blob $FOO_BASE_SHA is missing"
  tree_state "$t" > "$CASE_TMP/after.txt"
  diff "$CASE_TMP/before.txt" "$CASE_TMP/after.txt" || die "target changed"
}

case_apply_patch_refuses_a_conflict_atomically() {
  local st="$CASE_TMP/stage" t="$CASE_TMP/target"
  new_stage "$st"
  foo_patch "$st/level-2/patches/bookstore/Foo.java.patch"
  staged "$st" level-2 files/new-folder/added.txt 'added before the patch'
  staged "$st" level-2 files/existing.txt 'replaced before the patch'
  staged "$st" level-2 files/later.txt 'after the patch'
  manifest "$st" level-2 \
    'add|config|files/new-folder/added.txt|new-folder/added.txt|755|-' \
    'replace|config|files/existing.txt|existing.txt|755|-' \
    "patch|exercise|patches/bookstore/Foo.java.patch|bookstore/Foo.java|644|$FOO_BASE_SHA" \
    'add|config|files/later.txt|later.txt|644|-'
  new_target "$t"
  commit_foo_variant "$t" 's/^line 02$/line 02 changed on the target/'
  tree_state "$t" > "$CASE_TMP/before.txt"
  expect_fail "$CASE_TMP/run.txt" bash "$st/$APPLY" level-2 "$t"
  assert_grep "$CASE_TMP/run.txt" "level-2/manifest.tsv line 4:"
  assert_grep "$CASE_TMP/run.txt" "not clean"
  tree_state "$t" > "$CASE_TMP/after.txt"
  diff "$CASE_TMP/before.txt" "$CASE_TMP/after.txt" || die "target changed by a refused apply"
  assert_eq "$(git -C "$t" ls-files --unmerged)" "" "unmerged index entries"
}

case_apply_patch_refuses_a_patch_of_another_path() {
  local st="$CASE_TMP/stage" t="$CASE_TMP/target"
  new_stage "$st"
  foo_patch "$st/level-2/patches/README.md.patch"
  manifest "$st" level-2 "patch|exercise|patches/README.md.patch|README.md|644|$FOO_BASE_SHA"
  new_target "$t"
  tree_state "$t" > "$CASE_TMP/before.txt"
  expect_fail "$CASE_TMP/run.txt" bash "$st/$APPLY" level-2 "$t"
  assert_grep "$CASE_TMP/run.txt" "level-2/manifest.tsv line 2:"
  assert_grep "$CASE_TMP/run.txt" "patch touches bookstore/Foo.java, not README.md"
  tree_state "$t" > "$CASE_TMP/after.txt"
  diff "$CASE_TMP/before.txt" "$CASE_TMP/after.txt" || die "target changed"
}

# refuse_patch <patch file> <base> <expected reason>: a level-2 step with a valid add row on line 2 and a patch row
# of bookstore/Foo.java on line 3 must be refused, naming line 3 and the reason, with the target tree (at the patch
# base) unchanged.
refuse_patch() {
  local patch=$1 base=$2 reason=$3 st="$CASE_TMP/stage" t="$CASE_TMP/target"
  rm -rf "$st" "$t"
  new_stage "$st"
  staged "$st" level-2 files/valid.txt 'valid'
  mkdir -p "$st/level-2/patches/bookstore"
  cp "$patch" "$st/level-2/patches/bookstore/Foo.java.patch"
  manifest "$st" level-2 'add|config|files/valid.txt|valid.txt|644|-' \
    "patch|exercise|patches/bookstore/Foo.java.patch|bookstore/Foo.java|644|$base"
  new_target "$t"
  tree_state "$t" > "$CASE_TMP/before.txt"
  expect_fail "$CASE_TMP/run.txt" bash "$st/$APPLY" level-2 "$t"
  assert_grep "$CASE_TMP/run.txt" "level-2/manifest.tsv line 3:"
  assert_grep "$CASE_TMP/run.txt" "$reason"
  tree_state "$t" > "$CASE_TMP/after.txt"
  diff "$CASE_TMP/before.txt" "$CASE_TMP/after.txt" || die "target changed by a refused patch"
}

case_apply_patch_refuses_a_base_that_does_not_match_the_patch() {
  local other
  foo_patch "$CASE_TMP/patches/good.patch"
  other=$(printf 'other\n' | git hash-object --stdin)
  refuse_patch "$CASE_TMP/patches/good.patch" "$other" "base sha $other does not match the patch index line"
}

case_apply_patch_refuses_patches_it_cannot_trust() {
  local p="$CASE_TMP/patches" d="$CASE_TMP/plain-diff"
  foo_patch "$p/good.patch"
  # A git diff with its index line removed.
  sed '/^index /d' "$p/good.patch" > "$p/no-index.patch"
  assert_no_grep "$p/no-index.patch" "index "
  refuse_patch "$p/no-index.patch" "$FOO_BASE_SHA" "patch has no index line"
  # A plain diff -u, which never has an index line.
  mkdir -p "$d/a/bookstore" "$d/b/bookstore"
  foo_base_lines > "$d/a/bookstore/Foo.java"
  sed 's/^line 02$/line 02 changed by the step/' "$d/a/bookstore/Foo.java" > "$d/b/bookstore/Foo.java"
  (cd "$d" && diff -u a/bookstore/Foo.java b/bookstore/Foo.java > "$p/diff-u.patch") || true
  assert_grep "$p/diff-u.patch" "+line 02 changed by the step"
  refuse_patch "$p/diff-u.patch" "$FOO_BASE_SHA" "patch has no index line"
  # The index line names the base, the target holds the base, but a context line was edited by hand.
  sed 's/^ line 04$/ line 04 edited by hand/' "$p/good.patch" > "$p/hand-edited.patch"
  assert_grep "$p/hand-edited.patch" " line 04 edited by hand"
  refuse_patch "$p/hand-edited.patch" "$FOO_BASE_SHA" "patch does not apply to its own base blob"
  # Not a patch at all.
  write_file "$p/garbage.patch" 'this is not a patch'
  refuse_patch "$p/garbage.patch" "$FOO_BASE_SHA" "not a valid patch: patches/bookstore/Foo.java.patch"
}

# ---------------------------------------------------------------------------------------------------------------
# build-level.sh
# ---------------------------------------------------------------------------------------------------------------

BUILD="scripts/build-level.sh"
STEPS='level-1 level-2 level-3 level-4-openspec level-4-speckit level-4-agentos'

# The fixed commit messages, written out here on purpose (not read from the scripts).
expected_message() {
  case $1 in
    level-1) echo 'Level 1 solution: context engineering' ;;
    level-2) echo 'Level 2 solution: give your agent eyes (LSP + MCP)' ;;
    level-3) echo 'Level 3 solution: behavioral control' ;;
    level-4-openspec) echo 'Level 4 start: OpenSpec installed and initialised' ;;
    level-4-speckit) echo 'Level 4 start: Spec Kit installed and initialised' ;;
    level-4-agentos) echo 'Level 4 start: Agent OS installed and initialised' ;;
  esac
}

expected_branch() {
  case $1 in
    level-1|level-2|level-3) echo "solution/$1" ;;
    *) echo "start/$1" ;;
  esac
}

expected_chain() {
  case $1 in
    level-1) echo 'level-1' ;;
    level-2) echo 'level-1 level-2' ;;
    level-3) echo 'level-1 level-2 level-3' ;;
    *) echo "level-1 level-2 level-3 $1" ;;
  esac
}

# chain_stage <stage>: six fixture steps (never real level content) that build on a simulated level-0 of make_source.
chain_stage() {
  local st=$1
  new_stage "$st"
  staged "$st" level-1 files/CLAUDE.staged.md 'fixture project memory'
  staged "$st" level-1 files/dot-claude/rules/domain.md 'fixture rule'
  staged "$st" level-1 files/CLAUDE.local.md.example 'fixture local example'
  manifest "$st" level-1 \
    'add|config|files/CLAUDE.staged.md|CLAUDE.md|644|-' \
    'add|config|files/dot-claude/rules/domain.md|.claude/rules/domain.md|644|-' \
    'add|config|files/CLAUDE.local.md.example|CLAUDE.local.md.example|644|-'
  staged "$st" level-2 files/dot-mcp.json '{}'
  staged "$st" level-2 files/dot-claude/settings.json '{ "level": 2 }'
  foo_patch "$st/level-2/patches/bookstore/Foo.java.patch"
  manifest "$st" level-2 \
    'add|config|files/dot-mcp.json|.mcp.json|644|-' \
    'add|config|files/dot-claude/settings.json|.claude/settings.json|644|-' \
    "patch|exercise|patches/bookstore/Foo.java.patch|bookstore/Foo.java|644|$FOO_BASE_SHA"
  staged "$st" level-3 files/dot-claude/settings.json '{ "level": 3 }'
  staged "$st" level-3 files/dot-claude/hooks/check.sh '#!/bin/sh' 'exit 0'
  mkdir -p "$st/level-3/files/dot-claude/hooks"
  printf 'Write-Output "check"\r\nexit 0\r\n' > "$st/level-3/files/dot-claude/hooks/check.ps1"
  staged "$st" level-3 files/dot-githooks/pre-commit '#!/bin/sh' 'echo pre-commit'
  staged "$st" level-3 files/bookstore/Cancel.java 'class Cancel {}'
  manifest "$st" level-3 \
    'replace|config|files/dot-claude/settings.json|.claude/settings.json|644|-' \
    'add|config|files/dot-claude/hooks/check.sh|.claude/hooks/check.sh|755|-' \
    'add|config|files/dot-claude/hooks/check.ps1|.claude/hooks/check.ps1|644|-' \
    'add|config|files/dot-githooks/pre-commit|.githooks/pre-commit|755|-' \
    'add|exercise|files/bookstore/Cancel.java|bookstore/Cancel.java|644|-'
  staged "$st" level-4-openspec files/openspec/config.yaml 'schema: fixture'
  manifest "$st" level-4-openspec 'add|config|files/openspec/config.yaml|openspec/config.yaml|644|-'
  staged "$st" level-4-speckit files/dot-specify/memory/constitution.md 'fixture constitution'
  manifest "$st" level-4-speckit \
    'add|config|files/dot-specify/memory/constitution.md|.specify/memory/constitution.md|644|-'
  staged "$st" level-4-agentos files/agent-os/standards/index.yml 'fixture: true'
  manifest "$st" level-4-agentos 'add|config|files/agent-os/standards/index.yml|agent-os/standards/index.yml|644|-'
}

# level0 <out>: a simulated level-0 of the shared source repo.
level0() {
  shared_source
  bash "$SIM" --repo "$SOURCE" src-tag "$1" > /dev/null
}

# expected_name_status <stage> <step>: A for add rows, M for replace and patch rows.
expected_name_status() {
  awk -F '\t' '/^#/ { next } NF { print ($1 == "add" ? "A" : "M") "\t" $4 }' "$1/$2/manifest.tsv" | LC_ALL=C sort
}

case_build_commits_the_chain_one_commit_per_step() {
  local st="$CASE_TMP/stage" step t chain n k s commit
  chain_stage "$st"
  for step in $STEPS; do
    t="$CASE_TMP/build-$step"
    level0 "$t"
    expect_ok "$CASE_TMP/run-$step.txt" bash "$st/$BUILD" "$step" "$t"
    chain=$(expected_chain "$step")
    n=$(echo $chain | wc -w | tr -d ' ')
    assert_eq "$(commit_count "$t")" "$((n + 1))" "commit count for $step"
    assert_eq "$(git -C "$t" status --porcelain --untracked-files=all)" "" "status after building $step"
    k=$n
    for s in $chain; do
      k=$((k - 1))
      commit=$(git -C "$t" rev-parse "HEAD~$k")
      assert_eq "$(git -C "$t" log -1 --format=%s "$commit")" "$(expected_message "$s")" "message of the $s commit"
      git -C "$t" diff --name-status "$commit^" "$commit" | LC_ALL=C sort > "$CASE_TMP/ns.txt"
      expected_name_status "$st" "$s" > "$CASE_TMP/ns-expected.txt"
      diff "$CASE_TMP/ns-expected.txt" "$CASE_TMP/ns.txt" || die "the $s commit does not match its manifest"
    done
  done
  assert_eq "$(git -C "$CASE_TMP/build-level-4-agentos" rev-parse HEAD~4)" \
    "$(git -C "$CASE_TMP/build-level-1" rev-parse HEAD~1)" "level-0 commit under the chain"
  assert_eq "$(sed -n 2p "$CASE_TMP/build-level-2/bookstore/Foo.java")" 'line 02 changed by the step' "patched line"
}

case_build_refuses_a_dirty_target() {
  local st="$CASE_TMP/stage" t="$CASE_TMP/target"
  chain_stage "$st"
  level0 "$t"
  write_file "$t/stray.txt" 'not committed'
  expect_fail "$CASE_TMP/run.txt" bash "$st/$BUILD" level-1 "$t"
  assert_grep "$CASE_TMP/run.txt" "uncommitted changes"
  assert_eq "$(commit_count "$t")" 1 "commit count"
  assert_absent "$t/CLAUDE.md"
}

case_build_refuses_a_git_ignored_target() {
  local st="$CASE_TMP/stage" t="$CASE_TMP/target"
  chain_stage "$st"
  staged "$st" level-1 files/notes/todo.md 'ignored by the target .gitignore'
  printf 'add\tconfig\tfiles/notes/todo.md\tnotes/todo.md\t644\t-\n' >> "$st/level-1/manifest.tsv"
  level0 "$t"
  expect_fail "$CASE_TMP/run.txt" bash "$st/$BUILD" level-1 "$t"
  assert_grep "$CASE_TMP/run.txt" "git-ignored"
  assert_grep "$CASE_TMP/run.txt" "notes/todo.md"
}

case_build_stops_at_a_failing_step_naming_the_line() {
  local st="$CASE_TMP/stage" t="$CASE_TMP/target"
  chain_stage "$st"
  staged "$st" level-2 files/README.md 'collides'
  printf 'add\tconfig\tfiles/README.md\tREADME.md\t644\t-\n' >> "$st/level-2/manifest.tsv"
  level0 "$t"
  expect_fail "$CASE_TMP/run.txt" bash "$st/$BUILD" level-3 "$t"
  assert_grep "$CASE_TMP/run.txt" "level-2/manifest.tsv line 5: add: target exists: README.md"
  assert_eq "$(commit_count "$t")" 2 "commit count (level-0 and level-1 only)"
  assert_eq "$(git -C "$t" log -1 --format=%s)" "$(expected_message level-1)" "last commit"
  assert_eq "$(git -C "$t" status --porcelain --untracked-files=all)" "" "status"
}

case_build_refuses_bad_arguments() {
  local st="$CASE_TMP/stage" t="$CASE_TMP/target"
  chain_stage "$st"
  level0 "$t"
  expect_fail "$CASE_TMP/run1.txt" bash "$st/$BUILD" level-5 "$t"
  assert_grep "$CASE_TMP/run1.txt" "unknown step: level-5"
  expect_fail "$CASE_TMP/run2.txt" bash "$st/$BUILD" level-1 "$CASE_TMP/missing"
  assert_grep "$CASE_TMP/run2.txt" "target is not a folder"
  assert_absent "$CASE_TMP/missing"
  expect_fail "$CASE_TMP/run3.txt" bash "$st/$BUILD" level-1
  assert_grep "$CASE_TMP/run3.txt" "usage: build-level"
  assert_eq "$(commit_count "$t")" 1 "commit count"
}

case_build_refuses_a_target_that_is_not_a_level_0_repo() {
  local st="$CASE_TMP/stage" t="$CASE_TMP/target" e="$CASE_TMP/no-commit" p="$CASE_TMP/plain"
  chain_stage "$st"
  # An empty repository: no commit yet.
  mkdir -p "$e"
  git -C "$e" init -q
  expect_fail "$CASE_TMP/run1.txt" bash "$st/$BUILD" level-1 "$e"
  assert_grep "$CASE_TMP/run1.txt" "build-level: error: target has no commit"
  git -C "$e" rev-parse --verify --quiet HEAD > /dev/null && die "a commit was made in the empty repository"
  assert_eq "$(git -C "$e" status --porcelain --untracked-files=all)" "" "status of the empty repository"
  # A plain folder.
  write_file "$p/kept.txt" 'plain'
  tree_state "$p" > "$CASE_TMP/plain-before.txt"
  expect_fail "$CASE_TMP/run2.txt" bash "$st/$BUILD" level-1 "$p"
  assert_grep "$CASE_TMP/run2.txt" "build-level: error: not a git work tree"
  tree_state "$p" > "$CASE_TMP/plain-after.txt"
  diff "$CASE_TMP/plain-before.txt" "$CASE_TMP/plain-after.txt" || die "plain folder changed"
  # A folder below the top level of a level-0 repository.
  level0 "$t"
  tree_state "$t" > "$CASE_TMP/before.txt"
  expect_fail "$CASE_TMP/run3.txt" bash "$st/$BUILD" level-1 "$t/docs"
  assert_grep "$CASE_TMP/run3.txt" "build-level: error: target must be the top level of its work tree"
  tree_state "$t" > "$CASE_TMP/after.txt"
  diff "$CASE_TMP/before.txt" "$CASE_TMP/after.txt" || die "level-0 repository changed"
}

# ---------------------------------------------------------------------------------------------------------------
# cut-level-branches.sh
# ---------------------------------------------------------------------------------------------------------------

CUT="scripts/cut-level-branches.sh"

# make_workshop <dir>: a level-0 repo tagged level-0-start, with a bare remote "origin", a local commit on main that
# is not pushed yet (so any push would change the remote) and a pre-push hook that records any push attempt.
make_workshop() {
  local d=$1
  level0 "$d"
  git -C "$d" tag level-0-start
  git -C "$d" tag -a -m "annotated" annotated-tag
  git clone -q --bare "$d" "$d-remote.git"
  git -C "$d" remote add origin "$d-remote.git"
  git -C "$d" fetch -q origin
  git -C "$d" branch -q --set-upstream-to=origin/main main
  write_file "$d/local.txt" 'not pushed'
  git -C "$d" add local.txt
  git -C "$d" commit -q -m "local commit, not pushed"
  write_file "$d/.git/hooks/pre-push" '#!/bin/sh' "touch '$CASE_TMP/push-attempted'"
  chmod 755 "$d/.git/hooks/pre-push"
}

remote_state() {
  git -C "$1-remote.git" for-each-ref --format='%(refname) %(objectname)'
}

all_branches() {
  local s
  for s in $STEPS; do
    expected_branch "$s"
  done
}

case_cut_dry_run_is_the_default_and_changes_nothing() {
  local st="$CASE_TMP/stage" ws="$CASE_TMP/workshop" b
  chain_stage "$st"
  make_workshop "$ws"
  ref_state "$ws" > "$CASE_TMP/before.txt"
  remote_state "$ws" > "$CASE_TMP/remote-before.txt"
  expect_ok "$CASE_TMP/run.txt" bash "$st/$CUT" --repo "$ws" level-0-start
  assert_grep "$CASE_TMP/run.txt" "dry run"
  assert_grep "$CASE_TMP/run.txt" "--execute"
  assert_grep "$CASE_TMP/run.txt" "+ git clone"
  assert_grep "$CASE_TMP/run.txt" "apply-level.sh' level-3 '<tmp>/work'"
  assert_grep "$CASE_TMP/run.txt" "update-ref --stdin"
  for b in $(all_branches); do
    assert_grep "$CASE_TMP/run.txt" "create refs/heads/$b"
  done
  # Same from inside the repository, without --repo.
  expect_ok "$CASE_TMP/run-cwd.txt" sh -c 'cd "$1" && bash "$2" level-0-start' sh "$ws" "$st/$CUT"
  assert_grep "$CASE_TMP/run-cwd.txt" "dry run"
  ref_state "$ws" > "$CASE_TMP/after.txt"
  remote_state "$ws" > "$CASE_TMP/remote-after.txt"
  diff "$CASE_TMP/before.txt" "$CASE_TMP/after.txt" || die "dry run changed the repository"
  diff "$CASE_TMP/remote-before.txt" "$CASE_TMP/remote-after.txt" || die "dry run changed the remote"
  assert_absent "$CASE_TMP/push-attempted"
}

case_cut_execute_creates_six_local_branches_only() {
  local st="$CASE_TMP/stage" ws="$CASE_TMP/workshop" s b l0 n t
  chain_stage "$st"
  make_workshop "$ws"
  git -C "$ws" for-each-ref --format='%(refname) %(objectname)' | LC_ALL=C sort > "$CASE_TMP/refs-before.txt"
  git -C "$ws" tag --list -n > "$CASE_TMP/tags-before.txt"
  git -C "$ws" worktree list --porcelain > "$CASE_TMP/worktrees-before.txt"
  git -C "$ws" rev-parse HEAD > "$CASE_TMP/head-before.txt"
  remote_state "$ws" > "$CASE_TMP/remote-before.txt"
  expect_ok "$CASE_TMP/run.txt" bash "$st/$CUT" --execute --repo "$ws" level-0-start
  git -C "$ws" for-each-ref --format='%(refname) %(objectname)' | LC_ALL=C sort > "$CASE_TMP/refs-after.txt"
  comm -23 "$CASE_TMP/refs-before.txt" "$CASE_TMP/refs-after.txt" > "$CASE_TMP/refs-gone.txt"
  [ ! -s "$CASE_TMP/refs-gone.txt" ] || { cat "$CASE_TMP/refs-gone.txt" >&2; die "existing refs changed or vanished"; }
  comm -13 "$CASE_TMP/refs-before.txt" "$CASE_TMP/refs-after.txt" | cut -d' ' -f1 > "$CASE_TMP/refs-new.txt"
  all_branches | sed 's|^|refs/heads/|' | LC_ALL=C sort > "$CASE_TMP/refs-expected.txt"
  diff "$CASE_TMP/refs-expected.txt" "$CASE_TMP/refs-new.txt" || die "new refs are not exactly the six local branches"
  git -C "$ws" tag --list -n > "$CASE_TMP/tags-after.txt"
  diff "$CASE_TMP/tags-before.txt" "$CASE_TMP/tags-after.txt" || die "tags changed"
  git -C "$ws" worktree list --porcelain > "$CASE_TMP/worktrees-after.txt"
  diff "$CASE_TMP/worktrees-before.txt" "$CASE_TMP/worktrees-after.txt" || die "worktrees changed"
  assert_eq "$(git -C "$ws" rev-parse HEAD)" "$(cat "$CASE_TMP/head-before.txt")" "HEAD"
  assert_eq "$(git -C "$ws" status --porcelain --untracked-files=all)" "" "status"
  remote_state "$ws" > "$CASE_TMP/remote-after.txt"
  diff "$CASE_TMP/remote-before.txt" "$CASE_TMP/remote-after.txt" || die "the remote changed"
  assert_absent "$CASE_TMP/push-attempted"

  # Chain: one commit per step on its parent.
  l0=$(git -C "$ws" rev-parse 'level-0-start^{commit}')
  assert_eq "$(git -C "$ws" rev-parse solution/level-1^)" "$l0" "parent of solution/level-1"
  assert_eq "$(git -C "$ws" rev-parse solution/level-2^)" "$(git -C "$ws" rev-parse solution/level-1)" "parent of solution/level-2"
  assert_eq "$(git -C "$ws" rev-parse solution/level-3^)" "$(git -C "$ws" rev-parse solution/level-2)" "parent of solution/level-3"
  for s in $STEPS; do
    b=$(expected_branch "$s")
    n=$(expected_chain "$s" | wc -w | tr -d ' ')
    assert_eq "$(git -C "$ws" rev-list --count "$l0..$b")" "$n" "commits on $b since level-0-start"
    assert_eq "$(git -C "$ws" log -1 --format=%s "$b")" "$(expected_message "$s")" "message of $b"
    case $s in
      level-4-*)
        assert_eq "$(git -C "$ws" rev-parse "$b^")" "$(git -C "$ws" rev-parse solution/level-3)" "parent of $b"
        ;;
    esac
    # The branch tree equals the tree build-level.sh produces for the step on a fresh simulated level-0.
    t="$CASE_TMP/reference-$s"
    level0 "$t"
    bash "$st/$BUILD" "$s" "$t" > /dev/null
    assert_eq "$(git -C "$ws" rev-parse "$b^{tree}")" "$(git -C "$t" rev-parse 'HEAD^{tree}')" "tree of $b"
  done
}

case_cut_refuses_when_a_target_branch_exists() {
  local st="$CASE_TMP/stage" ws="$CASE_TMP/workshop" b copy k=0
  chain_stage "$st"
  make_workshop "$ws"
  for b in $(all_branches); do
    k=$((k + 1))
    copy="$CASE_TMP/copy-$k"
    cp -R "$ws" "$copy"
    git -C "$copy" branch "$b" main
    ref_state "$copy" > "$CASE_TMP/before.txt"
    expect_fail "$CASE_TMP/dry-$k.txt" bash "$st/$CUT" --repo "$copy" level-0-start
    assert_grep "$CASE_TMP/dry-$k.txt" "branch $b already exists"
    expect_fail "$CASE_TMP/exec-$k.txt" bash "$st/$CUT" --execute --repo "$copy" level-0-start
    assert_grep "$CASE_TMP/exec-$k.txt" "branch $b already exists"
    ref_state "$copy" > "$CASE_TMP/after.txt"
    diff "$CASE_TMP/before.txt" "$CASE_TMP/after.txt" || die "refused cut changed the repository ($b)"
  done
}

case_cut_creates_nothing_when_a_step_fails() {
  local st="$CASE_TMP/stage" ws="$CASE_TMP/workshop"
  chain_stage "$st"
  staged "$st" level-3 files/README.md 'collides'
  printf 'add\tconfig\tfiles/README.md\tREADME.md\t644\t-\n' >> "$st/level-3/manifest.tsv"
  make_workshop "$ws"
  ref_state "$ws" > "$CASE_TMP/before.txt"
  remote_state "$ws" > "$CASE_TMP/remote-before.txt"
  expect_fail "$CASE_TMP/run.txt" bash "$st/$CUT" --execute --repo "$ws" level-0-start
  assert_grep "$CASE_TMP/run.txt" "level-3/manifest.tsv line 7: add: target exists: README.md"
  ref_state "$ws" > "$CASE_TMP/after.txt"
  diff "$CASE_TMP/before.txt" "$CASE_TMP/after.txt" || die "failed cut changed the repository"
  remote_state "$ws" > "$CASE_TMP/remote-after.txt"
  diff "$CASE_TMP/remote-before.txt" "$CASE_TMP/remote-after.txt" || die "failed cut changed the remote"
}

# The last level-4 track fails after five steps succeeded in the work clone: still no branch.
case_cut_creates_nothing_when_the_last_level_4_step_fails() {
  local st="$CASE_TMP/stage" ws="$CASE_TMP/workshop"
  chain_stage "$st"
  staged "$st" level-4-agentos files/README.md 'collides'
  printf 'add\tconfig\tfiles/README.md\tREADME.md\t644\t-\n' >> "$st/level-4-agentos/manifest.tsv"
  make_workshop "$ws"
  ref_state "$ws" > "$CASE_TMP/before.txt"
  remote_state "$ws" > "$CASE_TMP/remote-before.txt"
  expect_fail "$CASE_TMP/run.txt" bash "$st/$CUT" --execute --repo "$ws" level-0-start
  assert_grep "$CASE_TMP/run.txt" "level-4-agentos/manifest.tsv line 3: add: target exists: README.md"
  assert_grep "$CASE_TMP/run.txt" "cut-level-branches: error: step level-4-agentos failed; no branch was created"
  assert_no_grep "$CASE_TMP/run.txt" "cut-level-branches: created"
  ref_state "$ws" > "$CASE_TMP/after.txt"
  diff "$CASE_TMP/before.txt" "$CASE_TMP/after.txt" || die "failed cut changed the repository"
  remote_state "$ws" > "$CASE_TMP/remote-after.txt"
  diff "$CASE_TMP/remote-before.txt" "$CASE_TMP/remote-after.txt" || die "failed cut changed the remote"
  assert_eq "$(git -C "$ws" for-each-ref --format='%(refname)' refs/heads/solution refs/heads/start)" "" \
    "solution/* and start/* branches"
  assert_absent "$CASE_TMP/push-attempted"
}

case_cut_refuses_bad_arguments() {
  local st="$CASE_TMP/stage" ws="$CASE_TMP/workshop"
  chain_stage "$st"
  make_workshop "$ws"
  ref_state "$ws" > "$CASE_TMP/before.txt"
  expect_fail "$CASE_TMP/run1.txt" bash "$st/$CUT" --execute --repo "$ws" no-such-ref
  assert_grep "$CASE_TMP/run1.txt" "unknown ref: no-such-ref"
  expect_fail "$CASE_TMP/run2.txt" bash "$st/$CUT" --repo "$ws"
  expect_fail "$CASE_TMP/run3.txt" bash "$st/$CUT" --push --repo "$ws" level-0-start
  assert_grep "$CASE_TMP/run3.txt" "usage: cut-level-branches"
  mkdir -p "$CASE_TMP/plain"
  expect_fail "$CASE_TMP/run5.txt" bash "$st/$CUT" --execute --repo "$CASE_TMP/plain" level-0-start
  assert_grep "$CASE_TMP/run5.txt" "cut-level-branches: error: not a git work tree"
  assert_eq "$(ls -A "$CASE_TMP/plain")" "" "plain folder after a refused cut"
  rm -rf "$st/level-4-speckit"
  expect_fail "$CASE_TMP/run4.txt" bash "$st/$CUT" --execute --repo "$ws" level-0-start
  assert_grep "$CASE_TMP/run4.txt" "level-4-speckit/manifest.tsv is missing"
  ref_state "$ws" > "$CASE_TMP/after.txt"
  diff "$CASE_TMP/before.txt" "$CASE_TMP/after.txt" || die "refused cut changed the repository"
}

# ---------------------------------------------------------------------------------------------------------------
# Line endings and modes
# ---------------------------------------------------------------------------------------------------------------

# check_eol <clone> <path> lf|crlf: `git ls-files --eol` and `od -c` of a fresh clone.
check_eol() {
  local c=$1 p=$2 want=$3 got crs lfs
  got=$(git -C "$c" ls-files --eol -- "$p" | awk '{ print $1, $2, $3, $4 }')
  od -c "$c/$p" > "$CASE_TMP/od.txt"
  crs=$(tr -cd '\r' < "$c/$p" | wc -c | tr -d ' ')
  lfs=$(tr -cd '\n' < "$c/$p" | wc -c | tr -d ' ')
  if [ "$want" = crlf ]; then
    assert_eq "$got" "i/lf w/crlf attr/text eol=crlf" "ls-files --eol of $p"
    grep -F -q '\r' "$CASE_TMP/od.txt" || die "od -c shows no \\r in $p"
    grep -E -q '\\r +\\n' "$CASE_TMP/od.txt" || die "od -c shows no \\r \\n pair in $p"
    assert_eq "$crs" "$lfs" "every line of $p ends with CR LF"
  else
    assert_eq "$got" "i/lf w/lf attr/text eol=lf" "ls-files --eol of $p"
    if grep -F -q '\r' "$CASE_TMP/od.txt"; then
      die "od -c shows \\r in $p"
    fi
    assert_eq "$crs" 0 "carriage returns in $p"
  fi
}

case_eol_staging_folder_gitattributes() {
  local r="$CASE_TMP/factory" c="$CASE_TMP/clone" p=docs/spec-pack/level-branches
  mkdir -p "$r/$p"
  git -C "$r" init -q
  cp "$S_DIR/.gitattributes" "$r/$p/.gitattributes"
  write_file "$r/$p/level-3/files/dot-claude/hooks/check.sh" '#!/bin/sh' 'exit 0'
  write_file "$r/$p/level-3/files/dot-githooks/pre-commit" '#!/bin/sh' 'echo pre-commit'
  printf 'Write-Output "check"\r\nexit 0\r\n' > "$r/$p/level-3/files/dot-claude/hooks/check.ps1"
  write_file "$r/$p/level-3/files/dot-claude/hooks/other.ps1" 'Write-Output "written with LF"' 'exit 0'
  write_file "$r/$p/scripts/x.sh" '#!/usr/bin/env bash' 'true'
  git -C "$r" add --all
  git -C "$r" commit -q -m "staging folder"
  git clone -q "$r" "$c"
  check_eol "$c" "$p/level-3/files/dot-claude/hooks/check.ps1" crlf
  check_eol "$c" "$p/level-3/files/dot-claude/hooks/other.ps1" crlf
  check_eol "$c" "$p/level-3/files/dot-claude/hooks/check.sh" lf
  check_eol "$c" "$p/level-3/files/dot-githooks/pre-commit" lf
  check_eol "$c" "$p/scripts/x.sh" lf
}

case_eol_and_modes_on_a_clone_of_a_built_level() {
  local st="$CASE_TMP/stage" t="$CASE_TMP/target" c="$CASE_TMP/clone"
  chain_stage "$st"
  level0 "$t"
  expect_ok "$CASE_TMP/run.txt" bash "$st/$BUILD" level-3 "$t"
  git clone -q "$t" "$c"
  check_eol "$c" .claude/hooks/check.ps1 crlf
  check_eol "$c" .claude/hooks/check.sh lf
  check_eol "$c" .githooks/pre-commit lf
  assert_eq "$(git -C "$c" ls-files --stage .claude/hooks/check.sh | cut -c1-6)" 100755 "mode of check.sh"
  assert_eq "$(git -C "$c" ls-files --stage .githooks/pre-commit | cut -c1-6)" 100755 "mode of pre-commit"
  assert_eq "$(git -C "$c" ls-files --stage .claude/hooks/check.ps1 | cut -c1-6)" 100644 "mode of check.ps1"
  [ -x "$c/.claude/hooks/check.sh" ] || die "check.sh is not executable in the clone"
  [ -x "$c/.githooks/pre-commit" ] || die "pre-commit is not executable in the clone"
}

case_staging_scripts_are_executable_lf_bash() {
  local f
  for f in "$S_DIR"/scripts/*.sh "$S_DIR/tests/test-staging.sh"; do
    [ -x "$f" ] || die "not executable (mode 755): $f"
    assert_eq "$(head -n 1 "$f")" '#!/usr/bin/env bash' "shebang of $f"
    assert_eq "$(tr -cd '\r' < "$f" | wc -c | tr -d ' ')" 0 "carriage returns in $f"
    grep -q '^set -euo pipefail$' "$f" || [ "$(basename "$f")" = lib-steps.sh ] || die "no set -euo pipefail in $f"
  done
}

# Without a template, macOS makes temp folders in the system temp folder even when TMPDIR is set. Every temp folder
# of the scripts, this test and the README recipes uses a template under ${TMPDIR:-/tmp}, so a run with TMPDIR set
# writes nothing outside it. The command name is split so this file's own checks do not count as uses.
case_temp_folders_use_a_template_under_tmpdir() {
  local f word="mk""temp" valid uses=0
  valid='^[0-9]+:'"$word"'( -d)? "\$\{TMPDIR:-/tmp\}/[^"/ ]+\.XXXXXX"$'
  : > "$CASE_TMP/bad.txt"
  for f in "$S_DIR"/scripts/*.sh "$S_DIR/tests/test-staging.sh" "$S_DIR/README.md"; do
    grep -n -o -E "$word( -d)?( [^ )]*)?" "$f" > "$CASE_TMP/uses.txt" || true
    uses=$((uses + $(wc -l < "$CASE_TMP/uses.txt")))
    grep -v -E "$valid" "$CASE_TMP/uses.txt" | sed "s|^|$f:|" >> "$CASE_TMP/bad.txt" || true
  done
  [ "$uses" -ge 6 ] || die "expected at least 6 temp folder commands, found $uses"
  if [ -s "$CASE_TMP/bad.txt" ]; then
    cat "$CASE_TMP/bad.txt" >&2
    die "temp folder commands without a template under \${TMPDIR:-/tmp}"
  fi
}

case_scripts_leave_no_temp_files_behind() {
  local st="$CASE_TMP/stage" tmp="$CASE_TMP/tmp" src="$CASE_TMP/src" ws="$CASE_TMP/workshop"
  mkdir -p "$tmp"
  # apply: a success and a refused 3-way merge, both after the backup folder is made.
  new_stage "$st"
  foo_patch "$st/level-2/patches/bookstore/Foo.java.patch"
  manifest "$st" level-2 "patch|exercise|patches/bookstore/Foo.java.patch|bookstore/Foo.java|644|$FOO_BASE_SHA"
  new_target "$CASE_TMP/ok"
  expect_ok "$CASE_TMP/run1.txt" env TMPDIR="$tmp" bash "$st/$APPLY" level-2 "$CASE_TMP/ok"
  assert_eq "$(ls -A "$tmp")" "" "temp folder after an apply"
  new_target "$CASE_TMP/conflict"
  commit_foo_variant "$CASE_TMP/conflict" 's/^line 02$/line 02 changed on the target/'
  expect_fail "$CASE_TMP/run2.txt" env TMPDIR="$tmp" bash "$st/$APPLY" level-2 "$CASE_TMP/conflict"
  assert_grep "$CASE_TMP/run2.txt" "not clean"
  assert_eq "$(ls -A "$tmp")" "" "temp folder after a rolled back apply"
  # simulate: the publish script's stderr goes to a temp file.
  make_source "$src"
  fake_publish "$src" 'exit 3'
  expect_ok "$CASE_TMP/run3.txt" env TMPDIR="$tmp" bash "$SIM" --repo "$src" HEAD "$CASE_TMP/out"
  assert_grep "$CASE_TMP/run3.txt" "--list-paths failed (exit 3)"
  assert_eq "$(ls -A "$tmp")" "" "temp folder after a simulate"
  # cut --execute: the work clone lives in a temp folder.
  chain_stage "$CASE_TMP/chain"
  make_workshop "$ws"
  expect_ok "$CASE_TMP/run4.txt" env TMPDIR="$tmp" bash "$CASE_TMP/chain/$CUT" --execute --repo "$ws" level-0-start
  assert_eq "$(ls -A "$tmp")" "" "temp folder after a cut"
}

case_readme_cut_procedure_names_only_local_operations() {
  local r="$S_DIR/README.md" b
  awk '/^## Cut and verify procedure/ { f = 1; next } f && /^## / { exit } f { print }' "$r" > "$CASE_TMP/cut.txt"
  assert_grep "$CASE_TMP/cut.txt" "cut-level-branches.sh --execute level-0-start"
  assert_no_grep "$CASE_TMP/cut.txt" "git push"
  # No command line outside the publishing section pushes, in any form (git -C <dir> push, git ... push -u, ...).
  awk '/^## / { pub = ($0 ~ /^## Publishing the branches/) } /^ *```/ { code = !code; next } code && !pub { print NR ": " $0 }' \
    "$r" > "$CASE_TMP/code.txt"
  assert_grep "$CASE_TMP/code.txt" "cut-level-branches.sh --execute level-0-start"
  assert_grep "$CASE_TMP/code.txt" "./mvnw clean verify"
  if grep -E '(^|[[:space:]])push([[:space:]]|$)' "$CASE_TMP/code.txt" > "$CASE_TMP/pushes.txt"; then
    cat "$CASE_TMP/pushes.txt" >&2
    die "a README command outside 'Publishing the branches' pushes"
  fi
  awk '/^## Publishing the branches/ { f = 1; next } f && /^## / { exit } f { print }' "$r" > "$CASE_TMP/publish.txt"
  for b in $(all_branches); do
    assert_grep "$CASE_TMP/publish.txt" "git push origin $b"
  done
  assert_eq "$(grep -c 'git push' "$r")" 6 "git push lines in the README"
  assert_no_grep "$r" "--force"
  assert_no_grep "$r" "push -f"
}

# ---------------------------------------------------------------------------------------------------------------
# Failures after the checks: phase-2 writes, rollback, cleanup and internal git failures
# ---------------------------------------------------------------------------------------------------------------

# fail_shim <dir> <command> <glob>: <dir>/<command> runs the real command, except that it exits 1 when its arguments,
# joined by spaces, match the shell pattern <glob>. Put <dir> first on PATH to make one call of a script fail.
fail_shim() {
  local d=$1 cmd=$2 glob=$3 real
  real=$(command -v "$cmd")
  mkdir -p "$d"
  write_file "$d/$cmd" '#!/bin/sh' "pat='$glob'" 'case "$*" in' \
    "  \$pat) echo \"shim: $cmd \$*: failing on purpose\" >&2; exit 1 ;;" 'esac' "exec '$real' \"\$@\""
  chmod 755 "$d/$cmd"
}

ST=""
TGT=""
# write_stage <row...>: a level-2 step with the given rows ('|' separated) in $ST; each staged file under files/ is
# made from its row, and patches/bookstore/Foo.java.patch is copied from $CASE_TMP/foo.patch (see foo_patch). The
# target $TGT is at the patch base and also holds ro.txt (mode 444) and locked/keep.txt (folder locked/ mode 555).
# When a row stages files/same.txt, the target also holds a committed same.txt (mode 644) with the same content.
write_stage() {
  local r s
  ST="$CASE_TMP/stage"
  TGT="$CASE_TMP/target"
  if [ -d "$TGT" ]; then
    chmod -R u+w "$TGT"
  fi
  rm -rf "$ST" "$TGT"
  new_stage "$ST"
  for r in "$@"; do
    s=$(printf '%s\n' "$r" | cut -d'|' -f3)
    case $s in
      files/*) staged "$ST" level-2 "$s" "staged $s" ;;
      patches/bookstore/Foo.java.patch)
        mkdir -p "$ST/level-2/patches/bookstore"
        cp "$CASE_TMP/foo.patch" "$ST/level-2/$s"
        ;;
    esac
  done
  manifest "$ST" level-2 "$@"
  new_target "$TGT"
  write_file "$TGT/ro.txt" 'read-only content'
  write_file "$TGT/locked/keep.txt" 'in a read-only folder'
  if [ -f "$ST/level-2/files/same.txt" ]; then
    cp "$ST/level-2/files/same.txt" "$TGT/same.txt"
    chmod 644 "$TGT/same.txt"
  fi
  git -C "$TGT" add --all
  git -C "$TGT" commit -q -m "read-only file and folder"
  chmod 444 "$TGT/ro.txt"
  chmod 555 "$TGT/locked"
}

# refuse_write <line> <reason> <row...>: rows that pass every check, but the write of <line> fails. The apply must
# name the line and the reason, report a complete rollback, leave the target tree (index included) unchanged and
# leave no backup folder in TMPDIR.
# Env: SHIM_PATH  a folder put first on PATH for the apply (see fail_shim)
refuse_write() {
  local line=$1 reason=$2 tmp="$CASE_TMP/tmp"
  shift 2
  write_stage "$@"
  rm -rf "$tmp"
  mkdir -p "$tmp"
  tree_state "$TGT" > "$CASE_TMP/before.txt"
  expect_fail "$CASE_TMP/run.txt" env PATH="${SHIM_PATH:+$SHIM_PATH:}$PATH" TMPDIR="$tmp" \
    bash "$ST/$APPLY" level-2 "$TGT"
  assert_grep "$CASE_TMP/run.txt" "apply-level: error: level-2/manifest.tsv line $line: $reason"
  assert_grep "$CASE_TMP/run.txt" "apply-level: rolled back; the target tree is unchanged"
  assert_no_grep "$CASE_TMP/run.txt" "rollback incomplete"
  tree_state "$TGT" > "$CASE_TMP/after.txt"
  diff "$CASE_TMP/before.txt" "$CASE_TMP/after.txt" || die "target changed by a rolled back apply"
  assert_eq "$(ls -A "$tmp")" "" "TMPDIR after a rolled back apply"
}

case_apply_rolls_back_a_failing_write_after_a_patch_row() {
  foo_patch "$CASE_TMP/foo.patch"
  # The patch on line 2 stages Foo.java in the index; the replace on line 3 cannot write the read-only ro.txt.
  refuse_write 3 "replace: cannot write ro.txt" \
    "patch|exercise|patches/bookstore/Foo.java.patch|bookstore/Foo.java|644|$FOO_BASE_SHA" \
    'replace|config|files/ro.txt|ro.txt|644|-'
  assert_eq "$(git -C "$TGT" diff --cached --name-only)" "" "staged changes after the rollback"
  # Two folders created on line 3 are removed again.
  refuse_write 4 "replace: cannot write ro.txt" \
    'add|config|files/valid.txt|valid.txt|644|-' \
    'add|config|files/newdir/sub/x.md|newdir/sub/x.md|644|-' \
    'replace|config|files/ro.txt|ro.txt|644|-'
  assert_absent "$TGT/newdir"
  assert_absent "$TGT/valid.txt"
}

case_apply_rolls_back_every_other_failing_write() {
  local shim="$CASE_TMP/shim" k
  # An add into a read-only folder.
  refuse_write 3 "add: cannot write locked/x.md" \
    'add|config|files/valid.txt|valid.txt|644|-' \
    'add|config|files/locked/x.md|locked/x.md|644|-'
  # A folder that cannot be made below a read-only folder; the folder made on line 2 is removed again.
  refuse_write 3 "add: cannot create the folder of locked/new/x.md" \
    'add|config|files/newdir/a.md|newdir/a.md|644|-' \
    'add|config|files/locked/new/x.md|locked/new/x.md|644|-'
  assert_absent "$TGT/newdir"
  # A mode that cannot be set.
  fail_shim "$shim" chmod '644 */later.txt'
  SHIM_PATH=$shim refuse_write 3 "cannot set mode 644 on later.txt" \
    'replace|config|files/existing.txt|existing.txt|755|-' \
    'add|config|files/later.txt|later.txt|644|-'
  # A mode-only change: same.txt keeps its content and only gains the exec bit; the rollback must restore the mode.
  refuse_write 3 "replace: cannot write ro.txt" \
    'replace|config|files/same.txt|same.txt|755|-' \
    'replace|config|files/ro.txt|ro.txt|644|-'
  [ ! -x "$TGT/same.txt" ] || die "same.txt is still executable after the rollback"
  # A backup copy that fails: refused before the first write, nothing to roll back, no backup folder left.
  for k in 1 2; do
    case $k in
      1) fail_shim "$CASE_TMP/shim-b$k" cp '-p */existing.txt */apply-level.*/row-1' ;;
      2) fail_shim "$CASE_TMP/shim-b$k" cp '-p */index */apply-level.*/index' ;;
    esac
    write_stage 'add|config|files/valid.txt|valid.txt|644|-' 'replace|config|files/existing.txt|existing.txt|644|-'
    rm -rf "$CASE_TMP/tmp"
    mkdir -p "$CASE_TMP/tmp"
    tree_state "$TGT" > "$CASE_TMP/before.txt"
    expect_fail "$CASE_TMP/backup-$k.txt" env PATH="$CASE_TMP/shim-b$k:$PATH" TMPDIR="$CASE_TMP/tmp" \
      bash "$ST/$APPLY" level-2 "$TGT"
    case $k in
      1) assert_grep "$CASE_TMP/backup-$k.txt" \
        "apply-level: error: level-2/manifest.tsv line 3: cannot back up existing.txt; nothing was written" ;;
      2) assert_grep "$CASE_TMP/backup-$k.txt" "apply-level: error: cannot back up the git index; nothing was written" ;;
    esac
    assert_no_grep "$CASE_TMP/backup-$k.txt" "rolled back"
    assert_no_grep "$CASE_TMP/backup-$k.txt" "apply-level: level-2 line"
    tree_state "$TGT" > "$CASE_TMP/after.txt"
    diff "$CASE_TMP/before.txt" "$CASE_TMP/after.txt" || die "target changed by a failed backup"
    assert_eq "$(ls -A "$CASE_TMP/tmp")" "" "TMPDIR after a failed backup"
  done
}

# A restore step that fails does not stop the rollback: the rest is restored, the backup folder is removed, and the
# apply names what it could not restore instead of claiming an unchanged tree.
case_apply_reports_an_incomplete_rollback_and_still_finishes_it() {
  local tmp="$CASE_TMP/tmp" shim1="$CASE_TMP/shim-row" shim2="$CASE_TMP/shim-index"
  foo_patch "$CASE_TMP/foo.patch"
  mkdir -p "$tmp"
  # Row 0 (line 2, existing.txt) cannot be restored; lines 3 and 4 must still be rolled back.
  fail_shim "$shim1" cp '-p */apply-level.*/row-0 *'
  write_stage 'replace|config|files/existing.txt|existing.txt|644|-' \
    'add|config|files/newdir/sub/x.md|newdir/sub/x.md|644|-' \
    "patch|exercise|patches/bookstore/Foo.java.patch|bookstore/Foo.java|644|$FOO_BASE_SHA" \
    'replace|config|files/ro.txt|ro.txt|644|-'
  git -C "$TGT" ls-files --stage > "$CASE_TMP/index-before.txt"
  expect_fail "$CASE_TMP/run1.txt" env PATH="$shim1:$PATH" TMPDIR="$tmp" bash "$ST/$APPLY" level-2 "$TGT"
  assert_grep "$CASE_TMP/run1.txt" "apply-level: error: level-2/manifest.tsv line 5: replace: cannot write ro.txt"
  assert_grep "$CASE_TMP/run1.txt" "apply-level: rollback incomplete: could not restore existing.txt"
  assert_grep "$CASE_TMP/run1.txt" "apply-level: rollback incomplete; the target tree is NOT as it was before the apply"
  assert_no_grep "$CASE_TMP/run1.txt" "the target tree is unchanged"
  assert_eq "$(cat "$TGT/existing.txt")" 'staged files/existing.txt' "existing.txt (could not be restored)"
  assert_absent "$TGT/newdir"
  assert_eq "$(cat "$TGT/bookstore/Foo.java")" "$(foo_base_lines)" "Foo.java after the rollback"
  git -C "$TGT" ls-files --stage > "$CASE_TMP/index-after.txt"
  diff "$CASE_TMP/index-before.txt" "$CASE_TMP/index-after.txt" || die "index not restored"
  assert_eq "$(git -C "$TGT" status --porcelain --untracked-files=all)" " M existing.txt" "status after the rollback"
  assert_eq "$(ls -A "$tmp")" "" "TMPDIR after an incomplete rollback"
  # The index cannot be restored; the files still are.
  fail_shim "$shim2" cp '-p */apply-level.*/index *'
  write_stage "patch|exercise|patches/bookstore/Foo.java.patch|bookstore/Foo.java|644|$FOO_BASE_SHA" \
    'replace|config|files/ro.txt|ro.txt|644|-'
  expect_fail "$CASE_TMP/run2.txt" env PATH="$shim2:$PATH" TMPDIR="$tmp" bash "$ST/$APPLY" level-2 "$TGT"
  assert_grep "$CASE_TMP/run2.txt" "apply-level: error: level-2/manifest.tsv line 3: replace: cannot write ro.txt"
  assert_grep "$CASE_TMP/run2.txt" "apply-level: rollback incomplete: could not restore the git index"
  assert_no_grep "$CASE_TMP/run2.txt" "the target tree is unchanged"
  assert_eq "$(cat "$TGT/bookstore/Foo.java")" "$(foo_base_lines)" "Foo.java after the rollback"
  assert_eq "$(git -C "$TGT" diff --cached --name-only)" "bookstore/Foo.java" "staged changes left in the index"
  assert_eq "$(ls -A "$tmp")" "" "TMPDIR after an incomplete rollback"
  # A file the apply added cannot be removed again (rm shim): it is named, no unchanged tree is claimed.
  fail_shim "$CASE_TMP/shim-rm" rm '-f */top.txt'
  write_stage 'add|config|files/top.txt|top.txt|644|-' 'replace|config|files/ro.txt|ro.txt|644|-'
  expect_fail "$CASE_TMP/run3.txt" env PATH="$CASE_TMP/shim-rm:$PATH" TMPDIR="$tmp" bash "$ST/$APPLY" level-2 "$TGT"
  assert_grep "$CASE_TMP/run3.txt" "apply-level: error: level-2/manifest.tsv line 3: replace: cannot write ro.txt"
  assert_grep "$CASE_TMP/run3.txt" "apply-level: rollback incomplete: could not restore top.txt"
  assert_grep "$CASE_TMP/run3.txt" "apply-level: rollback incomplete; the target tree is NOT as it was before the apply"
  assert_no_grep "$CASE_TMP/run3.txt" "the target tree is unchanged"
  assert_eq "$(git -C "$TGT" status --porcelain --untracked-files=all)" "?? top.txt" "status after the rollback"
  assert_eq "$(ls -A "$tmp")" "" "TMPDIR after an incomplete rollback"
}

# Each git command of cut --execute that has a guard fails once (git shim): the guard's message, no ref and no
# remote change, no temp folder left.
case_cut_creates_nothing_when_an_internal_git_command_fails() {
  local st="$CASE_TMP/stage" ws="$CASE_TMP/workshop" tmp="$CASE_TMP/tmp" k=0 glob msg
  chain_stage "$st"
  make_workshop "$ws"
  mkdir -p "$tmp"
  ref_state "$ws" > "$CASE_TMP/before.txt"
  remote_state "$ws" > "$CASE_TMP/remote-before.txt"
  while IFS='|' read -r glob msg; do
    k=$((k + 1))
    fail_shim "$CASE_TMP/shim-$k" git "$glob"
    expect_fail "$CASE_TMP/run-$k.txt" env PATH="$CASE_TMP/shim-$k:$PATH" TMPDIR="$tmp" \
      bash "$st/$CUT" --execute --repo "$ws" level-0-start
    assert_grep "$CASE_TMP/run-$k.txt" "shim: git"
    assert_grep "$CASE_TMP/run-$k.txt" "cut-level-branches: error: $msg"
    assert_no_grep "$CASE_TMP/run-$k.txt" "cut-level-branches: created"
    ref_state "$ws" > "$CASE_TMP/after.txt"
    diff "$CASE_TMP/before.txt" "$CASE_TMP/after.txt" || die "failed cut changed the repository ($glob)"
    remote_state "$ws" > "$CASE_TMP/remote-after.txt"
    diff "$CASE_TMP/remote-before.txt" "$CASE_TMP/remote-after.txt" || die "failed cut changed the remote ($glob)"
    assert_eq "$(ls -A "$tmp")" "" "TMPDIR after a failed cut ($glob)"
  done <<'EOF'
clone --quiet *|clone failed
-C */work config user.name *|cannot set user.name in the work clone
-C */work checkout --quiet --detach [0-9a-f]*|checkout of
-C */work branch solution/level-1|branch in the work clone failed
-C */work checkout --quiet --detach solution/level-3|checkout of solution/level-3 failed
-C */work branch start/level-4-agentos|branch in the work clone failed
-C * fetch --quiet *|fetch failed
-C */work rev-parse --verify refs/heads/start/level-4-speckit*|cannot resolve start/level-4-speckit in the work clone
-C * update-ref --stdin|creating the branches failed; none was created
EOF
  [ "$k" = 9 ] || die "expected 9 git failure points, ran $k"
  assert_absent "$CASE_TMP/push-attempted"
}

# Every exit cleanup runs all of its steps: a removal that fails (rm shim) is reported, the other temp files are still
# removed, and the exit status is the one of the run.
case_cleanup_failures_are_reported_and_the_rest_still_runs() {
  local src="$CASE_TMP/src" tmp="$CASE_TMP/tmp" out="$CASE_TMP/out" empty="$CASE_TMP/empty-out"
  local st="$CASE_TMP/stage" ws="$CASE_TMP/workshop"
  mkdir -p "$tmp" "$empty"
  make_source "$src"
  fake_publish "$src" "printf '%s\\n' ../outside" 'exit 0'
  # simulate refuses after the extraction; the new out-dir cannot be removed, the stderr temp file still is.
  fail_shim "$CASE_TMP/shim-1" rm '-rf */out'
  expect_fail "$CASE_TMP/run1.txt" env PATH="$CASE_TMP/shim-1:$PATH" TMPDIR="$tmp" bash "$SIM" --repo "$src" HEAD "$out"
  assert_grep "$CASE_TMP/run1.txt" "unsafe path ('..' path component): '../outside'"
  # The message names the resolved path (pwd -P), so only its last component is compared.
  grep -E -q '^simulate-level-0: error: could not remove the out-dir; remove it by hand: /.*/out$' \
    "$CASE_TMP/run1.txt" || die "no out-dir removal error naming the out-dir"
  [ -d "$out" ] || die "fixture: the failing rm should leave the out-dir"
  assert_eq "$(ls -A "$tmp")" "" "TMPDIR after a failed out-dir cleanup"
  # The same with an existing empty out-dir that cannot be emptied again.
  fail_shim "$CASE_TMP/shim-2" rm '-rf */empty-out/*'
  expect_fail "$CASE_TMP/run2.txt" env PATH="$CASE_TMP/shim-2:$PATH" TMPDIR="$tmp" \
    bash "$SIM" --repo "$src" HEAD "$empty"
  assert_grep "$CASE_TMP/run2.txt" "unsafe path ('..' path component): '../outside'"
  grep -E -q '^simulate-level-0: error: could not empty the out-dir; empty it by hand: /.*/empty-out$' \
    "$CASE_TMP/run2.txt" || die "no out-dir emptying error naming the out-dir"
  assert_eq "$(ls -A "$tmp")" "" "TMPDIR after a failed out-dir cleanup"
  # cut: the work clone cannot be removed; the six branches exist, the cut exits 0 and says what is left.
  chain_stage "$st"
  make_workshop "$ws"
  fail_shim "$CASE_TMP/shim-3" rm '-rf */cut-level-branches.*'
  expect_ok "$CASE_TMP/run3.txt" env PATH="$CASE_TMP/shim-3:$PATH" TMPDIR="$tmp" \
    bash "$st/$CUT" --execute --repo "$ws" level-0-start
  assert_grep "$CASE_TMP/run3.txt" "cut-level-branches: done."
  assert_grep "$CASE_TMP/run3.txt" \
    "cut-level-branches: warning: could not remove the temp folder $tmp/cut-level-branches."
  git -C "$ws" for-each-ref --format='%(refname)' refs/heads/solution refs/heads/start > "$CASE_TMP/branches.txt"
  assert_eq "$(wc -l < "$CASE_TMP/branches.txt" | tr -d ' ')" 6 "branches after the cut"
  rm -rf "$tmp"/cut-level-branches.*
  # apply: the backup folder cannot be removed after a successful apply; the apply exits 0 and says so.
  write_stage 'add|config|files/valid.txt|valid.txt|644|-'
  fail_shim "$CASE_TMP/shim-4" rm '-rf */apply-level.*'
  expect_ok "$CASE_TMP/run4.txt" env PATH="$CASE_TMP/shim-4:$PATH" TMPDIR="$tmp" bash "$ST/$APPLY" level-2 "$TGT"
  assert_grep "$CASE_TMP/run4.txt" "apply-level: level-2: 1 rows applied"
  assert_grep "$CASE_TMP/run4.txt" "apply-level: warning: could not remove the backup folder $tmp/apply-level."
  assert_file "$TGT/valid.txt"
}

# expect_usage <outfile> <prog> <cmd...>: the command must exit 2 and print the usage line of <prog>.
expect_usage() {
  local of=$1 prog=$2 rc
  shift 2
  set +e
  "$@" > "$of" 2>&1
  rc=$?
  set -e
  if [ "$rc" -ne 2 ]; then
    sed 's/^/      /' "$of" >&2
    die "expected exit 2 (usage), got $rc: $*"
  fi
  assert_grep "$of" "usage: $prog "
}

case_usage_on_help_unknown_options_and_missing_values() {
  local st="$CASE_TMP/stage" t="$CASE_TMP/target" ws="$CASE_TMP/workshop" o="$CASE_TMP/out" r="$CASE_TMP/usage"
  shared_source
  chain_stage "$st"
  new_target "$t"
  make_workshop "$ws"
  tree_state "$t" > "$CASE_TMP/target-before.txt"
  ref_state "$ws" > "$CASE_TMP/before.txt"
  # An unknown option is given with one argument too few, so it is refused as an option, not by the argument count.
  # apply-level.sh
  expect_usage "$r-a1.txt" apply-level bash "$st/$APPLY" -h
  expect_usage "$r-a2.txt" apply-level bash "$st/$APPLY" --help level-1 "$t"
  expect_usage "$r-a3.txt" apply-level bash "$st/$APPLY" --dry-run level-1
  expect_usage "$r-a4.txt" apply-level bash "$st/$APPLY" --group
  expect_usage "$r-a5.txt" apply-level bash "$st/$APPLY" level-1 "$t" extra
  # simulate-level-0.sh
  expect_usage "$r-s1.txt" simulate-level-0 bash "$SIM" -h
  expect_usage "$r-s2.txt" simulate-level-0 bash "$SIM" --help --repo "$SOURCE" src-tag "$o"
  expect_usage "$r-s3.txt" simulate-level-0 bash "$SIM" --dry-run "$o"
  expect_usage "$r-s4.txt" simulate-level-0 bash "$SIM" --repo
  expect_usage "$r-s5.txt" simulate-level-0 bash "$SIM" --repo "$SOURCE" src-tag "$o" extra
  assert_absent "$o"
  # cut-level-branches.sh
  expect_usage "$r-c1.txt" cut-level-branches bash "$st/$CUT" -h
  expect_usage "$r-c2.txt" cut-level-branches bash "$st/$CUT" --help --execute --repo "$ws" level-0-start
  expect_usage "$r-c3.txt" cut-level-branches bash "$st/$CUT" --execute --repo "$ws" --dry-run
  expect_usage "$r-c4.txt" cut-level-branches bash "$st/$CUT" --execute --repo
  expect_usage "$r-c5.txt" cut-level-branches bash "$st/$CUT" --execute --repo "$ws" level-0-start extra
  tree_state "$t" > "$CASE_TMP/target-after.txt"
  diff "$CASE_TMP/target-before.txt" "$CASE_TMP/target-after.txt" || die "a usage error changed the apply target"
  ref_state "$ws" > "$CASE_TMP/after.txt"
  diff "$CASE_TMP/before.txt" "$CASE_TMP/after.txt" || die "a usage error changed the workshop repository"
}

# ---------------------------------------------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------------------------------------------

echo "# test-staging: bash $(bash -c 'echo "$BASH_VERSION"'), $(git --version)"

run_case "simulate: built-in strip list equals spec 09 step 2" case_sim_builtin_list_equals_spec09
run_case "simulate: strips every listed path, keeps the rest, one tree" case_sim_strips_every_path_and_keeps_the_rest
run_case "simulate: single commit, reproducible, modes kept" case_sim_single_reproducible_commit
run_case "simulate: uses --list-paths of the ref when it answers" case_sim_uses_list_paths_of_the_ref
run_case "simulate: falls back to the built-in list when --list-paths fails" case_sim_falls_back_when_list_paths_fails
run_case "simulate: falls back to the built-in list when the publish script is absent" case_sim_reports_absent_publish_script
run_case "simulate: refuses unsafe or empty --list-paths output" case_sim_refuses_unsafe_list_paths_output
run_case "simulate: refuses a non-empty out-dir" case_sim_refuses_non_empty_out_dir
run_case "simulate: fills an existing empty out-dir" case_sim_into_an_existing_empty_out_dir
run_case "simulate: refuses a non-repo source, a file or link as out-dir, a missing parent and missing arguments" case_sim_refuses_bad_arguments
run_case "simulate: refuses an out-dir inside the source repo" case_sim_refuses_out_dir_inside_source
run_case "simulate: refuses an unknown ref" case_sim_refuses_unknown_ref
run_case "simulate: source repo refs, worktrees, tags and status unchanged" case_sim_leaves_source_refs_untouched
run_case "apply: add decodes dot- and CLAUDE.staged.md, leaves changes uncommitted" case_apply_add_decodes_paths_and_leaves_changes_uncommitted
run_case "apply: replace overwrites an existing target" case_apply_replace_overwrites_an_existing_target
run_case "apply: modes 644 and 755 come from the manifest" case_apply_sets_modes_from_the_manifest
run_case "apply: --group config and --group exercise filter rows" case_apply_group_filter
run_case "apply: --group with no rows in the step applies nothing" case_apply_group_without_rows_applies_nothing
run_case "apply: add refuses an existing target" case_apply_add_refuses_an_existing_target
run_case "apply: replace refuses a missing target" case_apply_replace_refuses_a_missing_target
run_case "apply: refuses staged paths that break the encoding rule" case_apply_refuses_rule_breaking_staged_paths
run_case "apply: refuses absolute, '..', '.git' and empty-component targets" case_apply_refuses_unsafe_targets
run_case "apply: refuses a file as target folder, a folder or link as replace or patch target" case_apply_refuses_targets_that_are_not_regular_files
run_case "apply: refuses malformed manifest rows" case_apply_refuses_malformed_rows
run_case "apply: refuses staged files no manifest row names" case_apply_refuses_unlisted_staged_files
run_case "apply: refuses to write through a symbolic link" case_apply_refuses_a_symlinked_target_folder
run_case "apply: refuses an unknown step, a non-root target and a missing manifest" case_apply_refuses_bad_arguments
run_case "apply: patch on its base blob" case_apply_patch_on_its_base
run_case "apply: patch merges 3-way when the target moved on" case_apply_patch_three_way_when_the_target_moved_on
run_case "apply: patch refuses when the target differs and the base blob is missing" case_apply_patch_refuses_a_missing_base_blob
run_case "apply: patch refuses an unclean 3-way merge, target unchanged (atomic)" case_apply_patch_refuses_a_conflict_atomically
run_case "apply: patch refuses a patch of another path" case_apply_patch_refuses_a_patch_of_another_path
run_case "apply: patch refuses a base sha that does not match the patch index line, target unchanged" case_apply_patch_refuses_a_base_that_does_not_match_the_patch
run_case "apply: patch refuses no index line, a patch that misses its own base and a non-patch" case_apply_patch_refuses_patches_it_cannot_trust
run_case "build: chain of every step, one commit per step, fixed messages" case_build_commits_the_chain_one_commit_per_step
run_case "build: refuses a target with uncommitted changes" case_build_refuses_a_dirty_target
run_case "build: refuses a step whose target is git-ignored" case_build_refuses_a_git_ignored_target
run_case "build: stops at a failing step and names its manifest line" case_build_stops_at_a_failing_step_naming_the_line
run_case "build: refuses an unknown step and bad arguments" case_build_refuses_bad_arguments
run_case "build: refuses a repo without a commit, a plain folder and a subfolder" case_build_refuses_a_target_that_is_not_a_level_0_repo
run_case "cut: dry run by default, prints every command, changes nothing" case_cut_dry_run_is_the_default_and_changes_nothing
run_case "cut: --execute creates exactly the six local branches; no push, tags unchanged" case_cut_execute_creates_six_local_branches_only
run_case "cut: refuses when any target branch exists (dry run and --execute)" case_cut_refuses_when_a_target_branch_exists
run_case "cut: a failing step creates no branch" case_cut_creates_nothing_when_a_step_fails
run_case "cut: refuses an unknown ref, unknown options and a missing step" case_cut_refuses_bad_arguments
run_case "eol: S/.gitattributes gives LF for *.sh and dot-githooks/*, CRLF for *.ps1 (fresh clone, od -c)" case_eol_staging_folder_gitattributes
run_case "eol: a clone of a built level keeps LF, CRLF and mode 755" case_eol_and_modes_on_a_clone_of_a_built_level
run_case "scripts: mode 755, LF, bash shebang, strict mode" case_staging_scripts_are_executable_lf_bash
run_case "temp folders: every one uses a template under \${TMPDIR:-/tmp} (scripts, test, README)" case_temp_folders_use_a_template_under_tmpdir
run_case "temp folders: apply, simulate and cut leave nothing behind in TMPDIR" case_scripts_leave_no_temp_files_behind
run_case "README: the cut procedure names only local operations; pushes are a separate section" case_readme_cut_procedure_names_only_local_operations
run_case "cut: a failing last level-4 track step creates no branch" case_cut_creates_nothing_when_the_last_level_4_step_fails
run_case "apply: a write that fails after a patch row rolls back files, folders, the index and the backup" case_apply_rolls_back_a_failing_write_after_a_patch_row
run_case "apply: an add, a folder or a mode that cannot be written rolls back; a failed backup writes nothing" case_apply_rolls_back_every_other_failing_write
run_case "apply: a restore step that fails is named, the rest of the rollback still runs" case_apply_reports_an_incomplete_rollback_and_still_finishes_it
run_case "cut: any internal git command that fails creates no branch" case_cut_creates_nothing_when_an_internal_git_command_fails
run_case "cleanup: a removal that fails is reported; the rest of the cleanup still runs" case_cleanup_failures_are_reported_and_the_rest_still_runs
run_case "usage: apply, simulate and cut exit 2 on -h, unknown options, missing values and extra arguments" case_usage_on_help_unknown_options_and_missing_values

echo "1..$CASE_NO"
if [ "$FAILED" -ne 0 ]; then
  echo "FAILED $FAILED of $CASE_NO:$FAILED_NAMES"
  exit 1
fi
echo "PASSED all $CASE_NO cases"
