package nl.openvalue.bookstore.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

class TitleTest {

    @Test
    void stripsSurroundingWhitespace() {
        assertThat(new Title("  Pride and Prejudice \t").value()).isEqualTo("Pride and Prejudice");
    }

    @Test
    void acceptsOneCharacter() {
        assertThat(new Title("E").value()).isEqualTo("E");
    }

    @Test
    void acceptsTwoHundredCharacters() {
        String longest = "a".repeat(200);
        assertThat(new Title(longest).value()).isEqualTo(longest);
    }

    @Test
    void countsTheLengthAfterStripping() {
        String longest = "a".repeat(200);
        assertThat(new Title("  " + longest + "  ").value()).isEqualTo(longest);
    }

    @Test
    void rejectsTwoHundredAndOneCharacters() {
        assertRejected(() -> new Title("a".repeat(201)));
    }

    @Test
    void rejectsABlankTitle() {
        assertRejected(() -> new Title(""));
        assertRejected(() -> new Title("   "));
    }

    @Test
    void rejectsNull() {
        assertRejected(() -> new Title(null));
    }

    private static void assertRejected(ThrowingCallable creation) {
        assertThatThrownBy(creation)
                .isInstanceOf(InvalidTitleException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
    }
}
