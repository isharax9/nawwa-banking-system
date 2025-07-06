package lk.banking.timer;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Customer;
import lk.banking.core.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Singleton
@Startup
public class StatementGenerationService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    /**
     * Generate monthly statements at midnight on the 1st of every month.
     */
    @Schedule(dayOfMonth = "1", hour = "0", minute = "0", second = "0", persistent = false)
    @Transactional
    public void generateStatements() {
        System.out.println("[Statement] Monthly statement generation started for all customers...");

        LocalDate firstDayOfThisMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate firstDayOfLastMonth = firstDayOfThisMonth.minusMonths(1);

        LocalDateTime from = firstDayOfLastMonth.atStartOfDay();
        LocalDateTime to = firstDayOfThisMonth.atStartOfDay();

        // Get all customers
        List<Customer> customers = em.createQuery("SELECT c FROM Customer c", Customer.class)
                .getResultList();

        for (Customer customer : customers) {
            List<Account> accounts = em.createQuery(
                            "SELECT a FROM Account a WHERE a.customer.id = :cid", Account.class)
                    .setParameter("cid", customer.getId())
                    .getResultList();

            System.out.println("----- Monthly Statement for Customer: " + customer.getName() + " (" + customer.getEmail() + ") -----");
            for (Account account : accounts) {
                // Fetch transactions within the last month
                List<Transaction> transactions = em.createQuery(
                                "SELECT t FROM Transaction t WHERE t.account.id = :aid AND t.timestamp >= :from AND t.timestamp < :to ORDER BY t.timestamp ASC", Transaction.class)
                        .setParameter("aid", account.getId())
                        .setParameter("from", from)
                        .setParameter("to", to)
                        .getResultList();

                BigDecimal openingBalance = getOpeningBalance(account, from);
                BigDecimal closingBalance = account.getBalance();

                System.out.println("Account Number: " + account.getAccountNumber());
                System.out.println("Account Type: " + account.getType());
                System.out.println("Opening Balance: " + openingBalance);
                System.out.println("Transactions:");
                if (transactions.isEmpty()) {
                    System.out.println("  No transactions for this period.");
                } else {
                    for (Transaction tx : transactions) {
                        System.out.printf("  [%s] %s %s (%s) | %s\n",
                                tx.getTimestamp(),
                                tx.getType(),
                                tx.getAmount(),
                                tx.getStatus(),
                                tx.getDescription() != null ? tx.getDescription() : "");
                    }
                }
                System.out.println("Closing Balance: " + closingBalance);
                System.out.println("---------------------------------------------------");
            }
        }

        System.out.println("[Statement] Monthly statement generation completed.");
    }

    /**
     * Calculate the account's opening balance at the beginning of the period.
     */
    private BigDecimal getOpeningBalance(Account account, LocalDateTime from) {
        // Start with current balance and roll back transactions in the period
        BigDecimal balance = account.getBalance();

        List<Transaction> transactions = em.createQuery(
                        "SELECT t FROM Transaction t WHERE t.account.id = :aid AND t.timestamp >= :from", Transaction.class)
                .setParameter("aid", account.getId())
                .setParameter("from", from)
                .getResultList();

        for (Transaction tx : transactions) {
            // Reverse the transaction to get the balance before this transaction
            balance = balance.subtract(tx.getAmount());
        }

        return balance;
    }
}