package lk.banking.services;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import lk.banking.core.dto.AccountDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Customer;
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.User;
import lk.banking.core.entity.enums.AccountType; // For changeAccountType
import lk.banking.core.entity.enums.TransactionStatus;
import lk.banking.core.entity.enums.TransactionType;
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.CustomerNotFoundException;
import lk.banking.core.exception.DuplicateAccountException;
import lk.banking.core.exception.InvalidTransactionException; // For deleteAccount validation
import lk.banking.core.exception.UserNotFoundException;
import lk.banking.core.exception.ValidationException; // For create/update/changeType validation
import lk.banking.core.util.AccountNumberGenerator;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Logger;

@Stateless
public class AccountServiceImpl implements AccountService {

    private static final Logger LOGGER = Logger.getLogger(AccountServiceImpl.class.getName());

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    private static final MathContext MATH_CONTEXT = new MathContext(10, RoundingMode.HALF_UP); // Same as timer
    private static final BigDecimal DAILY_INTEREST_RATE = BigDecimal.valueOf(0.00002); // Same as timer

    @Override
    public BigDecimal calculateAccruedInterest(Long accountId, LocalDateTime toDateTime) {
        LOGGER.info("AccountServiceImpl: Calculating accrued interest for account ID: " + accountId + " up to " + toDateTime);
        Account account = getAccountById(accountId); // Reusing existing method

        if (account.getType() != AccountType.SAVINGS) {
            throw new InvalidTransactionException("Interest can only be calculated for SAVINGS accounts.");
        }
        if (!account.getIsActive()) {
            throw new InvalidTransactionException("Cannot calculate interest for inactive account " + account.getAccountNumber() + ".");
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO; // No interest on non-positive balance
        }
        if (toDateTime == null) {
            toDateTime = LocalDateTime.now();
        }
        if (account.getLastInterestAppliedDate() == null) {
            // If interest never applied, calculate from creation date
            // Or from a predefined system start date for interest calculation if applicable
            account.setLastInterestAppliedDate(account.getCreatedAt()); // Temporarily set for calc
        }

        LocalDateTime lastApplied = account.getLastInterestAppliedDate();

        // Calculate full days passed since last application up to the toDateTime
        long days = ChronoUnit.DAYS.between(lastApplied.toLocalDate(), toDateTime.toLocalDate());

        if (days <= 0) {
            LOGGER.info("AccountServiceImpl: No full days passed since last interest application for account " + account.getAccountNumber() + ". Accrued interest: 0.");
            return BigDecimal.ZERO;
        }

        // Accrued interest calculation (simple or compounded, consistent with timer)
        BigDecimal accruedInterest = BigDecimal.ZERO;
        BigDecimal tempBalance = account.getBalance(); // Start with current balance for calculation

        for (int i = 0; i < days; i++) {
            BigDecimal dailyInterest = tempBalance.multiply(DAILY_INTEREST_RATE, MATH_CONTEXT);
            accruedInterest = accruedInterest.add(dailyInterest);
            tempBalance = tempBalance.add(dailyInterest); // Compounding daily
        }

        LOGGER.info("AccountServiceImpl: Calculated accrued interest of " + accruedInterest + " for account " + account.getAccountNumber() + " over " + days + " days.");
        return accruedInterest.setScale(2, RoundingMode.HALF_UP); // Scale to 2 decimal places for currency
    }

    @Override
    public Account applyAccruedInterest(Long accountId, BigDecimal interestAmount) {
        LOGGER.info("AccountServiceImpl: Applying interest of " + interestAmount + " to account ID: " + accountId);
        Account account = getAccountById(accountId); // Reusing existing method

        if (account.getType() != AccountType.SAVINGS) {
            throw new InvalidTransactionException("Interest can only be applied to SAVINGS accounts.");
        }
        if (!account.getIsActive()) {
            throw new InvalidTransactionException("Cannot apply interest to inactive account " + account.getAccountNumber() + ".");
        }
        if (interestAmount == null || interestAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Interest amount to apply must be positive.");
        }

        BigDecimal oldBalance = account.getBalance();
        account.setBalance(oldBalance.add(interestAmount));
        account.setLastInterestAppliedDate(LocalDateTime.now()); // Update last applied date after application

        // Create a transaction record for this application
        Transaction interestTransaction = new Transaction(
                account,
                interestAmount,
                TransactionType.DEPOSIT, // Use DEPOSIT type for interest
                TransactionStatus.COMPLETED,
                LocalDateTime.now(),
                "Customer applied accrued interest"
        );
        em.persist(interestTransaction);

        LOGGER.info("AccountServiceImpl: Applied interest " + interestAmount + " to account " + account.getAccountNumber() + ". Balance changed from " + oldBalance + " to " + account.getBalance());
        return account; // Return updated account
    }

