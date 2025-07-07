package lk.banking.core.exception;

/**
 * Exception thrown when attempting to create an account with a duplicate account number.
 */
public class DuplicateAccountException extends BankingException {
  public DuplicateAccountException(String message) {
    super(message);
  }
}