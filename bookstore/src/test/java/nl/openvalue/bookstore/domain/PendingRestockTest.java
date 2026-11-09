package nl.openvalue.bookstore.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PendingRestockTest {

    private static final LocalDate DELIVERY = LocalDate.of(2026, 7, 3);
    private static final LocalDate OTHER_DELIVERY = LocalDate.of(2001, 2, 3);

    @Test
    void keepsEveryValueItWasBuiltWith() {
        PendingRestock restock = new PendingRestock("R-7781", 12, DELIVERY, true);

        assertThat(restock.restockOrderId()).isEqualTo("R-7781");
        assertThat(restock.quantity()).isEqualTo(12);
        assertThat(restock.expectedDeliveryDate()).isEqualTo(DELIVERY);
        assertThat(restock.backordered()).isTrue();
    }

    @Test
    void keepsADifferentSetOfValuesAndAnUnsetBackorderedFlag() {
        PendingRestock restock = new PendingRestock("2f5b7c1e-0a44-4c43-9d2a-6b1f3e8d9a10", 3, OTHER_DELIVERY, false);

        assertThat(restock.restockOrderId()).isEqualTo("2f5b7c1e-0a44-4c43-9d2a-6b1f3e8d9a10");
        assertThat(restock.quantity()).isEqualTo(3);
        assertThat(restock.expectedDeliveryDate()).isEqualTo(OTHER_DELIVERY);
        assertThat(restock.backordered()).isFalse();
    }

    @Test
    void acceptsAnOrderIdOfOneCharacterAndOfSixtyFourCharacters() {
        assertThat(new PendingRestock("R", 1, DELIVERY, false).restockOrderId()).isEqualTo("R");
        assertThat(new PendingRestock("R".repeat(64), 1, DELIVERY, false).restockOrderId()).hasSize(64);
    }

    @Test
    void acceptsAQuantityOfOne() {
        assertThat(new PendingRestock("R-1", 1, DELIVERY, false).quantity()).isEqualTo(1);
    }

    @Test
    void doesNotTrimTheOrderId() {
        assertThat(new PendingRestock(" R-1 ", 1, DELIVERY, false).restockOrderId()).isEqualTo(" R-1 ");
    }

    @Test
    void rejectsAMissingOrderId() {
        assertThatThrownBy(() -> new PendingRestock(null, 5, DELIVERY, false))
                .isExactlyInstanceOf(InvalidPendingRestockException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    void rejectsABlankOrderId(String blank) {
        assertThatThrownBy(() -> new PendingRestock(blank, 5, DELIVERY, false))
                .isExactlyInstanceOf(InvalidPendingRestockException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
    }

    @Test
    void rejectsAnOrderIdMadeOfAUnicodeSpaceOnly() {
        String emSpace = Character.toString(0x2003);

        assertThatThrownBy(() -> new PendingRestock(emSpace, 5, DELIVERY, false))
                .isExactlyInstanceOf(InvalidPendingRestockException.class);
        assertThatThrownBy(() -> new PendingRestock(emSpace + emSpace, 5, DELIVERY, false))
                .isExactlyInstanceOf(InvalidPendingRestockException.class);
    }

    @Test
    void rejectsAnOrderIdOfSixtyFiveCharacters() {
        assertThatThrownBy(() -> new PendingRestock("R".repeat(65), 5, DELIVERY, false))
                .isExactlyInstanceOf(InvalidPendingRestockException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
    }

    @Test
    void measuresTheLengthOfTheOrderIdAsGivenIncludingATrailingSpace() {
        assertThatThrownBy(() -> new PendingRestock("R".repeat(64) + " ", 5, DELIVERY, false))
                .isExactlyInstanceOf(InvalidPendingRestockException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsAQuantityBelowOne(int quantity) {
        assertThatThrownBy(() -> new PendingRestock("R-1", quantity, DELIVERY, false))
                .isExactlyInstanceOf(InvalidPendingRestockException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
    }

    @Test
    void rejectsAMissingExpectedDeliveryDate() {
        assertThatThrownBy(() -> new PendingRestock("R-1", 5, null, false))
                .isExactlyInstanceOf(InvalidPendingRestockException.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
    }
}
