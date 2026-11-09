package nl.openvalue.bookstore.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.List;
import nl.openvalue.bookstore.domaininteraction.UnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

class OfflineChatModelTest {

    private final OfflineChatModel model = new OfflineChatModel();

    @Test
    void callAnswersOffline() {
        Prompt question = new Prompt(new UserMessage("Do you have anything by Austen?"));
        Prompt conversation = new Prompt(List.of(new SystemMessage("You help customers of a bookstore."),
                new UserMessage("Tell me about Frankenstein"), new AssistantMessage("It is a novel."),
                new UserMessage("Is it in stock?")));

        UnavailableException first = catchThrowableOfType(UnavailableException.class, () -> model.call(question));
        UnavailableException second = catchThrowableOfType(UnavailableException.class,
                () -> model.call(conversation));

        assertOfflineProblem(first);
        assertOfflineProblem(second);
    }

    @Test
    void offlineProblemHasTheSpecText() {
        UnavailableException problem = OfflineChatModel.offline();

        assertOfflineProblem(problem);
        // Each call gives a fresh exception, so no stack trace is shared between requests.
        assertThat(OfflineChatModel.offline()).isNotSameAs(problem);
    }

    private static void assertOfflineProblem(UnavailableException problem) {
        assertThat(problem).isNotNull();
        assertThat(problem.code()).isEqualTo("assistant-offline");
        assertThat(problem.title()).isEqualTo("Assistant offline");
        assertThat(problem.getMessage()).isEqualTo("Assistant offline: set BOOKSTORE_AI_API_KEY");
        assertThat(problem.details()).isEmpty();
    }
}
