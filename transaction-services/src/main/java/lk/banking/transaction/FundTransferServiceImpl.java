package lk.banking.transaction;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException; // For fetching accounts by number if needed
import jakarta.transaction.Transactional; // Keep for explicit transaction demarcation
import lk.banking.core.dto.TransferRequestDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.enums.TransactionStatus;
import lk.banking.core.entity.enums.TransactionType;
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.InsufficientFundsException;
import lk.banking.core.exception.InvalidTransactionException; // Import this exception

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Stateless
public class FundTransferServiceImpl implements FundTransferService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    @Transactional // Ensures atomicity: all operations succeed or all roll back
    public Transaction transferFunds(TransferRequestDto requestDto) {
        // 1. Basic validation for the transfer request
        if (requestDto == null || requestDto.getAmount() == null) {
            throw new InvalidTransactionException("Transfer request or amount cannot be null.");
        }

        BigDecimal amount = requestDto.getAmount();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be positive.");
        }

        if (requestDto.getFromAccountId().equals(requestDto.getToAccountId())) {
            throw new InvalidTransactionException("Cannot transfer funds to the same account.");
        }

        // 2. Retrieve accounts
        // Using em.find() for direct ID lookup, which is generally faster for primary keys.
        Account fromAccount = em.find(Account.class, requestDto.getFromAccountId());
        Account toAccount = em.find(Account.class, requestDto.getToAccountId());

        if (fromAccount == null) {
            throw new AccountNotFoundException("Source account with ID " + requestDto.getFromAccountId() + " not found.");
        }
        if (toAccount == null) {
            throw new AccountNotFoundException("Destination account with ID " + requestDto.getToAccountId() + " not found.");
        }

        // 3. Check for sufficient funds
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds in source account " + fromAccount.getAccountNumber() + ".");
        }

        // 4. Update account balances (these are managed entities, changes will be flushed)
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        // No need for em.merge(fromAccount) and em.merge(toAccount) here,
        // as they are managed entities from em.find(). Their state changes
        // will be automatically persisted at the end of the transaction.

        // 5. Record debit transaction
        Transaction debitTransaction = new Transaction(
                fromAccount,
                amount.negate(), // Debit represented as negative
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                LocalDateTime.now(),
                String.format("Transfer out to account %s (ref: %s)",
                        toAccount.getAccountNumber(), requestDto.getToAccountId()) // Added ref for clarity
        );
        em.persist(debitTransaction);

        // 6. Record credit transaction
        Transaction creditTransaction = new Transaction(
                toAccount,
                amount,
                TransactionType.TRANSFER, // Could be specific like TransactionType.RECEIVE or TransactionType.CREDIT
                TransactionStatus.COMPLETED,
                LocalDateTime.now(),
                String.format("Transfer in from account %s (ref: %s)",
                        fromAccount.getAccountNumber(), requestDto.getFromAccountId()) // Added ref for clarity
        );
        em.persist(creditTransaction);

        // IMPORTANT: Consider removing or refactoring the 'transferFunds' method
        // from lk.banking.services.TransactionServiceImpl to avoid duplicate logic
        // and ensure this is the single authoritative source for transfers.

        // Returning the debit transaction as the primary record for the transfer
        return debitTransaction;
    }
}