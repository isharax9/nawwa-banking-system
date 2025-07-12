package lk.banking.timer;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lk.banking.core.dto.TransferRequestDto;
import lk.banking.core.entity.ScheduledTransfer;
import lk.banking.transaction.FundTransferService;

import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Startup
public class ScheduledTransferProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTransferProcessor.class);

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Inject
    private FundTransferService fundTransferService;

    // TEMPORARY CHANGE FOR TESTING: Run every minute
    @Schedule(minute = "*/1", hour = "*", persistent = false) // Runs every minute
    // @Schedule(second = "*/30", minute = "*", hour = "*", persistent = false) // Or even every 30 seconds
    // ORIGINAL: @Schedule(minute = "*/10", hour = "*", persistent = false) // Revert to this for production
    @Transactional
    public void processScheduledTransfers() {
        LOGGER.info("Starting scheduled transfer processing job.");

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
                fundTransferService.transferFunds(
                        new TransferRequestDto(
                                transfer.getFromAccount().getId(),
                                transfer.getToAccount().getId(),
                                transfer.getAmount()
                        )
                );

                transfer.setProcessed(true);
                LOGGER.info("Successfully processed scheduled transfer ID: {}", transfer.getId());

            } catch (Exception e) {
                LOGGER.error("Failed to process scheduled transfer ID {}. Reason: {}", transfer.getId(), e.getMessage(), e);
            }
        }
        LOGGER.info("Scheduled transfer processing job completed.");
    }
}