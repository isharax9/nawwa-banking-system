package lk.banking.web.servlet;

import jakarta.ejb.EJBException; // Import EJBException
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.entity.User;
import lk.banking.core.entity.enums.UserRole;
import lk.banking.core.exception.ResourceConflictException;
import lk.banking.core.exception.RoleNotFoundException;
import lk.banking.core.exception.ValidationException;
import lk.banking.core.util.ValidationUtils;
import lk.banking.security.UserManagementService;

import java.io.IOException;
import java.util.logging.Logger;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(RegisterServlet.class.getName());

    @Inject
    private UserManagementService userManagementService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("RegisterServlet: Handling GET request to display registration form.");
        request.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String email = request.getParameter("email");
        String name = request.getParameter("name");
        String address = request.getParameter("address");
        String phoneNumber = request.getParameter("phoneNumber");

        LOGGER.info("RegisterServlet: Processing POST request for username: " + username + ", email: " + email);

        String errorMessage = null;
        if (username == null || username.trim().isEmpty()) {
            errorMessage = "Username is required.";
        } else if (password == null || password.trim().isEmpty()) {
            errorMessage = "Password is required.";
        } else if (email == null || email.trim().isEmpty()) {
            errorMessage = "Email is required.";
        } else if (!ValidationUtils.isValidEmail(email)) {
            errorMessage = "Invalid email format.";
        } else if (name == null || name.trim().isEmpty()) {
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
            LOGGER.warning("RegisterServlet: Validation error: " + errorMessage);
            request.setAttribute("param", request.getParameterMap());
            doGet(request, response);
            return;
        }

        try {
            User registeredUser = userManagementService.register(
                    username,
                    password,
                    email,
                    name,
                    address,
                    phoneNumber,
                    UserRole.CUSTOMER
            );

            LOGGER.info("RegisterServlet: User '" + username + "' registered successfully. Redirecting to login.");
            response.sendRedirect(request.getContextPath() + "/login?registrationSuccess=true");
        }
        // IMPORTANT: Catch EJBException first, and then check its cause
        catch (EJBException e) {
            Throwable cause = e.getCause(); // Get the underlying cause of the EJBException

            if (cause instanceof ValidationException) {
                request.setAttribute("errorMessage", cause.getMessage());
                LOGGER.warning("RegisterServlet: Validation error for user '" + username + "': " + cause.getMessage());
            } else if (cause instanceof ResourceConflictException) {
                request.setAttribute("errorMessage", cause.getMessage());
                LOGGER.warning("RegisterServlet: Conflict during registration for user '" + username + "': " + cause.getMessage());
            } else if (cause instanceof RoleNotFoundException) {
                request.setAttribute("errorMessage", "System error: Required user role not found. Please contact support.");
                LOGGER.log(java.util.logging.Level.SEVERE, "RegisterServlet: Role not found during registration for '" + username + "'.", e);
            } else {
                // If it's another type of EJBException cause, treat as unexpected
                LOGGER.log(java.util.logging.Level.SEVERE, "RegisterServlet: An unexpected EJBException occurred during registration for user '" + username + "'.", e);
                request.setAttribute("errorMessage", "An unexpected error occurred during registration. Please try again later.");
            }
            request.setAttribute("param", request.getParameterMap()); // Re-populate fields
            doGet(request, response);
        }
        // Catch any other exceptions that are not wrapped in EJBException (less common from EJB calls)
        catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "RegisterServlet: An unexpected non-EJB exception occurred during registration for user '" + username + "'.", e);
            request.setAttribute("errorMessage", "An unexpected error occurred during registration. Please try again later.");
            request.setAttribute("param", request.getParameterMap()); // Re-populate fields
            doGet(request, response);
        }
    }
}