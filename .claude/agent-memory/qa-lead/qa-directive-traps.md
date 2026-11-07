---
name: qa-directive-traps
description: Recurring traps when writing verifier directives in this repo's SDD cycle (clean-tree snapshots, greps, probes, ArchUnit vacuity, warnings, timestamps, evidence files, CI run ids, moved base, final integrity hashes, test data that cannot falsify a criterion); numbered list below
metadata:
  type: feedback
---

Traps that recur across specs in this repo. Write them into every qaDirective that touches them.

1. **"Clean working tree" before the gate.** Nothing is committed before the QA gate, so the tree always holds the impl's uncommitted changes. Never ask for an empty `git status`. Ask for `git status --porcelain` plus `shasum -a 256` of the probe-touched files, identical before the first probe and after the last restore. Verifiers have read-only git: they restore with Edit or rm, never checkout or stash.
   **Why:** a literal "clean tree" criterion cannot be met, and a verifier ends up either failing wrongly or hand-waving it.
   Plan drafts also write it into impl criteria ("restored with `git checkout`, tree clean"): impl agents have read-only git too. Flag it at Gate 1 with the snapshot rewrite.

2. **Path-reference ban vs `.gitignore`.** The team rule "no file references docs/spec-pack, agent-os, .claude, CLAUDE.md" (spec 09 strips them from `main`) collides with the root `.gitignore`, which must keep its `.claude/...` ignore entries. Every grep gate needs that exemption spelled out. In the spec PR diff on `factory`, agent files ride along (`.claude/agent-memory/**`, `agent-os/specs/**`), so whole-diff greps for qa-final run on product files only (diff minus `.claude/` and `agent-os/`). Report agent-file hits as findings for the orchestrator. The harness MEMORY.md index format uses em dashes.

3. **A probe must fail for the stated reason.** Make the verifier quote the failing assertion or exception line. Examples: a missing-migration entity without `@Id` fails as a mapping error, not as schema validation; a compile error is never a negative control.

4. **ArchUnit vacuity (allowEmptyShould everywhere).**
   - Every rule passes on empty packages, so the production run needs negative controls: a violating class in a real layer package, and a root-package class touching domain.
   - The self-test's production sanity check must read `@AnalyzeClasses` reflectively, because a hard-coded copy misses a package typo.
   - String-typed predicates (annotation FQN, package names) are silently vacuous when misspelled. Only a fixture-backed per-rule case catches that.
   - "At least one violator per rule" is too weak for a rule with several package strings (rule 5: `jakarta.persistence..` plus `org.springframework.data..`): one violator per string, or deleting the other string survives. Write that into the self-test criterion at strategy time.
   - `areAnnotatedWith` matches direct annotations only; `areMetaAnnotatedWith` also matches direct ones. Decide at strategy time which one the criterion asks for.
   - Never write "failure details contain the expected FQN" into a self-test criterion. ArchUnit details name origin AND target, and fixture chains across layers (A in query uses B in command, C in data uses A) make a designated violator the target of a neighbour rule. A one-token layer swap among near-identical rules then survives. Ask for an origin-anchored match (first `<...>` token starts with the FQN) plus `getDescription()` pinned per rule. Pinned descriptions also catch shared-instance stacking (stricter rules) and allowlist or suffix widening, which one sampled violator per rule cannot catch.
   - Completeness by reflection over `@ArchTest` fields misses `@ArchTest` methods. Add "declares no @ArchTest method" to the criterion.
   - Layered descriptions render each constraint as `where layer '<name>' may ...` (javap of 1.5.1); the layer definition lines also contain `layer '`. Count or pin `where layer '`, never the bare token.
   - A parameter predicate ("any parameter is double or float") needs four position witnesses, each its own fixture class (the origin regex accepts any member of a class, so a second member does not help): lone parameter (kills `size() > 1 &&`), first of several (kills `skip(1)`), middle of three (kills last-only and first-only), last (kills first-only). Single-parameter fixtures alone let an exact-list `haveRawParameterTypes(double.class)` pass, so keep multi-parameter ones too. Include a method and a constructor. Pin all four in the Gate 1 fixture wording; pinning only "middle" cost spec 02 two plan amendments.
   - A CompositeArchRule that repeats one package list per half needs every occurrence falsified: a fixture per package per half, or a per-half description pin (for example the full bracket occurs once per half). A set comparison over the whole description hides a drop from one half.

5. **Framework warnings vs task sequencing.** The verifier rule "green with a framework warning = FAIL" collides with skeleton-first specs: the first task's referee prints warnings that later tasks fix. At strategy time, give every expected warning an owning acceptance criterion. Known ones on this stack: Mockito inline-mock self-attach on JDK 25 (dynamic-agent and CDS warnings, fixed by a Surefire `-javaagent` argLine), JPA open-in-view, Flyway "No migrations found". Also the Maven `prefixes.txt` 401 warnings, which come from repositories declared in the pinned flyway-parent POM and are not machine-local. Put a warning-inventory item in C0: every warning quoted and classified as fixable here (FAIL), owned by a named criterion, or unowned (flag it).
   **Why:** otherwise verifiers wave warnings through as "environmental" or "owned elsewhere", and no task ever fixes the unowned ones.
   The inventory grep must be `warn|deprecat|self-attach`. Mockito's line "Mockito is currently self-attaching ..." contains no "warn" and slips through a plain `warn` grep.

