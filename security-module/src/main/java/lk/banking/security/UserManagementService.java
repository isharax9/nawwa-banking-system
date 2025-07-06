package lk.banking.security;

import jakarta.ejb.Local;
import lk.banking.core.entity.User;
import lk.banking.core.entity.enums.UserRole;

import java.util.List;

@Local
public interface UserManagementService {
    User register(String username, String password, UserRole role);
    User getUserById(Long id);
    User getUserByUsername(String username);
    List<User> getAllUsers();
    boolean assignRole(Long userId, UserRole role);
    boolean removeUser(Long userId);
}