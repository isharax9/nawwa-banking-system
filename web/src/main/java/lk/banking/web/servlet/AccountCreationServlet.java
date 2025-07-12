package lk.banking.web.servlet;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.AccountDto;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.entity.Customer;
import lk.banking.core.entity.enums.AccountType;
import lk.banking.core.exception.CustomerNotFoundException;
import lk.banking.core.exception.DuplicateAccountException;
import lk.banking.core.exception.ValidationException;
import lk.banking.services.AccountService;
import lk.banking.services.CustomerService;
import lk.banking.web.util.FlashMessageUtil; // Import FlashMessageUtil

import java.io.IOException;
import java.math.BigDecimal;
import java.util.logging.Logger;

@WebServlet("/account-create")
public class AccountCreationServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AccountCreationServlet.class.getName());

    @Inject
    private AccountService accountService;

    @Inject
    private CustomerService customerService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("AccountCreationServlet: Handling GET request to display account creation form.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null) {
            LOGGER.warning("AccountCreationServlet: No logged-in user found. Redirecting to login.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        request.setAttribute("accountTypes", AccountType.values());
        request.getRequestDispatcher("/WEB-INF/jsp/account-create.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        LOGGER.info("AccountCreationServlet: Processing POST request for account creation.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null) {
            LOGGER.warning("AccountCreationServlet: No logged-in user found during POST. Redirecting to login.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String accountTypeStr = request.getParameter("accountType");
        String initialBalanceStr = request.getParameter("initialBalance");

        AccountType type = null;
        BigDecimal initialBalance = BigDecimal.ZERO;

        String errorMessage = null;
        if (accountTypeStr == null || accountTypeStr.trim().isEmpty()) {
            errorMessage = "Account type is required.";
        } else {
            try {
                type = AccountType.valueOf(accountTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                errorMessage = "Invalid account type selected.";
            }
        }

        if (errorMessage == null && (initialBalanceStr == null || initialBalanceStr.trim().isEmpty())) {
            errorMessage = "Initial balance is required.";
        } else if (errorMessage == null) {
            try {
                initialBalance = new BigDecimal(initialBalanceStr);
                if (initialBalance.compareTo(BigDecimal.ZERO) <= 0) {
                    errorMessage = "Initial balance must be a positive amount.";
                }
            } catch (NumberFormatException e) {
                errorMessage = "Invalid initial balance format.";
            }
        }

        if (errorMessage != null) {
            request.setAttribute("errorMessage", errorMessage);
            LOGGER.warning("AccountCreationServlet: Validation error: " + errorMessage);
            doGet(request, response);
            return;
        }

        try {
            String userEmail = loggedInUser.getEmail();
            if (userEmail == null || userEmail.trim().isEmpty()) {
                LOGGER.severe("AccountCreationServlet: LoggedInUser DTO for CUSTOMER role has null/empty email. Cannot create account.");
                throw new CustomerNotFoundException("Your user profile is incomplete. Cannot create account without a linked customer email.");
            }

            Customer customer = customerService.getCustomerByEmail(userEmail);
            if (customer == null) {
                LOGGER.severe("AccountCreationServlet: Customer not found in DB for user email: " + userEmail);
                throw new CustomerNotFoundException("Customer profile not found for your user account. Cannot create account.");
            }

            AccountDto accountDto = new AccountDto();
            accountDto.setType(type);
            accountDto.setBalance(initialBalance);
            accountDto.setCustomerId(customer.getId());

            accountService.createAccount(accountDto);

            LOGGER.info("AccountCreationServlet: Account created successfully for customer ID " + customer.getId() + ".");
            FlashMessageUtil.putSuccessMessage(request.getSession(), "New account created successfully!"); // Use FlashMessageUtil
            response.sendRedirect(request.getContextPath() + "/dashboard"); // No more URL params

        } catch (CustomerNotFoundException e) {
            LOGGER.log(java.util.logging.Level.WARNING, "AccountCreationServlet: Customer not found for user: " + loggedInUser.getUsername(), e);
            request.setAttribute("errorMessage", e.getMessage());
            doGet(request, response);
        } catch (DuplicateAccountException e) {
            LOGGER.log(java.util.logging.Level.WARNING, "AccountCreationServlet: Duplicate account creation attempt for user: " + loggedInUser.getUsername(), e);
            request.setAttribute("errorMessage", "Failed to create account: " + e.getMessage() + ". Please try again.");
            doGet(request, response);
        } catch (ValidationException e) {
            LOGGER.log(java.util.logging.Level.WARNING, "AccountCreationServlet: Validation error during account creation for user: " + loggedInUser.getUsername(), e);
            request.setAttribute("errorMessage", e.getMessage());
            doGet(request, response);
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "AccountCreationServlet: An unexpected error occurred during account creation for user " + loggedInUser.getUsername() + ".", e);
            request.setAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
            doGet(request, response);
        }
    }
}