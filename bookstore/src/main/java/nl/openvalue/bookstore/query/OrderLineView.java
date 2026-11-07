package nl.openvalue.bookstore.query;

import nl.openvalue.bookstore.domaininteraction.OrderLineDTO;

/**
 * One line of an order as a reader sees it: the isbn and title as they were when the order was placed, the quantity,
 * the unit price and the line total as plain decimal strings. The book id is not shown.
 */
public record OrderLineView(String isbn, String title, int quantity, String unitPrice, String lineTotal) {

    public static OrderLineView from(OrderLineDTO line) {
        return new OrderLineView(line.isbn(), line.title(), line.quantity(), line.unitPriceAmount().toPlainString(),
                line.lineTotal().toPlainString());
    }
}
