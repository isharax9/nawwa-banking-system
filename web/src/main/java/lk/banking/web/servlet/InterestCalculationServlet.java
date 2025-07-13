package lk.banking.web.servlet;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.enums.AccountType; // For filtering accounts
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.BankingException;
import lk.banking.core.exception.InvalidTransactionException;
import lk.banking.services.AccountService;
import lk.banking.web.util.FlashMessageUtil;
import lk.banking.web.util.ServletUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Servlet for allowing customers to check and apply accrued interest to their savings accounts.
 */
@WebServlet("/interest-calculation")
public class InterestCalculationServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(InterestCalculationServlet.class.getName());

    @EJB
    private AccountService accountService;

    /**
     * Handles GET requests to display the interest calculation form.
     * If an account is selected, it calculates and displays the accrued interest.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("InterestCalculationServlet: Handling GET request.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.hasRole(lk.banking.core.entity.enums.UserRole.CUSTOMER)) {
            LOGGER.warning("InterestCalculationServlet: Unauthorized access. Redirecting to login.");
            FlashMessageUtil.putErrorMessage(request.getSession(), "Access denied. Only customers can view interest calculation.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        FlashMessageUtil.retrieveAndClearMessages(request); // Retrieve flash messages

        try {
            // Fetch only SAVINGS accounts for the logged-in user
            List<Account> accounts = accountService.getAccountsByCustomer(loggedInUser.getCustomerId()).stream()
                    .filter(acc -> acc.getType() == AccountType.SAVINGS)
                    .collect(Collectors.toList());
            request.setAttribute("savingsAccounts", accounts);
            LOGGER.info("InterestCalculationServlet: Loaded " + accounts.size() + " savings accounts for user " + loggedInUser.getUsername());

            String selectedAccountIdStr = request.getParameter("accountId");
            if (selectedAccountIdStr != null && !selectedAccountIdStr.trim().isEmpty()) {
                try {
                    Long selectedAccountId = Long.parseLong(selectedAccountIdStr);
                    // Ensure the selected account actually belongs to the user and is a savings account
                    Account selectedAccount = accounts.stream()
                            .filter(acc -> acc.getId().equals(selectedAccountId))
                            .findFirst()
                            .orElse(null);

                    if (selectedAccount == null) {
                        LOGGER.warning("InterestCalculationServlet: Selected account ID " + selectedAccountId + " not found or not a savings account for user " + loggedInUser.getUsername());
                        request.setAttribute("errorMessage", "Invalid savings account selected.");
                    } else {
                        // Calculate accrued interest for the selected account
                        BigDecimal accruedInterest = accountService.calculateAccruedInterest(selectedAccountId, LocalDateTime.now());
                        request.setAttribute("selectedAccount", selectedAccount); // Pass the full account object
                        request.setAttribute("accruedInterest", accruedInterest);
                        request.setAttribute("hasAccruedInterest", accruedInterest.compareTo(BigDecimal.ZERO) > 0);
                        LOGGER.info("InterestCalculationServlet: Accrued interest for account " + selectedAccountId + " is: " + accruedInterest);
                    }
                } catch (NumberFormatException e) {
                    request.setAttribute("errorMessage", "Invalid account ID format.");
                    LOGGER.warning("InterestCalculationServlet: Invalid account ID parameter: " + selectedAccountIdStr);
                } catch (EJBException e) {
                    String displayErrorMessage = ServletUtil.getRootErrorMessage(e, "Error calculating interest.", LOGGER);
                    request.setAttribute("errorMessage", displayErrorMessage);
                    LOGGER.log(java.util.logging.Level.WARNING, "InterestCalculationServlet: EJBException during interest calculation for account " + selectedAccountIdStr, e);
                }
            }

            request.getRequestDispatcher("/WEB-INF/jsp/interest-calculation.jsp").forward(request, response);

        } catch (EJBException e) {
            String displayErrorMessage = ServletUtil.getRootErrorMessage(e, "Error loading savings accounts. Please try again later.", LOGGER);
            request.setAttribute("errorMessage", displayErrorMessage);
            LOGGER.log(java.util.logging.Level.SEVERE, "InterestCalculationServlet: EJBException during initial account fetch for " + loggedInUser.getUsername(), e);
            request.setAttribute("savingsAccounts", Collections.emptyList());
            request.getRequestDispatcher("/WEB-INF/jsp/interest-calculation.jsp").forward(request, response);
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "InterestCalculationServlet: An unhandled general error occurred during interest calculation form display for " + loggedInUser.getUsername(), e);
            request.setAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
            request.setAttribute("savingsAccounts", Collections.emptyList());
            request.getRequestDispatcher("/WEB-INF/jsp/interest-calculation.jsp").forward(request, response);
        }
    }

    /**
     * Handles POST requests to apply accrued interest to the selected account.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("InterestCalculationServlet: Handling POST request to apply interest.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.hasRole(lk.banking.core.entity.enums.UserRole.CUSTOMER)) {
            LOGGER.warning("InterestCalculationServlet: Unauthorized POST access. Redirecting to login.");
            FlashMessageUtil.putErrorMessage(request.getSession(), "Access denied. You do not have permission to apply interest.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String accountIdStr = request.getParameter("accountId");
        String interestAmountStr = request.getParameter("accruedInterest"); // Hidden field from JSP

        Long accountId = null;
        BigDecimal interestAmount = null;
        String errorMessage = null;

        if (accountIdStr == null || accountIdStr.trim().isEmpty()) {
            errorMessage = "Account ID is missing.";
        } else {
            try { accountId = Long.parseLong(accountIdStr); }
            catch (NumberFormatException e) { errorMessage = "Invalid account ID format."; }
        }

        if (errorMessage == null && (interestAmountStr == null || interestAmountStr.trim().isEmpty())) {
            errorMessage = "Interest amount is missing."; // Should be passed from hidden field
        } else if (errorMessage == null) {
            try {
                interestAmount = new BigDecimal(interestAmountStr);
                if (interestAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    errorMessage = "Interest amount to apply must be positive.";
                }
            } catch (NumberFormatException e) {
                errorMessage = "Invalid interest amount format.";
            }
        }

        if (errorMessage != null) {
            request.setAttribute("errorMessage", errorMessage); // Set for re-displaying form with error
            LOGGER.warning("InterestCalculationServlet: Validation error: " + errorMessage);
            doGet(request, response); // Redisplay form with error
            return;
        }

        try {
            // Call the service to apply the interest
            accountService.applyAccruedInterest(accountId, interestAmount);

            String successMessage = String.format("Interest of %s successfully applied to account ID %d!", interestAmount, accountId);
            LOGGER.info("InterestCalculationServlet: " + successMessage + " by user " + loggedInUser.getUsername());
            FlashMessageUtil.putSuccessMessage(request.getSession(), successMessage);
            response.sendRedirect(request.getContextPath() + "/dashboard"); // Redirect to dashboard on success

        } catch (EJBException e) {
            String displayErrorMessage = ServletUtil.getRootErrorMessage(e, "An error occurred while applying interest. Please try again later.", LOGGER);
            request.setAttribute("errorMessage", displayErrorMessage); // Set for re-displaying form with error
            LOGGER.log(java.util.logging.Level.WARNING, "InterestCalculationServlet: EJBException during interest application for account " + accountId + ".", e);
            doGet(request, response); // Redisplay form with error
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "InterestCalculationServlet: An unhandled general error occurred during interest application for " + loggedInUser.getUsername(), e);
            request.setAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
            doGet(request, response); // Redisplay form with error
        }
    }
}