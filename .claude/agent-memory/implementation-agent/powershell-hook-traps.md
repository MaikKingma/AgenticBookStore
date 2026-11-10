---
name: powershell-hook-traps
description: Traps when writing PowerShell twins of bash hooks and testing them with pwsh in Docker from a bash harness
metadata:
  type: reference
---

- Dormant: D16 (decisions.md, factory 7ba87a9) dropped the PowerShell hooks; this matters only if they come back.
- `mcr.microsoft.com/powershell:latest` lacks arm64: pin `7.5-azurelinux-3.0[-arm64]` by digest per `uname -m`. No git
  in it: replay the host git's answers, fail any other call. Replaced HOME: export `DOCKER_CONFIG` first.
- `docker run --rm` killed before start leaves a Created container: label each run, `docker rm -f` by label on EXIT.
- `pwsh -File x.ps1` passes `exit 2` on; `pwsh -Command "& 'x.ps1'"` turns it into 1 (append `; exit $LASTEXITCODE`).
- Twin of `grep -Eq`: `-cmatch`, line by line, `[ \t\v\f\r]` for `[[:space:]]`; pin each whitespace member per side
  of the colon. Holds only in the C locale (UTF-8 `[[:space:]]` also matches U+00A0, U+3000): scope comments to ASCII.
- Quote `'-Dprop.name=value'` (5.1 splits at the dot). A native command that cannot start leaves `$LASTEXITCODE`:
  preset 127 plus try/catch per call, and kill each preset mutant with LASTEXITCODE 0 just before the call.
- Bash `"$(...)"` drops trailing empty lines; mimic and pin it. See [[bash-script-traps]].
