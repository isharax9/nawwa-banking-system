package lk.banking.core.dto;

import java.math.BigDecimal;

/**
 * Data Transfer Object for requesting a fund transfer between two accounts.
 */
public class TransferRequestDto {
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;

    public TransferRequestDto() {}

    public TransferRequestDto(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }

    // Getters and setters
    public Long getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(Long fromAccountId) { this.fromAccountId = fromAccountId; }
    public Long getToAccountId() { return toAccountId; }
    public void setToAccountId(Long toAccountId) { this.toAccountId = toAccountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    @Override
    public String toString() {
        return "TransferRequestDto{" +
                "fromAccountId=" + fromAccountId +
                ", toAccountId=" + toAccountId +
                ", amount=" + amount +
                '}';
    }
}