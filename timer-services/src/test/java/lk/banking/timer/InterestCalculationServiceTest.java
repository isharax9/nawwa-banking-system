package lk.banking.timer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.Customer; // Needed for Account setup
import lk.banking.core.entity.enums.AccountType;
import lk.banking.core.entity.enums.TransactionStatus;
import lk.banking.core.entity.enums.TransactionType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Answers; // For deep stubbing on TypedQuery if needed, but not primarily here.

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterestCalculationService Unit Tests")
public class InterestCalculationServiceTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private InterestCalculationService interestCalculationService;

    @Mock
    private TypedQuery<Account> mockAccountQuery; // Used for fetching savings accounts

    private Customer testCustomer;
    private Account activeSavingsAccount;
    private Account inactiveSavingsAccount;
    private Account currentAccount;
    private Account zeroBalanceSavingsAccount;
    private Account negativeBalanceSavingsAccount;

    private static final BigDecimal DAILY_INTEREST_RATE_TEST = BigDecimal.valueOf(0.01); // Must match service's rate for accurate tests

    @BeforeEach
    void setUp() {
        testCustomer = new Customer("John Doe", "john.doe@example.com", "123 Main St", "1234567890");
        testCustomer.setId(1L);

        activeSavingsAccount = new Account("SVS001", AccountType.SAVINGS, BigDecimal.valueOf(1000.00), testCustomer);
        activeSavingsAccount.setId(1L);
        activeSavingsAccount.setIsActive(true);
        // Set lastInterestAppliedDate to 3 days ago for a clear interest calculation
        activeSavingsAccount.setLastInterestAppliedDate(LocalDateTime.now().minusDays(3).withHour(0).withMinute(0).withSecond(0).withNano(0));
        activeSavingsAccount.setCreatedAt(LocalDateTime.now().minusMonths(6));

        inactiveSavingsAccount = new Account("SVS002", AccountType.SAVINGS, BigDecimal.valueOf(2000.00), testCustomer);
        inactiveSavingsAccount.setId(2L);
        inactiveSavingsAccount.setIsActive(false); // Inactive
        inactiveSavingsAccount.setLastInterestAppliedDate(LocalDateTime.now().minusDays(10));
        inactiveSavingsAccount.setCreatedAt(LocalDateTime.now().minusMonths(6));

        currentAccount = new Account("CUR001", AccountType.CURRENT, BigDecimal.valueOf(1500.00), testCustomer);
        currentAccount.setId(3L);
        currentAccount.setIsActive(true); // Should not be selected by query

        zeroBalanceSavingsAccount = new Account("SVS003", AccountType.SAVINGS, BigDecimal.ZERO, testCustomer);
        zeroBalanceSavingsAccount.setId(4L);
        zeroBalanceSavingsAccount.setIsActive(true);
        zeroBalanceSavingsAccount.setLastInterestAppliedDate(LocalDateTime.now().minusDays(3));
        zeroBalanceSavingsAccount.setCreatedAt(LocalDateTime.now().minusMonths(6));

        negativeBalanceSavingsAccount = new Account("SVS004", AccountType.SAVINGS, BigDecimal.valueOf(-100.00), testCustomer);
        negativeBalanceSavingsAccount.setId(5L);
        negativeBalanceSavingsAccount.setIsActive(true);
        negativeBalanceSavingsAccount.setLastInterestAppliedDate(LocalDateTime.now().minusDays(3));
        negativeBalanceSavingsAccount.setCreatedAt(LocalDateTime.now().minusMonths(6));
    }

    // Helper to calculate expected compound interest for tests
    private BigDecimal calculateExpectedCompoundInterest(BigDecimal initialBalance, int days) {
        BigDecimal interest = BigDecimal.ZERO;
        BigDecimal tempBalance = initialBalance;
        MathContext mc = new MathContext(10, RoundingMode.HALF_UP); // Must match service's MathContext

        for (int i = 0; i < days; i++) {
            BigDecimal dailyInterest = tempBalance.multiply(DAILY_INTEREST_RATE_TEST, mc);
            interest = interest.add(dailyInterest);
            tempBalance = tempBalance.add(dailyInterest);
        }
        return interest.setScale(2, RoundingMode.HALF_UP); // Round to 2 decimal places for comparison
    }

    // --- Test calculateInterest method ---

    @Test
    @DisplayName("should calculate and apply interest to active savings accounts for full days")
    void calculateInterest_SuccessFullDays() {
        // Given
        List<Account> accountsToProcess = Arrays.asList(activeSavingsAccount);
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountQuery);
        when(mockAccountQuery.setParameter(eq("type"), eq(AccountType.SAVINGS))).thenReturn(mockAccountQuery);
        when(mockAccountQuery.getResultList()).thenReturn(accountsToProcess);

        BigDecimal initialBalance = activeSavingsAccount.getBalance();
        LocalDateTime initialLastAppliedDate = activeSavingsAccount.getLastInterestAppliedDate();
        long daysPassed = ChronoUnit.DAYS.between(initialLastAppliedDate.toLocalDate(), LocalDateTime.now().toLocalDate());
        BigDecimal expectedInterest = calculateExpectedCompoundInterest(initialBalance, (int) daysPassed);
        BigDecimal expectedNewBalance = initialBalance.add(expectedInterest);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        // When
        interestCalculationService.calculateInterest();

        // Then
        verify(entityManager, times(1)).createQuery(anyString(), eq(Account.class));
        verify(mockAccountQuery, times(1)).setParameter(eq("type"), eq(AccountType.SAVINGS));
        verify(mockAccountQuery, times(1)).getResultList();

        assertThat(activeSavingsAccount.getBalance()).isEqualByComparingTo(expectedNewBalance);
        assertThat(activeSavingsAccount.getLastInterestAppliedDate().toLocalDate()).isEqualTo(LocalDateTime.now().toLocalDate());

        verify(entityManager, times(1)).persist(transactionCaptor.capture());
        Transaction capturedTransaction = transactionCaptor.getValue();

        assertThat(capturedTransaction.getAccount()).isEqualTo(activeSavingsAccount);
        assertThat(capturedTransaction.getAmount()).isEqualByComparingTo(expectedInterest);
        assertThat(capturedTransaction.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(capturedTransaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(capturedTransaction.getDescription()).contains("Automated daily interest applied for " + daysPassed + " days");
    }

    @Test
    @DisplayName("should skip interest calculation if no active savings accounts are found")
    void calculateInterest_NoSavingsAccountsFound() {
        // Given
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountQuery);
        when(mockAccountQuery.setParameter(eq("type"), eq(AccountType.SAVINGS))).thenReturn(mockAccountQuery);
        when(mockAccountQuery.getResultList()).thenReturn(Collections.emptyList());

        // When
        interestCalculationService.calculateInterest();

        // Then
        verify(entityManager, times(1)).createQuery(anyString(), eq(Account.class));
        verify(mockAccountQuery, times(1)).setParameter(eq("type"), eq(AccountType.SAVINGS));
        verify(mockAccountQuery, times(1)).getResultList();
        verify(entityManager, never()).persist(any(Transaction.class)); // No transactions should be persisted
        // No balance changes on any account objects
    }

    @Test
    @DisplayName("should skip interest for accounts with non-positive balance")
    void calculateInterest_NonPositiveBalance() {
        // Given
        List<Account> accountsToProcess = Arrays.asList(zeroBalanceSavingsAccount, negativeBalanceSavingsAccount);
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountQuery);
        when(mockAccountQuery.setParameter(eq("type"), eq(AccountType.SAVINGS))).thenReturn(mockAccountQuery);
        when(mockAccountQuery.getResultList()).thenReturn(accountsToProcess);

        BigDecimal initialZeroBalance = zeroBalanceSavingsAccount.getBalance();
        BigDecimal initialNegativeBalance = negativeBalanceSavingsAccount.getBalance();

        // When
        interestCalculationService.calculateInterest();

        // Then
        verify(entityManager, times(1)).createQuery(anyString(), eq(Account.class));
        verify(mockAccountQuery, times(1)).setParameter(eq("type"), eq(AccountType.SAVINGS));
        verify(mockAccountQuery, times(1)).getResultList();
        verify(entityManager, never()).persist(any(Transaction.class)); // No transactions for these accounts

        assertThat(zeroBalanceSavingsAccount.getBalance()).isEqualByComparingTo(initialZeroBalance);
        assertThat(negativeBalanceSavingsAccount.getBalance()).isEqualByComparingTo(initialNegativeBalance);
    }

    @Test
    @DisplayName("should skip interest if no full days passed since last application")
    void calculateInterest_NoFullDaysPassed() {
        // Given
        Account recentInterestAccount = new Account("SVS005", AccountType.SAVINGS, BigDecimal.valueOf(1000.00), testCustomer);
        recentInterestAccount.setId(6L);
        recentInterestAccount.setIsActive(true);
        // Set lastInterestAppliedDate to less than a day ago
        recentInterestAccount.setLastInterestAppliedDate(LocalDateTime.now().minusHours(1));
        recentInterestAccount.setCreatedAt(LocalDateTime.now().minusMonths(6));

        List<Account> accountsToProcess = Arrays.asList(recentInterestAccount);
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountQuery);
        when(mockAccountQuery.setParameter(eq("type"), eq(AccountType.SAVINGS))).thenReturn(mockAccountQuery);
        when(mockAccountQuery.getResultList()).thenReturn(accountsToProcess);

        BigDecimal initialBalance = recentInterestAccount.getBalance();

        // When
        interestCalculationService.calculateInterest();

        // Then
        verify(entityManager, times(1)).createQuery(anyString(), eq(Account.class));
        verify(mockAccountQuery, times(1)).setParameter(eq("type"), eq(AccountType.SAVINGS));
        verify(mockAccountQuery, times(1)).getResultList();
        verify(entityManager, never()).persist(any(Transaction.class)); // No transactions

        assertThat(recentInterestAccount.getBalance()).isEqualByComparingTo(initialBalance); // Balance should not change
    }

    @Test
    @DisplayName("should apply interest using createdAt date if lastInterestAppliedDate is null and enough days passed")
    void calculateInterest_NullLastAppliedDate_EnoughDays() {
        // Given
        Account newAccountOldCreation = new Account("SVS006", AccountType.SAVINGS, BigDecimal.valueOf(500.00), testCustomer);
        newAccountOldCreation.setId(7L);
        newAccountOldCreation.setIsActive(true);
        newAccountOldCreation.setLastInterestAppliedDate(null); // Null last applied date
        // Created date is several days ago, so interest should be applied
        newAccountOldCreation.setCreatedAt(LocalDateTime.now().minusDays(5).withHour(0).withMinute(0).withSecond(0).withNano(0));

        List<Account> accountsToProcess = Arrays.asList(newAccountOldCreation);
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountQuery);
        when(mockAccountQuery.setParameter(eq("type"), eq(AccountType.SAVINGS))).thenReturn(mockAccountQuery);
        when(mockAccountQuery.getResultList()).thenReturn(accountsToProcess);

        BigDecimal initialBalance = newAccountOldCreation.getBalance();
        long daysPassed = ChronoUnit.DAYS.between(newAccountOldCreation.getCreatedAt().toLocalDate(), LocalDateTime.now().toLocalDate());
        BigDecimal expectedInterest = calculateExpectedCompoundInterest(initialBalance, (int) daysPassed);
        BigDecimal expectedNewBalance = initialBalance.add(expectedInterest);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        // When
        interestCalculationService.calculateInterest();

        // Then
        assertThat(newAccountOldCreation.getBalance()).isEqualByComparingTo(expectedNewBalance);
        assertThat(newAccountOldCreation.getLastInterestAppliedDate().toLocalDate()).isEqualTo(LocalDateTime.now().toLocalDate());
        verify(entityManager, times(1)).persist(transactionCaptor.capture());
        assertThat(transactionCaptor.getValue().getAmount()).isEqualByComparingTo(expectedInterest);
    }

    @Test
    @DisplayName("should skip interest if lastInterestAppliedDate is null and created less than a day ago")
    void calculateInterest_NullLastAppliedDate_RecentCreation() {
        // Given
        Account veryNewAccount = new Account("SVS007", AccountType.SAVINGS, BigDecimal.valueOf(500.00), testCustomer);
        veryNewAccount.setId(8L);
        veryNewAccount.setIsActive(true);
        veryNewAccount.setLastInterestAppliedDate(null); // Null last applied date
        // Created very recently (less than a day ago)
        veryNewAccount.setCreatedAt(LocalDateTime.now().minusHours(5));

        List<Account> accountsToProcess = Arrays.asList(veryNewAccount);
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountQuery);
        when(mockAccountQuery.setParameter(eq("type"), eq(AccountType.SAVINGS))).thenReturn(mockAccountQuery);
        when(mockAccountQuery.getResultList()).thenReturn(accountsToProcess);

        BigDecimal initialBalance = veryNewAccount.getBalance();

        // When
        interestCalculationService.calculateInterest();

        // Then
        verify(entityManager, never()).persist(any(Transaction.class)); // No transactions
        assertThat(veryNewAccount.getBalance()).isEqualByComparingTo(initialBalance); // Balance should not change
    }

    @Test
    @DisplayName("should process multiple active savings accounts correctly")
    void calculateInterest_MultipleAccounts() {
        // Given
        Account account1 = activeSavingsAccount; // 1000.00, 3 days ago
        Account account2 = new Account("SVS008", AccountType.SAVINGS, BigDecimal.valueOf(2000.00), testCustomer);
        account2.setId(9L);
        account2.setIsActive(true);
        account2.setLastInterestAppliedDate(LocalDateTime.now().minusDays(2).withHour(0).withMinute(0).withSecond(0).withNano(0));
        account2.setCreatedAt(LocalDateTime.now().minusMonths(5));

        List<Account> accountsToProcess = Arrays.asList(account1, account2);
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountQuery);
        when(mockAccountQuery.setParameter(eq("type"), eq(AccountType.SAVINGS))).thenReturn(mockAccountQuery);
        when(mockAccountQuery.getResultList()).thenReturn(accountsToProcess);

        BigDecimal initialBalance1 = account1.getBalance();
        BigDecimal initialBalance2 = account2.getBalance();

        long daysPassed1 = ChronoUnit.DAYS.between(account1.getLastInterestAppliedDate().toLocalDate(), LocalDateTime.now().toLocalDate());
        BigDecimal expectedInterest1 = calculateExpectedCompoundInterest(initialBalance1, (int) daysPassed1);
        BigDecimal expectedNewBalance1 = initialBalance1.add(expectedInterest1);

        long daysPassed2 = ChronoUnit.DAYS.between(account2.getLastInterestAppliedDate().toLocalDate(), LocalDateTime.now().toLocalDate());
        BigDecimal expectedInterest2 = calculateExpectedCompoundInterest(initialBalance2, (int) daysPassed2);
        BigDecimal expectedNewBalance2 = initialBalance2.add(expectedInterest2);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        // When
        interestCalculationService.calculateInterest();

        // Then
        verify(entityManager, times(1)).createQuery(anyString(), eq(Account.class));
        verify(mockAccountQuery, times(1)).setParameter(eq("type"), eq(AccountType.SAVINGS));
        verify(mockAccountQuery, times(1)).getResultList();

        assertThat(account1.getBalance()).isEqualByComparingTo(expectedNewBalance1);
        assertThat(account1.getLastInterestAppliedDate().toLocalDate()).isEqualTo(LocalDateTime.now().toLocalDate());
        assertThat(account2.getBalance()).isEqualByComparingTo(expectedNewBalance2);
        assertThat(account2.getLastInterestAppliedDate().toLocalDate()).isEqualTo(LocalDateTime.now().toLocalDate());

        verify(entityManager, times(2)).persist(transactionCaptor.capture()); // Two transactions expected
        List<Transaction> capturedTransactions = transactionCaptor.getAllValues();
        assertThat(capturedTransactions).hasSize(2);

        // Check each captured transaction
        Transaction trans1 = capturedTransactions.stream().filter(t -> t.getAccount().equals(account1)).findFirst().orElse(null);
        Transaction trans2 = capturedTransactions.stream().filter(t -> t.getAccount().equals(account2)).findFirst().orElse(null);

        assertThat(trans1).isNotNull();
        assertThat(trans1.getAmount()).isEqualByComparingTo(expectedInterest1);
        assertThat(trans2).isNotNull();
        assertThat(trans2.getAmount()).isEqualByComparingTo(expectedInterest2);
    }
}