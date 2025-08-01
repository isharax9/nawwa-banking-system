package lk.banking.core.entity;

import jakarta.persistence.*;
import lk.banking.core.entity.enums.AccountType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Represents a bank account, linked to a customer, with audit fields and improved design.
 */
@Entity
@Table(name = "accounts", uniqueConstraints = @UniqueConstraint(columnNames = "accountNumber"))
public class Account implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=32)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=32)
    private AccountType type;

    @Column(nullable=false)
    private BigDecimal balance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean isActive = true;

    // NEW FIELD: To track when interest was last applied
    @Column(nullable = true) // Can be null initially or for accounts not earning interest
    private LocalDateTime lastInterestAppliedDate;

    // ---- Constructors ----
    public Account() {}

    public Account(String accountNumber, AccountType type, BigDecimal balance, Customer customer) {
        this.accountNumber = accountNumber;
        this.type = type;
        this.balance = balance;
        this.customer = customer;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        // lastInterestAppliedDate will be null initially for new accounts
        // or explicitly set by interest calculation logic
    }

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
        // For new SAVINGS accounts, set initial lastInterestAppliedDate to now
        if (this.type == AccountType.SAVINGS) {
            this.lastInterestAppliedDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ---- Getters and Setters ----

    public void setId(Long id) {
        this.id = id;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    // NEW: Getter and Setter for lastInterestAppliedDate
    public LocalDateTime getLastInterestAppliedDate() { return lastInterestAppliedDate; }
    public void setLastInterestAppliedDate(LocalDateTime lastInterestAppliedDate) { this.lastInterestAppliedDate = lastInterestAppliedDate; }

    // NEW: Formatted getter for JSP display
    public String getFormattedLastInterestAppliedDate() {
        if (this.lastInterestAppliedDate == null) {
            return "N/A"; // Or current date depending on interpretation
        }
        return this.lastInterestAppliedDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    // ---- equals & hashCode based on accountNumber ----
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        Account account = (Account) o;
        return Objects.equals(accountNumber, account.accountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber);
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", number='" + accountNumber + '\'' +
                ", type=" + type +
                ", balance=" + balance +
                ", isActive=" + isActive +
                ", lastInterestAppliedDate=" + lastInterestAppliedDate +
                '}';
    }
}