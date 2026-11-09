package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import nl.openvalue.bookstore.assistant.OfflineChatModel;
import nl.openvalue.bookstore.assistant.ScriptedChatModel;
import nl.openvalue.bookstore.assistant.ScriptedChatModel.ReceivedPrompt;
import nl.openvalue.bookstore.command.CommandResult;
import nl.openvalue.bookstore.domaininteraction.CatalogueFlow;
import nl.openvalue.bookstore.domaininteraction.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * The assistant end to end over real HTTP into the full application on PostgreSQL, with a scripted chat model in place
 * of a real one. The model asks for tools and answers from what they return, so the tools, the chat client, the
 * conversation memory and the flows run as in production. This is the one test context that is online for the
 * assistant: it has its own rate limit, raised so that no test depends on the order of the others, and a spy around
 * the catalogue flow to show that a tool call reaches the real flow. The scripted model is reset before every test and
 * every test uses conversation ids of its own.
 */
@TestPropertySource(properties = {"spring.ai.model.chat=scripted", "bookstore.assistant.max-requests-per-minute=1000"})
@Import(ScriptedChatModel.class)
class AssistantIntegrationTest extends IntegrationTestBase {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
            new ParameterizedTypeReference<>() {
            };

    private static final TypeReference<List<Map<String, Object>>> JSON_LIST = new TypeReference<>() {
    };

    private static final TypeReference<Map<String, Object>> JSON_MAP = new TypeReference<>() {
    };

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final String PRIDE_ISBN = "9780141439518";
    private static final String FRANKENSTEIN_ISBN = "9780141439471";
    private static final String PERSUASION_ISBN = "9780141439686";
    private static final String EMMA_ISBN = "9780141439587";

    /** The answer to a failed model call: the spec 12 text, no key, no internal message. */
    private static final Map<String, Object> UNAVAILABLE = Map.of(
            "title", "Assistant unavailable",
            "status", 503,
            "detail", "The assistant is unavailable right now, try again later",
            "instance", "/assistant/chat",
            "code", "assistant-unavailable",
            "details", List.of());

    /** What a failing live call says; none of it may reach the answer. */
    private static final String FAILURE_MESSAGE = "upstream refused key sk-ant-dummy-not-a-key";

    @Autowired
    private ChatMemory chatMemory;

    @Autowired
    private ScriptedChatModel scripted;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Environment environment;

    @MockitoSpyBean
    private CatalogueFlow catalogueFlow;

    @BeforeEach
    void resetScriptedModel() {
        scripted.reset();
    }

    @Test
    void scriptedContextHasOnlyTheScriptedModel() {
        assertThat(environment.getProperty("spring.ai.model.chat")).isEqualTo("scripted");

        Map<String, ChatModel> chatModels = context.getBeansOfType(ChatModel.class);
        assertThat(chatModels).hasSize(1);
        assertThat(chatModels.values()).allSatisfy(model -> assertThat(model).isInstanceOf(ScriptedChatModel.class));
        assertThat(chatModels.values()).containsExactly(scripted);
        assertThat(context.getBeanNamesForType(OfflineChatModel.class)).isEmpty();
        assertThat(context.getBeanNamesForType(AnthropicChatModel.class)).isEmpty();
    }

