package lk.banking.core.exception;

/**
 * Exception thrown when input validation fails.
 */
public class ValidationException extends BankingException {
  public ValidationException(String message) {
    super(message);
  }
}