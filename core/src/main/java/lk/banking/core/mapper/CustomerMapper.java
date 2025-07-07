package lk.banking.core.mapper;

import lk.banking.core.dto.CustomerDto;
import lk.banking.core.entity.Customer;

public class CustomerMapper {

    /**
     * Converts a Customer entity to a CustomerDto.
     */
    public static CustomerDto toDto(Customer entity) {
        if (entity == null) return null;
        return new CustomerDto(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getAddress(),
                entity.getPhoneNumber()
        );
    }

    /**
     * Converts a CustomerDto to a new Customer entity.
     */
    public static Customer toEntity(CustomerDto dto) {
        if (dto == null) return null;
        return new Customer(
                dto.getName(),
                dto.getEmail(),
                dto.getAddress(),
                dto.getPhoneNumber()
        );
    }

    /**
     * Updates an existing Customer entity with data from CustomerDto.
     */
    public static void updateEntity(Customer entity, CustomerDto dto) {
        if (entity == null || dto == null) return;
        entity.setName(dto.getName());
        entity.setEmail(dto.getEmail());
        entity.setAddress(dto.getAddress());
        entity.setPhoneNumber(dto.getPhoneNumber());
    }
}