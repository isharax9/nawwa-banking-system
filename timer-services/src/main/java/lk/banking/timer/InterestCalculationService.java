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

    // Use a MathContext that implies a desired scale for final results, or apply setScale explicitly.
    // For currency, it's typical to use a precision that allows for calculations
    // then round to the currency's scale at the end.
    private static final MathContext CALCULATION_MATH_CONTEXT = new MathContext(20, RoundingMode.HALF_UP); // High precision for intermediate steps
    private static final int CURRENCY_SCALE = 2; // For currency, e.g., cents
    private static final BigDecimal DAILY_INTEREST_RATE = BigDecimal.valueOf(0.01); // Example: 1% daily interest rate

    /**
     * Runs daily at 2am to calculate and apply interest to saving accounts.
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

            LocalDateTime lastApplied = account.getLastInterestAppliedDate();
            if (lastApplied == null) {
                lastApplied = account.getCreatedAt();
                // If created less than a full day ago based on today's date, skip.
                if (ChronoUnit.DAYS.between(lastApplied.toLocalDate(), LocalDateTime.now().toLocalDate()) <= 0) {
                    LOGGER.fine("Account " + account.getAccountNumber() + " created very recently (no full days passed). Skipping automated interest calculation for now.");
                    continue;
                }
            } else {
                // If the last applied date is today, no full day has passed since then.
                if (lastApplied.toLocalDate().isEqual(LocalDateTime.now().toLocalDate())) {
                    LOGGER.fine("Account " + account.getAccountNumber() + ": Interest already applied today. Skipping.");
                    continue;
                }
            }


            // Number of days since last interest application (or start of period)
            long days = ChronoUnit.DAYS.between(lastApplied.toLocalDate(), LocalDateTime.now().toLocalDate());

            if (days <= 0) { // This check becomes redundant with the above, but harmless.
                LOGGER.fine("Account " + account.getAccountNumber() + ": No full days passed since last automated interest application. Skipping.");
                continue;
            }

            // Interest calculation (compounded daily based on this logic)
            BigDecimal interest = BigDecimal.ZERO;
            BigDecimal tempBalance = currentBalance;
            for (int i = 0; i < days; i++) {
                // Perform daily interest calculation with high precision
                BigDecimal dailyInterest = tempBalance.multiply(DAILY_INTEREST_RATE, CALCULATION_MATH_CONTEXT);
                interest = interest.add(dailyInterest);
                tempBalance = tempBalance.add(dailyInterest); // Compound daily if that's the rule
            }

            // IMPORTANT: Round the final calculated interest and the new balance to currency scale
            interest = interest.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
            BigDecimal newBalance = currentBalance.add(interest).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);


            // Add interest to the balance
            account.setBalance(newBalance); // Set the rounded new balance
            account.setLastInterestAppliedDate(LocalDateTime.now()); // Update last applied date

            // Create a transaction record for the interest application for audit purposes
            Transaction interestTransaction = new Transaction(
                    account,
                    interest, // Use the rounded interest amount for the transaction record
                    TransactionType.DEPOSIT,
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