package lk.banking.web.servlet;

import jakarta.ejb.EJB; // For EJB injection
import jakarta.ejb.EJBException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.dto.CustomerDto; // To pass DTOs to JSP
import lk.banking.core.entity.Customer; // For Customer entity (before mapping)
import lk.banking.core.entity.enums.UserRole; // For role checks
import lk.banking.core.exception.BankingException;
import lk.banking.core.mapper.CustomerMapper; // For mapping entities to DTOs
import lk.banking.services.CustomerService; // To get all customers
import lk.banking.web.util.FlashMessageUtil;
import lk.banking.web.util.ServletUtil;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Servlet for displaying and managing a list of all customer profiles in the system.
 * Accessible by ADMIN and EMPLOYEE roles only.
 * Provides actions like view details, edit, etc.
 */
@WebServlet("/customers/manage") // Maps to /customers/manage
public class CustomerManagementListServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(CustomerManagementListServlet.class.getName());

    @EJB
    private CustomerService customerService; // Inject CustomerService

    /**
     * Handles GET requests to display the list of customers.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("CustomerManagementListServlet: Handling GET request.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        // Security check: Only ADMIN or EMPLOYEE can access this page
        if (loggedInUser == null || (!loggedInUser.hasRole(UserRole.ADMIN) && !loggedInUser.hasRole(UserRole.EMPLOYEE))) {
            LOGGER.warning("CustomerManagementListServlet: Unauthorized access attempt by user: " + (loggedInUser != null ? loggedInUser.getUsername() : "N/A"));
            FlashMessageUtil.putErrorMessage(request.getSession(), "Access denied. You do not have permission to manage customers.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        FlashMessageUtil.retrieveAndClearMessages(request); // Retrieve flash messages on GET

        try {
            // Fetch all customers from the service layer
            List<Customer> entityCustomers = customerService.getAllCustomers();

            // Convert Customer entities to CustomerDto for presentation
            List<CustomerDto> customerDtos = entityCustomers.stream()
                    .map(CustomerMapper::toDto) // Use your CustomerMapper
                    .collect(Collectors.toList());

            request.setAttribute("customers", customerDtos); // Pass DTOs to JSP
            LOGGER.info("CustomerManagementListServlet: Loaded " + customerDtos.size() + " customers for management.");

            request.getRequestDispatcher("/WEB-INF/jsp/customers-manage.jsp").forward(request, response);

        } catch (EJBException e) {
            String displayErrorMessage = ServletUtil.getRootErrorMessage(e, "Error fetching customer data. Please try again later.", LOGGER);
            FlashMessageUtil.putErrorMessage(request.getSession(), displayErrorMessage);
            LOGGER.log(java.util.logging.Level.SEVERE, "CustomerManagementListServlet: EJBException during customer fetch for " + loggedInUser.getUsername(), e);
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "CustomerManagementListServlet: An unhandled general error occurred during customer fetch for " + loggedInUser.getUsername(), e);
            FlashMessageUtil.putErrorMessage(request.getSession(), "An unexpected error occurred. Please try again later.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
        }
    }

    /**
     * Handles POST requests for actions on customers (e.g., future activate/deactivate, link user).
     * This will be expanded later.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // This method will be implemented later for customer actions
        LOGGER.info("CustomerManagementListServlet: Handling POST request (actions not yet implemented).");
        doGet(request, response); // For now, just re-display the list
    }
}