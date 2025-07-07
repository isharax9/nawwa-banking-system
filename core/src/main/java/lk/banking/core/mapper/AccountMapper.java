package lk.banking.core.mapper;

import lk.banking.core.dto.AccountDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Customer;

public class AccountMapper {

    /**
     * Converts an Account entity to an AccountDto.
     */
    public static AccountDto toDto(Account entity) {
        if (entity == null) return null;
        return new AccountDto(
                entity.getId(),
                entity.getAccountNumber(),
                entity.getType(),
                entity.getBalance(),
                entity.getCustomer() != null ? entity.getCustomer().getId() : null
        );
    }

    /**
     * Converts an AccountDto to a new Account entity.
     * You must provide the Customer entity.
     */
    public static Account toEntity(AccountDto dto, Customer customer) {
        if (dto == null) return null;
        return new Account(
                dto.getAccountNumber(),
                dto.getType(),
                dto.getBalance(),
                customer
        );
    }

    /**
     * Updates an existing Account entity with data from AccountDto.
     * You must provide the Customer entity.
     */
    public static void updateEntity(Account entity, AccountDto dto, Customer customer) {
        if (entity == null || dto == null) return;
        entity.setAccountNumber(dto.getAccountNumber());
        entity.setType(dto.getType());
        entity.setBalance(dto.getBalance());
        entity.setCustomer(customer);
    }
}