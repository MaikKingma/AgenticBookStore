---
name: view-and-detail-pin-probes
description: Probes for JSON view mappers (derived-flag region guards, null-key forms) and for wrapped-cause detail messages; how to verify a Required Action outline on patched test copies
metadata:
  type: feedback
---

Rule: for a view record that copies an optional component, probe a guard on every DERIVED flag the DTO exposes, not only the null check.

**Why:** a view mapping `pending == null || !stock.lowStock() ? null : ...` survived every witness because all pending-restock fixtures were low items (a fixture that sets the pending restock usually also sets stock low).
**How to apply:**
- Write the guard mutants in both polarities and at the boundary (`<`, `<=`, `>=`, `!=` on available vs reorder level), then check which region each existing witness sits in. A witness strictly above, one at, and one below the limit kills all of them.
- Null-key forms: `jsonPath("$", hasKey(k))` plus `value(nullValue())`, never `doesNotExist()`. `@JsonInclude` NON_NULL, NON_EMPTY, NON_ABSENT on the class and on the component all die on `hasKey`, and NON_DEFAULT also dies on dropped zero/false keys. Run the class-level one once under the full referee to prove the IT pins (a Map `isEqualTo` with a null value is itself a presence check).
- A list view needs element-index mutants: first, last, next and previous element's pending restock, plus a filter dropping null-pending items. The "pending only on the middle item" fixture kills all five.

Rule: a wrapped-cause detail message is pinned by `isEqualTo(prefix + domainMessage)` plus a cause-message assertion, not `startsWith`.

**Why:** `startsWith(prefix)` left the part after `": "` free, so `e.toString()`, an appended suffix and the class name all survived; a cause pinned only by `isExactlyInstanceOf` also let a different exception of the same type survive.
**How to apply:** probe the suffix expression (`getMessage`, `toString`, `getClass().getSimpleName()`, appended text, upper-cased, substring, dropped) and the cause (dropped, `getCause()`, wrapped, same type with another message). For a parameterized test derive the expected domain message per case inside the test.

Technique: verify a Required Action outline before offering it (FF1). Generate patched copies of the test classes from the `.orig` by exact replacement (`python3 -I`), install them with a wrapper script, run the survivors, the boundary twins and the earlier kills, restore with `cp -p` and compare hashes. A CONTROL run on the fix must be green with the new cases (old total plus the new tests).

Technique: a test-only rework round reuses the earlier round's mutant directories unchanged. Prove production is byte-identical (live hash vs the earlier hash file and `cmp` against the `.orig`), re-snapshot `.orig` for the two changed test files (keep a copy of the old snapshot first), then run CONTROL, the named probes and ALL earlier probes against the new tests (100 runs took about 4 minutes). Expect failure counts to equal the earlier ones or rise, never fall; diff the test files against the old `.orig` (pure insertions plus the one named assertion) before re-snapping, because the re-snap erases that baseline.

Related: [[env-switch-mutant-runner]], [[shell-traps]]
