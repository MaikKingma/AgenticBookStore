#!/usr/bin/env bash
# Tests scripts/publish-workshop-main.sh against temporary repositories only.
# Every repository, including the bare remotes, lives under one mktemp directory that is removed on exit.
# The test never runs git against the repository that holds this file and never pushes anywhere else.
# Runs on macOS /bin/bash 3.2 with BSD tools and on Linux bash 5 with GNU tools. The script under test runs with
# "$BASH", the interpreter that runs this test, so /bin/bash scripts/test-publish-workshop-main.sh tests the
# script on bash 3.2 even when another bash comes first on PATH.
set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd -P)
SCRIPT="$script_dir/publish-workshop-main.sh"

root=$(mktemp -d "${TMPDIR:-/tmp}/publish-workshop-main-test.XXXXXX")
root=$(cd "$root" && pwd -P)
finished=no
on_exit() {
  if [ "$finished" = no ]; then
    printf 'test aborted by a failing setup command after %d assertions\n' "${assertions:-0}"
  fi
  rm -rf "$root"
}
trap on_exit EXIT

# Hermetic git: no user, XDG or system configuration, a fixed identity, no hooks or signing from outside.
mkdir -p "$root/home/.config" "$root/tmp"
export HOME="$root/home"
export XDG_CONFIG_HOME="$root/home/.config"
export GIT_CONFIG_GLOBAL="$root/home/gitconfig"
: >"$GIT_CONFIG_GLOBAL"
export GIT_CONFIG_NOSYSTEM=1
export GIT_CEILING_DIRECTORIES="$root"
export GIT_TERMINAL_PROMPT=0
export GIT_AUTHOR_NAME='Publish Test'
export GIT_AUTHOR_EMAIL='publish-test@example.com'
export GIT_COMMITTER_NAME='Publish Test'
export GIT_COMMITTER_EMAIL='publish-test@example.com'
unset GIT_DIR GIT_WORK_TREE GIT_INDEX_FILE GIT_OBJECT_DIRECTORY GIT_ALTERNATE_OBJECT_DIRECTORIES
unset GIT_CONFIG_COUNT GIT_CONFIG_PARAMETERS GIT_NAMESPACE GIT_PREFIX
# The script creates its own temporary directory here, so the test can prove it is removed.
export TMPDIR="$root/tmp"

# The 11 paths the script must strip, in spec order. The test keeps its own copy as the oracle.
EXPECTED_PATHS='CLAUDE.md
CLAUDE.local.md
AGENTS.md
.claude
.mcp.json
openspec
.specify
specs
agent-os
.githooks
docs/spec-pack'

MESSAGE='Agentic Bookstore: workshop baseline'
TAG=level-0-start

assertions=0
failures=0
pass() { assertions=$((assertions + 1)); printf 'ok %d - %s\n' "$assertions" "$1"; }
fail() { assertions=$((assertions + 1)); failures=$((failures + 1)); printf 'not ok %d - %s\n' "$assertions" "$1"; }
check() {
  local name=$1
  shift
  if "$@"; then pass "$name"; else fail "$name"; fi
}
same() { [ "$1" = "$2" ]; }
differ() { [ "$1" != "$2" ]; }
empty() { [ -z "$1" ]; }
has_line() { printf '%s\n' "$2" | grep -qxF -- "$1"; }
lacks_line() { ! has_line "$1" "$2"; }
has_text() { printf '%s\n' "$2" | grep -qF -- "$1"; }
show() { printf '%s\n' "$1" | sed 's/^/    | /'; }
# The path lines of a --check report (every line except the header that ends with a colon), sorted.
reported_paths() { printf '%s\n' "$1" | { grep -v ':$' || true; } | sort; }
count_removed() { printf '%s\n' "$1" | grep -c '^removed: ' || true; }

