package lk.banking.core.dto;

import java.io.Serializable;
import java.time.LocalDateTime; // For createdAt, updatedAt
import java.util.Set;
import lk.banking.core.entity.enums.UserRole;

/**
 * Data Transfer Object for User entity (excludes password, includes useful admin/display fields).
 */
public class UserDto implements Serializable {
    private Long id;
    private String username;
    private String email; // NEW: Added email
    private String phone; // NEW: Added phone
    private Boolean isActive; // NEW: Added isActive status
    private LocalDateTime createdAt; // NEW: Added creation timestamp
    private LocalDateTime updatedAt; // NEW: Added update timestamp
    private Set<UserRole> roles;

    public UserDto() {}

    // Updated constructor
    public UserDto(Long id, String username, String email, String phone, Boolean isActive, LocalDateTime createdAt, LocalDateTime updatedAt, Set<UserRole> roles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.roles = roles;
    }

    // Getters and setters for new fields
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Existing getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Set<UserRole> getRoles() { return roles; }
    public void setRoles(Set<UserRole> roles) { this.roles = roles; }

    // Helper method for formatted timestamp (like TransactionDto)
    public String getFormattedCreatedAt() {
        if (this.createdAt == null) {
            return "";
        }
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return this.createdAt.format(formatter);
    }
}