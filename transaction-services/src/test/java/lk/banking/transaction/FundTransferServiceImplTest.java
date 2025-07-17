package lk.banking.transaction;

import jakarta.persistence.EntityManager;
import lk.banking.core.dto.TransferRequestDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Customer; // Required for Account setup
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.enums.AccountType;
import lk.banking.core.entity.enums.TransactionStatus;
import lk.banking.core.entity.enums.TransactionType;
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.InsufficientFundsException;
import lk.banking.core.exception.InvalidTransactionException;
import lk.banking.core.exception.ValidationException; // Although not thrown directly by service, good to keep in mind

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor; // To capture arguments passed to persist
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FundTransferServiceImpl Unit Tests")
public class FundTransferServiceImplTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private FundTransferServiceImpl fundTransferService;

    // Test Data
    private Customer testCustomer;
    private Account fromAccount;
    private Account toAccount;
    private TransferRequestDto transferRequestDto;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer("John Doe", "john.doe@example.com", "123 Main St", "1234567890");
        testCustomer.setId(1L);

        fromAccount = new Account("FROM123456", AccountType.SAVINGS, BigDecimal.valueOf(1000.00), testCustomer);
        fromAccount.setId(101L);
        fromAccount.setIsActive(true);

        toAccount = new Account("TO987654", AccountType.CURRENT, BigDecimal.valueOf(500.00), testCustomer);
        toAccount.setId(102L);
        toAccount.setIsActive(true);

        transferRequestDto = new TransferRequestDto();
        transferRequestDto.setFromAccountId(fromAccount.getId());
        transferRequestDto.setToAccountId(toAccount.getId());
        transferRequestDto.setAmount(BigDecimal.valueOf(100.00));
    }

    // --- Test transferFunds method ---

    @Test
    @DisplayName("should successfully transfer funds between two active accounts with sufficient funds")
    void transferFunds_Success() {
        // Given
        when(entityManager.find(eq(Account.class), eq(fromAccount.getId()))).thenReturn(fromAccount);
        when(entityManager.find(eq(Account.class), eq(toAccount.getId()))).thenReturn(toAccount);

        BigDecimal initialFromBalance = fromAccount.getBalance();
        BigDecimal initialToBalance = toAccount.getBalance();
        BigDecimal transferAmount = transferRequestDto.getAmount();

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        // When
        Transaction resultTransaction = fundTransferService.transferFunds(transferRequestDto);

        // Then
        assertThat(resultTransaction).isNotNull();
        // The service returns the debit transaction.
        assertThat(resultTransaction.getAccount()).isEqualTo(fromAccount);
        assertThat(resultTransaction.getAmount()).isEqualByComparingTo(transferAmount.negate()); // Debit is negative
        assertThat(resultTransaction.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(resultTransaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);

        // Verify account balances are updated
        assertThat(fromAccount.getBalance()).isEqualByComparingTo(initialFromBalance.subtract(transferAmount));
        assertThat(toAccount.getBalance()).isEqualByComparingTo(initialToBalance.add(transferAmount));

        // Verify that merge was called for both accounts (though not strictly necessary as they are managed entities)
        // and persist was called twice for transactions.
        verify(entityManager, times(2)).find(eq(Account.class), anyLong());
        verify(entityManager, times(2)).persist(transactionCaptor.capture());

        List<Transaction> capturedTransactions = transactionCaptor.getAllValues();
        assertThat(capturedTransactions).hasSize(2);

        // Verify the two captured transactions
        Transaction capturedDebit = capturedTransactions.get(0);
        Transaction capturedCredit = capturedTransactions.get(1);

        assertThat(capturedDebit.getAccount()).isEqualTo(fromAccount);
        assertThat(capturedDebit.getAmount()).isEqualByComparingTo(transferAmount.negate());
        assertThat(capturedDebit.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(capturedDebit.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(capturedDebit.getDescription()).contains(toAccount.getAccountNumber());

        assertThat(capturedCredit.getAccount()).isEqualTo(toAccount);
        assertThat(capturedCredit.getAmount()).isEqualByComparingTo(transferAmount);
        assertThat(capturedCredit.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(capturedCredit.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(capturedCredit.getDescription()).contains(fromAccount.getAccountNumber());
    }

    @Test
    @DisplayName("should throw InvalidTransactionException if transferRequestDto is null")
    void transferFunds_RequestDtoNull() {
        // Given
        TransferRequestDto nullRequestDto = null;

        // When / Then
        assertThatThrownBy(() -> fundTransferService.transferFunds(nullRequestDto))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transfer request or amount cannot be null.");

        verify(entityManager, never()).find(any(), any());
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InvalidTransactionException if transfer amount is null")
    void transferFunds_AmountNull() {
        // Given
        transferRequestDto.setAmount(null);

        // When / Then
        assertThatThrownBy(() -> fundTransferService.transferFunds(transferRequestDto))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transfer request or amount cannot be null.");

        verify(entityManager, never()).find(any(), any());
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InvalidTransactionException if transfer amount is zero or negative")
    void transferFunds_AmountZeroOrNegative() {
        // Given
        transferRequestDto.setAmount(BigDecimal.ZERO);

        // When / Then
        assertThatThrownBy(() -> fundTransferService.transferFunds(transferRequestDto))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transfer amount must be positive.");

        transferRequestDto.setAmount(BigDecimal.valueOf(-50.00));
        assertThatThrownBy(() -> fundTransferService.transferFunds(transferRequestDto))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transfer amount must be positive.");

        verify(entityManager, never()).find(any(), any());
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InvalidTransactionException if source and destination accounts are the same")
    void transferFunds_SameAccount() {
        // Given
        transferRequestDto.setToAccountId(fromAccount.getId()); // Same as fromAccount

        // When / Then
        assertThatThrownBy(() -> fundTransferService.transferFunds(transferRequestDto))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Cannot transfer funds to the same account.");

        verify(entityManager, never()).find(any(), any());
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw AccountNotFoundException if source account is not found")
    void transferFunds_FromAccountNotFound() {
        // Given
        when(entityManager.find(eq(Account.class), eq(fromAccount.getId()))).thenReturn(null);
        when(entityManager.find(eq(Account.class), eq(toAccount.getId()))).thenReturn(toAccount); // Still mock toAccount, but not reached

        // When / Then
        assertThatThrownBy(() -> fundTransferService.transferFunds(transferRequestDto))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Source account with ID " + fromAccount.getId() + " not found.");

        verify(entityManager, times(1)).find(eq(Account.class), eq(fromAccount.getId())); // Only fromAccount checked
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw AccountNotFoundException if destination account is not found")
    void transferFunds_ToAccountNotFound() {
        // Given
        when(entityManager.find(eq(Account.class), eq(fromAccount.getId()))).thenReturn(fromAccount);
        when(entityManager.find(eq(Account.class), eq(toAccount.getId()))).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> fundTransferService.transferFunds(transferRequestDto))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Destination account with ID " + toAccount.getId() + " not found.");

        verify(entityManager, times(1)).find(eq(Account.class), eq(fromAccount.getId()));
        verify(entityManager, times(1)).find(eq(Account.class), eq(toAccount.getId()));
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InvalidTransactionException if source account is inactive")
    void transferFunds_FromAccountInactive() {
        // Given
        fromAccount.setIsActive(false); // Make source account inactive
        when(entityManager.find(eq(Account.class), eq(fromAccount.getId()))).thenReturn(fromAccount);
        when(entityManager.find(eq(Account.class), eq(toAccount.getId()))).thenReturn(toAccount);

        // When / Then
        assertThatThrownBy(() -> fundTransferService.transferFunds(transferRequestDto))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transfer denied: Source account " + fromAccount.getAccountNumber() + " is inactive.");

        verify(entityManager, times(1)).find(eq(Account.class), eq(fromAccount.getId()));
        verify(entityManager, times(1)).find(eq(Account.class), eq(toAccount.getId()));
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InvalidTransactionException if destination account is inactive")
    void transferFunds_ToAccountInactive() {
        // Given
        toAccount.setIsActive(false); // Make destination account inactive
        when(entityManager.find(eq(Account.class), eq(fromAccount.getId()))).thenReturn(fromAccount);
        when(entityManager.find(eq(Account.class), eq(toAccount.getId()))).thenReturn(toAccount);

        // When / Then
        assertThatThrownBy(() -> fundTransferService.transferFunds(transferRequestDto))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transfer denied: Destination account " + toAccount.getAccountNumber() + " is inactive.");

        verify(entityManager, times(1)).find(eq(Account.class), eq(fromAccount.getId()));
        verify(entityManager, times(1)).find(eq(Account.class), eq(toAccount.getId()));
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InsufficientFundsException if source account has insufficient funds")
    void transferFunds_InsufficientFunds() {
        // Given
        transferRequestDto.setAmount(BigDecimal.valueOf(1500.00)); // More than fromAccount's 1000.00 balance
        when(entityManager.find(eq(Account.class), eq(fromAccount.getId()))).thenReturn(fromAccount);
        when(entityManager.find(eq(Account.class), eq(toAccount.getId()))).thenReturn(toAccount);

        // When / Then
        assertThatThrownBy(() -> fundTransferService.transferFunds(transferRequestDto))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds in source account " + fromAccount.getAccountNumber() + ".");

        verify(entityManager, times(1)).find(eq(Account.class), eq(fromAccount.getId()));
        verify(entityManager, times(1)).find(eq(Account.class), eq(toAccount.getId()));
        verify(entityManager, never()).persist(any(Transaction.class));
    }
}