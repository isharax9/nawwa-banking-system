package lk.banking.services;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject; // Using Inject for potential future use or if you have a CDI setup
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import lk.banking.core.dto.AccountDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Customer;
import lk.banking.core.entity.User; // Import User entity
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.CustomerNotFoundException; // Import CustomerNotFoundException
import lk.banking.core.exception.DuplicateAccountException; // Import if you want to handle collisions
import lk.banking.core.exception.UserNotFoundException; // Assuming you'll add this exception in core.exception
import lk.banking.core.util.AccountNumberGenerator;
import lk.banking.services.interceptor.AuditInterceptor;
import lk.banking.services.interceptor.PerformanceMonitorInterceptor;
import lk.banking.services.interceptor.SecurityInterceptor;

import java.math.BigDecimal;
import java.util.List;

@Stateless
@Interceptors({AuditInterceptor.class, PerformanceMonitorInterceptor.class, SecurityInterceptor.class})
public class AccountServiceImpl implements AccountService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    // It's a good practice to inject services if they are stateless EJBs
    // For simplicity, we'll keep direct EM usage for now, but for complex
    // scenarios, you might inject CustomerService etc.

    @Override
    public Account createAccount(AccountDto accountDto) {
        Customer customer = em.find(Customer.class, accountDto.getCustomerId());
        if (customer == null) {
            // More semantically correct exception here
            throw new CustomerNotFoundException("Customer with ID " + accountDto.getCustomerId() + " not found.");
        }

        String newAccountNumber;
        boolean unique = false;
        int attempts = 0;
        final int MAX_ATTEMPTS = 5; // Prevent infinite loops in case of extreme collision

        while (!unique && attempts < MAX_ATTEMPTS) {
            newAccountNumber = AccountNumberGenerator.generateAccountNumber();
            try {
                // Check if account number already exists
                em.createQuery("SELECT a FROM Account a WHERE a.accountNumber = :num", Account.class)
                        .setParameter("num", newAccountNumber)
                        .getSingleResult(); // Will throw NoResultException if not found, or NonUniqueResultException if more than one (shouldn't happen with unique constraint)
                // If we reach here, it means an account with this number exists
                attempts++;
            } catch (NoResultException e) {
                // Account number is unique, proceed
                unique = true;
                accountDto.setAccountNumber(newAccountNumber); // Set the generated account number in the DTO
            } catch (jakarta.persistence.NonUniqueResultException e) {
                // This indicates a severe data integrity issue if accountNumber is unique
                // Log this or throw a more critical exception if necessary
                System.err.println("Database integrity error: Duplicate account number despite unique constraint: " + newAccountNumber);
                throw new DuplicateAccountException("Failed to generate unique account number due to existing duplicates.");
            }
        }

        if (!unique) {
            throw new DuplicateAccountException("Failed to generate a unique account number after " + MAX_ATTEMPTS + " attempts.");
        }

        Account account = new Account(
                accountDto.getAccountNumber(), // Use the newly generated and validated unique account number
                accountDto.getType(),
                accountDto.getBalance() != null ? accountDto.getBalance() : BigDecimal.ZERO,
                customer
        );
        em.persist(account);
        return account;
    }

    @Override
    public Account getAccountById(Long id) {
        Account account = em.find(Account.class, id);
        if (account == null) {
            throw new AccountNotFoundException("Account with ID " + id + " not found.");
        }
        return account;
    }

    @Override
    public Account getAccountByNumber(String accountNumber) {
        try {
            return em.createQuery(
                            "SELECT a FROM Account a WHERE a.accountNumber = :num", Account.class)
                    .setParameter("num", accountNumber)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new AccountNotFoundException("Account with number " + accountNumber + " not found.");
        }
    }

    @Override
    public List<Account> getAccountsByCustomer(Long customerId) {
        return em.createQuery(
                        "SELECT a FROM Account a WHERE a.customer.id = :cid", Account.class)
                .setParameter("cid", customerId)
                .getResultList();
    }

    @Override
    public Account updateAccount(AccountDto accountDto) {
        Account account = em.find(Account.class, accountDto.getId());
        if (account == null) {
            throw new AccountNotFoundException("Account with ID " + accountDto.getId() + " not found for update.");
        }
        // Account number is typically immutable, so we don't update it from DTO
        // Customer relationship is also typically immutable via this method
        account.setType(accountDto.getType());
        // Only update balance if provided and non-null, otherwise keep current balance
        if (accountDto.getBalance() != null) {
            account.setBalance(accountDto.getBalance());
        }
        em.merge(account); // No need to merge if the entity is managed. But harmless.
        return account;
    }

    @Override
    public void deleteAccount(Long id) {
        Account account = em.find(Account.class, id);
        if (account == null) {
            throw new AccountNotFoundException("Account with ID " + id + " not found for deletion.");
        }
        em.remove(account);
    }

    @Override
    public List<Account> findAccountsByUserId(Long userId) {
        User user = em.find(User.class, userId);
        if (user == null) {
            // Assuming you'll add UserNotFoundException to core.exception package
            throw new UserNotFoundException("User with ID " + userId + " not found.");
        }

        // IMPORTANT: The User entity (as you shared it) does not have a direct
        // relationship (e.g., @OneToOne Customer customer) to the Customer entity.
        // To properly implement this, you should add a 'Customer customer' field
        // and a @OneToOne/@JoinColumn to the User entity.

        // As a workaround, we'll try to find a customer by email, assuming
        // the user's email address is also the customer's email.
        // This is a common pattern when direct foreign keys are not used,
        // but it's less robust than a direct JPA relationship.
        try {
            Customer customer = em.createQuery(
                            "SELECT c FROM Customer c WHERE c.email = :email", Customer.class)
                    .setParameter("email", user.getEmail())
                    .getSingleResult();
            return getAccountsByCustomer(customer.getId());
        } catch (NoResultException e) {
            // No customer found linked to this user's email
            System.out.println("No customer found for user email: " + user.getEmail());
            return List.of();
        }
    }

    @Override
    public List<Account> getAllAccounts() {
        return em.createQuery("SELECT a FROM Account a WHERE a.isActive = TRUE", Account.class).getResultList();
    }
}