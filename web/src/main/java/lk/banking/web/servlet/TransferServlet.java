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
import lk.banking.core.entity.ScheduledTransfer;
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.BankingException;
import lk.banking.core.exception.InsufficientFundsException;
import lk.banking.core.exception.InvalidTransactionException;
import lk.banking.core.exception.ScheduledTransferException;
import lk.banking.core.exception.ValidationException;
import lk.banking.services.AccountService;
import lk.banking.transaction.ScheduledTransferService;
import lk.banking.transaction.TransactionManager;
import lk.banking.web.util.FlashMessageUtil;
import lk.banking.web.util.ServletUtil;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;


@WebServlet("/transfer")
public class TransferServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TransferServlet.class.getName());

    @EJB
    private AccountService accountService;

    private TransactionManager transactionManager;
    private ScheduledTransferService scheduledTransferService;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            String transactionManagerJndiName = "java:global/banking-system-ear/transaction-services/TransactionManagerBean!lk.banking.transaction.TransactionManager";
            LOGGER.info("TransferServlet: Attempting JNDI lookup for TransactionManager at: " + transactionManagerJndiName);
            transactionManager = (TransactionManager) new InitialContext().lookup(transactionManagerJndiName);
            LOGGER.info("TransferServlet: JNDI lookup successful for TransactionManager.");

            String scheduledTransferServiceJndiName = "java:global/banking-system-ear/transaction-services/ScheduledTransferServiceImpl!lk.banking.transaction.ScheduledTransferService";
            LOGGER.info("TransferServlet: Attempting JNDI lookup for ScheduledTransferService at: " + scheduledTransferServiceJndiName);
            scheduledTransferService = (ScheduledTransferService) new InitialContext().lookup(scheduledTransferServiceJndiName);
            LOGGER.info("TransferServlet: JNDI lookup successful for ScheduledTransferService.");

        } catch (NamingException e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "TransferServlet: Failed to lookup one or more EJB services via JNDI.", e);
            throw new ServletException("Failed to initialize TransferServlet: Required services not found.", e);
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

            // NEW: Set the minimum date for the scheduledDate input to TODAY
            request.setAttribute("minScheduledDate", LocalDate.now().toString()); // Allow today for testing

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
        String toAccountNumberStr = request.getParameter("toAccountNumber");
        String amountStr = request.getParameter("amount");
        String scheduleTransferParam = request.getParameter("scheduleTransfer");
        String scheduledDateStr = request.getParameter("scheduledDate");
        String scheduledTimeStr = request.getParameter("scheduledTime");


        Long fromAccountId = null;
        Long toAccountId = null;
        BigDecimal amount = null;
        boolean isScheduled = "on".equals(scheduleTransferParam);
        LocalDateTime scheduledDateTime = null;

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

        if (errorMessage == null && isScheduled) {
            if (scheduledDateStr == null || scheduledDateStr.trim().isEmpty()) {
                errorMessage = "Scheduled date is required for scheduled transfers.";
            } else if (scheduledTimeStr == null || scheduledTimeStr.trim().isEmpty()) {
                errorMessage = "Scheduled time is required for scheduled transfers.";
            } else {
                try {
                    LocalDate scheduledDate = LocalDate.parse(scheduledDateStr);
                    LocalTime scheduledTime = LocalTime.parse(scheduledTimeStr);
                    scheduledDateTime = LocalDateTime.of(scheduledDate, scheduledTime);

                    // TEMPORARY CHANGE FOR TESTING: Allow scheduling for current time or later
                    if (scheduledDateTime.isBefore(LocalDateTime.now())) { // Check against current time
                        errorMessage = "Scheduled transfers must be set for the current time or later.";
                    }
                    // ORIGINAL: if (scheduledDateTime.isBefore(LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0))) {
                    // ORIGINAL:    errorMessage = "Scheduled transfers must be set for tomorrow or later.";
                    // ORIGINAL: }
                } catch (DateTimeParseException e) {
                    errorMessage = "Invalid date or time format for scheduled transfer.";
                    LOGGER.warning("TransferServlet: DateTimeParse error: " + e.getMessage());
                }
            }
        }

        if (errorMessage != null) {
            request.setAttribute("errorMessage", errorMessage);
            LOGGER.warning("TransferServlet: Validation error: " + errorMessage);
            doGet(request, response);
            return;
        }

        try {
            if (transactionManager == null) {
                LOGGER.severe("TransferServlet: TransactionManager is null. JNDI lookup failed during servlet initialization.");
                throw new ServletException("Service 'TransactionManager' unavailable. Please try again later.");
            }
            if (isScheduled && scheduledTransferService == null) {
                LOGGER.severe("TransferServlet: ScheduledTransferService is null. JNDI lookup failed during servlet initialization.");
                throw new ServletException("Service 'ScheduledTransferService' unavailable. Please try again later.");
            }

            Account toAccount = null;
            try {
                toAccount = accountService.getAccountByNumber(toAccountNumberStr);
                toAccountId = toAccount.getId();
            } catch (AccountNotFoundException e) {
                errorMessage = "Destination account not found: " + e.getMessage();
                LOGGER.warning("TransferServlet: Destination account lookup failed: " + e.getMessage());
                request.setAttribute("errorMessage", errorMessage);
                doGet(request, response);
                return;
            }

            if (fromAccountId.equals(toAccountId)) {
                errorMessage = "Cannot transfer funds to the same account.";
            }

            if (errorMessage != null) {
                request.setAttribute("errorMessage", errorMessage);
                LOGGER.warning("TransferServlet: Business validation error: " + errorMessage);
                doGet(request, response);
                return;
            }

            Account fromAccount = null;
            try {
                fromAccount = accountService.getAccountById(fromAccountId);
            } catch (AccountNotFoundException e) {
                errorMessage = "Source account not found: " + e.getMessage();
                LOGGER.severe("TransferServlet: Source account lookup failed for logged-in user's account: " + e.getMessage());
                request.setAttribute("errorMessage", errorMessage);
                doGet(request, response);
                return;
            }

            String successMessage;
            if (isScheduled) {
                ScheduledTransfer scheduledTransfer = new ScheduledTransfer(
                        fromAccount,
                        toAccount,
                        amount,
                        scheduledDateTime
                );
                scheduledTransferService.scheduleTransfer(scheduledTransfer);
                successMessage = String.format("Transfer of $%s from %s to %s scheduled successfully for %s!",
                        amount, fromAccount.getAccountNumber(), toAccountNumberStr, scheduledDateTime);
                LOGGER.info("TransferServlet: Scheduled transfer created: " + scheduledTransfer.getId());
            } else {
                TransferRequestDto transferRequestDto = new TransferRequestDto(fromAccountId, toAccountId, amount);
                transactionManager.transferFunds(transferRequestDto);
                successMessage = String.format("Immediate transfer of $%s from %s to %s completed successfully!",
                        amount, fromAccount.getAccountNumber(), toAccountNumberStr);
                LOGGER.info("TransferServlet: Immediate transfer completed.");
            }

            FlashMessageUtil.putSuccessMessage(request.getSession(), successMessage);
            response.sendRedirect(request.getContextPath() + "/dashboard");

        } catch (Exception e) {
            Exception unwrappedException = ServletUtil.unwrapEJBException(e);

            String displayErrorMessage;
            if (unwrappedException instanceof BankingException) {
                BankingException bankingCause = (BankingException) unwrappedException;
                if (bankingCause instanceof AccountNotFoundException) {
                    displayErrorMessage = "Account not found: " + bankingCause.getMessage();
                } else if (bankingCause instanceof InsufficientFundsException) {
                    displayErrorMessage = "Insufficient funds: " + bankingCause.getMessage();
                } else if (bankingCause instanceof InvalidTransactionException || bankingCause instanceof ValidationException) {
                    displayErrorMessage = "Invalid transfer: " + bankingCause.getMessage();
                } else if (bankingCause instanceof ScheduledTransferException) {
                    displayErrorMessage = "Scheduled transfer error: " + bankingCause.getMessage();
                } else {
                    displayErrorMessage = "A banking-related error occurred: " + bankingCause.getMessage();
                }
                LOGGER.log(java.util.logging.Level.WARNING, "TransferServlet: Banking error during transfer for user " + loggedInUser.getUsername() + ": " + displayErrorMessage, unwrappedException);
            } else {
                displayErrorMessage = "An unexpected error occurred. Please try again later.";
                LOGGER.log(java.util.logging.Level.SEVERE, "TransferServlet: An unexpected error during transfer processing for user " + loggedInUser.getUsername(), unwrappedException);
            }
            request.setAttribute("errorMessage", displayErrorMessage);
            doGet(request, response);
        }
    }
}