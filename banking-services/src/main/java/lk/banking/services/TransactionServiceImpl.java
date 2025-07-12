package lk.banking.services;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException; // For specific query results
import lk.banking.core.dto.TransactionDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Customer; // Needed for getTransactionsByUser
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.User; // Needed for getTransactionsByUser
import lk.banking.core.entity.enums.TransactionStatus;
import lk.banking.core.entity.enums.TransactionType;
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.InsufficientFundsException; // Import
import lk.banking.core.exception.InvalidTransactionException; // Import
import lk.banking.core.exception.UserNotFoundException; // Import
import lk.banking.services.interceptor.AuditInterceptor;
import lk.banking.services.interceptor.PerformanceMonitorInterceptor;
import lk.banking.services.interceptor.SecurityInterceptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors; // For getTransactionsByUser

@Stateless
@Interceptors({AuditInterceptor.class, PerformanceMonitorInterceptor.class, SecurityInterceptor.class})
public class TransactionServiceImpl implements TransactionServices {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    public Transaction createTransaction(TransactionDto transactionDto) {
        Account account = em.find(Account.class, transactionDto.getAccountId());
        if (account == null) {
            throw new AccountNotFoundException("Account with ID " + transactionDto.getAccountId() + " not found.");
        }

        // For auditing and consistency, it's often better to set the timestamp on the server
        LocalDateTime transactionTimestamp = LocalDateTime.now();

        Transaction transaction = new Transaction(
                account,
                transactionDto.getAmount(),
                transactionDto.getType(),
                // Initial status is PENDING. It should be updated to COMPLETED/FAILED
                // after the actual balance manipulation logic (e.g., in a transferFunds method).
                TransactionStatus.PENDING,
                transactionTimestamp,
                transactionDto.getDescription()
        );
        em.persist(transaction);

        // IMPORTANT: For true deposits/withdrawals, balance updates would occur here.
        // For example, if type is DEPOSIT: account.setBalance(account.getBalance().add(transaction.getAmount()));
        // Then, set status to COMPLETED: transaction.setStatus(TransactionStatus.COMPLETED);
        // And persist/merge.
        // This method currently only records the transaction, not executes fund movement.

        return transaction;
    }

    @Override
    public Transaction getTransactionById(Long id) {
        Transaction transaction = em.find(Transaction.class, id);
        if (transaction == null) {
            // Consider creating a specific TransactionNotFoundException if needed
            throw new InvalidTransactionException("Transaction with ID " + id + " not found.");
        }
        return transaction;
    }

    @Override
    public List<Transaction> getTransactionsByAccount(Long accountId) {
        // You might want to throw AccountNotFoundException if accountId does not exist
        // Or handle an empty list as a valid scenario (no transactions for that account)
        return em.createQuery(
                        "SELECT t FROM Transaction t WHERE t.account.id = :aid ORDER BY t.timestamp DESC", Transaction.class) // Added ORDER BY
                .setParameter("aid", accountId)
                .getResultList();
    }

    @Override
    public List<Transaction> getAllTransactions() {
        return em.createQuery("SELECT t FROM Transaction t ORDER BY t.timestamp DESC", Transaction.class).getResultList(); // Added ORDER BY
    }

    @Override
    public boolean transferFunds(Long performingUserId, String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        // Return type 'boolean' is generally discouraged. Prefer throwing exceptions for failures,
        // and returning a meaningful object (e.g., the created transaction) on success.
        // For now, we'll implement it to return boolean as per your interface.

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be positive.");
        }
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new InvalidTransactionException("Cannot transfer funds to the same account.");
        }

        Account fromAccount;
        Account toAccount;

        try {
            fromAccount = getAccountByNumber(fromAccountNumber); // Reuse existing method
            toAccount = getAccountByNumber(toAccountNumber); // Reuse existing method
        } catch (AccountNotFoundException e) {
            throw new AccountNotFoundException("One of the accounts involved in transfer was not found: " + e.getMessage());
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Account " + fromAccountNumber + " has insufficient funds for transfer.");
        }

        // Perform the debit from 'fromAccount'
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        em.merge(fromAccount); // Update the account balance in DB

        // Create a debit transaction record
        Transaction debitTransaction = new Transaction(
                fromAccount,
                amount,
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED, // Set to completed after balance update
                LocalDateTime.now(),
                "Funds transfer out to account " + toAccountNumber + " (initiated by user ID: " + performingUserId + ")"
        );
        em.persist(debitTransaction);

        // Perform the credit to 'toAccount'
        toAccount.setBalance(toAccount.getBalance().add(amount));
        em.merge(toAccount); // Update the account balance in DB

        // Create a credit transaction record
        Transaction creditTransaction = new Transaction(
                toAccount,
                amount,
                TransactionType.TRANSFER, // Or consider a specific CREDIT_IN/TRANSFER_IN type if needed
                TransactionStatus.COMPLETED, // Set to completed after balance update
                LocalDateTime.now(),
                "Funds transfer in from account " + fromAccountNumber + " (initiated by user ID: " + performingUserId + ")"
        );
        em.persist(creditTransaction);

        // If all operations succeed, the transaction (EJB's managed transaction) commits.
        // If any error occurs, the whole operation (debit, credit, transaction records) rolls back.
        return true; // Indicate success as per interface contract
    }

    @Override
    public List<Transaction> getTransactionsByAccountAndDateRange(Long accountId, LocalDateTime from, LocalDateTime to, boolean includeArchived) {
        return List.of();
    }


    // Helper method to get account by number, reusing logic from AccountService
    // In a real app, you might inject AccountService here.
    private Account getAccountByNumber(String accountNumber) {
        try {
            return em.createQuery(
                            "SELECT a FROM Account a WHERE a.accountNumber = :num", Account.class)
                    .setParameter("num", accountNumber)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new AccountNotFoundException("Account with number " + accountNumber + " not found.");
        }
    }


    @Override
    public List<Transaction> getTransactionsByUser(Long userId) {
        User user = em.find(User.class, userId);
        if (user == null) {
            throw new UserNotFoundException("User with ID " + userId + " not found.");
        }

        // IMPORTANT: As discussed previously, your User entity does not directly link to Customer.
        // This implementation assumes a Customer exists with the same email as the User.
        // For a more robust solution, add a @OneToOne relationship from User to Customer.
        Customer customer;
        try {
            customer = em.createQuery(
                            "SELECT c FROM Customer c WHERE c.email = :email", Customer.class)
                    .setParameter("email", user.getEmail())
                    .getSingleResult();
        } catch (NoResultException e) {
            // No customer found linked to this user's email, so no accounts/transactions
            return List.of();
        }

        // Get all accounts belonging to this customer
        List<Account> accounts = em.createQuery(
                        "SELECT a FROM Account a WHERE a.customer.id = :customerId", Account.class)
                .setParameter("customerId", customer.getId())
                .getResultList();

        if (accounts.isEmpty()) {
            return List.of(); // No accounts for this customer, so no transactions
        }

        // Get transactions for all these accounts
        // MODIFY THIS QUERY: JOIN FETCH t.account
        return em.createQuery(
                        "SELECT t FROM Transaction t JOIN FETCH t.account a WHERE t.account.id IN :accountIds ORDER BY t.timestamp DESC", Transaction.class)
                .setParameter("accountIds", accounts.stream().map(Account::getId).collect(Collectors.toList()))
                .getResultList();
    }
}