package lk.banking.transaction;

import jakarta.ejb.Local;
import lk.banking.core.entity.ScheduledTransfer;

import java.util.List;

@Local
public interface ScheduledTransferService {
    /**
     * Schedules a new fund transfer.
     * @param transfer The ScheduledTransfer entity to persist.
     * @return The persisted ScheduledTransfer entity.
     * @throws lk.banking.core.exception.ScheduledTransferException if scheduling fails (e.g., invalid data).
     */
    ScheduledTransfer scheduleTransfer(ScheduledTransfer transfer);

    /**
     * Retrieves all scheduled transfers that are pending and due for processing.
     * @return A list of pending ScheduledTransfer entities.
     */
    List<ScheduledTransfer> getPendingTransfers();

    /**
     * Retrieves all scheduled transfers in the system.
     * This method is primarily for administrative purposes or for filtering in the web layer.
     * It should typically eagerly fetch associated Accounts to avoid LazyInitializationException.
     * @return A list of all ScheduledTransfer entities.
     */
    List<ScheduledTransfer> getAllScheduledTransfers(); // NEW METHOD

    /**
     * Marks a scheduled transfer as processed.
     * @param transferId The ID of the scheduled transfer to mark.
     * @throws lk.banking.core.exception.ScheduledTransferException if the transfer is not found or already processed.
     */
    void markTransferProcessed(Long transferId);

    // Highly Recommended: For customer-specific views, add a method like this
    // List<ScheduledTransfer> getScheduledTransfersByCustomerId(Long customerId);
}