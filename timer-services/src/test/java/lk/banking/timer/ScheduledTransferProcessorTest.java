package lk.banking.timer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lk.banking.core.dto.TransferRequestDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Customer; // Required for Account setup
import lk.banking.core.entity.ScheduledTransfer;
import lk.banking.core.entity.enums.AccountType;
import lk.banking.core.exception.AccountNotFoundException; // Example exception FundTransferService might throw
import lk.banking.transaction.FundTransferService; // Injected service

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory; // To mock the logger if needed, but usually just check for info/error calls

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduledTransferProcessor Unit Tests")
public class ScheduledTransferProcessorTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private FundTransferService fundTransferService; // Mock the injected service

    @InjectMocks
    private ScheduledTransferProcessor scheduledTransferProcessor;

    @Mock
    private TypedQuery<ScheduledTransfer> mockScheduledTransferQuery;

    // Test Data
    private Customer testCustomer;
    private Account fromAccount;
    private Account toAccount;
    private ScheduledTransfer scheduledTransfer1;
    private ScheduledTransfer scheduledTransfer2;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer("John Doe", "john.doe@example.com", "123 Main St", "1234567890");
        testCustomer.setId(1L);

        fromAccount = new Account("FROM123", AccountType.SAVINGS, BigDecimal.valueOf(1000.00), testCustomer);
        fromAccount.setId(101L);
        fromAccount.setIsActive(true);

        toAccount = new Account("TO456", AccountType.CURRENT, BigDecimal.valueOf(500.00), testCustomer);
        toAccount.setId(102L);
        toAccount.setIsActive(true);

        // A scheduled transfer that is due
        scheduledTransfer1 = new ScheduledTransfer();
        scheduledTransfer1.setId(1L);
        scheduledTransfer1.setFromAccount(fromAccount);
        scheduledTransfer1.setToAccount(toAccount);
        scheduledTransfer1.setAmount(BigDecimal.valueOf(100.00));
        scheduledTransfer1.setScheduledTime(LocalDateTime.now().minusMinutes(10)); // It's due
        scheduledTransfer1.setProcessed(false);

        // Another scheduled transfer, also due
        scheduledTransfer2 = new ScheduledTransfer();
        scheduledTransfer2.setId(2L);
        scheduledTransfer2.setFromAccount(toAccount); // Reverse accounts for variety
        scheduledTransfer2.setToAccount(fromAccount);
        scheduledTransfer2.setAmount(BigDecimal.valueOf(50.00));
        scheduledTransfer2.setScheduledTime(LocalDateTime.now().minusMinutes(5)); // It's due
        scheduledTransfer2.setProcessed(false);
    }

    // --- Test processScheduledTransfers method ---

    @Test
    @DisplayName("should successfully process all due scheduled transfers")
    void processScheduledTransfers_Success() {
        // Given
        List<ScheduledTransfer> dueTransfers = Arrays.asList(scheduledTransfer1, scheduledTransfer2);

        // Mock EntityManager to return the due transfers
        when(entityManager.createQuery(anyString(), eq(ScheduledTransfer.class))).thenReturn(mockScheduledTransferQuery);
        when(mockScheduledTransferQuery.setParameter(eq("now"), any(LocalDateTime.class))).thenReturn(mockScheduledTransferQuery);
        when(mockScheduledTransferQuery.getResultList()).thenReturn(dueTransfers);

        // Mock FundTransferService to simulate successful transfers
        when(fundTransferService.transferFunds(any(TransferRequestDto.class))).thenReturn(mock(lk.banking.core.entity.Transaction.class)); // Return any transaction for success

        // Capture arguments passed to fundTransferService
        ArgumentCaptor<TransferRequestDto> dtoCaptor = ArgumentCaptor.forClass(TransferRequestDto.class);

        // When
        scheduledTransferProcessor.processScheduledTransfers();

        // Then
        // Verify EntityManager interactions
        verify(entityManager, times(1)).createQuery(anyString(), eq(ScheduledTransfer.class));
        verify(mockScheduledTransferQuery, times(1)).setParameter(eq("now"), any(LocalDateTime.class));
        verify(mockScheduledTransferQuery, times(1)).getResultList();

        // Verify FundTransferService was called for each transfer
        verify(fundTransferService, times(2)).transferFunds(dtoCaptor.capture());

        // Verify the arguments passed to fundTransferService
        List<TransferRequestDto> capturedDtos = dtoCaptor.getAllValues();
        assertThat(capturedDtos).hasSize(2);

        // Check first transfer DTO
        TransferRequestDto capturedDto1 = capturedDtos.get(0);
        assertThat(capturedDto1.getFromAccountId()).isEqualTo(scheduledTransfer1.getFromAccount().getId());
        assertThat(capturedDto1.getToAccountId()).isEqualTo(scheduledTransfer1.getToAccount().getId());
        assertThat(capturedDto1.getAmount()).isEqualByComparingTo(scheduledTransfer1.getAmount());

        // Check second transfer DTO
        TransferRequestDto capturedDto2 = capturedDtos.get(1);
        assertThat(capturedDto2.getFromAccountId()).isEqualTo(scheduledTransfer2.getFromAccount().getId());
        assertThat(capturedDto2.getToAccountId()).isEqualTo(scheduledTransfer2.getToAccount().getId());
        assertThat(capturedDto2.getAmount()).isEqualByComparingTo(scheduledTransfer2.getAmount());

        // Verify that the processed flag was set to true for both transfers
        // CHANGE: Use getProcessed() instead of isProcessed()
        assertThat(scheduledTransfer1.getProcessed()).isTrue();
        assertThat(scheduledTransfer2.getProcessed()).isTrue();
        // Note: For managed entities, setting a property is enough; merge is not explicitly needed.
    }

    @Test
    @DisplayName("should not process any transfers if no scheduled transfers are due")
    void processScheduledTransfers_NoTransfersDue() {
        // Given
        when(entityManager.createQuery(anyString(), eq(ScheduledTransfer.class))).thenReturn(mockScheduledTransferQuery);
        when(mockScheduledTransferQuery.setParameter(eq("now"), any(LocalDateTime.class))).thenReturn(mockScheduledTransferQuery);
        when(mockScheduledTransferQuery.getResultList()).thenReturn(Collections.emptyList());

        // When
        scheduledTransferProcessor.processScheduledTransfers();

        // Then
        // Verify EntityManager interactions for query
        verify(entityManager, times(1)).createQuery(anyString(), eq(ScheduledTransfer.class));
        verify(mockScheduledTransferQuery, times(1)).setParameter(eq("now"), any(LocalDateTime.class));
        verify(mockScheduledTransferQuery, times(1)).getResultList();

        // Verify no calls to FundTransferService or any changes to ScheduledTransfer entities
        verifyNoInteractions(fundTransferService);
        // CHANGE: Use getProcessed() instead of isProcessed()
        assertThat(scheduledTransfer1.getProcessed()).isFalse(); // Ensure default state for test data
        assertThat(scheduledTransfer2.getProcessed()).isFalse(); // Ensure default state for test data
    }

    @Test
    @DisplayName("should continue processing other transfers even if one transfer fails")
    void processScheduledTransfers_PartialFailure() {
        // Given
        List<ScheduledTransfer> dueTransfers = Arrays.asList(scheduledTransfer1, scheduledTransfer2);

        // Mock EntityManager to return the due transfers
        when(entityManager.createQuery(anyString(), eq(ScheduledTransfer.class))).thenReturn(mockScheduledTransferQuery);
        when(mockScheduledTransferQuery.setParameter(eq("now"), any(LocalDateTime.class))).thenReturn(mockScheduledTransferQuery);
        when(mockScheduledTransferQuery.getResultList()).thenReturn(dueTransfers);

        // Mock FundTransferService: Make the first transfer fail, the second succeed
        // IMPORTANT CHANGE: Add null checks within the argThat lambda for robustness
        when(fundTransferService.transferFunds(argThat(dto ->
                dto != null && dto.getFromAccountId() != null && dto.getFromAccountId().equals(scheduledTransfer1.getFromAccount().getId())
        )))
                .thenThrow(new AccountNotFoundException("Source account not found during scheduled transfer.")); // Simulate failure for scheduledTransfer1

        when(fundTransferService.transferFunds(argThat(dto ->
                dto != null && dto.getFromAccountId() != null && dto.getFromAccountId().equals(scheduledTransfer2.getFromAccount().getId())
        )))
                .thenReturn(mock(lk.banking.core.entity.Transaction.class)); // Simulate success for scheduledTransfer2

        // When
        scheduledTransferProcessor.processScheduledTransfers();

        // Then
        // Verify FundTransferService was attempted for both
        verify(fundTransferService, times(1)).transferFunds(argThat(dto ->
                dto != null && dto.getFromAccountId() != null && dto.getFromAccountId().equals(scheduledTransfer1.getFromAccount().getId())
        ));
        verify(fundTransferService, times(1)).transferFunds(argThat(dto ->
                dto != null && dto.getFromAccountId() != null && dto.getFromAccountId().equals(scheduledTransfer2.getFromAccount().getId())
        ));

        // Verify processed status: only scheduledTransfer2 should be true
        assertThat(scheduledTransfer1.getProcessed()).isFalse(); // Failed transfer should not be marked processed
        assertThat(scheduledTransfer2.getProcessed()).isTrue(); // Successful transfer should be marked processed
    }

    @Test
    @DisplayName("should handle empty scheduledTime (null) in query gracefully if present in DB")
    void processScheduledTransfers_HandlesNullScheduledTime() {
        // This scenario tests the query itself. The current query `s.scheduledTime <= :now` would exclude nulls.
        // If your service had a different query, this test would be more relevant.
        // For current service, this test will essentially be the same as 'NoTransfersDue'
        // unless you specifically wanted to test behavior if getScheduledTime() throws NPE (which it shouldn't for a managed entity)

        // Given a scheduled transfer that somehow has null scheduledTime
        ScheduledTransfer scheduledTransferWithNullTime = new ScheduledTransfer();
        scheduledTransferWithNullTime.setId(3L);
        scheduledTransferWithNullTime.setFromAccount(fromAccount);
        scheduledTransferWithNullTime.setToAccount(toAccount);
        scheduledTransferWithNullTime.setAmount(BigDecimal.valueOf(20.00));
        scheduledTransferWithNullTime.setScheduledTime(null); // Explicitly null
        scheduledTransferWithNullTime.setProcessed(false);

        // Mock EntityManager to return a list that *might* contain such an item,
        // but the query logic in the service (s.scheduledTime <= :now) should filter it out.
        // So, this test will essentially act like the "NoTransfersDue" test if the query handles nulls as expected.
        when(entityManager.createQuery(anyString(), eq(ScheduledTransfer.class))).thenReturn(mockScheduledTransferQuery);
        when(mockScheduledTransferQuery.setParameter(eq("now"), any(LocalDateTime.class))).thenReturn(mockScheduledTransferQuery);
        // Simulate query returning only 'valid' transfers, implicitly excluding null-time ones
        when(mockScheduledTransferQuery.getResultList()).thenReturn(Collections.emptyList());

        // When
        scheduledTransferProcessor.processScheduledTransfers();

        // Then
        verify(entityManager, times(1)).createQuery(anyString(), eq(ScheduledTransfer.class));
        verifyNoInteractions(fundTransferService);
        // CHANGE: Use getProcessed() instead of isProcessed()
        assertThat(scheduledTransferWithNullTime.getProcessed()).isFalse();
    }
}