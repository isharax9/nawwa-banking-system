package lk.banking.security;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException; // Import for getSingleResult()
import jakarta.persistence.PersistenceContext;
import lk.banking.core.entity.User;
import lk.banking.core.exception.UnauthorizedAccessException; // Import for login failures
import lk.banking.core.exception.UserNotFoundException;       // Import for user not found cases
import lk.banking.core.exception.ValidationException;         // Import for password policy validation

@Stateless
public class AuthenticationServiceImpl implements AuthenticationService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    public User authenticate(String username, String password) {
        try {
            // Find user by username. Assume username is unique.
            // Consider also checking user.isActive = TRUE if you have inactive users.
            User user = em.createQuery(
                            "SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult(); // Throws NoResultException if not found

            // Verify password using BCrypt
            if (user != null && PasswordService.verifyPassword(password, user.getPassword())) {
                // Check if user is active
                if (!user.getIsActive()) {
                    // This could be a different exception like AccountLockedException or UserInactiveException
                    throw new UnauthorizedAccessException("Account is inactive or locked.");
                }
                return user; // Authentication successful
            } else {
                // Password mismatch or user is null (though NoResultException handles null user)
                // For security, throw the same exception for both user not found and bad password
                throw new UnauthorizedAccessException("Invalid credentials provided.");
            }
        } catch (NoResultException e) {
            // User with given username not found.
            // For security, throw a generic "Invalid credentials" message to prevent username enumeration.
            throw new UnauthorizedAccessException("Invalid credentials provided.");
        }
        // Other PersistenceExceptions might occur if there's a serious database issue,
        // which should ideally be handled by a broader exception mapper or caught at a higher level.
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = em.find(User.class, userId);
        if (user == null) {
            throw new UserNotFoundException("User with ID " + userId + " not found.");
        }

        // Verify old password
        if (!PasswordService.verifyPassword(oldPassword, user.getPassword())) {
            throw new UnauthorizedAccessException("Old password does not match.");
        }

        // Validate new password strength
        if (!PasswordService.isPasswordStrong(newPassword)) {
            // Reusing ValidationException from core module
            throw new ValidationException("New password does not meet complexity requirements. Must be minimum 8 characters, include uppercase, lowercase, digit, and special character.");
        }

        // Hash and set new password
        user.setPassword(PasswordService.hashPassword(newPassword));
        // The user entity is managed; changes will be flushed to the database at transaction commit.
        // em.merge(user); // No need to explicitly merge a managed entity.

        return true; // Indicates password change process was successful
    }
}