#!/usr/bin/env bash
# apply-level.sh: apply one staged step to a target repository and leave the changes uncommitted.
#
# Usage: apply-level.sh [--group config|exercise] <step> <target-repo>
#
# <step> is a folder next to scripts/ (level-1, level-2, level-3, level-4-openspec, level-4-speckit,
# level-4-agentos) with manifest.tsv, files/ and optional patches/. See ../README.md for the manifest format and the
# path encoding rule. Every row is checked before the first write; if a write fails, everything written is rolled
# back, so on any error the target tree is unchanged. If the rollback itself cannot restore something, it goes on with
# the rest and names each path it could not restore ("rollback incomplete"). Errors name the failing manifest line.
set -euo pipefail

PROG=apply-level
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
STAGE_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)
# shellcheck source=lib-steps.sh
. "$SCRIPT_DIR/lib-steps.sh"

TAB=$(printf '\t')
CR=$(printf '\r')

usage() {
  echo "usage: $PROG [--group config|exercise] <step> <target-repo>" >&2
  echo "steps: $ALL_STEPS" >&2
  exit 2
}

die() {
  echo "$PROG: error: $*" >&2
  exit 1
}

# line_die <line-number> <message>
line_die() {
  echo "$PROG: error: $STEP/manifest.tsv line $1: $2" >&2
  exit 1
}

# path_problem <path>: prints why a relative path is unsafe, nothing when it is fine.
path_problem() {
  local rest=$1 c
  case $rest in
    '') echo "empty path"; return ;;
    /*) echo "absolute path"; return ;;
  esac
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

# staged_problem <path>: prints why a staged path breaks the encoding rule, nothing when it is fine.
staged_problem() {
  local rest=$1 c
  while :; do
    c=${rest%%/*}
    case $c in
      .*) echo "staged path component '$c' starts with a dot (stage it as dot-${c#.})"; return ;;
      CLAUDE.md) echo "staged path component must not be named CLAUDE.md (stage it as CLAUDE.staged.md)"; return ;;
      CLAUDE.local.md|AGENTS.md) echo "staged path component must not be named $c"; return ;;
    esac
    [ "$rest" = "${rest#*/}" ] && break
    rest=${rest#*/}
  done
}

# target_problem <path>: prints why a target path is unsafe, nothing when it is fine.
target_problem() {
  local rest=$1 c lower
  while :; do
    c=${rest%%/*}
    lower=$(printf '%s' "$c" | tr 'A-Z' 'a-z')
    if [ "$lower" = .git ]; then
      echo "target has a '.git' path component"
      return
    fi
    [ "$rest" = "${rest#*/}" ] && break
    rest=${rest#*/}
  done
}

# decode_path <staged-relative-path>: dot-x becomes .x and CLAUDE.staged.md becomes CLAUDE.md, per component.
decode_path() {
  local rest=$1 c out=""
  while :; do
    c=${rest%%/*}
    case $c in
      dot-*) c=".${c#dot-}" ;;
      CLAUDE.staged.md) c='CLAUDE.md' ;;
    esac
    out="${out:+$out/}$c"
    [ "$rest" = "${rest#*/}" ] && break
    rest=${rest#*/}
  done
  printf '%s' "$out"
}

# ---------------------------------------------------------------------------------------------------------------
# Arguments
# ---------------------------------------------------------------------------------------------------------------

GROUP=all
while [ $# -gt 0 ]; do
  case $1 in
    --group)
      [ $# -ge 2 ] || usage
      case $2 in
        config|exercise) GROUP=$2 ;;
        *) die "--group must be config or exercise, got '$2'" ;;
      esac
      shift 2
      ;;
    -h|--help) usage ;;
    --) shift; break ;;
    -*) usage ;;
    *) break ;;
  esac
