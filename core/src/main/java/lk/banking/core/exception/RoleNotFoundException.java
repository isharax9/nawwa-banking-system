package lk.banking.core.exception;

/**
 * Exception thrown when a role cannot be found in the system.
 */
public class RoleNotFoundException extends BankingException {
    public RoleNotFoundException(String message) {
        super(message);
    }
}