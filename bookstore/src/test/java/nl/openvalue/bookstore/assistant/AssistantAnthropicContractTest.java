package nl.openvalue.bookstore.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import nl.openvalue.bookstore.domaininteraction.BookDTO;
import nl.openvalue.bookstore.domaininteraction.CatalogueFlow;
import nl.openvalue.bookstore.domaininteraction.InventoryFlow;
import nl.openvalue.bookstore.domaininteraction.StockItemDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The assistant's chat client with the real {@link AnthropicChatModel} against a local stub of the Messages API, so
 * the request bodies the model client really sends can be read. No Spring context, no key, no network beyond the
 * loopback address.
 *
 * <p>The stub answers like the live API: a request that does not switch thinking off gets a signed thinking block in
 * front of the answer, a request that does gets the answer alone. Spring AI 2.0.2 cannot replay a signed thinking
 * block in order (it writes the text before the thinking block and stores the thinking text as a second assistant
 * message), so the live API rejects the second message of a conversation. The chat client therefore has to switch
 * thinking off, and a follow-up turn must replay the earlier answer as one plain assistant message.
 */
@ExtendWith(MockitoExtension.class)
class AssistantAnthropicContractTest {

    private static final String DUMMY_KEY = "sk-ant-dummy-not-a-key";
    private static final String MODEL = "claude-haiku-5-5";
    private static final int MAX_TOKENS = 1024;
    private static final UUID PERSUASION_ID = UUID.fromString("5e1f0c2a-7b3d-4e8f-9a10-2b3c4d5e6f70");
    private static final String TOOL_USE_ID = "toolu_1";
    private static final String THINKING_TEXT = "pondering";
    private static final List<String> THINKING_BLOCKS = List.of("thinking", "redacted_thinking");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** One answer of the stub: its text and, when set, a tool call that follows the text. */
    private record Step(String text, String toolName, Map<String, Object> toolInput) {
    }

    @Mock
    private CatalogueFlow catalogue;

    @Mock
    private InventoryFlow inventory;

    private final Queue<Step> script = new ConcurrentLinkedQueue<>();
    private final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;

    @AfterEach
    void stopTheStub() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void followUpTurnReplaysNoThinkingBlock() throws IOException {
        script.add(reply("Hello there"));
        script.add(reply("We carry fiction and history"));
        ChatClient chatClient = chatClient();
        String conversation = UUID.randomUUID().toString();

        String first = ask(chatClient, conversation, "What can you do?");
        String second = ask(chatClient, conversation, "And what genres do you have?");

        assertThat(first).isEqualTo("Hello there");
        assertThat(second).isEqualTo("We carry fiction and history");
        assertThat(requests).hasSize(2);
        JsonNode turnTwo = requests.get(1);
        assertSoftly(softly -> {
            softly.assertThat(requests).extracting(AssistantAnthropicContractTest::thinkingType)
                    .as("thinking.type of each request").containsOnly("disabled");
            softly.assertThat(roles(turnTwo)).as("roles of turn 2").containsExactly("user", "assistant", "user");
            softly.assertThat(blockTypes(turnTwo)).as("block types per message of turn 2")
                    .containsExactly(List.of("text"), List.of("text"), List.of("text"));
            softly.assertThat(textsOf(turnTwo)).as("texts of turn 2").containsExactly("What can you do?",
                    "Hello there", "And what genres do you have?");
            softly.assertThat(allBlockTypes(requests)).as("block types in all requests")
                    .doesNotContainAnyElementsOf(THINKING_BLOCKS);
            softly.assertThat(allTexts(requests)).as("texts in all requests").doesNotContain(THINKING_TEXT);
        });
    }

