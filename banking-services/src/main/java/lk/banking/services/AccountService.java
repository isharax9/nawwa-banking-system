package lk.banking.services;

import jakarta.ejb.Local;
import lk.banking.core.dto.AccountDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.enums.AccountType; // Import for changeAccountType

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Local
public interface AccountService {
    /**
     * Creates a new bank account.
     * @param accountDto DTO containing account details and customer ID.
     * @return The newly created Account entity.
     * @throws lk.banking.core.exception.CustomerNotFoundException if the customer is not found.
     * @throws lk.banking.core.exception.DuplicateAccountException if the generated account number conflicts.
     * @throws lk.banking.core.exception.ValidationException if initial balance is not positive.
     */
    Account createAccount(AccountDto accountDto);

    /**
     * Retrieves an account by its ID.
     * @param id The ID of the account.
     * @return The Account entity.
     * @throws lk.banking.core.exception.AccountNotFoundException if the account is not found.
     */
    Account getAccountById(Long id);

    /**
     * Retrieves an account by its account number.
     * @param accountNumber The account number string.
     * @return The Account entity.
     * @throws lk.banking.core.exception.AccountNotFoundException if the account is not found.
     */
    Account getAccountByNumber(String accountNumber);

    /**
     * Retrieves all accounts belonging to a specific customer.
     * @param customerId The ID of the customer.
     * @return A list of Account entities.
     */
    List<Account> getAccountsByCustomer(Long customerId);

    /**
     * Retrieves all accounts in the system.
     * @return A list of all Account entities.
     */
    List<Account> getAllAccounts();

    /**
     * Updates an existing account's details.
     * @param accountDto DTO containing the updated account details (ID, type, balance).
     * @return The updated Account entity.
     * @throws lk.banking.core.exception.AccountNotFoundException if the account is not found.
     * @throws lk.banking.core.exception.ValidationException if update data is invalid.
     */
    Account updateAccount(AccountDto accountDto);

    /**
     * Deactivates an account by setting its isActive status to false (soft delete).
     * @param id The ID of the account to deactivate.
     * @return true if the account was deactivated or already inactive.
     * @throws lk.banking.core.exception.AccountNotFoundException if the account is not found.
     */
    boolean deactivateAccount(Long id); // NEW METHOD

    /**
     * Activates an account by setting its isActive status to true.
     * @param id The ID of the account to activate.
     * @return true if the account was activated or already active.
     * @throws lk.banking.core.exception.AccountNotFoundException if the account is not found.
     */
    boolean activateAccount(Long id); // NEW METHOD

    /**
     * Changes the type of an existing account (account conversion).
     * @param id The ID of the account to change.
     * @param newType The new AccountType.
     * @return The updated Account entity.
     * @throws lk.banking.core.exception.AccountNotFoundException if the account is not found.
     * @throws lk.banking.core.exception.InvalidTransactionException if the new type is invalid for conversion or other business rules.
     */
    Account changeAccountType(Long id, AccountType newType); // NEW METHOD

    /**
     * Deletes an account permanently by its ID. This is a hard delete.
     * Use with extreme caution, as it removes all associated data.
     * @param id The ID of the account to delete.
     * @throws lk.banking.core.exception.AccountNotFoundException if the account is not found.
     * @throws lk.banking.core.exception.InvalidTransactionException if deletion is not allowed (e.g., non-zero balance).
     */
    void deleteAccount(Long id); // Existing, but added clarification on hard delete

    /**
     * Finds accounts associated with a given user ID.
     * This often implies a User-Customer-Account relationship.
     * @param id The ID of the user.
     * @return A list of Account entities.
     * @throws lk.banking.core.exception.UserNotFoundException if the user is not found.
     */
    List<Account> findAccountsByUserId(Long id);

    /**
     * Calculates the interest accrued on a savings account since the last application date.
     * This method does NOT apply the interest to the account balance.
     * @param accountId The ID of the savings account.
     * @param toDateTime The date/time up to which interest should be calculated (usually LocalDateTime.now()).
     * @return The calculated accrued interest amount.
     * @throws lk.banking.core.exception.AccountNotFoundException if the account is not found or is not a savings account.
     * @throws lk.banking.core.exception.InvalidTransactionException if calculation is not applicable or input is invalid.
     */
    BigDecimal calculateAccruedInterest(Long accountId, LocalDateTime toDateTime); // NEW METHOD

    /**
     * Applies a given amount of accrued interest to a savings account and records a transaction.
     * This method assumes the interest amount has already been calculated.
     * @param accountId The ID of the account to apply interest to.
     * @param interestAmount The amount of interest to apply.
     * @return The updated Account entity.
     * @throws lk.banking.core.exception.AccountNotFoundException if the account is not found or is not a savings account.
     * @throws lk.banking.core.exception.InvalidTransactionException if application is not allowed (e.g., negative amount, account inactive).
     */
    Account applyAccruedInterest(Long accountId, BigDecimal interestAmount); // NEW METHOD
}