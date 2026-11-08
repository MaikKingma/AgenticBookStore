package nl.openvalue.distributor.api;

import nl.openvalue.distributor.catalog.NotOrderableException;
import nl.openvalue.distributor.catalog.TitleNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Turns every expected failure of the REST controllers into the {@link ErrorEnvelope}. Scoped to the controllers of
 * this package, so it never touches the MCP endpoint.
 */
@RestControllerAdvice(basePackageClasses = TitlesController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorEnvelope> invalidRequest(InvalidRequestException exception) {
        return envelope(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorEnvelope> invalidParameter(MethodArgumentTypeMismatchException exception) {
        return envelope(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "Parameter '" + exception.getName() + "' has an invalid value");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorEnvelope> unreadableBody(HttpMessageNotReadableException exception) {
        return envelope(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "The request body is missing or malformed");
    }

    @ExceptionHandler(TitleNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> titleNotFound(TitleNotFoundException exception) {
        return envelope(HttpStatus.NOT_FOUND, "TITLE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(RestockOrderNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> restockOrderNotFound(RestockOrderNotFoundException exception) {
        return envelope(HttpStatus.NOT_FOUND, "RESTOCK_ORDER_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(NotOrderableException.class)
    public ResponseEntity<ErrorEnvelope> notOrderable(NotOrderableException exception) {
        return envelope(HttpStatus.UNPROCESSABLE_CONTENT, "NOT_ORDERABLE", exception.getMessage());
    }

    @ExceptionHandler(TemporarilyUnavailableException.class)
    public ResponseEntity<ErrorEnvelope> temporarilyUnavailable(TemporarilyUnavailableException exception) {
        return envelope(HttpStatus.SERVICE_UNAVAILABLE, "TEMPORARILY_UNAVAILABLE", exception.getMessage());
    }

    private static ResponseEntity<ErrorEnvelope> envelope(HttpStatus status, String errorCode, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorEnvelope(errorCode, message));
    }
}
