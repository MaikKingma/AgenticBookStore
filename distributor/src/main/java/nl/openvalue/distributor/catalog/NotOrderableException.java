package nl.openvalue.distributor.catalog;

/**
 * The title exists but cannot be ordered (it is out of print).
 */
public class NotOrderableException extends RuntimeException {

    public NotOrderableException(String ean) {
        super("Title with EAN " + ean + " is not orderable");
    }
}
