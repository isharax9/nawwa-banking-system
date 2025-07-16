package lk.banking.transaction;

import jakarta.persistence.EntityManager;
import lk.banking.core.dto.TransactionDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Customer; // Required for Account setup
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.enums.AccountType;
import lk.banking.core.entity.enums.TransactionStatus;
import lk.banking.core.entity.enums.TransactionType;
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.InsufficientFundsException;
import lk.banking.core.exception.InvalidTransactionException;
import lk.banking.core.exception.ValidationException; // Keep for general validation checks

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentProcessingServiceImpl Unit Tests")
public class PaymentProcessingServiceImplTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private PaymentProcessingServiceImpl paymentProcessingService;

    // Test Data
    private Customer testCustomer;
    private Account testAccount; // Account for deposits/withdrawals/payments
    private TransactionDto baseTransactionDto;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer("John Doe", "john.doe@example.com", "123 Main St", "1234567890");
        testCustomer.setId(1L);

        testAccount = new Account("ACC123456789", AccountType.SAVINGS, BigDecimal.valueOf(1000.00), testCustomer);
        testAccount.setId(201L);
        testAccount.setIsActive(true); // Default to active

        baseTransactionDto = new TransactionDto();
        baseTransactionDto.setAccountId(testAccount.getId());
        baseTransactionDto.setAmount(BigDecimal.valueOf(100.00));
        baseTransactionDto.setDescription("Test Transaction");
        // Type will be set in individual tests
    }

    // --- Test processPayment method - Success Scenarios ---

    @Test
    @DisplayName("should successfully process a DEPOSIT transaction")
    void processPayment_DepositSuccess() {
        // Given
        baseTransactionDto.setType(TransactionType.DEPOSIT);
        BigDecimal initialBalance = testAccount.getBalance();
        BigDecimal depositAmount = baseTransactionDto.getAmount();

        when(entityManager.find(eq(Account.class), eq(testAccount.getId()))).thenReturn(testAccount);
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        // When
        Transaction resultTransaction = paymentProcessingService.processPayment(baseTransactionDto);

        // Then
        assertThat(resultTransaction).isNotNull();
        assertThat(resultTransaction.getAccount()).isEqualTo(testAccount);
        assertThat(resultTransaction.getAmount()).isEqualByComparingTo(depositAmount); // Deposit amount is positive
        assertThat(resultTransaction.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(resultTransaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(testAccount.getBalance()).isEqualByComparingTo(initialBalance.add(depositAmount));

        verify(entityManager, times(1)).find(eq(Account.class), eq(testAccount.getId()));
        verify(entityManager, times(1)).persist(transactionCaptor.capture());

        Transaction capturedTransaction = transactionCaptor.getValue();
        assertThat(capturedTransaction.getAccount()).isEqualTo(testAccount);
        assertThat(capturedTransaction.getAmount()).isEqualByComparingTo(depositAmount);
        assertThat(capturedTransaction.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(capturedTransaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    @DisplayName("should successfully process a WITHDRAWAL transaction with sufficient funds")
    void processPayment_WithdrawalSuccess() {
        // Given
        baseTransactionDto.setType(TransactionType.WITHDRAWAL);
        BigDecimal initialBalance = testAccount.getBalance();
        BigDecimal withdrawalAmount = baseTransactionDto.getAmount(); // 100.00, less than 1000.00

        when(entityManager.find(eq(Account.class), eq(testAccount.getId()))).thenReturn(testAccount);
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        // When
        Transaction resultTransaction = paymentProcessingService.processPayment(baseTransactionDto);

        // Then
        assertThat(resultTransaction).isNotNull();
        assertThat(resultTransaction.getAccount()).isEqualTo(testAccount);
        assertThat(resultTransaction.getAmount()).isEqualByComparingTo(withdrawalAmount.negate()); // Withdrawal is negative
        assertThat(resultTransaction.getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(resultTransaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(testAccount.getBalance()).isEqualByComparingTo(initialBalance.subtract(withdrawalAmount));

        verify(entityManager, times(1)).find(eq(Account.class), eq(testAccount.getId()));
        verify(entityManager, times(1)).persist(transactionCaptor.capture());
    }

    @Test
    @DisplayName("should successfully process a PAYMENT transaction with sufficient funds")
    void processPayment_PaymentSuccess() {
        // Given
        baseTransactionDto.setType(TransactionType.PAYMENT);
        BigDecimal initialBalance = testAccount.getBalance();
        BigDecimal paymentAmount = baseTransactionDto.getAmount(); // 100.00, less than 1000.00

        when(entityManager.find(eq(Account.class), eq(testAccount.getId()))).thenReturn(testAccount);
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        // When
        Transaction resultTransaction = paymentProcessingService.processPayment(baseTransactionDto);

        // Then
        assertThat(resultTransaction).isNotNull();
        assertThat(resultTransaction.getAccount()).isEqualTo(testAccount);
        assertThat(resultTransaction.getAmount()).isEqualByComparingTo(paymentAmount.negate()); // Payment is negative
        assertThat(resultTransaction.getType()).isEqualTo(TransactionType.PAYMENT);
        assertThat(resultTransaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(testAccount.getBalance()).isEqualByComparingTo(initialBalance.subtract(paymentAmount));

        verify(entityManager, times(1)).find(eq(Account.class), eq(testAccount.getId()));
        verify(entityManager, times(1)).persist(transactionCaptor.capture());
    }

    // --- Test processPayment method - Validation Failures ---

    @Test
    @DisplayName("should throw InvalidTransactionException if transactionDto is null")
    void processPayment_TransactionDtoNull() {
        // Given
        TransactionDto nullDto = null;

        // When / Then
        assertThatThrownBy(() -> paymentProcessingService.processPayment(nullDto))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transaction data (account ID, amount, type) cannot be null.");

        verify(entityManager, never()).find(any(), any());
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InvalidTransactionException if account ID in transactionDto is null")
    void processPayment_AccountIdNull() {
        // Given
        baseTransactionDto.setAccountId(null);
        baseTransactionDto.setType(TransactionType.DEPOSIT); // Set a type so it's not the next null check

        // When / Then
        assertThatThrownBy(() -> paymentProcessingService.processPayment(baseTransactionDto))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transaction data (account ID, amount, type) cannot be null.");

        verify(entityManager, never()).find(any(), any());
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InvalidTransactionException if amount in transactionDto is null")
    void processPayment_AmountNull() {
        // Given
        baseTransactionDto.setAmount(null);
        baseTransactionDto.setType(TransactionType.DEPOSIT);

        // When / Then
        assertThatThrownBy(() -> paymentProcessingService.processPayment(baseTransactionDto))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transaction data (account ID, amount, type) cannot be null.");

        verify(entityManager, never()).find(any(), any());
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InvalidTransactionException if type in transactionDto is null")
    void processPayment_TypeNull() {
        // Given
        baseTransactionDto.setType(null);

        // When / Then
        assertThatThrownBy(() -> paymentProcessingService.processPayment(baseTransactionDto))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transaction data (account ID, amount, type) cannot be null.");

        verify(entityManager, never()).find(any(), any());
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InvalidTransactionException if transaction amount is zero or negative")
    void processPayment_AmountZeroOrNegative() {
        // Given
        baseTransactionDto.setAccountId(testAccount.getId()); // Ensure account ID is set for later checks
        baseTransactionDto.setType(TransactionType.DEPOSIT);

        baseTransactionDto.setAmount(BigDecimal.ZERO);
        assertThatThrownBy(() -> paymentProcessingService.processPayment(baseTransactionDto))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transaction amount must be positive.");

        baseTransactionDto.setAmount(BigDecimal.valueOf(-50.00));
        assertThatThrownBy(() -> paymentProcessingService.processPayment(baseTransactionDto))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transaction amount must be positive.");

        verify(entityManager, never()).find(any(), any());
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    // --- Test processPayment method - Account Related Failures ---

    @Test
    @DisplayName("should throw AccountNotFoundException if account is not found")
    void processPayment_AccountNotFound() {
        // Given
        Long nonExistentAccountId = 999L;
        baseTransactionDto.setAccountId(nonExistentAccountId);
        baseTransactionDto.setType(TransactionType.DEPOSIT);

        when(entityManager.find(eq(Account.class), eq(nonExistentAccountId))).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> paymentProcessingService.processPayment(baseTransactionDto))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account with ID " + nonExistentAccountId + " not found.");

        verify(entityManager, times(1)).find(eq(Account.class), eq(nonExistentAccountId));
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InvalidTransactionException if account is inactive for DEPOSIT")
    void processPayment_AccountInactive_Deposit() {
        // Given
        testAccount.setIsActive(false); // Set account to inactive
        baseTransactionDto.setType(TransactionType.DEPOSIT);
        when(entityManager.find(eq(Account.class), eq(testAccount.getId()))).thenReturn(testAccount);

        // When / Then
        assertThatThrownBy(() -> paymentProcessingService.processPayment(baseTransactionDto))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transaction denied: Account " + testAccount.getAccountNumber() + " is inactive.");

        verify(entityManager, times(1)).find(eq(Account.class), eq(testAccount.getId()));
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InvalidTransactionException if account is inactive for WITHDRAWAL")
    void processPayment_AccountInactive_Withdrawal() {
        // Given
        testAccount.setIsActive(false); // Set account to inactive
        baseTransactionDto.setType(TransactionType.WITHDRAWAL);
        when(entityManager.find(eq(Account.class), eq(testAccount.getId()))).thenReturn(testAccount);

        // When / Then
        assertThatThrownBy(() -> paymentProcessingService.processPayment(baseTransactionDto))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transaction denied: Account " + testAccount.getAccountNumber() + " is inactive.");

        verify(entityManager, times(1)).find(eq(Account.class), eq(testAccount.getId()));
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InvalidTransactionException if account is inactive for PAYMENT")
    void processPayment_AccountInactive_Payment() {
        // Given
        testAccount.setIsActive(false); // Set account to inactive
        baseTransactionDto.setType(TransactionType.PAYMENT);
        when(entityManager.find(eq(Account.class), eq(testAccount.getId()))).thenReturn(testAccount);

        // When / Then
        assertThatThrownBy(() -> paymentProcessingService.processPayment(baseTransactionDto))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transaction denied: Account " + testAccount.getAccountNumber() + " is inactive.");

        verify(entityManager, times(1)).find(eq(Account.class), eq(testAccount.getId()));
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    // --- Test processPayment method - Insufficient Funds ---

    @Test
    @DisplayName("should throw InsufficientFundsException for WITHDRAWAL with insufficient funds")
    void processPayment_InsufficientFunds_Withdrawal() {
        // Given
        baseTransactionDto.setType(TransactionType.WITHDRAWAL);
        baseTransactionDto.setAmount(BigDecimal.valueOf(1500.00)); // More than 1000.00 balance
        when(entityManager.find(eq(Account.class), eq(testAccount.getId()))).thenReturn(testAccount);

        // When / Then
        assertThatThrownBy(() -> paymentProcessingService.processPayment(baseTransactionDto))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds for withdrawal in account " + testAccount.getAccountNumber() + ".");

        verify(entityManager, times(1)).find(eq(Account.class), eq(testAccount.getId()));
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InsufficientFundsException for PAYMENT with insufficient funds")
    void processPayment_InsufficientFunds_Payment() {
        // Given
        baseTransactionDto.setType(TransactionType.PAYMENT);
        baseTransactionDto.setAmount(BigDecimal.valueOf(1500.00)); // More than 1000.00 balance
        when(entityManager.find(eq(Account.class), eq(testAccount.getId()))).thenReturn(testAccount);

        // When / Then
        assertThatThrownBy(() -> paymentProcessingService.processPayment(baseTransactionDto))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds for payment in account " + testAccount.getAccountNumber() + ".");

        verify(entityManager, times(1)).find(eq(Account.class), eq(testAccount.getId()));
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    // --- Test processPayment method - Unsupported Type ---
    @Test
    @DisplayName("should throw InvalidTransactionException for unsupported transaction type")
    void processPayment_UnsupportedType() {
        // Given
        baseTransactionDto.setType(TransactionType.TRANSFER); // TRANSFER is not directly handled by this service for balance change
        when(entityManager.find(eq(Account.class), eq(testAccount.getId()))).thenReturn(testAccount);

        // When / Then
        assertThatThrownBy(() -> paymentProcessingService.processPayment(baseTransactionDto))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Unsupported transaction type for payment processing: TRANSFER");

        verify(entityManager, times(1)).find(eq(Account.class), eq(testAccount.getId()));
        verify(entityManager, never()).persist(any(Transaction.class));
    }
}