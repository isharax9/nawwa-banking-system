// lk.banking.core.exception.UserNotFoundException.java
package lk.banking.core.exception;

/**
 * Exception thrown when a user cannot be found by id or criteria.
 */
public class UserNotFoundException extends BankingException {
  public UserNotFoundException(String message) {
    super(message);
  }
}