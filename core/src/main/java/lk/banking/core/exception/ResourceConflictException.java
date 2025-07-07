package lk.banking.core.exception;

/**
 * Exception thrown when there is a conflict with an existing resource (e.g., username or email already exists).
 */
public class ResourceConflictException extends BankingException {
    public ResourceConflictException(String message) {
        super(message);
    }
}