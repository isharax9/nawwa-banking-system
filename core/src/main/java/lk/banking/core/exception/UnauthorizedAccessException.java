package lk.banking.core.exception;

/**
 * Exception thrown when a user tries to access a resource or perform an action they are not authorized for.
 */
public class UnauthorizedAccessException extends BankingException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}