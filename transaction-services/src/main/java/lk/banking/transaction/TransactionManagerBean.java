package lk.banking.transaction;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import lk.banking.core.dto.TransferRequestDto;
import lk.banking.core.dto.TransactionDto;
import lk.banking.core.entity.Transaction;

@Stateless
public class TransactionManagerBean {

    @Inject
    private FundTransferService fundTransferService;

    @Inject
    private PaymentProcessingService paymentProcessingService;

    public Transaction transferFunds(TransferRequestDto transferRequestDto) {
        return fundTransferService.transferFunds(transferRequestDto);
    }

    public Transaction processPayment(TransactionDto transactionDto) {
        return paymentProcessingService.processPayment(transactionDto);
    }
}