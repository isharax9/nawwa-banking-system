package lk.banking.core.mapper;

import lk.banking.core.dto.TransactionDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.enums.AccountType; // Import AccountType

/**
 * Mapper for Transaction <-> TransactionDto conversion.
 */
public class TransactionMapper {

    /**
     * Converts a Transaction entity to a TransactionDto.
     * Includes mapping of associated account details (number, type).
     */
    public static TransactionDto toDto(Transaction entity) {
        if (entity == null) return null;

        String accountNumber = null;
        AccountType accountType = null;
        if (entity.getAccount() != null) {
            accountNumber = entity.getAccount().getAccountNumber();
            accountType = entity.getAccount().getType();
        }

        return new TransactionDto(
                entity.getId(),
                entity.getAccount() != null ? entity.getAccount().getId() : null,
                accountNumber, // NEW: Map account number
                accountType,   // NEW: Map account type
                entity.getAmount(),
                entity.getType(),
                entity.getStatus(),
                entity.getTimestamp(),
                entity.getDescription()
        );
    }

    /**
     * Converts a TransactionDto to a new Transaction entity.
     * You must provide the Account entity.
     */
    public static Transaction toEntity(TransactionDto dto, Account account) {
        if (dto == null) return null;
        return new Transaction(
                account,
                dto.getAmount(),
                dto.getType(),
                dto.getStatus(),
                dto.getTimestamp(),
                dto.getDescription()
        );
    }

    /**
     * Updates an existing Transaction entity with data from TransactionDto.
     * You must provide the Account entity.
     */
    public static void updateEntity(Transaction entity, TransactionDto dto, Account account) {
        if (entity == null || dto == null) return;
        entity.setAccount(account);
        entity.setAmount(dto.getAmount());
        entity.setType(dto.getType());
        entity.setStatus(dto.getStatus());
        entity.setTimestamp(dto.getTimestamp());
        entity.setDescription(dto.getDescription());
    }
}