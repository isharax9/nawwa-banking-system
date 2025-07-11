package lk.banking.web.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lk.banking.core.entity.enums.UserRole;
import lk.banking.security.UserManagementService;

import java.io.Serializable;

@Named
@RequestScoped
public class UserController implements Serializable {

    private String username;
    private String password;
    private String email;
    private String phone;
    private UserRole role;

    @Inject
    private UserManagementService userManagementService;

    public String createUser() {
        try {
            userManagementService.register(username, password, role);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "User created successfully!", null));
            return "/pages/admin/users.xhtml?faces-redirect=true";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error creating user: " + e.getMessage(), null));
            return null;
        }
    }
    public UserRole[] getAvailableRoles() {
        return UserRole.values();
    }

    // Getters and Setters
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