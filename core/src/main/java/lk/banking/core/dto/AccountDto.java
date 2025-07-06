package lk.banking.core.dto;

import java.math.BigDecimal;
import lk.banking.core.entity.enums.AccountType;

public class AccountDto {
    private Long id;
    private String accountNumber;
    private AccountType type;
    private BigDecimal balance;
    private Long customerId;

    public AccountDto() {}

    public AccountDto(Long id, String accountNumber, AccountType type, BigDecimal balance, Long customerId) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.type = type;
        this.balance = balance;
        this.customerId = customerId;
    }
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public AccountType getType() { return type; }
    public void setType(AccountType type) { this.type = type; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
}