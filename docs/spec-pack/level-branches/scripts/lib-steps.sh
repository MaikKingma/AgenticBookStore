#!/usr/bin/env bash
# lib-steps.sh: step names, chain, commit messages and branch names shared by the level-branches scripts.
# Sourced by apply-level.sh, build-level.sh and cut-level-branches.sh; running it on its own does nothing.

ALL_STEPS='level-1 level-2 level-3 level-4-openspec level-4-speckit level-4-agentos'

is_step() {
  case $1 in
    level-1|level-2|level-3|level-4-openspec|level-4-speckit|level-4-agentos) return 0 ;;
    *) return 1 ;;
  esac
}

# step_chain <step>: the steps applied, in order, on top of level-0 to build <step>.
step_chain() {
  case $1 in
    level-1) echo 'level-1' ;;
    level-2) echo 'level-1 level-2' ;;
    level-3) echo 'level-1 level-2 level-3' ;;
    level-4-openspec|level-4-speckit|level-4-agentos) echo "level-1 level-2 level-3 $1" ;;
    *) return 1 ;;
  esac
}

# step_message <step>: the fixed commit message of <step>.
step_message() {
  case $1 in
    level-1) echo 'Level 1 solution: context engineering' ;;
    level-2) echo 'Level 2 solution: give your agent eyes (LSP + MCP)' ;;
    level-3) echo 'Level 3 solution: behavioral control' ;;
    level-4-openspec) echo 'Level 4 start: OpenSpec installed and initialised' ;;
    level-4-speckit) echo 'Level 4 start: Spec Kit installed and initialised' ;;
    level-4-agentos) echo 'Level 4 start: Agent OS installed and initialised' ;;
    *) return 1 ;;
  esac
}

# step_branch <step>: the branch that holds <step>.
step_branch() {
  case $1 in
    level-1|level-2|level-3) echo "solution/$1" ;;
    level-4-openspec|level-4-speckit|level-4-agentos) echo "start/$1" ;;
    *) return 1 ;;
  esac
}

# manifest_targets <step-dir>: the target path of every manifest row (all groups), one per line.
manifest_targets() {
  awk -F '\t' '/^#/ { next } NF { print $4 }' "$1/manifest.tsv"
}

# commit_step <stage-root> <step> <repo>: apply <step> to <repo> and commit it with the step's fixed message.
# The caller defines run(), which prints and/or executes a command, and sets EXECUTE=1 when commands really run.
# Every failure returns non-zero explicitly, because callers use `commit_step ... || die`, which turns off errexit.
commit_step() {
  local stage_root=$1 step=$2 repo=$3 message
  message=$(step_message "$step") || return 1
  run bash "$stage_root/scripts/apply-level.sh" "$step" "$repo" || return 1
  run git -C "$repo" add --all || return 1
  run git -C "$repo" commit --quiet -m "$message" || return 1
  if [ "${EXECUTE:-1}" = 1 ]; then
    check_step_commit "$stage_root" "$step" "$repo" || return 1
  fi
}

# check_step_commit <stage-root> <step> <repo>: HEAD changes exactly the manifest targets and the tree is clean.
check_step_commit() {
  local stage_root=$1 step=$2 repo=$3 expected actual
  expected=$(manifest_targets "$stage_root/$step" | LC_ALL=C sort)
  actual=$(git -C "$repo" -c core.quotePath=false diff-tree --no-commit-id --name-only -r HEAD | LC_ALL=C sort)
  if [ "$expected" != "$actual" ]; then
    echo "error: the $step commit does not change exactly the manifest targets (is a target git-ignored?)" >&2
    echo "expected:" >&2
    printf '%s\n' "$expected" | sed 's/^/  /' >&2
    echo "committed:" >&2
    printf '%s\n' "$actual" | sed 's/^/  /' >&2
    return 1
  fi
  if [ -n "$(git -C "$repo" status --porcelain)" ]; then
    echo "error: $repo is not clean after the $step commit" >&2
    return 1
  fi
}
