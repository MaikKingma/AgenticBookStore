package nl.openvalue.distributor.api;

/**
 * A request failed one of the hand-written checks (paging bounds, quantity range, blank EAN). Answered with 400
 * {@code INVALID_REQUEST}.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
