package nl.openvalue.archfixtures.command;

import org.springframework.data.repository.CrudRepository;

/**
 * Fixture: a correctly named command class that uses Spring Data, which belongs in data. Its nested class has no
 * allowed suffix and must not be reported, since the naming rules cover top-level classes only.
 */
public class BookCommands {

    CrudRepository<String, Long> repository;

    static class InnerThing {
    }
}
