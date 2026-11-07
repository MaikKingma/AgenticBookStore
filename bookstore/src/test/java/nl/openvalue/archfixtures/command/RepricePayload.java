package nl.openvalue.archfixtures.command;

/**
 * Fixture: a command payload with a constructor whose first parameter is the price as a float. A rule that skips the
 * first parameter does not report it.
 */
public class RepricePayload {

    RepricePayload(float price, String isbn) {
    }
}
