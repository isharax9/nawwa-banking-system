package lk.banking.web.servlet;

// import jakarta.ejb.EJB; // Temporarily remove @EJB import
import jakarta.ejb.EJBException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.CustomerDto;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.entity.Customer;
import lk.banking.core.entity.enums.UserRole;
import lk.banking.core.exception.BankingException;
import lk.banking.core.exception.CustomerNotFoundException;
import lk.banking.core.exception.ValidationException;
import lk.banking.core.util.ValidationUtils;
import lk.banking.services.CustomerService; // Import the interface
import lk.banking.web.util.FlashMessageUtil;
import lk.banking.web.util.ServletUtil;

import javax.naming.InitialContext; // Import
import javax.naming.NamingException; // Import
import java.io.IOException;
import java.util.logging.Logger;

@WebServlet("/customers/edit/*")
public class CustomerEditServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(CustomerEditServlet.class.getName());

    // REMOVED @EJB for CustomerService to use JNDI lookup in init()
    private CustomerService customerService;

    // Add init method for JNDI lookup
    @Override
    public void init() throws ServletException {
        super.init();
        try {
            // CONFIRM THIS JNDI NAME FROM YOUR GLASSFISH LOGS
            // Look for "EJB [CustomerServiceImpl] successfully bound to JNDI name [...]"
            String customerServiceJndiName = "java:global/banking-system-ear/banking-services/CustomerServiceImpl!lk.banking.services.CustomerService";
            LOGGER.info("CustomerEditServlet: Attempting JNDI lookup for CustomerService at: " + customerServiceJndiName);
            customerService = (CustomerService) new InitialContext().lookup(customerServiceJndiName);
            LOGGER.info("CustomerEditServlet: JNDI lookup successful for CustomerService.");
        } catch (NamingException e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "CustomerEditServlet: Failed to lookup CustomerService via JNDI.", e);
            throw new ServletException("Failed to initialize CustomerEditServlet: CustomerService not found.", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("CustomerEditServlet: Handling GET request.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null || (!loggedInUser.hasRole(UserRole.ADMIN) && !loggedInUser.hasRole(UserRole.EMPLOYEE))) {
            LOGGER.warning("CustomerEditServlet: Unauthorized access attempt by user: " + (loggedInUser != null ? loggedInUser.getUsername() : "N/A"));
            FlashMessageUtil.putErrorMessage(request.getSession(), "Access denied. You do not have permission to edit customer profiles.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        FlashMessageUtil.retrieveAndClearMessages(request);

        String pathInfo = request.getPathInfo();
        Long customerId = null;
        if (pathInfo != null && pathInfo.length() > 1) {
            try {
                customerId = Long.parseLong(pathInfo.substring(1));
            } catch (NumberFormatException e) {
                LOGGER.warning("CustomerEditServlet: Invalid customer ID format in URL: " + pathInfo);
                FlashMessageUtil.putErrorMessage(request.getSession(), "Invalid customer ID provided for edit.");
                response.sendRedirect(request.getContextPath() + "/customers/manage");
                return;
            }
        }

        if (customerId == null) {
            LOGGER.warning("CustomerEditServlet: No customer ID provided in URL for edit.");
            FlashMessageUtil.putErrorMessage(request.getSession(), "Customer ID is missing for profile edit.");
            response.sendRedirect(request.getContextPath() + "/customers/manage");
            return;
        }

        try {
            Customer customer = customerService.getCustomerById(customerId);
            request.setAttribute("customer", customer);
            LOGGER.info("CustomerEditServlet: Loaded customer " + customer.getName() + " for editing.");
            request.getRequestDispatcher("/WEB-INF/jsp/customer-edit.jsp").forward(request, response);

        } catch (EJBException e) {
            String displayErrorMessage = ServletUtil.getRootErrorMessage(e, "Error loading customer profile for edit. Please try again later.", LOGGER);
            FlashMessageUtil.putErrorMessage(request.getSession(), displayErrorMessage);
            LOGGER.log(java.util.logging.Level.SEVERE, "CustomerEditServlet: EJBException during customer fetch for ID " + customerId, e);
            response.sendRedirect(request.getContextPath() + "/customers/manage");
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "CustomerEditServlet: An unhandled general error occurred during customer fetch for ID " + customerId, e);
            FlashMessageUtil.putErrorMessage(request.getSession(), "An unexpected error occurred. Please try again later.");
            response.sendRedirect(request.getContextPath() + "/customers/manage");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("CustomerEditServlet: Processing POST request for customer profile update.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null || (!loggedInUser.hasRole(UserRole.ADMIN) && !loggedInUser.hasRole(UserRole.EMPLOYEE))) {
            LOGGER.warning("CustomerEditServlet: Unauthorized POST access attempt by user: " + (loggedInUser != null ? loggedInUser.getUsername() : "N/A"));
            FlashMessageUtil.putErrorMessage(request.getSession(), "Access denied. You do not have permission to edit customer profiles.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        String customerIdStr = request.getParameter("customerId");
        String name = request.getParameter("name");
        String address = request.getParameter("address");
        String phoneNumber = request.getParameter("phoneNumber");

        Long customerId = null;
        String errorMessage = null;

        if (customerIdStr == null || customerIdStr.trim().isEmpty()) {
            errorMessage = "Customer ID is missing for update.";
        } else {
            try { customerId = Long.parseLong(customerIdStr); }
            catch (NumberFormatException e) { errorMessage = "Invalid Customer ID format."; }
        }

        if (errorMessage == null && (name == null || name.trim().isEmpty() ||
                address == null || address.trim().isEmpty() ||
                phoneNumber == null || phoneNumber.trim().isEmpty())) {
            errorMessage = "All fields (Name, Address, Phone Number) are required.";
        } else if (errorMessage == null && !ValidationUtils.isValidPhoneNumber(phoneNumber)) {
            errorMessage = "Invalid phone number format. Must be 10-15 digits.";
        }

        if (errorMessage != null) {
            request.setAttribute("errorMessage", errorMessage);
            LOGGER.warning("CustomerEditServlet: Validation error: " + errorMessage);
            try {
                // Use the JNDI-looked-up customerService
                request.setAttribute("customer", customerService.getCustomerById(customerId));
            } catch (Exception ignore) { LOGGER.severe("Customer not found while trying to re-populate form after validation error: " + customerId); }
            request.getRequestDispatcher("/WEB-INF/jsp/customer-edit.jsp").forward(request, response);
            return;
        }

        try {
            // Use the JNDI-looked-up customerService
            Customer existingCustomer = customerService.getCustomerById(customerId);

            CustomerDto customerDto = new CustomerDto(
                    existingCustomer.getId(),
                    name,
                    existingCustomer.getEmail(),
                    address,
                    phoneNumber
            );

            customerService.updateCustomer(customerDto);

            LOGGER.info("CustomerEditServlet: Customer profile " + customerId + " updated successfully by user: " + loggedInUser.getUsername());
            FlashMessageUtil.putSuccessMessage(request.getSession(), "Customer profile for " + existingCustomer.getName() + " updated successfully!");
            response.sendRedirect(request.getContextPath() + "/customers/manage");

        } catch (Exception e) {
            String displayErrorMessage = ServletUtil.getRootErrorMessage(e, "An unexpected error occurred during profile update. Please try again later.", LOGGER);
            request.setAttribute("errorMessage", displayErrorMessage);
            try { // Reload customer data for form re-population
                // Use the JNDI-looked-up customerService
                request.setAttribute("customer", customerService.getCustomerById(customerId));
            } catch (Exception ignore) { LOGGER.severe("Customer not found while trying to re-populate form after error: " + customerId); }
            request.getRequestDispatcher("/WEB-INF/jsp/customer-edit.jsp").forward(request, response);
        }
    }
}