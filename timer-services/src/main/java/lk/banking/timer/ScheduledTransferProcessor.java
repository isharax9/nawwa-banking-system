package lk.banking.timer;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lk.banking.core.entity.ScheduledTransfer;
import lk.banking.transaction.FundTransferService;

import java.time.LocalDateTime;
import java.util.List;

@Singleton
@Startup
public class ScheduledTransferProcessor {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Inject
    private FundTransferService fundTransferService;

    // Run every 10 minutes
    @Schedule(minute = "*/10", hour = "*", persistent = false)
    @Transactional
    public void processScheduledTransfers() {
        List<ScheduledTransfer> transfers = em.createQuery(
                        "SELECT s FROM ScheduledTransfer s WHERE s.processed = false AND s.scheduledTime <= :now", ScheduledTransfer.class)
                .setParameter("now", LocalDateTime.now())
                .getResultList();

        for (ScheduledTransfer transfer : transfers) {
            try {
                fundTransferService.transferFunds(
                        new lk.banking.core.dto.TransferRequestDto(
                                transfer.getFromAccount().getId(),
                                transfer.getToAccount().getId(),
                                transfer.getAmount()
                        )
                );
                transfer.setProcessed(true);
                em.merge(transfer);
                System.out.println("Processed scheduled transfer: " + transfer.getId());
            } catch (Exception e) {
                System.err.println("Failed to process scheduled transfer " + transfer.getId() + ": " + e.getMessage());
            }
        }
    }
}