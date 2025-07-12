package lk.banking.web.servlet;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.dto.TransferRequestDto;
import lk.banking.core.entity.Account;
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.InsufficientFundsException;
import lk.banking.core.exception.InvalidTransactionException;
import lk.banking.core.exception.ValidationException;
import lk.banking.services.AccountService;
import lk.banking.transaction.TransactionManager; // Use interface type
import lk.banking.web.util.FlashMessageUtil;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;


@WebServlet("/transfer")
public class TransferServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TransferServlet.class.getName());

    @EJB
    private AccountService accountService;

    // Declare as interface type, as per previous fix
    private TransactionManager transactionManager;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            // Retrieve the exact JNDI name from your GlassFish logs
            String jndiName = "java:global/banking-system-ear/transaction-services/TransactionManagerBean!lk.banking.transaction.TransactionManager"; // CONFIRM THIS JNDI NAME FROM GLASSFISH LOGS
            LOGGER.info("TransferServlet: Attempting JNDI lookup for TransactionManager at: " + jndiName);
            transactionManager = (TransactionManager) new InitialContext().lookup(jndiName);
            LOGGER.info("TransferServlet: JNDI lookup successful for TransactionManager.");
        } catch (NamingException e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "TransferServlet: Failed to lookup TransactionManager via JNDI.", e);
            throw new ServletException("Failed to initialize TransferServlet: TransactionManager not found.", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("TransferServlet: Handling GET request to display transfer form.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.hasRole(lk.banking.core.entity.enums.UserRole.CUSTOMER)) {
            LOGGER.warning("TransferServlet: Unauthorized access. Redirecting to login.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            List<Account> accounts = accountService.getAccountsByCustomer(loggedInUser.getCustomerId());
            request.setAttribute("accounts", accounts);
            LOGGER.info("TransferServlet: Loaded " + accounts.size() + " accounts for user " + loggedInUser.getUsername());

            request.getRequestDispatcher("/WEB-INF/jsp/transfer.jsp").forward(request, response);

        } catch (EJBException e) {
            Throwable cause = e.getCause();
            LOGGER.log(java.util.logging.Level.SEVERE, "TransferServlet: EJBException while fetching accounts for " + loggedInUser.getUsername(), e);
            request.setAttribute("errorMessage", "Error loading account data. " + (cause != null ? cause.getMessage() : ""));
            request.setAttribute("accounts", Collections.emptyList());
            request.getRequestDispatcher("/WEB-INF/jsp/transfer.jsp").forward(request, response);
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "TransferServlet: Unexpected error while fetching accounts for " + loggedInUser.getUsername(), e);
            request.setAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
            request.setAttribute("accounts", Collections.emptyList());
            request.getRequestDispatcher("/WEB-INF/jsp/transfer.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("TransferServlet: Processing POST request for fund transfer.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.hasRole(lk.banking.core.entity.enums.UserRole.CUSTOMER)) {
            LOGGER.warning("TransferServlet: Unauthorized POST access. Redirecting to login.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String fromAccountIdStr = request.getParameter("fromAccountId");
        String toAccountNumberStr = request.getParameter("toAccountNumber"); // Changed from toAccountIdStr
        String amountStr = request.getParameter("amount");

        Long fromAccountId = null;
        Long toAccountId = null; // Will be obtained after lookup
        BigDecimal amount = null;

        // --- Input Validation ---
        String errorMessage = null;
        if (fromAccountIdStr == null || fromAccountIdStr.trim().isEmpty()) {
            errorMessage = "Source account is required.";
        } else {
            try { fromAccountId = Long.parseLong(fromAccountIdStr); }
            catch (NumberFormatException e) { errorMessage = "Invalid source account ID format."; }
        }

        if (errorMessage == null && (toAccountNumberStr == null || toAccountNumberStr.trim().isEmpty())) {
            errorMessage = "Destination account number is required.";
        }
        // No format validation for account number here, as AccountService.getAccountByNumber will handle "not found"


        if (errorMessage == null && (amountStr == null || amountStr.trim().isEmpty())) {
            errorMessage = "Amount is required.";
        } else if (errorMessage == null) {
            try {
                amount = new BigDecimal(amountStr);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    errorMessage = "Transfer amount must be a positive number.";
                }
            } catch (NumberFormatException e) {
                errorMessage = "Invalid amount format.";
            }
        }

        if (errorMessage != null) {
            request.setAttribute("errorMessage", errorMessage);
            LOGGER.warning("TransferServlet: Validation error: " + errorMessage);
            doGet(request, response); // Redisplay form with error, preserving input
            return;
        }

        try {
            if (transactionManager == null) {
                LOGGER.severe("TransferServlet: TransactionManager is null. JNDI lookup failed during servlet initialization.");
                throw new ServletException("Service unavailable. Please try again later.");
            }

            // Look up destination account by number
            Account toAccount = null;
            try {
                toAccount = accountService.getAccountByNumber(toAccountNumberStr);
                toAccountId = toAccount.getId(); // Get the ID for the DTO
            } catch (AccountNotFoundException e) {
                errorMessage = "Destination account not found: " + e.getMessage();
                LOGGER.warning("TransferServlet: Destination account lookup failed: " + e.getMessage());
                request.setAttribute("errorMessage", errorMessage);
                doGet(request, response);
                return;
            }


            // Additional business-level validation (after lookup)
            if (fromAccountId.equals(toAccountId)) {
                errorMessage = "Cannot transfer funds to the same account.";
            }

            if (errorMessage != null) {
                request.setAttribute("errorMessage", errorMessage);
                LOGGER.warning("TransferServlet: Business validation error: " + errorMessage);
                doGet(request, response); // Redisplay form with error
                return;
            }


            // Create a TransferRequestDto
            TransferRequestDto transferRequestDto = new TransferRequestDto(fromAccountId, toAccountId, amount);

            // Process the transfer using the TransactionManagerBean's FundTransferService
            transactionManager.transferFunds(transferRequestDto);

            LOGGER.info("TransferServlet: Funds transfer of " + amount + " from account ID " + fromAccountId + " to account ID " + toAccountId + " (Number: " + toAccountNumberStr + ") successful by user " + loggedInUser.getUsername());
            FlashMessageUtil.putSuccessMessage(request.getSession(), "Transfer of " + amount + " from " + fromAccountId + " to " + toAccountNumberStr + " completed successfully!");
            response.sendRedirect(request.getContextPath() + "/dashboard");

        } catch (EJBException e) {
            Throwable cause = e.getCause();
            String displayErrorMessage;
            if (cause instanceof AccountNotFoundException) {
                displayErrorMessage = "Account not found: " + cause.getMessage();
                LOGGER.warning("TransferServlet: Account not found during transfer: " + cause.getMessage());
            } else if (cause instanceof InsufficientFundsException) {
                displayErrorMessage = "Insufficient funds: " + cause.getMessage();
                LOGGER.warning("TransferServlet: Insufficient funds for transfer: " + cause.getMessage());
            } else if (cause instanceof InvalidTransactionException || cause instanceof ValidationException) {
                displayErrorMessage = "Invalid transfer: " + cause.getMessage();
                LOGGER.warning("TransferServlet: Invalid transfer details: " + cause.getMessage());
            } else {
                displayErrorMessage = "An unexpected banking error occurred. Please try again.";
                LOGGER.log(java.util.logging.Level.SEVERE, "TransferServlet: Unexpected EJBException during transfer processing for user " + loggedInUser.getUsername(), e);
            }
            request.setAttribute("errorMessage", displayErrorMessage);
            doGet(request, response); // Redisplay form with error
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "TransferServlet: An unexpected general error occurred during transfer processing for user " + loggedInUser.getUsername(), e);
            request.setAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
            doGet(request, response);
        }
    }
}