package lk.banking.security;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lk.banking.core.entity.User;

@Stateless
public class AuthenticationServiceImpl implements AuthenticationService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    public User authenticate(String username, String password) {
        User user = em.createQuery(
                        "SELECT u FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst()
                .orElse(null);
        if (user != null && PasswordService.verifyPassword(password, user.getPassword())) {
            return user;
        }
        return null;
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = em.find(User.class, userId);
        if (user == null) return false;
        if (!PasswordService.verifyPassword(oldPassword, user.getPassword()))
            return false;
        user.setPassword(PasswordService.hashPassword(newPassword));
        em.merge(user);
        return true;
    }
}