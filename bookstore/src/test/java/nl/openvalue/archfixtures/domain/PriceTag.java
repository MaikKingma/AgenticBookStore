package nl.openvalue.archfixtures.domain;

/**
 * Fixture: a domain class with a method whose only parameter is the price as a double. A rule that ignores code units
 * with a single parameter does not report it.
 */
public class PriceTag {

    void reprice(double price) {
    }
}
