package lk.banking.transaction;

import jakarta.ejb.Local;
import lk.banking.core.dto.TransactionDto;
import lk.banking.core.entity.Transaction;

@Local
public interface PaymentProcessingService {
    Transaction processPayment(TransactionDto transactionDto);
}