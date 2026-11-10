package nl.openvalue.archfixtures;

import nl.openvalue.archfixtures.domain.BookEntity;

/**
 * Fixture: a root-level class outside the eight layers that touches domain. Rule 1 must report it, which proves the
 * layered rules consider all dependencies and not only those between layers.
 */
public class RootLevelConfig {

    BookEntity book;
}
