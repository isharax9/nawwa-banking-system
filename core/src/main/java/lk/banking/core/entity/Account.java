package lk.banking.core.entity;

import jakarta.persistence.*;
import lk.banking.core.entity.enums.AccountType;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private AccountType type;

    @Column(nullable=false)
    private BigDecimal balance;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<Transaction> transactions;

    // Constructors, getters, setters
    public Account() {}

    public Account(String accountNumber, AccountType type, BigDecimal balance, Customer customer) {
        this.accountNumber = accountNumber;
        this.type = type;
        this.balance = balance;
        this.customer = customer;
    }
    // Getters and setters
    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public AccountType getType() { return type; }
    public void setType(AccountType type) { this.type = type; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }
}