done
[ $# -eq 2 ] || usage
STEP=$1
TARGET=$2

is_step "$STEP" || die "unknown step: $STEP (expected one of: $ALL_STEPS)"
STEP_DIR="$STAGE_ROOT/$STEP"
MANIFEST="$STEP_DIR/manifest.tsv"
[ -f "$MANIFEST" ] || die "$STEP/manifest.tsv is missing"

[ -d "$TARGET" ] || die "target is not a folder: $TARGET"
TARGET_ABS=$(cd "$TARGET" && pwd -P)
top=$(git -C "$TARGET_ABS" rev-parse --show-toplevel 2>/dev/null) || die "not a git work tree: $TARGET"
[ "$(cd "$top" && pwd -P)" = "$TARGET_ABS" ] || die "target must be the top level of its work tree: $TARGET"

# ---------------------------------------------------------------------------------------------------------------
# Phase 1: read and check every row. Nothing is written in this phase.
# ---------------------------------------------------------------------------------------------------------------

N=0
lineno=0
while IFS= read -r line || [ -n "$line" ]; do
  lineno=$((lineno + 1))
  case $line in
    *"$CR"*) line_die "$lineno" "carriage return in the row (manifest.tsv must use LF line endings)" ;;
    '#'*|'') continue ;;
  esac
  tabs=${line//[!$TAB]/}
  [ "${#tabs}" -eq 5 ] || line_die "$lineno" "expected 6 tab-separated fields, found $((${#tabs} + 1))"
  case $line in
    "$TAB"*|*"$TAB"|*"$TAB$TAB"*) line_die "$lineno" "empty field" ;;
  esac
  IFS=$TAB read -r op group staged target mode base <<EOF
$line
EOF

  case $op in add|replace|patch) ;; *) line_die "$lineno" "unknown op '$op' (add, replace or patch)" ;; esac
  case $group in config|exercise) ;; *) line_die "$lineno" "unknown group '$group' (config or exercise)" ;; esac
  case $mode in 644|755) ;; *) line_die "$lineno" "mode must be 644 or 755, got '$mode'" ;; esac
  if [ "$op" = patch ]; then
    printf '%s' "$base" | grep -Eq '^[0-9a-f]{40}$' || line_die "$lineno" "base must be a 40-hex blob sha for patch, got '$base'"
  else
    [ "$base" = - ] || line_die "$lineno" "base must be '-' for $op, got '$base'"
  fi

  problem=$(path_problem "$staged")
  [ -z "$problem" ] || line_die "$lineno" "staged path $staged: $problem"
  problem=$(staged_problem "$staged")
  [ -z "$problem" ] || line_die "$lineno" "$problem"
  problem=$(path_problem "$target")
  [ -z "$problem" ] || line_die "$lineno" "target $target: $problem"
  problem=$(target_problem "$target")
  [ -z "$problem" ] || line_die "$lineno" "$problem"

  if [ "$op" = patch ]; then
    case $staged in
      patches/?*.patch) encoded=${staged#patches/}; encoded=${encoded%.patch} ;;
      *) line_die "$lineno" "staged path must start with patches/ and end with .patch for patch: $staged" ;;
    esac
  else
    case $staged in
      files/?*) encoded=${staged#files/} ;;
      *) line_die "$lineno" "staged path must start with files/ for $op: $staged" ;;
    esac
  fi
  decoded=$(decode_path "$encoded")
  [ "$decoded" = "$target" ] || line_die "$lineno" "staged path $staged does not decode to the target $target (it decodes to $decoded)"

  if [ ! -f "$STEP_DIR/$staged" ] || [ -L "$STEP_DIR/$staged" ]; then
    line_die "$lineno" "staged file is missing (or not a regular file): $STEP/$staged"
  fi

  i=0
  while [ "$i" -lt "$N" ]; do
    [ "${R_TARGET[$i]}" != "$target" ] || line_die "$lineno" "duplicate target $target (also on line ${R_LINE[$i]})"
    i=$((i + 1))
  done

  R_LINE[$N]=$lineno
  R_OP[$N]=$op
  R_GROUP[$N]=$group
  R_STAGED[$N]=$staged
  R_TARGET[$N]=$target
  R_MODE[$N]=$mode
  R_BASE[$N]=$base
  N=$((N + 1))
done < "$MANIFEST"

