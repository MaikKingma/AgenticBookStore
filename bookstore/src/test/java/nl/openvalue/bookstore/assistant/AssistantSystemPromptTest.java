package nl.openvalue.bookstore.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nl.openvalue.bookstore.domain.Genre;
import nl.openvalue.bookstore.domain.UnknownGenreException;
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
    void promptNamesWhatTheToolsReturnAndNothingElse() throws IOException {
        String prompt = new String(promptBytes(), StandardCharsets.UTF_8);

        assertThat(prompt).contains("The tools return only these facts about a book");
        assertThat(prompt).contains("title, authors, genre, price in EUR, publication year, "
                + "the number of copies in stock and whether a restock is on its way");
        assertThat(prompt).contains("The catalogue has nothing else");
        assertThat(prompt).contains("no descriptions, summaries or reviews");
        assertThat(prompt).contains("never offer them");
    }

    @Test
    void promptTellsTheAssistantToLookABookUpAgainOnAFollowUp() throws IOException {
        String prompt = new String(promptBytes(), StandardCharsets.UTF_8);

        assertThat(prompt).contains("follow-up about a book named earlier");
        assertThat(prompt).contains("look the book up again by its title");
        assertThat(prompt).contains("search on the title alone");
        assertThat(prompt).contains("before you check its stock or a restock");
    }

    @Test
    void promptListsTheGenresInReadableFormInDeclarationOrder() throws IOException {
        String prompt = new String(promptBytes(), StandardCharsets.UTF_8);
        List<String> readable = Arrays.stream(Genre.values())
                .map(genre -> genre.name().toLowerCase(Locale.ROOT).replace('_', ' '))
                .toList();

        Matcher listed = Pattern.compile("We sell these genres: ([a-z, ]+)\\.").matcher(prompt);
        assertThat(listed.find()).as("a sentence 'We sell these genres: ...' in the prompt").isTrue();
        assertThat(Arrays.asList(listed.group(1).split(", ")))
                .as("the genres of the prompt, in order, against Genre.values()")
                .containsExactlyElementsOf(readable);
        assertThat(readable).hasSize(10);
        assertThat(prompt).contains("when the customer asks which genres we have");
    }

    /**
     * The genres are listed readably for the customer, which invites the model to pass "science fiction" to a tool.
     * Genre.parse takes capitals with underscores, and also a hyphen or any case, but never a space, so the prompt
     * names the form for tool arguments, and the example it gives is a form that is accepted.
     */
    @Test
    void promptSaysHowToPassAGenreToATool() throws IOException {
        String prompt = new String(promptBytes(), StandardCharsets.UTF_8);

        Matcher sentence = Pattern.compile("When you pass a genre to a tool, write it in capitals with underscores "
                + "between words, for example ([^.]+)\\.").matcher(prompt);
        assertThat(sentence.find()).as("a sentence on how to pass a genre to a tool in the prompt").isTrue();
        String example = sentence.group(1);
        assertThat(example).as("the example is a constant: capitals, underscores, no space")
                .matches("[A-Z]+(_[A-Z]+)*")
                .isEqualTo("SCIENCE_FICTION");
        assertThat(Genre.parse(example)).isEqualTo(Genre.SCIENCE_FICTION);
        assertThat(Genre.valueOf(example)).as("the example is the name of a genre").isEqualTo(Genre.SCIENCE_FICTION);
    }

    /** Why the sentence is needed: the readable form of the prompt is rejected as a tool argument, the named form is not. */
    @Test
    void theNamedGenreFormIsAcceptedAndTheReadableFormWithASpaceIsRejected() {
        for (Genre genre : Genre.values()) {
            String readable = genre.name().toLowerCase(Locale.ROOT).replace('_', ' ');
            assertThat(Genre.parse(genre.name())).as("named form %s", genre.name()).isEqualTo(genre);
            if (readable.contains(" ")) {
                assertThatThrownBy(() -> Genre.parse(readable)).as("readable form '%s'", readable)
                        .isInstanceOf(UnknownGenreException.class);
            } else {
                assertThat(Genre.parse(readable)).as("one word, readable form '%s'", readable).isEqualTo(genre);
            }
        }
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
