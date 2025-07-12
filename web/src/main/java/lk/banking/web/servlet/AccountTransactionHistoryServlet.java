package lk.banking.web.servlet;

import jakarta.ejb.EJB; // For EJB injection
import jakarta.ejb.EJBException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.dto.TransactionDto; // To pass DTOs to JSP
import lk.banking.core.entity.Account;     // To get account details
import lk.banking.core.entity.Transaction; // For Transaction entity (before mapping)
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.BankingException;
import lk.banking.core.exception.UserNotFoundException; // Potentially from TransactionServices.getTransactionsByUser
import lk.banking.core.mapper.TransactionMapper; // For mapping entities to DTOs
import lk.banking.services.AccountService; // To get account details
import lk.banking.services.TransactionServices; // To get transaction history
import lk.banking.web.util.FlashMessageUtil; // For messages
import lk.banking.web.util.ServletUtil;     // For unwrapping EJB exceptions

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Servlet for displaying the detailed transaction history of a specific account.
 */
@WebServlet("/transactions/account/*") // Maps to /transactions/account/{accountId}
public class AccountTransactionHistoryServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AccountTransactionHistoryServlet.class.getName());

    @EJB
    private AccountService accountService;

    @EJB
    private TransactionServices transactionService; // Inject TransactionServices

    /**
     * Handles GET requests to display the transaction history for an account.
     * The account ID is extracted from the URL path.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("AccountTransactionHistoryServlet: Handling GET request.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.hasRole(lk.banking.core.entity.enums.UserRole.CUSTOMER)) {
            LOGGER.warning("AccountTransactionHistoryServlet: Unauthorized access. Redirecting to login.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Extract accountId from the URL path (e.g., /transactions/account/123 -> 123)
        String pathInfo = request.getPathInfo(); // "/{accountId}"
        Long accountId = null;
        if (pathInfo != null && pathInfo.length() > 1) {
            try {
                accountId = Long.parseLong(pathInfo.substring(1)); // Remove leading '/'
            } catch (NumberFormatException e) {
                LOGGER.warning("AccountTransactionHistoryServlet: Invalid account ID format in URL: " + pathInfo);
                FlashMessageUtil.putErrorMessage(request.getSession(), "Invalid account ID provided.");
                response.sendRedirect(request.getContextPath() + "/dashboard"); // Redirect to dashboard on invalid ID
                return;
            }
        }

        if (accountId == null) {
            LOGGER.warning("AccountTransactionHistoryServlet: No account ID provided in URL.");
            FlashMessageUtil.putErrorMessage(request.getSession(), "Account ID is missing for transaction history.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        try {
            // Fetch the account details
            Account account = accountService.getAccountById(accountId);

            // IMPORTANT SECURITY CHECK: Ensure the account belongs to the logged-in user
            // This prevents customers from viewing other customers' account history by changing URL ID.
            if (!account.getCustomer().getId().equals(loggedInUser.getCustomerId())) {
                LOGGER.warning("AccountTransactionHistoryServlet: Unauthorized attempt to view account " + accountId + " by user " + loggedInUser.getUsername());
                FlashMessageUtil.putErrorMessage(request.getSession(), "Access denied: You are not authorized to view this account's history.");
                response.sendRedirect(request.getContextPath() + "/dashboard");
                return;
            }

            // Fetch transactions for this account
            List<Transaction> entityTransactions = transactionService.getTransactionsByAccount(accountId);

            // Convert entities to DTOs for presentation
            List<TransactionDto> transactionDtos = entityTransactions.stream()
                    .map(TransactionMapper::toDto)
                    .collect(Collectors.toList());

            request.setAttribute("account", account); // Pass the account entity (or convert to AccountDto)
            request.setAttribute("transactions", transactionDtos); // Pass DTOs
            LOGGER.info("AccountTransactionHistoryServlet: Loaded " + transactionDtos.size() + " transactions for account " + account.getAccountNumber() + " (ID: " + accountId + ").");

            // Forward to the JSP
            request.getRequestDispatcher("/WEB-INF/jsp/account-transactions.jsp").forward(request, response);

        } catch (EJBException e) {
            Exception unwrappedException = ServletUtil.unwrapEJBException(e);
            String displayErrorMessage;
            if (unwrappedException instanceof BankingException) {
                displayErrorMessage = "Banking error: " + unwrappedException.getMessage();
                LOGGER.warning("AccountTransactionHistoryServlet: Banking exception during history fetch: " + displayErrorMessage);
            } else {
                displayErrorMessage = "An unexpected error occurred while fetching account history. Please try again later.";
                LOGGER.log(java.util.logging.Level.SEVERE, "AccountTransactionHistoryServlet: Unexpected EJBException during history fetch for user " + loggedInUser.getUsername(), unwrappedException);
            }
            FlashMessageUtil.putErrorMessage(request.getSession(), displayErrorMessage);
            response.sendRedirect(request.getContextPath() + "/dashboard"); // Redirect on error
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "AccountTransactionHistoryServlet: An unhandled general error occurred during history fetch for user " + loggedInUser.getUsername(), e);
            FlashMessageUtil.putErrorMessage(request.getSession(), "An unexpected error occurred. Please try again later.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
        }
    }
}