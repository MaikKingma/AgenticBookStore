---
title: Wrap-up
sidebar_position: 8
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

The last 15 minutes are in the room. This page is the recap and your take-home material.

## Who cleared which level

In the room, a show of hands: who cleared which level? Count Level 0 to Level 4. Check yourself against the list of each level:

- [Level 0 cleared when](./01-level-0-break-the-agent.mdx#level-cleared-when)
- [Level 1 cleared when](./02-level-1-context-engineering.mdx#level-cleared-when)
- [Level 2 cleared when](./03-level-2-give-your-agent-eyes.mdx#level-cleared-when)
- [Level 3 cleared when](./04-level-3-behavioral-control.mdx#level-cleared-when)
- [Level 4 cleared when](./05-level-4-spec-driven-development.mdx#level-cleared-when)

## Show and tell

Two or three participants show their harness folder (`tree .claude`) and their spec.

## The recap map

Back to the keynote: context, behaviour, structure, verification.

In Level 0 your observation card sorted what went wrong into three columns: didn't know (context), didn't care (behaviour), didn't check (verification). Levels 1 to 4 worked through those columns:

- [Level 1: Context engineering](./02-level-1-context-engineering.mdx)
- [Level 2: Give your agent eyes (LSP + MCP)](./03-level-2-give-your-agent-eyes.mdx)
- [Level 3: Behavioral control](./04-level-3-behavioral-control.mdx)
- [Level 4: Spec-driven development](./05-level-4-spec-driven-development.mdx)

The [cheat sheet](./reference/cheat-sheet.md) keeps the commands and files of the day on one page.

## Take-home

Good news first: the guide stays online, so you can come back to every level.

The workshop API key is revoked tonight.

### Anthropic Academy courses

- [Claude Code 101](https://anthropic.skilljar.com/claude-code-101)
- [Claude Code in Action](https://anthropic.skilljar.com/claude-code-in-action)
- [Introduction to agent skills](https://anthropic.skilljar.com/introduction-to-agent-skills)
- [Introduction to subagents](https://anthropic.skilljar.com/introduction-to-subagents)
- [Introduction to Model Context Protocol](https://anthropic.skilljar.com/introduction-to-model-context-protocol)

All five are in the [Anthropic Academy catalogue](https://anthropic.skilljar.com/).

### Next steps

- Open harnesses, for example [OpenCode](https://opencode.ai/).
- Build your own harness on the [Claude Agent SDK](https://code.claude.com/docs/en/agent-sdk/overview).

## Install a spec framework yourself

Level 4 used prepared branches, so you did not install anything. To use a framework in your own repository, install it and then initialise it in the repository.

### OpenSpec

<Tabs groupId="os">
  <TabItem value="unix" label="macOS/Linux">

```bash
curl -fsSL https://openspec.dev/cli/install.sh | sh
```

  </TabItem>
  <TabItem value="windows" label="Windows PowerShell">

```powershell
irm https://openspec.dev/cli/install.ps1 | iex
```

  </TabItem>
</Tabs>

Or with npm (Node 22.12+):

```bash
npm install -g @fission-ai/openspec@latest
```

Then, in your repository:

```bash
openspec init --tools claude
```

### Spec Kit

```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
```

```bash
uv tool install specify-cli
```

Then, in your repository. Commit or branch first.

```bash
specify init --here --force --integration claude --script sh
```

On Windows the script type is `--script ps`.

### Agent OS

```bash
git clone https://github.com/buildermethods/agent-os.git ~/agent-os && rm -rf ~/agent-os/.git
```

Then, in your repository:

```bash
~/agent-os/scripts/project-install.sh
```

Agent OS needs bash. On Windows use WSL or Git Bash.

More on each framework is in its repository: [OpenSpec](https://github.com/Fission-AI/OpenSpec), [Spec Kit](https://github.com/github/spec-kit) and [Agent OS](https://github.com/buildermethods/agent-os).
