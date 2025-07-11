package lk.banking.core.dto;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import lk.banking.core.entity.enums.UserRole;

/**
 * A simplified DTO representing the currently logged-in user.
 * This object is safe to store in the HTTP session as it does not contain
 * sensitive information like the password hash, nor is it a JPA managed entity.
 */
public class LoggedInUser implements Serializable {
    private static final long serialVersionUID = 1L; // Recommended for Serializable classes

    private Long id;
    private String username;
    private String email; // Added email as it's useful for customer lookup
    private Set<UserRole> roles;
    private Long customerId; // Optional: If a user maps directly to a customer

    public LoggedInUser() {
        this.roles = new HashSet<>(); // Initialize to avoid null pointer
    }

    public LoggedInUser(Long id, String username, String email, Set<UserRole> roles, Long customerId) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = (roles != null) ? new HashSet<>(roles) : new HashSet<>();
        this.customerId = customerId;
    }

    // Getters (no setters to ensure immutability once created and stored in session)
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    // Return a copy of the set to prevent external modification
    public Set<UserRole> getRoles() {
        return Collections.unmodifiableSet(roles); // Make roles unmodifiable
    }

    public Long getCustomerId() {
        return customerId;
    }

    /**
     * Checks if the logged-in user has a specific role.
     * @param role The UserRole to check for.
     * @return true if the user has the role, false otherwise.
     */
    public boolean hasRole(UserRole role) {
        return roles != null && roles.contains(role);
    }

    @Override
    public String toString() {
        return "LoggedInUser{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", roles=" + roles +
                ", customerId=" + customerId +
                '}';
    }
}