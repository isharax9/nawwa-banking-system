package lk.banking.transaction;

import jakarta.annotation.security.RolesAllowed; // IMPORT THIS
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lk.banking.core.dto.TransferRequestDto;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.enums.TransactionStatus;
import lk.banking.core.entity.enums.TransactionType;
import lk.banking.core.exception.AccountNotFoundException;
import lk.banking.core.exception.InsufficientFundsException;
import lk.banking.core.exception.InvalidTransactionException;
import lk.banking.core.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.logging.Logger;

import static jakarta.ejb.TransactionAttributeType.REQUIRED;

@Stateless
public class FundTransferServiceImpl implements FundTransferService {

    private static final Logger LOGGER = Logger.getLogger(FundTransferServiceImpl.class.getName());

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    @RolesAllowed({"CUSTOMER", "EMPLOYEE", "ADMIN"}) // Customers can transfer, employees/admins can also initiate
    @TransactionAttribute(REQUIRED)
    public Transaction transferFunds(TransferRequestDto requestDto) {
        LOGGER.info("FundTransferService: Processing transfer from account ID: " + requestDto.getFromAccountId() + " to ID: " + requestDto.getToAccountId() + " amount: " + requestDto.getAmount());

        if (requestDto == null || requestDto.getAmount() == null) {
            throw new InvalidTransactionException("Transfer request or amount cannot be null.");
        }

        BigDecimal amount = requestDto.getAmount();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be positive.");
        }

        if (requestDto.getFromAccountId().equals(requestDto.getToAccountId())) {
            throw new InvalidTransactionException("Cannot transfer funds to the same account.");
        }

        Account fromAccount = em.find(Account.class, requestDto.getFromAccountId());
        Account toAccount = em.find(Account.class, requestDto.getToAccountId());

        if (fromAccount == null) {
            LOGGER.warning("FundTransferService: Source account with ID " + requestDto.getFromAccountId() + " not found.");
            throw new AccountNotFoundException("Source account with ID " + requestDto.getFromAccountId() + " not found.");
        }
        if (toAccount == null) {
            LOGGER.warning("FundTransferService: Destination account with ID " + requestDto.getToAccountId() + " not found.");
            throw new AccountNotFoundException("Destination account with ID " + requestDto.getToAccountId() + " not found.");
        }

        if (!fromAccount.getIsActive()) {
            LOGGER.warning("FundTransferService: Transfer denied from inactive source account: " + fromAccount.getAccountNumber());
            throw new InvalidTransactionException("Transfer denied: Source account " + fromAccount.getAccountNumber() + " is inactive.");
        }
        if (!toAccount.getIsActive()) {
            LOGGER.warning("FundTransferService: Transfer denied to inactive destination account: " + toAccount.getAccountNumber());
            throw new InvalidTransactionException("Transfer denied: Destination account " + toAccount.getAccountNumber() + " is inactive.");
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            LOGGER.warning("FundTransferService: Insufficient funds in source account " + fromAccount.getAccountNumber() + ". Balance: " + fromAccount.getBalance() + ", Attempted: " + amount);
            throw new InsufficientFundsException("Insufficient funds in source account " + fromAccount.getAccountNumber() + ".");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        Transaction debitTransaction = new Transaction(
                fromAccount,
                amount.negate(),
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                LocalDateTime.now(),
                String.format("Transfer out to account %s", toAccount.getAccountNumber())
        );
        em.persist(debitTransaction);

        Transaction creditTransaction = new Transaction(
                toAccount,
                amount,
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                LocalDateTime.now(),
                String.format("Transfer in from account %s", fromAccount.getAccountNumber())
        );
        em.persist(creditTransaction);

        LOGGER.info("FundTransferService: Transfer successful between " + fromAccount.getAccountNumber() + " and " + toAccount.getAccountNumber() + " for " + amount);
        return debitTransaction;
    }
}