package lk.banking.services;

import jakarta.ejb.Local;
import lk.banking.core.dto.TransactionDto;
import lk.banking.core.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Local
public interface TransactionServices {
    Transaction createTransaction(TransactionDto transactionDto);
    Transaction getTransactionById(Long id);
    List<Transaction> getTransactionsByAccount(Long accountId);
    List<Transaction> getAllTransactions();

    boolean transferFunds(Long id, String fromAccount, String toAccount, BigDecimal amount);

    List<Transaction> getTransactionsByAccountAndDateRange(Long accountId, LocalDateTime from, LocalDateTime to, boolean includeArchived);

    List<Transaction> getTransactionsByUser(Long userId);

    // ADD THIS NEW METHOD
    List<Transaction> getTransactionsByUser(Long userId, int maxResults);
}