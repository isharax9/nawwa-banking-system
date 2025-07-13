package lk.banking.services;

import jakarta.ejb.Local;
import lk.banking.core.dto.AccountDto;
import lk.banking.core.entity.Account;

import java.util.List;

@Local
public interface AccountService {
    /**
     * Creates a new bank account.
     * @param accountDto DTO containing account details and customer ID.
     * @return The newly created Account entity.
     * @throws lk.banking.core.exception.CustomerNotFoundException if the customer is not found.
     * @throws lk.banking.core.exception.DuplicateAccountException if the generated account number conflicts.
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
    List<Account> getAllAccounts(); // NEW METHOD

    /**
     * Updates an existing account's details.
     * @param accountDto DTO containing the updated account details (ID, type, balance).
     * @return The updated Account entity.
     * @throws lk.banking.core.exception.AccountNotFoundException if the account is not found.
     */
    Account updateAccount(AccountDto accountDto);

    /**
     * Deletes an account by its ID. Note: In real systems, accounts are typically deactivated, not hard deleted.
     * @param id The ID of the account to delete.
     * @throws lk.banking.core.exception.AccountNotFoundException if the account is not found.
     */
    void deleteAccount(Long id);

    /**
     * Finds accounts associated with a given user ID.
     * This often implies a User-Customer-Account relationship.
     * @param id The ID of the user.
     * @return A list of Account entities.
     * @throws lk.banking.core.exception.UserNotFoundException if the user is not found.
     */
    List<Account> findAccountsByUserId(Long id);
}