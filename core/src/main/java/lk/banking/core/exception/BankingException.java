package lk.banking.core.exception;

/**
 * Base exception for all banking-related errors.
 * Extend this class for specific business exceptions.
 */
public class BankingException extends RuntimeException {
    public BankingException(String message) {
        super(message);
    }
    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}