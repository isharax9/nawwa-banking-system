package lk.banking.core.exception;

/**
 * Exception thrown for errors related to scheduled transfers.
 */
public class ScheduledTransferException extends BankingException {
    public ScheduledTransferException(String message) {
        super(message);
    }
    public ScheduledTransferException(String message, Throwable cause) {
        super(message, cause);
    }
}