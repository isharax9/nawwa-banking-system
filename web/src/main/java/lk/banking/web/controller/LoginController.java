package lk.banking.web.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lk.banking.core.entity.User;
import lk.banking.core.dto.LoggedInUser; // Import the LoggedInUser DTO
import lk.banking.core.exception.UnauthorizedAccessException; // Import for login failures
import lk.banking.security.AuthenticationService;
import lk.banking.web.util.WebUtils;

import java.io.Serializable;
import java.util.stream.Collectors; // Needed for mapping roles if using UserMapper.toDto directly

@Named
@RequestScoped
public class LoginController implements Serializable {

    private String username;
    private String password;

    @Inject
    private AuthenticationService authenticationService;

    /**
     * Handles the user login attempt.
     * Authenticates credentials and, if successful, stores user info in session.
     * @return JSF navigation outcome.
     */
    public String login() {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            // Attempt to authenticate the user. This method throws UnauthorizedAccessException on failure.
            User authenticatedUser = authenticationService.authenticate(username, password);

            // If authentication succeeds, create a LoggedInUser DTO from the User entity
            // UserMapper.toDto returns UserDto, which is close, but LoggedInUser is specifically for session.
            // Let's create LoggedInUser from the authenticated User.
            LoggedInUser loggedInUser = new LoggedInUser(
                    authenticatedUser.getId(),
                    authenticatedUser.getUsername(),
                    authenticatedUser.getEmail(), // Ensure email is available in User entity for customer lookup later
                    authenticatedUser.getRoles().stream()
                            .map(r -> r.getName()) // Map Role entity to UserRole enum
                            .collect(Collectors.toSet()),
                    // For customerId, you need a way to link User to Customer.
                    // If User has a direct customer ID/entity, retrieve it here.
                    // Otherwise, it remains null until we define that link or fetch it separately.
                    null // Placeholder for customerId until User entity is linked to Customer
            );

            // Store the safe LoggedInUser DTO in the session using WebUtils
            WebUtils.setLoggedInUser(loggedInUser);

            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Login Successful!", null));
            return "/pages/dashboard.xhtml?faces-redirect=true"; // Navigate to dashboard
        } catch (UnauthorizedAccessException e) {
            // Catch specific authentication failure exception
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    e.getMessage(), null)); // Use the message from the exception (e.g., "Invalid credentials provided.")
            return null; // Stay on the current page (login page)
        } catch (Exception e) {
            // Catch any other unexpected exceptions (e.g., database issues, NullPointerException)
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_FATAL,
                    "An unexpected error occurred during login. Please try again later.", null));
            System.err.println("Login error: " + e.getMessage()); // Log detailed error
            e.printStackTrace(); // Print stack trace for debugging
            return null;
        }
    }

    /**
     * Handles user logout by invalidating the session.
     * @return JSF navigation outcome.
     */
    public String logout() {
        WebUtils.logoutUser(); // Use WebUtils to invalidate session
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Logged out successfully.", null));
        return "/pages/login.xhtml?faces-redirect=true"; // Navigate back to login page
    }

    // Getters and Setters for JSF form binding
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}