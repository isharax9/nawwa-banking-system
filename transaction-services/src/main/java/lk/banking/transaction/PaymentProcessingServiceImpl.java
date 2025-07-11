package lk.banking.transaction;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional; // Keep for explicit transaction demarcation
import lk.banking.core.dto.TransactionDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.enums.TransactionStatus;
import lk.banking.core.entity.enums.TransactionType;
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.InsufficientFundsException;
import lk.banking.core.exception.InvalidTransactionException; // Import for validation

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Stateless
public class PaymentProcessingServiceImpl implements PaymentProcessingService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    @Transactional
    public Transaction processPayment(TransactionDto transactionDto) {
        // 1. Validate TransactionDto input
        if (transactionDto == null || transactionDto.getAccountId() == null || transactionDto.getAmount() == null || transactionDto.getType() == null) {
            throw new InvalidTransactionException("Transaction data (account ID, amount, type) cannot be null.");
        }

        BigDecimal amount = transactionDto.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transaction amount must be positive.");
        }

        // 2. Retrieve Account
        Account account = em.find(Account.class, transactionDto.getAccountId());
        if (account == null) {
            throw new AccountNotFoundException("Account with ID " + transactionDto.getAccountId() + " not found.");
        }

        // 3. Process balance update based on transaction type
        TransactionType type = transactionDto.getType();
        BigDecimal finalAmountForRecord; // The amount to store in the transaction record

        if (type == TransactionType.WITHDRAWAL || type == TransactionType.PAYMENT) {
            if (account.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException("Insufficient funds for " + type.name().toLowerCase() + " in account " + account.getAccountNumber() + ".");
            }
            account.setBalance(account.getBalance().subtract(amount));
            finalAmountForRecord = amount.negate(); // Represent outgoing as negative
        } else if (type == TransactionType.DEPOSIT) {
            account.setBalance(account.getBalance().add(amount));
            finalAmountForRecord = amount; // Represent incoming as positive
        } else {
            // If this service is only for DEPOSIT, WITHDRAWAL, PAYMENT,
            // then other types like TRANSFER should be rejected or handled elsewhere.
            throw new InvalidTransactionException("Unsupported transaction type for payment processing: " + type.name());
        }

        // No need for em.merge(account) here if 'account' is a managed entity.

        // 4. Record the transaction
        Transaction transaction = new Transaction(
                account,
                finalAmountForRecord, // Store the signed amount
                type,
                TransactionStatus.COMPLETED,
                LocalDateTime.now(), // Use server-side timestamp for consistency
                transactionDto.getDescription()
        );
        em.persist(transaction);

        return transaction;
    }
}