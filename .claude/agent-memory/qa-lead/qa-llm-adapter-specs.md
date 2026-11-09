---
name: qa-llm-adapter-specs
description: QA strategy traps for specs that add an LLM adapter (Spring AI ChatClient, tools, chat memory, offline mode, shared API key, rate limit, JS client length limits); keyed referee, environment-reading unit tests, framework-default-equivalent mutants, network-safe probes, EPP pipeline order; read before the strategy of any model or tool-calling spec
metadata:
  type: feedback
---

Settled while writing the spec 08 (bookstore assistant) strategy. See also [[qa-directive-traps]] and
[[qa-test-data-falsifiers]].

1. **Keyed referee as the offline proof.** "Tests stay offline even with the key exported" is only proven by running the
   referee a second time with a fake key exported (`BOOKSTORE_AI_API_KEY=probe-not-a-key bash with-budget.sh ...`, a
   literal env prefix is allowed by the shell guard): same totals and Started count, zero `401|UnknownHost|...` lines.
   Surefire forks inherit the shell environment unless the pom sets `environmentVariables`; check that premise.
   **Why:** it also catches unit tests that read the process environment, which no single plain run can show.

2. **No-context config tests read the real environment.** A `StandardEnvironment` includes `systemEnvironment`, so a
   yml/EPP test "key empty without the variable" goes red in a shell that exports it. Rewrite at Gate 1: build the
   environment without system sources (or `MockEnvironment`) and feed the variable through a `MapPropertySource`.

3. **Never remove the test-profile key blank in a probe once an endpoint exists.** Before the controller exists the probe
   only builds a client bean (no request); afterwards an offline-path test would send a real request with the fake key.
   Allow the D1-removed probe in the first task only and name it.

4. **Framework-default-equivalent mutants.** Spring AI auto-config supplies a `MessageWindowChatMemory` (20, in memory)
   `@ConditionalOnMissingBean`, and 20 is the builder default: removing our bean or `.maxMessages(20)` changes no test.
   Pin the bean's origin (`getBeanDefinition(name).getFactoryBeanName()`) and treat the window literal as inspection.
   Same family: an EPP that `addLast`s a property behind an "unset" guard; each single mutant (`addFirst`, guard removed)
   is equivalent, only the pair is must-fail. Look for library defaults before listing a mutant as must-fail.

5. **Chat memory window semantics (Spring AI 2.0.2, res-1 verified ordering).** The memory advisor reads memory before
   adding the new user message and stores only the user message and the final answer. At request 11 of a 20-window the
   prompt history is Q1..A10 plus Q11; at request 12 it is Q2..A11 plus Q12. Ask for the full ordered text list at both
   requests: it kills window 19 and 21.

6. **The tool-calling double trap.** A `ChatModel` double whose `getOptions()` is not `ToolCallingChatOptions` drops tools
   silently (empty reply, tool never called). Make it a must-fail probe on the double itself and require an in-test
   negative control that differs only in `getOptions()`.

7. **Adapter layer rules cut off domain data.** An inbound adapter allowed only `domaininteraction` cannot read a domain
   enum (`Genre.values()`); a card saying "lists every Genre name" must take them from the use-case exception's
   `details`. Witness with made-up names so a hard-coded list fails. Grep the layer rules before accepting such text.

8. **Client-mirrored contracts.** A JS client counts UTF-16 units: test the server limit with a supplementary character
   (two units), a non-ASCII BMP character (kills byte length) and a trailing space (kills `strip()`). Copy the client's
   exact `Accept` header (read it from the client source on the base branch) into at least one error case per status,
   because ProblemDetail content negotiation depends on it.

9. **Sliding-window limiter witnesses.** Start instants need a sub-second fraction (kills seconds truncation); "slides"
   needs exactly k freed AND the next rejected (kills a tumbling window); "rejections not counted" needs exactly the
   limit then one more rejected; a `synchronized` removal probe is nondeterministic, so require a start latch plus many
   rounds and pre-declare the three-run rule.

11. **Pipeline position is invisible to direct-call unit tests.** An `EnvironmentPostProcessor` that reads a
    config-file value (the offline switch) is wrong if it runs before `ConfigDataEnvironmentPostProcessor`
    (`ORDER` = HIGHEST + 10 in Boot 4.1.2): the key reads null and the app stays offline even with a key. Unit tests
    that call `postProcessEnvironment` directly and a test profile with a blank key are green in both orders, so an
    added `@Order(HIGHEST_PRECEDENCE)` survives everything. Falsifier: in the shared full context, the switch's
    property source sits after every `application*.yml` config source (index comparison, with non-vacuity on both
    names). Put it in the strategy's falsifier list for the EPP task.
    The key blank guard (`isBlank`) needs the U+2003 witness too, like every other blank guard.

10. **DoD grep over `target/` with a fat jar.** Check the dependency jars' raw bytes (`grep -c -a <literal> <jar>`) at
    strategy time: deflated entries do not match, so the repackaged jar is expected clean; the run after the build is
    still the evidence.
