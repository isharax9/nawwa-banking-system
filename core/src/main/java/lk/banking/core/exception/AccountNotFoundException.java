package lk.banking.core.exception;

/**
 * Exception thrown when an account cannot be found by id or number.
 */
public class AccountNotFoundException extends BankingException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}