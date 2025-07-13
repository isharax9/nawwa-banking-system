package lk.banking.core.mapper;

import lk.banking.core.dto.UserDto;
import lk.banking.core.entity.User;
import lk.banking.core.entity.enums.UserRole;
import lk.banking.core.entity.Role;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper for User <-> UserDto conversion.
 */
public class UserMapper {

    /**
     * Converts a User entity to a UserDto.
     * @param entity The User entity.
     * @return A UserDto, excluding sensitive information like password.
     */
    public static UserDto toDto(User entity) {
        if (entity == null) return null;
        Set<UserRole> roles = entity.getRoles() != null
                ? entity.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet())
                : null; // Or new HashSet<>()

        // Updated constructor call to include new fields
        return new UserDto(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(), // NEW: Map email
                entity.getPhone(), // NEW: Map phone
                entity.getIsActive(), // NEW: Map isActive
                entity.getCreatedAt(), // NEW: Map createdAt
                entity.getUpdatedAt(), // NEW: Map updatedAt
                roles
        );
    }

    // No toEntity(UserDto dto) provided, since password and roles assignment
    // should be handled via registration/service logic for security reasons.
}