    @Test
    void toolCallIsExecutedAgainstOurFlows() {
        UUID pride = addBook(PRIDE_ISBN, "Pride and Prejudice", "Jane Austen", "fiction", "12.50", 1813);
        addBook(FRANKENSTEIN_ISBN, "Frankenstein", "Mary Shelley", "science_fiction", "9.99", 1818);
        receive(pride, 3);
        UUID conversationId = UUID.randomUUID();
        scripted.thenCallTool("searchCatalogue", "{\"query\":\"austen\"}")
                .thenReply(prompt -> "My first match: " + firstTitleOf(toolContentOf(prompt.getInstructions())));

        Map<String, Object> reply = chat(conversationId, "Do you have anything by Austen?");

        assertThat(reply).isEqualTo(Map.of("conversationId", conversationId.toString(),
                "reply", "My first match: Pride and Prejudice"));
        verify(catalogueFlow).search("austen", null, null, null);
        List<ReceivedPrompt> received = scripted.received();
        assertThat(received).hasSize(2);
        assertThat(received.get(0).toolNames()).containsExactlyInAnyOrder("searchCatalogue", "getBookDetails",
                "checkAvailability", "recommendByGenre");
        String toolContent = toolContentOf(received.get(1).messages());
        assertThat(toolContent).contains("Pride and Prejudice").doesNotContain("Frankenstein");
        List<Map<String, Object>> hits = JSON.readValue(toolContent, JSON_LIST);
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0)).isEqualTo(Map.of("id", pride.toString(), "title", "Pride and Prejudice",
                "authors", List.of("Jane Austen"), "genre", "FICTION", "price", "12.50", "inStock", true));
    }

    @Test
    void followUpQuestionSeesOnlyItsOwnConversation() {
        UUID conversationX = UUID.randomUUID();
        UUID conversationY = UUID.randomUUID();
        scripted.thenReply("Frankenstein is a novel by Mary Shelley.")
                .thenReply("I cannot find Dracula in the shop.")
                .thenReply("I have no stock figures for it here.");

        String xReply = chat(conversationX, "Tell me about Frankenstein").get("reply").toString();
        String yReply = chat(conversationY, "Do you have Dracula?").get("reply").toString();
        String xFollowUpReply = chat(conversationX, "Is it in stock?").get("reply").toString();

        assertThat(xReply).isEqualTo("Frankenstein is a novel by Mary Shelley.");
        assertThat(yReply).isEqualTo("I cannot find Dracula in the shop.");
        assertThat(xFollowUpReply).isEqualTo("I have no stock figures for it here.");
        List<ReceivedPrompt> received = scripted.received();
        assertThat(received).hasSize(3);
        assertThat(withoutSystemMessage(received.get(0))).containsExactly("USER: Tell me about Frankenstein");
        assertThat(withoutSystemMessage(received.get(1))).containsExactly("USER: Do you have Dracula?");
        List<String> followUp = withoutSystemMessage(received.get(2));
        assertThat(followUp).containsExactly("USER: Tell me about Frankenstein",
                "ASSISTANT: Frankenstein is a novel by Mary Shelley.", "USER: Is it in stock?");
        assertThat(followUp).noneMatch(message -> message.contains("Dracula"));
        assertThat(followUp).noneMatch(message -> message.contains(yReply));
    }

    @Test
    void toolErrorReachesTheModelAsShortText() {
        UUID unknown = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        scripted.thenCallTool("getBookDetails", "{\"bookId\":\"" + unknown + "\"}")
                .thenReply("I could not find that book.");

        Map<String, Object> reply = chat(conversationId, "Tell me about book " + unknown);

        assertThat(reply).isEqualTo(Map.of("conversationId", conversationId.toString(),
                "reply", "I could not find that book."));
        assertThat(scripted.received()).hasSize(2);
        String toolContent = toolContentOf(scripted.received().get(1).messages());
        assertThat(toolContent).isIn("No book with id " + unknown, "\"No book with id " + unknown + "\"");
        assertThat(toolContent).doesNotContain("\tat ").doesNotContain("\n").doesNotContain("Exception");
    }

    @Test
    void availabilityShowsThePendingRestock() {
        UUID persuasion = addBook(PERSUASION_ISBN, "Persuasion", "Jane Austen", "fiction", "8.50", 1817);
        receive(persuasion, 4);
        // The order takes the stock down to 2, below the reorder level, so the default distributor answer is stored.
        placeOrder(persuasion, 2);
        UUID emma = addBook(EMMA_ISBN, "Emma", "Jane Austen", "fiction", "9.50", 1815);
        receive(emma, 5);
        scripted.thenCallTool("checkAvailability", "{\"bookId\":\"" + persuasion + "\"}")
                .thenReply("Persuasion is on its way.")
                .thenCallTool("checkAvailability", "{\"bookId\":\"" + emma + "\"}")
                .thenReply("Emma is on the shelf.");

        chat(UUID.randomUUID(), "When can I get Persuasion?");
        chat(UUID.randomUUID(), "And Emma?");

        List<ReceivedPrompt> received = scripted.received();
        assertThat(received).hasSize(4);
        Map<String, Object> withRestock = JSON.readValue(toolContentOf(received.get(1).messages()), JSON_MAP);
        assertThat(withRestock.keySet()).containsExactlyInAnyOrder("available", "pendingRestockExpected");
        assertThat(withRestock).containsEntry("available", 2)
                .containsEntry("pendingRestockExpected", defaultExpectedDeliveryDate.toString());
        Map<String, Object> withoutRestock = JSON.readValue(toolContentOf(received.get(3).messages()), JSON_MAP);
        assertThat(withoutRestock.keySet()).containsExactlyInAnyOrder("available", "pendingRestockExpected");
        assertThat(withoutRestock).containsEntry("available", 5).containsEntry("pendingRestockExpected", null);
    }

    @Test
    void failedModelCallLeavesNoEntryForTheQuestion() {
        UUID fresh = UUID.randomUUID();
        UUID oneTurn = UUID.randomUUID();
        UUID fullWindow = UUID.randomUUID();
        UUID bystander = UUID.randomUUID();
        scripted.thenReply("Emma is a novel by Jane Austen.")
                .thenReply("Dracula is not in the shop.");
        chat(oneTurn, "Tell me about Emma");
        chat(bystander, "Do you have Dracula?");
        chatMemory.add(fullWindow.toString(), windowOfTwentyMessages());
        List<String> window = windowOfTwentyLines();
        assertThat(memoryOf(fullWindow)).as("the window is full before the failure").containsExactlyElementsOf(window);
        scripted.thenFail(new IllegalStateException(FAILURE_MESSAGE))
                .thenFail(new UnsupportedOperationException(FAILURE_MESSAGE))
                .thenFail(new IllegalArgumentException(FAILURE_MESSAGE));

        Map<String, Object> freshAnswer = failedChat(fresh, "Is Persuasion in stock?");
        Map<String, Object> oneTurnAnswer = failedChat(oneTurn, "Is it in stock?");
        Map<String, Object> fullWindowAnswer = failedChat(fullWindow, "One more window question");

        assertThat(freshAnswer).isEqualTo(UNAVAILABLE);
        assertThat(oneTurnAnswer).isEqualTo(UNAVAILABLE);
        assertThat(fullWindowAnswer).isEqualTo(UNAVAILABLE);
        assertThat(scripted.received()).as("every failed question reached the model").hasSize(5);
        assertThat(chatMemory.get(fresh.toString())).isEmpty();
        assertThat(memoryOf(oneTurn)).containsExactly("USER: Tell me about Emma",
                "ASSISTANT: Emma is a novel by Jane Austen.");
        assertThat(memoryOf(fullWindow)).hasSize(20).containsExactlyElementsOf(window);
        assertThat(memoryOf(fullWindow).get(0)).isEqualTo("USER: Window question 1");
        assertThat(memoryOf(bystander)).containsExactly("USER: Do you have Dracula?",
                "ASSISTANT: Dracula is not in the shop.");
    }

    @Test
    void followUpAfterAFailedCallDoesNotSeeTheFailedQuestion() {
        UUID conversationId = UUID.randomUUID();
        scripted.thenReply("Frankenstein is a novel by Mary Shelley.")
                .thenFail(new IllegalStateException(FAILURE_MESSAGE))
                .thenReply("It was first published in 1818.");
        chat(conversationId, "Tell me about Frankenstein");
        EntityExchangeResult<Map<String, Object>> failed = send(conversationId, "Who wrote Dracula instead?");
        int receivedBeforeFollowUp = scripted.received().size();

        Map<String, Object> reply = chat(conversationId, "When was it published?");

        List<ReceivedPrompt> received = scripted.received();
        assertThat(received).hasSize(3);
        assertThat(receivedBeforeFollowUp).isEqualTo(2);
        List<String> followUp = withoutSystemMessage(received.get(2));
        assertThat(followUp).containsExactly("USER: Tell me about Frankenstein",
                "ASSISTANT: Frankenstein is a novel by Mary Shelley.", "USER: When was it published?");
        assertThat(received.subList(receivedBeforeFollowUp, received.size()))
                .allSatisfy(prompt -> assertThat(prompt.messages())
                        .noneMatch(message -> message.getText() != null && message.getText().contains("Dracula")));
        assertThat(reply).isEqualTo(Map.of("conversationId", conversationId.toString(),
                "reply", "It was first published in 1818."));
        assertThat(memoryOf(conversationId)).containsExactly("USER: Tell me about Frankenstein",
                "ASSISTANT: Frankenstein is a novel by Mary Shelley.", "USER: When was it published?",
                "ASSISTANT: It was first published in 1818.");
        assertThat(unavailableProblem(failed)).isEqualTo(UNAVAILABLE);
    }

    @Test
    void failureAfterAToolRoundLeavesTheConversationAsItWas() {
        UUID pride = addBook(PRIDE_ISBN, "Pride and Prejudice", "Jane Austen", "fiction", "12.50", 1813);
        receive(pride, 2);
        UUID conversationId = UUID.randomUUID();
        scripted.thenReply("Welcome to the shop.")
                .thenCallTool("searchCatalogue", "{\"query\":\"austen\"}")
                .thenFail(new IllegalStateException(FAILURE_MESSAGE));
        chat(conversationId, "Hello there");
        List<String> before = memoryOf(conversationId);

        Map<String, Object> answer = failedChat(conversationId, "Anything by Austen?");

        assertThat(answer).isEqualTo(UNAVAILABLE);
        verify(catalogueFlow).search("austen", null, null, null);
        List<ReceivedPrompt> received = scripted.received();
        assertThat(received).as("the tool round ran before the failure").hasSize(3);
        assertThat(toolContentOf(received.get(2).messages())).contains("Pride and Prejudice");
        assertThat(before).containsExactly("USER: Hello there", "ASSISTANT: Welcome to the shop.");
        assertThat(memoryOf(conversationId)).isEqualTo(before);
    }

    @Test
    void useCaseRejectionFromTheModelCallKeepsItsAnswerAndLeavesTheConversationAsItWas() {
        UUID fresh = UUID.randomUUID();
        UUID oneTurn = UUID.randomUUID();
        scripted.thenReply("Emma is a novel by Jane Austen.")
                .thenFail(new NotFoundException("shelf-not-found", "Shelf not found", "No shelf 7 in the shop"))
                .thenFail(new NotFoundException("note-not-found", "Note not found", "No note for this book"));
        chat(oneTurn, "Tell me about Emma");
        List<String> before = memoryOf(oneTurn);

        EntityExchangeResult<Map<String, Object>> freshAnswer = send(fresh, "Which shelf holds Persuasion?");
        EntityExchangeResult<Map<String, Object>> oneTurnAnswer = send(oneTurn, "Is there a note for it?");

        assertThat(freshAnswer.getStatus().value()).isEqualTo(404);
        assertThat(freshAnswer.getResponseHeaders().getContentType()).hasToString("application/problem+json");
        assertThat(freshAnswer.getResponseBody()).isEqualTo(Map.of(
                "title", "Shelf not found",
                "status", 404,
                "detail", "No shelf 7 in the shop",
                "instance", "/assistant/chat",
                "code", "shelf-not-found",
                "details", List.of()));
        assertThat(oneTurnAnswer.getStatus().value()).isEqualTo(404);
        assertThat(oneTurnAnswer.getResponseBody()).containsEntry("code", "note-not-found")
                .containsEntry("title", "Note not found");
        assertThat(scripted.received()).as("both rejected questions reached the model").hasSize(3);
        assertThat(chatMemory.get(fresh.toString())).isEmpty();
        assertThat(before).containsExactly("USER: Tell me about Emma", "ASSISTANT: Emma is a novel by Jane Austen.");
        assertThat(memoryOf(oneTurn)).isEqualTo(before);
    }

    /** Posts a question whose model call fails and returns the problem, after its status, type and secrecy checks. */
    private Map<String, Object> failedChat(UUID conversationId, String message) {
        return unavailableProblem(send(conversationId, message));
    }

    /** Posts a question and returns the exchange, whatever its status. */
    private EntityExchangeResult<Map<String, Object>> send(UUID conversationId, String message) {
        return restTestClient.post().uri("/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("conversationId", conversationId.toString(), "message", message))
                .exchange()
                .expectBody(JSON_OBJECT).returnResult();
    }

    /** The problem of a 503 answer, after its type and secrecy checks. */
    private static Map<String, Object> unavailableProblem(EntityExchangeResult<Map<String, Object>> result) {
        assertThat(result.getStatus().value()).isEqualTo(503);
        assertThat(result.getResponseHeaders().getContentType()).hasToString("application/problem+json");
        String body = new String(result.getResponseBodyContent(), StandardCharsets.UTF_8);
        assertThat(body).doesNotContain(FAILURE_MESSAGE).doesNotContain("sk-ant-dummy-not-a-key")
                .doesNotContain("upstream").doesNotContain("Exception");
        return result.getResponseBody();
    }

    /** The messages the conversation memory holds, each as {@code TYPE: text}, oldest first. */
    private List<String> memoryOf(UUID conversationId) {
        return chatMemory.get(conversationId.toString()).stream()
                .map(message -> message.getMessageType() + ": " + message.getText())
                .toList();
    }

    /** Ten answered turns with texts of their own: exactly the 20 messages the memory window keeps. */
    private static List<Message> windowOfTwentyMessages() {
        List<Message> messages = new ArrayList<>();
        for (int turn = 1; turn <= 10; turn++) {
            messages.add(new UserMessage("Window question " + turn));
            messages.add(AssistantMessage.builder().content("Window answer " + turn).build());
        }
        return messages;
    }

    private static List<String> windowOfTwentyLines() {
        List<String> lines = new ArrayList<>();
        for (int turn = 1; turn <= 10; turn++) {
            lines.add("USER: Window question " + turn);
            lines.add("ASSISTANT: Window answer " + turn);
        }
        return lines;
    }

    private Map<String, Object> chat(UUID conversationId, String message) {
        return restTestClient.post().uri("/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("conversationId", conversationId.toString(), "message", message))
                .exchange()
                .expectStatus().isOk()
                .expectBody(JSON_OBJECT).returnResult().getResponseBody();
    }

    /** The messages of a prompt without the system prompt, each as {@code TYPE: text}. */
    private static List<String> withoutSystemMessage(ReceivedPrompt prompt) {
        return prompt.messages().stream()
                .filter(message -> message.getMessageType() != MessageType.SYSTEM)
                .map(message -> message.getMessageType() + ": " + message.getText())
                .toList();
    }

    /** What the one tool the model called returned, as the model received it. */
    private static String toolContentOf(List<Message> messages) {
        List<ToolResponseMessage> toolMessages = messages.stream()
                .filter(ToolResponseMessage.class::isInstance)
                .map(ToolResponseMessage.class::cast)
                .toList();
        assertThat(toolMessages).as("tool messages of the prompt").hasSize(1);
        List<ToolResponseMessage.ToolResponse> responses = toolMessages.get(0).getResponses();
        assertThat(responses).as("tool responses of the prompt").hasSize(1);
        return responses.get(0).responseData();
    }

    private static String firstTitleOf(String searchResult) {
        return JSON.readValue(searchResult, JSON_LIST).get(0).get("title").toString();
    }

    private UUID addBook(String isbn, String title, String author, String genre, String price, int year) {
        CommandResult result = restTestClient.post().uri("/books/commands/add")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("isbn", isbn, "title", title, "authors", List.of(author), "genre", genre,
                        "price", price, "publicationYear", year))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(CommandResult.class).returnResult().getResponseBody();
        assertThat(result).isNotNull();
        return result.id();
    }

    private void receive(UUID bookId, int quantity) {
        restTestClient.post().uri("/inventory/{bookId}/commands/receive", bookId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("quantity", quantity))
                .exchange()
                .expectStatus().isAccepted();
    }

    private void placeOrder(UUID bookId, int quantity) {
        restTestClient.post().uri("/orders/commands/place")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("customerEmail", "reader@example.com",
                        "lines", List.of(Map.of("bookId", bookId, "quantity", quantity))))
                .exchange()
                .expectStatus().isAccepted();
    }
}
