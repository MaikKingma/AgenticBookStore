---
title: Setup check
sidebar_position: 2
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

Do this before the workshop if you can, and again in the first 10 minutes of the day. Each step below is one command and what it must show.

## What you need

Arrive with this installed:

- Claude Code, logged in with a Pro/Max/Team/Enterprise account
- Temurin 25 (Java 25)
- git
- Docker Desktop (or another Docker engine), installed and running
- the Java language server `jdtls`, on the `PATH`

Node.js is not needed for the build: it installs its own copy. You only need Node.js 22.12 or later if you pick Track A (OpenSpec) and install it with npm; the install script needs no Node.js.

Windows participants also need Git for Windows (Git Bash). The `Bash` matcher, the `if` field and the `Bash(...)` permission rules of Level 3 only apply when Claude Code runs commands through Bash.

## Check your tools

Run each command in a terminal.

Claude Code is installed:

```bash
claude --version
```

Claude Code is logged in. Start it, then type `/status`. It must show that you are logged in with a Pro/Max/Team/Enterprise account.

```bash
claude
```

Java is version 25:

```bash
java -version
```

Docker is running (the tests and the local database need it):

```bash
docker info
```

The Java language server is on the `PATH`. If this fails, install it in the next section.

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

Clone the repository:

```bash
git clone https://github.com/MaikKingma/AgenticBookStore.git && cd AgenticBookStore
```

:::tip[Checkpoint]

```bash
./mvnw verify
```

Expected: the build ends green. The Maven dependencies and the PostgreSQL image were downloaded at home, so run this once at home before the workshop.

:::

If something is red, see [Docker is not running](./reference/troubleshooting.md#docker-not-running) and [jdtls not starting](./reference/troubleshooting.md#jdtls-not-starting).

## Install jdtls

The plugin does not ship the binary. `jdtls` needs JDK 21 or later, and you have 25.

<Tabs groupId="jdtls">
  <TabItem value="macos" label="macOS">

```bash
brew install jdtls
```

  </TabItem>
  <TabItem value="arch" label="Arch Linux">

```bash
yay -S jdtls
```

  </TabItem>
  <TabItem value="linux" label="Other Linux">

Extract a JDT LS release and put a `jdtls` wrapper script on the `PATH`.

  </TabItem>
  <TabItem value="wsl" label="Windows (WSL)">

Use WSL and install as on Linux there. Native Windows is not verified yet.

  </TabItem>
</Tabs>

The setup check covers the binary only. The `jdtls-lsp` plugin is installed in [Level 2 Part A](./03-level-2-give-your-agent-eyes.mdx#part-a-lsp), so Levels 0 and 1 run without it.

## Install OpenSpec (Track A only)

Only if you pick Track A (OpenSpec) in Level 4. The start branch holds the OpenSpec project files but not the `openspec` program, so install it now. The install script needs no Node.js. The version is pinned to 1.16.0, so everybody has the same one:

<Tabs groupId="os">
  <TabItem value="unix" label="macOS/Linux">

```bash
curl -fsSL https://openspec.dev/cli/install.sh | OPENSPEC_VERSION=v1.16.0 sh
```

  </TabItem>
  <TabItem value="windows" label="Windows PowerShell">

```powershell
$env:OPENSPEC_VERSION = "v1.16.0"
irm https://openspec.dev/cli/install.ps1 | iex
```

  </TabItem>
</Tabs>

If the installer asks you to add its folder to your `PATH`, do that and open a new terminal. With Node.js 22.12 or later, npm installs the same version instead:

```bash
npm install -g @fission-ai/openspec@1.16.0
```

Check the install. This prints 1.16.0:

```bash
openspec --version
```

## Set the workshop key

The workshop key is shown on the screen. Never commit it. Set it in the terminal where you [start the app](#start-the-app):

<Tabs groupId="os">
  <TabItem value="unix" label="macOS/Linux">

```bash
export BOOKSTORE_AI_API_KEY=<key on screen>
```

  </TabItem>
  <TabItem value="windows" label="Windows PowerShell">

```powershell
$env:BOOKSTORE_AI_API_KEY="<key on screen>"
```

  </TabItem>
</Tabs>

In WSL use the macOS/Linux form.

:::warning[One key only]

The workshop only ever uses `BOOKSTORE_AI_API_KEY`. If `ANTHROPIC_API_KEY` is defined in your shell, Claude Code would switch to that key. The `/status` check above is how you confirm that Claude Code uses your subscription. More in [the `ANTHROPIC_API_KEY` pitfall](./reference/troubleshooting.md#anthropic-api-key).

:::

## Start the app

In the same terminal that holds the key, from the repository root, start the bookstore:

```bash
./mvnw -pl bookstore spring-boot:run
```

Docker must be running: the app starts PostgreSQL in a container. When it is up, open the storefront at http://localhost:8080/shop/. Stop the app with Ctrl+C. The database keeps running when the app stops.

## Choose the model

Also inside Claude Code, run `/model sonnet`. Pro usage limits are shared over a 5-hour window and Opus (the default) drains them faster. Sonnet comfortably covers four hours of levels. See [Usage limits](./reference/troubleshooting.md#usage-limits) if you hit a limit.

## You are ready when

- `claude --version` prints a version and `/status` shows your subscription.
- `java -version` shows 25.
- `docker info` works.
- `which jdtls` (or `Get-Command jdtls`) finds the language server.
- `./mvnw verify` ends green.
- `BOOKSTORE_AI_API_KEY` is set in your terminal.
