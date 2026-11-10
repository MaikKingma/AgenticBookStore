package nl.openvalue.bookstore.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class EmailAddressTest {

    /** Twelve characters: "@example.com". */
    private static final String DOMAIN = "@example.com";

    @Test
    void acceptsASimpleAddress() {
        assertThat(new EmailAddress("reader@example.com").value()).isEqualTo("reader@example.com");
    }

    @Test
    void trimsAndLowerCasesTheAddress() {
        assertThat(new EmailAddress("  Reader@Example.COM\t").value()).isEqualTo("reader@example.com");
    }

    @Test
    void lowerCasesWithTheRootLocale() {
        Locale originalDefault = Locale.getDefault();
        Locale originalDisplay = Locale.getDefault(Locale.Category.DISPLAY);
        Locale originalFormat = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));

            assertThat(new EmailAddress("IAN@EXAMPLE.COM").value()).isEqualTo("ian@example.com");
        } finally {
            Locale.setDefault(originalDefault);
            Locale.setDefault(Locale.Category.DISPLAY, originalDisplay);
            Locale.setDefault(Locale.Category.FORMAT, originalFormat);
        }
    }

    @Test
    void rejectsNullEmptyAndBlank() {
        assertRejected(null);
        assertRejected("");
        assertRejected("   ");
        assertRejected(" \t ");
    }

    @Test
    void rejectsAddressesThatDoNotMatchThePattern() {
        assertRejected("reader");
        assertRejected("reader@example");
        assertRejected("reader@");
        assertRejected("reader\t@example.com");
        assertRejected("rea\tder@example.com");
        // whitespace in each of the three character classes
        assertRejected("a b@c.de");
        assertRejected("a@b c.de");
        assertRejected("a@b.c d");
        // an at sign in the local part or domain, and after the dot
        assertRejected("a@b@c.de");
        assertRejected("a@b.c@d");
        // at least one character in each of the three positions
        assertRejected("@example.com");
        assertRejected("reader@.com");
        assertRejected("reader@example.");
    }

    @Test
    void accepts254AndRejects255Characters() {
        String address254 = "a".repeat(242) + DOMAIN;
        String address255 = "a".repeat(243) + DOMAIN;

        assertThat(address254).hasSize(254);
        assertThat(address255).hasSize(255);
        assertThat(new EmailAddress(address254).value()).isEqualTo(address254);
        assertRejected(address255);
    }

    @Test
    void checksTheLengthAfterTrimming() {
        String address254 = "b".repeat(242) + DOMAIN;

        assertThat(new EmailAddress("  " + address254 + "  ").value()).isEqualTo(address254);
    }

    @Test
    void checksTheLengthAfterLowerCasing() {
        String dottedCapitalI = Character.toString(0x130);
        String given254 = dottedCapitalI + "c".repeat(241) + DOMAIN;
        String given253 = dottedCapitalI + "c".repeat(240) + DOMAIN;

        assertThat(given254).hasSize(254);
        assertThat(given254.toLowerCase(Locale.ROOT)).hasSize(255);
        assertRejected(given254);

        assertThat(given253).hasSize(253);
        assertThat(new EmailAddress(given253).value()).hasSize(254).isEqualTo(given253.toLowerCase(Locale.ROOT));
    }

    @Test
    void keepsANonAsciiSpaceBecauseTrimOnlyRemovesAsciiControlAndSpace() {
        String address = Character.toString(0x2003) + "a@b.co";

        assertThat(new EmailAddress(address).value()).isEqualTo(address);
    }

    @Test
    void equalAddressesAfterNormalisationAreEqual() {
        EmailAddress given = new EmailAddress(" Reader@Example.com ");
        EmailAddress normalised = new EmailAddress("reader@example.com");

        assertThat(given).isEqualTo(normalised);
        assertThat(given.hashCode()).isEqualTo(normalised.hashCode());
        assertThat(given).isNotEqualTo(new EmailAddress("other@example.com"));
    }

    private static void assertRejected(String value) {
        assertThatThrownBy(() -> new EmailAddress(value))
                .isInstanceOf(InvalidEmailAddressException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
    }
}
