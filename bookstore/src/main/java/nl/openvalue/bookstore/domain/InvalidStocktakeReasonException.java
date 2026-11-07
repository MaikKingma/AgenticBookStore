package nl.openvalue.bookstore.domain;

/** Thrown when the reason for a stocktake correction is missing, too short or too long. */
public class InvalidStocktakeReasonException extends RuntimeException {

    public InvalidStocktakeReasonException(String message) {
        super(message);
    }
}
