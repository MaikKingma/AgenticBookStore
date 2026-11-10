package nl.openvalue.bookstore.domain;

/**
 * Marks something that happened in the domain and that other parts of the application may react to. Aggregates
 * register domain events; flows pull them and publish them inside the use-case transaction.
 */
public interface DomainEvent {
}
