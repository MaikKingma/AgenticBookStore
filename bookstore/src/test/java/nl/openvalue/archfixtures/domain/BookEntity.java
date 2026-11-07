package nl.openvalue.archfixtures.domain;

import jakarta.persistence.Entity;

/**
 * Fixture: a domain class that uses Jakarta Persistence. Violates the rule that domain depends on java only and
 * the rule that persistence types live only in data. It lives outside nl.openvalue.bookstore, so Spring never scans it.
 */
@Entity
public class BookEntity {

    int pages;

    long[] printRuns;
}
