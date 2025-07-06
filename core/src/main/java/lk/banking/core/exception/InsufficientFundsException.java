package lk.banking.core.exception;

public class InsufficientFundsException extends BankingException {
  public InsufficientFundsException(String message) {
    super(message);
  }
}