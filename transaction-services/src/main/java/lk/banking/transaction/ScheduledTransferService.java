package lk.banking.transaction;

import jakarta.ejb.Local;
import lk.banking.core.entity.ScheduledTransfer;

import java.util.List;

@Local
public interface ScheduledTransferService {
    ScheduledTransfer scheduleTransfer(ScheduledTransfer transfer);
    List<ScheduledTransfer> getPendingTransfers();
    void markTransferProcessed(Long transferId);

    List<ScheduledTransfer> getAllScheduledTransfers(); // For the quick filter workaround
// OR BETTER:
// List<ScheduledTransfer> getScheduledTransfersByCustomerId(Long customerId);
// List<ScheduledTransfer> getScheduledTransfersByUserId(Long userId);
}