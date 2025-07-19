package lk.banking.services;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException; // For specific query results
import jakarta.persistence.PersistenceContext;
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

        LocalDateTime transactionTimestamp = LocalDateTime.now();

        Transaction transaction = new Transaction(
                account,
                transactionDto.getAmount(),
                transactionDto.getType(),
                TransactionStatus.PENDING,
                transactionTimestamp,
                transactionDto.getDescription()
        );
        em.persist(transaction);
        return transaction;
    }

    @Override
    public Transaction getTransactionById(Long id) {
        Transaction transaction = em.find(Transaction.class, id);
        if (transaction == null) {
            throw new InvalidTransactionException("Transaction with ID " + id + " not found.");
        }
        return transaction;
    }

    @Override
    public List<Transaction> getTransactionsByAccount(Long accountId) {
        return em.createQuery(
                        "SELECT t FROM Transaction t WHERE t.account.id = :aid ORDER BY t.timestamp DESC", Transaction.class)
                .setParameter("aid", accountId)
                .getResultList();
    }

    @Override
    public List<Transaction> getAllTransactions() {
        return em.createQuery("SELECT t FROM Transaction t ORDER BY t.timestamp DESC", Transaction.class).getResultList();
    }

    @Override
    public boolean transferFunds(Long performingUserId, String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be positive.");
        }
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new InvalidTransactionException("Cannot transfer funds to the same account.");
        }

        Account fromAccount;
        Account toAccount;

        try {
            fromAccount = getAccountByNumber(fromAccountNumber);
            toAccount = getAccountByNumber(toAccountNumber);
        } catch (AccountNotFoundException e) {
            throw new AccountNotFoundException("One of the accounts involved in transfer was not found: " + e.getMessage());
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Account " + fromAccountNumber + " has insufficient funds for transfer.");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        em.merge(fromAccount);

        Transaction debitTransaction = new Transaction(
                fromAccount,
                amount,
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                LocalDateTime.now(),
                "Funds transfer out to account " + toAccountNumber + " (initiated by user ID: " + performingUserId + ")"
        );
        em.persist(debitTransaction);

        toAccount.setBalance(toAccount.getBalance().add(amount));
        em.merge(toAccount);

        Transaction creditTransaction = new Transaction(
                toAccount,
                amount,
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                LocalDateTime.now(),
                "Funds transfer in from account " + fromAccountNumber + " (initiated by user ID: " + performingUserId + ")"
        );
        em.persist(creditTransaction);

        return true;
    }

    @Override
    public List<Transaction> getTransactionsByAccountAndDateRange(Long accountId, LocalDateTime from, LocalDateTime to, boolean includeArchived) {
        return List.of();
    }

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

    /**
     * Original method, now delegates to the new limited method with a safe default for the dashboard.
     */
    @Override
    public List<Transaction> getTransactionsByUser(Long userId) {
        // A safe limit for the dashboard view to prevent OutOfMemoryError
        return getTransactionsByUser(userId, 100);
    }

    /**
     * NEW METHOD: Fetches transactions for a user with a specified limit.
     * This is the method the PDF download servlet will call.
     */
    @Override
    public List<Transaction> getTransactionsByUser(Long userId, int maxResults) {
        User user = em.find(User.class, userId);
        if (user == null) {
            throw new UserNotFoundException("User with ID " + userId + " not found.");
        }

        Customer customer;
        try {
            customer = em.createQuery(
                            "SELECT c FROM Customer c WHERE c.email = :email", Customer.class)
                    .setParameter("email", user.getEmail())
                    .getSingleResult();
        } catch (NoResultException e) {
            return List.of();
        }

        List<Account> accounts = em.createQuery(
                        "SELECT a FROM Account a WHERE a.customer.id = :customerId", Account.class)
                .setParameter("customerId", customer.getId())
                .getResultList();

        if (accounts.isEmpty()) {
            return List.of();
        }

        // The query now includes setMaxResults to limit the number of transactions returned
        return em.createQuery(
                        "SELECT t FROM Transaction t JOIN FETCH t.account a WHERE t.account.id IN :accountIds ORDER BY t.timestamp DESC", Transaction.class)
                .setParameter("accountIds", accounts.stream().map(Account::getId).collect(Collectors.toList()))
                .setMaxResults(maxResults) // <-- This line limits the results
                .getResultList();
    }
}