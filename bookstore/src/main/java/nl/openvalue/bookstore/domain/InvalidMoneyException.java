package nl.openvalue.bookstore.domain;

/** Thrown when an amount of money is missing, unparsable, negative or not in euro. */
public class InvalidMoneyException extends RuntimeException {

    public InvalidMoneyException(String message) {
        super(message);
    }
}
