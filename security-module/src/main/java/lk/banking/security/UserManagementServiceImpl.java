package lk.banking.security;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lk.banking.core.entity.Role;
import lk.banking.core.entity.User;
import lk.banking.core.entity.enums.UserRole;

import java.util.HashSet;
import java.util.List;

@Stateless
public class UserManagementServiceImpl implements UserManagementService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    public User register(String username, String password, UserRole role) {
        if (getUserByUsername(username) != null) throw new RuntimeException("Username already exists");
        Role dbRole = em.createQuery(
                        "SELECT r FROM Role r WHERE r.name = :name", Role.class)
                .setParameter("name", role)
                .getResultStream().findFirst().orElse(null);
        if (dbRole == null) {
            dbRole = new Role(role);
            em.persist(dbRole);
        }
        User user = new User(username, PasswordService.hashPassword(password), new HashSet<>());
        user.getRoles().add(dbRole);
        em.persist(user);
        return user;
    }

    @Override
    public User getUserById(Long id) {
        return em.find(User.class, id);
    }

    @Override
    public User getUserByUsername(String username) {
        List<User> users = em.createQuery(
                        "SELECT u FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", username)
                .getResultList();
        return users.isEmpty() ? null : users.get(0);
    }

    @Override
    public List<User> getAllUsers() {
        return em.createQuery("SELECT u FROM User u", User.class).getResultList();
    }

    @Override
    public boolean assignRole(Long userId, UserRole role) {
        User user = em.find(User.class, userId);
        if (user == null) return false;
        Role dbRole = em.createQuery(
                        "SELECT r FROM Role r WHERE r.name = :name", Role.class)
                .setParameter("name", role)
                .getResultStream().findFirst().orElse(null);
        if (dbRole == null) {
            dbRole = new Role(role);
            em.persist(dbRole);
        }
        user.getRoles().add(dbRole);
        em.merge(user);
        return true;
    }

    @Override
    public boolean removeUser(Long userId) {
        User user = em.find(User.class, userId);
        if (user == null) return false;
        em.remove(user);
        return true;
    }
}