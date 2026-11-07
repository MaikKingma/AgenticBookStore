package nl.openvalue.archfixtures.assistant;

/**
 * Fixture: an assistant class with a double field that is not money. The money rule only guards domain,
 * domaininteraction, command and query, so it must not report this class.
 */
public class TemperatureProperties {

    double temperature;
}
