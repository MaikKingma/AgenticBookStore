package nl.openvalue.distributor.catalog;

/**
 * No title in the catalogue has the given EAN.
 */
public class TitleNotFoundException extends RuntimeException {

    public TitleNotFoundException(String ean) {
        super("No title with EAN " + ean);
    }
}
