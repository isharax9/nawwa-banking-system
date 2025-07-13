package lk.banking.core.dto;

import java.io.Serializable; // Add this import
import java.math.BigDecimal;
import java.time.LocalDateTime; // Add this import for createdAt/updatedAt
import lk.banking.core.entity.enums.AccountType;

/**
 * Data Transfer Object for Account entity.
 * Used for creating/updating accounts and transferring account data.
 */
public class AccountDto implements Serializable { // Ensure Serializable
    private Long id;
    private String accountNumber;
    private AccountType type;
    private BigDecimal balance;
    private Long customerId;
    private String customerName; // NEW: To display customer name
    private Boolean isActive; // NEW: To display account status
    private LocalDateTime createdAt; // NEW: To display creation date
    private LocalDateTime updatedAt; // NEW: To display update date

    public AccountDto() {}

    // Updated constructor to include new fields
    public AccountDto(Long id, String accountNumber, AccountType type, BigDecimal balance,
                      Long customerId, String customerName, Boolean isActive,
                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.type = type;
        this.balance = balance;
        this.customerId = customerId;
        this.customerName = customerName;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and setters for new fields
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }


    // Existing getters and setters
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

    // Helper method for formatted creation timestamp (like TransactionDto)
    public String getFormattedCreatedAt() {
        if (this.createdAt == null) {
            return "";
        }
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return this.createdAt.format(formatter);
    }

    @Override
    public String toString() {
        return "AccountDto{" +
                "id=" + id +
                ", accountNumber='" + accountNumber + '\'' +
                ", type=" + type +
                ", balance=" + balance +
                ", customerId=" + customerId +
                ", customerName='" + customerName + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}