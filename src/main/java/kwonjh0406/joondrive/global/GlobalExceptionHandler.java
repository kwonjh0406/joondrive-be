package kwonjh0406.joondrive.global;

import kwonjh0406.joondrive.auth.exception.EmailAlreadyExistsException;
import kwonjh0406.joondrive.file.exception.StorageLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<String> handleEmailExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(StorageLimitExceededException.class)
    public ResponseEntity<String> handleStorageLimitExceeded(StorageLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.valueOf(413)).body(ex.getMessage());
    }
}

