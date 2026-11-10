---
description: Verify, check scope and commit the current change with a conventional commit message
argument-hint: [what this change does]
disable-model-invocation: true
allowed-tools: Bash(git status *), Bash(git diff *), Bash(git add *), Bash(git commit *), Bash(./mvnw *)
---
## Current state
- Status: !`git status --short`
- Changed files: !`git diff --stat HEAD`

## Task
1. Run `./mvnw verify` and show the final summary lines. Stop and report if it fails.
2. Check that every changed file belongs to: $ARGUMENTS. List anything outside that scope and ask before including it.
3. Stage the relevant files and commit with a conventional commit message whose subject summarises: $ARGUMENTS.
4. Report the commit hash and the message.
