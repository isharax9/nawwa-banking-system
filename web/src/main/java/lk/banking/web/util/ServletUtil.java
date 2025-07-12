package lk.banking.web.util;

import jakarta.ejb.EJBException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.exception.BankingException;
import lk.banking.core.exception.CustomerNotFoundException;
import lk.banking.core.entity.Account; // Import Account
import lk.banking.core.entity.Customer; // Import Customer
import lk.banking.services.AccountService; // Inject AccountService
import lk.banking.services.CustomerService; // Inject CustomerService

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException; // For JNDI lookup

public class ServletUtil {

    private static final Logger LOGGER = Logger.getLogger(ServletUtil.class.getName());

    // For unwrapping EJBExceptions (existing method)
    public static Exception unwrapEJBException(Exception e) {
        // ... (your existing unwrapEJBException method) ...
        if (e instanceof EJBException) {
            Throwable cause = e.getCause();
            if (cause != null) {
                if (cause instanceof BankingException) {
                    LOGGER.fine("ServletUtil: Unwrapped EJBException to BankingException: " + cause.getClass().getName());
                    return (BankingException) cause;
                } else {
                    LOGGER.fine("ServletUtil: EJBException cause is not a BankingException: " + cause.getClass().getName());
                    return new Exception("EJB operation failed: " + cause.getMessage(), cause);
                }
            } else {
                LOGGER.fine("ServletUtil: EJBException has no cause.");
                return new Exception("EJB operation failed: " + e.getMessage(), e);
            }
        }
        LOGGER.fine("ServletUtil: Not an EJBException. Returning original exception.");
        return e;
    }

    // NEW METHOD: Helper to get accounts for logged-in user (reusable logic)
    public static List<Account> getAccountsForLoggedInUser(HttpServletRequest request, HttpServletResponse response, LoggedInUser loggedInUser, Logger logger)
            throws ServletException, IOException {
        try {
            AccountService accountService = (AccountService) new InitialContext().lookup("java:global/banking-system-ear/banking-services/AccountServiceImpl!lk.banking.services.AccountService");
            CustomerService customerService = (CustomerService) new InitialContext().lookup("java:global/banking-system-ear/banking-services/CustomerServiceImpl!lk.banking.services.CustomerService");

            Customer customer = customerService.getCustomerByEmail(loggedInUser.getEmail());
            if (customer == null) {
                if (logger != null) logger.warning("ServletUtil: Customer profile not found for user's email: " + loggedInUser.getEmail());
                return Collections.emptyList();
            }
            return accountService.getAccountsByCustomer(customer.getId());

        } catch (NamingException e) {
            if (logger != null) logger.log(java.util.logging.Level.SEVERE, "ServletUtil: Failed to lookup AccountService or CustomerService via JNDI.", e);
            throw new ServletException("Service unavailable. Please try again later.", e);
        } catch (EJBException e) {
            Exception unwrapped = unwrapEJBException(e);
            if (logger != null) logger.log(java.util.logging.Level.SEVERE, "ServletUtil: Error fetching accounts: " + unwrapped.getMessage(), unwrapped);
            return Collections.emptyList(); // Return empty list on error
        }
    }
}