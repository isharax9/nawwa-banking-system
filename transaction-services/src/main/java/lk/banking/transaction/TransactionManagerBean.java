// File: lk/banking/transaction/TransactionManagerBean.java
package lk.banking.transaction;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject; // Essential for CDI injection of other EJBs/beans
import lk.banking.core.dto.TransferRequestDto;
import lk.banking.core.dto.TransactionDto;
import lk.banking.core.entity.Transaction;
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.InsufficientFundsException;
import lk.banking.core.exception.InvalidTransactionException;

/**
 * A Stateless Session Bean acting as a facade for the transaction services.
 * It encapsulates the complexities of various transaction types by delegating
 * to specialized services like FundTransferService and PaymentProcessingService.
 */
@Stateless
public class TransactionManagerBean implements TransactionManager { // Implements the new interface

    // Inject specialized services using CDI
    @Inject
    private FundTransferService fundTransferService;

    @Inject
    private PaymentProcessingService paymentProcessingService;

    /**
     * Delegates the fund transfer request to the FundTransferService.
     * @param transferRequestDto DTO containing fromAccount, toAccount, and amount.
     * @return The Transaction representing the primary leg of the transfer.
     * @throws AccountNotFoundException if either account is not found.
     * @throws InsufficientFundsException if the source account has insufficient funds.
     * @throws InvalidTransactionException if the amount is non-positive or accounts are identical.
     */
    @Override
    public Transaction transferFunds(TransferRequestDto transferRequestDto) {
        // This bean simply orchestrates/delegates. The actual logic and exception
        // handling are in FundTransferService.
        return fundTransferService.transferFunds(transferRequestDto);
    }

    /**
     * Delegates the payment processing request to the PaymentProcessingService.
     * @param transactionDto DTO containing account ID, amount, type (DEPOSIT, WITHDRAWAL, PAYMENT), and description.
     * @return The Transaction record created for the payment.
     * @throws AccountNotFoundException if the account is not found.
     * @throws InsufficientFundsException if the account has insufficient funds for withdrawal/payment.
     * @throws InvalidTransactionException if the amount is non-positive or the type is unsupported.
     */
    @Override
    public Transaction processPayment(TransactionDto transactionDto) {
        // This bean simply orchestrates/delegates. The actual logic and exception
        // handling are in PaymentProcessingService.
        return paymentProcessingService.processPayment(transactionDto);
    }
}