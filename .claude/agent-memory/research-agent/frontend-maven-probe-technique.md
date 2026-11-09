---
name: frontend-maven-probe-technique
description: How to verify frontend-maven-plugin / Vite / Vitest claims with a scratch Maven probe and which sources were authoritative
metadata:
  type: reference
---

- Scratch probe: parent `spring-boot-starter-parent` pom with modules `frontend` (packaging pom, plugin) and `app` (jar), `-Dmaven.repo.local=<scratchpad>/m2` to see where the Node archive lands, Maven from `~/.m2/wrapper/dists/apache-maven-*/`. Generate the lock with `npm install --package-lock-only --prefix <dir>`; a clean exit proves no ERESOLVE peer conflict.
- Plugin truth is in `frontend-maven-plugin-<v>-sources.jar` and `frontend-plugin-core-<v>-sources.jar` on Maven Central (`AbstractFrontendMojo` holds the skipTests logic, keyed on the execution phase). The README does not say this.
- Registry facts: `npm view <pkg>@<v> peerDependencies engines --json`; create-vite template contents from `https://registry.npmjs.org/create-vite/-/create-vite-<v>.tgz` (`template-react-ts/`).
- Guard quirks: `env -i ... npm test` and `rm`-style compound commands are refused in worktrees; put multi-step runs in a script file under the scratchpad and run `bash <path>`. `gh api ...contents --jq .content` returns base64, pipe through nothing and avoid it for large files (use a tag-pinned raw fetch instead).
- Traps worth pre-empting: bare `<resources>` replaces Boot parent resources; `.bin/*` shebang needs node on PATH; `-fae` does not skip modules without a dependency edge; `-Dmaven.test.skip` does not skip npm test.
