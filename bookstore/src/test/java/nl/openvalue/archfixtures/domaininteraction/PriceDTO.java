package nl.openvalue.archfixtures.domaininteraction;

import java.util.UUID;

/**
 * Fixture: a domaininteraction class with a method that takes the new price as a double parameter, placed between
 * two other parameters so that it is neither the first nor the last one.
 */
public class PriceDTO {

    void changePrice(UUID bookId, double newPrice, String reason) {
    }
}
