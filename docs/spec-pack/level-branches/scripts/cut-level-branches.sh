#!/usr/bin/env bash
# cut-level-branches.sh: create the six level branches as LOCAL branches on top of a level-0 ref.
#
# Usage: cut-level-branches.sh [--execute] [--repo <repo>] <level-0-ref>
#
# Creates solution/level-1, solution/level-2, solution/level-3 (each one commit on the previous one, level-1 on
# <level-0-ref>) and start/level-4-openspec, start/level-4-speckit, start/level-4-agentos (each one commit on
# solution/level-3). Dry run by default: it prints every command and changes nothing. With --execute it builds the
# commits in a temporary clone, then fetches them into <repo> and creates the six branches in one atomic ref
# transaction. It refuses when any of the six branches exists. It never pushes and never creates, moves or deletes a
# tag. <repo> defaults to the repository of the current folder.
set -euo pipefail

PROG=cut-level-branches
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
STAGE_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)
# shellcheck source=lib-steps.sh
. "$SCRIPT_DIR/lib-steps.sh"

usage() {
  echo "usage: $PROG [--execute] [--repo <repo>] <level-0-ref>" >&2
  exit 2
}

die() {
  echo "$PROG: error: $*" >&2
  exit 1
}

# quote_arg <arg>: the argument as a shell word.
quote_arg() {
  case $1 in
    '') printf "''" ;;
    *[!A-Za-z0-9_./:=@%+,-]*) printf "'%s'" "$(printf '%s' "$1" | sed "s/'/'\\\\''/g")" ;;
    *) printf '%s' "$1" ;;
  esac
}

# run <cmd...>: prints the command; runs it only with --execute.
run() {
  local a
  printf '+'
  for a in "$@"; do
    printf ' '
    quote_arg "$a"
  done
  printf '\n'
  if [ "$EXECUTE" = 1 ]; then
    "$@"
  fi
}

EXECUTE=0
repo=.
while [ $# -gt 0 ]; do
  case $1 in
    --execute) EXECUTE=1; shift ;;
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
[ $# -eq 1 ] || usage
ref=$1

top=$(git -C "$repo" rev-parse --show-toplevel 2>/dev/null) || die "not a git work tree: $repo"
sha=$(git -C "$top" rev-parse --verify --quiet "$ref^{commit}") || die "unknown ref: $ref"

for s in $ALL_STEPS; do
  [ -f "$STAGE_ROOT/$s/manifest.tsv" ] || die "$s/manifest.tsv is missing"
done
for s in $ALL_STEPS; do
  b=$(step_branch "$s")
  if git -C "$top" show-ref --verify --quiet "refs/heads/$b"; then
    die "branch $b already exists; refusing to cut (nothing was changed)"
  fi
done

WORK_PARENT=""
cleanup() {
  local rc=$?
  # No errexit here: a temp folder that cannot be removed is reported and does not change the exit status.
  set +e
  if [ -n "$WORK_PARENT" ]; then
    rm -rf "$WORK_PARENT" || echo "$PROG: warning: could not remove the temp folder $WORK_PARENT" >&2
  fi
  exit "$rc"
}
trap cleanup EXIT

if [ "$EXECUTE" = 1 ]; then
  WORK_PARENT=$(mktemp -d "${TMPDIR:-/tmp}/$PROG.XXXXXX")
  WORK="$WORK_PARENT/work"
  echo "$PROG: creating local branches in $top"
else
  WORK='<tmp>/work'
  echo "$PROG: dry run; nothing is created. Re-run with --execute to create the branches."
fi
echo "$PROG: level-0 = $ref ($sha)"

# Build every step commit in a throwaway clone; the repository itself only receives objects and six new refs.
run git clone --quiet --no-checkout --no-tags -- "$top" "$WORK" || die "clone failed"
for key in user.name user.email; do
  value=$(git -C "$top" config --get "$key" || true)
  if [ -n "$value" ]; then
    run git -C "$WORK" config "$key" "$value" || die "cannot set $key in the work clone"
  fi
done
run git -C "$WORK" checkout --quiet --detach "$sha" || die "checkout of $sha failed"
for s in level-1 level-2 level-3; do
  commit_step "$STAGE_ROOT" "$s" "$WORK" || die "step $s failed; no branch was created"
  run git -C "$WORK" branch "$(step_branch "$s")" || die "branch in the work clone failed"
done
for s in level-4-openspec level-4-speckit level-4-agentos; do
  run git -C "$WORK" checkout --quiet --detach "$(step_branch level-3)" || die "checkout of solution/level-3 failed"
  commit_step "$STAGE_ROOT" "$s" "$WORK" || die "step $s failed; no branch was created"
  run git -C "$WORK" branch "$(step_branch "$s")" || die "branch in the work clone failed"
done

FETCH_REFS=()
for s in $ALL_STEPS; do
  FETCH_REFS[${#FETCH_REFS[@]}]="refs/heads/$(step_branch "$s")"
done
run git -C "$top" fetch --quiet --no-tags --no-write-fetch-head -- "$WORK" "${FETCH_REFS[@]}" || die "fetch failed"

# One atomic transaction; "create" fails if a branch appeared in the meantime.
transaction=""
for s in $ALL_STEPS; do
  b=$(step_branch "$s")
  if [ "$EXECUTE" = 1 ]; then
    commit=$(git -C "$WORK" rev-parse --verify "refs/heads/$b^{commit}") || die "cannot resolve $b in the work clone"
  else
    commit="<commit of $b>"
  fi
  transaction="${transaction}create refs/heads/$b $commit
"
done
echo "+ git -C $(quote_arg "$top") update-ref --stdin <<'EOF'"
printf '%s' "$transaction"
echo "EOF"
if [ "$EXECUTE" = 1 ]; then
  printf '%s' "$transaction" | git -C "$top" update-ref --stdin || die "creating the branches failed; none was created"
  for s in $ALL_STEPS; do
    b=$(step_branch "$s")
    echo "$PROG: created $b $(git -C "$top" log -1 --format='%h %s' "refs/heads/$b")"
  done
  echo "$PROG: done. Local branches only; nothing was pushed (see README, publishing the branches)."
else
  echo "$PROG: dry run complete. Re-run with --execute to create the branches."
fi
