package lk.banking.timer;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.ScheduledTransfer;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Singleton
@Startup
public class MaintenanceTaskService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    /**
     * Run weekly on Sunday at 4am
     */
    @Schedule(dayOfWeek = "Sun", hour = "4", minute = "0", second = "0", persistent = false)
    @Transactional
    public void runMaintenance() {
        System.out.println("[Maintenance] Weekly maintenance started at " + LocalDateTime.now());

        archiveOldTransactions();
        cleanUpOldScheduledTransfers();
        detectInactiveAccounts();

        System.out.println("[Maintenance] Weekly maintenance completed at " + LocalDateTime.now());
    }

    /**
     * Archive transactions older than 1 year (mark as archived in DB, or optionally move to archive table).
     */
    private void archiveOldTransactions() {
        LocalDateTime oneYearAgo = LocalDateTime.now().minus(1, ChronoUnit.YEARS);
        List<Transaction> oldTransactions = em.createQuery(
                        "SELECT t FROM Transaction t WHERE t.timestamp < :cutoff", Transaction.class)
                .setParameter("cutoff", oneYearAgo)
                .getResultList();

        int count = 0;
        for (Transaction tx : oldTransactions) {
            // Example: Set a flag, or move to an archive table
            // tx.setArchived(true); // Uncomment if you add an 'archived' field
            em.remove(tx); // Or just delete if archiving is not required
            count++;
        }
        System.out.println("[Maintenance] Archived or deleted " + count + " transactions older than 1 year.");
    }

    /**
     * Clean up scheduled transfers that have been processed and are older than 30 days.
     */
    private void cleanUpOldScheduledTransfers() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        List<ScheduledTransfer> oldTransfers = em.createQuery(
                        "SELECT s FROM ScheduledTransfer s WHERE s.processed = true AND s.scheduledTime < :cutoff", ScheduledTransfer.class)
                .setParameter("cutoff", thirtyDaysAgo)
                .getResultList();

        int count = 0;
        for (ScheduledTransfer transfer : oldTransfers) {
            em.remove(transfer);
            count++;
        }
        System.out.println("[Maintenance] Cleaned up " + count + " processed scheduled transfers older than 30 days.");
    }

    /**
     * Detect and log inactive accounts (no transactions in over 6 months).
     */
    private void detectInactiveAccounts() {
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minus(6, ChronoUnit.MONTHS);
        List<Account> inactiveAccounts = em.createQuery(
                        "SELECT a FROM Account a WHERE a.id NOT IN (" +
                                "SELECT DISTINCT t.account.id FROM Transaction t WHERE t.timestamp > :cutoff" +
                                ")", Account.class)
                .setParameter("cutoff", sixMonthsAgo)
                .getResultList();

        if (inactiveAccounts.isEmpty()) {
            System.out.println("[Maintenance] No inactive accounts detected.");
        } else {
            System.out.println("[Maintenance] Inactive accounts (no activity in 6+ months):");
            for (Account acc : inactiveAccounts) {
                System.out.println("[Maintenance] - Account ID: " + acc.getId() + ", Number: " + acc.getAccountNumber());
                // Optionally: mark as inactive in DB, send notification, etc.
            }
        }
    }
}