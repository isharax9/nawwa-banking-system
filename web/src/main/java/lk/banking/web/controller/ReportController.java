package lk.banking.web.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletResponse; // For file downloads
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Transaction;
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.BankingException; // Catch general banking exceptions
import lk.banking.services.AccountService;
import lk.banking.services.TransactionServices;
import lk.banking.transaction.TransactionManager; // Use the facade for transactions
import lk.banking.web.util.WebUtils; // For user session and messages

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal; // For transaction totals
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// Import for logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@RequestScoped
public class ReportController implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportController.class);

    @Inject
    private AccountService accountService; // To look up accounts if needed
    @Inject
    private TransactionServices transactionService; // To fetch transaction data

    // --- Input properties for report filters ---
    private Long accountId;
    private LocalDate startDate;
    private LocalDate endDate;

    // --- Output properties for JSF display ---
    private List<Transaction> transactionsForReport;
    private BigDecimal totalCredit;
    private BigDecimal totalDebit;

    // --- Actions ---

    /**
     * Action method to generate and display a transaction report on the JSF page.
     * Fetches transactions based on provided filters and calculates summary totals.
     */
    public void viewTransactionReport() {
        FacesContext context = FacesContext.getCurrentInstance();
        transactionsForReport = Collections.emptyList(); // Clear previous results
        totalCredit = BigDecimal.ZERO;
        totalDebit = BigDecimal.ZERO;

        if (accountId == null) {
            WebUtils.addErrorMessage("Please provide an Account ID to generate a report.");
            return;
        }
        if (startDate == null || endDate == null) {
            WebUtils.addErrorMessage("Please provide both a start date and an end date.");
            return;
        }
        if (startDate.isAfter(endDate)) {
            WebUtils.addErrorMessage("Start date cannot be after end date.");
            return;
        }

        try {
            // First, get the account to ensure it exists
            Account account = accountService.getAccountById(accountId); // Will throw AccountNotFoundException if not found

            // Fetch transactions for the specified account and date range
            // NOTE: Your TransactionService.getTransactionsByAccount currently only takes accountId.
            // You would need to add a method like getTransactionsByAccountAndDateRange(Long accountId, LocalDateTime from, LocalDateTime to)
            // to your TransactionServices (in banking-services module) for proper filtering.
            // For now, we'll fetch all by account and then filter in memory if necessary,
            // but a database query is more efficient.
            List<Transaction> allAccountTransactions = transactionService.getTransactionsByAccount(accountId); // Correct service call
            LocalDateTime from = startDate.atStartOfDay();
            LocalDateTime to = endDate.plusDays(1).atStartOfDay(); // End date is inclusive for the whole day

            transactionsForReport = allAccountTransactions.stream()
                    .filter(tx -> !tx.getIsArchived() && tx.getTimestamp().isAfter(from) && tx.getTimestamp().isBefore(to))
                    .sorted((t1, t2) -> t1.getTimestamp().compareTo(t2.getTimestamp()))
                    .collect(Collectors.toList());


            // Calculate totals
            for (Transaction tx : transactionsForReport) {
                if (tx.getAmount().compareTo(BigDecimal.ZERO) > 0) { // Positive amounts are credits (deposits, transfers in)
                    totalCredit = totalCredit.add(tx.getAmount());
                } else { // Negative amounts are debits (withdrawals, payments, transfers out)
                    totalDebit = totalDebit.add(tx.getAmount().abs()); // Use absolute value for total debit
                }
            }

            WebUtils.addInfoMessage("Transaction report generated successfully for account " + account.getAccountNumber() + ".");
            LOGGER.info("Generated transaction report for account ID: {} from {} to {}. Found {} transactions.", accountId, startDate, endDate, transactionsForReport.size());

        } catch (AccountNotFoundException e) {
            WebUtils.addErrorMessage("Error: " + e.getMessage());
            LOGGER.warn("Attempt to generate report for non-existent account ID: {}", accountId);
        } catch (BankingException e) {
            // Catch other specific banking exceptions for tailored messages
            WebUtils.addErrorMessage("Report generation failed: " + e.getMessage());
            LOGGER.error("Banking error during report generation for account ID {}: {}", accountId, e.getMessage(), e);
        } catch (Exception e) {
            // Catch any unexpected exceptions
            WebUtils.addErrorMessage("An unexpected error occurred during report generation. Please try again later.");
            LOGGER.error("Unexpected error during report generation for account ID {}: {}", accountId, e.getMessage(), e);
        }
    }

    /**
     * Action method to download a transaction report (e.g., as a PDF or Excel file).
     * This is a conceptual example; actual file generation needs a library like Apache POI or iText.
     */
    public void downloadTransactionReport() {
        if (transactionsForReport == null || transactionsForReport.isEmpty()) {
            WebUtils.addErrorMessage("Please generate a report first before attempting to download.");
            return;
        }
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();

        try {
            // --- Placeholder for actual file generation logic ---
            // Example: Using Apache POI for a simple CSV/Excel-like output in memory
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("Timestamp,Type,Amount,Status,Description\n");
            for (Transaction tx : transactionsForReport) {
                csvContent.append(String.format("%s,%s,%s,%s,\"%s\"\n",
                        tx.getTimestamp(), tx.getType(), tx.getAmount(), tx.getStatus(), tx.getDescription()));
            }
            byte[] fileBytes = csvContent.toString().getBytes("UTF-8"); // Or generate actual PDF/Excel bytes

            response.setContentType("text/csv"); // For CSV. Change to "application/pdf" or "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            response.setHeader("Content-Disposition", "attachment;filename=TransactionReport_" + accountId + "_" + startDate + "_" + endDate + ".csv");
            response.setContentLength(fileBytes.length);

            try (OutputStream outputStream = response.getOutputStream()) {
                outputStream.write(fileBytes);
            }
            facesContext.responseComplete(); // Important for file downloads in JSF
            LOGGER.info("Transaction report downloaded successfully for account ID: {}.", accountId);

        } catch (IOException e) {
            WebUtils.addErrorMessage("Error downloading report: " + e.getMessage());
            LOGGER.error("IO error during report download for account ID {}: {}", accountId, e.getMessage(), e);
        } catch (Exception e) {
            WebUtils.addErrorMessage("An unexpected error occurred during report download.");
            LOGGER.error("Unexpected error during report download for account ID {}: {}", accountId, e.getMessage(), e);
        }
    }

    // --- Getters and Setters for JSF UI binding ---
    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public List<Transaction> getTransactionsForReport() {
        return transactionsForReport;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }
}