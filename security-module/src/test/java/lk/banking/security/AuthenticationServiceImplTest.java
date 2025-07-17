package lk.banking.security; // Assuming this test class is in the same package as AuthenticationServiceImpl or has access

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import lk.banking.core.entity.Role; // Assuming Role is needed for User setup
import lk.banking.core.entity.User;
import lk.banking.core.entity.enums.UserRole; // Assuming UserRole is used in Role constructor
import lk.banking.core.exception.UnauthorizedAccessException;
import lk.banking.core.exception.UserNotFoundException;
import lk.banking.core.exception.ValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic; // Import for static mocking
import org.mockito.Mockito;      // Import for Mockito static methods
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationServiceImpl Unit Tests")
public class AuthenticationServiceImplTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Mock
    private TypedQuery<User> mockUserTypedQuery;

    // Test data
    private User activeUser;
    private User inactiveUser;
    private String correctPassword = "CorrectPassword1!";
    private String correctHashedPassword = "hashedCorrectPassword"; // Mocked hash
    private String wrongPassword = "WrongPassword1!";
    private String weakNewPassword = "weak";
    private String strongNewPassword = "NewStrongPassword1@";
    private String newHashedPassword = "hashedNewStrongPassword"; // Mocked hash

    @BeforeEach
    void setUp() {
        Set<Role> roles = new HashSet<>();
        roles.add(new Role(UserRole.CUSTOMER));

        activeUser = new User("testuser", correctHashedPassword, "test@example.com", "1234567890", roles);
        activeUser.setId(1L);
        activeUser.setIsActive(true);

        inactiveUser = new User("banneduser", correctHashedPassword, "banned@example.com", "0987654321", roles);
        inactiveUser.setId(2L);
        inactiveUser.setIsActive(false); // Crucial for inactive user tests
    }

    // --- Test authenticate method ---

    @Test
    @DisplayName("should authenticate an active user with correct credentials successfully")
    void authenticate_Success() {
        try (MockedStatic<PasswordService> mockedPasswordService = Mockito.mockStatic(PasswordService.class)) {
            // Given
            when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(mockUserTypedQuery);
            when(mockUserTypedQuery.setParameter(anyString(), eq(activeUser.getUsername()))).thenReturn(mockUserTypedQuery);
            when(mockUserTypedQuery.getSingleResult()).thenReturn(activeUser);

            // Mock static PasswordService methods
            mockedPasswordService.when(() -> PasswordService.verifyPassword(correctPassword, activeUser.getPassword())).thenReturn(true);

            // When
            User authenticatedUser = authenticationService.authenticate(activeUser.getUsername(), correctPassword);

            // Then
            assertThat(authenticatedUser).isNotNull();
            assertThat(authenticatedUser.getUsername()).isEqualTo(activeUser.getUsername());
            verify(entityManager, times(1)).createQuery(anyString(), eq(User.class));
            mockedPasswordService.verify(() -> PasswordService.verifyPassword(correctPassword, activeUser.getPassword()), times(1));
        }
    }

    @Test
    @DisplayName("should throw UnauthorizedAccessException when username not found during authentication")
    void authenticate_UserNotFound() {
        try (MockedStatic<PasswordService> mockedPasswordService = Mockito.mockStatic(PasswordService.class)) {
            // Given
            String nonExistentUsername = "nonexistent";
            when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(mockUserTypedQuery);
            when(mockUserTypedQuery.setParameter(anyString(), eq(nonExistentUsername))).thenReturn(mockUserTypedQuery);
            when(mockUserTypedQuery.getSingleResult()).thenThrow(NoResultException.class);

            // When / Then
            assertThatThrownBy(() -> authenticationService.authenticate(nonExistentUsername, correctPassword))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessageContaining("Invalid credentials provided.");

            verify(entityManager, times(1)).createQuery(anyString(), eq(User.class));
            mockedPasswordService.verify(() -> PasswordService.verifyPassword(anyString(), anyString()), never()); // Password verification should not be called
        }
    }

    @Test
    @DisplayName("should throw UnauthorizedAccessException when password is incorrect during authentication")
    void authenticate_IncorrectPassword() {
        try (MockedStatic<PasswordService> mockedPasswordService = Mockito.mockStatic(PasswordService.class)) {
            // Given
            when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(mockUserTypedQuery);
            when(mockUserTypedQuery.setParameter(anyString(), eq(activeUser.getUsername()))).thenReturn(mockUserTypedQuery);
            when(mockUserTypedQuery.getSingleResult()).thenReturn(activeUser);

            // Mock static PasswordService methods to return false for verification
            mockedPasswordService.when(() -> PasswordService.verifyPassword(wrongPassword, activeUser.getPassword())).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> authenticationService.authenticate(activeUser.getUsername(), wrongPassword))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessageContaining("Invalid credentials provided.");

            verify(entityManager, times(1)).createQuery(anyString(), eq(User.class));
            mockedPasswordService.verify(() -> PasswordService.verifyPassword(wrongPassword, activeUser.getPassword()), times(1));
        }
    }

    @Test
    @DisplayName("should throw UnauthorizedAccessException for an inactive user during authentication")
    void authenticate_InactiveUser() {
        try (MockedStatic<PasswordService> mockedPasswordService = Mockito.mockStatic(PasswordService.class)) {
            // Given
            when(entityManager.createQuery(anyString(), eq(User.class))).thenReturn(mockUserTypedQuery);
            when(mockUserTypedQuery.setParameter(anyString(), eq(inactiveUser.getUsername()))).thenReturn(mockUserTypedQuery);
            when(mockUserTypedQuery.getSingleResult()).thenReturn(inactiveUser); // Returns an inactive user

            // The password verification is not expected to be called for inactive users.
            // No need to mock PasswordService.verifyPassword for this specific scenario.

            // When / Then
            assertThatThrownBy(() -> authenticationService.authenticate(inactiveUser.getUsername(), correctPassword))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessageContaining("You're banned temporarily please contact support team 0372250045.");

            verify(entityManager, times(1)).createQuery(anyString(), eq(User.class));
            // IMPORTANT CHANGE: Verify that PasswordService.verifyPassword was NOT called.
            mockedPasswordService.verify(() -> PasswordService.verifyPassword(anyString(), anyString()), never());
        }
    }

    // --- Test changePassword method ---

    @Test
    @DisplayName("should change password successfully for an existing user")
    void changePassword_Success() {
        try (MockedStatic<PasswordService> mockedPasswordService = Mockito.mockStatic(PasswordService.class)) {
            // Given
            when(entityManager.find(eq(User.class), eq(activeUser.getId()))).thenReturn(activeUser);

            mockedPasswordService.when(() -> PasswordService.verifyPassword(correctPassword, activeUser.getPassword())).thenReturn(true);
            mockedPasswordService.when(() -> PasswordService.isPasswordStrong(strongNewPassword)).thenReturn(true);
            mockedPasswordService.when(() -> PasswordService.hashPassword(strongNewPassword)).thenReturn(newHashedPassword);

            // When
            boolean result = authenticationService.changePassword(activeUser.getId(), correctPassword, strongNewPassword);

            // Then
            assertThat(result).isTrue();
            assertThat(activeUser.getPassword()).isEqualTo(newHashedPassword); // Verify user object was updated
            verify(entityManager, times(1)).find(eq(User.class), eq(activeUser.getId()));
            mockedPasswordService.verify(() -> PasswordService.verifyPassword(correctPassword, correctHashedPassword), times(1));
            mockedPasswordService.verify(() -> PasswordService.isPasswordStrong(strongNewPassword), times(1));
            mockedPasswordService.verify(() -> PasswordService.hashPassword(strongNewPassword), times(1));
        }
    }

    @Test
    @DisplayName("should throw UserNotFoundException when changing password for non-existent user")
    void changePassword_UserNotFound() {
        try (MockedStatic<PasswordService> mockedPasswordService = Mockito.mockStatic(PasswordService.class)) {
            // Given
            Long nonExistentUserId = 99L;
            when(entityManager.find(eq(User.class), eq(nonExistentUserId))).thenReturn(null);

            // When / Then
            assertThatThrownBy(() -> authenticationService.changePassword(nonExistentUserId, correctPassword, strongNewPassword))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User with ID " + nonExistentUserId + " not found.");

            verify(entityManager, times(1)).find(eq(User.class), eq(nonExistentUserId));
            mockedPasswordService.verifyNoInteractions(); // No PasswordService methods should be called
        }
    }

    @Test
    @DisplayName("should throw UnauthorizedAccessException when old password mismatches during change password")
    void changePassword_OldPasswordMismatch() {
        try (MockedStatic<PasswordService> mockedPasswordService = Mockito.mockStatic(PasswordService.class)) {
            // Given
            when(entityManager.find(eq(User.class), eq(activeUser.getId()))).thenReturn(activeUser);

            mockedPasswordService.when(() -> PasswordService.verifyPassword(wrongPassword, activeUser.getPassword())).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> authenticationService.changePassword(activeUser.getId(), wrongPassword, strongNewPassword))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessageContaining("Old password does not match.");

            verify(entityManager, times(1)).find(eq(User.class), eq(activeUser.getId()));
            mockedPasswordService.verify(() -> PasswordService.verifyPassword(wrongPassword, activeUser.getPassword()), times(1));
            mockedPasswordService.verify(() -> PasswordService.isPasswordStrong(anyString()), never()); // Should not proceed to strong check
            mockedPasswordService.verify(() -> PasswordService.hashPassword(anyString()), never()); // Should not proceed to hash
        }
    }

    @Test
    @DisplayName("should throw ValidationException when new password is not strong enough during change password")
    void changePassword_NewPasswordNotStrong() {
        try (MockedStatic<PasswordService> mockedPasswordService = Mockito.mockStatic(PasswordService.class)) {
            // Given
            when(entityManager.find(eq(User.class), eq(activeUser.getId()))).thenReturn(activeUser);

            mockedPasswordService.when(() -> PasswordService.verifyPassword(correctPassword, activeUser.getPassword())).thenReturn(true);
            mockedPasswordService.when(() -> PasswordService.isPasswordStrong(weakNewPassword)).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> authenticationService.changePassword(activeUser.getId(), correctPassword, weakNewPassword))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("New password does not meet complexity requirements.");

            verify(entityManager, times(1)).find(eq(User.class), eq(activeUser.getId()));
            mockedPasswordService.verify(() -> PasswordService.verifyPassword(correctPassword, activeUser.getPassword()), times(1));
            mockedPasswordService.verify(() -> PasswordService.isPasswordStrong(weakNewPassword), times(1));
            mockedPasswordService.verify(() -> PasswordService.hashPassword(anyString()), never()); // Should not proceed to hash
        }
    }
}