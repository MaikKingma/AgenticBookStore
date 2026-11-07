package nl.openvalue.bookstore.domaininteraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import nl.openvalue.bookstore.domain.BookId;
import nl.openvalue.bookstore.domain.OnHandBelowReservedException;
import nl.openvalue.bookstore.domain.Quantity;
import nl.openvalue.bookstore.domain.StockItem;
import nl.openvalue.bookstore.domain.StockRunningLowEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StockItemDomainMapperTest {

    private static final Instant INSTANT = Instant.parse("2026-06-15T10:00:00Z");
    private static final Instant OTHER_INSTANT = Instant.parse("2026-09-01T08:30:00Z");
    private static final Clock CLOCK = Clock.fixed(INSTANT, ZoneOffset.UTC);
    private static final Clock OTHER_CLOCK = Clock.fixed(OTHER_INSTANT, ZoneOffset.UTC);

    private static final UUID ID = UUID.fromString("0b8f6a52-6d4e-4a53-9f43-2f1a8f3b7c10");

    @Test
    void roundTripKeepsEveryFigureWhenTheSignalIsSet() {
        StockItemDTO original = new StockItemDTO(ID, 9, 2, 4, 6, true);

        StockItem item = StockItemDomainMapper.toDomain(original, CLOCK);

        assertThat(new StockItemDTO(item)).isEqualTo(original);
    }

    @Test
    void roundTripKeepsEveryFigureWhenTheSignalIsNotSet() {
        StockItemDTO original = new StockItemDTO(ID, 9, 2, 4, 6, false);

        StockItem item = StockItemDomainMapper.toDomain(original, CLOCK);

        assertThat(new StockItemDTO(item)).isEqualTo(original);
    }

    @Test
    void toDomainRestoresEveryFigureOfTheDtoIntoTheStockItem() {
        StockItemDTO dto = new StockItemDTO(ID, 11, 3, 5, 8, true);

        StockItem item = StockItemDomainMapper.toDomain(dto, CLOCK);

        assertThat(item.bookId()).isEqualTo(new BookId(ID));
        assertThat(item.onHand()).isEqualTo(11);
        assertThat(item.reserved()).isEqualTo(3);
        assertThat(item.reorderLevel()).isEqualTo(5);
        assertThat(item.reorderQuantity()).isEqualTo(8);
        assertThat(item.lowStockSignalled()).isTrue();
    }

    @Test
    void toDomainRestoresAnUnsetSignalAsUnset() {
        StockItem item = StockItemDomainMapper.toDomain(new StockItemDTO(ID, 11, 3, 5, 8, false), CLOCK);

        assertThat(item.lowStockSignalled()).isFalse();
    }

    @Test
    void theDtoOfARestoredStockItemHoldsEveryFigureOfTheStockItem() {
        StockItem item = StockItem.restore(new BookId(ID), 12, 4, 2, 9, true, CLOCK);

        StockItemDTO dto = new StockItemDTO(item);

        assertThat(dto.bookId()).isEqualTo(ID);
        assertThat(dto.onHand()).isEqualTo(12);
        assertThat(dto.reserved()).isEqualTo(4);
        assertThat(dto.reorderLevel()).isEqualTo(2);
        assertThat(dto.reorderQuantity()).isEqualTo(9);
        assertThat(dto.lowStockSignalled()).isTrue();
    }

    @Test
    void theDtoOfARestoredStockItemKeepsAnUnsetSignalUnset() {
        StockItem item = StockItem.restore(new BookId(ID), 12, 4, 2, 9, false, CLOCK);

        assertThat(new StockItemDTO(item).lowStockSignalled()).isFalse();
    }

    @Test
    void stillAppliesTheStockItemInvariantsWhenRestoring() {
        StockItemDTO reservedAboveOnHand = new StockItemDTO(ID, 4, 5, 3, 10, false);

        assertThatThrownBy(() -> StockItemDomainMapper.toDomain(reservedAboveOnHand, CLOCK))
                .isInstanceOf(OnHandBelowReservedException.class);
    }

    @Test
    void usesTheGivenClockToStampTheEventsOfTheRestoredStockItem() {
        StockItemDTO lowButUnsignalled = new StockItemDTO(ID, 6, 0, 3, 7, false);

        StockItem item = StockItemDomainMapper.toDomain(lowButUnsignalled, CLOCK);
        item.correct(new Quantity(2), "stocktake");

        assertThat(item.pullDomainEvents()).containsExactly(new StockRunningLowEvent(ID, 2, 7, INSTANT));
    }

    @Test
    void usesAnotherGivenClockToStampTheEventsOfAnotherRestoredStockItem() {
        StockItemDTO lowButUnsignalled = new StockItemDTO(ID, 6, 0, 3, 7, false);

        StockItem item = StockItemDomainMapper.toDomain(lowButUnsignalled, OTHER_CLOCK);
        item.correct(new Quantity(2), "stocktake");

        assertThat(item.pullDomainEvents()).containsExactly(new StockRunningLowEvent(ID, 2, 7, OTHER_INSTANT));
    }

    // Columns: reorder level, reserved, available, expected low stock. On hand is available plus reserved.
    @ParameterizedTest
    @CsvSource({
            "3, 2, 2, true",
            "3, 2, 3, true",
            "3, 2, 4, false",
            "3, 0, 2, true",
            "3, 0, 3, true",
            "3, 0, 4, false",
            "5, 4, 4, true",
            "5, 4, 5, true",
            "5, 4, 6, false",
            "5, 0, 4, true",
            "5, 0, 5, true",
            "5, 0, 6, false"
    })
    void theDtoDerivesAvailableAndLowStockLikeTheStockItemAroundTheReorderLevel(int level, int reserved,
            int available, boolean expectedLowStock) {
        int onHand = available + reserved;
        StockItemDTO dto = new StockItemDTO(ID, onHand, reserved, level, 10, false);
        StockItem item = StockItem.restore(new BookId(ID), onHand, reserved, level, 10, false, CLOCK);

        assertThat(dto.available()).isEqualTo(available);
        assertThat(dto.lowStock()).isEqualTo(expectedLowStock);
        assertThat(item.available()).isEqualTo(available);
        assertThat(item.lowStock()).isEqualTo(expectedLowStock);
        assertThat(new StockItemDTO(item).available()).isEqualTo(available);
        assertThat(new StockItemDTO(item).lowStock()).isEqualTo(expectedLowStock);
    }
}
