package lk.banking.services;

import jakarta.ejb.Local;
import lk.banking.core.dto.TransactionDto;
import lk.banking.core.entity.Transaction;

import java.util.List;

@Local
public interface TransactionService {
    Transaction createTransaction(TransactionDto transactionDto);
    Transaction getTransactionById(Long id);
    List<Transaction> getTransactionsByAccount(Long accountId);
    List<Transaction> getAllTransactions();
}