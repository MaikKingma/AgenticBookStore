package nl.openvalue.archfixtures.command;

/**
 * Fixture: a command payload with a constructor that takes the price as a float parameter. The float parameter is
 * not the first one, so only a rule that checks every parameter reports it.
 */
public class PricePayload {

    PricePayload(String isbn, float price) {
    }
}
