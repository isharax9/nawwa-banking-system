package lk.banking.core.exception;

/**
 * Exception thrown when a transaction times out or takes too long to process.
 */
public class TransactionTimeoutException extends BankingException {
    public TransactionTimeoutException(String message) {
        super(message);
    }
    public TransactionTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}