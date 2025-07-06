package lk.banking.services;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lk.banking.core.dto.TransactionDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.enums.TransactionStatus;
import lk.banking.core.exception.AccountNotFoundException;

import java.util.List;

@Stateless
public class TransactionServiceImpl implements TransactionService {
    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    public Transaction createTransaction(TransactionDto transactionDto) {
        Account account = em.find(Account.class, transactionDto.getAccountId());
        if (account == null) throw new AccountNotFoundException("Account not found");

        Transaction transaction = new Transaction(
                account,
                transactionDto.getAmount(),
                transactionDto.getType(),
                TransactionStatus.PENDING,
                transactionDto.getTimestamp(),
                transactionDto.getDescription()
        );
        em.persist(transaction);
        return transaction;
    }

    @Override
    public Transaction getTransactionById(Long id) {
        return em.find(Transaction.class, id);
    }

    @Override
    public List<Transaction> getTransactionsByAccount(Long accountId) {
        return em.createQuery(
                        "SELECT t FROM Transaction t WHERE t.account.id = :aid", Transaction.class)
                .setParameter("aid", accountId)
                .getResultList();
    }

    @Override
    public List<Transaction> getAllTransactions() {
        return em.createQuery("SELECT t FROM Transaction t", Transaction.class).getResultList();
    }
}