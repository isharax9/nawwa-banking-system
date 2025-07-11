package lk.banking.web.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lk.banking.core.dto.RegisterUserDto; // Import the RegisterUserDto
import lk.banking.core.entity.User; // If you need to store the resulting user, though not directly used here
import lk.banking.core.entity.enums.UserRole; // Used for default role
import lk.banking.security.UserManagementService;
import lk.banking.core.exception.ValidationException;       // Import specific exceptions
import lk.banking.core.exception.ResourceConflictException; // Import specific exceptions
import lk.banking.core.exception.RoleNotFoundException;     // Import specific exceptions

import java.io.Serializable;

@Named
@RequestScoped
public class RegistrationController implements Serializable {

    private String username;
    private String password;
    private String email;
    private String phone; // Corresponds to phoneNumber in RegisterUserDto

    @Inject
    private UserManagementService userManagementService;

    /**
     * Handles the user registration attempt.
     * Collects form data, creates a RegisterUserDto, and calls the service.
     * @return JSF navigation outcome.
     */
    public String register() {
        FacesContext context = FacesContext.getCurrentInstance();

        // 1. Create the RegisterUserDto from form fields
        RegisterUserDto registerUserDto = new RegisterUserDto(
                this.username,
                this.password,
                this.email,
                this.phone, // Pass the phone number
                UserRole.CUSTOMER // Default initial role to CUSTOMER
        );

        try {
            // 2. Call the userManagementService.register with the DTO
            User newUser = userManagementService.register(registerUserDto); // Service now returns User entity

            // 3. Add success message and navigate
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Registration successful. Please log in.", null));
            return "/pages/login.xhtml?faces-redirect=true"; // Navigate to login page
        } catch (ValidationException e) {
            // Catch validation errors (e.g., weak password, invalid email format, empty fields)
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Registration failed: " + e.getMessage(), null));
            return null; // Stay on registration page
        } catch (ResourceConflictException e) {
            // Catch duplicate username or email errors
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Registration failed: " + e.getMessage(), null));
            return null; // Stay on registration page
        } catch (RoleNotFoundException e) {
            // Catch if the default CUSTOMER role somehow isn't found (unlikely if roles are pre-populated)
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_FATAL, "Registration failed: System error with roles. Please contact support.", null));
            System.err.println("Role not found during registration: " + e.getMessage()); // Log detailed error
            return null; // Stay on registration page
        } catch (Exception e) {
            // Catch any other unexpected errors
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_FATAL, "An unexpected error occurred during registration. Please try again later.", null));
            System.err.println("Registration error: " + e.getMessage()); // Log detailed error
            e.printStackTrace(); // Print stack trace for debugging
            return null; // Stay on registration page
        }
    }

    // Getters and Setters for JSF form binding
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}