package cz.semester.courseapp.http;

import cz.semester.courseapp.app.NotFoundOrConflictException;
import cz.semester.courseapp.domain.DomainException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiError> domain(DomainException exception) {
        LOGGER.warn("event=domain_error message=\"{}\"", exception.getMessage());
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(NotFoundOrConflictException.class)
    ResponseEntity<ApiError> notFoundOrConflict(NotFoundOrConflictException exception) {
        LOGGER.warn("event=application_conflict message=\"{}\"", exception.getMessage());
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<ApiError> validation(Exception exception) {
        LOGGER.warn("event=validation_error message=\"{}\"", exception.getMessage());
        return error(HttpStatus.BAD_REQUEST, "Neplatny vstup: " + exception.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> responseStatus(ResponseStatusException exception) {
        LOGGER.warn(
                "event=response_status_error status={} message=\"{}\"",
                exception.getStatusCode().value(),
                exception.getReason());
        return error(HttpStatus.valueOf(exception.getStatusCode().value()), exception.getReason());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiError.of(message, status.value()));
    }
}
