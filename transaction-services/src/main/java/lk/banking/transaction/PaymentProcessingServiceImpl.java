package lk.banking.transaction;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lk.banking.core.dto.TransactionDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.enums.TransactionStatus;
import lk.banking.core.entity.enums.TransactionType;
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.InsufficientFundsException;
import lk.banking.core.exception.InvalidTransactionException;
import lk.banking.core.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.logging.Logger;

@Stateless
public class PaymentProcessingServiceImpl implements PaymentProcessingService {

    private static final Logger LOGGER = Logger.getLogger(PaymentProcessingServiceImpl.class.getName());

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    @Transactional
    public Transaction processPayment(TransactionDto transactionDto) {
        LOGGER.info("PaymentProcessingService: Processing payment for account ID: " + transactionDto.getAccountId() + " type: " + transactionDto.getType() + " amount: " + transactionDto.getAmount());

        if (transactionDto == null || transactionDto.getAccountId() == null || transactionDto.getAmount() == null || transactionDto.getType() == null) {
            throw new InvalidTransactionException("Transaction data (account ID, amount, type) cannot be null.");
        }

        BigDecimal amount = transactionDto.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transaction amount must be positive.");
        }

        Account account = em.find(Account.class, transactionDto.getAccountId());
        if (account == null) {
            LOGGER.warning("PaymentProcessingService: Account with ID " + transactionDto.getAccountId() + " not found.");
            throw new AccountNotFoundException("Account with ID " + transactionDto.getAccountId() + " not found.");
        }

        // **** CRUCIAL CHANGE: Check if account is active ****
        if (!account.getIsActive()) {
            LOGGER.warning("PaymentProcessingService: Transaction denied for inactive account: " + account.getAccountNumber() + " Type: " + transactionDto.getType().name());
            throw new InvalidTransactionException("Transaction denied: Account " + account.getAccountNumber() + " is inactive.");
        }

        TransactionType type = transactionDto.getType();
        BigDecimal finalAmountForRecord;

        if (type == TransactionType.WITHDRAWAL || type == TransactionType.PAYMENT) {
            if (account.getBalance().compareTo(amount) < 0) {
                LOGGER.warning("PaymentProcessingService: Insufficient funds for " + type.name().toLowerCase() + " in account " + account.getAccountNumber() + ". Balance: " + account.getBalance() + ", Attempted: " + amount);
                throw new InsufficientFundsException("Insufficient funds for " + type.name().toLowerCase() + " in account " + account.getAccountNumber() + ".");
            }
            account.setBalance(account.getBalance().subtract(amount));
            finalAmountForRecord = amount.negate();
        } else if (type == TransactionType.DEPOSIT) {
            account.setBalance(account.getBalance().add(amount));
            finalAmountForRecord = amount;
        } else {
            LOGGER.warning("PaymentProcessingService: Unsupported transaction type for payment processing: " + type.name());
            throw new InvalidTransactionException("Unsupported transaction type for payment processing: " + type.name());
        }

        Transaction transaction = new Transaction(
                account,
                finalAmountForRecord,
                type,
                TransactionStatus.COMPLETED,
                LocalDateTime.now(),
                transactionDto.getDescription()
        );
        em.persist(transaction);

        LOGGER.info("PaymentProcessingService: " + type.name() + " of " + amount + " successful for account " + account.getAccountNumber());
        return transaction;
    }
}