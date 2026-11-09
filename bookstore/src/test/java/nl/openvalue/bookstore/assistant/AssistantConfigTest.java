package nl.openvalue.bookstore.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import nl.openvalue.bookstore.domaininteraction.BookDTO;
import nl.openvalue.bookstore.domaininteraction.CatalogueFlow;
import nl.openvalue.bookstore.domaininteraction.InventoryFlow;
import nl.openvalue.bookstore.domaininteraction.NotFoundException;
import nl.openvalue.bookstore.domaininteraction.StockItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * The chat client {@link AssistantConfig} builds, without a Spring context: the real bean methods over a scripted
 * model and the real tools with mocked flows.
 */
@ExtendWith(MockitoExtension.class)
class AssistantConfigTest {

    private static final UUID PERSUASION_ID = UUID.fromString("5e1f0c2a-7b3d-4e8f-9a10-2b3c4d5e6f70");
    private static final UUID EMMA_ID = UUID.fromString("0a9b8c7d-6e5f-4a3b-8c2d-1e0f9a8b7c6d");

    @Mock
    private CatalogueFlow catalogue;

    @Mock
    private InventoryFlow inventory;

    private final AssistantConfig config = new AssistantConfig();
    private final ScriptedChatModel model = new ScriptedChatModel();
    private ChatMemory chatMemory;
    private ChatClient chatClient;

    @BeforeEach
    void buildTheClient() {
        chatMemory = config.assistantChatMemory();
        chatClient = config.assistantChatClient(ChatClient.builder(model), new BookstoreTools(catalogue, inventory),
                chatMemory);
    }