    @Override
    public Account createAccount(AccountDto accountDto) {
        LOGGER.info("AccountServiceImpl: Creating account for customer ID: " + accountDto.getCustomerId());
        Customer customer = em.find(Customer.class, accountDto.getCustomerId());
        if (customer == null) {
            LOGGER.warning("AccountServiceImpl: Customer not found for ID: " + accountDto.getCustomerId());
            throw new CustomerNotFoundException("Customer with ID " + accountDto.getCustomerId() + " not found.");
        }

        if (accountDto.getBalance() == null || accountDto.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Initial balance must be a positive or zero amount."); // Allow zero for certain account types
        }
        if (accountDto.getType() == null) {
            throw new ValidationException("Account type is required.");
        }


        String newAccountNumber;
        boolean unique = false;
        int attempts = 0;
        final int MAX_ATTEMPTS = 5;

        while (!unique && attempts < MAX_ATTEMPTS) {
            newAccountNumber = AccountNumberGenerator.generateAccountNumber();
            try {
                em.createQuery(
                                "SELECT a FROM Account a WHERE a.accountNumber = :num", Account.class)
                        .setParameter("num", newAccountNumber)
                        .getSingleResult();
                attempts++;
            } catch (NoResultException e) {
                unique = true;
                accountDto.setAccountNumber(newAccountNumber);
            } catch (jakarta.persistence.NonUniqueResultException e) {
                LOGGER.severe("Database integrity error: Duplicate account number despite unique constraint: " + newAccountNumber);
                throw new DuplicateAccountException("Failed to generate unique account number due to existing duplicates.");
            }
        }

        if (!unique) {
            LOGGER.severe("Failed to generate a unique account number after " + MAX_ATTEMPTS + " attempts.");
            throw new DuplicateAccountException("Failed to generate a unique account number after " + MAX_ATTEMPTS + " attempts.");
        }

        Account account = new Account(
                accountDto.getAccountNumber(),
                accountDto.getType(),
                accountDto.getBalance() != null ? accountDto.getBalance() : BigDecimal.ZERO,
                customer
        );
        em.persist(account);
        LOGGER.info("AccountServiceImpl: Account " + account.getAccountNumber() + " created for customer ID " + customer.getId());
        return account;
    }

