package nl.openvalue.bookstore.query;

import java.util.List;
import java.util.Map;
import nl.openvalue.bookstore.domaininteraction.ConflictException;
import nl.openvalue.bookstore.domaininteraction.InvalidInputException;
import nl.openvalue.bookstore.domaininteraction.NotFoundException;
import nl.openvalue.bookstore.domaininteraction.UnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns the use-case exceptions of the queries into RFC 9457 problem details. */
@RestControllerAdvice(basePackageClasses = BookQueries.class)
public class QueryExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage(), exception.title(), exception.code(),
                exception.details());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage(), exception.title(), exception.code(),
                exception.details());
    }

    @ExceptionHandler(InvalidInputException.class)
    public ProblemDetail handleInvalidInput(InvalidInputException exception) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage(), exception.title(),
                exception.code(), exception.details());
    }

    @ExceptionHandler(UnavailableException.class)
    public ProblemDetail handleUnavailable(UnavailableException exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), exception.title(),
                exception.code(), exception.details());
    }

    private static ProblemDetail problem(HttpStatus status, String detail, String title, String code,
            List<Map<String, String>> details) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("code", code);
        problem.setProperty("details", details);
        return problem;
    }
}
