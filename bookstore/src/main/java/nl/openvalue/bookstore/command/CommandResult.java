package nl.openvalue.bookstore.command;

import java.util.UUID;

/** Response body of a command that creates something: the id of the new aggregate. */
public record CommandResult(UUID id) {
}
