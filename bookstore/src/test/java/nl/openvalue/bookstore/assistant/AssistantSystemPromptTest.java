package nl.openvalue.bookstore.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * The system prompt is product text. Every rule of the assistant is stated in it, and it holds nothing that Spring AI's
 * template renderer would read as a placeholder or tag.
 */
class AssistantSystemPromptTest {

    private static final String RESOURCE = "/prompts/assistant-system.st";

    @Test
    void promptStatesEveryRule() throws IOException {
        String prompt = new String(promptBytes(), StandardCharsets.UTF_8);

        assertThat(prompt).contains("the assistant of an online bookstore");
        assertThat(prompt).contains("Only talk about books in our catalogue");
        assertThat(prompt).contains("Always use the tools before naming a book");
        assertThat(prompt).contains("Never invent titles, authors or prices");
        assertThat(prompt).contains("All prices are in EUR");
        assertThat(prompt).contains("You cannot place or change orders, so point the customer to the shop");
        assertThat(prompt).contains("Answer in the language of the customer");
        assertThat(prompt).contains("at most 120 words");
    }

    @Test
    void promptIsSafeForTheTemplateRenderer() throws IOException {
        byte[] bytes = promptBytes();
        String prompt = new String(bytes, StandardCharsets.UTF_8);

        assertThat(prompt).isNotBlank();
        assertThat(prompt).doesNotContain("{", "}", "<", ">", "$");
        assertThat(prompt).doesNotContain(Character.toString(0x2014));
        for (byte b : bytes) {
            assertThat(b & 0xFF).as("every byte of the prompt is ASCII").isLessThan(0x80);
        }
    }

    private static byte[] promptBytes() throws IOException {
        try (InputStream in = AssistantSystemPromptTest.class.getResourceAsStream(RESOURCE)) {
            assertThat(in).as("resource %s on the classpath", RESOURCE).isNotNull();
            return in.readAllBytes();
        }
    }
}
