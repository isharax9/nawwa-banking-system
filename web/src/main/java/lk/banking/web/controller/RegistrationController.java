package lk.banking.web.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lk.banking.core.entity.enums.UserRole;
import lk.banking.security.UserManagementService;

import java.io.Serializable;

@Named
@RequestScoped
public class RegistrationController implements Serializable {

    private String username;
    private String password;
    private String email;
    private String phone;

    @Inject
    private UserManagementService userManagementService;

    public String register() {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            userManagementService.register(username, password, UserRole.CUSTOMER);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Registration successful. Please log in.", null));
            return "/pages/login.xhtml?faces-redirect=true";
        } catch (Exception e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Registration failed: " + e.getMessage(), null));
            return null;
        }
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
}