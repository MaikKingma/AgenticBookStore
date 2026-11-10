#!/usr/bin/env bash
# Publishes the workshop baseline: builds branch main as one parentless commit made from branch factory
# without the agent configuration paths below, and moves tag level-0-start to it.
# Runs on macOS /bin/bash 3.2 with BSD tools and on Linux bash 5 with GNU tools.
set -euo pipefail

# The single list of paths that never reach main. --list-paths prints it and --check (the CI guard) reads it.
STRIPPED_PATHS='CLAUDE.md
CLAUDE.local.md
AGENTS.md
.claude
.mcp.json
openspec
.specify
specs
agent-os
.githooks
docs/spec-pack
guide
.github/workflows/guide.yml'

PROGRAM=publish-workshop-main
SOURCE_BRANCH=factory
TARGET_BRANCH=main
TAG=level-0-start
MESSAGE='Agentic Bookstore: workshop baseline'

die() {
  printf '%s: %s\n' "$PROGRAM" "$*" >&2
  exit 1
}

usage() {
  cat <<'EOF'
Usage: scripts/publish-workshop-main.sh [--push [--remote <name>]] [--dry-run]
       scripts/publish-workshop-main.sh --list-paths
       scripts/publish-workshop-main.sh --check <revision>

Without options: on a clean checkout of branch factory, builds main as one parentless commit without the
listed paths and moves the local branch main and the local tag level-0-start to it. No remote is contacted.
  --push           also update main and level-0-start on the remote in one atomic push with leases;
                   refused when the local factory differs from the remote's factory
  --remote <name>  the remote for --push (default: origin)
  --dry-run        show what would happen, change no ref
  --list-paths     print the paths that never reach main
  --check <rev>    print every tracked path of <rev> under a listed path; exit 1 when there is one
EOF
}

list_paths() {
  printf '%s\n' "$STRIPPED_PATHS"
}

# Prints every tracked path of <revision> that lies under a listed path, relative to the top of the
# repository. Returns 1 when there is one, 0 otherwise. Must run from the top of the repository.
check_revision() {
  local rev=$1 found='' p files
  git rev-parse --verify --quiet "$rev^{commit}" >/dev/null || die "unknown revision: $rev"
  while IFS= read -r p; do
    files=$(git ls-tree -r --name-only "$rev" -- "$p")
    if [ -n "$files" ]; then
      found="$found$files
"
    fi
  done <<EOF
$STRIPPED_PATHS
EOF
  if [ -n "$found" ]; then
    printf '%s: agent configuration found in %s:\n%s' "$PROGRAM" "$rev" "$found" >&2
    return 1
  fi
  printf 'No agent configuration in %s.\n' "$rev"
}

# Prints the object id of ref <name> from git ls-remote output, or nothing when the remote does not have it.
ref_from_listing() {
  printf '%s\n' "$1" | awk -v name="$2" '$2 == name { print $1 }'
}

mode=publish
check_rev=''
dry_run=no
push=no
remote=''
while [ $# -gt 0 ]; do
  case $1 in
    --dry-run) dry_run=yes ;;
    --push) push=yes ;;
    --remote)
      if [ $# -lt 2 ] || [ -z "$2" ]; then
        printf '%s: --remote needs a remote name\n' "$PROGRAM" >&2
        usage >&2
        exit 2
      fi
      remote=$2
      shift
      ;;
    --list-paths) mode=list ;;
    --check)
      [ $# -ge 2 ] || die '--check needs a revision'
      mode=check
      check_rev=$2
      shift
      ;;
    -h | --help)
      usage
      exit 0
      ;;
    *)
      usage >&2
      exit 2
      ;;
  esac
  shift
done

if [ -n "$remote" ] && [ "$push" = no ]; then
  printf '%s: --remote only works together with --push\n' "$PROGRAM" >&2
  usage >&2
  exit 2
fi
remote=${remote:-origin}

if [ "$mode" = list ]; then
  list_paths
  exit 0
fi

top=$(git rev-parse --show-toplevel) || die 'not inside a git work tree'
# Paths given to git ls-tree are relative to the current directory, so everything runs from the top.
cd "$top"

if [ "$mode" = check ]; then
  rc=0
  check_revision "$check_rev" || rc=$?
  exit "$rc"
fi

# --- Refusals: nothing has been changed yet. ----------------------------------------------------------------

head_ref=$(git symbolic-ref --quiet HEAD || true)
[ "$head_ref" = "refs/heads/$SOURCE_BRANCH" ] \
  || die "run this on branch $SOURCE_BRANCH (HEAD is ${head_ref:-detached})"

# --untracked-files=normal reports untracked files even when the repository config hides them; ignored files
# never count.
dirty=$(git status --porcelain --untracked-files=normal)
if [ -n "$dirty" ]; then
  printf '%s\n' "$dirty" >&2
  die 'the working tree is not clean (tracked changes or untracked files)'
fi

# Moving a branch that another worktree has checked out would leave that worktree's index stale.
while IFS= read -r line; do
  if [ "$line" = "branch refs/heads/$TARGET_BRANCH" ]; then
    die "branch $TARGET_BRANCH is checked out in a worktree; switch that worktree to another branch first"
  fi
done <<EOF
$(git worktree list --porcelain)
EOF

old_main=$(git rev-parse --verify --quiet "refs/heads/$TARGET_BRANCH" || true)
old_tag=$(git rev-parse --verify --quiet "refs/tags/$TAG" || true)

