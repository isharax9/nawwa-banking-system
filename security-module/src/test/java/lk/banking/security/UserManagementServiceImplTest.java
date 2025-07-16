package lk.banking.security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.TypedQuery;
import lk.banking.core.entity.Customer;
import lk.banking.core.entity.Role;
import lk.banking.core.entity.User;
import lk.banking.core.entity.enums.UserRole;
import lk.banking.core.exception.ResourceConflictException;
import lk.banking.core.exception.RoleNotFoundException;
import lk.banking.core.exception.UserNotFoundException;
import lk.banking.core.exception.ValidationException;
import lk.banking.core.util.ValidationUtils; // Import the static utility class

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic; // For static mocking
import org.mockito.Mockito;      // For Mockito static methods
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Arrays; // For List.of in tests

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserManagementServiceImpl Unit Tests")
public class UserManagementServiceImplTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private UserManagementServiceImpl userManagementService;

    // Mocks for TypedQuery fields (only for simple cases, sequential will use local mocks)
    // Removed @Mock on mockUserQuery, mockCustomerQuery, mockRoleQuery as fields.
    // They will be created locally within methods for sequential calls for clarity and to avoid conflicts.

    // Test Data
    private User testUser;
    private Role customerRole;
    private Role adminRole;
    private Customer testCustomer;

    private String validUsername = "newuser";
    private String validPassword = "StrongPassword1!";
    private String validEmail = "newuser@example.com";
    private String validName = "New User";
    private String validAddress = "123 Test St";
    private String validPhoneNumber = "0771234567";
    private String hashedPassword = "hashedValidPassword";

    @BeforeEach
    void setUp() {
        customerRole = new Role(UserRole.CUSTOMER);
        customerRole.setId(1L);

        adminRole = new Role(UserRole.ADMIN);
        adminRole.setId(2L);

        testUser = new User(validUsername, hashedPassword, validEmail, validPhoneNumber, new HashSet<>(Collections.singletonList(customerRole)));
        testUser.setId(1L);
        testUser.setIsActive(true);

        testCustomer = new Customer(validName, validEmail, validAddress, validPhoneNumber);
        testCustomer.setId(1L);
    }
    // --- Test register method ---
    @Test
    @DisplayName("should throw ValidationException for missing fields during registration")
    void register_MissingFields() {
        try (MockedStatic<PasswordService> mockedPasswordService = Mockito.mockStatic(PasswordService.class);
             MockedStatic<ValidationUtils> mockedValidationUtils = Mockito.mockStatic(ValidationUtils.class)) {
            // Given (e.g., null username)
            String invalidUsername = null;

            // When / Then
            assertThatThrownBy(() -> userManagementService.register(invalidUsername, validPassword, validEmail, validName, validAddress, validPhoneNumber, UserRole.CUSTOMER))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("All registration fields (username, password, email, name, address, phone number) are required.");

            verify(entityManager, never()).createQuery(anyString(), any());
            verify(entityManager, never()).persist(any());
            mockedPasswordService.verifyNoInteractions();
            mockedValidationUtils.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("should throw ValidationException for weak password during registration")
    void register_WeakPassword() {
        try (MockedStatic<PasswordService> mockedPasswordService = Mockito.mockStatic(PasswordService.class);
             MockedStatic<ValidationUtils> mockedValidationUtils = Mockito.mockStatic(ValidationUtils.class)) {
            // Given
            String weakPassword = "weak";
            mockedPasswordService.when(() -> PasswordService.isPasswordStrong(weakPassword)).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> userManagementService.register(validUsername, weakPassword, validEmail, validName, validAddress, validPhoneNumber, UserRole.CUSTOMER))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Password does not meet complexity requirements.");

            verify(entityManager, never()).createQuery(anyString(), any());
            verify(entityManager, never()).persist(any());
            mockedPasswordService.verify(() -> PasswordService.isPasswordStrong(weakPassword), times(1));
            mockedValidationUtils.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("should throw ValidationException for invalid email format during registration")
    void register_InvalidEmailFormat() {
        try (MockedStatic<PasswordService> mockedPasswordService = Mockito.mockStatic(PasswordService.class);
             MockedStatic<ValidationUtils> mockedValidationUtils = Mockito.mockStatic(ValidationUtils.class)) {
            // Given
            String invalidEmail = "invalid-email";
            mockedPasswordService.when(() -> PasswordService.isPasswordStrong(validPassword)).thenReturn(true);
            mockedValidationUtils.when(() -> ValidationUtils.isValidEmail(invalidEmail)).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> userManagementService.register(validUsername, validPassword, invalidEmail, validName, validAddress, validPhoneNumber, UserRole.CUSTOMER))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid email format.");

            verify(entityManager, never()).createQuery(anyString(), any());
            verify(entityManager, never()).persist(any());
            mockedPasswordService.verify(() -> PasswordService.isPasswordStrong(validPassword), times(1));
            mockedValidationUtils.verify(() -> ValidationUtils.isValidEmail(invalidEmail), times(1));
        }
    }

    @Test
    @DisplayName("should throw ValidationException for invalid phone number format during registration")
    void register_InvalidPhoneNumberFormat() {
        try (MockedStatic<PasswordService> mockedPasswordService = Mockito.mockStatic(PasswordService.class);
             MockedStatic<ValidationUtils> mockedValidationUtils = Mockito.mockStatic(ValidationUtils.class)) {
            // Given
            String invalidPhoneNumber = "123"; // Too short
            mockedPasswordService.when(() -> PasswordService.isPasswordStrong(validPassword)).thenReturn(true);
            mockedValidationUtils.when(() -> ValidationUtils.isValidEmail(validEmail)).thenReturn(true);
            mockedValidationUtils.when(() -> ValidationUtils.isValidPhoneNumber(invalidPhoneNumber)).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> userManagementService.register(validUsername, validPassword, validEmail, validName, validAddress, invalidPhoneNumber, UserRole.CUSTOMER))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Invalid phone number format. Must be 10-15 digits.");

            verify(entityManager, never()).createQuery(anyString(), any());
            verify(entityManager, never()).persist(any());
            mockedPasswordService.verify(() -> PasswordService.isPasswordStrong(validPassword), times(1));
            mockedValidationUtils.verify(() -> ValidationUtils.isValidEmail(validEmail), times(1));
            mockedValidationUtils.verify(() -> ValidationUtils.isValidPhoneNumber(invalidPhoneNumber), times(1));
        }
    }

    @Test
    @DisplayName("should throw ResourceConflictException when username already exists")
    void register_UsernameAlreadyExists() {
        try (MockedStatic<PasswordService> mockedPasswordService = Mockito.mockStatic(PasswordService.class);
             MockedStatic<ValidationUtils> mockedValidationUtils = Mockito.mockStatic(ValidationUtils.class)) {

            // Given
            mockedPasswordService.when(() -> PasswordService.isPasswordStrong(validPassword)).thenReturn(true);
            mockedValidationUtils.when(() -> ValidationUtils.isValidEmail(validEmail)).thenReturn(true);
            mockedValidationUtils.when(() -> ValidationUtils.isValidPhoneNumber(validPhoneNumber)).thenReturn(true);

            // Create a specific TypedQuery mock for username check
            TypedQuery<User> usernameCheckQuery = mock(TypedQuery.class);
            when(entityManager.createQuery(anyString(), eq(User.class)))
                    .thenReturn(usernameCheckQuery); // This is the first User query

            // Mock username check to return existing user
            when(usernameCheckQuery.setParameter(eq("username"), eq(validUsername))).thenReturn(usernameCheckQuery);
            when(usernameCheckQuery.getSingleResult()).thenReturn(testUser); // Username already exists


            // When / Then
            assertThatThrownBy(() -> userManagementService.register(validUsername, validPassword, validEmail, validName, validAddress, validPhoneNumber, UserRole.CUSTOMER))
                    .isInstanceOf(ResourceConflictException.class)
                    .hasMessageContaining("Username '" + validUsername + "' already exists.");

            verify(entityManager, never()).persist(any()); // Should not persist
            mockedPasswordService.verify(() -> PasswordService.hashPassword(anyString()), never());
        }
    }



    @Test
    @DisplayName("should throw RoleNotFoundException when the specified role does not exist")
    void register_RoleNotFound() {
        try (MockedStatic<PasswordService> mockedPasswordService = Mockito.mockStatic(PasswordService.class);
             MockedStatic<ValidationUtils> mockedValidationUtils = Mockito.mockStatic(ValidationUtils.class)) {

            // Given
            mockedPasswordService.when(() -> PasswordService.isPasswordStrong(validPassword)).thenReturn(true);
            mockedValidationUtils.when(() -> ValidationUtils.isValidEmail(validEmail)).thenReturn(true); // Corrected typo here
            mockedValidationUtils.when(() -> ValidationUtils.isValidPhoneNumber(validPhoneNumber)).thenReturn(true);

            // Create specific TypedQuery mocks for each distinct query in sequence
            TypedQuery<User> usernameCheckQuery = mock(TypedQuery.class);
            TypedQuery<User> userEmailCheckQuery = mock(TypedQuery.class);
            TypedQuery<Customer> customerEmailCheckQuery = mock(TypedQuery.class);
            TypedQuery<Role> roleLookupQuery = mock(TypedQuery.class);

            // Mock sequential calls to entityManager.createQuery
            when(entityManager.createQuery(anyString(), eq(User.class)))
                    .thenReturn(usernameCheckQuery)
                    .thenReturn(userEmailCheckQuery);

            when(entityManager.createQuery(anyString(), eq(Customer.class)))
                    .thenReturn(customerEmailCheckQuery);

            when(entityManager.createQuery(anyString(), eq(Role.class)))
                    .thenReturn(roleLookupQuery);


            // Stub all uniqueness checks to pass
            when(usernameCheckQuery.setParameter(anyString(), anyString())).thenReturn(usernameCheckQuery);
            when(usernameCheckQuery.getSingleResult()).thenThrow(NoResultException.class);
            when(userEmailCheckQuery.setParameter(anyString(), anyString())).thenReturn(userEmailCheckQuery);
            when(userEmailCheckQuery.getSingleResult()).thenThrow(NoResultException.class);
            when(customerEmailCheckQuery.setParameter(anyString(), anyString())).thenReturn(customerEmailCheckQuery);
            when(customerEmailCheckQuery.getSingleResult()).thenThrow(NoResultException.class);

            // Mock Role lookup to throw NoResultException
            when(roleLookupQuery.setParameter(eq("name"), eq(UserRole.ADMIN))).thenReturn(roleLookupQuery);
            when(roleLookupQuery.getSingleResult()).thenThrow(NoResultException.class); // Role not found


            // When / Then
            assertThatThrownBy(() -> userManagementService.register(validUsername, validPassword, validEmail, validName, validAddress, validPhoneNumber, UserRole.ADMIN))
                    .isInstanceOf(RoleNotFoundException.class)
                    .hasMessageContaining("Role '" + UserRole.ADMIN.name() + "' not found in the system.");

            verify(entityManager, never()).persist(any()); // Should not persist
            mockedPasswordService.verify(() -> PasswordService.hashPassword(anyString()), never());
        }
    }


    // --- Test getUserById method ---
    @Test
    @DisplayName("should retrieve user by ID successfully")
    void getUserById_Success() {
        // Given
        when(entityManager.find(eq(User.class), eq(testUser.getId()))).thenReturn(testUser);

        // When
        User foundUser = userManagementService.getUserById(testUser.getId());

        // Then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(testUser.getId());
        assertThat(foundUser.getUsername()).isEqualTo(testUser.getUsername());
        verify(entityManager, times(1)).find(eq(User.class), eq(testUser.getId()));
    }

    @Test
    @DisplayName("should throw UserNotFoundException when getting non-existent user by ID")
    void getUserById_NotFound() {
        // Given
        Long nonExistentId = 99L;
        when(entityManager.find(eq(User.class), eq(nonExistentId))).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> userManagementService.getUserById(nonExistentId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User with ID " + nonExistentId + " not found.");
        verify(entityManager, times(1)).find(eq(User.class), eq(nonExistentId));
    }

    // --- Test getUserByUsername method ---
    @Test
    @DisplayName("should retrieve user by username successfully")
    void getUserByUsername_Success() {
        // Given
        // Create a specific TypedQuery mock for username lookup
        TypedQuery<User> usernameLookupQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(usernameLookupQuery);

        when(usernameLookupQuery.setParameter(anyString(), eq(testUser.getUsername()))).thenReturn(usernameLookupQuery);
        when(usernameLookupQuery.getSingleResult()).thenReturn(testUser);

        // When
        User foundUser = userManagementService.getUserByUsername(testUser.getUsername());

        // Then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getUsername()).isEqualTo(testUser.getUsername());
        verify(entityManager, times(1)).createQuery(anyString(), eq(User.class));
        verify(usernameLookupQuery, times(1)).setParameter(anyString(), eq(testUser.getUsername()));
        verify(usernameLookupQuery, times(1)).getSingleResult();
    }

    @Test
    @DisplayName("should throw UserNotFoundException when getting non-existent user by username")
    void getUserByUsername_NotFound() {
        // Given
        String nonExistentUsername = "nonexistent";
        // Create a specific TypedQuery mock for username lookup
        TypedQuery<User> usernameLookupQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(usernameLookupQuery);

        when(usernameLookupQuery.setParameter(anyString(), eq(nonExistentUsername))).thenReturn(usernameLookupQuery);
        when(usernameLookupQuery.getSingleResult()).thenThrow(NoResultException.class);

        // When / Then
        assertThatThrownBy(() -> userManagementService.getUserByUsername(nonExistentUsername))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User with username '" + nonExistentUsername + "' not found.");
        verify(entityManager, times(1)).createQuery(anyString(), eq(User.class));
        verify(usernameLookupQuery, times(1)).setParameter(anyString(), eq(nonExistentUsername));
        verify(usernameLookupQuery, times(1)).getSingleResult();
    }

    @Test
    @DisplayName("should throw ResourceConflictException when multiple users found for username")
    void getUserByUsername_NonUniqueResult() {
        // Given
        // Create a specific TypedQuery mock for username lookup
        TypedQuery<User> usernameLookupQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(usernameLookupQuery);

        when(usernameLookupQuery.setParameter(anyString(), eq(testUser.getUsername()))).thenReturn(usernameLookupQuery);
        when(usernameLookupQuery.getSingleResult()).thenThrow(NonUniqueResultException.class);

        // When / Then
        assertThatThrownBy(() -> userManagementService.getUserByUsername(testUser.getUsername()))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Multiple users found for username " + testUser.getUsername());
        verify(entityManager, times(1)).createQuery(anyString(), eq(User.class));
        verify(usernameLookupQuery, times(1)).setParameter(anyString(), eq(testUser.getUsername()));
        verify(usernameLookupQuery, times(1)).getSingleResult();
    }

    // --- Test getAllUsers method ---
    @Test
    @DisplayName("should retrieve all users successfully")
    void getAllUsers_Success() {
        // Given
        User anotherUser = new User("another", "hash", "another@example.com", "999888777", Collections.emptySet());
        anotherUser.setId(2L);
        List<User> allUsers = Arrays.asList(testUser, anotherUser);

        // Create a specific TypedQuery mock for getAllUsers
        TypedQuery<User> allUsersQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(allUsersQuery);

        when(allUsersQuery.getResultList()).thenReturn(allUsers);

        // When
        List<User> foundUsers = userManagementService.getAllUsers();

        // Then
        assertThat(foundUsers).containsExactlyInAnyOrder(testUser, anotherUser);
        verify(entityManager, times(1)).createQuery(anyString(), eq(User.class));
        verify(allUsersQuery, times(1)).getResultList();
    }

    @Test
    @DisplayName("should return empty list when no users found")
    void getAllUsers_NoUsers() {
        // Given
        // Create a specific TypedQuery mock for getAllUsers
        TypedQuery<User> allUsersQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(allUsersQuery);

        when(allUsersQuery.getResultList()).thenReturn(Collections.emptyList());

        // When
        List<User> foundUsers = userManagementService.getAllUsers();

        // Then
        assertThat(foundUsers).isEmpty();
        verify(entityManager, times(1)).createQuery(anyString(), eq(User.class));
        verify(allUsersQuery, times(1)).getResultList();
    }

    // --- Test assignRole method ---
    @Test
    @DisplayName("should assign a role to a user successfully")
    void assignRole_Success() {
        // Given
        User userWithoutAdminRole = new User("devuser", "hash", "dev@example.com", "111222333", new HashSet<>(Collections.singletonList(customerRole)));
        userWithoutAdminRole.setId(3L);
        userWithoutAdminRole.setIsActive(true);
        when(entityManager.find(eq(User.class), eq(userWithoutAdminRole.getId()))).thenReturn(userWithoutAdminRole);

        // Create a specific TypedQuery mock for role lookup
        TypedQuery<Role> roleLookupQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Role.class))).thenReturn(roleLookupQuery);

        when(roleLookupQuery.setParameter(anyString(), eq(UserRole.ADMIN))).thenReturn(roleLookupQuery);
        when(roleLookupQuery.getSingleResult()).thenReturn(adminRole);

        // When
        boolean result = userManagementService.assignRole(userWithoutAdminRole.getId(), UserRole.ADMIN);

        // Then
        assertThat(result).isTrue();
        assertThat(userWithoutAdminRole.getRoles()).contains(adminRole);
        verify(entityManager, times(1)).find(eq(User.class), eq(userWithoutAdminRole.getId()));
        verify(entityManager, times(1)).createQuery(anyString(), eq(Role.class));
    }

    @Test
    @DisplayName("should return true if user already has the role")
    void assignRole_AlreadyHasRole() {
        // Given
        User userWithCustomerRole = new User("customeruser", "hash", "cust@example.com", "444555666", new HashSet<>(Collections.singletonList(customerRole)));
        userWithCustomerRole.setId(4L);
        userWithCustomerRole.setIsActive(true);
        when(entityManager.find(eq(User.class), eq(userWithCustomerRole.getId()))).thenReturn(userWithCustomerRole);

        // Create a specific TypedQuery mock for role lookup
        TypedQuery<Role> roleLookupQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Role.class))).thenReturn(roleLookupQuery);

        when(roleLookupQuery.setParameter(anyString(), eq(UserRole.CUSTOMER))).thenReturn(roleLookupQuery);
        when(roleLookupQuery.getSingleResult()).thenReturn(customerRole);

        // When
        boolean result = userManagementService.assignRole(userWithCustomerRole.getId(), UserRole.CUSTOMER);

        // Then
        assertThat(result).isTrue();
        assertThat(userWithCustomerRole.getRoles()).contains(customerRole); // Role set should be unchanged
        verify(entityManager, times(1)).find(eq(User.class), eq(userWithCustomerRole.getId()));
        verify(entityManager, times(1)).createQuery(anyString(), eq(Role.class));
    }

    @Test
    @DisplayName("should throw UserNotFoundException when assigning role to non-existent user")
    void assignRole_UserNotFound() {
        // Given
        Long nonExistentUserId = 99L;
        when(entityManager.find(eq(User.class), eq(nonExistentUserId))).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> userManagementService.assignRole(nonExistentUserId, UserRole.ADMIN))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User with ID " + nonExistentUserId + " not found.");
        verify(entityManager, never()).createQuery(anyString(), any());
    }

    @Test
    @DisplayName("should throw RoleNotFoundException when assigning a non-existent role")
    void assignRole_RoleNotFound() {
        // Given
        when(entityManager.find(eq(User.class), eq(testUser.getId()))).thenReturn(testUser); // User exists

        // Create a specific TypedQuery mock for role lookup
        TypedQuery<Role> roleLookupQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Role.class))).thenReturn(roleLookupQuery);

        when(roleLookupQuery.setParameter(anyString(), eq(UserRole.ADMIN))).thenReturn(roleLookupQuery);
        when(roleLookupQuery.getSingleResult()).thenThrow(NoResultException.class); // Role not found


        // When / Then
        assertThatThrownBy(() -> userManagementService.assignRole(testUser.getId(), UserRole.ADMIN))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessageContaining("Role '" + UserRole.ADMIN.name() + "' not found in the system.");
        verify(entityManager, times(1)).find(eq(User.class), eq(testUser.getId()));
        verify(entityManager, times(1)).createQuery(anyString(), eq(Role.class));
    }


    // --- Test removeUser (deactivate) method ---
    @Test
    @DisplayName("should deactivate an active user successfully")
    void removeUser_Success() {
        // Given
        User activeUser = new User("active", "hash", "active@example.com", "111222333", Collections.emptySet());
        activeUser.setId(5L);
        activeUser.setIsActive(true);
        when(entityManager.find(eq(User.class), eq(activeUser.getId()))).thenReturn(activeUser);

        // When
        boolean result = userManagementService.removeUser(activeUser.getId());

        // Then
        assertThat(result).isTrue();
        assertThat(activeUser.getIsActive()).isFalse(); // Verify status changed
        verify(entityManager, times(1)).find(eq(User.class), eq(activeUser.getId()));
    }

    @Test
    @DisplayName("should return true if user is already inactive when calling removeUser")
    void removeUser_AlreadyInactive() {
        // Given
        User inactiveUser = new User("inactive", "hash", "inactive@example.com", "444555666", Collections.emptySet());
        inactiveUser.setId(6L);
        inactiveUser.setIsActive(false); // Already inactive
        when(entityManager.find(eq(User.class), eq(inactiveUser.getId()))).thenReturn(inactiveUser);

        // When
        boolean result = userManagementService.removeUser(inactiveUser.getId());

        // Then
        assertThat(result).isTrue();
        assertThat(inactiveUser.getIsActive()).isFalse(); // Status remains false
        verify(entityManager, times(1)).find(eq(User.class), eq(inactiveUser.getId()));
    }

    @Test
    @DisplayName("should throw UserNotFoundException when removing non-existent user")
    void removeUser_NotFound() {
        // Given
        Long nonExistentUserId = 99L;
        when(entityManager.find(eq(User.class), eq(nonExistentUserId))).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> userManagementService.removeUser(nonExistentUserId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User with ID " + nonExistentUserId + " not found.");
        verify(entityManager, times(1)).find(eq(User.class), eq(nonExistentUserId));
    }

    // --- Test activateUser method ---
    @Test
    @DisplayName("should activate an inactive user successfully")
    void activateUser_Success() {
        // Given
        User inactiveUser = new User("inactive", "hash", "inactive@example.com", "444555666", Collections.emptySet());
        inactiveUser.setId(6L);
        inactiveUser.setIsActive(false); // Start as inactive
        when(entityManager.find(eq(User.class), eq(inactiveUser.getId()))).thenReturn(inactiveUser);

        // When
        boolean result = userManagementService.activateUser(inactiveUser.getId());

        // Then
        assertThat(result).isTrue();
        assertThat(inactiveUser.getIsActive()).isTrue(); // Verify status changed
        verify(entityManager, times(1)).find(eq(User.class), eq(inactiveUser.getId()));
    }

    @Test
    @DisplayName("should return true if user is already active when calling activateUser")
    void activateUser_AlreadyActive() {
        // Given
        User activeUser = new User("active", "hash", "active@example.com", "111222333", Collections.emptySet());
        activeUser.setId(5L);
        activeUser.setIsActive(true); // Already active
        when(entityManager.find(eq(User.class), eq(activeUser.getId()))).thenReturn(activeUser);

        // When
        boolean result = userManagementService.activateUser(activeUser.getId());

        // Then
        assertThat(result).isTrue();
        assertThat(activeUser.getIsActive()).isTrue(); // Status remains true
        verify(entityManager, times(1)).find(eq(User.class), eq(activeUser.getId()));
    }

    @Test
    @DisplayName("should throw UserNotFoundException when activating non-existent user")
    void activateUser_NotFound() {
        // Given
        Long nonExistentUserId = 99L;
        when(entityManager.find(eq(User.class), eq(nonExistentUserId))).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> userManagementService.activateUser(nonExistentUserId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User with ID " + nonExistentUserId + " not found.");
        verify(entityManager, times(1)).find(eq(User.class), eq(nonExistentUserId));
    }
}