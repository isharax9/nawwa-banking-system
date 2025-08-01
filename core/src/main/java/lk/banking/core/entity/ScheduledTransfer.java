package lk.banking.core.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // IMPORT THIS
import java.util.Objects;

/**
 * Represents a scheduled fund transfer between two accounts.
 * Includes audit fields, processed flag, and extensibility for future scheduling options.
 */
@Entity
@Table(name = "scheduled_transfers")
public class ScheduledTransfer implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY) // FetchType.LAZY is fine, but remember to JOIN FETCH in queries for JSP display
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY) // FetchType.LAZY is fine
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime scheduledTime;

    @Column(nullable = false)
    private Boolean processed = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ---- Constructors ----
    public ScheduledTransfer() {}

    public ScheduledTransfer(Account fromAccount, Account toAccount, BigDecimal amount, LocalDateTime scheduledTime) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.scheduledTime = scheduledTime;
        this.processed = false; // Always false when scheduled initially
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ---- Getters and Setters ----
    public Long getId() { return id; }
    public Account getFromAccount() { return fromAccount; }
    public void setFromAccount(Account fromAccount) { this.fromAccount = fromAccount; }
    public Account getToAccount() { return toAccount; }
    public void setToAccount(Account toAccount) { this.toAccount = toAccount; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }
    public Boolean getProcessed() { return processed; }
    public void setProcessed(Boolean processed) { this.processed = processed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // NEW METHODS: Formatted timestamps for JSP display
    /**
     * Helper method to format the scheduledTime for display in JSPs.
     * @return Formatted scheduledTime string, or empty string if null.
     */
    public String getFormattedScheduledTime() {
        if (this.scheduledTime == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return this.scheduledTime.format(formatter);
    }

    /**
     * Helper method to format the createdAt timestamp for display in JSPs.
     * @return Formatted createdAt string, or empty string if null.
     */
    public String getFormattedCreatedAt() {
        if (this.createdAt == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return this.createdAt.format(formatter);
    }

    // ---- equals & hashCode (by id) ----
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduledTransfer)) return false;
        ScheduledTransfer that = (ScheduledTransfer) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "ScheduledTransfer{" +
                "id=" + id +
                ", fromAccount=" + (fromAccount != null ? fromAccount.getId() : null) +
                ", toAccount=" + (toAccount != null ? toAccount.getId() : null) +
                ", amount=" + amount +
                ", scheduledTime=" + scheduledTime +
                ", processed=" + processed +
                '}';
    }
}