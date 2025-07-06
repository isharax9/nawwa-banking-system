package lk.banking.transaction;

import jakarta.ejb.Local;
import lk.banking.core.dto.TransferRequestDto;
import lk.banking.core.entity.Transaction;

@Local
public interface FundTransferService {
    Transaction transferFunds(TransferRequestDto requestDto);
}