---
title: Troubleshooting
sidebar_position: 3
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

Find your symptom below. Each section holds the checks in the order to try them.

## Windows hooks {/* #windows-hooks */}

Claude Code runs natively on Windows, but the hook scripts of Level 3 are bash scripts. Use Git for Windows (Git Bash) or WSL2.

Agent OS needs bash (WSL or Git Bash). The Java language server `jdtls` runs through WSL, see [jdtls not starting](#jdtls-not-starting).

## jdtls not starting {/* #jdtls-not-starting */}

Levels 0 and 1 run without the plugin, so you lose nothing before Level 2.

First check that the binary is on the `PATH`:

<Tabs groupId="os">
  <TabItem value="unix" label="macOS/Linux">

```bash
which jdtls
```

  </TabItem>
  <TabItem value="windows" label="Windows PowerShell">

```powershell
Get-Command jdtls
```

  </TabItem>
</Tabs>

On Windows, run the check inside WSL. Native Windows is not verified yet. If the binary is missing, install it as in the [setup check](../00-setup-check.md#install-jdtls).

If the binary is there but nothing appears in Claude Code, check `/plugin` (tab **Errors**) and start Claude Code with debug output:

```bash
claude --debug
```

The error `Executable not found in $PATH: "jdtls"` means the binary is missing.

The debugging options in step 5 of [Level 2, Part A](../03-level-2-give-your-agent-eyes.mdx#part-a-lsp) list five checks and a prompt that lets Claude diagnose the setup without changing anything.

Still stuck after two minutes? Continue with Part B of [Level 2](../03-level-2-give-your-agent-eyes.mdx#part-b-mcp).

## Usage limits {/* #usage-limits */}

Pro usage limits are shared over a 5-hour window, and Opus (the default) drains them faster. Switch to Sonnet inside Claude Code:

```text
/model sonnet
```

Check where you stand with `/usage`. If you hit the limit anyway, pair up with a neighbour.

## MCP not connecting {/* #mcp-not-connecting */}

Start the distributor first. It listens on port 8090:

```bash
./mvnw -pl distributor spring-boot:run
```

Then check `/mcp` inside Claude Code: it must show the distributor connected. If Claude Code asks you to approve the project MCP server, approve it. Restart Claude Code if the distributor does not show up.

The fallback is the STDIO transport instead of HTTP. Build the distributor:

```bash
./mvnw -pl distributor package
```

Then register the jar. Replace `<absolute path>` with the absolute path that leads to the `distributor` folder of your clone:

```bash
claude mcp add distributor -- java -jar <absolute path>/distributor/target/distributor.jar --spring.profiles.active=stdio
```

## The `ANTHROPIC_API_KEY` pitfall {/* #anthropic-api-key */}

If `ANTHROPIC_API_KEY` is defined in your shell, Claude Code would switch to that key. The workshop only ever uses `BOOKSTORE_AI_API_KEY`.

Check inside Claude Code: `/status` must show your subscription. If it shows something else, this guide has no command to fix it for you: ask for help in the room.

## Offline copy shows a blank page {/* #offline-copy-blank-page */}

In Safari, the first load of the offline copy can come up with an empty page. Reload the page once. If it stays empty, open the offline copy in Chrome or Firefox. The online guide is not affected.

## Docker is not running {/* #docker-not-running */}

The tests and the local database need Docker. Check it:

```bash
docker info
```

If this fails, start Docker Desktop (or your Docker engine) and run it again. A missing Docker daemon is an environment error, not a reason to skip tests. If you cannot get Docker running, pair up with someone next to you.

## I am far behind {/* #far-behind */}

Skip the stretch goals and the optional Stop hook first, and keep Level 4. If you are still far behind at the start of a level, run the catch-up command of that level. It is the same command as in the at a glance box of the level page. Your uncommitted work stays recoverable in the stash.

Level 0:

```bash
git stash -u && git switch -C level-0 level-0-start
```

Level 1:

```bash
git stash -u && git switch -C level-1 level-0-start
```

Level 2:

```bash
git stash -u && git switch solution/level-1
```

Level 3:

```bash
git stash -u && git switch solution/level-2
```

Level 4 (replace `<track>` with `openspec`, `speckit` or `agentos`):

```bash
git stash -u && git switch start/level-4-<track>
```
