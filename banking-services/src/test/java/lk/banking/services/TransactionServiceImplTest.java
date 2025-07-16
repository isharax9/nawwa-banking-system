package lk.banking.services;

import lk.banking.core.dto.TransactionDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Customer;
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.User;
import lk.banking.core.entity.enums.AccountType;
import lk.banking.core.entity.enums.TransactionStatus;
import lk.banking.core.entity.enums.TransactionType;
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.InsufficientFundsException;
import lk.banking.core.exception.InvalidTransactionException;
import lk.banking.core.exception.UserNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Answers; // For deep stubbing

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionServiceImpl Unit Tests")
public class TransactionServiceImplTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    // Mocks for TypedQuery objects.
    // mockAccountTypedQuery field is removed as it caused conflicts with sequential createQuery calls.
    // Instead, local mocks will be created for 'from' and 'to' accounts in transferFunds tests.
    @Mock
    private TypedQuery<Customer> mockCustomerTypedQuery; // Used for getTransactionsByUser (customer lookup)
    @Mock
    private TypedQuery<Transaction> mockTransactionTypedQuery; // Used for getTransactionsByAccount, getAllTransactions, getTransactionsByUser (transaction lookup)


    private Customer testCustomer;
    private User testUser;
    private Account testAccountSavings;
    private Account testAccountCurrent;
    private Transaction testDepositTransaction;
    private Transaction testWithdrawalTransaction;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer("John Doe", "john.doe@example.com", "123 Main St", "1234567890");
        testCustomer.setId(1L);

        // Note: For User, you might want to add roles, but for this service's tests, it's not critical.
        testUser = new User("johndoe", "hashed_password", "john.doe@example.com", "1234567890", Collections.emptySet());
        testUser.setId(10L);

        testAccountSavings = new Account("100000000001", AccountType.SAVINGS, BigDecimal.valueOf(1000.00), testCustomer);
        testAccountSavings.setId(1L);
        testAccountSavings.setIsActive(true);
        testAccountSavings.setCreatedAt(LocalDateTime.now().minusMonths(3));
        testAccountSavings.setLastInterestAppliedDate(LocalDateTime.now().minusDays(1));

        testAccountCurrent = new Account("100000000002", AccountType.CURRENT, BigDecimal.valueOf(500.00), testCustomer);
        testAccountCurrent.setCreatedAt(LocalDateTime.now().minusMonths(2));
        testAccountCurrent.setId(2L);
        testAccountCurrent.setIsActive(true);

        testDepositTransaction = new Transaction(testAccountSavings, BigDecimal.valueOf(100), TransactionType.DEPOSIT, TransactionStatus.COMPLETED, LocalDateTime.now().minusDays(5), "Initial Deposit");
        testDepositTransaction.setId(100L);

        testWithdrawalTransaction = new Transaction(testAccountSavings, BigDecimal.valueOf(50), TransactionType.WITHDRAWAL, TransactionStatus.COMPLETED, LocalDateTime.now().minusDays(4), "ATM Withdrawal");
        testWithdrawalTransaction.setId(101L);
    }

    // --- Test createTransaction method ---
    @Test
    @DisplayName("should create a new transaction successfully")
    void createTransaction_Success() {
        // Given
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.setAccountId(testAccountSavings.getId());
        transactionDto.setAmount(BigDecimal.valueOf(100.00));
        transactionDto.setType(TransactionType.DEPOSIT);
        transactionDto.setDescription("Online Deposit");

        when(entityManager.find(eq(Account.class), eq(testAccountSavings.getId()))).thenReturn(testAccountSavings);

        // When
        Transaction createdTransaction = transactionService.createTransaction(transactionDto);

        // Then
        assertThat(createdTransaction).isNotNull();
        assertThat(createdTransaction.getAccount()).isEqualTo(testAccountSavings);
        assertThat(createdTransaction.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(createdTransaction.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(createdTransaction.getStatus()).isEqualTo(TransactionStatus.PENDING); // As per service impl
        assertThat(createdTransaction.getTimestamp()).isNotNull();
        verify(entityManager, times(1)).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw AccountNotFoundException when creating transaction for non-existent account")
    void createTransaction_AccountNotFound() {
        // Given
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.setAccountId(99L); // Non-existent account
        transactionDto.setAmount(BigDecimal.valueOf(50.00));
        transactionDto.setType(TransactionType.WITHDRAWAL);
        transactionDto.setDescription("Failed Withdrawal");

        when(entityManager.find(eq(Account.class), eq(99L))).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> transactionService.createTransaction(transactionDto))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account with ID 99 not found.");
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    // --- Test getTransactionById method ---
    @Test
    @DisplayName("should retrieve transaction by ID successfully")
    void getTransactionById_Success() {
        // Given
        when(entityManager.find(eq(Transaction.class), eq(testDepositTransaction.getId()))).thenReturn(testDepositTransaction);

        // When
        Transaction foundTransaction = transactionService.getTransactionById(testDepositTransaction.getId());

        // Then
        assertThat(foundTransaction).isNotNull();
        assertThat(foundTransaction.getId()).isEqualTo(testDepositTransaction.getId());
        assertThat(foundTransaction.getAmount()).isEqualByComparingTo(testDepositTransaction.getAmount());
        verify(entityManager, times(1)).find(eq(Transaction.class), eq(testDepositTransaction.getId()));
    }

    @Test
    @DisplayName("should throw InvalidTransactionException when getting non-existent transaction by ID")
    void getTransactionById_NotFound() {
        // Given
        Long nonExistentId = 999L;
        when(entityManager.find(eq(Transaction.class), eq(nonExistentId))).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> transactionService.getTransactionById(nonExistentId))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transaction with ID " + nonExistentId + " not found.");
        verify(entityManager, times(1)).find(eq(Transaction.class), eq(nonExistentId));
    }

    // --- Test getTransactionsByAccount method ---
    @Test
    @DisplayName("should retrieve transactions for a given account successfully")
    void getTransactionsByAccount_Success() {
        // Given
        List<Transaction> transactions = Arrays.asList(testDepositTransaction, testWithdrawalTransaction);

        // Mock the TypedQuery chain
        when(entityManager.createQuery(anyString(), eq(Transaction.class))).thenReturn(mockTransactionTypedQuery);
        when(mockTransactionTypedQuery.setParameter(anyString(), eq(testAccountSavings.getId()))).thenReturn(mockTransactionTypedQuery);
        when(mockTransactionTypedQuery.getResultList()).thenReturn(transactions);

        // When
        List<Transaction> foundTransactions = transactionService.getTransactionsByAccount(testAccountSavings.getId());

        // Then
        assertThat(foundTransactions).containsExactlyInAnyOrder(testDepositTransaction, testWithdrawalTransaction);
        verify(entityManager, times(1)).createQuery(anyString(), eq(Transaction.class));
        verify(mockTransactionTypedQuery, times(1)).setParameter(anyString(), eq(testAccountSavings.getId()));
        verify(mockTransactionTypedQuery, times(1)).getResultList();
    }

    @Test
    @DisplayName("should return empty list when no transactions found for account")
    void getTransactionsByAccount_NoTransactions() {
        // Given
        when(entityManager.createQuery(anyString(), eq(Transaction.class))).thenReturn(mockTransactionTypedQuery);
        when(mockTransactionTypedQuery.setParameter(anyString(), eq(testAccountSavings.getId()))).thenReturn(mockTransactionTypedQuery);
        when(mockTransactionTypedQuery.getResultList()).thenReturn(Collections.emptyList());

        // When
        List<Transaction> foundTransactions = transactionService.getTransactionsByAccount(testAccountSavings.getId());

        // Then
        assertThat(foundTransactions).isEmpty();
        verify(entityManager, times(1)).createQuery(anyString(), eq(Transaction.class));
        verify(mockTransactionTypedQuery, times(1)).setParameter(anyString(), eq(testAccountSavings.getId()));
        verify(mockTransactionTypedQuery, times(1)).getResultList();
    }

    // --- Test getAllTransactions method ---
    @Test
    @DisplayName("should retrieve all transactions successfully")
    void getAllTransactions_Success() {
        // Given
        List<Transaction> allTransactions = Arrays.asList(testDepositTransaction, testWithdrawalTransaction);

        // Mock the TypedQuery chain
        when(entityManager.createQuery(anyString(), eq(Transaction.class))).thenReturn(mockTransactionTypedQuery);
        when(mockTransactionTypedQuery.getResultList()).thenReturn(allTransactions);

        // When
        List<Transaction> foundTransactions = transactionService.getAllTransactions();

        // Then
        assertThat(foundTransactions).containsExactlyInAnyOrder(testDepositTransaction, testWithdrawalTransaction);
        verify(entityManager, times(1)).createQuery(anyString(), eq(Transaction.class));
        verify(mockTransactionTypedQuery, times(1)).getResultList();
    }

    @Test
    @DisplayName("should return empty list when no transactions found in getAllTransactions")
    void getAllTransactions_NoTransactions() {
        // Given
        when(entityManager.createQuery(anyString(), eq(Transaction.class))).thenReturn(mockTransactionTypedQuery);
        when(mockTransactionTypedQuery.getResultList()).thenReturn(Collections.emptyList());

        // When
        List<Transaction> foundTransactions = transactionService.getAllTransactions();

        // Then
        assertThat(foundTransactions).isEmpty();
        verify(entityManager, times(1)).createQuery(anyString(), eq(Transaction.class));
        verify(mockTransactionTypedQuery, times(1)).getResultList();
    }

    // --- Test transferFunds method ---
    @Test
    @DisplayName("should transfer funds successfully between two accounts")
    void transferFunds_Success() {
        // Given
        Long performingUserId = 10L;
        String fromAccNum = testAccountSavings.getAccountNumber();
        String toAccNum = testAccountCurrent.getAccountNumber();
        BigDecimal transferAmount = BigDecimal.valueOf(200.00);

        // Create specific mocks for the two Account queries in getAccountByNumber helper method
        TypedQuery<Account> fromAccountQuery = mock(TypedQuery.class);
        TypedQuery<Account> toAccountQuery = mock(TypedQuery.class);

        // Mock sequential calls to entityManager.createQuery for Account.class
        when(entityManager.createQuery(anyString(), eq(Account.class)))
                .thenReturn(fromAccountQuery) // First call for fromAccount
                .thenReturn(toAccountQuery);  // Second call for toAccount

        // Stub the fromAccountQuery chain
        when(fromAccountQuery.setParameter(eq("num"), eq(fromAccNum))).thenReturn(fromAccountQuery);
        when(fromAccountQuery.getSingleResult()).thenReturn(testAccountSavings);

        // Stub the toAccountQuery chain
        when(toAccountQuery.setParameter(eq("num"), eq(toAccNum))).thenReturn(toAccountQuery);
        when(toAccountQuery.getSingleResult()).thenReturn(testAccountCurrent);

        BigDecimal initialFromBalance = testAccountSavings.getBalance();
        BigDecimal initialToBalance = testAccountCurrent.getBalance();

        // When
        boolean result = transactionService.transferFunds(performingUserId, fromAccNum, toAccNum, transferAmount);

        // Then
        assertThat(result).isTrue();
        assertThat(testAccountSavings.getBalance()).isEqualByComparingTo(initialFromBalance.subtract(transferAmount));
        assertThat(testAccountCurrent.getBalance()).isEqualByComparingTo(initialToBalance.add(transferAmount));

        verify(entityManager, times(2)).merge(any(Account.class)); // For fromAccount and toAccount
        verify(entityManager, times(2)).persist(any(Transaction.class)); // One debit, one credit transaction
    }

    @Test
    @DisplayName("should throw InvalidTransactionException for non-positive transfer amount")
    void transferFunds_NonPositiveAmount() {
        // Given
        Long performingUserId = 10L;
        String fromAccNum = testAccountSavings.getAccountNumber();
        String toAccNum = testAccountCurrent.getAccountNumber();

        // When / Then
        assertThatThrownBy(() -> transactionService.transferFunds(performingUserId, fromAccNum, toAccNum, BigDecimal.ZERO))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transfer amount must be positive.");

        assertThatThrownBy(() -> transactionService.transferFunds(performingUserId, fromAccNum, toAccNum, BigDecimal.valueOf(-10.00)))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transfer amount must be positive.");

        assertThatThrownBy(() -> transactionService.transferFunds(performingUserId, fromAccNum, toAccNum, null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Transfer amount must be positive.");

        verify(entityManager, never()).merge(any(Account.class));
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InvalidTransactionException when transferring to the same account")
    void transferFunds_SameAccount() {
        // Given
        Long performingUserId = 10L;
        String accNum = testAccountSavings.getAccountNumber();
        BigDecimal transferAmount = BigDecimal.valueOf(100.00);

        // When / Then
        assertThatThrownBy(() -> transactionService.transferFunds(performingUserId, accNum, accNum, transferAmount))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Cannot transfer funds to the same account.");

        verify(entityManager, never()).merge(any(Account.class));
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw AccountNotFoundException when fromAccount is not found")
    void transferFunds_FromAccountNotFound() {
        // Given
        Long performingUserId = 10L;
        String fromAccNum = "nonExistentFrom";
        String toAccNum = testAccountCurrent.getAccountNumber();
        BigDecimal transferAmount = BigDecimal.valueOf(100.00);

        // Create specific mock for the fromAccount query
        TypedQuery<Account> fromAccountQuery = mock(TypedQuery.class);

        // Mock the call to entityManager.createQuery for Account.class to return fromAccountQuery
        when(entityManager.createQuery(anyString(), eq(Account.class)))
                .thenReturn(fromAccountQuery);

        // Stub the fromAccountQuery chain to throw NoResultException
        when(fromAccountQuery.setParameter(eq("num"), eq(fromAccNum))).thenReturn(fromAccountQuery);
        when(fromAccountQuery.getSingleResult()).thenThrow(NoResultException.class);


        // When / Then
        assertThatThrownBy(() -> transactionService.transferFunds(performingUserId, fromAccNum, toAccNum, transferAmount))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("One of the accounts involved in transfer was not found: Account with number nonExistentFrom not found.");

        verify(entityManager, never()).merge(any(Account.class));
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw AccountNotFoundException when toAccount is not found")
    void transferFunds_ToAccountNotFound() {
        // Given
        Long performingUserId = 10L;
        String fromAccNum = testAccountSavings.getAccountNumber();
        String toAccNum = "nonExistentTo";
        BigDecimal transferAmount = BigDecimal.valueOf(100.00);

        // Create specific mocks for the two Account queries in getAccountByNumber helper method
        TypedQuery<Account> fromAccountQuery = mock(TypedQuery.class);
        TypedQuery<Account> toAccountQuery = mock(TypedQuery.class);

        // Mock sequential calls to entityManager.createQuery for Account.class
        when(entityManager.createQuery(anyString(), eq(Account.class)))
                .thenReturn(fromAccountQuery) // First call for fromAccount
                .thenReturn(toAccountQuery);  // Second call for toAccount

        // Stub the fromAccountQuery chain
        when(fromAccountQuery.setParameter(eq("num"), eq(fromAccNum))).thenReturn(fromAccountQuery);
        when(fromAccountQuery.getSingleResult()).thenReturn(testAccountSavings);

        // Stub the toAccountQuery chain to throw NoResultException
        when(toAccountQuery.setParameter(eq("num"), eq(toAccNum))).thenReturn(toAccountQuery);
        when(toAccountQuery.getSingleResult()).thenThrow(NoResultException.class);


        // When / Then
        assertThatThrownBy(() -> transactionService.transferFunds(performingUserId, fromAccNum, toAccNum, transferAmount))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("One of the accounts involved in transfer was not found: Account with number nonExistentTo not found.");

        verify(entityManager, never()).merge(any(Account.class));
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InsufficientFundsException when fromAccount has insufficient funds")
    void transferFunds_InsufficientFunds() {
        // Given
        Long performingUserId = 10L;
        String fromAccNum = testAccountSavings.getAccountNumber();
        String toAccNum = testAccountCurrent.getAccountNumber();
        BigDecimal transferAmount = BigDecimal.valueOf(1500.00); // More than initial 1000.00

        // Create specific mocks for the two Account queries
        TypedQuery<Account> fromAccountQuery = mock(TypedQuery.class);
        TypedQuery<Account> toAccountQuery = mock(TypedQuery.class);

        // Mock sequential calls to entityManager.createQuery for Account.class
        when(entityManager.createQuery(anyString(), eq(Account.class)))
                .thenReturn(fromAccountQuery) // First call for fromAccount
                .thenReturn(toAccountQuery);  // Second call for toAccount

        // Stub the fromAccountQuery chain
        when(fromAccountQuery.setParameter(eq("num"), eq(fromAccNum))).thenReturn(fromAccountQuery);
        when(fromAccountQuery.getSingleResult()).thenReturn(testAccountSavings); // Returns account with 1000.00

        // Stub the toAccountQuery chain
        when(toAccountQuery.setParameter(eq("num"), eq(toAccNum))).thenReturn(toAccountQuery);
        when(toAccountQuery.getSingleResult()).thenReturn(testAccountCurrent);

        // When / Then
        assertThatThrownBy(() -> transactionService.transferFunds(performingUserId, fromAccNum, toAccNum, transferAmount))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Account " + fromAccNum + " has insufficient funds for transfer.");

        verify(entityManager, never()).merge(any(Account.class));
        verify(entityManager, never()).persist(any(Transaction.class));
    }

    // --- Test getTransactionsByAccountAndDateRange method ---
    @Test
    @DisplayName("should return empty list for getTransactionsByAccountAndDateRange as it's not fully implemented")
    void getTransactionsByAccountAndDateRange_NotImplemented() {
        // Given
        Long accountId = 1L;
        LocalDateTime from = LocalDateTime.now().minusDays(10);
        LocalDateTime to = LocalDateTime.now();
        boolean includeArchived = false;

        // When
        List<Transaction> result = transactionService.getTransactionsByAccountAndDateRange(accountId, from, to, includeArchived);

        // Then
        assertThat(result).isEmpty();
        // No interaction with EntityManager is expected given its current implementation returning List.of()
        // verifyNoInteractions(entityManager); // This could be too strict if any mock interaction occurs incidentally
        verify(entityManager, never()).createQuery(anyString(), any()); // More specific check
    }

    // --- Test getTransactionsByUser method ---
    @Test
    @DisplayName("should retrieve transactions for a user successfully")
    void getTransactionsByUser_Success() {
        // Given
        Long userId = testUser.getId();
        List<Account> userAccounts = Arrays.asList(testAccountSavings, testAccountCurrent);
        List<Transaction> transactions = Arrays.asList(testDepositTransaction, testWithdrawalTransaction);

        // Mock em.find for User
        when(entityManager.find(eq(User.class), eq(userId))).thenReturn(testUser);

        // Mock query for Customer by email
        when(entityManager.createQuery(anyString(), eq(Customer.class))).thenReturn(mockCustomerTypedQuery);
        when(mockCustomerTypedQuery.setParameter(anyString(), eq(testUser.getEmail()))).thenReturn(mockCustomerTypedQuery);
        when(mockCustomerTypedQuery.getSingleResult()).thenReturn(testCustomer);

        // Mock query for Accounts by customer ID
        // This is the second call to createQuery for a TypedQuery<Account> (first one would be for a different class if this were the overall first)
        TypedQuery<Account> mockAccountsByCustomerTypedQuery = mock(TypedQuery.class); // Not deep stub
        when(entityManager.createQuery(anyString(), eq(Account.class))) // This will match the query for accounts by customer ID
                .thenReturn(mockAccountsByCustomerTypedQuery);
        when(mockAccountsByCustomerTypedQuery.setParameter(anyString(), eq(testCustomer.getId()))).thenReturn(mockAccountsByCustomerTypedQuery);
        when(mockAccountsByCustomerTypedQuery.getResultList()).thenReturn(userAccounts);


        // Mock query for Transactions by account IDs
        // This is the call to createQuery with Transaction.class
        when(entityManager.createQuery(anyString(), eq(Transaction.class))).thenReturn(mockTransactionTypedQuery);
        when(mockTransactionTypedQuery.setParameter(anyString(), anyList())).thenReturn(mockTransactionTypedQuery);
        when(mockTransactionTypedQuery.getResultList()).thenReturn(transactions);


        // When
        List<Transaction> foundTransactions = transactionService.getTransactionsByUser(userId);

        // Then
        assertThat(foundTransactions).containsExactlyInAnyOrder(testDepositTransaction, testWithdrawalTransaction);
        verify(entityManager, times(1)).find(eq(User.class), eq(userId));
        verify(entityManager, times(1)).createQuery(anyString(), eq(Customer.class));
        verify(entityManager, times(1)).createQuery(anyString(), eq(Account.class));
        verify(entityManager, times(1)).createQuery(anyString(), eq(Transaction.class));
    }

    @Test
    @DisplayName("should throw UserNotFoundException when getting transactions for non-existent user")
    void getTransactionsByUser_UserNotFound() {
        // Given
        Long nonExistentUserId = 99L;
        when(entityManager.find(eq(User.class), eq(nonExistentUserId))).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> transactionService.getTransactionsByUser(nonExistentUserId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User with ID " + nonExistentUserId + " not found.");
        verify(entityManager, never()).createQuery(anyString(), any());
    }

    @Test
    @DisplayName("should return empty list when no customer linked to user email in getTransactionsByUser")
    void getTransactionsByUser_NoCustomerLinked() {
        // Given
        Long userId = testUser.getId();
        when(entityManager.find(eq(User.class), eq(userId))).thenReturn(testUser);

        // Mock query for Customer by email to throw NoResultException
        when(entityManager.createQuery(anyString(), eq(Customer.class))).thenReturn(mockCustomerTypedQuery);
        when(mockCustomerTypedQuery.setParameter(anyString(), anyString())).thenReturn(mockCustomerTypedQuery);
        when(mockCustomerTypedQuery.getSingleResult()).thenThrow(NoResultException.class);

        // When
        List<Transaction> foundTransactions = transactionService.getTransactionsByUser(userId);

        // Then
        assertThat(foundTransactions).isEmpty();
        verify(entityManager, times(1)).find(eq(User.class), eq(userId));
        verify(entityManager, times(1)).createQuery(anyString(), eq(Customer.class));
        verify(entityManager, never()).createQuery(anyString(), eq(Account.class)); // Should not proceed to account query
    }

    @Test
    @DisplayName("should return empty list when customer found but no accounts in getTransactionsByUser")
    void getTransactionsByUser_NoAccountsForCustomer() {
        // Given
        Long userId = testUser.getId();
        when(entityManager.find(eq(User.class), eq(userId))).thenReturn(testUser);

        // Mock query for Customer by email
        when(entityManager.createQuery(anyString(), eq(Customer.class))).thenReturn(mockCustomerTypedQuery);
        when(mockCustomerTypedQuery.setParameter(anyString(), anyString())).thenReturn(mockCustomerTypedQuery);
        when(mockCustomerTypedQuery.getSingleResult()).thenReturn(testCustomer);

        // Mock query for Accounts by customer ID to return empty list
        // This is the second call to createQuery for a TypedQuery<Account>
        TypedQuery<Account> mockAccountsByCustomerTypedQuery = mock(TypedQuery.class); // Not deep stub
        when(entityManager.createQuery(anyString(), eq(Account.class))) // This will match the query for accounts by customer ID
                .thenReturn(mockAccountsByCustomerTypedQuery);
        when(mockAccountsByCustomerTypedQuery.setParameter(anyString(), eq(testCustomer.getId()))).thenReturn(mockAccountsByCustomerTypedQuery);
        when(mockAccountsByCustomerTypedQuery.getResultList()).thenReturn(Collections.emptyList());

        // When
        List<Transaction> foundTransactions = transactionService.getTransactionsByUser(userId);

        // Then
        assertThat(foundTransactions).isEmpty();
        verify(entityManager, times(1)).find(eq(User.class), eq(userId));
        verify(entityManager, times(1)).createQuery(anyString(), eq(Customer.class));
        verify(entityManager, times(1)).createQuery(anyString(), eq(Account.class));
        verify(entityManager, never()).createQuery(anyString(), eq(Transaction.class)); // Should not proceed to transaction query
    }

    @Test
    @DisplayName("should return empty list when accounts found but no transactions in getTransactionsByUser")
    void getTransactionsByUser_NoTransactionsForAccounts() {
        // Given
        Long userId = testUser.getId();
        List<Account> userAccounts = Arrays.asList(testAccountSavings, testAccountCurrent);
        when(entityManager.find(eq(User.class), eq(userId))).thenReturn(testUser);

        // Mock query for Customer by email
        when(entityManager.createQuery(anyString(), eq(Customer.class))).thenReturn(mockCustomerTypedQuery);
        when(mockCustomerTypedQuery.setParameter(anyString(), anyString())).thenReturn(mockCustomerTypedQuery);
        when(mockCustomerTypedQuery.getSingleResult()).thenReturn(testCustomer);

        // Mock query for Accounts by customer ID
        // This is the second call to createQuery for a TypedQuery<Account>
        TypedQuery<Account> mockAccountsByCustomerTypedQuery = mock(TypedQuery.class); // Not deep stub
        when(entityManager.createQuery(anyString(), eq(Account.class))) // This will match the query for accounts by customer ID
                .thenReturn(mockAccountsByCustomerTypedQuery);
        when(mockAccountsByCustomerTypedQuery.setParameter(anyString(), eq(testCustomer.getId()))).thenReturn(mockAccountsByCustomerTypedQuery);
        when(mockAccountsByCustomerTypedQuery.getResultList()).thenReturn(userAccounts);

        // Mock query for Transactions by account IDs to return empty list
        // This is the call to createQuery with Transaction.class
        when(entityManager.createQuery(anyString(), eq(Transaction.class))).thenReturn(mockTransactionTypedQuery);
        when(mockTransactionTypedQuery.setParameter(anyString(), anyList())).thenReturn(mockTransactionTypedQuery);
        when(mockTransactionTypedQuery.getResultList()).thenReturn(Collections.emptyList());

        // When
        List<Transaction> foundTransactions = transactionService.getTransactionsByUser(userId);

        // Then
        assertThat(foundTransactions).isEmpty();
        verify(entityManager, times(1)).find(eq(User.class), eq(userId));
        verify(entityManager, times(1)).createQuery(anyString(), eq(Customer.class));
        verify(entityManager, times(1)).createQuery(anyString(), eq(Account.class));
        verify(entityManager, times(1)).createQuery(anyString(), eq(Transaction.class)); // Query for transactions is made
    }
}