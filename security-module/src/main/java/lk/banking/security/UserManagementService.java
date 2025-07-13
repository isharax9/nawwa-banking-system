package lk.banking.security;

import jakarta.ejb.Local;
import lk.banking.core.entity.User;
import lk.banking.core.entity.enums.UserRole;

import java.util.List;

@Local
public interface UserManagementService {
    /**
     * Registers a new user in the system. If the role is CUSTOMER, also creates a linked customer profile.
     * @param username The user's chosen username.
     * @param password The user's plain-text password.
     * @param email The user's email address (used for linking to customer).
     * @param name The user's full name (for customer profile).
     * @param address The user's address (for customer profile).
     * @param phoneNumber The user's phone number (for customer profile).
     * @param role The initial role for the user (e.g., UserRole.CUSTOMER).
     * @return The newly registered User entity.
     * @throws lk.banking.core.exception.ValidationException if input validation fails.
     * @throws lk.banking.core.exception.ResourceConflictException if username or email already exists.
     * @throws lk.banking.core.exception.RoleNotFoundException if the specified role does not exist.
     */
    User register(String username, String password, String email, String name, String address, String phoneNumber, UserRole role);

    /**
     * Retrieves a user by their ID.
     * @param id The ID of the user.
     * @return The User entity.
     * @throws lk.banking.core.exception.UserNotFoundException if the user is not found.
     */
    User getUserById(Long id);

    /**
     * Retrieves a user by their username.
     * @param username The username.
     * @return The User entity.
     * @throws lk.banking.core.exception.UserNotFoundException if the user is not found.
     * @throws lk.banking.core.exception.ResourceConflictException if multiple users found for the username (data integrity issue).
     */
    User getUserByUsername(String username);

    /**
     * Retrieves a list of all users in the system.
     * @return A list of User entities.
     */
    List<User> getAllUsers();

    /**
     * Assigns a specific role to a user.
     * @param userId The ID of the user.
     * @param role The UserRole to assign.
     * @return true if the role was assigned or already existed for the user.
     * @throws lk.banking.core.exception.UserNotFoundException if the user is not found.
     * @throws lk.banking.core.exception.RoleNotFoundException if the role does not exist.
     */
    boolean assignRole(Long userId, UserRole role);

    /**
     * Deactivates a user account (soft delete).
     * @param userId The ID of the user to deactivate.
     * @return true if the user was deactivated or already inactive.
     * @throws lk.banking.core.exception.UserNotFoundException if the user is not found.
     */
    boolean removeUser(Long userId); // Renamed from hard delete to more accurately reflect soft delete

    /**
     * Activates a user account.
     * @param userId The ID of the user to activate.
     * @return true if the user was activated or already active.
     * @throws lk.banking.core.exception.UserNotFoundException if the user is not found.
     */
    boolean activateUser(Long userId); // NEW METHOD
}