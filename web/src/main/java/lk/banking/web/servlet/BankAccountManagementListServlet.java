package lk.banking.web.servlet;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.dto.AccountDto; // To pass DTOs to JSP
import lk.banking.core.entity.Account;     // For Account entity (before mapping)
import lk.banking.core.entity.enums.AccountType; // For type conversion
import lk.banking.core.entity.enums.UserRole; // For role checks
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.BankingException;
import lk.banking.core.exception.InvalidTransactionException; // For delete validation
import lk.banking.core.exception.ValidationException; // For type conversion validation
import lk.banking.core.mapper.AccountMapper; // For mapping entities to DTOs
import lk.banking.services.AccountService; // To get all accounts and perform actions
import lk.banking.web.util.FlashMessageUtil;
import lk.banking.web.util.ServletUtil;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Servlet for displaying and managing a list of all bank accounts in the system.
 * Accessible by ADMIN and EMPLOYEE roles only.
 * Provides actions like activate, deactivate, change type, and delete.
 */
@WebServlet("/accounts/manage") // Maps to /accounts/manage
public class BankAccountManagementListServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(BankAccountManagementListServlet.class.getName());

    @EJB
    private AccountService accountService; // Inject AccountService

    /**
     * Handles GET requests to display the list of accounts.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("BankAccountManagementListServlet: Handling GET request.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        // Security check: Only ADMIN or EMPLOYEE can access this page
        if (loggedInUser == null || (!loggedInUser.hasRole(UserRole.ADMIN) && !loggedInUser.hasRole(UserRole.EMPLOYEE))) {
            LOGGER.warning("BankAccountManagementListServlet: Unauthorized access attempt by user: " + (loggedInUser != null ? loggedInUser.getUsername() : "N/A"));
            FlashMessageUtil.putErrorMessage(request.getSession(), "Access denied. You do not have permission to manage bank accounts.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        FlashMessageUtil.retrieveAndClearMessages(request); // Retrieve flash messages on GET

        try {
            // Fetch all accounts from the service layer
            List<Account> entityAccounts = accountService.getAllAccounts();

            // Convert Account entities to AccountDto for presentation
            List<AccountDto> accountDtos = entityAccounts.stream()
                    .map(AccountMapper::toDto)
                    .collect(Collectors.toList());

            request.setAttribute("accounts", accountDtos); // Pass DTOs to JSP
            request.setAttribute("accountTypes", AccountType.values()); // Pass types for dropdown
            LOGGER.info("BankAccountManagementListServlet: Loaded " + accountDtos.size() + " accounts for management.");

            request.getRequestDispatcher("/WEB-INF/jsp/accounts-manage.jsp").forward(request, response);

        } catch (EJBException e) {
            Exception unwrappedException = ServletUtil.unwrapEJBException(e);
            String displayErrorMessage;
            if (unwrappedException instanceof BankingException) {
                displayErrorMessage = "Banking error: " + unwrappedException.getMessage();
                LOGGER.warning("BankAccountManagementListServlet: Banking exception during account fetch: " + displayErrorMessage);
            } else {
                displayErrorMessage = "An unexpected error occurred while fetching accounts. Please try again later.";
                LOGGER.log(java.util.logging.Level.SEVERE, "BankAccountManagementListServlet: Unexpected EJBException during account fetch for " + loggedInUser.getUsername(), unwrappedException);
            }
            FlashMessageUtil.putErrorMessage(request.getSession(), displayErrorMessage);
            response.sendRedirect(request.getContextPath() + "/dashboard"); // Redirect on error
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "BankAccountManagementListServlet: An unhandled general error occurred during account fetch for " + loggedInUser.getUsername(), e);
            FlashMessageUtil.putErrorMessage(request.getSession(), "An unexpected error occurred. Please try again later.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
        }
    }

    /**
     * Handles POST requests for actions on accounts (e.g., activate/deactivate, change type, delete).
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("BankAccountManagementListServlet: Handling POST request for account action.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null || (!loggedInUser.hasRole(UserRole.ADMIN) && !loggedInUser.hasRole(UserRole.EMPLOYEE))) {
            LOGGER.warning("BankAccountManagementListServlet: Unauthorized POST access attempt by user: " + (loggedInUser != null ? loggedInUser.getUsername() : "N/A"));
            FlashMessageUtil.putErrorMessage(request.getSession(), "Access denied. You do not have permission to manage bank accounts.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        String accountIdStr = request.getParameter("accountId");
        String action = request.getParameter("action"); // "activate", "deactivate", "changeType", "delete"
        String newAccountTypeStr = request.getParameter("newAccountType"); // For changeType action

        Long accountId = null;
        String errorMessage = null;

        if (accountIdStr == null || accountIdStr.trim().isEmpty()) {
            errorMessage = "Account ID is missing for action.";
        } else {
            try { accountId = Long.parseLong(accountIdStr); }
            catch (NumberFormatException e) { errorMessage = "Invalid Account ID format."; }
        }

        if (errorMessage == null && (action == null || action.trim().isEmpty())) {
            errorMessage = "Action (activate/deactivate/changeType/delete) is required.";
        }

        if (errorMessage != null) {
            FlashMessageUtil.putErrorMessage(request.getSession(), errorMessage);
            LOGGER.warning("BankAccountManagementListServlet: Validation error for account action: " + errorMessage);
            response.sendRedirect(request.getContextPath() + "/accounts/manage"); // Redirect back to list
            return;
        }

        try {
            String successMessage = null;
            if ("deactivate".equals(action)) {
                accountService.deactivateAccount(accountId);
                successMessage = "Account " + accountId + " successfully deactivated.";
                LOGGER.info("BankAccountManagementListServlet: Deactivated account ID: " + accountId + " by " + loggedInUser.getUsername());
            } else if ("activate".equals(action)) {
                accountService.activateAccount(accountId);
                successMessage = "Account " + accountId + " successfully activated.";
                LOGGER.info("BankAccountManagementListServlet: Activated account ID: " + accountId + " by " + loggedInUser.getUsername());
            } else if ("changeType".equals(action)) {
                if (newAccountTypeStr == null || newAccountTypeStr.trim().isEmpty()) {
                    errorMessage = "New account type is required for conversion.";
                } else {
                    try {
                        AccountType newType = AccountType.valueOf(newAccountTypeStr.toUpperCase());
                        Account updatedAccount = accountService.changeAccountType(accountId, newType);
                        successMessage = "Account " + accountId + " type successfully changed to " + updatedAccount.getType().name() + ".";
                        LOGGER.info("BankAccountManagementListServlet: Account " + accountId + " type changed to " + updatedAccount.getType().name() + " by " + loggedInUser.getUsername());
                    } catch (IllegalArgumentException e) {
                        errorMessage = "Invalid account type selected for conversion.";
                        LOGGER.warning("BankAccountManagementListServlet: Invalid new account type '" + newAccountTypeStr + "' for account ID: " + accountId);
                    }
                }
            } else if ("delete".equals(action)) {
                // Confirm dialog is client-side, but always enforce server-side checks for deletion
                accountService.deleteAccount(accountId); // This performs hard delete
                successMessage = "Account " + accountId + " successfully deleted.";
                LOGGER.info("BankAccountManagementListServlet: Deleted account ID: " + accountId + " by " + loggedInUser.getUsername());
            } else {
                errorMessage = "Invalid action specified.";
                LOGGER.warning("BankAccountManagementListServlet: Invalid action: " + action + " for account ID: " + accountId);
            }

            if (errorMessage != null) {
                FlashMessageUtil.putErrorMessage(request.getSession(), errorMessage);
            } else {
                FlashMessageUtil.putSuccessMessage(request.getSession(), successMessage);
            }
            response.sendRedirect(request.getContextPath() + "/accounts/manage"); // Redirect back to list

        } catch (EJBException e) {
            Exception unwrappedException = ServletUtil.unwrapEJBException(e);
            String displayErrorMessage;
            if (unwrappedException instanceof BankingException) { // Catch common BankingException
                displayErrorMessage = "Banking error: " + unwrappedException.getMessage();
                LOGGER.warning("BankAccountManagementListServlet: Banking exception during account action: " + displayErrorMessage);
            } else {
                displayErrorMessage = "An unexpected error occurred during account action. Please try again later.";
                LOGGER.log(java.util.logging.Level.SEVERE, "BankAccountManagementListServlet: Unexpected EJBException during account action for " + loggedInUser.getUsername(), unwrappedException);
            }
            FlashMessageUtil.putErrorMessage(request.getSession(), displayErrorMessage);
            response.sendRedirect(request.getContextPath() + "/accounts/manage");
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "BankAccountManagementListServlet: An unhandled general error occurred during account action for " + loggedInUser.getUsername(), e);
            FlashMessageUtil.putErrorMessage(request.getSession(), "An unexpected error occurred. Please try again later.");
            response.sendRedirect(request.getContextPath() + "/accounts/manage");
        }
    }
}