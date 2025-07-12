package lk.banking.web.servlet;

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

        // Basic server-side validation - more comprehensive validation is in the service layer
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty() ||
                email == null || email.trim().isEmpty() || name == null || name.trim().isEmpty() ||
                address == null || address.trim().isEmpty() || phoneNumber == null || phoneNumber.trim().isEmpty()) {
            request.setAttribute("errorMessage", "All fields are required.");
            LOGGER.warning("RegisterServlet: Missing required fields in registration form.");
            doGet(request, response);
            return;
        }

        try {
            // Call the updated register method with all customer details
            User registeredUser = userManagementService.register(
                    username,
                    password,
                    email,
                    name,
                    address,
                    phoneNumber,
                    UserRole.CUSTOMER // Self-registration defaults to CUSTOMER
            );

            LOGGER.info("RegisterServlet: User '" + username + "' registered successfully. Redirecting to login.");
            response.sendRedirect(request.getContextPath() + "/login?registrationSuccess=true");
        } catch (ValidationException e) {
            request.setAttribute("errorMessage", e.getMessage());
            LOGGER.warning("RegisterServlet: Validation error for user '" + username + "': " + e.getMessage());
            doGet(request, response);
        } catch (ResourceConflictException e) {
            request.setAttribute("errorMessage", e.getMessage());
            LOGGER.warning("RegisterServlet: Conflict during registration for user '" + username + "': " + e.getMessage());
            doGet(request, response);
        } catch (RoleNotFoundException e) {
            request.setAttribute("errorMessage", "System error: Required user role not found. Please contact support.");
            LOGGER.log(java.util.logging.Level.SEVERE, "RegisterServlet: Role not found during registration for '" + username + "'.", e);
            doGet(request, response);
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "RegisterServlet: An unexpected error occurred during registration for user '" + username + "'.", e);
            request.setAttribute("errorMessage", "An unexpected error occurred during registration. Please try again later.");
            doGet(request, response);
        }
    }
}