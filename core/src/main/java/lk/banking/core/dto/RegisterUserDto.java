package lk.banking.core.dto;

import lk.banking.core.entity.enums.UserRole; // Assuming you might want to specify initial role

/**
 * Data Transfer Object for user registration requests.
 * Contains fields necessary for creating a new user account.
 */
public class RegisterUserDto {
    private String username;
    private String password; // Raw password from client (will be hashed)
    private String email;
    private String phoneNumber; // Optional
    private UserRole initialRole; // Optional, can be set by service if not provided

    public RegisterUserDto() {}

    public RegisterUserDto(String username, String password, String email, String phoneNumber, UserRole initialRole) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.initialRole = initialRole;
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public UserRole getInitialRole() { return initialRole; }
    public void setInitialRole(UserRole initialRole) { this.initialRole = initialRole; }

    @Override
    public String toString() {
        return "RegisterUserDto{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", initialRole=" + initialRole +
                '}';
    }
}