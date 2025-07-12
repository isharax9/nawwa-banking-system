package lk.banking.transaction;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lk.banking.core.entity.ScheduledTransfer;
import lk.banking.core.exception.ScheduledTransferException; // Import your custom exception

import java.time.LocalDateTime; // For filtering by scheduled time
import java.util.List;

@Stateless
public class ScheduledTransferServiceImpl implements ScheduledTransferService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    public ScheduledTransfer scheduleTransfer(ScheduledTransfer transfer) {
        // Basic validation: ensure accounts are set before persisting if not handled by caller
        if (transfer.getFromAccount() == null || transfer.getToAccount() == null) {
            throw new ScheduledTransferException("Source and destination accounts must be provided for scheduled transfer.");
        }
        if (transfer.getAmount() == null || transfer.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new ScheduledTransferException("Scheduled transfer amount must be positive.");
        }
        if (transfer.getScheduledTime() == null) {
            throw new ScheduledTransferException("Scheduled time must be provided for scheduled transfer.");
        }

        em.persist(transfer);
        return transfer;
    }

    @Override
    public List<ScheduledTransfer> getPendingTransfers() {
        // Retrieve transfers that are not yet processed AND whose scheduled time is now or in the past
        return em.createQuery("SELECT s FROM ScheduledTransfer s WHERE s.processed = false AND s.scheduledTime <= :now ORDER BY s.scheduledTime ASC", ScheduledTransfer.class)
                .setParameter("now", LocalDateTime.now())
                .getResultList();
    }

    @Override
    public void markTransferProcessed(Long transferId) {
        ScheduledTransfer transfer = em.find(ScheduledTransfer.class, transferId);
        if (transfer == null) {
            // Throwing an exception provides clear feedback if the transfer ID is invalid
            throw new ScheduledTransferException("Scheduled transfer with ID " + transferId + " not found to mark as processed.");
        }
        // You might also want to check if it's already processed, and throw an exception or log a warning
        if (transfer.getProcessed()) {
            System.out.println("Scheduled transfer with ID " + transferId + " was already processed.");
            // Optionally: throw new ScheduledTransferException("Scheduled transfer with ID " + transferId + " is already processed.");
            return; // Or throw exception
        }
        transfer.setProcessed(true);
        // No need for em.merge(transfer) as 'transfer' is already a managed entity from em.find()
    }

    @Override
    public List<ScheduledTransfer> getAllScheduledTransfers() {
        // Option 1 (Simple, if only showing non-processed or for admin):
        // return em.createQuery("SELECT s FROM ScheduledTransfer s ORDER BY s.scheduledTime ASC", ScheduledTransfer.class)
        //          .getResultList();

        // Option 2 (If you want to fetch accounts eagerly for display in JSP):
        return em.createQuery("SELECT s FROM ScheduledTransfer s JOIN FETCH s.fromAccount fa JOIN FETCH s.toAccount ta ORDER BY s.scheduledTime ASC", ScheduledTransfer.class)
                .getResultList();
    }
}