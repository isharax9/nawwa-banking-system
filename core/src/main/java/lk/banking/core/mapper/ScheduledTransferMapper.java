package lk.banking.core.mapper;

import lk.banking.core.dto.ScheduledTransferDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.ScheduledTransfer;

/**
 * Mapper for ScheduledTransfer <-> ScheduledTransferDto conversion.
 */
public class ScheduledTransferMapper {

    /**
     * Converts a ScheduledTransfer entity to a ScheduledTransferDto.
     */
    public static ScheduledTransferDto toDto(ScheduledTransfer entity) {
        if (entity == null) return null;
        return new ScheduledTransferDto(
                entity.getId(),
                entity.getFromAccount() != null ? entity.getFromAccount().getId() : null,
                entity.getToAccount() != null ? entity.getToAccount().getId() : null,
                entity.getAmount(),
                entity.getScheduledTime(),
                entity.getProcessed()
        );
    }

    /**
     * Converts a ScheduledTransferDto to a new ScheduledTransfer entity.
     * You must provide the fromAccount and toAccount entities.
     */
    public static ScheduledTransfer toEntity(ScheduledTransferDto dto, Account fromAccount, Account toAccount) {
        if (dto == null) return null;
        ScheduledTransfer transfer = new ScheduledTransfer(
                fromAccount,
                toAccount,
                dto.getAmount(),
                dto.getScheduledTime()
        );
        transfer.setProcessed(dto.getProcessed() != null ? dto.getProcessed() : false);
        return transfer;
    }

    /**
     * Updates an existing ScheduledTransfer entity with data from ScheduledTransferDto.
     * You must provide the fromAccount and toAccount entities.
     */
    public static void updateEntity(ScheduledTransfer entity, ScheduledTransferDto dto, Account fromAccount, Account toAccount) {
        if (entity == null || dto == null) return;
        entity.setFromAccount(fromAccount);
        entity.setToAccount(toAccount);
        entity.setAmount(dto.getAmount());
        entity.setScheduledTime(dto.getScheduledTime());
        entity.setProcessed(dto.getProcessed() != null ? dto.getProcessed() : false);
    }
}