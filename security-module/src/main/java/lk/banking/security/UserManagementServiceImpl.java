package lk.banking.security;

import jakarta.annotation.security.RolesAllowed; // IMPORT THIS
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NonUniqueResultException;
import lk.banking.core.entity.Customer;
import lk.banking.core.entity.Role;
import lk.banking.core.entity.User;
import lk.banking.core.entity.enums.UserRole;
import lk.banking.core.exception.ResourceConflictException;
import lk.banking.core.exception.RoleNotFoundException;
import lk.banking.core.exception.UserNotFoundException;
import lk.banking.core.exception.ValidationException;
import lk.banking.core.util.ValidationUtils;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@Stateless
public class UserManagementServiceImpl implements UserManagementService {

    private static final Logger LOGGER = Logger.getLogger(UserManagementServiceImpl.class.getName());

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    @RolesAllowed({"CUSTOMER", "EMPLOYEE", "ADMIN"}) // Allow CUSTOMER for self-registration
    public User register(String username, String password, String email, String name, String address, String phoneNumber, UserRole role) {
        LOGGER.info("Attempting to register new user: " + username + " with email: " + email);

        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty() ||
                email == null || email.trim().isEmpty() || name == null || name.trim().isEmpty() ||
                address == null || address.trim().isEmpty() || phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new ValidationException("All registration fields (username, password, email, name, address, phone number) are required.");
        }
        if (!PasswordService.isPasswordStrong(password)) {
            throw new ValidationException("Password does not meet complexity requirements.");
        }
        if (!ValidationUtils.isValidEmail(email)) {
            throw new ValidationException("Invalid email format.");
        }
        if (!ValidationUtils.isValidPhoneNumber(phoneNumber)) {
            throw new ValidationException("Invalid phone number format. Must be 10-15 digits.");
        }

        try {
            em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
            throw new ResourceConflictException("Username '" + username + "' already exists.");
        } catch (NoResultException e) { }
        catch (NonUniqueResultException e) {
            LOGGER.severe("Database integrity error: Multiple users found for username " + username);
            throw new ResourceConflictException("A critical error occurred: Duplicate username found.");
        }

        try {
            em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
            throw new ResourceConflictException("Email '" + email + "' is already registered with a user account.");
        } catch (NoResultException e) { }

        try {
            em.createQuery("SELECT c FROM Customer c WHERE c.email = :email", Customer.class)
                    .setParameter("email", email)
                    .getSingleResult();
            throw new ResourceConflictException("Email '" + email + "' is already registered with a customer profile.");
        } catch (NoResultException e) { }


        Role dbRole = null;
        try {
            dbRole = em.createQuery("SELECT r FROM Role r WHERE r.name = :name", Role.class)
                    .setParameter("name", role)
                    .getSingleResult();
        } catch (NoResultException e) {
            LOGGER.severe("Role '" + role.name() + "' not found in DB. Please ensure roles are pre-populated.");
            throw new RoleNotFoundException("Role '" + role.name() + "' not found in the system. Roles must be pre-configured.");
        }

        String hashedPassword = PasswordService.hashPassword(password);

        User user = new User();
        user.setUsername(username);
        user.setPassword(hashedPassword);
        user.setEmail(email);
        user.setPhone(phoneNumber);
        user.setIsActive(true);
        user.addRole(dbRole);

        em.persist(user);

        if (role == UserRole.CUSTOMER) {
            Customer customer = new Customer(name, email, address, phoneNumber);
            em.persist(customer);
            LOGGER.info("Created new customer profile for user: " + username + " with email: " + email);
        } else {
            LOGGER.info("User " + username + " registered with role " + role.name() + ". No customer profile created.");
        }

        LOGGER.info("User " + username + " registered successfully.");
        return user;
    }

    @Override
    @RolesAllowed({"ADMIN", "EMPLOYEE"}) // Read access for admins/employees
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
    @RolesAllowed({"ADMIN", "EMPLOYEE"}) // Read access for admins/employees
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
    @RolesAllowed({"ADMIN", "EMPLOYEE"}) // Read access for admins/employees
    public List<User> getAllUsers() {
        LOGGER.fine("Fetching all users.");
        return em.createQuery("SELECT u FROM User u", User.class)
                .getResultList();
    }

    @Override
    @RolesAllowed({"ADMIN"}) // Only ADMIN can assign roles (sensitive)
    public boolean assignRole(Long userId, UserRole role) {
        LOGGER.info("Assigning role " + role.name() + " to user ID: " + userId);
        User user = getUserById(userId); // Throws UserNotFoundException if not found

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
    @RolesAllowed({"ADMIN", "EMPLOYEE"}) // Employees can deactivate, but ADMIN should be able to do anything
    public boolean removeUser(Long userId) {
        LOGGER.info("Attempting to deactivate user with ID: " + userId);
        User user = getUserById(userId); // Throws UserNotFoundException if not found

        if (!user.getIsActive()) {
            LOGGER.info("User with ID " + userId + " is already inactive.");
            return true;
        }

        user.setIsActive(false);
        LOGGER.info("User with ID " + userId + " has been deactivated.");
        return true;
    }

    @Override
    @RolesAllowed({"ADMIN", "EMPLOYEE"}) // Employees can activate
    public boolean activateUser(Long userId) {
        LOGGER.info("Attempting to activate user with ID: " + userId);
        User user = getUserById(userId); // Throws UserNotFoundException if not found

        if (user.getIsActive()) {
            LOGGER.info("User with ID " + userId + " is already active.");
            return true;
        }

        user.setIsActive(true);
        LOGGER.info("User with ID " + userId + " has been activated.");
        return true;
    }
}