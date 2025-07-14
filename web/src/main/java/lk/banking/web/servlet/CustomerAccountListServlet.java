package lk.banking.web.servlet;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.AccountDto; // To pass DTOs to JSP
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.Customer; // To get customer details for display
import lk.banking.core.entity.enums.UserRole;
import lk.banking.core.exception.AccountNotFoundException; // From AccountService
import lk.banking.core.exception.BankingException;
import lk.banking.core.exception.CustomerNotFoundException; // From CustomerService
import lk.banking.core.mapper.AccountMapper; // For mapping entities to DTOs
import lk.banking.services.AccountService;
import lk.banking.services.CustomerService;
import lk.banking.web.util.FlashMessageUtil;
import lk.banking.web.util.ServletUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Servlet for displaying all bank accounts associated with a specific customer.
 * Accessible by ADMIN and EMPLOYEE roles.
 * Accessed via /customers/accounts/{customerId}
 */
@WebServlet("/customers/accounts/*")
public class CustomerAccountListServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(CustomerAccountListServlet.class.getName());

    @EJB
    private CustomerService customerService; // To get customer details
    @EJB
    private AccountService accountService; // To get accounts by customer

    /**
     * Handles GET requests to display the list of accounts for a specific customer.
     * Customer ID is extracted from the URL path.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("CustomerAccountListServlet: Handling GET request.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        // Security check: Only ADMIN or EMPLOYEE can access this page
        if (loggedInUser == null || (!loggedInUser.hasRole(UserRole.ADMIN) && !loggedInUser.hasRole(UserRole.EMPLOYEE))) {
            LOGGER.warning("CustomerAccountListServlet: Unauthorized access attempt by user: " + (loggedInUser != null ? loggedInUser.getUsername() : "N/A"));
            FlashMessageUtil.putErrorMessage(request.getSession(), "Access denied. You do not have permission to view customer accounts.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        FlashMessageUtil.retrieveAndClearMessages(request);

        // Extract customerId from the URL path (e.g., /customers/accounts/123 -> 123)
        String pathInfo = request.getPathInfo();
        Long customerId = null;
        if (pathInfo != null && pathInfo.length() > 1) {
            try {
                customerId = Long.parseLong(pathInfo.substring(1)); // Remove leading '/'
            } catch (NumberFormatException e) {
                LOGGER.warning("CustomerAccountListServlet: Invalid customer ID format in URL: " + pathInfo);
                FlashMessageUtil.putErrorMessage(request.getSession(), "Invalid customer ID provided.");
                response.sendRedirect(request.getContextPath() + "/customers/manage"); // Redirect back to customer list
                return;
            }
        }

        if (customerId == null) {
            LOGGER.warning("CustomerAccountListServlet: No customer ID provided in URL.");
            FlashMessageUtil.putErrorMessage(request.getSession(), "Customer ID is missing for account list.");
            response.sendRedirect(request.getContextPath() + "/customers/manage");
            return;
        }

        try {
            // Fetch the customer details for display
            Customer customer = customerService.getCustomerById(customerId); // Throws CustomerNotFoundException

            // Fetch accounts for this customer
            List<Account> entityAccounts = accountService.getAccountsByCustomer(customerId);

            // Convert Account entities to AccountDto for presentation
            List<AccountDto> accountDtos = entityAccounts.stream()
                    .map(AccountMapper::toDto)
                    .collect(Collectors.toList());

            request.setAttribute("customer", customer); // Pass customer entity for name/details
            request.setAttribute("accounts", accountDtos); // Pass DTOs
            LOGGER.info("CustomerAccountListServlet: Loaded " + accountDtos.size() + " accounts for customer " + customer.getName() + " (ID: " + customerId + ").");

            request.getRequestDispatcher("/WEB-INF/jsp/customer-accounts.jsp").forward(request, response);

        } catch (EJBException e) {
            String displayErrorMessage = ServletUtil.getRootErrorMessage(e, "Error fetching customer account data. Please try again later.", LOGGER);
            FlashMessageUtil.putErrorMessage(request.getSession(), displayErrorMessage);
            LOGGER.log(java.util.logging.Level.SEVERE, "CustomerAccountListServlet: EJBException during account fetch for customer ID " + customerId, e);
            response.sendRedirect(request.getContextPath() + "/customers/manage");
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "CustomerAccountListServlet: An unhandled general error occurred during account fetch for customer ID " + customerId, e);
            FlashMessageUtil.putErrorMessage(request.getSession(), "An unexpected error occurred. Please try again later.");
            response.sendRedirect(request.getContextPath() + "/customers/manage");
        }
    }
}