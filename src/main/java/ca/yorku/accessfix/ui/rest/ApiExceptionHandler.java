package ca.yorku.accessfix.ui.rest;

import ca.yorku.accessfix.common.AccessFixException;
import ca.yorku.accessfix.common.AuditBusyException;
import ca.yorku.accessfix.common.InvalidInputException;
import ca.yorku.accessfix.common.NotFoundException;
import ca.yorku.accessfix.common.PageLoadException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns AccessFix exceptions into JSON errors the React app can show: {"error": "..."}. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AccessFixException.class)
    public ResponseEntity<Map<String, String>> handle(AccessFixException e) {
        HttpStatus status = switch (e) {
            case InvalidInputException ignored -> HttpStatus.BAD_REQUEST;
            case NotFoundException ignored -> HttpStatus.NOT_FOUND;
            case AuditBusyException ignored -> HttpStatus.CONFLICT;
            case PageLoadException ignored -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }

    /** Makes unfinished starter methods obvious in the browser instead of a generic 500. */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, String>> notImplemented(UnsupportedOperationException e) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("error", String.valueOf(e.getMessage())));
    }
}
