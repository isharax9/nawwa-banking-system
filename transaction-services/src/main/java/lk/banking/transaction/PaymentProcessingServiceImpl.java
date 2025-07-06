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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Stateless
public class PaymentProcessingServiceImpl implements PaymentProcessingService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    @Transactional
    public Transaction processPayment(TransactionDto transactionDto) {
        Account account = em.find(Account.class, transactionDto.getAccountId());
        if (account == null) throw new AccountNotFoundException("Account not found");

        BigDecimal amount = transactionDto.getAmount();
        if (transactionDto.getType() == TransactionType.WITHDRAWAL || transactionDto.getType() == TransactionType.PAYMENT) {
            if (account.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException("Insufficient funds for payment");
            }
            account.setBalance(account.getBalance().subtract(amount));
        } else if (transactionDto.getType() == TransactionType.DEPOSIT) {
            account.setBalance(account.getBalance().add(amount));
        }
        em.merge(account);

        Transaction transaction = new Transaction(
                account,
                transactionDto.getType() == TransactionType.DEPOSIT ? amount : amount.negate(),
                transactionDto.getType(),
                TransactionStatus.COMPLETED,
                LocalDateTime.now(),
                transactionDto.getDescription()
        );
        em.persist(transaction);

        return transaction;
    }
}