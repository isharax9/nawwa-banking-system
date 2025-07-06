package lk.banking.security;

import jakarta.ejb.Local;
import lk.banking.core.entity.User;

@Local
public interface AuthenticationService {
    User authenticate(String username, String password);
    boolean changePassword(Long userId, String oldPassword, String newPassword);
}