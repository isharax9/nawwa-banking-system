package lk.banking.core.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for exposing scheduled transfer details.
 */
public class ScheduledTransferDto {
    private Long id;
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    private LocalDateTime scheduledTime;
    private Boolean processed;

    public ScheduledTransferDto() {}

    public ScheduledTransferDto(Long id, Long fromAccountId, Long toAccountId, BigDecimal amount, LocalDateTime scheduledTime, Boolean processed) {
        this.id = id;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.scheduledTime = scheduledTime;
        this.processed = processed;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(Long fromAccountId) { this.fromAccountId = fromAccountId; }
    public Long getToAccountId() { return toAccountId; }
    public void setToAccountId(Long toAccountId) { this.toAccountId = toAccountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }
    public Boolean getProcessed() { return processed; }
    public void setProcessed(Boolean processed) { this.processed = processed; }
}