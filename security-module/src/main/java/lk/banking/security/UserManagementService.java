package lk.banking.security;

import jakarta.ejb.Local;
import lk.banking.core.dto.RegisterUserDto;
import lk.banking.core.entity.User;
import lk.banking.core.entity.enums.UserRole;

import java.util.List;

@Local
public interface UserManagementService {
    // Modified: Added email, name, address, phoneNumber for customer registration
    User register(String username, String password, String email, String name, String address, String phoneNumber, UserRole role);
    User getUserById(Long id);
    User getUserByUsername(String username);
    List<User> getAllUsers();
    boolean assignRole(Long userId, UserRole role);
    boolean removeUser(Long userId);

    User register(RegisterUserDto registerUserDto);

    User register(String username, String password, UserRole userRole);
}