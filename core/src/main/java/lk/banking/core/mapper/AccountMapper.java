package lk.banking.core.mapper;

import lk.banking.core.dto.AccountDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Customer; // Import Customer to access name
import lk.banking.core.entity.enums.AccountType;

import java.math.BigDecimal;

public class AccountMapper {

    /**
     * Converts an Account entity to an AccountDto.
     * Includes mapping of customer name and account status for display.
     */
    public static AccountDto toDto(Account entity) {
        if (entity == null) return null;

        Long customerId = null;
        String customerName = null;
        // Check if customer is eagerly fetched and not null to avoid LazyInitializationException
        if (entity.getCustomer() != null) {
            customerId = entity.getCustomer().getId();
            customerName = entity.getCustomer().getName();
        }

        return new AccountDto(
                entity.getId(),
                entity.getAccountNumber(),
                entity.getType(),
                entity.getBalance(),
                customerId, // Map customer ID
                customerName, // NEW: Map customer name
                entity.getIsActive(), // NEW: Map isActive
                entity.getCreatedAt(), // NEW: Map createdAt
                entity.getUpdatedAt() // NEW: Map updatedAt
        );
    }

    /**
     * Converts an AccountDto to a new Account entity.
     * You must provide the Customer entity.
     */
    public static Account toEntity(AccountDto dto, Customer customer) {
        if (dto == null) return null;
        Account account = new Account(
                dto.getAccountNumber(),
                dto.getType(),
                dto.getBalance() != null ? dto.getBalance() : BigDecimal.ZERO, // Ensure non-null balance
                customer
        );
        // Set isActive only if it's explicitly provided in DTO, otherwise let constructor handle default
        if (dto.getIsActive() != null) {
            account.setIsActive(dto.getIsActive());
        }
        return account;
    }

    /**
     * Updates an existing Account entity with data from AccountDto.
     * You must provide the Customer entity.
     * Note: accountNumber, customerId typically not updated here.
     */
    public static void updateEntity(Account entity, AccountDto dto, Customer customer) {
        if (entity == null || dto == null) return;
        // Account number is usually immutable
        // entity.setAccountNumber(dto.getAccountNumber());
        entity.setType(dto.getType());
        entity.setBalance(dto.getBalance());
        // Customer relationship is usually immutable via this method
        // entity.setCustomer(customer);
        if (dto.getIsActive() != null) { // Allow updating active status
            entity.setIsActive(dto.getIsActive());
        }
    }
}