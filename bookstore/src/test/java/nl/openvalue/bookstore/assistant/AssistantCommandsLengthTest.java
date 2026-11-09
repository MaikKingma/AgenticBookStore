package nl.openvalue.bookstore.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.ProblemDetail;

/**
 * The length check of the chat endpoint with a maximum of 5 characters, without a Spring context: a real chat client
 * over a scripted model, so a message that passes the check reaches the model as it was sent.
 */
class AssistantCommandsLengthTest {

    private static final UUID CONVERSATION = UUID.fromString("2f4e6a8c-1b3d-4f5a-9c7e-0d2b4f6a8c1e");

    private final ScriptedChatModel model = new ScriptedChatModel();
    private final AssistantCommands commands = new AssistantCommands(ChatClient.create(model), model,
            new AssistantRateLimiter(new MutableClock(Instant.parse("2002-03-04T05:06:07.890Z")),
                    new AssistantProperties(20, 5)),
            new AssistantProperties(20, 5));

    @Test
    void fiveCharactersPass() {
        model.thenReply("Yes.");

        AssistantReplyView reply = commands.chat(new ChatPayload(CONVERSATION, "abcde"));

        assertThat(reply).isEqualTo(new AssistantReplyView(CONVERSATION, "Yes."));
        assertThat(receivedTexts()).containsExactly("abcde");
    }

    @Test
    void sixCharactersAreTooLong() {
        assertTooLong("abcdef");
    }

    @Test
    void trailingSpaceCounts() {
        assertTooLong("abcde ");
    }

    @Test
    void emojiCountsTwoCharacters() {
        assertTooLong("abcd" + Character.toString(0x1F4DA));
    }

    @Test
    void accentedLetterCountsOneCharacter() {
        String message = "abcd" + Character.toString(0xE9);
        model.thenReply("Oui.");

        AssistantReplyView reply = commands.chat(new ChatPayload(CONVERSATION, message));

        assertThat(reply.reply()).isEqualTo("Oui.");
        assertThat(receivedTexts()).containsExactly(message);
    }

    /** The message is refused before the model, and the advice answers it with the maximum of 5, not the default. */
    private void assertTooLong(String message) {
        AssistantCommands.MessageTooLongException tooLong = catchThrowableOfType(
                AssistantCommands.MessageTooLongException.class,
                () -> commands.chat(new ChatPayload(CONVERSATION, message)));
        assertThat(tooLong).as("the message is too long").isNotNull();
        assertThat(tooLong).hasMessage("must be at most 5 characters");
        assertThat(model.received()).isEmpty();

        ProblemDetail answer = new AssistantExceptionHandler().handleMessageTooLong(tooLong);

        assertThat(answer.getStatus()).isEqualTo(400);
        assertThat(answer.getTitle()).isEqualTo("Validation failed");
        assertThat(answer.getDetail()).isEqualTo("The request body is not valid");
        assertThat(answer.getProperties()).isEqualTo(Map.of(
                "code", "validation-failed",
                "details", List.of(Map.of("field", "message", "message", "must be at most 5 characters"))));
    }

    private List<String> receivedTexts() {
        assertThat(model.received()).hasSize(1);
        return model.received().get(0).messages().stream().map(Message::getText).toList();
    }
}
