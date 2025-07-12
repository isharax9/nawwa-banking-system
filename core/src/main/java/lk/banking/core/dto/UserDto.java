package lk.banking.core.dto;

import java.util.Set;
import lk.banking.core.entity.enums.UserRole;

/**
 * Data Transfer Object for User entity (excludes password).
 */
public class UserDto {
    private Long id;
    private String username;
    private Set<UserRole> roles;
    private String email; // Optional: If you want to include email in the DTO

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserDto() {}

    public UserDto(Long id, String username, Set<UserRole> roles) {
        this.id = id;
        this.username = username;
        this.roles = roles;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Set<UserRole> getRoles() { return roles; }
    public void setRoles(Set<UserRole> roles) { this.roles = roles; }
}