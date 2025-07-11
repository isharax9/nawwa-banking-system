package lk.banking.web.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lk.banking.core.dto.RegisterUserDto; // Import the RegisterUserDto
import lk.banking.core.entity.User; // If you need to store the resulting user, though not directly used here
import lk.banking.core.entity.enums.UserRole; // Used for available roles
import lk.banking.security.UserManagementService;
import lk.banking.core.exception.ValidationException;       // Import specific exceptions
import lk.banking.core.exception.ResourceConflictException; // Import specific exceptions
import lk.banking.core.exception.RoleNotFoundException;     // Import specific exceptions
import lk.banking.web.util.WebUtils; // For message helpers

import java.io.Serializable;

@Named
@RequestScoped
public class UserController implements Serializable { // Assuming this is for Admin to create users

    // --- Form properties for user creation ---
    private String username;
    private String password;
    private String email;
    private String phone;
    private UserRole role; // To select initial role for the new user

    @Inject
    private UserManagementService userManagementService;

    /**
     * Action method to create a new user from the admin interface.
     * Constructs a RegisterUserDto and calls the UserManagementService.
     *
     * @return JSF navigation outcome.
     */
    public String createUser() {
        FacesContext context = FacesContext.getCurrentInstance();

        // 1. Create the RegisterUserDto from form fields
        RegisterUserDto registerUserDto = new RegisterUserDto(
                this.username,
                this.password,
                this.email,
                this.phone,
                this.role // Pass the selected role
        );

        try {
            // 2. Call the userManagementService.register with the DTO
            User newUser = userManagementService.register(registerUserDto); // Service now returns User entity

            // 3. Add success message and navigate
            WebUtils.addInfoMessage("User '" + newUser.getUsername() + "' created successfully!");
            // Clear form fields after successful creation
            clearForm();
            return "/pages/admin/users.xhtml?faces-redirect=true"; // Navigate back to user list (assuming this exists)
        } catch (ValidationException e) {
            WebUtils.addErrorMessage("User creation failed: " + e.getMessage());
            return null; // Stay on the current page
        } catch (ResourceConflictException e) {
            WebUtils.addWarnMessage("User creation failed: " + e.getMessage());
            return null; // Stay on the current page
        } catch (RoleNotFoundException e) {
            WebUtils.addFatalMessage("User creation failed: " + e.getMessage() + ". Please ensure roles are configured.");
            System.err.println("Role not found during user creation: " + e.getMessage());
            return null; // Stay on the current page
        } catch (Exception e) {
            // Catch any other unexpected errors
            WebUtils.addFatalMessage("An unexpected error occurred during user creation. Please try again later.");
            System.err.println("User creation error: " + e.getMessage());
            e.printStackTrace();
            return null; // Stay on the current page
        }
    }

    /**
     * Helper method to get all available UserRole enum values for UI dropdown.
     * @return Array of UserRole enums.
     */
    public UserRole[] getAvailableRoles() {
        return UserRole.values();
    }

    /**
     * Clears the form fields after successful submission or for new input.
     */
    private void clearForm() {
        this.username = null;
        this.password = null;
        this.email = null;
        this.phone = null;
        this.role = null;
    }

    // --- Getters and Setters for JSF form binding ---
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
}