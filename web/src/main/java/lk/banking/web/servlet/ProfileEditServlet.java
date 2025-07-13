package lk.banking.web.servlet;

import jakarta.ejb.EJBException;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.dto.CustomerDto;
import lk.banking.core.entity.Customer;
// Removed specific exception imports
// import lk.banking.core.exception.CustomerNotFoundException;
// import lk.banking.core.exception.ValidationException;
import lk.banking.core.exception.CustomerNotFoundException;
import lk.banking.core.util.ValidationUtils;
import lk.banking.services.CustomerService;
import lk.banking.web.util.FlashMessageUtil;
import lk.banking.web.util.ServletUtil; // Already imported

import java.io.IOException;
import java.util.logging.Logger;

@WebServlet("/profile-edit")
public class ProfileEditServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ProfileEditServlet.class.getName());

    @Inject
    private CustomerService customerService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("ProfileEditServlet: Handling GET request to display profile edit form.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null) {
            LOGGER.warning("ProfileEditServlet: No logged-in user found. Redirecting to login.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        if (!loggedInUser.hasRole(lk.banking.core.entity.enums.UserRole.CUSTOMER)) {
            LOGGER.warning("ProfileEditServlet: Non-customer user (" + loggedInUser.getUsername() + ") attempted to access profile edit.");
            FlashMessageUtil.putErrorMessage(request.getSession(), "Access denied. Only customers can edit profiles.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        try {
            Customer customer = customerService.getCustomerByEmail(loggedInUser.getEmail());
            if (customer == null) {
                LOGGER.severe("ProfileEditServlet: Customer profile not found for logged-in user email: " + loggedInUser.getEmail());
                request.setAttribute("errorMessage", "Your customer profile could not be loaded. Please contact support.");
                request.getRequestDispatcher("/WEB-INF/jsp/dashboard.jsp").forward(request, response);
                return;
            }

            request.setAttribute("customer", customer);
            request.getRequestDispatcher("/WEB-INF/jsp/profile-edit.jsp").forward(request, response);

        } catch (EJBException e) {
            // Use the new ServletUtil to get the error message
            String displayErrorMessage = ServletUtil.getRootErrorMessage(e, "Error loading profile data.", LOGGER);
            FlashMessageUtil.putErrorMessage(request.getSession(), displayErrorMessage);
            response.sendRedirect(request.getContextPath() + "/dashboard"); // Redirect on error
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "ProfileEditServlet: An unexpected error occurred while loading profile for user " + loggedInUser.getUsername() + ".", e);
            FlashMessageUtil.putErrorMessage(request.getSession(), "An unexpected error occurred. Please try again later.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("ProfileEditServlet: Processing POST request for profile update.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null) {
            LOGGER.warning("ProfileEditServlet: No logged-in user found during POST. Redirecting to login.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        if (!loggedInUser.hasRole(lk.banking.core.entity.enums.UserRole.CUSTOMER)) {
            LOGGER.warning("ProfileEditServlet: Non-customer user (" + loggedInUser.getUsername() + ") attempted to submit profile edit.");
            FlashMessageUtil.putErrorMessage(request.getSession(), "Access denied. Only customers can edit profiles.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        String name = request.getParameter("name");
        String address = request.getParameter("address");
        String phoneNumber = request.getParameter("phoneNumber");

        String errorMessage = null;
        if (name == null || name.trim().isEmpty()) {
            errorMessage = "Full Name is required.";
        } else if (address == null || address.trim().isEmpty()) {
            errorMessage = "Address is required.";
        } else if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            errorMessage = "Phone Number is required.";
        } else if (!ValidationUtils.isValidPhoneNumber(phoneNumber)) {
            errorMessage = "Invalid phone number format. Must be 10-15 digits.";
        }

        if (errorMessage != null) {
            request.setAttribute("errorMessage", errorMessage);
            LOGGER.warning("ProfileEditServlet: Validation error: " + errorMessage);
            try {
                request.setAttribute("customer", customerService.getCustomerByEmail(loggedInUser.getEmail()));
            } catch (Exception ignore) { /* Logged by service */ }
            doGet(request, response);
            return;
        }

        try {
            Customer customer = customerService.getCustomerByEmail(loggedInUser.getEmail());
            if (customer == null) {
                LOGGER.severe("ProfileEditServlet: Customer profile not found for logged-in user email: " + loggedInUser.getEmail() + " during update.");
                throw new CustomerNotFoundException("Your customer profile could not be found for update. Please contact support.");
            }

            CustomerDto customerDto = new CustomerDto(
                    customer.getId(),
                    name,
                    customer.getEmail(),
                    address,
                    phoneNumber
            );

            customerService.updateCustomer(customerDto);

            LOGGER.info("ProfileEditServlet: Customer profile updated successfully for user: " + loggedInUser.getUsername());
            FlashMessageUtil.putSuccessMessage(request.getSession(), "Your profile has been updated successfully!");
            response.sendRedirect(request.getContextPath() + "/dashboard");

        } catch (Exception e) { // Catch generic Exception
            // Use the new ServletUtil.getRootErrorMessage to handle all exceptions consistently
            String displayErrorMessage = ServletUtil.getRootErrorMessage(e, "An unexpected error occurred during profile update. Please try again later.", LOGGER);
            request.setAttribute("errorMessage", displayErrorMessage);
            try { request.setAttribute("customer", customerService.getCustomerByEmail(loggedInUser.getEmail())); }
            catch (Exception ignore) { LOGGER.severe("Customer not found for re-populating after error in ProfileEditServlet doPost"); }
            doGet(request, response);
        }
    }
}