package lk.banking.web.servlet;

import jakarta.ejb.EJB; // Keep for AccountService for now
import jakarta.ejb.EJBException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.dto.TransactionDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.enums.TransactionType;
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.InsufficientFundsException;
import lk.banking.core.exception.InvalidTransactionException;
import lk.banking.core.exception.ValidationException;
import lk.banking.services.AccountService;
// Don't import TransactionManagerBean directly here if using JNDI lookup
// import lk.banking.transaction.TransactionManagerBean;
import lk.banking.transaction.TransactionManager; // Import the interface instead
import lk.banking.web.util.FlashMessageUtil;

import javax.naming.InitialContext; // For JNDI lookup
import javax.naming.NamingException; // For JNDI lookup exceptions
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

@WebServlet("/deposit-withdraw")
public class DepositWithdrawServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DepositWithdrawServlet.class.getName());

    @EJB
    private AccountService accountService;

    // Remove the direct injection for TransactionManagerBean
    // @EJB
    // private TransactionManagerBean transactionManager; // Old line

    // Declare a field for the lookup result
    private TransactionManager transactionManager; // Reference the interface

    // You can initialize in init() or constructor
    @Override
    public void init() throws ServletException {
        super.init();
        try {
            // EJB JNDI name for local interface in EAR structure (Jakarta EE convention)
            // Format: java:app/MODULE_NAME/BEAN_CLASS_NAME!INTERFACE_FULL_NAME
            // For a bean with local interface, it's often simpler:
            // java:global/EAR_NAME/EJB_MODULE_NAME/BEAN_CLASS_NAME!INTERFACE_FULL_NAME
            // Or a simpler one if EJB module is in EAR's lib:
            // java:app/EJB_MODULE_NAME/BEAN_CLASS_NAME!INTERFACE_FULL_NAME
            // Or even shorter if default context is used:
            // java:comp/env/ejb/BEAN_LOOKUP_NAME (if defined in web.xml)
            // Simplest for Glassfish with @Local is often just the bean's interface name

            // Let's try the common JNDI lookup for a local EJB in an EAR
            // Try 1: java:global/EAR_NAME/EJB_MODULE_NAME/BeanClassName!InterfaceFullName
            // Example: java:global/banking-system-ear/transaction-services/TransactionManagerBean!lk.banking.transaction.TransactionManager
            // You might need to adjust EAR_NAME and EJB_MODULE_NAME based on your build.
            // Check your GlassFish server logs for exact JNDI names
            // When deploying an EJB module, GlassFish usually logs the JNDI names. Look for something like:
            // "Binding ejb ... to JNDI name: java:global/..."
            // Usually, the bean name is the simple class name without 'Impl' or 'Bean' suffix.
            // Let's try the full bean name and interface name.

            String jndiName = "java:global/banking-system-ear/transaction-services/TransactionManagerBean!lk.banking.transaction.TransactionManager";
            LOGGER.info("Attempting JNDI lookup for: " + jndiName);
            transactionManager = (TransactionManager) new InitialContext().lookup(jndiName);
            LOGGER.info("JNDI lookup successful for TransactionManagerBean.");

        } catch (NamingException e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to lookup TransactionManagerBean via JNDI.", e);
            throw new ServletException("Failed to initialize DepositWithdrawServlet: TransactionManagerBean not found.", e);
        }
    }

    // ... (rest of doGet and doPost methods remain the same) ...

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("DepositWithdrawServlet: Handling GET request to display form.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.hasRole(lk.banking.core.entity.enums.UserRole.CUSTOMER)) {
            LOGGER.warning("DepositWithdrawServlet: Unauthorized access. Redirecting to login.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            // Note: accountService is still @EJB injected, so it should work if it did before.
            List<Account> accounts = accountService.getAccountsByCustomer(loggedInUser.getCustomerId());
            request.setAttribute("accounts", accounts);
            LOGGER.info("DepositWithdrawServlet: Loaded " + accounts.size() + " accounts for user " + loggedInUser.getUsername());

            request.setAttribute("transactionTypes", new TransactionType[]{TransactionType.DEPOSIT, TransactionType.WITHDRAWAL, TransactionType.PAYMENT});

            request.getRequestDispatcher("/WEB-INF/jsp/deposit-withdraw.jsp").forward(request, response);

        } catch (EJBException e) {
            Throwable cause = e.getCause();
            LOGGER.log(java.util.logging.Level.SEVERE, "DepositWithdrawServlet: EJBException while fetching accounts for " + loggedInUser.getUsername(), e);
            request.setAttribute("errorMessage", "Error loading account data. " + (cause != null ? cause.getMessage() : ""));
            request.setAttribute("accounts", Collections.emptyList());
            request.getRequestDispatcher("/WEB-INF/jsp/deposit-withdraw.jsp").forward(request, response);
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "DepositWithdrawServlet: Unexpected error while fetching accounts for " + loggedInUser.getUsername(), e);
            request.setAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
            request.setAttribute("accounts", Collections.emptyList());
            request.getRequestDispatcher("/WEB-INF/jsp/deposit-withdraw.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("DepositWithdrawServlet: Processing POST request for transaction.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.hasRole(lk.banking.core.entity.enums.UserRole.CUSTOMER)) {
            LOGGER.warning("DepositWithdrawServlet: Unauthorized POST access. Redirecting to login.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String accountIdStr = request.getParameter("accountId");
        String typeStr = request.getParameter("transactionType");
        String amountStr = request.getParameter("amount");
        String description = request.getParameter("description");

        Long accountId = null;
        TransactionType type = null;
        BigDecimal amount = null;

        String errorMessage = null;
        if (accountIdStr == null || accountIdStr.trim().isEmpty()) {
            errorMessage = "Account selection is required.";
        } else {
            try {
                accountId = Long.parseLong(accountIdStr);
            } catch (NumberFormatException e) {
                errorMessage = "Invalid account ID format.";
            }
        }

        if (errorMessage == null && (typeStr == null || typeStr.trim().isEmpty())) {
            errorMessage = "Transaction type is required (Deposit/Withdrawal).";
        } else if (errorMessage == null) {
            try {
                type = TransactionType.valueOf(typeStr.toUpperCase());
                if (!(type == TransactionType.DEPOSIT || type == TransactionType.WITHDRAWAL || type == TransactionType.PAYMENT)) {
                    errorMessage = "Invalid transaction type for this operation. Please choose Deposit, Withdrawal, or Payment.";
                }
            } catch (IllegalArgumentException e) {
                errorMessage = "Invalid transaction type selected.";
            }
        }

        if (errorMessage == null && (amountStr == null || amountStr.trim().isEmpty())) {
            errorMessage = "Amount is required.";
        } else if (errorMessage == null) {
            try {
                amount = new BigDecimal(amountStr);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    errorMessage = "Amount must be a positive number.";
                }
            } catch (NumberFormatException e) {
                errorMessage = "Invalid amount format.";
            }
        }

        if (description != null) {
            description = description.trim();
        }

        if (errorMessage != null) {
            request.setAttribute("errorMessage", errorMessage);
            LOGGER.warning("DepositWithdrawServlet: Validation error: " + errorMessage);
            doGet(request, response);
            return;
        }

        try {
            // Ensure transactionManager is not null (from init() method)
            if (transactionManager == null) {
                LOGGER.severe("TransactionManager is null. JNDI lookup failed during servlet initialization.");
                throw new ServletException("Service unavailable. Please try again later.");
            }

            TransactionDto transactionDto = new TransactionDto();
            transactionDto.setAccountId(accountId);
            transactionDto.setType(type);
            transactionDto.setAmount(amount);
            transactionDto.setTimestamp(LocalDateTime.now());
            transactionDto.setDescription(description);

            transactionManager.processPayment(transactionDto);

            LOGGER.info("DepositWithdrawServlet: Transaction " + type.name() + " of " + amount + " successful for account " + accountId + " by user " + loggedInUser.getUsername());
            FlashMessageUtil.putSuccessMessage(request.getSession(), type.name() + " of " + amount + " to account " + accountId + " completed successfully!");
            response.sendRedirect(request.getContextPath() + "/dashboard");

        } catch (EJBException e) {
            Throwable cause = e.getCause();
            String displayErrorMessage;
            if (cause instanceof AccountNotFoundException) {
                displayErrorMessage = "Account not found: " + cause.getMessage();
                LOGGER.warning("DepositWithdrawServlet: Account not found during transaction: " + cause.getMessage());
            } else if (cause instanceof InsufficientFundsException) {
                displayErrorMessage = "Insufficient funds: " + cause.getMessage();
                LOGGER.warning("DepositWithdrawServlet: Insufficient funds for transaction: " + cause.getMessage());
            } else if (cause instanceof InvalidTransactionException || cause instanceof ValidationException) {
                displayErrorMessage = "Invalid transaction: " + cause.getMessage();
                LOGGER.warning("DepositWithdrawServlet: Invalid transaction details: " + cause.getMessage());
            } else {
                displayErrorMessage = "An unexpected banking error occurred. Please try again.";
                LOGGER.log(java.util.logging.Level.SEVERE, "DepositWithdrawServlet: Unexpected EJBException during transaction processing for user " + loggedInUser.getUsername(), e);
            }
            request.setAttribute("errorMessage", displayErrorMessage);
            doGet(request, response);
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "DepositWithdrawServlet: An unexpected general error occurred during transaction processing for user " + loggedInUser.getUsername(), e);
            request.setAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
            doGet(request, response);
        }
    }
}