package lk.banking.core.exception;

/**
 * Exception thrown when a customer cannot be found by id or criteria.
 */
public class CustomerNotFoundException extends BankingException {
    public CustomerNotFoundException(String message) {
        super(message);
    }
}