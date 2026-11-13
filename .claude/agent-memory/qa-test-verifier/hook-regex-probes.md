---
name: hook-regex-probes
description: Claude Code hook scripts that grep stdin JSON - mutate every regex element, equivalence calls
metadata:
  type: feedback
---
Rule: for a hook that filters stdin JSON with one `grep -E` pattern, mutate EVERY element of the pattern, not only the whitespace classes: the `[^"]*` prefix (anchor it: `"git`), the `[[:space:]]+` separator (`*`), the class per side of the colon, the quantifier per side, and the line split. Then check the fixture command values (`grep -o '"command"...'`).
**Why:** a hook harness with 16 whitespace fixtures still let the anchored-prefix mutant survive, because every command fixture started with `git commit`; a real `git add -A && git commit` would then pass a broken hook unbuilt. `+` to `*` also survived (no `ls legitcommits`-style negative). Both went unnoticed through three QA rounds that probed only classes.
**How to apply:**
- Positive fixture with a prefix (`git add -A && git commit -am wip`) and a negative with the words glued inside a word. Prove each with an insertion-only harness copy: green on production, red on its mutant.
- Equivalent, say so instead of asking for cases: `true` to `tru` after the colon (valid JSON has only `true` there); `>&2` dropped in a git `pre-commit` (git sends hook stdout to stderr); VT/FF or raw control bytes inside a JSON string.
- Per side and per member: a whole-class mutant is killed by one fixture while single positions survive. Quantifier `*` to `?`/`{0,2}` is killed only by a run of 3. LF ends the grep line, so LF-before-colon is a negative case.
- Locale trap: `[[:space:]]` in a UTF-8 locale also matches non-ASCII spaces (U+00A0 on macOS, U+2003, U+3000); such a character does not split words in bash/zsh, so it is no real command.
- Docs quotes: fetch the `.md` form with curl, compare sha256 with the evidence header, compare each quoted line exactly (non-ASCII as `?`); the control must print which line it mutated.
Related: [[bash-script-qa-probes]], [[shell-traps]], [[consistency-check-verification]]
