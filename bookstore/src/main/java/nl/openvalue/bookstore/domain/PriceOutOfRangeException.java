package nl.openvalue.bookstore.domain;

/** Thrown when the price of a book is not above the lower bound or is above the upper bound. */
public class PriceOutOfRangeException extends RuntimeException {

    public PriceOutOfRangeException(Money price, Money lowerBoundExclusive, Money upperBoundInclusive) {
        super("Price " + price + " is out of range: it must be greater than " + lowerBoundExclusive
                + " and at most " + upperBoundInclusive);
    }
}