    @Test
    void systemMessageIsThePromptVerbatim() throws IOException {
        model.thenReply("Hello.");

        ask(UUID.randomUUID(), "Hi");

        Message first = model.received().get(0).messages().get(0);
        assertThat(first.getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(first.getText()).isEqualTo(promptResource());
    }

    @Test
    void offersExactlyTheFourTools() {
        model.thenReply("Hello.");

        ask(UUID.randomUUID(), "Hi");

        assertThat(model.received().get(0).toolNames())
                .containsExactlyInAnyOrder("searchCatalogue", "getBookDetails", "checkAvailability", "recommendByGenre");
    }

    @ParameterizedTest
    @CsvSource({
        "austen, Persuasion, " + "5e1f0c2a-7b3d-4e8f-9a10-2b3c4d5e6f70",
        "'  emma ', ' Emma ', " + "0a9b8c7d-6e5f-4a3b-8c2d-1e0f9a8b7c6d"
    })
    void toolCallRunsAgainstTheFlows(String query, String title, UUID bookId) {
        when(catalogue.search(query, null, null, null)).thenReturn(List.of(book(bookId, title)));
        when(inventory.findByBookId(bookId)).thenReturn(new StockItemDTO(bookId, 4, 1, 1, 5, false));
        model.thenCallTool("searchCatalogue", "{\"query\":\"" + query + "\"}")
                .thenReply(prompt -> "The tool said " + toolResult(prompt));

        String reply = ask(UUID.randomUUID(), "Do you have " + query + "?");

        verify(catalogue).search(query, null, null, null);
        verifyNoMoreInteractions(catalogue);
        assertThat(model.received()).hasSize(2);
        List<Message> second = model.received().get(1).messages();
        assertThat(second).extracting(Message::getMessageType)
                .containsExactly(MessageType.SYSTEM, MessageType.USER, MessageType.ASSISTANT, MessageType.TOOL);
        assertThat(second.get(1).getText()).isEqualTo("Do you have " + query + "?");
        assertThat(((AssistantMessage) second.get(2)).getToolCalls())
                .extracting(AssistantMessage.ToolCall::name)
                .containsExactly("searchCatalogue");
        ToolResponseMessage.ToolResponse response = ((ToolResponseMessage) second.get(3)).getResponses().get(0);
        assertThat(response.name()).isEqualTo("searchCatalogue");
        assertThat(response.responseData()).contains("\"title\":\"" + title + "\"", bookId.toString());
        assertThat(reply).isEqualTo("The tool said " + response.responseData());
    }

    @Test
    void toolFailureReachesTheModelAsItsText() {
        when(catalogue.findById(PERSUASION_ID)).thenThrow(new NotFoundException("book-not-found", "Book not found",
                "No book with id " + PERSUASION_ID, List.of(Map.of("shelf", "B7", "isbn", "9780141439686"))));
        when(catalogue.findById(EMMA_ID)).thenThrow(new NotFoundException("book-gone", "Book gone",
                "Emma left the catalogue", List.of()));
        model.thenCallTool("getBookDetails", "{\"bookId\":\"" + PERSUASION_ID + "\"}")
                .thenReply(prompt -> toolResult(prompt))
                .thenCallTool("getBookDetails", "{\"bookId\":\"" + EMMA_ID + "\"}")
                .thenReply(prompt -> toolResult(prompt));

        String first = ask(UUID.randomUUID(), "Tell me about Persuasion");
        String second = ask(UUID.randomUUID(), "Tell me about Emma");

        assertThat(first).isEqualTo("No book with id " + PERSUASION_ID);
        assertThat(second).isEqualTo("Emma left the catalogue");
        verifyNoInteractions(inventory);
    }

    @Test
    void conversationsStayApart() {
        UUID x = UUID.randomUUID();
        UUID y = UUID.randomUUID();
        model.thenReply("x answer one").thenReply("y answer one").thenReply("x answer two").thenReply("y answer two");

        ask(x, "x question one");
        ask(y, "y question one");
        ask(x, "x question two");
        ask(y, "y question two");

        assertThat(history(2)).containsExactly("USER:x question one", "ASSISTANT:x answer one", "USER:x question two");
        assertThat(history(3)).containsExactly("USER:y question one", "ASSISTANT:y answer one", "USER:y question two");
    }

    @Test
    void memoryKeepsOnlyQuestionAndFinalAnswer() {
        when(catalogue.search("austen", null, null, null)).thenReturn(List.of(book(PERSUASION_ID, "Persuasion")));
        when(inventory.findByBookId(PERSUASION_ID)).thenReturn(new StockItemDTO(PERSUASION_ID, 4, 1, 1, 5, false));
        UUID x = UUID.randomUUID();
        model.thenCallTool("searchCatalogue", "{\"query\":\"austen\"}").thenReply("Persuasion is in stock.");

        ask(x, "Anything by Austen?");

        assertThat(chatMemory.get(x.toString()))
                .extracting(AssistantConfigTest::describe)
                .containsExactly("USER:Anything by Austen?", "ASSISTANT:Persuasion is in stock.");
    }

    @Test
    void memoryWindowKeepsTwentyMessages() {
        UUID x = UUID.randomUUID();
        IntStream.rangeClosed(1, 12).forEach(n -> model.thenReply("A" + n));

        IntStream.rangeClosed(1, 12).forEach(n -> ask(x, "Q" + n));

        assertThat(history(10)).containsExactlyElementsOf(exchanges(1, 10, "Q11"));
        assertThat(history(11)).containsExactlyElementsOf(exchanges(2, 11, "Q12"));
        assertThat(chatMemory.get(x.toString())).extracting(AssistantConfigTest::describe)
                .containsExactlyElementsOf(exchanges(3, 12, null));
    }

    /**
     * With complete turns only, the window cuts forward to the next question, so 20 and 21 look the same. A failed
     * first call leaves its question unanswered in memory and shifts the count by one, which tells them apart.
     */
    @Test
    void memoryWindowKeepsTwentyMessagesAfterAFailedCall() {
        UUID x = UUID.randomUUID();
        model.thenReply(prompt -> {
            throw new IllegalStateException("model unavailable");
        });
        IntStream.rangeClosed(2, 12).forEach(n -> model.thenReply("A" + n));

        assertThatThrownBy(() -> ask(x, "Q1")).isInstanceOf(IllegalStateException.class)
                .hasMessage("model unavailable");
        IntStream.rangeClosed(2, 12).forEach(n -> ask(x, "Q" + n));

        assertThat(model.received()).hasSize(12);
        assertThat(history(11)).containsExactlyElementsOf(exchanges(2, 11, "Q12"));
    }

    @Test
    void missingConversationIdIsRejected() {
        model.thenReply("never given");

        assertThatThrownBy(() -> chatClient.prompt().user("Hi").call().content())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("conversationId cannot be null");
        assertThat(model.received()).isEmpty();
    }

    private String ask(UUID conversationId, String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId.toString()))
                .call()
                .content();
    }

    /** The messages of one received prompt after its system message, as "TYPE:text". */
    private List<String> history(int call) {
        List<Message> messages = model.received().get(call).messages();
        assertThat(messages.get(0).getMessageType()).isEqualTo(MessageType.SYSTEM);
        return messages.subList(1, messages.size()).stream().map(AssistantConfigTest::describe).toList();
    }

    /** Q and A pairs from {@code first} to {@code last}, then the new question if there is one. */
    private static List<String> exchanges(int first, int last, String newQuestion) {
        List<String> expected = new ArrayList<>();
        for (int n = first; n <= last; n++) {
            expected.add("USER:Q" + n);
            expected.add("ASSISTANT:A" + n);
        }
        if (newQuestion != null) {
            expected.add("USER:" + newQuestion);
        }
        return expected;
    }

    private static String describe(Message message) {
        return message.getMessageType() + ":" + message.getText();
    }

    private static String toolResult(Prompt prompt) {
        List<Message> messages = prompt.getInstructions();
        ToolResponseMessage tool = (ToolResponseMessage) messages.get(messages.size() - 1);
        return tool.getResponses().get(0).responseData();
    }

    private static String promptResource() throws IOException {
        try (InputStream in = AssistantConfigTest.class.getResourceAsStream("/prompts/assistant-system.st")) {
            assertThat(in).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static BookDTO book(UUID id, String title) {
        return new BookDTO(id, "9780141439686", title, List.of("Jane Austen"), "ROMANCE", new BigDecimal("8.99"),
                "EUR", 1817, "AVAILABLE");
    }
}
