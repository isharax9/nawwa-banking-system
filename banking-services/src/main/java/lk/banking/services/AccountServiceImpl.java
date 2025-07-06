package lk.banking.services;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lk.banking.core.dto.AccountDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Customer;
import lk.banking.core.entity.enums.AccountType;
import lk.banking.core.exception.AccountNotFoundException;

import java.math.BigDecimal;
import java.util.List;

@Stateless
public class AccountServiceImpl implements AccountService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    public Account createAccount(AccountDto accountDto) {
        Customer customer = em.find(Customer.class, accountDto.getCustomerId());
        if (customer == null) throw new AccountNotFoundException("Customer not found");
        Account account = new Account(
                accountDto.getAccountNumber(),
                accountDto.getType(),
                accountDto.getBalance() != null ? accountDto.getBalance() : BigDecimal.ZERO,
                customer
        );
        em.persist(account);
        return account;
    }

    @Override
    public Account getAccountById(Long id) {
        Account account = em.find(Account.class, id);
        if (account == null) throw new AccountNotFoundException("Account not found");
        return account;
    }

    @Override
    public Account getAccountByNumber(String accountNumber) {
        List<Account> results = em.createQuery(
                        "SELECT a FROM Account a WHERE a.accountNumber = :num", Account.class)
                .setParameter("num", accountNumber)
                .getResultList();
        if (results.isEmpty()) throw new AccountNotFoundException("Account not found");
        return results.get(0);
    }

    @Override
    public List<Account> getAccountsByCustomer(Long customerId) {
        return em.createQuery(
                        "SELECT a FROM Account a WHERE a.customer.id = :cid", Account.class)
                .setParameter("cid", customerId)
                .getResultList();
    }

    @Override
    public Account updateAccount(AccountDto accountDto) {
        Account account = em.find(Account.class, accountDto.getId());
        if (account == null) throw new AccountNotFoundException("Account not found");
        account.setType(accountDto.getType());
        account.setBalance(accountDto.getBalance());
        em.merge(account);
        return account;
    }

    @Override
    public void deleteAccount(Long id) {
        Account account = em.find(Account.class, id);
        if (account == null) throw new AccountNotFoundException("Account not found");
        em.remove(account);
    }
}