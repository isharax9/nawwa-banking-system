package lk.banking.security;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException; // For getSingleResult()
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NonUniqueResultException; // For unexpected duplicates
import lk.banking.core.dto.RegisterUserDto;
import lk.banking.core.entity.Role;
import lk.banking.core.entity.User;
import lk.banking.core.entity.enums.UserRole;
import lk.banking.core.exception.ResourceConflictException; // For duplicate username/email
import lk.banking.core.exception.RoleNotFoundException;     // For roles not found
import lk.banking.core.exception.UserNotFoundException;       // For user not found
import lk.banking.core.exception.ValidationException;         // For password validation
// Assuming SecurityUtils will be used for password strength, if not, it should be in PasswordService
// import lk.banking.security.SecurityUtils; // Not directly needed here if PasswordService handles strength

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Stateless
public class UserManagementServiceImpl implements UserManagementService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    public User register(String username, String password, UserRole role) {
        // 1. Validate input (optional, but good practice for external inputs)
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new ValidationException("Username and password cannot be empty.");
        }
        if (!PasswordService.isPasswordStrong(password)) {
            throw new ValidationException("Password does not meet complexity requirements.");
        }

        // 2. Check for duplicate username
        try {
            em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
            throw new ResourceConflictException("Username '" + username + "' already exists.");
        } catch (NoResultException e) {
            // No user with this username, proceed
        } catch (NonUniqueResultException e) {
            // This indicates a database integrity issue (duplicate usernames despite unique constraint)
            System.err.println("Database integrity error: Duplicate username despite unique constraint: " + username);
            throw new ResourceConflictException("A critical error occurred: Duplicate username found.");
        }


        // 3. Find/Validate Role
        Role dbRole = null;
        try {
            dbRole = em.createQuery("SELECT r FROM Role r WHERE r.name = :name", Role.class)
                    .setParameter("name", role)
                    .getSingleResult();
        } catch (NoResultException e) {
            // If roles are pre-populated, this means an invalid role was requested
            throw new RoleNotFoundException("Role '" + role.name() + "' not found in the system. Roles must be pre-configured.");
            // If you truly want to auto-create roles, uncomment below (but generally discouraged)
            // dbRole = new Role(role);
            // em.persist(dbRole);
        }

        // 4. Hash password
        String hashedPassword = PasswordService.hashPassword(password);

        // 5. Create User entity and set properties
        User user = new User();
        user.setUsername(username);
        user.setPassword(hashedPassword);
        // You might want to add email and phone to the register method's parameters
        // For now, setting defaults or leaving null if not provided
        user.setEmail(username + "@example.com"); // Placeholder: Consider requiring email in registration
        user.setPhone(null); // Placeholder
        user.setIsActive(true); // New users are active by default

        // 6. Assign the role
        user.addRole(dbRole); // Uses the helper method from User entity

        // 7. Persist the new user
        em.persist(user);

        return user;
    }

    @Override
    public User getUserById(Long id) {
        User user = em.find(User.class, id);
        if (user == null) {
            throw new UserNotFoundException("User with ID " + id + " not found.");
        }
        return user;
    }

    @Override
    public User getUserByUsername(String username) {
        try {
            // Use getSingleResult() to get exactly one result or throw NoResultException
            return em.createQuery(
                            "SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            // User not found, throw your custom exception
            throw new UserNotFoundException("User with username '" + username + "' not found.");
        } catch (NonUniqueResultException e) {
            // This should ideally not happen if username is unique in DB
            System.err.println("Database integrity error: Multiple users found for username " + username);
            throw new ResourceConflictException("Multiple users found for username " + username);
        }
    }

    @Override
    public List<User> getAllUsers() {
        return em.createQuery("SELECT u FROM User u", User.class)
                .getResultList();
    }

    @Override
    public boolean assignRole(Long userId, UserRole role) {
        User user = getUserById(userId); // Reuse getUserById to get user or throw UserNotFoundException

        // Find Role entity
        Role dbRole;
        try {
            dbRole = em.createQuery("SELECT r FROM Role r WHERE r.name = :name", Role.class)
                    .setParameter("name", role)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new RoleNotFoundException("Role '" + role.name() + "' not found in the system.");
        }

        // Check if user already has the role
        if (user.getRoles().contains(dbRole)) {
            // If already has, don't throw error but return true (idempotent operation)
            // Or throw a specific exception if that's desired behavior:
            // throw new ResourceConflictException("User already has role: " + role.name());
            return true;
        }

        user.addRole(dbRole); // Add role using helper method
        // em.merge(user); // Not strictly necessary for a managed entity
        return true;
    }

    @Override
    public boolean removeUser(Long userId) {
        User user = getUserById(userId); // Reuse getUserById to get user or throw UserNotFoundException

        // Option 1: Hard delete (current behavior, but generally discouraged for historical data)
        em.remove(user);
        System.out.println("User with ID " + userId + " has been permanently deleted.");
        return true;

        /*
        // Option 2: Soft delete (RECOMMENDED for most banking systems)
        // user.setIsActive(false);
        // em.merge(user); // If not already managed
        // System.out.println("User with ID " + userId + " has been deactivated.");
        // return true;
        */
    }

    @Override
    public User register(RegisterUserDto registerUserDto) {
        return null;
    }
}