// File: lk/banking/web/rest/AccountResource.java
package lk.banking.web.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lk.banking.core.dto.AccountDto;     // DTO for account data
import lk.banking.core.entity.Account;     // Entity (only used for internal conversion)
import lk.banking.core.exception.AccountNotFoundException; // For clarity, though mapped by ExceptionMapper
import lk.banking.core.exception.CustomerNotFoundException; // For clarity, though mapped by ExceptionMapper
import lk.banking.core.mapper.AccountMapper; // Mapper to convert Account entity to AccountDto
import lk.banking.services.AccountService;  // Your Account EJB service
import lk.banking.services.CustomerService; // Potentially needed for account creation/validation

import java.util.List;
import java.util.stream.Collectors; // For stream operations

/**
 * JAX-RS Resource for managing bank accounts.
 * Provides endpoints for CRUD operations on accounts.
 * All responses will be JSON, and requests are expected to be JSON.
 */
@Path("/accounts") // Base path for all account-related endpoints: /api/accounts
@Produces(MediaType.APPLICATION_JSON) // Default media type for responses
@Consumes(MediaType.APPLICATION_JSON) // Default media type for requests
public class AccountResource {

    @Inject
    private AccountService accountService; // Inject your AccountService EJB

    @Inject
    private CustomerService customerService; // Inject CustomerService, potentially needed for validation/lookup

    /**
     * Retrieves all active accounts in the system.
     * Note: In a real system, this endpoint should be restricted to ADMIN/EMPLOYEE roles.
     * For customer users, a separate endpoint like /users/{userId}/accounts would be appropriate.
     *
     * @return A list of AccountDto objects.
     */
    @GET
    public List<AccountDto> getAllAccounts() {
        // This method in AccountService.java returns List<Account>
        List<Account> accounts = accountService.getAllAccounts(); // Assuming getAllAccounts() is available or a query to fetch all active accounts.
        // (You only provided getAccountsByCustomer, getAccountById, getAccountByNumber in AccountService.
        // You might need to add a method to AccountService to get all accounts.)
        return accounts.stream()
                .map(AccountMapper::toDto) // Convert each Account entity to AccountDto
                .collect(Collectors.toList());
        // SECURITY NOTE: This endpoint typically requires an ADMIN or EMPLOYEE role.
        // You would use @RolesAllowed("ADMIN") or @RolesAllowed({"ADMIN", "EMPLOYEE"}) here.
    }

    /**
     * Retrieves a specific account by its ID.
     *
     * @param id The ID of the account.
     * @return 200 OK with the AccountDto if found.
     *         404 Not Found if the account does not exist (handled by BankingExceptionMapper for AccountNotFoundException).
     */
    @GET
    @Path("/{id}") // Full path: /api/accounts/{id}
    public Response getAccountById(@PathParam("id") Long id) {
        // accountService.getAccountById(id) throws AccountNotFoundException if not found.
        // This exception will be caught by BankingExceptionMapper and translated to 404.
        Account account = accountService.getAccountById(id);

        // Convert entity to DTO before returning
        AccountDto accountDto = AccountMapper.toDto(account);
        return Response.ok(accountDto).build();
        // SECURITY NOTE: This endpoint should be protected. A CUSTOMER user should
        // only be allowed to retrieve their OWN accounts, not arbitrary account IDs.
        // An ADMIN/EMPLOYEE could retrieve any account.
    }

    /**
     * Retrieves a specific account by its account number.
     *
     * @param accountNumber The account number string.
     * @return 200 OK with the AccountDto if found.
     *         404 Not Found if the account does not exist.
     */
    @GET
    @Path("/number/{accountNumber}") // Full path: /api/accounts/number/{accountNumber}
    public Response getAccountByNumber(@PathParam("accountNumber") String accountNumber) {
        // accountService.getAccountByNumber(accountNumber) throws AccountNotFoundException if not found.
        Account account = accountService.getAccountByNumber(accountNumber);

        AccountDto accountDto = AccountMapper.toDto(account);
        return Response.ok(accountDto).build();
        // SECURITY NOTE: Similar to getAccountById, restrict access based on roles/ownership.
    }

    /**
     * Creates a new bank account.
     *
     * @param accountDto DTO containing account details (e.g., type, initial balance, customerId).
     * @return 201 Created with the newly created AccountDto.
     *         400 Bad Request if validation fails (e.g., missing customerId, invalid amount).
     *         404 Not Found if the specified customerId does not exist (handled by BankingExceptionMapper for CustomerNotFoundException).
     *         409 Conflict if an account number collision occurs (handled by BankingExceptionMapper for DuplicateAccountException).
     */
    @POST // Full path: /api/accounts
    public Response createAccount(AccountDto accountDto) {
        // accountDto should contain customerId for the new account
        if (accountDto.getCustomerId() == null) {
            // Use your ValidationException from core.exception
            throw new WebApplicationException("Customer ID is required to create an account.", Response.Status.BAD_REQUEST);
        }

        // accountService.createAccount handles customer lookup and account number generation/uniqueness.
        // It throws CustomerNotFoundException or DuplicateAccountException.
        Account newAccount = accountService.createAccount(accountDto);

        // Convert the newly created Account entity to AccountDto for response
        AccountDto newAccountDto = AccountMapper.toDto(newAccount);

        // Return 201 Created status with the new account's details
        return Response.status(Response.Status.CREATED)
                .entity(newAccountDto)
                .build();
        // SECURITY NOTE: Account creation typically requires an ADMIN or EMPLOYEE role.
    }

    /**
     * Updates an existing account's details.
     *
     * @param id The ID of the account to update.
     * @param accountDto DTO containing updated account details (type, balance).
     * @return 200 OK with the updated AccountDto.
     *         404 Aren't Found if the account does not exist.
     *         400 Bad Request if input is invalid.
     */
    @PUT
    @Path("/{id}") // Full path: /api/accounts/{id}
    public Response updateAccount(@PathParam("id") Long id, AccountDto accountDto) {
        if (accountDto == null || id == null || !id.equals(accountDto.getId())) {
            throw new WebApplicationException("Invalid request. Account ID in path must match DTO ID.", Response.Status.BAD_REQUEST);
        }
        // Ensure the ID in the DTO is set from the path param, or if DTO contains it, it must match.
        accountDto.setId(id);

        // accountService.updateAccount handles the lookup and update.
        // It throws AccountNotFoundException if the account doesn't exist.
        Account updatedAccount = accountService.updateAccount(accountDto);

        AccountDto updatedAccountDto = AccountMapper.toDto(updatedAccount);
        return Response.ok(updatedAccountDto).build();
        // SECURITY NOTE: Account updates typically require an ADMIN or EMPLOYEE role.
    }

    /**
     * Deletes (or deactivates) a bank account.
     * Note: In a real banking system, accounts are typically deactivated, not hard deleted.
     *
     * @param id The ID of the account to delete.
     * @return 204 No Content on successful deletion.
     *         404 Not Found if the account does not exist.
     */
    @DELETE
    @Path("/{id}") // Full path: /api/accounts/{id}
    public Response deleteAccount(@PathParam("id") Long id) {
        // accountService.deleteAccount handles the lookup and removal.
        // It throws AccountNotFoundException if the account doesn't exist.
        accountService.deleteAccount(id);
        return Response.noContent().build(); // 204 No Content for successful deletion
        // SECURITY NOTE: Account deletion typically requires an ADMIN role.
    }
}