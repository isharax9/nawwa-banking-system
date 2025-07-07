package lk.banking.core.mapper;

import lk.banking.core.dto.RoleDto;
import lk.banking.core.entity.Role;

/**
 * Mapper for Role <-> RoleDto conversion.
 */
public class RoleMapper {

    /**
     * Converts a Role entity to a RoleDto.
     */
    public static RoleDto toDto(Role entity) {
        if (entity == null) return null;
        return new RoleDto(
                entity.getId(),
                entity.getName()
        );
    }

    /**
     * Converts a RoleDto to a new Role entity.
     */
    public static Role toEntity(RoleDto dto) {
        if (dto == null) return null;
        return new Role(dto.getName());
    }

    /**
     * Updates an existing Role entity with data from RoleDto.
     */
    public static void updateEntity(Role entity, RoleDto dto) {
        if (entity == null || dto == null) return;
        entity.setName(dto.getName());
    }
}