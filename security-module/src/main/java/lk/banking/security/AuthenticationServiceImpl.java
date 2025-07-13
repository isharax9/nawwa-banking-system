package lk.banking.security;

import jakarta.annotation.security.PermitAll; // IMPORT THIS (for authenticating)
import jakarta.annotation.security.RolesAllowed; // IMPORT THIS (for changePassword)
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import lk.banking.core.entity.User;
import lk.banking.core.exception.UnauthorizedAccessException;
import lk.banking.core.exception.UserNotFoundException;
import lk.banking.core.exception.ValidationException;

import java.util.logging.Logger;

@Stateless
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger LOGGER = Logger.getLogger(AuthenticationServiceImpl.class.getName());

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    @PermitAll // Allow anyone (unauthenticated) to call authenticate (for login)
    public User authenticate(String username, String password) {
        try {
            User user = em.createQuery(
                            "SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();

            if (!user.getIsActive()) {
                LOGGER.warning("AuthenticationServiceImpl: User '" + username + "' is inactive/locked.");
                throw new UnauthorizedAccessException("You're banned temporarily please contact support team 0372250045.");
            }

            if (PasswordService.verifyPassword(password, user.getPassword())) {
                LOGGER.info("AuthenticationServiceImpl: User '" + username + "' authenticated successfully.");
                return user;
            } else {
                LOGGER.warning("AuthenticationServiceImpl: Invalid password for user '" + username + "'.");
                throw new UnauthorizedAccessException("Invalid credentials provided.");
            }
        } catch (NoResultException e) {
            LOGGER.warning("AuthenticationServiceImpl: Username '" + username + "' not found.");
            throw new UnauthorizedAccessException("Invalid credentials provided.");
        }
    }

    @Override
    @RolesAllowed({"CUSTOMER", "EMPLOYEE", "ADMIN"}) // Only authenticated users can change their password
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