# Only --push talks to a remote. Its refs are read once; the push later expects exactly these values.
remote_main=''
remote_tag=''
if [ "$push" = yes ]; then
  remote_refs=$(git ls-remote "$remote" "refs/heads/$SOURCE_BRANCH" "refs/heads/$TARGET_BRANCH" "refs/tags/$TAG") \
    || die "cannot read the refs of remote $remote"
  remote_factory=$(ref_from_listing "$remote_refs" "refs/heads/$SOURCE_BRANCH")
  remote_main=$(ref_from_listing "$remote_refs" "refs/heads/$TARGET_BRANCH")
  remote_tag=$(ref_from_listing "$remote_refs" "refs/tags/$TAG")
  local_factory=$(git rev-parse "refs/heads/$SOURCE_BRANCH")
  [ "$local_factory" = "$remote_factory" ] \
    || die "$SOURCE_BRANCH ($local_factory) differs from $SOURCE_BRANCH on $remote (${remote_factory:-absent}); push or pull it first"
fi

# --- Build the commit in a temporary worktree of factory. --------------------------------------------------

work=''
cleanup() {
  if [ -n "$work" ]; then
    git worktree remove --force "$work/tree" >/dev/null 2>&1 || true
    rm -rf "$work"
    git worktree prune >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
work=$(mktemp -d "${TMPDIR:-/tmp}/$PROGRAM.XXXXXX")

git worktree add --quiet --detach "$work/tree" "refs/heads/$SOURCE_BRANCH"
removed=''
while IFS= read -r p; do
  if [ -n "$(git -C "$work/tree" ls-files -- "$p")" ]; then
    git -C "$work/tree" rm -r -q -- "$p"
    removed="${removed}removed: $p
"
  fi
done <<EOF
$STRIPPED_PATHS
EOF
tree=$(git -C "$work/tree" write-tree)

# An existing baseline is reused when it is one parentless commit with this tree and the baseline message, so a
# rerun, or a factory change only inside the listed paths, publishes the same commit again.
is_baseline() {
  local c=$1
  [ -n "$c" ] || return 1
  git cat-file -e "$c^{commit}" 2>/dev/null || return 1
  [ "$(git rev-parse "$c^{tree}")" = "$tree" ] || return 1
  ! git rev-parse --verify --quiet "$c^1" >/dev/null || return 1
  [ "$(git log -1 --format=%B "$c")" = "$MESSAGE" ]
}

commit=''
if [ "$push" = yes ] && is_baseline "$remote_main"; then
  commit=$remote_main
elif is_baseline "$old_main"; then
  commit=$old_main
fi
if [ -n "$commit" ]; then
  printf '%s already matches %s without the listed paths; reusing %s.\n' "$TARGET_BRANCH" "$SOURCE_BRANCH" "$commit"
fi

if [ "$dry_run" = yes ]; then
  printf 'This is a dry run; no ref changes.\n'
  printf 'Local %s and %s would move to %s (now %s / %s).\n' "$TARGET_BRANCH" "$TAG" \
    "${commit:-a new commit of tree $tree}" "${old_main:-none}" "${old_tag:-none}"
  if [ "$push" = yes ]; then
    printf '%s and %s on %s would move there too (now %s / %s).\n' "$TARGET_BRANCH" "$TAG" "$remote" \
      "${remote_main:-none}" "${remote_tag:-none}"
  fi
  printf '%s' "$removed" | sed 's/^removed: /would remove: /'
  exit 0
fi

if [ -z "$commit" ]; then
  commit=$(git commit-tree "$tree" -m "$MESSAGE")
fi
check_revision "$commit" >/dev/null || die "internal error: $commit still holds agent configuration"

# --- Push (only with --push), move the local refs, then report. --------------------------------------------

# One atomic push: both refs move or neither does. Each lease names the value read above (empty means the ref
# must not exist), so a remote that changed in the meantime rejects the push instead of being overwritten.
if [ "$push" = yes ]; then
  git push --atomic \
    "--force-with-lease=refs/heads/$TARGET_BRANCH:$remote_main" \
    "--force-with-lease=refs/tags/$TAG:$remote_tag" \
    "$remote" "$commit:refs/heads/$TARGET_BRANCH" "$commit:refs/tags/$TAG" \
    || die "the push to $remote failed; no ref moved, locally or on $remote"
fi

# Both local refs move in one transaction, and only if they still hold the values read at the start. The zero
# id (all zeros, as long as this repository's object ids) means "must not exist yet".
zero=$(git hash-object -t blob --stdin </dev/null | tr '0-9a-f' '0')
printf 'update refs/heads/%s %s %s\nupdate refs/tags/%s %s %s\n' \
  "$TARGET_BRANCH" "$commit" "${old_main:-$zero}" "$TAG" "$commit" "${old_tag:-$zero}" | git update-ref --stdin

printf '%s and %s now at %s (previous local %s: %s).\n' "$TARGET_BRANCH" "$TAG" "$commit" "$TARGET_BRANCH" \
  "${old_main:-none}"
if [ "$push" = yes ]; then
  printf 'Pushed to %s (previous %s there: %s).\n' "$remote" "$TARGET_BRANCH" "${remote_main:-none}"
fi
if [ -n "$removed" ]; then
  printf '%s' "$removed"
else
  printf 'No listed path was present in %s.\n' "$SOURCE_BRANCH"
fi
