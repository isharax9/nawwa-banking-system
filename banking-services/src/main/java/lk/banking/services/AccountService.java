package lk.banking.services;

import jakarta.ejb.Local;
import lk.banking.core.dto.AccountDto;
import lk.banking.core.entity.Account;

import java.util.List;

@Local
public interface AccountService {
    Account createAccount(AccountDto accountDto);
    Account getAccountById(Long id);
    Account getAccountByNumber(String accountNumber);
    List<Account> getAccountsByCustomer(Long customerId);
    Account updateAccount(AccountDto accountDto);
    void deleteAccount(Long id);

    List<Account> findAccountsByUserId(Long id);
}