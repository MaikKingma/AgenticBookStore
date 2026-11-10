package nl.openvalue.bookstore.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

class AuthorNameTest {

    @Test
    void stripsSurroundingWhitespace() {
        assertThat(new AuthorName("  Jane Austen \t").value()).isEqualTo("Jane Austen");
    }

    @Test
    void acceptsOneCharacter() {
        assertThat(new AuthorName("J").value()).isEqualTo("J");
    }

    @Test
    void acceptsOneHundredAndTwentyCharacters() {
        String longest = "a".repeat(120);
        assertThat(new AuthorName(longest).value()).isEqualTo(longest);
    }

    @Test
    void countsTheLengthAfterStripping() {
        String longest = "a".repeat(120);
        assertThat(new AuthorName("  " + longest + "  ").value()).isEqualTo(longest);
    }

    @Test
    void rejectsOneHundredAndTwentyOneCharacters() {
        assertRejected(() -> new AuthorName("a".repeat(121)));
    }

    @Test
    void rejectsABlankName() {
        assertRejected(() -> new AuthorName(""));
        assertRejected(() -> new AuthorName("   "));
    }

    @Test
    void rejectsNull() {
        assertRejected(() -> new AuthorName(null));
    }

    private static void assertRejected(ThrowingCallable creation) {
        assertThatThrownBy(creation)
                .isInstanceOf(InvalidAuthorNameException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
    }
}