# True when a repository-relative path is one of the listed paths or lies under one.
is_listed() {
  case $1 in
    CLAUDE.md | CLAUDE.local.md | AGENTS.md | .mcp.json) return 0 ;;
    .claude/* | openspec/* | .specify/* | specs/* | agent-os/* | .githooks/* | docs/spec-pack/*) return 0 ;;
  esac
  return 1
}

# Runs the script with the given working directory; sets out, err and rc. Every run gets its own author and
# committer date, an hour after the previous run, so a script that always makes a new commit gets a new id.
out=''
err=''
rc=0
clock=1767225600
run_in() {
  local dir=$1
  shift
  clock=$((clock + 3600))
  rc=0
  (cd "$dir" && GIT_AUTHOR_DATE="$clock +0000" GIT_COMMITTER_DATE="$clock +0000" "$BASH" "$SCRIPT" "$@") \
    >"$root/out" 2>"$root/err" || rc=$?
  out=$(cat "$root/out")
  err=$(cat "$root/err")
}

# State snapshots: every ref of a repository, the worktrees of the work repository, the script's leftovers.
refs_of() { git -C "$1" for-each-ref --format='%(refname) %(objectname)'; }
worktrees() { git -C "$work" worktree list --porcelain | grep '^worktree ' || true; }
tmp_left() { ls -A "$TMPDIR"; }
rev() { git -C "$1" rev-parse --verify --quiet "$2" || true; }

# The tree of <rev> in <repo> without the listed paths, in git ls-tree -r format (mode, type, id, path).
tab=$(printf '\t')
stripped_tree() {
  local line
  git -C "$1" ls-tree -r "$2" | while IFS= read -r line; do
    if ! is_listed "${line#*"$tab"}"; then printf '%s\n' "$line"; fi
  done
}

write_file() {
  local repo=$1 path=$2 content=${3:-x}
  mkdir -p "$(dirname "$repo/$path")"
  printf '%s\n' "$content" >"$repo/$path"
}

remote="$root/remote with space.git"
work="$root/work with space"

git init --quiet --bare "$remote"
git init --quiet -b factory "$work"
git -C "$work" config commit.gpgSign false
git -C "$work" config tag.gpgSign false
git -C "$work" remote add origin "$remote"

# Every listed path, the nested agent paths of the real repository, look-alikes and normal files.
write_file "$work" CLAUDE.md '# agents'
write_file "$work" CLAUDE.local.md 'local'
write_file "$work" AGENTS.md 'agents'
write_file "$work" .claude/settings.json '{}'
write_file "$work" .claude/agent-memory/orchestrator/MEMORY.md 'memory'
write_file "$work" .claude/skills/hexagonal-slice/SKILL.md 'skill'
write_file "$work" .mcp.json '{}'
write_file "$work" openspec/project.md 'openspec'
write_file "$work" .specify/memory.md 'specify'
write_file "$work" specs/one.md 'spec'
write_file "$work" agent-os/specs/2026-01-01-x/plan.md 'plan'
write_file "$work" agent-os/standards/x.md 'standard'
write_file "$work" .githooks/pre-commit '#!/bin/sh'
write_file "$work" docs/spec-pack/product/roadmap.md 'roadmap'
ln -s ../docs/spec-pack/product "$work/agent-os/product"
write_file "$work" .claude-foo/x 'look-alike'
write_file "$work" CLAUDE.md.example 'look-alike'
write_file "$work" docs/specs-notes.md 'look-alike'
write_file "$work" bookstore/x/specs/y 'look-alike'
write_file "$work" docs/spec-pack-old/x 'look-alike'
write_file "$work" specs-old/x 'look-alike'
write_file "$work" README.md '# Bookstore'
write_file "$work" pom.xml '<project/>'
write_file "$work" 'docs/with space.md' 'spaced'
write_file "$work" docs/architecture.md 'layers'
write_file "$work" mvnw '#!/bin/sh'
chmod +x "$work/mvnw"
printf '%s\n' '.claude/comms/' 'ignored.log' >"$work/.gitignore"
git -C "$work" add -A
git -C "$work" commit --quiet -m 'factory one'

# Files the .gitignore covers must not count as untracked.
write_file "$work" .claude/comms/mailboxes/x/note.md 'session state'
write_file "$work" ignored.log 'log'
check 'fixture work tree holds ignored files (they must not count as untracked)' \
  has_line '!! ignored.log' "$(git -C "$work" status --porcelain --ignored)"

# --- --list-paths -------------------------------------------------------------------------------------------

run_in "$root" --list-paths
check '--list-paths exits 0' same "$rc" 0
check '--list-paths prints the 11 paths in spec order' same "$out" "$EXPECTED_PATHS"

# --- --check ------------------------------------------------------------------------------------------------

factory_files=$(git -C "$work" ls-tree -r --name-only factory)
listed_files=''
while IFS= read -r f; do
  if is_listed "$f"; then listed_files="$listed_files$f
"; fi
done <<EOF
$factory_files
EOF
while IFS= read -r p; do
  check "fixture factory holds $p" differ "$(git -C "$work" ls-tree -r --name-only factory -- "$p")" ''
done <<EOF
$EXPECTED_PATHS
EOF

run_in "$work" --check factory
check '--check on factory exits non-zero' differ "$rc" 0
reported=$(reported_paths "$err")
check '--check on factory names exactly the files under listed paths' \
  same "$reported" "$(printf '%s' "$listed_files" | sort)"
check '--check names the agent-os/product symlink' has_line agent-os/product "$err"
check '--check does not name the look-alike .claude-foo/x' lacks_line .claude-foo/x "$err"

run_in "$work/docs" --check factory
check '--check from a subdirectory exits non-zero' differ "$rc" 0
check '--check from a subdirectory names the same files' \
  same "$(reported_paths "$err")" "$reported"

run_in "$work" --check no-such-revision
check '--check on an unknown revision exits non-zero' differ "$rc" 0

# --- default run: builds main and the tag locally -------------------------------------------------------------

# A broken origin proves that the default mode needs no remote at all.
git -C "$work" remote set-url origin "$root/no such remote.git"
run_in "$work"
show "$out"
show "$err"
check 'default run exits 0' same "$rc" 0
main1=$(rev "$work" refs/heads/main)
check 'default run creates local main' differ "$main1" ''
check 'main has exactly one commit' same "$(git -C "$work" rev-list --count main)" 1
check 'main has the baseline message and nothing else' same "$(git -C "$work" log -1 --format=%B main)" "$MESSAGE"
check 'main holds the factory tree without the listed paths (modes and blob ids equal)' \
  same "$(git -C "$work" ls-tree -r main)" "$(stripped_tree "$work" factory)"
while IFS= read -r p; do
  check "main holds nothing under $p" empty "$(git -C "$work" ls-tree -r --name-only main -- "$p")"
  check "prints removed: $p" has_line "removed: $p" "$out"
done <<EOF
$EXPECTED_PATHS
EOF
for p in agent-os/product agent-os/specs .claude/agent-memory .claude/skills .claude/settings.json; do
  check "main holds nothing under $p" empty "$(git -C "$work" ls-tree -r --name-only main -- "$p")"
done
for p in .claude-foo/x CLAUDE.md.example docs/specs-notes.md bookstore/x/specs/y docs/spec-pack-old/x specs-old/x \
  README.md pom.xml 'docs/with space.md' docs/architecture.md .gitignore; do
  check "main keeps $p" same "$(git -C "$work" ls-tree --name-only main -- "$p")" "$p"
done
check 'mvnw keeps its executable bit' same "$(git -C "$work" ls-tree main -- mvnw | cut -c1-6)" 100755
check 'local tag points to main' same "$(rev "$work" "refs/tags/$TAG^{commit}")" "$main1"
check 'removed paths are printed after the line that reports the moved refs' \
  same "$(count_removed "$(printf '%s\n' "$out" | sed -n '/now at/,$p')")" 11
check 'HEAD stays on factory' same "$(git -C "$work" symbolic-ref HEAD)" refs/heads/factory
check 'working tree stays clean' empty "$(git -C "$work" status --porcelain)"
check 'temporary worktree removed' same "$(worktrees)" "worktree $work"
check 'temporary directory removed' empty "$(tmp_left)"
check 'bare remote untouched' empty "$(refs_of "$remote")"

# A factory without any listed path: nothing to remove (an empty list under set -u on bash 3.2).
plain="$root/plain repo"
git init --quiet -b factory "$plain"
git -C "$plain" config commit.gpgSign false
write_file "$plain" README.md '# plain'
git -C "$plain" add -A
git -C "$plain" commit --quiet -m 'plain'
run_in "$plain"
check 'factory without listed paths: exits 0' same "$rc" 0
check 'factory without listed paths: main tree equals factory tree' \
  same "$(rev "$plain" 'main^{tree}')" "$(rev "$plain" 'factory^{tree}')"
check 'factory without listed paths: nothing printed as removed' same "$(count_removed "$out")" 0
check 'factory without listed paths: main has one commit' same "$(git -C "$plain" rev-list --count main)" 1

# --- idempotency, locally -------------------------------------------------------------------------------------

run_in "$work"
check 'rerun with factory unchanged: exits 0' same "$rc" 0
check 'rerun with factory unchanged: main is the same commit' same "$(rev "$work" refs/heads/main)" "$main1"
check 'rerun with factory unchanged: tag is the same commit' same "$(rev "$work" "refs/tags/$TAG")" "$main1"
check 'rerun with factory unchanged: says it reuses main' has_text 'reusing' "$out"

write_file "$work" .claude/agent-memory/orchestrator/MEMORY.md 'memory changed'
git -C "$work" commit --quiet -am 'memory only'
run_in "$work"
check 'factory changed only inside listed paths: exits 0' same "$rc" 0
check 'factory changed only inside listed paths: main is the same commit' \
  same "$(rev "$work" refs/heads/main)" "$main1"

# A main with the right tree and message but with a parent is not a baseline: a new root commit replaces it.
parented=$(git -C "$work" commit-tree "$main1^{tree}" -p "$main1" -m "$MESSAGE")
git -C "$work" update-ref refs/heads/main "$parented"
run_in "$work"
main2=$(rev "$work" refs/heads/main)
check 'main with a parent: exits 0' same "$rc" 0
check 'main with a parent: replaced' differ "$main2" "$parented"
check 'main with a parent: replaced by one parentless commit' same "$(git -C "$work" rev-list --count main)" 1
check 'main with a parent: same tree as before' same "$(rev "$work" 'main^{tree}')" "$(rev "$work" "$main1^{tree}")"
check 'main with a parent: tag follows main' same "$(rev "$work" "refs/tags/$TAG")" "$main2"

# A parentless main with the right tree but another message is not reused either.
other=$(git -C "$work" commit-tree "$main1^{tree}" -m 'first commit')
git -C "$work" update-ref refs/heads/main "$other"
run_in "$work"
main3=$(rev "$work" refs/heads/main)
check 'main with another message: replaced' differ "$main3" "$other"
check 'main with another message: new main has the baseline message' \
  same "$(git -C "$work" log -1 --format=%B main)" "$MESSAGE"

# --- refusals: each leaves every ref, worktree and temporary file as it was ----------------------------------

expect_refusal() {
  local name=$1 phrase=$2 refs_before remote_before worktrees_before
  shift 2
  refs_before=$(refs_of "$work")
  remote_before=$(refs_of "$remote")
  worktrees_before=$(worktrees)
  run_in "$work" "$@"
  check "refuses $name: exits non-zero" differ "$rc" 0
  check "refuses $name: says why" has_text "$phrase" "$err"
  check "refuses $name: no local ref moved" same "$(refs_of "$work")" "$refs_before"
  check "refuses $name: bare remote unchanged" same "$(refs_of "$remote")" "$remote_before"
  check "refuses $name: no worktree added or removed" same "$(worktrees)" "$worktrees_before"
  check "refuses $name: no temporary directory left" empty "$(tmp_left)"
}

write_file "$work" README.md 'tracked change'
expect_refusal 'a tracked change' 'not clean'
git -C "$work" add README.md
expect_refusal 'a staged change' 'not clean'
git -C "$work" reset --quiet --hard
write_file "$work" untracked.txt 'untracked'
expect_refusal 'an untracked file' 'not clean'
# A repository config that hides untracked files from git status must not hide them from the script.
git -C "$work" config status.showUntrackedFiles no
expect_refusal 'an untracked file hidden by status.showUntrackedFiles=no' 'not clean'
git -C "$work" config --unset status.showUntrackedFiles
rm "$work/untracked.txt"
git -C "$work" checkout --quiet --detach factory
expect_refusal 'a detached HEAD at the factory commit' 'branch factory'
git -C "$work" checkout --quiet -b factory-x factory
expect_refusal 'branch factory-x' 'branch factory'
git -C "$work" checkout --quiet factory
git -C "$work" branch --quiet -D factory-x
git -C "$work" worktree add --quiet "$root/main checkout" main
expect_refusal 'main checked out in a worktree' 'checked out'
git -C "$work" worktree remove "$root/main checkout"

# --- a failure after the temporary worktree exists cleans up and moves nothing ------------------------------

write_file "$work" README.md 'real change'
git -C "$work" commit --quiet -am 'real change'
refs_before=$(refs_of "$work")
rc=0
(cd "$work" && env -u GIT_AUTHOR_NAME -u GIT_AUTHOR_EMAIL -u GIT_COMMITTER_NAME -u GIT_COMMITTER_EMAIL \
  GIT_CONFIG_COUNT=1 GIT_CONFIG_KEY_0=user.useConfigOnly GIT_CONFIG_VALUE_0=true "$BASH" "$SCRIPT") \
  >"$root/out" 2>"$root/err" || rc=$?
out=$(cat "$root/out")
show "$(cat "$root/err")"
check 'commit failure (no identity): exits non-zero' differ "$rc" 0
check 'commit failure: nothing printed as removed' same "$(count_removed "$out")" 0
check 'commit failure: no local ref moved' same "$(refs_of "$work")" "$refs_before"
check 'commit failure: temporary worktree removed' same "$(worktrees)" "worktree $work"
check 'commit failure: worktree administration pruned' empty "$(ls -A "$work/.git/worktrees" 2>/dev/null || true)"
check 'commit failure: temporary directory removed' empty "$(tmp_left)"

# --- --dry-run changes nothing ------------------------------------------------------------------------------

refs_before=$(refs_of "$work")
run_in "$work" --dry-run
check '--dry-run: exits 0' same "$rc" 0
check '--dry-run: says it is a dry run' has_text 'dry run' "$out"
check '--dry-run: no local ref moved' same "$(refs_of "$work")" "$refs_before"
check '--dry-run: no worktree left' same "$(worktrees)" "worktree $work"
check '--dry-run: no temporary directory left' empty "$(tmp_left)"

# --- a real change makes a new single commit and moves the tag ----------------------------------------------

run_in "$work"
main4=$(rev "$work" refs/heads/main)
check 'real change: exits 0' same "$rc" 0
check 'real change: new commit' differ "$main4" "$main3"
check 'real change: still one parentless commit' same "$(git -C "$work" rev-list --count main)" 1
check 'real change: tag moved to the new main' same "$(rev "$work" "refs/tags/$TAG")" "$main4"
check 'real change: tree is factory without the listed paths' \
  same "$(git -C "$work" ls-tree -r main)" "$(stripped_tree "$work" factory)"

# --- --push ---------------------------------------------------------------------------------------------------

# The remote starts like the real one: an unrelated root commit on main that the work repository does not have,
# and no tag.
seed="$root/seed repo"
git init --quiet -b main "$seed"
git -C "$seed" config commit.gpgSign false
write_file "$seed" README.md 'first'
git -C "$seed" add -A
git -C "$seed" commit --quiet -m 'first commit'
git -C "$seed" push --quiet "$remote" main
remote_seed=$(rev "$remote" refs/heads/main)
git -C "$work" remote set-url origin "$remote"
git -C "$work" push --quiet origin factory

# A test double for git, used only when a test puts it first on PATH: it records the arguments of a push and can
# move the remote's main just before the push reaches the remote.
real_git=$(command -v git)
mkdir -p "$root/shim"
cat >"$root/shim/git" <<'EOF'
#!/bin/sh
for arg in "$@"; do
  if [ "$arg" = push ]; then
    printf '%s\n' "$@" >"$SHIM_LOG"
    if [ -n "${SHIM_MOVE_MAIN_TO:-}" ]; then
      "$SHIM_REAL_GIT" -C "$SHIM_REMOTE" update-ref refs/heads/main "$SHIM_MOVE_MAIN_TO"
    fi
    break
  fi
done
exec "$SHIM_REAL_GIT" "$@"
EOF
chmod +x "$root/shim/git"
run_with_shim() {
  local saved_path=$PATH
  export PATH="$root/shim:$PATH" SHIM_LOG="$root/push-args" SHIM_REAL_GIT="$real_git" SHIM_REMOTE="$remote"
  : >"$SHIM_LOG"
  run_in "$@"
  export PATH="$saved_path"
}

run_with_shim "$work" --push
show "$out"
show "$err"
push_args=$(cat "$root/push-args")
show "$push_args"
check '--push: exits 0' same "$rc" 0
check '--push: remote main is local main' same "$(rev "$remote" refs/heads/main)" "$main4"
check '--push: remote tag is local main' same "$(rev "$remote" "refs/tags/$TAG")" "$main4"
check '--push: remote main has exactly one commit' same "$(git -C "$remote" rev-list --count main)" 1
check '--push: prints removed paths' same "$(count_removed "$out")" 11
check '--push: one atomic push' has_line --atomic "$push_args"
check '--push: lease on main expects the remote main' \
  has_line "--force-with-lease=refs/heads/main:$remote_seed" "$push_args"
check '--push: lease on the tag expects it absent' has_line "--force-with-lease=refs/tags/$TAG:" "$push_args"
check '--push: no bare --force' lacks_line --force "$push_args"
check '--push: no forced refspec' empty "$(printf '%s\n' "$push_args" | grep '^+' || true)"
check '--push: pushes the commit to main' has_line "$main4:refs/heads/main" "$push_args"
check '--push: pushes the commit to the tag' has_line "$main4:refs/tags/$TAG" "$push_args"

run_in "$work" --push
check '--push rerun unchanged: exits 0' same "$rc" 0
check '--push rerun unchanged: remote main unchanged' same "$(rev "$remote" refs/heads/main)" "$main4"
check '--push rerun unchanged: remote tag unchanged' same "$(rev "$remote" "refs/tags/$TAG")" "$main4"

# Without a local main, the remote main is the baseline to reuse.
git -C "$work" update-ref -d refs/heads/main
run_in "$work" --push
check '--push without a local main: exits 0' same "$rc" 0
check '--push without a local main: reuses the remote main' same "$(rev "$work" refs/heads/main)" "$main4"
check '--push without a local main: remote main unchanged' same "$(rev "$remote" refs/heads/main)" "$main4"
check '--push without a local main: remote tag unchanged' same "$(rev "$remote" "refs/tags/$TAG")" "$main4"

# Local factory must equal the remote's factory: ahead, behind and diverged are all refused.
write_file "$work" README.md 'ahead'
git -C "$work" commit --quiet -am 'ahead'
ahead=$(rev "$work" refs/heads/factory)
expect_refusal 'factory ahead of the remote with --push' 'differs' --push
git -C "$work" push --quiet origin factory
git -C "$work" reset --quiet --hard HEAD~1
expect_refusal 'factory behind the remote with --push' 'differs' --push
write_file "$work" README.md 'diverged'
git -C "$work" commit --quiet -am 'diverged'
expect_refusal 'factory diverged from the remote with --push' 'differs' --push
git -C "$work" reset --quiet --hard "$ahead"

# Atomic: the remote rejects the tag, so main must not move either.
cat >"$remote/hooks/update" <<'EOF'
#!/bin/sh
# Rejects any update of the tag; every other ref is accepted.
if [ "$1" = refs/tags/level-0-start ]; then
  exit 1
fi
exit 0
EOF
chmod +x "$remote/hooks/update"
refs_before=$(refs_of "$work")
run_in "$work" --push
check 'remote rejects the tag: exits non-zero' differ "$rc" 0
check 'remote rejects the tag: remote main unchanged' same "$(rev "$remote" refs/heads/main)" "$main4"
check 'remote rejects the tag: remote tag unchanged' same "$(rev "$remote" "refs/tags/$TAG")" "$main4"
check 'remote rejects the tag: no local ref moved' same "$(refs_of "$work")" "$refs_before"
check 'remote rejects the tag: nothing printed as removed' same "$(count_removed "$out")" 0
rm "$remote/hooks/update"

# Lease: someone moves the remote main between the read and the push; the push must fail and keep their main.
remote_main_before=$(rev "$remote" refs/heads/main)
export SHIM_MOVE_MAIN_TO="$ahead"
run_with_shim "$work" --push
unset SHIM_MOVE_MAIN_TO
show "$err"
check 'remote main moved before the push: exits non-zero' differ "$rc" 0
check 'remote main moved before the push: reported as stale' has_text 'stale info' "$err"
check 'remote main moved before the push: their main kept' same "$(rev "$remote" refs/heads/main)" "$ahead"
check 'remote main moved before the push: remote tag unchanged' same "$(rev "$remote" "refs/tags/$TAG")" "$main4"
check 'remote main moved before the push: no local ref moved' same "$(refs_of "$work")" "$refs_before"
git -C "$remote" update-ref refs/heads/main "$remote_main_before"

# --dry-run with --push touches no ref anywhere.
remote_before=$(refs_of "$remote")
run_in "$work" --push --dry-run
check '--push --dry-run: exits 0' same "$rc" 0
check '--push --dry-run: says it is a dry run' has_text 'dry run' "$out"
check '--push --dry-run: no local ref moved' same "$(refs_of "$work")" "$refs_before"
check '--push --dry-run: bare remote unchanged' same "$(refs_of "$remote")" "$remote_before"
check '--push --dry-run: no worktree left' same "$(worktrees)" "worktree $work"
check '--push --dry-run: no temporary directory left' empty "$(tmp_left)"

# An empty --remote value is a usage error, not origin: origin still points at the test remote here, so a
# fallback to origin would push.
remote_before=$(refs_of "$remote")
refs_before=$(refs_of "$work")
run_in "$work" --push --remote ''
check '--push --remote with an empty name is a usage error' same "$rc" 2
check '--push --remote with an empty name: bare remote unchanged' same "$(refs_of "$remote")" "$remote_before"
check '--push --remote with an empty name: no local ref moved' same "$(refs_of "$work")" "$refs_before"

# --remote: origin is broken, the named remote is used.
git -C "$work" remote set-url origin "$root/no such remote.git"
git -C "$work" remote add upstream "$remote"
run_in "$work" --push --remote upstream
show "$err"
main5=$(rev "$work" refs/heads/main)
check '--push --remote upstream with a broken origin: exits 0' same "$rc" 0
check '--push --remote upstream: new local main' differ "$main5" "$main4"
check '--push --remote upstream: remote main is the new main' same "$(rev "$remote" refs/heads/main)" "$main5"
check '--push --remote upstream: remote tag is the new main' same "$(rev "$remote" "refs/tags/$TAG")" "$main5"
check '--push --remote upstream: remote main has one commit' same "$(git -C "$remote" rev-list --count main)" 1
check '--push --remote upstream: local tag is the new main' same "$(rev "$work" "refs/tags/$TAG")" "$main5"

run_in "$work" --remote upstream
check '--remote without --push is a usage error' same "$rc" 2
run_in "$work" --push --remote
check '--remote without a name is a usage error' same "$rc" 2
run_in "$work" --no-such-option
check 'an unknown option is a usage error' same "$rc" 2
run_in "$work" --help
check '--help exits 0' same "$rc" 0
check '--help prints the usage' has_text 'Usage:' "$out"

# --- the guard on a fresh clone of the published main ---------------------------------------------------------

clone="$root/guard clone"
git clone --quiet --branch main "$remote" "$clone"
git -C "$clone" config commit.gpgSign false
check 'fresh clone of main: one commit' same "$(git -C "$clone" rev-list --count HEAD)" 1
check 'fresh clone of main: tag is HEAD' same "$(rev "$clone" "refs/tags/$TAG^{commit}")" "$(rev "$clone" HEAD)"
run_in "$clone" --check HEAD
check '--check HEAD on the published main: exits 0' same "$rc" 0
run_in "$clone/docs" --check HEAD
check '--check HEAD on the published main from a subdirectory: exits 0' same "$rc" 0
write_file "$clone" .claude/settings.json '{}'
git -C "$clone" add .claude/settings.json
git -C "$clone" commit --quiet -m 'add agent settings'
run_in "$clone" --check HEAD
check '--check HEAD after adding .claude/settings.json: exits non-zero' differ "$rc" 0
check '--check HEAD after adding .claude/settings.json: names exactly that path' \
  same "$(reported_paths "$err")" .claude/settings.json
run_in "$clone/docs" --check HEAD
check '--check HEAD from a subdirectory: exits non-zero' differ "$rc" 0
check '--check HEAD from a subdirectory: names exactly .claude/settings.json' \
  same "$(reported_paths "$err")" .claude/settings.json

finished=yes
printf '%d assertions, %d failures (script run with %s, bash %s)\n' "$assertions" "$failures" "$BASH" \
  "$BASH_VERSION"
[ "$failures" -eq 0 ]
