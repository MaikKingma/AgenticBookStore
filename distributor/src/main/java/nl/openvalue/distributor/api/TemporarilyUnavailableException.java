package nl.openvalue.distributor.api;

/** Thrown by the chaos knobs to simulate a distributor that is temporarily down. */
public class TemporarilyUnavailableException extends RuntimeException {

    public TemporarilyUnavailableException() {
        super("The distributor is temporarily unavailable, try again later");
    }
}
