package lk.banking.timer;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.enums.AccountType;

import java.math.BigDecimal;
import java.util.List;

@Singleton
@Startup
public class InterestCalculationService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    // Run daily at 2am
    @Schedule(hour = "2", minute = "0", second = "0", persistent = false)
    @Transactional
    public void calculateInterest() {
        System.out.println("Running daily interest calculation...");
        List<Account> savingsAccounts = em.createQuery(
                        "SELECT a FROM Account a WHERE a.type = :type", Account.class)
                .setParameter("type", AccountType.SAVINGS)
                .getResultList();

        for (Account account : savingsAccounts) {
            BigDecimal interest = account.getBalance().multiply(BigDecimal.valueOf(0.002)); // 0.2% daily
            account.setBalance(account.getBalance().add(interest));
            em.merge(account);
        }
    }
}