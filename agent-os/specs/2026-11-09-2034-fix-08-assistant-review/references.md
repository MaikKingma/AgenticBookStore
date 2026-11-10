# References

- `agent-os/specs/2026-11-09-1104-08-bookstore-assistant/plan.md`: the spec 08 cycle, its decisions, QA feed-forward
  and amendments; its QA ledger lists residuals R1, R2, R17 and R22 (R1 and R22 are fixed here, R17 is deferred).
- `bookstore/src/main/java/nl/openvalue/bookstore/assistant/AssistantCommands.java`: the chat endpoint and its check
  order; the one chat call.
- `bookstore/src/main/java/nl/openvalue/bookstore/assistant/AssistantConfig.java`: the chat memory bean
  (`MessageWindowChatMemory`, 20 messages) and the `MessageChatMemoryAdvisor`.
- `bookstore/src/main/java/nl/openvalue/bookstore/assistant/OfflineChatModel.java` and `AssistantExceptionHandler.java`:
  the existing `UnavailableException` path and the offline problem the new one mirrors.
- `bookstore/src/main/java/nl/openvalue/bookstore/domaininteraction/UnavailableException.java`: the constructor that
  keeps a cause.
- `bookstore/src/test/java/nl/openvalue/bookstore/assistant/ScriptedChatModel.java`,
  `AssistantCommandsTest.java` (mocked `ChatClient` request chain), `AssistantCommandsLengthTest.java`,
  `bookstore/src/test/java/nl/openvalue/bookstore/AssistantIntegrationTest.java` (scripted model, memory scenario),
  `AssistantModeIntegrationTest.java` (`startsOfflineWithoutAKey`).
- `bookstore/src/main/java/nl/openvalue/bookstore/assistant/AssistantModeConfig.java` and
  `bookstore/src/test/resources/application-test.yml`: how offline mode is chosen and why an exported variable wins.
- `bookstore/src/test/java/nl/openvalue/bookstore/IntegrationTestBase.java`: the shared integration test base.
- `frontend/src/pages/AssistantPage.tsx` (`shownProblem`) and `AssistantPage.test.tsx`.
- `agent-os/specs/2026-11-09-1010-fix-12-storefront-review/`: the previous lite bugfix in this repo.
