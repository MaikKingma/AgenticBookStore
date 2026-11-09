package nl.openvalue.bookstore.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.BookDTO;
import nl.openvalue.bookstore.domaininteraction.CatalogueFlow;
import nl.openvalue.bookstore.domaininteraction.InventoryFlow;
import nl.openvalue.bookstore.domaininteraction.StockItemDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

/**
 * The scripted test double, and the trap it exists to avoid: a chat model whose options are not tool-calling options
 * makes Spring AI drop every tool without a word.
 */
@ExtendWith(MockitoExtension.class)
class ScriptedChatModelTest {

    private static final UUID PERSUASION_ID = UUID.fromString("5e1f0c2a-7b3d-4e8f-9a10-2b3c4d5e6f70");

    @Mock
    private CatalogueFlow catalogue;

    @Mock
    private InventoryFlow inventory;

    @Test
    void toolCallingOptionsLetTheToolRun() {
        when(catalogue.search("austen", null, null, null)).thenReturn(List.of(persuasion()));
        when(inventory.findByBookId(PERSUASION_ID)).thenReturn(new StockItemDTO(PERSUASION_ID, 4, 1, 1, 5, false));
        ScriptedChatModel model = new ScriptedChatModel()
                .thenCallTool("searchCatalogue", "{\"query\":\"austen\"}")
                .thenReply("Persuasion is in stock.");

        String reply = clientWithTools(model).prompt().user("Anything by Austen?").call().content();

        assertThat(reply).isEqualTo("Persuasion is in stock.");
        verify(catalogue).search("austen", null, null, null);
        assertThat(model.received()).hasSize(2);
        assertThat(model.received().get(0).toolNames())
                .containsExactlyInAnyOrder("searchCatalogue", "getBookDetails", "checkAvailability", "recommendByGenre");
    }

    @Test
    void defaultOptionsDropTheToolsSilently() {
        ScriptedChatModel model = new ScriptedChatModel() {
            @Override
            public ChatOptions getOptions() {
                return ChatOptions.builder().build();
            }
        };
        model.thenCallTool("searchCatalogue", "{\"query\":\"austen\"}").thenReply("Persuasion is in stock.");

        String reply = clientWithTools(model).prompt().user("Anything by Austen?").call().content();

        assertThat(reply).isEmpty();
        verifyNoInteractions(catalogue, inventory);
        assertThat(model.received()).hasSize(1);
        assertThat(model.received().get(0).toolNames()).isEmpty();
    }

    @Test
    void clientWithoutToolsRecordsNoToolNames() {
        ScriptedChatModel model = new ScriptedChatModel().thenReply("Hello, reader.");

        String reply = ChatClient.builder(model).build().prompt().user("Hi").call().content();

        assertThat(reply).isEqualTo("Hello, reader.");
        assertThat(model.received()).hasSize(1);
        assertThat(model.received().get(0).toolNames()).isEmpty();
    }

    @Test
    void optionsAreToolCallingOptions() {
        assertThat(new ScriptedChatModel().getOptions()).isInstanceOf(ToolCallingChatOptions.class);
    }

    @Test
    void stepsAnswerThePromptTheyReceive() {
        ScriptedChatModel model = new ScriptedChatModel()
                .thenReply(prompt -> "You asked: " + prompt.getUserMessage().getText());

        String first = model.call(new Prompt(new UserMessage("Is Emma in stock?")))
                .getResult().getOutput().getText();

        assertThat(first).isEqualTo("You asked: Is Emma in stock?");
        model.thenReply(prompt -> "You asked: " + prompt.getUserMessage().getText());
        String second = model.call(new Prompt(new UserMessage("Any thrillers?"))).getResult().getOutput().getText();
        assertThat(second).isEqualTo("You asked: Any thrillers?");
    }

    @Test
    void recordsAnImmutableCopyOfEveryPrompt() {
        ScriptedChatModel model = new ScriptedChatModel().thenReply("first").thenReply("second");
        List<Message> messages = new ArrayList<>(List.of(new UserMessage("one")));
        model.call(new Prompt(messages));
        messages.add(new UserMessage("added later"));
        model.call(new Prompt(new UserMessage("two")));

        List<ScriptedChatModel.ReceivedPrompt> received = model.received();

        assertThat(received).hasSize(2);
        assertThat(received.get(0).messages()).extracting(Message::getText).containsExactly("one");
        assertThat(received.get(1).messages()).extracting(Message::getText).containsExactly("two");
        assertThatThrownBy(() -> received.get(0).messages().add(new UserMessage("x")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> received.add(received.get(0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void callWithNoStepLeftFailsNamingTheCallNumber() {
        ScriptedChatModel empty = new ScriptedChatModel();
        assertThatThrownBy(() -> empty.call(new Prompt(new UserMessage("hello"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No scripted step left for call 1");
        assertThat(empty.received()).hasSize(1);
        assertThat(empty.received().get(0).messages()).extracting(Message::getText).containsExactly("hello");

        ScriptedChatModel oneStep = new ScriptedChatModel().thenReply("only answer");
        oneStep.call(new Prompt(new UserMessage("first")));
        assertThatThrownBy(() -> oneStep.call(new Prompt(new UserMessage("second"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No scripted step left for call 2");
    }

    @Test
    void resetForgetsPromptsAndSteps() {
        ScriptedChatModel model = new ScriptedChatModel().thenReply("first").thenReply("never given");
        model.call(new Prompt(new UserMessage("before reset")));

        model.reset();

        assertThat(model.received()).isEmpty();
        assertThatThrownBy(() -> model.call(new Prompt(new UserMessage("after reset"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No scripted step left for call 1");
    }

    private ChatClient clientWithTools(ScriptedChatModel model) {
        return ChatClient.builder(model).defaultTools(new BookstoreTools(catalogue, inventory)).build();
    }

    private static BookDTO persuasion() {
        return new BookDTO(PERSUASION_ID, "9780141439686", "Persuasion", List.of("Jane Austen"), "ROMANCE",
                new BigDecimal("8.99"), "EUR", 1817, "AVAILABLE");
    }
}
