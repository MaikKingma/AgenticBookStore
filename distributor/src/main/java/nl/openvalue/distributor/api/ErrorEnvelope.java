package nl.openvalue.distributor.api;

/**
 * The distributor's own error body: exactly {@code errorCode} and {@code message}, served as plain
 * {@code application/json}.
 */
public record ErrorEnvelope(String errorCode, String message) {
}
