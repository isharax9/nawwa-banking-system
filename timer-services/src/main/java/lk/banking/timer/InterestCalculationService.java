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
import java.math.MathContext;   // Import for MathContext
import java.math.RoundingMode; // Import for RoundingMode
import java.util.List;

// Recommended: Add a proper logging framework (e.g., SLF4J with Logback/Log4j2)
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

@Singleton
@Startup
public class InterestCalculationService {

    // private static final Logger LOGGER = LoggerFactory.getLogger(InterestCalculationService.class); // For proper logging

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    // Define MathContext for consistent financial calculations
    private static final MathContext MATH_CONTEXT = new MathContext(10, RoundingMode.HALF_UP); // 10 precision, HALF_UP rounding
    // Define the daily interest rate
    private static final BigDecimal DAILY_INTEREST_RATE = BigDecimal.valueOf(0.002); // 0.2% daily (adjust as per actual banking rules)

    /**
     * Runs daily at 2am to calculate and apply interest to savings accounts.
     */
    @Schedule(hour = "2", minute = "0", second = "0", persistent = false)
    @Transactional // Ensures atomicity for balance updates
    public void calculateInterest() {
        System.out.println("Running daily interest calculation...");
        // LOGGER.info("Running daily interest calculation..."); // Use this with proper logging

        List<Account> savingsAccounts = em.createQuery(
                        "SELECT a FROM Account a WHERE a.type = :type AND a.isActive = TRUE", Account.class) // Consider only active accounts
                .setParameter("type", AccountType.SAVINGS)
                .getResultList();

        if (savingsAccounts.isEmpty()) {
            System.out.println("No active savings accounts found for interest calculation.");
            // LOGGER.info("No active savings accounts found for interest calculation.");
            return;
        }

        System.out.println("Processing interest for " + savingsAccounts.size() + " savings accounts.");
        // LOGGER.info("Processing interest for {} savings accounts.", savingsAccounts.size());

        for (Account account : savingsAccounts) {
            BigDecimal currentBalance = account.getBalance();
            BigDecimal interest = currentBalance.multiply(DAILY_INTEREST_RATE, MATH_CONTEXT);

            // Add interest to the balance
            account.setBalance(currentBalance.add(interest));

            // Optional: Create a transaction record for the interest application for audit purposes
            // This would link to your banking.services.TransactionServices or create directly
            /*
            Transaction interestTransaction = new Transaction(
                    account,
                    interest,
                    TransactionType.DEPOSIT, // Or a specific INTEREST_EARNED type
                    TransactionStatus.COMPLETED,
                    LocalDateTime.now(),
                    "Daily interest applied"
            );
            em.persist(interestTransaction);
            */

            // No need for em.merge(account) as 'account' is a managed entity.
            System.out.println("Account " + account.getAccountNumber() + ": Balance updated from " + currentBalance + " to " + account.getBalance() + " (Interest: " + interest + ")");
            // LOGGER.debug("Account {}: Balance updated from {} to {} (Interest: {})",
            //              account.getAccountNumber(), currentBalance, account.getBalance(), interest);
        }

        System.out.println("Daily interest calculation completed.");
        // LOGGER.info("Daily interest calculation completed.");
    }
}