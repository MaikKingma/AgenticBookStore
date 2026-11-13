---
name: claude-code-headless-probe-techniques
description: How to probe Claude Code behaviour headless (claude -p) without touching the user's ~/.claude, and the traps that make probes lie or write user config
applies-to:
  - .claude/**
  - scripts/**
metadata:
  type: reference
---

Checked on Claude Code 2.1.319. Re-verify after an update (`claude --version`).

## Isolation that keeps subscription auth
- Use `--setting-sources project,local` plus `ENABLE_CLAUDEAI_MCP_SERVERS=false`. This drops user skills, agents,
  hooks and settings. Keychain OAuth still works.
- Do NOT use `CLAUDE_CONFIG_DIR`: the Keychain entry is keyed per config dir, so you get "Not logged in".
- Do NOT use `--bare`: it never reads OAuth and needs an API key, which is forbidden here.
- The init event still arrives before an auth failure. A scratch `CLAUDE_CONFIG_DIR` is therefore fine for zero-cost
  startup-only checks (plugins, mcp_servers, trust).
- `claude -p "/context"` is a local command: no API call, cost 0. It emits `context_usage.memory_files`, which is good
  for "what loaded at start".

## Write traps (they modify user config)
- `claude plugin marketplace add` and `plugin install --marketplace` write `extraKnownMarketplaces` into
  `~/.claude/settings.json` even with `CLAUDE_CODE_PLUGIN_CACHE_DIR` set. That variable redirects plugin storage
  only. Never run them in a probe.
- `brew cat <formula>` silently turns on Homebrew developer mode (`brew developer off` reverts it). Read formulas from
  raw.githubusercontent.com instead.

## Evidence traps
- An untrusted `-p` folder ignores project `permissions.allow` and `extraKnownMarketplaces`, and prints a warning to
  stderr. Hooks, `.mcp.json` and skill `allowed-tools` still apply. To supply allow rules, use `--settings <file>`,
  which is not trust-gated.
- A PreToolUse hook exit 2 runs BEFORE the deny rules. A red hook masks a deny, so test denies with the hook passing.
- `result.permission_denials` lists hook blocks too. Use the `system/permission_denied` event, whose
  `decision_reason_type` field says why, or the tool_result text.
- On-demand instructions (path rules, nested CLAUDE.md) are NOT in stream-json. Prove the load with an
  InstructionsLoaded hook passed through `--settings` (`load_reason: path_glob_match`, `trigger_file_path`) or with the
  transcript's `nested_memory` attachment. `/context` does not list them even after the load.
- A plugin enabled only by repo settings is never "auto-recorded" in `-p`, so it misses the cache. To load a plugin
  for one session, use `--plugin-dir <marketplace copy>/plugins/<name>`. Its plugin.json carries the lspServers.
- The jdtls wrapper's index dir is `~/Library/Caches/jdtls/jdtls-<sha1(basename(cwd))>`. Use a unique sandbox
  basename when timing the first index.

## Sources that proved authoritative
- `https://code.claude.com/docs/en/<page>.md`, fetched with curl into scratch and grepped. This beats WebFetch
  summaries.
- Key pages: permissions.md ("What runs before you trust a folder"), hooks.md, memory.md, plugins/org.md.
