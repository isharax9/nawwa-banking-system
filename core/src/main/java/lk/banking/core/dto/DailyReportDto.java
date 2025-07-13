package lk.banking.core.dto;

import lk.banking.core.entity.enums.TransactionType;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.List; // For top accounts
import java.util.ArrayList; // For top accounts

/**
 * DTO for carrying daily banking report data.
 */
public class DailyReportDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private LocalDate reportDate;
    private Long newAccountsCount;
    private Map<TransactionType, Integer> transactionCountByType;
    private Map<TransactionType, BigDecimal> transactionSumByType;
    private List<TopAccountActivity> topAccounts; // List of custom objects for top accounts

    public DailyReportDto() {}

    public DailyReportDto(LocalDate reportDate, Long newAccountsCount,
                          Map<TransactionType, Integer> transactionCountByType,
                          Map<TransactionType, BigDecimal> transactionSumByType,
                          List<TopAccountActivity> topAccounts) {
        this.reportDate = reportDate;
        this.newAccountsCount = newAccountsCount;
        this.transactionCountByType = transactionCountByType;
        this.transactionSumByType = transactionSumByType;
        this.topAccounts = topAccounts;
    }

    // Getters and Setters
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public Long getNewAccountsCount() { return newAccountsCount; }
    public void setNewAccountsCount(Long newAccountsCount) { this.newAccountsCount = newAccountsCount; }
    public Map<TransactionType, Integer> getTransactionCountByType() { return transactionCountByType; }
    public void setTransactionCountByType(Map<TransactionType, Integer> transactionCountByType) { this.transactionCountByType = transactionCountByType; }
    public Map<TransactionType, BigDecimal> getTransactionSumByType() { return transactionSumByType; }
    public void setTransactionSumByType(Map<TransactionType, BigDecimal> transactionSumByType) { this.transactionSumByType = transactionSumByType; }
    public List<TopAccountActivity> getTopAccounts() { return topAccounts; }
    public void setTopAccounts(List<TopAccountActivity> topAccounts) { this.topAccounts = topAccounts; }

    // Nested DTO for Top Account Activity
    public static class TopAccountActivity implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String accountNumber;
        private Long transactionCount;

        public TopAccountActivity() {}

        public TopAccountActivity(String accountNumber, Long transactionCount) {
            this.accountNumber = accountNumber;
            this.transactionCount = transactionCount;
        }
        // Getters and Setters
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        public Long getTransactionCount() { return transactionCount; }
        public void setTransactionCount(Long transactionCount) { this.transactionCount = transactionCount; }
    }
}