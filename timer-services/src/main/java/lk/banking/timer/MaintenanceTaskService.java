package lk.banking.timer;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.ScheduledTransfer;
import lk.banking.core.entity.Transaction;

import java.time.LocalDateTime;
import java.util.List;

// Recommended: Add a proper logging framework (e.g., SLF4J with Logback/Log4j2)
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

@Singleton
@Startup
public class MaintenanceTaskService {

    // private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceTaskService.class); // For proper logging

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    /**
     * Run weekly on Sunday at 4am to perform various maintenance tasks.
     */
    @Schedule(dayOfWeek = "Sun", hour = "4", persistent = false)
    @Transactional
    public void runMaintenance() {
        System.out.println("[Maintenance] Weekly maintenance started at " + LocalDateTime.now());
        // LOGGER.info("[Maintenance] Weekly maintenance started.");

        archiveOldTransactions();
        cleanUpOldScheduledTransfers();
        detectInactiveAccounts();

        System.out.println("[Maintenance] Weekly maintenance completed at " + LocalDateTime.now());
        // LOGGER.info("[Maintenance] Weekly maintenance completed.");
    }

    /**
     * Archives (marks as archived) transactions older than 1 year.
     * IMPORTANT: For banking, direct deletion of transactions is generally discouraged.
     * Instead, mark them as 'archived' in the database.
     * Requires an 'isArchived' boolean field in the Transaction entity.
     */
    private void archiveOldTransactions() {
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);

        // Query for transactions older than cutoff and not yet archived
        List<Transaction> oldTransactions = em.createQuery(
                        "SELECT t FROM Transaction t WHERE t.timestamp < :cutoff AND t.isArchived = FALSE", Transaction.class)
                .setParameter("cutoff", oneYearAgo)
                .getResultList();

        int count = 0;
        if (!oldTransactions.isEmpty()) {
            for (Transaction tx : oldTransactions) {
                tx.setIsArchived(true); // Mark the transaction as archived
                // No need for em.merge(tx) as the entity is managed
                count++;
            }
        }
        System.out.println("[Maintenance] Marked " + count + " transactions older than 1 year as archived.");
        // LOGGER.info("[Maintenance] Marked {} transactions older than 1 year as archived.", count);
    }

    /**
     * Cleans up (deletes) scheduled transfers that have been processed and are older than 30 days.
     * Deleting processed scheduled transfers is generally acceptable as their primary record is in Transactions.
     */
    private void cleanUpOldScheduledTransfers() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<ScheduledTransfer> oldTransfers = em.createQuery(
                        "SELECT s FROM ScheduledTransfer s WHERE s.processed = true AND s.scheduledTime < :cutoff", ScheduledTransfer.class)
                .setParameter("cutoff", thirtyDaysAgo)
                .getResultList();

        int count = 0;
        if (!oldTransfers.isEmpty()) {
            for (ScheduledTransfer transfer : oldTransfers) {
                em.remove(transfer); // This will delete the entity from the database
                count++;
            }
        }
        System.out.println("[Maintenance] Cleaned up " + count + " processed scheduled transfers older than 30 days.");
        // LOGGER.info("[Maintenance] Cleaned up {} processed scheduled transfers older than 30 days.", count);
    }

    /**
     * Detects and logs inactive accounts (no transactions in over 6 months).
     * Optionally, these accounts could be deactivated.
     */
    private void detectInactiveAccounts() {
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);

        // Query to find accounts that have no transactions more recent than the cutoff
        List<Account> inactiveAccounts = em.createQuery(
                        "SELECT a FROM Account a WHERE a.isActive = TRUE AND a.id NOT IN (" + // Consider only active accounts for this check
                                "SELECT DISTINCT t.account.id FROM Transaction t WHERE t.timestamp > :cutoff" +
                                ")", Account.class)
                .setParameter("cutoff", sixMonthsAgo)
                .getResultList();

        if (inactiveAccounts.isEmpty()) {
            System.out.println("[Maintenance] No inactive accounts detected.");
            // LOGGER.info("[Maintenance] No inactive accounts detected.");
        } else {
            System.out.println("[Maintenance] Inactive accounts (no activity in 6+ months):");
            // LOGGER.warn("[Maintenance] Detected {} inactive accounts:", inactiveAccounts.size()); // Use WARN level for important findings
            for (Account acc : inactiveAccounts) {
                System.out.println("[Maintenance] - Account ID: " + acc.getId() + ", Number: " + acc.getAccountNumber());
                // LOGGER.warn("[Maintenance] - Account ID: {}, Number: {}", acc.getId(), acc.getAccountNumber());

                // Optional: Mark account as inactive
                // acc.setIsActive(false);
                // em.merge(acc);
            }
        }
    }
}