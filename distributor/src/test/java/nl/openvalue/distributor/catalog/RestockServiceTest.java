package nl.openvalue.distributor.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RestockServiceTest {

    private static final ZoneId AMSTERDAM = ZoneId.of("Europe/Amsterdam");

    private static final String IN_STOCK_EAN = "9789900000011";
    private static final String BACKORDER_EAN = "9789900000028";
    private static final String OUT_OF_PRINT_EAN = "9789900000035";

    private final TitleCatalog catalog = new TitleCatalog(List.of(
            title(IN_STOCK_EAN, Availability.IN_STOCK, 5),
            title(BACKORDER_EAN, Availability.BACKORDER, 0),
            title(OUT_OF_PRINT_EAN, Availability.OUT_OF_PRINT, 0)), List.of());

    // The week of Monday 2021-11-01 to Sunday 2021-11-07 (a year well before any real run date).

    @Test
    void inStockOrderOnFridayIsConfirmedForTuesday() {
        RestockService service = serviceAt("2021-11-05T10:00:00Z");

        RestockOrder order = service.placeOrder(IN_STOCK_EAN, 10, "REF-1");

        assertThat(order.status()).isEqualTo(RestockStatus.CONFIRMED);
        assertThat(order.expectedDeliveryDate()).isEqualTo(LocalDate.of(2021, 11, 9));
    }

    @Test
    void orderOnThursdayIsDeliveredOnMonday() {
        RestockService service = serviceAt("2021-11-04T10:00:00Z");

        assertThat(service.placeOrder(IN_STOCK_EAN, 10, null).expectedDeliveryDate())
                .isEqualTo(LocalDate.of(2021, 11, 8));
    }

    @Test
    void orderOnSaturdayIsDeliveredOnTuesday() {
        RestockService service = serviceAt("2021-11-06T10:00:00Z");

        assertThat(service.placeOrder(IN_STOCK_EAN, 10, null).expectedDeliveryDate())
                .isEqualTo(LocalDate.of(2021, 11, 9));
    }

    @Test
    void orderOnSundayIsDeliveredOnTuesday() {
        RestockService service = serviceAt("2021-11-07T10:00:00Z");

        assertThat(service.placeOrder(IN_STOCK_EAN, 10, null).expectedDeliveryDate())
                .isEqualTo(LocalDate.of(2021, 11, 9));
    }

    @Test
    void orderOnMondayIsDeliveredOnWednesday() {
        RestockService service = serviceAt("2021-11-08T10:00:00Z");

        assertThat(service.placeOrder(IN_STOCK_EAN, 10, null).expectedDeliveryDate())
                .isEqualTo(LocalDate.of(2021, 11, 10));
    }

    @Test
    void orderOnWednesdayIsDeliveredOnFriday() {
        RestockService service = serviceAt("2021-11-03T10:00:00Z");

        assertThat(service.placeOrder(IN_STOCK_EAN, 10, null).expectedDeliveryDate())
                .isEqualTo(LocalDate.of(2021, 11, 5));
    }

    @Test
    void backorderIsBackorderedForFourteenCalendarDays() {
        RestockService service = serviceAt("2021-11-05T10:00:00Z");

        RestockOrder order = service.placeOrder(BACKORDER_EAN, 10, null);

        assertThat(order.status()).isEqualTo(RestockStatus.BACKORDERED);
        assertThat(order.expectedDeliveryDate()).isEqualTo(LocalDate.of(2021, 11, 19));
    }

    @Test
    void outOfPrintIsNotOrderableAndStoresNothing() {
        RestockService service = serviceAt("2021-11-05T10:00:00Z");
        assertThat(service.orderCount()).isZero();

        assertThatThrownBy(() -> service.placeOrder(OUT_OF_PRINT_EAN, 10, "REF-9"))
                .isInstanceOf(NotOrderableException.class)
                .hasMessageContaining(OUT_OF_PRINT_EAN);

        assertThat(service.orderCount()).isZero();
    }

    @Test
    void unknownEanThrowsTitleNotFound() {
        RestockService service = serviceAt("2021-11-05T10:00:00Z");

        assertThatThrownBy(() -> service.placeOrder("9780000000002", 10, null))
                .isInstanceOf(TitleNotFoundException.class)
                .hasMessageContaining("9780000000002");

        assertThat(service.orderCount()).isZero();
    }

    @Test
    void nullEanThrowsTitleNotFound() {
        RestockService service = serviceAt("2021-11-05T10:00:00Z");

        assertThatThrownBy(() -> service.placeOrder(null, 10, null)).isInstanceOf(TitleNotFoundException.class);

        assertThat(service.orderCount()).isZero();
    }

    @Test
    void deliveryDateUsesTheClockZone() {
        // 23:30 UTC on Thursday 4 November is 00:30 on Friday 5 November in Amsterdam (CET, UTC+1): Tuesday 9
        // November. A UTC-based date would be the Thursday and give Monday 8 November.
        RestockService service = serviceAt("2021-11-04T23:30:00Z");

        assertThat(service.placeOrder(IN_STOCK_EAN, 10, null).expectedDeliveryDate())
                .isEqualTo(LocalDate.of(2021, 11, 9));
        assertThat(service.quote(catalog.findByEan(IN_STOCK_EAN).orElseThrow()))
                .contains(new DeliveryQuote(RestockStatus.CONFIRMED, LocalDate.of(2021, 11, 9)));
    }

    @Test
    void placedOrderIsFoundById() {
        RestockService service = serviceAt("2021-11-05T10:00:00Z");

        RestockOrder first = service.placeOrder(IN_STOCK_EAN, 10, "SHOP-2021-0042");
        assertThat(service.orderCount()).isEqualTo(1);
        RestockOrder second = service.placeOrder(BACKORDER_EAN, 37, null);
        assertThat(service.orderCount()).isEqualTo(2);

        assertThat(service.findOrder(first.id())).contains(new RestockOrder(
                first.id(), IN_STOCK_EAN, 10, "SHOP-2021-0042", RestockStatus.CONFIRMED, LocalDate.of(2021, 11, 9)));
        assertThat(service.findOrder(second.id())).contains(new RestockOrder(
                second.id(), BACKORDER_EAN, 37, null, RestockStatus.BACKORDERED, LocalDate.of(2021, 11, 19)));
    }

    @Test
    void unknownOrderIdIsEmpty() {
        RestockService service = serviceAt("2021-11-05T10:00:00Z");
        service.placeOrder(IN_STOCK_EAN, 10, null);

        assertThat(service.findOrder("00000000-0000-4000-8000-000000000000")).isEmpty();
        assertThat(service.findOrder("not-an-id")).isEmpty();
    }

    @Test
    void findOrderOfNullIsEmpty() {
        RestockService service = serviceAt("2021-11-05T10:00:00Z");
        service.placeOrder(IN_STOCK_EAN, 10, null);

        assertThat(service.findOrder(null)).isEmpty();
    }

    @Test
    void twoOrdersGetDistinctIds() {
        RestockService service = serviceAt("2021-11-05T10:00:00Z");

        RestockOrder first = service.placeOrder(IN_STOCK_EAN, 10, null);
        RestockOrder second = service.placeOrder(IN_STOCK_EAN, 10, null);

        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(first.id()).isEqualTo(UUID.fromString(first.id()).toString());
        assertThat(second.id()).isEqualTo(UUID.fromString(second.id()).toString());
    }

    @Test
    void quoteForOutOfPrintIsEmpty() {
        RestockService service = serviceAt("2021-11-05T10:00:00Z");

        assertThat(service.quote(catalog.findByEan(OUT_OF_PRINT_EAN).orElseThrow())).isEmpty();
    }

    @Test
    void quoteForInStockIsConfirmedTwoWorkingDaysOut() {
        RestockService service = serviceAt("2021-11-05T10:00:00Z");

        assertThat(service.quote(catalog.findByEan(IN_STOCK_EAN).orElseThrow()))
                .contains(new DeliveryQuote(RestockStatus.CONFIRMED, LocalDate.of(2021, 11, 9)));
    }

    @Test
    void quoteForBackorderIsBackorderedFourteenCalendarDaysOut() {
        RestockService service = serviceAt("2021-11-05T10:00:00Z");

        assertThat(service.quote(catalog.findByEan(BACKORDER_EAN).orElseThrow()))
                .contains(new DeliveryQuote(RestockStatus.BACKORDERED, LocalDate.of(2021, 11, 19)));
    }

    @Test
    void quoteDoesNotStoreAnOrder() {
        RestockService service = serviceAt("2021-11-05T10:00:00Z");

        service.quote(catalog.findByEan(IN_STOCK_EAN).orElseThrow());

        assertThat(service.orderCount()).isZero();
    }

    @Test
    void inStockOrderAboveTheWarehouseQuantityIsStillConfirmed() {
        RestockService service = serviceAt("2021-11-05T10:00:00Z");

        RestockOrder order = service.placeOrder(IN_STOCK_EAN, 500, null);

        assertThat(catalog.findByEan(IN_STOCK_EAN).orElseThrow().warehouseQuantity()).isEqualTo(5);
        assertThat(order.status()).isEqualTo(RestockStatus.CONFIRMED);
        assertThat(order.quantity()).isEqualTo(500);
    }

    @Test
    void placingAnOrderNeverChangesTheWarehouseQuantity() {
        RestockService service = serviceAt("2021-11-05T10:00:00Z");

        service.placeOrder(IN_STOCK_EAN, 37, null);
        service.placeOrder(BACKORDER_EAN, 10, null);

        assertThat(catalog.findByEan(IN_STOCK_EAN).orElseThrow().warehouseQuantity()).isEqualTo(5);
        assertThat(catalog.findByEan(BACKORDER_EAN).orElseThrow().warehouseQuantity()).isZero();
    }

    private RestockService serviceAt(String instant) {
        return new RestockService(catalog, Clock.fixed(Instant.parse(instant), AMSTERDAM));
    }

    private static Title title(String ean, Availability availability, int warehouseQuantity) {
        return new Title(ean, "Fixture " + ean, List.of(new Contributor("Jane Austen", ContributorRole.AUTHOR)),
                List.of("FIC004000"), Format.PAPERBACK, 999, "EUR", availability, warehouseQuantity,
                LocalDate.of(1813, 1, 28));
    }
}
