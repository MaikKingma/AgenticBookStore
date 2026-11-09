#!/usr/bin/env bash
# build-level.sh: build one level on a level-0 repository by applying its chain, one commit per step.
#
# Usage: build-level.sh <step> <target-repo>
#
# <target-repo> holds a level-0 tree (for example from simulate-level-0.sh) with a clean working tree. The chain is
# level-1 -> level-2 -> level-3 -> one level-4 track (see ../README.md). Each step is applied with apply-level.sh and
# committed with the step's fixed message, using the caller's git identity. A failing step stops the build: the
# commits of the earlier steps stay, the failing step leaves the tree unchanged.
set -euo pipefail

PROG=build-level
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
STAGE_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)
# shellcheck source=lib-steps.sh
. "$SCRIPT_DIR/lib-steps.sh"

usage() {
  echo "usage: $PROG <step> <target-repo>" >&2
  echo "steps: $ALL_STEPS" >&2
  exit 2
}

die() {
  echo "$PROG: error: $*" >&2
  exit 1
}

# run <cmd...>: commands of commit_step run as they are.
run() {
  "$@"
}
EXECUTE=1

[ $# -eq 2 ] || usage
step=$1
target=$2
is_step "$step" || die "unknown step: $step (expected one of: $ALL_STEPS)"
[ -d "$target" ] || die "target is not a folder: $target"
top=$(git -C "$target" rev-parse --show-toplevel 2>/dev/null) || die "not a git work tree: $target"
[ "$(cd "$top" && pwd -P)" = "$(cd "$target" && pwd -P)" ] || die "target must be the top level of its work tree: $target"
git -C "$target" rev-parse --verify --quiet HEAD > /dev/null || die "target has no commit: $target"
[ -z "$(git -C "$target" status --porcelain --untracked-files=all)" ] || die "target has uncommitted changes: $target"

for s in $(step_chain "$step"); do
  commit_step "$STAGE_ROOT" "$s" "$target" || die "step $s failed"
  echo "$PROG: committed $s as $(git -C "$target" rev-parse --short HEAD): $(step_message "$s")"
done
echo "$PROG: $step built in $target"
