---
name: spring-ai-chat-client-traps
description: Spring AI 2.0.x ChatClient/Anthropic traps (tool loop in advisor, test-double options, blank key, env fallbacks, temperature) and how to verify them offline
applies-to:
  - bookstore/src/main/java/nl/openvalue/bookstore/assistant/**
  - bookstore/src/test/java/nl/openvalue/bookstore/assistant/**
metadata:
  type: reference
---

Traps (Spring AI 2.0.2, anthropic-java-core 2.72.0; re-check on a BOM bump):
- The tool loop lives in ChatClient's auto-registered ToolCallingAdvisor, not in AnthropicChatModel.call().
- A test-double ChatModel MUST override getOptions() to return ToolCallingChatOptions. With the default
  ChatOptions the tool callbacks are dropped and the advisor skips: tools never run and the reply is empty.
- A blank api-key ("") does not fail startup. A null key makes the SDK setup read ANTHROPIC_API_KEY or
  ANTHROPIC_AUTH_TOKEN. A null base-url always reads ANTHROPIC_BASE_URL.
- `spring.ai.model.chat` = anything but `anthropic` turns the Anthropic auto-config off. ChatClient.Builder needs
  exactly one ChatModel bean.
- The memory advisor needs the ChatMemory.CONVERSATION_ID param on every call (no default in 2.0). It sits outside
  the tool loop, so memory stores only the user message and the final answer.
- Temperature, top_p and top_k are sent only when non-null. An empty yml value binds to null.
- Boot 4.x EnvironmentPostProcessor: the interface and spring.factories key are both
  `org.springframework.boot.EnvironmentPostProcessor`.

Verification technique:
- Read the request body without network access: a test helper in package `org.springframework.ai.anthropic` (test
  sources) calls the package-private `AnthropicChatModel.createRequest(prompt, false)` and inspects
  `temperature()`, `model()` and so on.
- Run the scratch build with `env -u BOOKSTORE_AI_API_KEY ./mvnw -f <scratch>/pom.xml test` so no real key leaks
  in. Never print key env vars.
- Scratch-project basics are in [[spring-ai-mcp-traps]].
