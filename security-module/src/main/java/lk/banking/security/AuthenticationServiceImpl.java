package lk.banking.security;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import lk.banking.core.entity.User;
import lk.banking.core.exception.UnauthorizedAccessException; // Import for login failures
import lk.banking.core.exception.UserNotFoundException;       // Import for user not found cases
import lk.banking.core.exception.ValidationException;         // Import for password policy validation

import java.util.logging.Logger;

@Stateless
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger LOGGER = Logger.getLogger(AuthenticationServiceImpl.class.getName());

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    public User authenticate(String username, String password) {
        try {
            User user = em.createQuery(
                            "SELECT u FROM User u WHERE u.username = :username", User.class) // Don't check isActive here
                    .setParameter("username", username)
                    .getSingleResult();

            if (!user.getIsActive()) { // Check isActive status AFTER finding user
                LOGGER.warning("AuthenticationServiceImpl: User '" + username + "' is inactive/locked.");
                // Provide the specific message for inactive users
                throw new UnauthorizedAccessException("You're banned temporarily please contact support team 0372250045.");
            }

            if (PasswordService.verifyPassword(password, user.getPassword())) {
                LOGGER.info("AuthenticationServiceImpl: User '" + username + "' authenticated successfully.");
                return user;
            } else {
                LOGGER.warning("AuthenticationServiceImpl: Invalid password for user '" + username + "'.");
                throw new UnauthorizedAccessException("Invalid credentials provided."); // Generic message for security
            }
        } catch (NoResultException e) {
            LOGGER.warning("AuthenticationServiceImpl: Username '" + username + "' not found.");
            throw new UnauthorizedAccessException("Invalid credentials provided."); // Generic message for security
        }
        // Other PersistenceExceptions might occur if there's a serious database issue,
        // which should ideally be handled by a broader exception mapper or caught at a higher level.
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        LOGGER.info("AuthenticationServiceImpl: Attempting password change for user ID: " + userId);
        User user = em.find(User.class, userId);
        if (user == null) {
            LOGGER.warning("AuthenticationServiceImpl: User with ID " + userId + " not found for password change.");
            throw new UserNotFoundException("User with ID " + userId + " not found.");
        }
        if (!PasswordService.verifyPassword(oldPassword, user.getPassword())) {
            LOGGER.warning("AuthenticationServiceImpl: Old password mismatch for user ID: " + userId);
            throw new UnauthorizedAccessException("Old password does not match.");
        }
        if (!PasswordService.isPasswordStrong(newPassword)) {
            LOGGER.warning("AuthenticationServiceImpl: New password for user ID " + userId + " does not meet strength requirements.");
            throw new ValidationException("New password does not meet complexity requirements. Must be minimum 8 characters, include uppercase, lowercase, digit, and special character.");
        }

        user.setPassword(PasswordService.hashPassword(newPassword));
        LOGGER.info("AuthenticationServiceImpl: Password successfully changed for user ID: " + userId);
        return true;
    }
}