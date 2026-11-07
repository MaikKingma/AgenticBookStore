package nl.openvalue.archfixtures.process;

import nl.openvalue.archfixtures.assistant.BookAssistant;
import nl.openvalue.archfixtures.domain.BookEntity;
import org.springframework.web.client.RestClient;

/**
 * Fixture: a process class that references a domain class that is not an event (so the exception for events stays
 * narrow), uses RestClient outside acl and uses another outer adapter (assistant).
 */
public class LeakyDelegate {

    BookEntity entity;

    RestClient restClient;

    BookAssistant assistant;
}
