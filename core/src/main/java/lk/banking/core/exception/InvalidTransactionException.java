package lk.banking.core.exception;

/**
 * Exception thrown when a transaction fails validation or is not allowed.
 */
public class InvalidTransactionException extends BankingException {
    public InvalidTransactionException(String message) {
        super(message);
    }
}