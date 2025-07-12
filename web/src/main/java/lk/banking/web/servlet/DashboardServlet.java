package lk.banking.web.servlet;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.dto.TransactionDto; // Import TransactionDto
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Customer;
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.enums.UserRole;
import lk.banking.core.exception.CustomerNotFoundException;
import lk.banking.core.exception.UserNotFoundException;
import lk.banking.core.mapper.TransactionMapper; // Import TransactionMapper
import lk.banking.services.AccountService;
import lk.banking.services.CustomerService;
import lk.banking.services.TransactionServices;
import lk.banking.web.util.FlashMessageUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DashboardServlet.class.getName());

    @Inject
    private AccountService accountService;

    @Inject
    private CustomerService customerService;

    @Inject
    private TransactionServices transactionService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("DashboardServlet: Handling GET request.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");

        if (loggedInUser == null) {
            LOGGER.warning("DashboardServlet: No logged-in user found in session. Redirecting to login.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        request.setAttribute("loggedInUser", loggedInUser);
        request.setAttribute("userRoles", loggedInUser.getRoles().stream().map(Enum::name).collect(Collectors.toSet()));

        FlashMessageUtil.retrieveAndClearMessages(request);

        try {
            if (loggedInUser.hasRole(UserRole.CUSTOMER)) {
                LOGGER.info("DashboardServlet: Loading data for CUSTOMER: " + loggedInUser.getUsername());

                Customer customer = null;
                List<Account> accounts = Collections.emptyList();
                List<TransactionDto> recentTransactions = Collections.emptyList(); // CHANGE: Declare as List<TransactionDto>

                String userEmail = loggedInUser.getEmail();
                if (userEmail == null || userEmail.trim().isEmpty()) {
                    LOGGER.severe("DashboardServlet: LoggedInUser DTO for CUSTOMER role has null/empty email. This indicates a data or mapping issue.");
                    request.setAttribute("errorMessage", "Your user profile is incomplete. Cannot load customer data.");
                } else {
                    try {
                        customer = customerService.getCustomerByEmail(userEmail);
                        if (customer == null) {
                            LOGGER.warning("DashboardServlet: Customer profile not found in DB for user's email: " + userEmail);
                            request.setAttribute("errorMessage", "Your customer profile could not be loaded. Please ensure your account is fully set up or contact support.");
                        } else {
                            request.setAttribute("customer", customer);

                            accounts = accountService.getAccountsByCustomer(customer.getId());
                            request.setAttribute("accounts", accounts);
                            LOGGER.info("DashboardServlet: Found " + accounts.size() + " accounts for customer ID: " + customer.getId());

                            // CHANGE: Convert Transaction entities to TransactionDto
                            List<Transaction> entityTransactions = transactionService.getTransactionsByUser(loggedInUser.getId());
                            recentTransactions = entityTransactions.stream()
                                    .map(TransactionMapper::toDto) // Use your TransactionMapper
                                    .collect(Collectors.toList());
                            request.setAttribute("recentTransactions", recentTransactions);
                            LOGGER.info("DashboardServlet: Found " + recentTransactions.size() + " recent transactions for user ID: " + loggedInUser.getId() + " (as DTOs).");
                        }
                    } catch (CustomerNotFoundException e) {
                        LOGGER.log(java.util.logging.Level.WARNING, "DashboardServlet: CustomerNotFoundException for user " + loggedInUser.getUsername() + ".", e);
                        request.setAttribute("errorMessage", "Your customer profile could not be found. Please ensure your account is fully set up or contact support.");
                    }
                }
            } else if (loggedInUser.hasRole(UserRole.EMPLOYEE) || loggedInUser.hasRole(UserRole.ADMIN)) {
                LOGGER.info("DashboardServlet: Loading data for ADMIN/EMPLOYEE: " + loggedInUser.getUsername());
                request.setAttribute("message", "Welcome, " + loggedInUser.getUsername() + " (" + loggedInUser.getRoles().stream().findFirst().map(Enum::name).orElse("Unknown") + " Role).");
            }

            request.getRequestDispatcher("/WEB-INF/jsp/dashboard.jsp").forward(request, response);

        } catch (UserNotFoundException e) {
            LOGGER.log(java.util.logging.Level.WARNING, "DashboardServlet: User details not found for " + loggedInUser.getUsername() + " during dashboard load.", e);
            request.setAttribute("errorMessage", "Could not load user details. Please try again.");
            request.getRequestDispatcher("/WEB-INF/jsp/dashboard.jsp").forward(request, response);
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "DashboardServlet: An unexpected error occurred while loading dashboard for user " + loggedInUser.getUsername(), e);
            request.setAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
            request.getRequestDispatcher("/WEB-INF/jsp/dashboard.jsp").forward(request, response);
        }
    }
}