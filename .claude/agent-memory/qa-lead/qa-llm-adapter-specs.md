---
name: qa-llm-adapter-specs
description: QA strategy traps for specs that add an LLM adapter (Spring AI ChatClient, tools, chat memory, offline mode, shared API key, rate limit, JS client length limits); keyed referee, environment-reading unit tests, framework-default-equivalent mutants, network-safe probes, EPP pipeline order, catch-all tool error helpers (NPE kills, parser catch behind a guard), failure translation with memory restore (cause, catch type, concurrency); read before the strategy of any model or tool-calling spec
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

5. **Chat memory window semantics (Spring AI 2.0.2, read in the sources jar).** The memory advisor stores the user
   message BEFORE the model call and the final answer after it (never tool messages). At request 11 of a 20-window the
   prompt history is Q1..A10 plus Q11; at request 12 it is Q2..A11 plus Q12. `MessageWindowChatMemory.process` snaps
   the cut forward to the next USER message, so with complete turns only an odd window equals the even one below it:
   exact lists kill 19 but never 21 (my spec 08 G11 premise was wrong). Witness: a first call that throws (its question
   stays unanswered), then exact history at a later request. Same fact: an offline check inside the model leaves the
   question in memory, so "no memory after an offline request" is the killer of a removed pre-call offline check.

6. **The tool-calling double trap.** A `ChatModel` double whose `getOptions()` is not `ToolCallingChatOptions` drops tools
   silently (empty reply, tool never called). Make it a must-fail probe on the double itself and require an in-test
   negative control that differs only in `getOptions()`. A client built without tools passes tool-calling options
   with null callbacks: the double must handle that (else a `defaultTools`-dropped mutant dies only by NPE), and it
   must record a call that fails for lack of a step.

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
    names). Put it in the strategy's falsifier list for the EPP task. Compare only names that start with
    `Config resource`: Boot appends its own `applicationInfo` source after every EPP, so "every name containing
    `application`" is red on correct code (spec 08 G24 had to be reworded). List real runtime values before wording
    such a falsifier.
    The key blank guard (`isBlank`) needs the U+2003 witness too, like every other blank guard.

12. **Tool adapters with a catch-all error helper.** Two blind spots recur:
    - A mutant (a cap of 11, an eager lookup) can be "killed" only because an unstubbed mock returns null, the
      resulting NPE becomes the generic text, and that text fails the test. The kill is real but fragile. Demand
      lenient stubs on the boundary row so the asserting line kills it.
    - A parse guard in front of a library parser (`length != 36` before `UUID.fromString`) hides the parser's catch.
      Every row fails the guard first, so no row enters the catch. Ask for a row that passes the guard and fails the
      parser (36 characters, not hex).
    For rejections, an empty `details` list is the natural default and lets a defensive filter survive. Ask for a
    non-empty foreign-key witness at strategy time. Before accepting an "unreachable" claim, read the flow methods the
    tool actually calls, not the whole flow class (spec 08 T2).

13. **Failure translation and memory restore at the chat boundary (fix-08 T1).** The use-case exception's cause has
    no consumer (`AssistantExceptionHandler` reads message, title, code, details only), so a dropped-cause mutant
    survives every web and IT test; a "WARN without throwable" mutant dies only by NPE unless the test asserts
    `getThrowableProxy()` isNotNull first; `catch (Throwable)` for `RuntimeException` has no test. If the decision is
    binding, list the witnesses up front: a direct `commands.chat(...)` call with `hasCauseReference`, and a gate grep
    on the catch types. A dropped `List.copyOf` on the snapshot is equivalent (the in-memory repository returns a
    fresh `ArrayList` and stores `List.copyOf`). Snapshot plus `clear`/`add` restore can erase a concurrent turn on the
    same id: price it as a residual. Once failures answer 503 `assistant-unavailable`, an escaped live call in an
    offline test also answers 503: offline assertions must check the code, never the status alone. Docs naming causes
    (`javap`, fix-09 T2): `SpringAiAnthropicHttpClient.execute` wraps every `IOException` (timeouts) in `AnthropicIoException`; 401/429 are `AnthropicServiceException`, all `RuntimeException`.

14. **Provider-shape defects need a stub contract test of the real model client (fix-08 T4).** Model doubles never
    emit provider blocks (signed thinking), so a library replay bug shows only live. Review rules: the stub answers on
    the request field the way the API does (thinking block only when `thinking.type` is not `disabled`); after a
    config fix the request-field assertion is the discriminator, replay-shape assertions carry the red shape; check
    red fidelity by matching the red body's message/content indices to the provider's error text; demand a guard
    test for `defaultOptions` merge side effects (model, `max_tokens`, sampling params) killed by override mutants;
    an unconditional option vs a model chosen by env variable is a residual. Live acceptance stays a keyed re-check.
    A loopback stub answering 500 sees 3 requests per chat (no retry setting in `AssistantConfig`, the client retries
    twice) but the app logs 1 WARN: brief stub counts as chats x 3, WARN counts per chat (spec 09 qa-final R19).

15. **Test contexts against exported variables (fix-08 T3, A4).** Env vars outrank `application-test.yml`. Pin a blank
    key plus a loopback base URL (`http://127.0.0.1:9`) in the test base's `@TestPropertySource`; never
    `spring.ai.model.chat=none` there (the EPP then skips and the switch's full-boot witness goes red). Falsifiers:
    first source holding each key indexed before `systemEnvironment` (filter `configurationProperties`), exact URL
    value. Proof the URL binds: an information run with the model exported, per-host `Failed to connect` counts. Spring
    AI 2.0.2 has one URL property; `AnthropicSetup` reads `ANTHROPIC_BASE_URL` only when it is null.

16. **Prompt and tool-text fixes (fix-08 T5).** Pin each rule by meaning-bearing `contains` phrases, the tool
    description and `@ToolParam` text with `isEqualTo` (schema `properties.<p>.description`). Survivors are
    usually restatements or motivation sentences: no RA when the remaining pinned text still carries the rule and the
    rule is pinned where the model reads it at call time; gate-grep them at qa-final. A readable list in the prompt
    ("science fiction") invites that form as a tool argument: check the parser's accepted forms (`Genre.parse`: case,
    `-`, `_`, no space) and that the tool path really calls it. Behaviour stays live-only: sample a live check 3 times.

10. **DoD grep over `target/` with a fat jar.** Check the dependency jars' raw bytes (`grep -c -a <literal> <jar>`) at
    strategy time: deflated entries do not match, so the repackaged jar is expected clean; the run after the build is
    still the evidence.
