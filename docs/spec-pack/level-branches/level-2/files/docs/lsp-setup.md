# LSP setup for Claude Code (Java)

## What you get

Claude Code has a built-in, read-only LSP tool: go to definition, find references, type info, document and
workspace symbols, implementations and call hierarchy. After every edit, diagnostics are reported automatically.
There is no enable flag, and none is needed.

## 1. Install the language server binary

The plugin does not ship the `jdtls` binary. It needs JDK 21 or later (the upstream `jdtls` wrapper refuses older Java).

- macOS: `brew install jdtls`
- Arch Linux: `yay -S jdtls`
- Other Linux: extract a JDT LS release and put a `jdtls` wrapper script on your `PATH`.
- Windows: use WSL.

Check with `which jdtls` (PowerShell: `Get-Command jdtls`).

## 2. Install the plugin

Run `/plugin install jdtls-lsp@claude-plugins-official` in Claude Code. If the marketplace is missing, first run
`/plugin marketplace add anthropics/claude-plugins-official`.

On this branch the plugin is also declared in `.claude/settings.json`, so Claude Code can load it after you accept
the workspace trust dialog.

## 3. Verify

Add a deliberate type error to a `.java` file, for example assign a `String` to an `int`. Claude Code prints
`Found N new diagnostic issues ...`. Remove the error again.

## Troubleshooting

Nothing appears: open `/plugin` (tab **Errors**) and start `claude --debug`.
`Executable not found in $PATH: "jdtls"` means the binary is missing or not on your `PATH`.

More: https://code.claude.com/docs/en/plugins/code-intelligence
