package lk.banking.timer;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Customer;
import lk.banking.core.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Recommended: Add a proper logging framework (e.g., SLF4J with Logback/Log4j2)
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Startup
public class StatementGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatementGenerationService.class);

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    /**
     * Generate monthly statements at midnight on the 1st of every month.
     * Processes statements for the previous month.
     */
    @Schedule(dayOfMonth = "1", hour = "0", minute = "0", second = "0", persistent = false)
    @Transactional // Ensures atomicity for database reads and writes (if any)
    public void generateStatements() {
        LOGGER.info("[Statement] Monthly statement generation started for all customers...");

        LocalDate firstDayOfThisMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate firstDayOfLastMonth = firstDayOfThisMonth.minusMonths(1);

        LocalDateTime from = firstDayOfLastMonth.atStartOfDay();
        LocalDateTime to = firstDayOfThisMonth.atStartOfDay(); // Exclusive upper bound

        LOGGER.info("[Statement] Generating statements for period: {} to {}", from, to);

        // 1. Fetch all customers with their active accounts eagerly to avoid N+1 for accounts
        // Note: isActive is assumed on Customer. If not, remove "c.isActive = TRUE".
        List<Customer> customers = em.createQuery(
                        "SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.accounts a WHERE c.isActive = TRUE AND a.isActive = TRUE", Customer.class)
                .getResultList();

        if (customers.isEmpty()) {
            LOGGER.info("[Statement] No active customers found to generate statements for.");
            return;
        }

        // 2. Fetch all relevant transactions for the period for all accounts in one go
        // Use a IN clause for account IDs if you want to optimize for a subset of accounts
        // For simplicity, fetching all transactions in the period and then grouping.
        // Important: Ensure to exclude archived transactions.
        List<Transaction> allRelevantTransactions = em.createQuery(
                        "SELECT t FROM Transaction t WHERE t.timestamp >= :from AND t.timestamp < :to AND t.isArchived = FALSE", Transaction.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        // Group transactions by account ID for easy lookup
        Map<Long, List<Transaction>> transactionsByAccountId = allRelevantTransactions.stream()
                .collect(Collectors.groupingBy(t -> t.getAccount().getId()));


        for (Customer customer : customers) {
            LOGGER.info("----- Monthly Statement for Customer: {} ({}) -----", customer.getName(), customer.getEmail());

            // Accounts for this specific customer (already fetched eagerly or lazily loaded within transaction)
            List<Account> customerAccounts = customer.getAccounts(); // Already loaded or can be safely loaded here

            if (customerAccounts == null || customerAccounts.isEmpty()) {
                LOGGER.info("No active accounts for customer {}.", customer.getName());
                continue;
            }

            for (Account account : customerAccounts) {
                // Fetch transactions for this specific account from the pre-fetched map
                List<Transaction> transactionsForAccount = transactionsByAccountId
                        .getOrDefault(account.getId(), Collections.emptyList())
                        .stream()
                        .sorted((t1, t2) -> t1.getTimestamp().compareTo(t2.getTimestamp())) // Ensure chronological order for display
                        .collect(Collectors.toList());

                // Calculate opening balance at the start of the period
                // This method correctly computes it by reversing transactions within the period from the current balance.
                BigDecimal openingBalance = getOpeningBalance(account, from, transactionsForAccount);
                BigDecimal closingBalance = account.getBalance(); // This is the current balance

                LOGGER.info("  Account Number: {}", account.getAccountNumber());
                LOGGER.info("  Account Type: {}", account.getType());
                LOGGER.info("  Opening Balance ({}): {}", from.toLocalDate(), openingBalance);
                LOGGER.info("  Transactions ({} to {}):", from.toLocalDate(), to.toLocalDate());

                if (transactionsForAccount.isEmpty()) {
                    LOGGER.info("    No transactions for this period.");
                } else {
                    for (Transaction tx : transactionsForAccount) {
                        LOGGER.info("    [{}] {} {} ({}) | {}",
                                tx.getTimestamp(),
                                tx.getType(),
                                tx.getAmount(),
                                tx.getStatus(),
                                tx.getDescription() != null ? tx.getDescription() : "");
                    }
                }
                LOGGER.info("  Closing Balance ({}): {}", LocalDate.now(), closingBalance); // Closing balance is current balance
                LOGGER.info("---------------------------------------------------");

                // In a real system, generate a PDF/email here using a reporting library
                // notificationService.sendAccountStatement(customer, account, from, to, transactionsForAccount, openingBalance, closingBalance);
            }
        }

        LOGGER.info("[Statement] Monthly statement generation completed.");
    }

    /**
     * Calculate the account's opening balance at the beginning of the period.
     * This method takes the current balance and "rolls back" all transactions
     * that occurred *within* or *after* the 'from' timestamp from the given list of transactions.
     *
     * @param account The account entity (containing current balance).
     * @param from The start timestamp of the period.
     * @param transactionsInPeriod All transactions for this account within the period (from 'from' onwards).
     * @return The calculated opening balance for the period.
     */
    private BigDecimal getOpeningBalance(Account account, LocalDateTime from, List<Transaction> transactionsInPeriod) {
        BigDecimal balance = account.getBalance(); // Start with the current balance

        // Iterate through transactions within the period (and potentially up to now if list contains future tx)
        // and reverse their effect from the current balance to get the balance at 'from' timestamp.
        for (Transaction tx : transactionsInPeriod) {
            // Subtract the amount to roll back its effect
            // If tx.getAmount() is already signed (positive for deposit, negative for withdrawal/payment),
            // simply subtract it.
            balance = balance.subtract(tx.getAmount());
        }

        return balance;
    }
}