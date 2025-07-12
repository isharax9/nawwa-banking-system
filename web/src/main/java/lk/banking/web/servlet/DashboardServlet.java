package lk.banking.web.servlet;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.LoggedInUser; // Change from UserDto to LoggedInUser
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Customer;
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.enums.UserRole;
import lk.banking.core.exception.CustomerNotFoundException;
import lk.banking.core.exception.UserNotFoundException;
import lk.banking.services.AccountService;
import lk.banking.services.CustomerService;
import lk.banking.services.TransactionServices;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors; // For UserRoles processing

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

        // Retrieve logged-in user from session
        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser"); // Cast to LoggedInUser

        if (loggedInUser == null) {
            LOGGER.warning("DashboardServlet: No logged-in user found in session. Redirecting to login.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Make LoggedInUser DTO and roles available to JSP
        request.setAttribute("loggedInUser", loggedInUser);
        // You can use loggedInUser.getRoles() directly in JSP now, but storing specific role names as a Set is convenient
        request.setAttribute("userRoles", loggedInUser.getRoles().stream().map(Enum::name).collect(Collectors.toSet()));


        try {
            if (loggedInUser.hasRole(UserRole.CUSTOMER)) { // Use hasRole method from LoggedInUser
                LOGGER.info("DashboardServlet: Loading data for CUSTOMER: " + loggedInUser.getUsername());

                Customer customer = null;
                List<Account> accounts = Collections.emptyList();
                List<Transaction> recentTransactions = Collections.emptyList();

                // Get customer using email from LoggedInUser
                String userEmail = loggedInUser.getEmail(); // Now email should be populated!
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

                            // Fetch accounts and transactions
                            accounts = accountService.getAccountsByCustomer(customer.getId());
                            request.setAttribute("accounts", accounts);
                            LOGGER.info("DashboardServlet: Found " + accounts.size() + " accounts for customer ID: " + customer.getId());

                            // Note: transactionService.getTransactionsByUser uses loggedInUser.getId()
                            recentTransactions = transactionService.getTransactionsByUser(loggedInUser.getId());
                            request.setAttribute("recentTransactions", recentTransactions);
                            LOGGER.info("DashboardServlet: Found " + recentTransactions.size() + " recent transactions for user ID: " + loggedInUser.getId());
                        }
                    } catch (CustomerNotFoundException e) {
                        LOGGER.log(java.util.logging.Level.WARNING, "DashboardServlet: CustomerNotFoundException for user " + loggedInUser.getUsername(), e);
                        request.setAttribute("errorMessage", "Your customer profile could not be found. Please ensure your account is fully set up or contact support.");
                    }
                }
            } else if (loggedInUser.hasRole(UserRole.EMPLOYEE) || loggedInUser.hasRole(UserRole.ADMIN)) {
                LOGGER.info("DashboardServlet: Employee/Admin user logged in. Dashboard content might vary. User: " + loggedInUser.getUsername());
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