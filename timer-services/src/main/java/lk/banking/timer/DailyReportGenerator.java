package lk.banking.timer;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors; // Added for potential future use or cleaner stream operations

// Recommended: Add a proper logging framework (e.g., SLF4J with Logback/Log4j2)
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

@Singleton
@Startup
public class DailyReportGenerator {

    // private static final Logger LOGGER = LoggerFactory.getLogger(DailyReportGenerator.class); // For proper logging

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    /**
     * Runs every day at 3am.
     * Generates and logs summary reports for the previous day:
     * - Number of new accounts
     * - Total volume and count of transactions (by type)
     * - Accounts with most activity
     */
    @Schedule(hour = "3", minute = "0", second = "0", persistent = false)
    @Transactional // Ensures the entire method runs in a transaction
    public void generateDailyReports() {
        LocalDate reportDate = LocalDate.now().minusDays(1);
        LocalDateTime from = reportDate.atStartOfDay();
        LocalDateTime to = reportDate.plusDays(1).atStartOfDay();

        System.out.println("===== Daily Banking Report for " + reportDate + " =====");
        // LOGGER.info("===== Daily Banking Report for {} =====", reportDate); // Use this with proper logging

        // 1. New accounts created in the reporting period
        // Corrected query: Use 'createdAt' field from Account entity
        Long newAccounts = em.createQuery(
                        "SELECT COUNT(a) FROM Account a WHERE a.createdAt >= :from AND a.createdAt < :to", Long.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        System.out.println("New accounts created: " + newAccounts);
        // LOGGER.info("New accounts created: {}", newAccounts); // Use this with proper logging

        // 2. Transactions for the day (all types)
        List<Transaction> transactions = em.createQuery(
                        "SELECT t FROM Transaction t WHERE t.timestamp >= :from AND t.timestamp < :to", Transaction.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        Map<TransactionType, Integer> transactionCountByType = new HashMap<>();
        Map<TransactionType, BigDecimal> transactionSumByType = new HashMap<>();

        for (Transaction tx : transactions) {
            transactionCountByType.put(tx.getType(),
                    transactionCountByType.getOrDefault(tx.getType(), 0) + 1);
            // Use tx.getAmount().abs() to sum up the absolute values for total volume
            transactionSumByType.put(tx.getType(),
                    transactionSumByType.getOrDefault(tx.getType(), BigDecimal.ZERO).add(tx.getAmount().abs()));
        }

        System.out.println("Transaction Summary by Type:");
        // LOGGER.info("Transaction Summary by Type:"); // Use this with proper logging
        for (TransactionType type : TransactionType.values()) {
            System.out.println(type + ": count=" +
                    transactionCountByType.getOrDefault(type, 0)
                    + ", amount=" +
                    transactionSumByType.getOrDefault(type, BigDecimal.ZERO)
            );
            // LOGGER.info("{}: count={}, amount={}", type,
            //         transactionCountByType.getOrDefault(type, 0),
            //         transactionSumByType.getOrDefault(type, BigDecimal.ZERO)
            // ); // Use this with proper logging
        }

        // 3. Find top 5 accounts by number of transactions
        List<Object[]> topAccounts = em.createQuery(
                        "SELECT t.account.accountNumber, COUNT(t) AS txCount " +
                                "FROM Transaction t WHERE t.timestamp >= :from AND t.timestamp < :to " +
                                "GROUP BY t.account.accountNumber ORDER BY txCount DESC", Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .setMaxResults(5)
                .getResultList();

        System.out.println("Accounts with most activity:");
        // LOGGER.info("Accounts with most activity:"); // Use this with proper logging
        if (topAccounts.isEmpty()) {
            System.out.println("No account activity recorded for this period.");
            // LOGGER.info("No account activity recorded for this period.");
        } else {
            for (Object[] row : topAccounts) {
                System.out.println("Account: " + row[0] + " -> " + row[1] + " transactions");
                // LOGGER.info("Account: {} -> {} transactions", row[0], row[1]); // Use this with proper logging
            }
        }

        System.out.println("===== End of Daily Report =====");
        // LOGGER.info("===== End of Daily Report ====="); // Use this with proper logging
    }
}