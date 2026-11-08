package nl.openvalue.distributor.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Answers a wrong HTTP method (405) and an unsupported request body type (415) under {@code /api/} with the
 * {@link ErrorEnvelope}, keeping the status and the {@code Allow} or {@code Accept} header Spring computed.
 *
 * <p>This advice is deliberately not scoped to the controllers of this package like {@link ApiExceptionHandler}: a 405
 * is raised while the handler is still being looked up, so there is no handler yet and Spring never consults a scoped
 * advice for it. Because it applies to every request, it guards on the request path and, for anything outside
 * {@code /api/} (the actuator, MCP), rethrows the same exception so Spring Boot answers exactly as it would without
 * this class.
 */
@RestControllerAdvice
public class ApiRequestMismatchHandler {

    private static final String API_PREFIX = "/api/";

    @ExceptionHandler
    public ResponseEntity<ErrorEnvelope> methodNotSupported(HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) throws HttpRequestMethodNotSupportedException {
        requireApiPath(exception, request);
        return envelope(exception, "METHOD_NOT_ALLOWED",
                "Method " + exception.getMethod() + " is not allowed here");
    }

    @ExceptionHandler
    public ResponseEntity<ErrorEnvelope> mediaTypeNotSupported(HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request) throws HttpMediaTypeNotSupportedException {
        requireApiPath(exception, request);
        return envelope(exception, "UNSUPPORTED_MEDIA_TYPE",
                "The request body must be application/json");
    }

    /** Rethrows {@code exception} unchanged unless the request is for a path under {@code <contextPath>/api/}. */
    private static <E extends Exception> void requireApiPath(E exception, HttpServletRequest request) throws E {
        if (!request.getRequestURI().startsWith(request.getContextPath() + API_PREFIX)) {
            throw exception;
        }
    }

    private static ResponseEntity<ErrorEnvelope> envelope(ErrorResponse exception, String errorCode, String message) {
        return ResponseEntity.status(exception.getStatusCode())
                .headers(exception.getHeaders())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorEnvelope(errorCode, message));
    }
}