    @Test
    void toolRoundReplaysNoThinkingBlock() throws IOException {
        when(catalogue.search("austen", null, null, null)).thenReturn(List.of(persuasion()));
        when(inventory.findByBookId(PERSUASION_ID)).thenReturn(new StockItemDTO(PERSUASION_ID, 4, 1, 1, 5, false));
        script.add(replyWithToolCall("Let me look", "searchCatalogue", Map.of("query", "austen")));
        script.add(reply("Persuasion is in stock."));
        script.add(reply("It is a romance."));
        ChatClient chatClient = chatClient();
        String conversation = UUID.randomUUID().toString();

        String first = ask(chatClient, conversation, "Anything by Austen?");
        String second = ask(chatClient, conversation, "What kind of book is it?");

        assertThat(first).isEqualTo("Persuasion is in stock.");
        assertThat(second).isEqualTo("It is a romance.");
        verify(catalogue).search("austen", null, null, null);
        assertThat(requests).hasSize(3);
        JsonNode toolLoop = requests.get(1);
        JsonNode turnTwo = requests.get(2);
        assertSoftly(softly -> {
            softly.assertThat(requests).extracting(AssistantAnthropicContractTest::thinkingType)
                    .as("thinking.type of each request").containsOnly("disabled");
            softly.assertThat(allBlockTypes(requests)).as("block types in all requests")
                    .doesNotContainAnyElementsOf(THINKING_BLOCKS);
            softly.assertThat(allTexts(requests)).as("texts in all requests").doesNotContain(THINKING_TEXT);
            softly.assertThat(roles(toolLoop)).as("roles of the tool loop request")
                    .containsExactly("user", "assistant", "user");
            softly.assertThat(blockTypes(toolLoop)).as("block types per message of the tool loop request")
                    .containsExactly(List.of("text"), List.of("text", "tool_use"), List.of("tool_result"));
            softly.assertThat(toolResultOf(toolLoop)).as("tool result in the tool loop request")
                    .contains(TOOL_USE_ID, PERSUASION_ID.toString(), "Persuasion");
            softly.assertThat(roles(turnTwo)).as("roles of turn 2").containsExactly("user", "assistant", "user");
            softly.assertThat(textsOf(turnTwo)).as("texts of turn 2").containsExactly("Anything by Austen?",
                    "Persuasion is in stock.", "What kind of book is it?");
        });
    }

    @Test
    void everyRequestKeepsTheConfiguredModelAndMaxTokensAndNoSamplingParameter() throws IOException {
        script.add(reply("Hello there"));
        script.add(reply("We carry fiction and history"));
        ChatClient chatClient = chatClient();
        String conversation = UUID.randomUUID().toString();

        ask(chatClient, conversation, "What can you do?");
        ask(chatClient, conversation, "And what genres do you have?");

        assertThat(requests).hasSize(2);
        assertSoftly(softly -> {
            softly.assertThat(requests).extracting(request -> request.path("model").asString())
                    .as("model of each request").containsOnly(MODEL);
            softly.assertThat(requests).extracting(request -> request.path("max_tokens").asInt())
                    .as("max_tokens of each request").containsOnly(MAX_TOKENS);
            for (String sampling : List.of("temperature", "top_p", "top_k")) {
                softly.assertThat(requests).extracting(request -> request.has(sampling))
                        .as(sampling + " present in each request").containsOnly(false);
            }
        });
    }

