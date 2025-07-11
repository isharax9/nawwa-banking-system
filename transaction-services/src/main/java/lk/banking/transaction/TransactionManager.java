package lk.banking.transaction;

import jakarta.ejb.Local;
import lk.banking.core.dto.TransferRequestDto;
import lk.banking.core.dto.TransactionDto;
import lk.banking.core.entity.Transaction;

/**
 * Local interface for the TransactionManagerBean, acting as a facade
 * for various transaction-related services.
 */
@Local
public interface TransactionManager {
    /**
     * Initiates a fund transfer between two accounts.
     * Delegates to FundTransferService.
     * @param transferRequestDto DTO containing details for the fund transfer.
     * @return The primary Transaction record created for the transfer (e.g., the debit leg).
     */
    Transaction transferFunds(TransferRequestDto transferRequestDto);

    /**
     * Processes a single-leg payment transaction (deposit, withdrawal, or simple payment).
     * Delegates to PaymentProcessingService.
     * @param transactionDto DTO containing details for the payment.
     * @return The Transaction record created for the payment.
     */
    Transaction processPayment(TransactionDto transactionDto);
}