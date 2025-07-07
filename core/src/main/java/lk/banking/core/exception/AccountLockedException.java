package lk.banking.core.exception;

/**
 * Exception thrown when an account is locked due to security reasons or too many failed attempts.
 */
public class AccountLockedException extends BankingException {
    public AccountLockedException(String message) {
        super(message);
    }
}