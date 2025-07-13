package lk.banking.timer;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Transaction; // For audit record
import lk.banking.core.entity.enums.AccountType;
import lk.banking.core.entity.enums.TransactionStatus; // For audit record
import lk.banking.core.entity.enums.TransactionType; // For audit record

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit; // For date difference
import java.util.List;
import java.util.logging.Logger;

@Singleton
@Startup
public class InterestCalculationService {

    private static final Logger LOGGER = Logger.getLogger(InterestCalculationService.class.getName());

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    private static final MathContext MATH_CONTEXT = new MathContext(10, RoundingMode.HALF_UP);
    private static final BigDecimal DAILY_INTEREST_RATE = BigDecimal.valueOf(0.00002); // Adjusted for more realistic daily rate (0.002% daily means ~0.73% annually)
    // You can adjust this to a more realistic value. A 0.2% daily rate as before is ~73% annually.

    /**
     * Runs daily at 2am to calculate and apply interest to savings accounts.
     */
    @Schedule(hour = "2", minute = "0", second = "0", persistent = false)
    @Transactional // Ensures atomicity for balance updates
    public void calculateInterest() {
        LOGGER.info("Running daily automated interest calculation...");

        List<Account> savingsAccounts = em.createQuery(
                        "SELECT a FROM Account a WHERE a.type = :type AND a.isActive = TRUE", Account.class)
                .setParameter("type", AccountType.SAVINGS)
                .getResultList();

        if (savingsAccounts.isEmpty()) {
            LOGGER.info("No active savings accounts found for automated interest calculation.");
            return;
        }

        LOGGER.info("Processing automated interest for " + savingsAccounts.size() + " savings accounts.");

        for (Account account : savingsAccounts) {
            BigDecimal currentBalance = account.getBalance();
            // Calculate interest only for positive balances, or as per bank rules
            if (currentBalance.compareTo(BigDecimal.ZERO) <= 0) {
                LOGGER.fine("Account " + account.getAccountNumber() + " has non-positive balance. Skipping automated interest calculation.");
                continue;
            }

            // Calculate daily interest for the last full day since last application
            LocalDateTime lastApplied = account.getLastInterestAppliedDate();
            if (lastApplied == null) {
                // If never applied, assume it's new and apply from creation date or current date
                lastApplied = account.getCreatedAt();
                if (lastApplied.isAfter(LocalDateTime.now().minusDays(1))) { // If created less than a day ago
                    LOGGER.fine("Account " + account.getAccountNumber() + " created recently. Skipping automated interest calculation for now.");
                    // Skip if too new for a full day's interest.
                    continue;
                }
            }

            // Number of days since last interest application (or start of period)
            long days = ChronoUnit.DAYS.between(lastApplied.toLocalDate(), LocalDateTime.now().toLocalDate());

            if (days <= 0) {
                LOGGER.fine("Account " + account.getAccountNumber() + ": No full days passed since last automated interest application. Skipping.");
                continue; // Only apply for full days that have passed
            }

            // Interest calculation (compounded daily based on this logic)
            BigDecimal interest = BigDecimal.ZERO;
            BigDecimal tempBalance = currentBalance;
            for (int i = 0; i < days; i++) {
                BigDecimal dailyInterest = tempBalance.multiply(DAILY_INTEREST_RATE, MATH_CONTEXT);
                interest = interest.add(dailyInterest);
                tempBalance = tempBalance.add(dailyInterest); // Compound daily if that's the rule
            }

            // Add interest to the balance
            account.setBalance(currentBalance.add(interest));
            account.setLastInterestAppliedDate(LocalDateTime.now()); // Update last applied date

            // Create a transaction record for the interest application for audit purposes
            Transaction interestTransaction = new Transaction(
                    account,
                    interest,
                    TransactionType.DEPOSIT, // Or a specific INTEREST_EARNED type if you add it
                    TransactionStatus.COMPLETED,
                    LocalDateTime.now(),
                    "Automated daily interest applied for " + days + " days"
            );
            em.persist(interestTransaction);

            LOGGER.info("Account " + account.getAccountNumber() + ": Balance updated from " + currentBalance + " to " + account.getBalance() + " (Automated Interest: " + interest + " for " + days + " days).");
        }

        LOGGER.info("Daily automated interest calculation completed.");
    }
}