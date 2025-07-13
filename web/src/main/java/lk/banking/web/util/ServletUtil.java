package lk.banking.web.util;

import jakarta.ejb.EJB; // For @EJB if used internally, though JNDI is being used for services
import jakarta.ejb.EJBException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Customer;
import lk.banking.core.exception.*;
import lk.banking.services.AccountService; // For AccountService lookup
import lk.banking.services.CustomerService; // For CustomerService lookup

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class for common Servlet-related helper methods,
 * including unwrapping exceptions from EJB calls and providing user-friendly display messages.
 */
public class ServletUtil {

    private static final Logger LOGGER = Logger.getLogger(ServletUtil.class.getName());

    /**
     * Unwraps the root cause of an Exception (especially EJBException) and returns a user-friendly error message string.
     * This method directly handles known BankingException types by comparing class names as strings
     * to bypass classloader 'instanceof' issues.
     *
     * @param e The exception caught.
     * @param defaultMessage The default message to return if the exception is unrecognized or a critical system error.
     * @param logger The logger to use for internal logging of the cause.
     * @return A user-friendly error message string.
     */
    public static String getRootErrorMessage(Exception e, String defaultMessage, Logger logger) {
        Throwable currentCause = e;
        // Iterate through causes to find the deepest, most specific exception
        // Stop if getCause() returns null or the same cause to prevent infinite loops for circular causes
        while (currentCause.getCause() != null && currentCause.getCause() != currentCause) {
            currentCause = currentCause.getCause();
        }

        // Now 'currentCause' is the true root cause (or the original 'e' if no deeper cause).

        // Log the actual class name of the root cause for debugging
        if (logger != null) {
            logger.fine("ServletUtil: Root cause class name: " + currentCause.getClass().getName() + ", Message: " + currentCause.getMessage());
        }

        // Use string comparison for class names to bypass 'instanceof' classloader issues
        String causeClassName = currentCause.getClass().getName();

        // Handle specific BankingException types
        if (causeClassName.equals(AccountNotFoundException.class.getName()) ||
                causeClassName.equals(CustomerNotFoundException.class.getName()) ||
                causeClassName.equals(UserNotFoundException.class.getName())) {
            // For Not Found exceptions, use their message directly
            return currentCause.getMessage();
        } else if (causeClassName.equals(InsufficientFundsException.class.getName()) ||
                causeClassName.equals(InvalidTransactionException.class.getName()) ||
                causeClassName.equals(ValidationException.class.getName()) ||
                causeClassName.equals(ResourceConflictException.class.getName()) ||
                causeClassName.equals(ScheduledTransferException.class.getName())) {
            // For validation or business rule exceptions, use their message directly
            return currentCause.getMessage();
        } else if (causeClassName.equals(UnauthorizedAccessException.class.getName())) {
            // For UnauthorizedAccessException, use its specific message
            // This includes the "banned temporarily" message
            return currentCause.getMessage();
        } else if (causeClassName.equals(RoleNotFoundException.class.getName())) {
            // Special message for RoleNotFound
            return "System configuration error: Required user role not found. Please contact support.";
        }
        // Fallback for any other type of exception (including unhandled BankingExceptions, or general RuntimeExceptions)
        if (logger != null) {
            logger.log(java.util.logging.Level.SEVERE, "ServletUtil: Unrecognized or unexpected exception type in root cause: " + causeClassName + ". Original exception: " + e.getMessage(), e);
        }
        return defaultMessage; // Return the generic default message
    }

    /**
     * Helper method to retrieve all accounts associated with the logged-in user.
     * This method directly performs JNDI lookups for required services.
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @param loggedInUser The LoggedInUser DTO from session.
     * @param logger The logger of the calling servlet for consistent logging.
     * @return A list of Account entities.
     * @throws ServletException if JNDI lookup fails or a critical service error occurs.
     * @throws IOException if a redirect is performed due to unauthenticated state (should be handled by filter).
     */
    public static List<Account> getAccountsForLoggedInUser(HttpServletRequest request, HttpServletResponse response, LoggedInUser loggedInUser, Logger logger)
            throws ServletException, IOException {
        // This method assumes the loggedInUser is not null and is a CUSTOMER.
        // The calling servlet should handle authentication and role checks.

        // JNDI lookups (assuming these are constant throughout the app's lifecycle)
        AccountService accountService = null;
        CustomerService customerService = null;
        try {
            // Confirm these JNDI names from your GlassFish logs
            accountService = (AccountService) new InitialContext().lookup("java:global/banking-system-ear/banking-services/AccountServiceImpl!lk.banking.services.AccountService");
            customerService = (CustomerService) new InitialContext().lookup("java:global/banking-system-ear/banking-services/CustomerServiceImpl!lk.banking.services.CustomerService");
        } catch (NamingException e) {
            if (logger != null) logger.log(java.util.logging.Level.SEVERE, "ServletUtil: Failed to lookup AccountService or CustomerService via JNDI in getAccountsForLoggedInUser.", e);
            throw new ServletException("Service unavailable: Account/Customer services not found.", e);
        }

        try {
            Customer customer = null;
            String userEmail = loggedInUser.getEmail();
            if (userEmail != null && !userEmail.trim().isEmpty()) {
                customer = customerService.getCustomerByEmail(userEmail);
            }

            if (customer == null) {
                if (logger != null) logger.warning("ServletUtil: Customer profile not found for user's email: " + loggedInUser.getEmail());
                // In a real app, might flash an error message here.
                return Collections.emptyList();
            }
            // Ensure customer accounts are eagerly fetched if you intend to access customer details from accounts in JSP.
            // accountService.getAccountsByCustomer already uses JOIN FETCH, so it's good.
            return accountService.getAccountsByCustomer(customer.getId());

        } catch (EJBException e) {
            // Use getRootErrorMessage for getting the display message from the EJBException
            String displayMsg = getRootErrorMessage(e, "Error fetching accounts from EJB.", logger);
            if (logger != null) logger.log(java.util.logging.Level.SEVERE, "ServletUtil: Error fetching accounts: " + displayMsg, e);
            return Collections.emptyList(); // Return empty list on error
        } catch (Exception e) {
            if (logger != null) logger.log(java.util.logging.Level.SEVERE, "ServletUtil: Unexpected error fetching accounts for logged-in user: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public static Exception unwrapEJBException(EJBException e) {

        return null;
    }
}