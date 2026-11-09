#!/usr/bin/env bash
# simulate-level-0.sh: build a scratch git repo that stands in for tag level-0-start.
#
# Usage: simulate-level-0.sh [--repo <source-repo>] <ref> <out-dir>
#
# The single commit of <out-dir> holds the tree of <ref> minus the spec 09 strip list. The source repository is only
# read (git rev-parse, git archive); nothing is written to it. When <ref> contains scripts/publish-workshop-main.sh and
# `publish-workshop-main.sh --list-paths` exits 0 with one repo-relative path per line, that list is used; otherwise
# the built-in copy below. The output names the list used ("list: ...") and every path of it ("strip-path: ...").
set -euo pipefail

PROG=simulate-level-0

# Built-in copy of the strip list in docs/spec-pack/specs/09-workshop-baseline.md, section "Publishing main",
# step 2. Same order and spelling as the spec; tests/test-staging.sh diffs the two.
BUILTIN_STRIP_LIST='CLAUDE.md
CLAUDE.local.md
AGENTS.md
.claude/
.mcp.json
openspec/
.specify/
specs/
agent-os/
.githooks/
docs/spec-pack/'

# Fixed identity and date, so the same <ref> always gives the same commit.
SIM_NAME='Level 0 Simulator'
SIM_EMAIL='simulate-level-0@localhost'
SIM_DATE='2026-01-01T00:00:00Z'

usage() {
  echo "usage: $PROG [--repo <source-repo>] <ref> <out-dir>" >&2
  exit 2
}

OUT_CREATED=0
OUT_ABS=""
DONE=0

die() {
  echo "$PROG: error: $*" >&2
  exit 1
}

cleanup() {
  local rc=$?
  # No errexit here: every cleanup step runs, even after one fails, and a failure is reported.
  set +e
  if [ "$DONE" != 1 ] && [ -n "$OUT_ABS" ]; then
    if [ "$OUT_CREATED" = 1 ]; then
      rm -rf "$OUT_ABS" || echo "$PROG: error: could not remove the out-dir; remove it by hand: $OUT_ABS" >&2
    else
      find "$OUT_ABS" -mindepth 1 -maxdepth 1 -exec rm -rf {} + ||
        echo "$PROG: error: could not empty the out-dir; empty it by hand: $OUT_ABS" >&2
    fi
  fi
  if [ -n "${ERR_FILE:-}" ]; then
    rm -f "$ERR_FILE" || echo "$PROG: warning: could not remove the temp file $ERR_FILE" >&2
  fi
  exit "$rc"
}
trap cleanup EXIT

# list_problem <path>: prints why a strip-list entry is unsafe, nothing when it is fine.
list_problem() {
  local p=$1 rest c
  case $p in
    '') echo "empty path"; return ;;
    /*) echo "absolute path"; return ;;
    *"$(printf '\r')"*) echo "carriage return in path"; return ;;
  esac
  rest=${p%/}
  while :; do
    c=${rest%%/*}
    case $c in
      '') echo "empty path component"; return ;;
      .|..) echo "'$c' path component"; return ;;
    esac
    [ "$rest" = "${rest#*/}" ] && break
    rest=${rest#*/}
  done
}

repo=.
while [ $# -gt 0 ]; do
  case $1 in
    --repo)
      [ $# -ge 2 ] || usage
      repo=$2
      shift 2
      ;;
    -h|--help) usage ;;
    --) shift; break ;;
    -*) usage ;;
    *) break ;;
  esac
done
[ $# -eq 2 ] || usage
ref=$1
out=$2

src_top=$(git -C "$repo" rev-parse --show-toplevel 2>/dev/null) || die "not a git work tree: $repo"
src_top=$(cd "$src_top" && pwd -P)
sha=$(git -C "$src_top" rev-parse --verify --quiet "$ref^{commit}") || die "unknown ref: $ref"

if [ -e "$out" ] || [ -L "$out" ]; then
  [ -d "$out" ] && [ ! -L "$out" ] || die "out-dir exists and is not a folder: $out"
  [ -z "$(ls -A "$out")" ] || die "out-dir is not empty: $out"
  out_resolved=$(cd "$out" && pwd -P)
else
  out_resolved=$(cd "$(dirname "$out")" 2>/dev/null && pwd -P) || die "parent of out-dir does not exist: $out"
  out_resolved="$out_resolved/$(basename "$out")"
fi
case "$out_resolved/" in
  "$src_top"/*) die "out-dir must be outside the source repository: $out" ;;
esac
if [ ! -d "$out" ]; then
  mkdir "$out"
  OUT_CREATED=1
fi
OUT_ABS=$(cd "$out" && pwd -P)

git -C "$src_top" archive --format=tar "$sha" | tar -xf - -C "$OUT_ABS"

strip_list=$BUILTIN_STRIP_LIST
list_name="built-in"
publish="$OUT_ABS/scripts/publish-workshop-main.sh"
if [ -f "$publish" ]; then
  ERR_FILE=$(mktemp "${TMPDIR:-/tmp}/$PROG.XXXXXX")
  # Run the ref's own script on the extracted copy; git discovery stops at the out-dir.
  if listed=$(
    cd "$OUT_ABS"
    unset GIT_DIR GIT_WORK_TREE GIT_INDEX_FILE
    GIT_CEILING_DIRECTORIES=$(dirname "$OUT_ABS") bash "$publish" --list-paths 2> "$ERR_FILE"
  ); then
    count=0
    while IFS= read -r line; do
      [ -n "$line" ] || continue
      problem=$(list_problem "$line")
      [ -z "$problem" ] || die "scripts/publish-workshop-main.sh --list-paths printed an unsafe path ($problem): '$line'"
      count=$((count + 1))
    done <<EOF
$listed
EOF
    [ "$count" -gt 0 ] || die "scripts/publish-workshop-main.sh --list-paths exited 0 but printed no path"
    strip_list=$(printf '%s\n' "$listed" | sed '/^$/d')
    list_name="scripts/publish-workshop-main.sh --list-paths"
  else
    rc=$?
    echo "$PROG: scripts/publish-workshop-main.sh --list-paths failed (exit $rc); using the built-in list"
    sed 's/^/  stderr: /' "$ERR_FILE"
  fi
else
  echo "$PROG: no scripts/publish-workshop-main.sh in $ref; using the built-in list"
fi

echo "list: $list_name"
while IFS= read -r p; do
  echo "strip-path: $p"
  target="$OUT_ABS/${p%/}"
  if [ -e "$target" ] || [ -L "$target" ]; then
    rm -rf "$target"
    echo "removed: $p"
  fi
done <<EOF
$strip_list
EOF

git -C "$OUT_ABS" init -q
git -C "$OUT_ABS" symbolic-ref HEAD refs/heads/main
git -C "$OUT_ABS" add --all --force
GIT_AUTHOR_NAME=$SIM_NAME GIT_AUTHOR_EMAIL=$SIM_EMAIL GIT_AUTHOR_DATE=$SIM_DATE \
  GIT_COMMITTER_NAME=$SIM_NAME GIT_COMMITTER_EMAIL=$SIM_EMAIL GIT_COMMITTER_DATE=$SIM_DATE \
  git -C "$OUT_ABS" -c commit.gpgsign=false commit -q -m "Agentic Bookstore: workshop baseline (simulated from $sha)"

DONE=1
echo "simulated level-0: $OUT_ABS at $(git -C "$OUT_ABS" rev-parse HEAD) (from $ref $sha)"
