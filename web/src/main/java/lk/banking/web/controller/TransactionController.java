package lk.banking.web.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lk.banking.core.dto.LoggedInUser; // Import the LoggedInUser DTO
import lk.banking.core.entity.Transaction;
import lk.banking.services.TransactionServices;
import lk.banking.web.util.WebUtils;
// import lk.banking.core.entity.User; // No longer needed directly here, LoggedInUser is used instead

import java.io.Serializable; // Good practice for managed beans
import java.util.List;

@Named
@RequestScoped
public class TransactionController implements Serializable {

    @Inject
    private TransactionServices transactionService; // Injected EJB for transaction operations

    /**
     * Retrieves a list of transactions for the currently logged-in user.
     * This method assumes a link between User and Customer (via email, as per TransactionServiceImpl)
     * and then fetches transactions for all accounts associated with that customer.
     *
     * @return A list of Transaction entities, or an empty list if no user is logged in
     *         or no transactions are found for the user's accounts.
     *         Exceptions (e.g., UserNotFoundException if WebUtils.getLoggedInUser().getId() somehow invalid)
     *         would typically be handled by JSF's lifecycle or a custom PhaseListener/ExceptionHandler.
     */
    public List<Transaction> getMyTransactions() {
        LoggedInUser loggedInUser = WebUtils.getLoggedInUser(); // Get the LoggedInUser DTO from session

        if (loggedInUser != null) {
            // Call the EJB service to get transactions for the logged-in user's ID
            // transactionService.getTransactionsByUser will handle finding associated accounts and their transactions.
            return transactionService.getTransactionsByUser(loggedInUser.getId());
        } else {
            // No user logged in, return an empty list
            return List.of();
        }
    }

    // You might add methods here for viewing specific transaction details, filtering transactions, etc.
}