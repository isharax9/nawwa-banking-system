package lk.banking.timer;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lk.banking.core.dto.DailyReportDto; // Import new DTO
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
@Startup
public class DailyReportGenerator implements DailyReportService { // Implement the new interface

    private static final Logger LOGGER = Logger.getLogger(DailyReportGenerator.class.getName());

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    // A field to store the last generated report for quick access (simple caching)
    private DailyReportDto lastGeneratedReport;

    // --- Timer Method (unchanged schedule) ---
    @Schedule(hour = "3", minute = "0", second = "0", persistent = false)
    @Transactional
    public void generateDailyReports() {
        LocalDate reportDate = LocalDate.now().minusDays(1);
        LOGGER.info("===== Automated Daily Banking Report Generation for " + reportDate + " =====");
        DailyReportDto report = generateReportData(reportDate); // Call the new reusable method
        this.lastGeneratedReport = report; // Store the report

        // Log the report content (as previously)
        logReport(report);

        LOGGER.info("===== End of Automated Daily Report Generation =====");
    }

    // --- DailyReportService Interface Implementations ---
    @Override
    public DailyReportDto generateReportForDate(LocalDate date) {
        LOGGER.info("Generating report data for requested date: " + date);
        return generateReportData(date);
    }

    @Override
    public DailyReportDto getYesterdayReport() {
        if (this.lastGeneratedReport != null && this.lastGeneratedReport.getReportDate().equals(LocalDate.now().minusDays(1))) {
            LOGGER.info("Returning cached yesterday's report.");
            return this.lastGeneratedReport;
        } else {
            LOGGER.info("Cache missed or outdated. Generating yesterday's report on demand.");
            // If cache is empty or for a different day, generate it on demand
            return generateReportData(LocalDate.now().minusDays(1));
        }
    }


    // --- Core Report Generation Logic (now a private, reusable method) ---
    private DailyReportDto generateReportData(LocalDate reportDate) {
        LocalDateTime from = reportDate.atStartOfDay();
        LocalDateTime to = reportDate.plusDays(1).atStartOfDay();

        // New accounts created
        Long newAccounts = em.createQuery(
                        "SELECT COUNT(a) FROM Account a WHERE a.createdAt >= :from AND a.createdAt < :to", Long.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        // Transactions for the day (all types)
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
            transactionSumByType.put(tx.getType(),
                    transactionSumByType.getOrDefault(tx.getType(), BigDecimal.ZERO).add(tx.getAmount().abs()));
        }

        // Find top 5 accounts by number of transactions
        List<Object[]> topAccountsRaw = em.createQuery(
                        "SELECT t.account.accountNumber, COUNT(t) AS txCount " +
                                "FROM Transaction t WHERE t.timestamp >= :from AND t.timestamp < :to " +
                                "GROUP BY t.account.accountNumber ORDER BY txCount DESC", Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .setMaxResults(5)
                .getResultList();

        List<DailyReportDto.TopAccountActivity> topAccounts = topAccountsRaw.stream()
                .map(row -> new DailyReportDto.TopAccountActivity((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());

        return new DailyReportDto(reportDate, newAccounts, transactionCountByType, transactionSumByType, topAccounts);
    }

    // --- Helper to log the report to console (for automated run logging) ---
    private void logReport(DailyReportDto report) {
        LOGGER.info("Report Date: " + report.getReportDate());
        LOGGER.info("New accounts created: " + report.getNewAccountsCount());
        LOGGER.info("Transaction Summary by Type:");
        for (TransactionType type : TransactionType.values()) {
            LOGGER.info(type + ": count=" +
                    report.getTransactionCountByType().getOrDefault(type, 0)
                    + ", amount=" +
                    report.getTransactionSumByType().getOrDefault(type, BigDecimal.ZERO)
            );
        }
        LOGGER.info("Accounts with most activity:");
        if (report.getTopAccounts().isEmpty()) {
            LOGGER.info("No account activity recorded for this period.");
        } else {
            for (DailyReportDto.TopAccountActivity row : report.getTopAccounts()) {
                LOGGER.info("Account: " + row.getAccountNumber() + " -> " + row.getTransactionCount() + " transactions");
            }
        }
    }
}