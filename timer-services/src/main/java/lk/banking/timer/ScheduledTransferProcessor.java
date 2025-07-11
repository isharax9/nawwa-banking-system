package lk.banking.timer;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lk.banking.core.dto.TransferRequestDto; // Explicit import
import lk.banking.core.entity.ScheduledTransfer;
import lk.banking.transaction.FundTransferService; // Explicit import

import java.time.LocalDateTime;
import java.util.List;

// Recommended: Add a proper logging framework (e.g., SLF4J with Logback/Log4j2)
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Startup
public class ScheduledTransferProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTransferProcessor.class);

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Inject // EJB injection for another EJB service
    private FundTransferService fundTransferService;

    /**
     * Runs every 10 minutes to process scheduled transfers that are due.
     * The method is transactional, but individual transfer failures are caught
     * to allow the batch to continue. Failed transfers will be retried
     * until their status is updated or an administrator intervenes.
     */
    @Schedule(minute = "*/10", hour = "*", persistent = false)
    @Transactional // The outer transaction for the batch processing
    public void processScheduledTransfers() {
        LOGGER.info("Starting scheduled transfer processing job.");

        // Query for transfers that are not yet processed and whose scheduled time has arrived or passed
        List<ScheduledTransfer> transfersToProcess = em.createQuery(
                        "SELECT s FROM ScheduledTransfer s WHERE s.processed = false AND s.scheduledTime <= :now ORDER BY s.scheduledTime ASC", ScheduledTransfer.class)
                .setParameter("now", LocalDateTime.now())
                .getResultList();

        if (transfersToProcess.isEmpty()) {
            LOGGER.info("No scheduled transfers due for processing at this time.");
            return;
        }

        LOGGER.info("Found {} scheduled transfers to process.", transfersToProcess.size());

        for (ScheduledTransfer transfer : transfersToProcess) {
            try {
                // IMPORTANT: The fundTransferService.transferFunds() method is also @Transactional.
                // In an EJB context (default REQUIRED), this inner transaction will join the outer
                // transaction of the processScheduledTransfers. If fundTransferService.transferFunds()
                // throws an exception, its changes will be rolled back.
                fundTransferService.transferFunds(
                        new TransferRequestDto( // Fully qualified DTO for clarity
                                transfer.getFromAccount().getId(),
                                transfer.getToAccount().getId(),
                                transfer.getAmount()
                        )
                );

                // If transferFunds succeeds, mark the scheduled transfer as processed
                transfer.setProcessed(true);
                // No need for em.merge(transfer); as 'transfer' is already a managed entity
                LOGGER.info("Successfully processed scheduled transfer ID: {}", transfer.getId());

            } catch (Exception e) {
                // Catching generic Exception to ensure the loop continues for other transfers.
                // For a robust system, consider defining a 'status' field in ScheduledTransfer (e.g., FAILED, RETRYING)
                // and incrementing a 'retryCount' here.
                LOGGER.error("Failed to process scheduled transfer ID {}. Reason: {}", transfer.getId(), e.getMessage(), e);
                // The scheduled transfer remains `processed = false`, so it will be retried.
                // If a transfer consistently fails, it will cause repeated errors.
                // Consider a max retry count or moving to a 'failed' state after N attempts.
            }
        }
        LOGGER.info("Scheduled transfer processing job completed.");
    }
}