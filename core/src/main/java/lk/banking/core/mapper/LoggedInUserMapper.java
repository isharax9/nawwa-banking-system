package lk.banking.core.mapper;

import lk.banking.core.dto.LoggedInUser; // Your new DTO for session
import lk.banking.core.entity.Role;
import lk.banking.core.entity.User;
import lk.banking.core.entity.enums.UserRole; // For roles mapping

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper for User entity to LoggedInUser DTO conversion.
 * Designed for user session storage, including essential fields like email and roles.
 */
public class LoggedInUserMapper {

    /**
     * Converts a User entity to a LoggedInUser DTO.
     * @param entity The User entity.
     * @param customerId Optional: The ID of the associated customer, if known.
     * @return A LoggedInUser DTO.
     */
    public static LoggedInUser toLoggedInUser(User entity, Long customerId) {
        if (entity == null) {
            return null;
        }

        Set<UserRole> roles = entity.getRoles() != null
                ? entity.getRoles().stream()
                .map(Role::getName) // Assuming Role has a getName() that returns UserRole
                .collect(Collectors.toSet())
                : null; // Or new HashSet<>() if roles should never be null

        return new LoggedInUser(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(), // Crucial: Map the email field
                roles,
                customerId // Pass the customerId if available (can be null for non-customer roles)
        );
    }
}