# Every staged file is named by exactly one row (duplicate targets are refused above, and a staged path decodes to
# its target, so one row per file).
for sub in files patches; do
  [ -d "$STEP_DIR/$sub" ] || continue
  while IFS= read -r f; do
    rel=${f#"$STEP_DIR"/}
    found=0
    i=0
    while [ "$i" -lt "$N" ]; do
      if [ "${R_STAGED[$i]}" = "$rel" ]; then
        found=1
        break
      fi
      i=$((i + 1))
    done
    [ "$found" = 1 ] || die "$STEP/$rel is not named by any manifest row"
  done <<EOF
$(find "$STEP_DIR/$sub" \( -type f -o -type l \) -print | LC_ALL=C sort)
EOF
done

# Rows of the selected group, checked against the target.
SEL=""
i=0
while [ "$i" -lt "$N" ]; do
  if [ "$GROUP" = all ] || [ "${R_GROUP[$i]}" = "$GROUP" ]; then
    SEL="$SEL $i"
    ln=${R_LINE[$i]}
    t=${R_TARGET[$i]}
    tpath="$TARGET_ABS/$t"
    anc=$(dirname "$t")
    while [ "$anc" != . ]; do
      if [ -L "$TARGET_ABS/$anc" ]; then
        line_die "$ln" "target folder $anc is a symbolic link"
      elif [ -e "$TARGET_ABS/$anc" ] && [ ! -d "$TARGET_ABS/$anc" ]; then
        line_die "$ln" "target folder $anc is not a folder"
      fi
      anc=$(dirname "$anc")
    done
    case ${R_OP[$i]} in
      add)
        if [ -e "$tpath" ] || [ -L "$tpath" ]; then
          line_die "$ln" "add: target exists: $t"
        fi
        ;;
      replace|patch)
        [ -e "$tpath" ] || line_die "$ln" "${R_OP[$i]}: target does not exist: $t"
        { [ -f "$tpath" ] && [ ! -L "$tpath" ]; } || line_die "$ln" "${R_OP[$i]}: target is not a regular file: $t"
        ;;
    esac
    if [ "${R_OP[$i]}" = patch ]; then
      pfile="$STEP_DIR/${R_STAGED[$i]}"
      base=${R_BASE[$i]}
      touched=$(git -C "$TARGET_ABS" apply --numstat -- "$pfile" 2>&1) || line_die "$ln" "not a valid patch: ${R_STAGED[$i]}: $touched"
      touched=$(printf '%s\n' "$touched" | cut -f3 | tr '\n' ' ' | sed 's/ *$//')
      [ "$touched" = "$t" ] || line_die "$ln" "patch touches $touched, not $t"
      pre=$(sed -n 's/^index \([0-9a-f]*\)\.\..*/\1/p' "$pfile" | head -n 1)
      [ -n "$pre" ] || line_die "$ln" "patch has no index line (generate it with git diff)"
      case $base in
        "$pre"*) ;;
        *) line_die "$ln" "base sha $base does not match the patch index line ($pre)" ;;
      esac
      current=$(git -C "$TARGET_ABS" hash-object -- "$t")
      if [ "$current" = "$base" ]; then
        git -C "$TARGET_ABS" apply --check -- "$pfile" 2> /dev/null || line_die "$ln" "patch does not apply to its own base blob"
      elif ! git -C "$TARGET_ABS" cat-file -e "$base^{blob}" 2> /dev/null; then
        line_die "$ln" "target $t differs from the base and the base blob $base is missing from the target repository, so no 3-way merge is possible"
      fi
    fi
  fi
  i=$((i + 1))
done

if [ -z "$SEL" ]; then
  echo "$PROG: $STEP: no rows in group $GROUP; nothing applied"
  exit 0
fi

# ---------------------------------------------------------------------------------------------------------------
# Phase 2: write. Back up first; roll back on any failure.
# ---------------------------------------------------------------------------------------------------------------

CREATED_DIRS=""
FINISHED=0
WRITING=0
BACKUP=""
HAD_INDEX=0
STARTED=""
NOT_RESTORED=""

# same_file <a> <b>: same content and same permission bits.
same_file() {
  cmp -s "$1" "$2" && [ "$(ls -ld "$1" | cut -c1-10)" = "$(ls -ld "$2" | cut -c1-10)" ]
}

# not_restored <what>: records something the rollback could not restore.
not_restored() {
  NOT_RESTORED="$NOT_RESTORED$1
"
}

# rollback: restores every row whose write started, removes the created folders and restores the index. Runs without
# errexit and does every step even when one fails; what it could not restore is recorded in NOT_RESTORED.
rollback() {
  local i d t
  for i in $STARTED; do
    t=${R_TARGET[$i]}
    if [ -e "$BACKUP/row-$i" ]; then
      # A file the failed write never changed (for example a read-only one) is left as it is.
      same_file "$BACKUP/row-$i" "$TARGET_ABS/$t" || cp -p "$BACKUP/row-$i" "$TARGET_ABS/$t" || not_restored "$t"
    else
      rm -f "$TARGET_ABS/$t" || not_restored "$t"
    fi
  done
  while IFS= read -r d; do
    [ -n "$d" ] || continue
    if [ -e "$TARGET_ABS/$d" ]; then
      rmdir "$TARGET_ABS/$d" || not_restored "$d/"
    fi
  done <<EOF
$CREATED_DIRS
EOF
  if [ "$HAD_INDEX" = 1 ]; then
    cp -p "$BACKUP/index" "$INDEX_FILE" || not_restored "the git index"
  else
    rm -f "$INDEX_FILE" || not_restored "the git index"
  fi
}

