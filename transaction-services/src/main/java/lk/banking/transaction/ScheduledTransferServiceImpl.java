package lk.banking.transaction;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lk.banking.core.entity.ScheduledTransfer;

import java.util.List;

@Stateless
public class ScheduledTransferServiceImpl implements ScheduledTransferService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    public ScheduledTransfer scheduleTransfer(ScheduledTransfer transfer) {
        em.persist(transfer);
        return transfer;
    }

    @Override
    public List<ScheduledTransfer> getPendingTransfers() {
        return em.createQuery("SELECT s FROM ScheduledTransfer s WHERE s.processed = false", ScheduledTransfer.class)
                .getResultList();
    }

    @Override
    public void markTransferProcessed(Long transferId) {
        ScheduledTransfer transfer = em.find(ScheduledTransfer.class, transferId);
        if (transfer != null) {
            transfer.setProcessed(true);
            em.merge(transfer);
        }
    }
}