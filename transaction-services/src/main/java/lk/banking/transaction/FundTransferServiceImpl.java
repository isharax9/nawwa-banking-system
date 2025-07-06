package lk.banking.transaction;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lk.banking.core.dto.TransferRequestDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.enums.TransactionStatus;
import lk.banking.core.entity.enums.TransactionType;
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.InsufficientFundsException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Stateless
public class FundTransferServiceImpl implements FundTransferService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    @Transactional
    public Transaction transferFunds(TransferRequestDto requestDto) {
        Account fromAccount = em.find(Account.class, requestDto.getFromAccountId());
        Account toAccount = em.find(Account.class, requestDto.getToAccountId());

        if (fromAccount == null || toAccount == null) {
            throw new AccountNotFoundException("Source or destination account not found");
        }
        BigDecimal amount = requestDto.getAmount();
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds in source account");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        em.merge(fromAccount);
        em.merge(toAccount);

        Transaction transaction = new Transaction(
                fromAccount,
                amount.negate(),
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                LocalDateTime.now(),
                String.format("Transferred to account %s", toAccount.getAccountNumber())
        );
        em.persist(transaction);

        Transaction transaction2 = new Transaction(
                toAccount,
                amount,
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                LocalDateTime.now(),
                String.format("Received from account %s", fromAccount.getAccountNumber())
        );
        em.persist(transaction2);

        return transaction;
    }
}