on_exit() {
  local rc=$? p
  # No errexit here: every cleanup step runs, even after one fails.
  set +e
  if [ "$WRITING" = 1 ] && [ "$FINISHED" != 1 ]; then
    rollback
    if [ -z "$NOT_RESTORED" ]; then
      echo "$PROG: rolled back; the target tree is unchanged" >&2
    else
      while IFS= read -r p; do
        [ -z "$p" ] || echo "$PROG: rollback incomplete: could not restore $p" >&2
      done <<EOF
$NOT_RESTORED
EOF
      echo "$PROG: rollback incomplete; the target tree is NOT as it was before the apply" >&2
    fi
    [ "$rc" != 0 ] || rc=1
  fi
  if [ -n "$BACKUP" ]; then
    rm -rf "$BACKUP" || echo "$PROG: warning: could not remove the backup folder $BACKUP" >&2
  fi
  exit "$rc"
}
# Installed before the backup is made, so the backup folder is removed on every exit.
trap on_exit EXIT

BACKUP=$(mktemp -d "${TMPDIR:-/tmp}/$PROG.XXXXXX")
INDEX_FILE=$(git -C "$TARGET_ABS" rev-parse --git-path index)
case $INDEX_FILE in /*) ;; *) INDEX_FILE="$TARGET_ABS/$INDEX_FILE" ;; esac
if [ -f "$INDEX_FILE" ]; then
  cp -p "$INDEX_FILE" "$BACKUP/index" || die "cannot back up the git index; nothing was written"
  HAD_INDEX=1
fi
for i in $SEL; do
  if [ -e "$TARGET_ABS/${R_TARGET[$i]}" ]; then
    cp -p "$TARGET_ABS/${R_TARGET[$i]}" "$BACKUP/row-$i" ||
      line_die "${R_LINE[$i]}" "cannot back up ${R_TARGET[$i]}; nothing was written"
  fi
done

# make_parent <target-relative-path>: creates missing parent folders and records them, deepest first.
make_parent() {
  local d new=""
  d=$(dirname "$1")
  while [ "$d" != . ] && [ ! -e "$TARGET_ABS/$d" ]; do
    new="$new$d
"
    d=$(dirname "$d")
  done
  CREATED_DIRS="$new$CREATED_DIRS"
  mkdir -p "$TARGET_ABS/$(dirname "$1")"
}

WRITING=1
for i in $SEL; do
  ln=${R_LINE[$i]}
  t=${R_TARGET[$i]}
  STARTED="$STARTED $i"
  case ${R_OP[$i]} in
    add)
      make_parent "$t" || line_die "$ln" "add: cannot create the folder of $t"
      cp "$STEP_DIR/${R_STAGED[$i]}" "$TARGET_ABS/$t" || line_die "$ln" "add: cannot write $t"
      ;;
    replace)
      cp "$STEP_DIR/${R_STAGED[$i]}" "$TARGET_ABS/$t" || line_die "$ln" "replace: cannot write $t"
      ;;
    patch)
      if ! out=$(git -C "$TARGET_ABS" apply --3way -- "$STEP_DIR/${R_STAGED[$i]}" 2>&1); then
        printf '%s\n' "$out" | sed 's/^/  git apply: /' >&2
        line_die "$ln" "patch: the 3-way merge is not clean for $t"
      fi
      ;;
  esac
  chmod "${R_MODE[$i]}" "$TARGET_ABS/$t" || line_die "$ln" "cannot set mode ${R_MODE[$i]} on $t"
  echo "$PROG: $STEP line $ln: ${R_OP[$i]} $t (${R_MODE[$i]}, ${R_GROUP[$i]})"
done
FINISHED=1

count=$(echo $SEL | wc -w | tr -d ' ')
echo "$PROG: $STEP: $count rows applied (group: $GROUP); changes left uncommitted in $TARGET_ABS"
