package lk.banking.services;

import lk.banking.core.dto.AccountDto;
import lk.banking.core.entity.*;
import lk.banking.core.entity.enums.AccountType;
import lk.banking.core.entity.enums.UserRole;
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.CustomerNotFoundException;
import lk.banking.core.exception.InvalidTransactionException;
import lk.banking.core.exception.UserNotFoundException;
import lk.banking.core.exception.ValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountServiceImpl Unit Tests")
public class AccountServiceImplTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AccountServiceImpl accountService;

    // Mocks for TypedQuery that are used in multiple queries (e.g., uniqueness checks, getAll methods)
    @Mock
    private TypedQuery<Account> mockAccountTypedQuery;
    @Mock
    private TypedQuery<Customer> mockCustomerTypedQuery;
    @Mock
    private TypedQuery<User> mockUserTypedQuery;


    private Customer testCustomer;
    private User testUser;
    private Account testAccountSavings;
    private Account testAccountCurrent;
    private Customer accountDumpCustomer;

    @BeforeEach
    void setUp() {
        // Initialize common test data
        testCustomer = new Customer("John Doe", "john.doe@example.com", "123 Main St", "1234567890");
        testCustomer.setId(1L);

        // For User, create a Set of Role objects
        Set<Role> userRoles = new HashSet<>();
        userRoles.add(new Role(UserRole.CUSTOMER)); // Assume Role constructor takes UserRole
        testUser = new User("johndoe", "hashed_password", "john.doe@example.com", "1234567890", userRoles);
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

        accountDumpCustomer = new Customer("System Dump Account Holder", "account.dump@bank.com", "System Archive Location", "0000000000");
        accountDumpCustomer.setId(999L); // ID for dump customer
    }

    // --- Test createAccount method ---
    @Test
    @DisplayName("should create a new account successfully")
    void createAccount_Success() {
        // Given
        AccountDto accountDto = new AccountDto();
        accountDto.setCustomerId(testCustomer.getId());
        accountDto.setType(AccountType.SAVINGS);
        accountDto.setBalance(BigDecimal.valueOf(100.00));

        // Mock dependencies specific to this test
        when(entityManager.find(eq(Customer.class), eq(testCustomer.getId()))).thenReturn(testCustomer);

        // Mock Account number generation uniqueness check
        // when createQuery is called for Account and setParameter, then return a mock query.
        // For getSingleResult(), throw NoResultException to simulate uniqueness.
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.setParameter(anyString(), anyString())).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.getSingleResult()).thenThrow(NoResultException.class);

        // When
        Account createdAccount = accountService.createAccount(accountDto);

        // Then
        assertThat(createdAccount).isNotNull();
        assertThat(createdAccount.getCustomer()).isEqualTo(testCustomer); // Verify customer object is set
        assertThat(createdAccount.getType()).isEqualTo(AccountType.SAVINGS);
        assertThat(createdAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(createdAccount.getAccountNumber()).isNotNull().hasSize(12);
        verify(entityManager, times(1)).persist(any(Account.class));
    }

    @Test
    @DisplayName("should throw CustomerNotFoundException when creating account for non-existent customer")
    void createAccount_CustomerNotFound() {
        // Given
        AccountDto accountDto = new AccountDto();
        accountDto.setCustomerId(99L);
        accountDto.setType(AccountType.CURRENT);
        accountDto.setBalance(BigDecimal.valueOf(50.00));

        when(entityManager.find(eq(Customer.class), eq(99L))).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> accountService.createAccount(accountDto))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("Customer with ID 99 not found.");
        verify(entityManager, never()).persist(any(Account.class));
    }

    @Test
    @DisplayName("should throw ValidationException when creating account with negative balance")
    void createAccount_NegativeBalance() {
        // Given
        AccountDto accountDto = new AccountDto();
        accountDto.setCustomerId(testCustomer.getId());
        accountDto.setType(AccountType.SAVINGS);
        accountDto.setBalance(BigDecimal.valueOf(-10.00));

        // Mock customer existence for the validation to proceed to balance check
        when(entityManager.find(eq(Customer.class), eq(testCustomer.getId()))).thenReturn(testCustomer);


        // When / Then
        assertThatThrownBy(() -> accountService.createAccount(accountDto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Initial balance must be a positive or zero amount.");
        verify(entityManager, never()).persist(any(Account.class));
    }

    // --- Test getAccountById method ---
    @Test
    @DisplayName("should retrieve account by ID successfully")
    void getAccountById_Success() {
        // Given
        // Mock the createQuery for getAccountById (since it uses a query with JOIN FETCH)
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.setParameter(anyString(), anyLong())).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.getSingleResult()).thenReturn(testAccountSavings);

        // When
        Account foundAccount = accountService.getAccountById(testAccountSavings.getId());

        // Then
        assertThat(foundAccount).isNotNull();
        assertThat(foundAccount.getId()).isEqualTo(testAccountSavings.getId());
        assertThat(foundAccount.getAccountNumber()).isEqualTo(testAccountSavings.getAccountNumber());
        assertThat(foundAccount.getCustomer()).isEqualTo(testCustomer); // Verify customer is eagerly fetched
    }

    @Test
    @DisplayName("should throw AccountNotFoundException when getting non-existent account by ID")
    void getAccountById_NotFound() {
        // Given
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.setParameter(anyString(), anyLong())).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.getSingleResult()).thenThrow(NoResultException.class); // Simulate not found

        // When / Then
        assertThatThrownBy(() -> accountService.getAccountById(99L))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account with ID 99 not found.");
    }

    // --- Test deactivateAccount method ---
    @Test
    @DisplayName("should deactivate an active account successfully")
    void deactivateAccount_Success() {
        // Given
        testAccountSavings.setIsActive(true); // Ensure it's active
        when(entityManager.find(eq(Account.class), eq(testAccountSavings.getId()))).thenReturn(testAccountSavings);

        // When
        boolean result = accountService.deactivateAccount(testAccountSavings.getId());

        // Then
        assertThat(result).isTrue();
        assertThat(testAccountSavings.getIsActive()).isFalse();
        // No verify(entityManager).merge because it's a managed entity.
    }

    @Test
    @DisplayName("should return true if account is already inactive")
    void deactivateAccount_AlreadyInactive() {
        // Given
        testAccountSavings.setIsActive(false); // Already inactive
        when(entityManager.find(eq(Account.class), eq(testAccountSavings.getId()))).thenReturn(testAccountSavings);

        // When
        boolean result = accountService.deactivateAccount(testAccountSavings.getId());

        // Then
        assertThat(result).isTrue();
        assertThat(testAccountSavings.getIsActive()).isFalse();
        verify(entityManager, never()).merge(any(Account.class));
    }

    @Test
    @DisplayName("should throw AccountNotFoundException when deactivating non-existent account")
    void deactivateAccount_NotFound() {
        // Given
        when(entityManager.find(eq(Account.class), anyLong())).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> accountService.deactivateAccount(99L))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account with ID 99 not found for deactivation.");
    }

    // --- Test activateAccount method ---
    @Test
    @DisplayName("should activate an inactive account successfully")
    void activateAccount_Success() {
        // Given
        testAccountSavings.setIsActive(false); // Start as inactive
        when(entityManager.find(eq(Account.class), eq(testAccountSavings.getId()))).thenReturn(testAccountSavings);

        // When
        boolean result = accountService.activateAccount(testAccountSavings.getId());

        // Then
        assertThat(result).isTrue();
        assertThat(testAccountSavings.getIsActive()).isTrue(); // Verify status changed
    }

    @Test
    @DisplayName("should return true if account is already active")
    void activateAccount_AlreadyActive() {
        // Given
        testAccountSavings.setIsActive(true); // Already active
        when(entityManager.find(eq(Account.class), eq(testAccountSavings.getId()))).thenReturn(testAccountSavings);

        // When
        boolean result = accountService.activateAccount(testAccountSavings.getId());

        // Then
        assertThat(result).isTrue();
        assertThat(testAccountSavings.getIsActive()).isTrue(); // Status remains true
    }

    @Test
    @DisplayName("should throw AccountNotFoundException when activating non-existent account")
    void activateAccount_NotFound() {
        // Given
        when(entityManager.find(eq(Account.class), anyLong())).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> accountService.activateAccount(99L))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account with ID 99 not found for activation.");
    }

    // --- Test changeAccountType method ---
    @Test
    @DisplayName("should change account type successfully")
    void changeAccountType_Success() {
        // Given
        testAccountSavings.setType(AccountType.SAVINGS);
        AccountType newType = AccountType.CURRENT;
        when(entityManager.find(eq(Account.class), eq(testAccountSavings.getId()))).thenReturn(testAccountSavings);

        // When
        Account updatedAccount = accountService.changeAccountType(testAccountSavings.getId(), newType);

        // Then
        assertThat(updatedAccount).isNotNull();
        assertThat(updatedAccount.getType()).isEqualTo(newType);
    }

    @Test
    @DisplayName("should return same account if new type is identical")
    void changeAccountType_SameType() {
        // Given
        testAccountSavings.setType(AccountType.SAVINGS);
        AccountType newType = AccountType.SAVINGS;
        when(entityManager.find(eq(Account.class), eq(testAccountSavings.getId()))).thenReturn(testAccountSavings);

        // When
        Account updatedAccount = accountService.changeAccountType(testAccountSavings.getId(), newType);

        // Then
        assertThat(updatedAccount).isSameAs(testAccountSavings); // Should be the same instance
        assertThat(updatedAccount.getType()).isEqualTo(newType);
    }

    @Test
    @DisplayName("should throw AccountNotFoundException when changing type of non-existent account")
    void changeAccountType_NotFound() {
        // Given
        when(entityManager.find(eq(Account.class), anyLong())).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> accountService.changeAccountType(99L, AccountType.LOAN))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account with ID 99 not found for type change.");
    }

    // --- Test getAccountByNumber method ---
    @Test
    @DisplayName("should retrieve account by number successfully")
    void getAccountByNumber_Success() {
        // Given
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.setParameter(anyString(), anyString())).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.getSingleResult()).thenReturn(testAccountSavings);

        // When
        Account foundAccount = accountService.getAccountByNumber(testAccountSavings.getAccountNumber());

        // Then
        assertThat(foundAccount).isNotNull();
        assertThat(foundAccount.getAccountNumber()).isEqualTo(testAccountSavings.getAccountNumber());
    }

    @Test
    @DisplayName("should throw AccountNotFoundException when getting non-existent account by number")
    void getAccountByNumber_NotFound() {
        // Given
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.setParameter(anyString(), anyString())).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.getSingleResult()).thenThrow(NoResultException.class);

        // When / Then
        assertThatThrownBy(() -> accountService.getAccountByNumber("nonexistent"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account with number nonexistent not found.");
    }

    // --- Test getAccountsByCustomer method ---
    @Test
    @DisplayName("should retrieve accounts by customer successfully")
    void getAccountsByCustomer_Success() {
        // Given
        List<Account> accounts = Arrays.asList(testAccountSavings, testAccountCurrent);
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.setParameter(anyString(), anyLong())).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.getResultList()).thenReturn(accounts);

        // When
        List<Account> foundAccounts = accountService.getAccountsByCustomer(testCustomer.getId());

        // Then
        assertThat(foundAccounts).containsExactlyInAnyOrder(testAccountSavings, testAccountCurrent);
    }

    @Test
    @DisplayName("should return empty list when no accounts found for customer")
    void getAccountsByCustomer_NoAccounts() {
        // Given
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.setParameter(anyString(), anyLong())).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.getResultList()).thenReturn(Collections.emptyList());

        // When
        List<Account> foundAccounts = accountService.getAccountsByCustomer(testCustomer.getId());

        // Then
        assertThat(foundAccounts).isEmpty();
    }

    // --- Test getAllAccounts method ---
    @Test
    @DisplayName("should retrieve all accounts successfully")
    void getAllAccounts_Success() {
        // Given
        List<Account> allAccounts = Arrays.asList(testAccountSavings, testAccountCurrent);
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.getResultList()).thenReturn(allAccounts);

        // When
        List<Account> foundAccounts = accountService.getAllAccounts();

        // Then
        assertThat(foundAccounts).containsExactlyInAnyOrder(testAccountSavings, testAccountCurrent);
    }

    @Test
    @DisplayName("should return empty list when no accounts found in getAllAccounts")
    void getAllAccounts_NoAccounts() {
        // Given
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.getResultList()).thenReturn(Collections.emptyList());

        // When
        List<Account> foundAccounts = accountService.getAllAccounts();

        // Then
        assertThat(foundAccounts).isEmpty();
    }

    // --- Test updateAccount method ---
    @Test
    @DisplayName("should update an existing account successfully")
    void updateAccount_Success() {
        // Given
        AccountDto accountDto = new AccountDto();
        accountDto.setId(testAccountSavings.getId());
        accountDto.setType(AccountType.CURRENT);
        accountDto.setBalance(BigDecimal.valueOf(1200.00));

        when(entityManager.find(eq(Account.class), eq(testAccountSavings.getId()))).thenReturn(testAccountSavings);

        // When
        Account updatedAccount = accountService.updateAccount(accountDto);

        // Then
        assertThat(updatedAccount).isNotNull();
        assertThat(updatedAccount.getType()).isEqualTo(AccountType.CURRENT);
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1200.00));
    }

    @Test
    @DisplayName("should throw AccountNotFoundException when updating non-existent account")
    void updateAccount_NotFound() {
        // Given
        AccountDto accountDto = new AccountDto();
        accountDto.setId(99L);
        accountDto.setType(AccountType.CURRENT);
        accountDto.setBalance(BigDecimal.valueOf(100.00));

        when(entityManager.find(eq(Account.class), anyLong())).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> accountService.updateAccount(accountDto))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account with ID 99 not found for update.");
    }

    // --- Test findAccountsByUserId method ---
    @Test
    @DisplayName("should find accounts by user ID successfully")
    void findAccountsByUserId_Success() {
        // Given
        when(entityManager.find(eq(User.class), eq(testUser.getId()))).thenReturn(testUser);
        when(entityManager.createQuery(anyString(), eq(Customer.class))).thenReturn(mockCustomerTypedQuery);
        when(mockCustomerTypedQuery.setParameter(anyString(), anyString())).thenReturn(mockCustomerTypedQuery);
        when(mockCustomerTypedQuery.getSingleResult()).thenReturn(testCustomer);

        List<Account> userAccounts = Arrays.asList(testAccountSavings, testAccountCurrent);
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.setParameter(anyString(), any())).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.getResultList()).thenReturn(userAccounts);

        // When
        List<Account> foundAccounts = accountService.findAccountsByUserId(testUser.getId());

        // Then
        assertThat(foundAccounts).containsExactlyInAnyOrder(testAccountSavings, testAccountCurrent);
    }

    @Test
    @DisplayName("should throw UserNotFoundException when finding accounts for non-existent user")
    void findAccountsByUserId_UserNotFound() {
        // Given
        when(entityManager.find(eq(User.class), anyLong())).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> accountService.findAccountsByUserId(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User with ID 99 not found.");
    }

    @Test
    @DisplayName("should return empty list when no customer linked to user email")
    void findAccountsByUserId_NoCustomerLinked() {
        // Given
        when(entityManager.find(eq(User.class), eq(testUser.getId()))).thenReturn(testUser);
        when(entityManager.createQuery(anyString(), eq(Customer.class))).thenReturn(mockCustomerTypedQuery);
        when(mockCustomerTypedQuery.setParameter(anyString(), anyString())).thenReturn(mockCustomerTypedQuery);
        when(mockCustomerTypedQuery.getSingleResult()).thenThrow(NoResultException.class);

        // When
        List<Account> foundAccounts = accountService.findAccountsByUserId(testUser.getId());

        // Then
        assertThat(foundAccounts).isEmpty();
    }

    // --- Test calculateAccruedInterest method ---
    @Test
    @DisplayName("should calculate accrued interest correctly for full days")
    void calculateAccruedInterest_SuccessFullDays() {
        // Given
        testAccountSavings.setBalance(BigDecimal.valueOf(1000.00));
        testAccountSavings.setLastInterestAppliedDate(LocalDateTime.now().minusDays(3).withHour(0).withMinute(0).withSecond(0).withNano(0)); // 3 full days ago

        // Correct mock for getAccountById internal call
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.setParameter(anyString(), eq(testAccountSavings.getId()))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.getSingleResult()).thenReturn(testAccountSavings);

        // When
        BigDecimal accruedInterest = accountService.calculateAccruedInterest(testAccountSavings.getId(), LocalDateTime.now());

        // Then (Expected value based on 1000 * ((1 + 0.00002)^3 - 1) scaled to 2 decimal places)
        assertThat(accruedInterest).isEqualByComparingTo(new BigDecimal("0.06"));
    }

    @Test
    @DisplayName("should return zero interest for no full days passed")
    void calculateAccruedInterest_NoDaysPassed() {
        // Given
        testAccountSavings.setBalance(BigDecimal.valueOf(1000.00));
        testAccountSavings.setLastInterestAppliedDate(LocalDateTime.now().minusHours(1)); // Less than a day ago

        // Correct mock for getAccountById internal call
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.setParameter(anyString(), eq(testAccountSavings.getId()))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.getSingleResult()).thenReturn(testAccountSavings);

        // When
        BigDecimal accruedInterest = accountService.calculateAccruedInterest(testAccountSavings.getId(), LocalDateTime.now());

        // Then
        assertThat(accruedInterest).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("should throw InvalidTransactionException for non-SAVINGS account in calculateAccruedInterest")
    void calculateAccruedInterest_NonSavingsAccount() {
        // Given
        testAccountCurrent.setType(AccountType.CURRENT); // Not a saving account

        // Correct mock for getAccountById internal call
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.setParameter(anyString(), eq(testAccountCurrent.getId()))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.getSingleResult()).thenReturn(testAccountCurrent); // Return the current account

        // When / Then
        assertThatThrownBy(() -> accountService.calculateAccruedInterest(testAccountCurrent.getId(), LocalDateTime.now()))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Interest can only be calculated for SAVINGS accounts.");
    }

    @Test
    @DisplayName("should throw InvalidTransactionException for inactive account in calculateAccruedInterest")
    void calculateAccruedInterest_InactiveAccount() {
        // Given
        testAccountSavings.setIsActive(false); // Make account inactive

        // Correct mock for getAccountById internal call
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.setParameter(anyString(), eq(testAccountSavings.getId()))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.getSingleResult()).thenReturn(testAccountSavings); // Return the inactive savings account

        // When / Then
        assertThatThrownBy(() -> accountService.calculateAccruedInterest(testAccountSavings.getId(), LocalDateTime.now()))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Cannot calculate interest for inactive account");
    }

    // --- Test applyAccruedInterest method ---
    @Test
    @DisplayName("should apply accrued interest and create transaction successfully")
    void applyAccruedInterest_Success() {
        // Given
        BigDecimal interestAmount = new BigDecimal("10.50");
        BigDecimal initialBalance = testAccountSavings.getBalance();
        LocalDateTime initialLastAppliedDate = testAccountSavings.getLastInterestAppliedDate();

        // Correct mock for getAccountById internal call
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.setParameter(anyString(), eq(testAccountSavings.getId()))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.getSingleResult()).thenReturn(testAccountSavings);

        // When
        Account updatedAccount = accountService.applyAccruedInterest(testAccountSavings.getId(), interestAmount);

        // Then
        assertThat(updatedAccount).isNotNull();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(initialBalance.add(interestAmount));
        assertThat(updatedAccount.getLastInterestAppliedDate()).isAfter(initialLastAppliedDate); // Date should be updated
        verify(entityManager, times(1)).persist(any(Transaction.class)); // Verify transaction record was persisted
    }

    @Test
    @DisplayName("should throw InvalidTransactionException for non-SAVINGS account in applyAccruedInterest")
    void applyAccruedInterest_NonSavingsAccount() {
        // Given
        testAccountCurrent.setType(AccountType.CURRENT); // Not a savings account

        // Correct mock for getAccountById internal call
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.setParameter(anyString(), eq(testAccountCurrent.getId()))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.getSingleResult()).thenReturn(testAccountCurrent); // Return the current account

        // When / Then
        assertThatThrownBy(() -> accountService.applyAccruedInterest(testAccountCurrent.getId(), BigDecimal.ONE))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Interest can only be applied to SAVINGS accounts.");
    }

    @Test
    @DisplayName("should throw InvalidTransactionException for inactive account in applyAccruedInterest")
    void applyAccruedInterest_InactiveAccount() {
        // Given
        testAccountSavings.setIsActive(false); // Make account inactive

        // Correct mock for getAccountById internal call
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.setParameter(anyString(), eq(testAccountSavings.getId()))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.getSingleResult()).thenReturn(testAccountSavings); // Return the inactive savings account

        // When / Then
        assertThatThrownBy(() -> accountService.applyAccruedInterest(testAccountSavings.getId(), BigDecimal.ONE))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Cannot apply interest to inactive account");
    }

    @Test
    @DisplayName("should throw InvalidTransactionException for non-positive interest amount in applyAccruedInterest")
    void applyAccruedInterest_NonPositiveAmount() {
        // Given
        // Correct mock for getAccountById internal call
        when(entityManager.createQuery(anyString(), eq(Account.class))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.setParameter(anyString(), eq(testAccountSavings.getId()))).thenReturn(mockAccountTypedQuery);
        when(mockAccountTypedQuery.getSingleResult()).thenReturn(testAccountSavings);

        // When / Then
        assertThatThrownBy(() -> accountService.applyAccruedInterest(testAccountSavings.getId(), BigDecimal.valueOf(-5.00)))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Interest amount to apply must be positive.");

        assertThatThrownBy(() -> accountService.applyAccruedInterest(testAccountSavings.getId(), BigDecimal.ZERO))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Interest amount to apply must be positive.");
    }
}