6. **Frontmatter timestamps are hand-written.** Agents' `updated`/`created` values have run minutes ahead of the real file write. Prove "the referee ran after the impl finished" with `stat` mtimes (impl artifact, last product file, Surefire XML), and direct verifiers to do the same.
   **Why:** the lens-2 ordering check and stale-agent detection both silently trust a value nobody took from the clock.

7. **Scope argument instead of a directive item.** Verifiers skip global greps as "untouched by this diff". Accept that only with a quoted scope proof plus my own cheap confirmation, and direct them to execute every global item literally.

8. **Probes that remove a file need `clean`.** `./mvnw test` without `clean` keeps a moved migration or a deleted probe class in `target/`, so the probe passes falsely or the next probe fails for the previous one's reason. Write `clean test -Dtest=...` into every probe item.

9. **Shared test-infrastructure hooks need a guard test, not a code-review fallback.** A "report whether removing `@BeforeEach` fails a test, else inspection" item leaves the hook unguarded, and once accepted at strategy time I cannot REJECT on it later. Demand a guard at strategy time instead: a `@TestInstance(PER_CLASS)` subclass seeds a row in `@BeforeAll`, and its test asserts the row is gone. Check also what a fixture really exercises: FK-linked tables truncated in ONE statement do not need CASCADE, and CASCADE masks a partial table list. Require an unlinked table.

10. **Verifiers of one session share one scratchpad.** Generic evidence names (`referee.log`, `snap-before.txt`, `n1.log`) get overwritten by the next task's verifier, so an earlier review's evidence can no longer be checked again, and a verifier can cite a stale file. Put "task-prefixed scratch file names, cited by absolute path" into C0 at strategy time. In reviews, check the evidence file's mtime against the verdict before trusting it.

11. **Annotation greps match Javadoc.** `grep '@'` over Java sources hits inline tags such as `{@code ...}`. Use `grep -nE '^\s*@|^\s*import '` and read the files; the class-file check (`javap -v`, no `Runtime*Annotations`) misses SOURCE-retention annotations. `\s` works in macOS `grep -E` but silently matches nothing in `git grep -E`: write `[[:space:]]` in any `git grep` item and demand a control that bites.

12. **CI run ids in a qa-final brief.** Ids handed over by the orchestrator can belong to a task branch, not the spec tip. Never write run ids; write `gh run list --commit <tip>` and `gh run view <id> --json headSha`. The `pull_request` run's `HEAD is now at <sha> Merge <tip> into <base>` line proves the merged tree.

13. **Base moved during the cycle.** An owner commit on the base breaks `merge-base --is-ancestor`. The fix is a merge into the spec branch (never rebase or force-push). Re-run the tip-identity items (fresh clone, counts, git facts, PR readiness, CI on the tip); carry the rest forward only with `git diff --name-status <old tip> <new tip>` showing no product file. The finalize commit moves the tip again: repeat the same check before the human merge.

14. **Integration integrity at the final review.** Record each task's sha256 baselines in the ledger matrices; at final-review, hash the gated files at the spec tip and compare. One command proves that squash merges and base merges altered nothing a gate reviewed.

15. **Test data that cannot falsify the criterion.** Check the data of every listed test, not only its name:
   - Combined filters: each criterion needs a row that only it excludes. If every author match shares the genre, a dropped genre filter passes.
   - Order claims ("422 before 409", "discontinued before range") need a case where both failures are present.
   - A round trip through one mapper misses a symmetric column swap; demand one raw JDBC read of the stored row.
   - Sort and cap tests need rows inserted out of the expected order.
   - Rounding modes need both 10.005 and 10.015. A check-digit formula `(10 - s % 10) % 10` needs a valid case with check digit 0.
   - Mutation probes on annotations: removing a method-level `@Transactional` leaves a class-level one in force. Remove both, or the probe survives for the wrong reason.
   - A guard's falsifier needs input that passes every later check once the guard is gone. Otherwise a downstream check rejects it by accident. Example: an "ASCII digits only" test with U+0668 at the check-digit position is rejected by the checksum arithmetic, so the `Character.isDigit` mutant survives. A witness that works: U+0661 replacing a 5 at a weight-1 position, because `0x661 - '0' - 5 = 1580`, which is 0 mod 10. Write "a non-ASCII digit that the naive checksum still accepts" into the directive.
   - "Uses the given clock" needs a clock fixed in a past year (for example 2000-06-15) and a value that only the real clock accepts (year 2027). A clock fixed in the current real year gives the same bound as `Clock.systemUTC()`, so a code path that ignores the clock passes. Name this witness in the falsifier list of every task that hands a `Clock` on (mapper, flow).
   - `strip()` versus `trim()` needs a non-ASCII whitespace witness: U+2003, written as the ` ` escape. U+00A0 is no witness, because `Character.isWhitespace` excludes it. `Locale.ROOT` casing needs a run with `-DargLine="-Duser.language=tr -Duser.country=TR"`. Ask for both at strategy time if the criterion matters, otherwise they end up as inspection-only.

**How to apply:** at strategy time put these into the common directive block. In reviews, CHALLENGE any probe or clean-tree claim that lacks the snapshot or the quoted reason line.