    /** The production factory over the real model client, tools with mocked flows and the real chat memory. */
    private ChatClient chatClient() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::answerLikeTheApi);
        server.start();
        AnthropicChatModel model = AnthropicChatModel.builder()
                .options(AnthropicChatOptions.builder()
                        .apiKey(DUMMY_KEY)
                        .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                        .model(MODEL)
                        .maxTokens(MAX_TOKENS)
                        .build())
                .build();
        AssistantConfig config = new AssistantConfig();
        return config.assistantChatClient(ChatClient.builder(model), new BookstoreTools(catalogue, inventory),
                config.assistantChatMemory());
    }

    private static String ask(ChatClient chatClient, String conversation, String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversation))
                .call()
                .content();
    }

    /** Records the request, then answers with the next scripted step; a request past the script gets a 500. */
    private void answerLikeTheApi(HttpExchange exchange) throws IOException {
        JsonNode request = JSON.readTree(exchange.getRequestBody().readAllBytes());
        requests.add(request);
        Step step = script.poll();
        int status = step == null ? 500 : 200;
        String body = step == null
                ? "{\"type\":\"error\",\"error\":{\"type\":\"api_error\",\"message\":\"the stub script is empty\"}}"
                : message(step, "disabled".equals(thinkingType(request)), requests.size());
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    /** A Messages API answer: the thinking block (with a signature) first, only while thinking is not switched off. */
    private static String message(Step step, boolean thinkingDisabled, int requestNumber) {
        List<Map<String, Object>> content = new ArrayList<>();
        if (!thinkingDisabled) {
            content.add(block("type", "thinking", "thinking", THINKING_TEXT, "signature", "SIG" + requestNumber));
        }
        content.add(block("type", "text", "text", step.text()));
        if (step.toolName() != null) {
            content.add(block("type", "tool_use", "id", TOOL_USE_ID, "name", step.toolName(), "input",
                    step.toolInput()));
        }
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("id", "msg_" + requestNumber);
        message.put("type", "message");
        message.put("role", "assistant");
        message.put("model", MODEL);
        message.put("content", content);
        message.put("stop_reason", step.toolName() == null ? "end_turn" : "tool_use");
        message.put("stop_sequence", null);
        message.put("usage", Map.of("input_tokens", 1, "output_tokens", 1));
        return JSON.writeValueAsString(message);
    }

    private static Map<String, Object> block(Object... keysAndValues) {
        Map<String, Object> block = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            block.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return block;
    }

    private static Step reply(String text) {
        return new Step(text, null, null);
    }

    private static Step replyWithToolCall(String text, String toolName, Map<String, Object> input) {
        return new Step(text, toolName, input);
    }

    /** The value of {@code thinking.type} of a request, or "absent" when the request does not set thinking. */
    private static String thinkingType(JsonNode request) {
        return request.path("thinking").path("type").asString("absent");
    }

    private static List<String> roles(JsonNode request) {
        List<String> roles = new ArrayList<>();
        request.path("messages").forEach(message -> roles.add(message.path("role").asString()));
        return roles;
    }

    /** For each message, the types of its content blocks in order; a plain string content counts as one text block. */
    private static List<List<String>> blockTypes(JsonNode request) {
        List<List<String>> types = new ArrayList<>();
        request.path("messages").forEach(message -> {
            List<String> perMessage = new ArrayList<>();
            JsonNode content = message.path("content");
            if (content.isString()) {
                perMessage.add("text");
            } else {
                content.forEach(block -> perMessage.add(block.path("type").asString()));
            }
            types.add(perMessage);
        });
        return types;
    }

    private static List<String> allBlockTypes(List<JsonNode> requests) {
        return requests.stream().flatMap(request -> blockTypes(request).stream()).flatMap(List::stream).toList();
    }

    /** For each message, its text blocks joined, or its plain string content. */
    private static List<String> textsOf(JsonNode request) {
        List<String> texts = new ArrayList<>();
        request.path("messages").forEach(message -> {
            JsonNode content = message.path("content");
            if (content.isString()) {
                texts.add(content.asString());
            } else {
                StringBuilder text = new StringBuilder();
                content.forEach(block -> text.append(block.path("text").asString("")));
                texts.add(text.toString());
            }
        });
        return texts;
    }

    private static List<String> allTexts(List<JsonNode> requests) {
        return requests.stream().flatMap(request -> textsOf(request).stream()).toList();
    }

    /** The tool_result blocks of the last message of a request, as JSON text. */
    private static String toolResultOf(JsonNode request) {
        JsonNode messages = request.path("messages");
        JsonNode last = messages.get(messages.size() - 1);
        StringBuilder result = new StringBuilder();
        last.path("content").forEach(block -> {
            if ("tool_result".equals(block.path("type").asString())) {
                result.append(block);
            }
        });
        return result.toString();
    }

    private static BookDTO persuasion() {
        return new BookDTO(PERSUASION_ID, "9780141439686", "Persuasion", List.of("Jane Austen"), "ROMANCE",
                new BigDecimal("8.99"), "EUR", 1817, "AVAILABLE");
    }
}
