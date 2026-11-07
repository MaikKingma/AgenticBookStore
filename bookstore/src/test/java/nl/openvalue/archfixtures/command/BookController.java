package nl.openvalue.archfixtures.command;

import nl.openvalue.archfixtures.data.BookDataServiceImpl;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fixture: a command class with a wrong name that calls a data class directly. The annotation is allowed here, since
 * command may hold controllers.
 */
@RestController
public class BookController {

    Object call() {
        return new BookDataServiceImpl();
    }
}
