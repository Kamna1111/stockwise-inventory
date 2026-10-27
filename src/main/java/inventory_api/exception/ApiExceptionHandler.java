package inventory_api.exception;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<?> notFound(ResourceNotFoundException ex) { return response(HttpStatus.NOT_FOUND, ex.getMessage()); }
    @ExceptionHandler(ConflictException.class)
    ResponseEntity<?> conflict(ConflictException ex) { return response(HttpStatus.CONFLICT, ex.getMessage()); }
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<?> concurrentUpdate(ObjectOptimisticLockingFailureException ex) {
        return response(HttpStatus.CONFLICT, "This product changed during the request. Refresh it and try again.");
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> badRequest(IllegalArgumentException ex) { return response(HttpStatus.BAD_REQUEST, ex.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> invalid(MethodArgumentNotValidException ex) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(e -> e.getField(), e -> e.getDefaultMessage(), (a, b) -> a));
        return ResponseEntity.badRequest().body(Map.of("timestamp", Instant.now(), "status", 400, "error", "Validation failed", "fields", fields));
    }
    @ExceptionHandler({DataIntegrityViolationException.class, HttpMessageNotReadableException.class})
    ResponseEntity<?> malformed(Exception ex) { return response(HttpStatus.BAD_REQUEST, "Request could not be saved or parsed"); }
    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now(), "status", status.value(), "error", message));
    }
}
