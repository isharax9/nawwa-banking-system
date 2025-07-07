package lk.banking.core.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lk.banking.core.entity.enums.TransactionStatus;
import lk.banking.core.entity.enums.TransactionType;

/**
 * Data Transfer Object for Transaction entity.
 * Used for creating, updating, and transferring transaction data.
 */
public class TransactionDto {
    private Long id;
    private Long accountId;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private LocalDateTime timestamp;
    private String description;

    public TransactionDto() {}

    public TransactionDto(Long id, Long accountId, BigDecimal amount, TransactionType type,
                          TransactionStatus status, LocalDateTime timestamp, String description) {
        this.id = id;
        this.accountId = accountId;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.timestamp = timestamp;
        this.description = description;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return "TransactionDto{" +
                "id=" + id +
                ", accountId=" + accountId +
                ", amount=" + amount +
                ", type=" + type +
                ", status=" + status +
                ", timestamp=" + timestamp +
                ", description='" + description + '\'' +
                '}';
    }
}