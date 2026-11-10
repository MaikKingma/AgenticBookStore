package nl.openvalue.bookstore.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

class IsbnTest {

    @Test
    void acceptsAValidIsbn13() {
        assertThat(new Isbn("9780141439518").value()).isEqualTo("9780141439518");
    }

    @Test
    void acceptsAValidIsbnWhoseCheckDigitIsZero() {
        assertThat(new Isbn("9780141439600").value()).isEqualTo("9780141439600");
    }

    @Test
    void acceptsThe979Prefix() {
        assertThat(new Isbn("9791090636071").value()).isEqualTo("9791090636071");
    }

    @Test
    void normalisesHyphensAndSpaces() {
        assertThat(new Isbn("978-0-14-143951-8").value()).isEqualTo("9780141439518");
        assertThat(new Isbn("978 0 14 143951 8").value()).isEqualTo("9780141439518");
        assertThat(new Isbn(" 978-0 14-143951 8 ").value()).isEqualTo("9780141439518");
    }

    @Test
    void isEqualAcrossSpellings() {
        assertThat(new Isbn("978-0-14-143951-8")).isEqualTo(new Isbn("9780141439518"));
        assertThat(new Isbn("978 0 14 143951 8")).isEqualTo(new Isbn("978-0-14-143951-8"));
    }

    @Test
    void rejectsABadChecksum() {
        assertRejected(() -> new Isbn("9780141439519"));
    }

    @Test
    void rejectsAWrongPrefixEvenWithAValidChecksum() {
        assertRejected(() -> new Isbn("9771234567898"));
    }

    @Test
    void rejectsTwelveDigits() {
        assertRejected(() -> new Isbn("978014143951"));
    }

    @Test
    void rejectsFourteenDigits() {
        assertRejected(() -> new Isbn("97801414395188"));
    }

    @Test
    void rejectsALetter() {
        assertRejected(() -> new Isbn("978014143951X"));
    }

    @Test
    void rejectsANonAsciiUnicodeDigit() {
        // U+0668 is the Arabic-Indic digit eight; only the ASCII digits 0 to 9 are allowed.
        assertRejected(() -> new Isbn("978014143951٨"));
    }

    @Test
    void rejectsANonAsciiDigitEvenWhenTheNaiveChecksumWouldMatch() {
        // U+0661 is the Arabic-Indic digit one. It replaces the 5 of 9780141439518; its char value minus '0' differs
        // from 5 by a multiple of ten, so a checksum over raw char values would still match.
        assertRejected(() -> new Isbn("9780141439١18"));
    }

    @Test
    void rejectsNull() {
        assertRejected(() -> new Isbn(null));
    }

    @Test
    void rejectsAnEmptyValue() {
        assertRejected(() -> new Isbn(" - "));
    }

    private static void assertRejected(ThrowingCallable creation) {
        assertThatThrownBy(creation)
                .isInstanceOf(InvalidIsbnException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
    }
}
