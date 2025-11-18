package kwonjh0406.joondrive.file.exception;

public class StorageLimitExceededException extends RuntimeException {
    public StorageLimitExceededException(String message) {
        super(message);
    }
}