    @Override
    public Account getAccountById(Long id) {
        LOGGER.fine("AccountServiceImpl: Fetching account by ID: " + id);
        try {
            // Eagerly fetch customer to prevent LazyInitializationException in web layer
            return em.createQuery("SELECT a FROM Account a JOIN FETCH a.customer WHERE a.id = :id", Account.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            LOGGER.warning("AccountServiceImpl: Account with ID " + id + " not found.");
            throw new AccountNotFoundException("Account with ID " + id + " not found.");
        }
    }

    @Override
    public Account getAccountByNumber(String accountNumber) {
        LOGGER.fine("AccountServiceImpl: Fetching account by number: " + accountNumber);
        try {
            // Eagerly fetch customer if needed by calling context
            return em.createQuery(
                            "SELECT a FROM Account a WHERE a.accountNumber = :num", Account.class)
                    .setParameter("num", accountNumber)
                    .getSingleResult();
        } catch (NoResultException e) {
            LOGGER.warning("AccountServiceImpl: Account with number " + accountNumber + " not found.");
            throw new AccountNotFoundException("Account with number " + accountNumber + " not found.");
        }
    }

    @Override
    public List<Account> getAccountsByCustomer(Long customerId) {
        LOGGER.fine("AccountServiceImpl: Fetching accounts for customer ID: " + customerId);
        // Eagerly fetch customer for each account if needed
        return em.createQuery(
                        "SELECT a FROM Account a JOIN FETCH a.customer WHERE a.customer.id = :cid", Account.class)
                .setParameter("cid", customerId)
                .getResultList();
    }

    @Override
    public List<Account> getAllAccounts() {
        LOGGER.fine("AccountServiceImpl: Fetching all accounts.");
        // Eagerly fetch customer for each account if needed
        return em.createQuery("SELECT a FROM Account a JOIN FETCH a.customer", Account.class).getResultList();
    }

    @Override
    public Account updateAccount(AccountDto accountDto) {
        LOGGER.info("AccountServiceImpl: Updating account ID: " + accountDto.getId());
        Account account = em.find(Account.class, accountDto.getId());
        if (account == null) {
            LOGGER.warning("AccountServiceImpl: Account with ID " + accountDto.getId() + " not found for update.");
            throw new AccountNotFoundException("Account with ID " + accountDto.getId() + " not found for update.");
        }
        // Account number and Customer are typically immutable after creation
        if (accountDto.getType() != null) {
            account.setType(accountDto.getType());
        }
        if (accountDto.getBalance() != null) {
            // Be cautious: directly setting balance should usually only be for specific internal adjustments,
            // not a general API for clients. Transactions should change balance.
            account.setBalance(accountDto.getBalance());
        }
        // No explicit merge needed for managed entity
        LOGGER.info("AccountServiceImpl: Account " + account.getAccountNumber() + " updated.");
        return account;
    }

    @Override
    public boolean deactivateAccount(Long id) {
        LOGGER.info("AccountServiceImpl: Deactivating account ID: " + id);
        Account account = em.find(Account.class, id);
        if (account == null) {
            LOGGER.warning("AccountServiceImpl: Account with ID " + id + " not found for deactivation.");
            throw new AccountNotFoundException("Account with ID " + id + " not found for deactivation.");
        }
        if (!account.getIsActive()) {
            LOGGER.info("AccountServiceImpl: Account " + account.getAccountNumber() + " is already inactive.");
            return true;
        }
        account.setIsActive(false);
        LOGGER.info("AccountServiceImpl: Account " + account.getAccountNumber() + " deactivated.");
        return true;
    }

    @Override
    public boolean activateAccount(Long id) {
        LOGGER.info("AccountServiceImpl: Activating account ID: " + id);
        Account account = em.find(Account.class, id);
        if (account == null) {
            LOGGER.warning("AccountServiceImpl: Account with ID " + id + " not found for activation.");
            throw new AccountNotFoundException("Account with ID " + id + " not found for activation.");
        }
        if (account.getIsActive()) {
            LOGGER.info("AccountServiceImpl: Account " + account.getAccountNumber() + " is already active.");
            return true;
        }
        account.setIsActive(true);
        LOGGER.info("AccountServiceImpl: Account " + account.getAccountNumber() + " activated.");
        return true;
    }

    @Override
    public Account changeAccountType(Long id, AccountType newType) {
        LOGGER.info("AccountServiceImpl: Changing type for account ID " + id + " to " + newType.name());
        Account account = em.find(Account.class, id);
        if (account == null) {
            LOGGER.warning("AccountServiceImpl: Account with ID " + id + " not found for type change.");
            throw new AccountNotFoundException("Account with ID " + id + " not found for type change.");
        }
        if (newType == null) {
            throw new ValidationException("New account type cannot be null.");
        }
        if (account.getType() == newType) {
            LOGGER.info("AccountServiceImpl: Account " + account.getAccountNumber() + " already has type " + newType.name());
            return account; // No change needed
        }

        // Add business rules for conversion here if needed (e.g., cannot convert if balance is too high/low for new type)
        // Example: Cannot convert a savings account with balance > 0 to a loan account
        // if (account.getType() == AccountType.SAVINGS && newType == AccountType.LOAN && account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
        //     throw new InvalidTransactionException("Cannot convert non-zero savings account to loan account.");
        // }

        account.setType(newType);
        LOGGER.info("AccountServiceImpl: Account " + account.getAccountNumber() + " type changed to " + newType.name());
        return account;
    }

    @Override
    public void deleteAccount(Long id) {
        LOGGER.warning("AccountServiceImpl: Permanently deleting account ID: " + id + ". This action is irreversible.");
        Account account = em.find(Account.class, id);
        if (account == null) {
            LOGGER.warning("AccountServiceImpl: Account with ID " + id + " not found for deletion.");
            throw new AccountNotFoundException("Account with ID " + id + " not found for deletion.");
        }

        // IMPORTANT BUSINESS RULE: Prevent deletion if account has non-zero balance
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            LOGGER.warning("AccountServiceImpl: Attempt to delete account " + account.getAccountNumber() + " with non-zero balance.");
            throw new InvalidTransactionException("Cannot delete account with non-zero balance. Balance must be zero.");
        }

        // Additional checks: Prevent deletion if account has active scheduled transfers or recent transactions
        // Or handle cascading deletion of related entities if orphanRemoval is not sufficient or if you want to keep transactions.
        // As per entity setup, transactions and scheduled transfers should orphan remove.

        em.remove(account);
        LOGGER.info("AccountServiceImpl: Account " + account.getAccountNumber() + " permanently deleted.");
    }

    @Override
    public List<Account> findAccountsByUserId(Long id) {
        LOGGER.fine("AccountServiceImpl: Finding accounts for user ID: " + id);
        User user = em.find(User.class, id);
        if (user == null) {
            LOGGER.warning("AccountServiceImpl: User with ID " + id + " not found while finding accounts.");
            throw new UserNotFoundException("User with ID " + id + " not found.");
        }

        try {
            Customer customer = em.createQuery(
                            "SELECT c FROM Customer c WHERE c.email = :email", Customer.class)
                    .setParameter("email", user.getEmail())
                    .getSingleResult();
            // Eagerly fetch customer for accounts if needed by calling context
            return em.createQuery(
                            "SELECT a FROM Account a JOIN FETCH a.customer WHERE a.customer.id = :customerId", Account.class)
                    .setParameter("customerId", customer.getId())
                    .getResultList();
        } catch (NoResultException e) {
            LOGGER.info("AccountServiceImpl: No customer found for user email: " + user.getEmail() + ". Returning empty account list.");
            return List.of();
        }
    }
}