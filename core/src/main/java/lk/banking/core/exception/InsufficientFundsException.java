package lk.banking.core.exception;

/**
 * Exception thrown when an account has insufficient funds for a transaction.
 */
public class InsufficientFundsException extends BankingException {
  public InsufficientFundsException(String message) {
    super(message);
  }
}