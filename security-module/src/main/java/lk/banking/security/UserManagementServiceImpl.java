package lk.banking.security;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NonUniqueResultException;
import lk.banking.core.dto.RegisterUserDto;
import lk.banking.core.entity.Customer; // Import Customer entity
import lk.banking.core.entity.Role;
import lk.banking.core.entity.User;
import lk.banking.core.entity.enums.UserRole;
import lk.banking.core.exception.ResourceConflictException;
import lk.banking.core.exception.RoleNotFoundException;
import lk.banking.core.exception.UserNotFoundException;
import lk.banking.core.exception.ValidationException;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger; // Using java.util.logging here for consistency

@Stateless
public class UserManagementServiceImpl implements UserManagementService {

    private static final Logger LOGGER = Logger.getLogger(UserManagementServiceImpl.class.getName());

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    // Modified signature
    public User register(String username, String password, String email, String name, String address, String phoneNumber, UserRole role) {
        LOGGER.info("Attempting to register new user: " + username + " with email: " + email);

        // 1. Validate input
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty() ||
                email == null || email.trim().isEmpty() || name == null || name.trim().isEmpty() ||
                address == null || address.trim().isEmpty() || phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new ValidationException("All registration fields (username, password, email, name, address, phone number) are required.");
        }
        if (!PasswordService.isPasswordStrong(password)) {
            throw new ValidationException("Password does not meet complexity requirements.");
        }
        // Basic email format check, though more robust checks are in ValidationUtils
        if (!email.contains("@") || !email.contains(".")) {
            throw new ValidationException("Invalid email format.");
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
            LOGGER.severe("Database integrity error: Multiple users found for username " + username);
            throw new ResourceConflictException("A critical error occurred: Duplicate username found.");
        }

        // 3. Check for duplicate email in User table (already has unique constraint)
        try {
            em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
            throw new ResourceConflictException("Email '" + email + "' is already registered with a user account.");
        } catch (NoResultException e) {
            // No user with this email, proceed
        }

        // 4. Check for duplicate email in Customer table (already has unique constraint)
        try {
            em.createQuery("SELECT c FROM Customer c WHERE c.email = :email", Customer.class)
                    .setParameter("email", email)
                    .getSingleResult();
            throw new ResourceConflictException("Email '" + email + "' is already registered with a customer profile.");
        } catch (NoResultException e) {
            // No customer with this email, proceed
        }


        // 5. Find/Validate Role (should be CUSTOMER for self-registration)
        Role dbRole = null;
        try {
            dbRole = em.createQuery("SELECT r FROM Role r WHERE r.name = :name", Role.class)
                    .setParameter("name", role)
                    .getSingleResult();
        } catch (NoResultException e) {
            LOGGER.severe("Role '" + role.name() + "' not found in DB. Please ensure roles are pre-populated.");
            throw new RoleNotFoundException("Role '" + role.name() + "' not found in the system. Roles must be pre-configured.");
        }

        // 6. Hash password
        String hashedPassword = PasswordService.hashPassword(password);

        // 7. Create User entity
        User user = new User();
        user.setUsername(username);
        user.setPassword(hashedPassword);
        user.setEmail(email); // Set the provided email
        user.setPhone(phoneNumber); // Set the provided phone number
        user.setIsActive(true);
        user.addRole(dbRole); // Add the resolved role

        em.persist(user); // Persist the user first

        // 8. Create Customer entity and link by email
        // Assumes that a User (who is a CUSTOMER) maps one-to-one to a Customer entity
        if (role == UserRole.CUSTOMER) {
            Customer customer = new Customer(name, email, address, phoneNumber);
            // Optionally link user to customer here directly if you add a @OneToOne from User to Customer
            // For now, they are linked implicitly via the shared unique 'email' field.
            em.persist(customer);
            LOGGER.info("Created new customer profile for user: " + username + " with email: " + email);
        } else {
            LOGGER.info("User " + username + " registered with role " + role.name() + ". No customer profile created.");
        }

        LOGGER.info("User " + username + " registered successfully.");
        return user;
    }

    // ... (rest of the UserManagementServiceImpl methods remain the same) ...

    @Override
    public User getUserById(Long id) {
        LOGGER.fine("Fetching user by ID: " + id);
        User user = em.find(User.class, id);
        if (user == null) {
            LOGGER.warning("User with ID " + id + " not found.");
            throw new UserNotFoundException("User with ID " + id + " not found.");
        }
        return user;
    }

    @Override
    public User getUserByUsername(String username) {
        LOGGER.fine("Fetching user by username: " + username);
        try {
            return em.createQuery(
                            "SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            LOGGER.warning("User with username '" + username + "' not found.");
            throw new UserNotFoundException("User with username '" + username + "' not found.");
        } catch (NonUniqueResultException e) {
            LOGGER.severe("Database integrity error: Multiple users found for username " + username);
            throw new ResourceConflictException("Multiple users found for username " + username);
        }
    }

    @Override
    public List<User> getAllUsers() {
        LOGGER.fine("Fetching all users.");
        return em.createQuery("SELECT u FROM User u", User.class)
                .getResultList();
    }

    @Override
    public boolean assignRole(Long userId, UserRole role) {
        LOGGER.info("Assigning role " + role.name() + " to user ID: " + userId);
        User user = getUserById(userId); // Reuse getUserById to get user or throw UserNotFoundException

        Role dbRole;
        try {
            dbRole = em.createQuery("SELECT r FROM Role r WHERE r.name = :name", Role.class)
                    .setParameter("name", role)
                    .getSingleResult();
        } catch (NoResultException e) {
            LOGGER.severe("Role '" + role.name() + "' not found in DB for assignment.");
            throw new RoleNotFoundException("Role '" + role.name() + "' not found in the system.");
        }

        if (user.getRoles().contains(dbRole)) {
            LOGGER.info("User " + userId + " already has role " + role.name() + ".");
            return true;
        }

        user.addRole(dbRole);
        LOGGER.info("Role " + role.name() + " assigned to user ID: " + userId + ".");
        return true;
    }

    @Override
    public boolean removeUser(Long userId) {
        LOGGER.warning("Attempting to remove user with ID: " + userId + ". (Hard delete is configured)");
        User user = getUserById(userId); // Reuse getUserById to get user or throw UserNotFoundException

        em.remove(user);
        LOGGER.info("User with ID " + userId + " has been permanently deleted.");
        return true;

        /*
        // RECOMMENDED: Soft delete
        // LOGGER.info("Attempting to deactivate user with ID: " + userId);
        // User user = getUserById(userId);
        // user.setIsActive(false);
        // LOGGER.info("User with ID " + userId + " has been deactivated.");
        // return true;
        */
    }

    @Override
    public User register(RegisterUserDto registerUserDto) {
        return null;
    }

    @Override
    public User register(String username, String password, UserRole userRole) {
        return null;
    }
}