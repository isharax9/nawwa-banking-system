package lk.banking.web.servlet;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.entity.User;
import lk.banking.core.entity.enums.UserRole; // Needed for assigning role
import lk.banking.core.exception.ResourceConflictException; // For duplicate username/email
import lk.banking.core.exception.RoleNotFoundException;     // If a role isn't configured
import lk.banking.core.exception.ValidationException;       // For password/username validation
import lk.banking.security.UserManagementService;

import java.io.IOException;

/**
 * Servlet for handling user registration.
 * Displays the registration form on GET and processes registration on POST.
 */
@WebServlet("/register") // Maps the servlet to the URL /register
public class RegisterServlet extends HttpServlet {

    @Inject // Inject the UserManagementService EJB
    private UserManagementService userManagementService;

    /**
     * Handles GET requests to display the registration form.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Forward to the registration JSP page
        request.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(request, response);
    }

    /**
     * Handles POST requests to process registration form submissions.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password"); // Plaintext password from form

        // Basic server-side validation (more comprehensive validation is in the service layer)
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            request.setAttribute("errorMessage", "Username and password are required.");
            doGet(request, response); // Redisplay form with error
            return;
        }

        try {
            // For self-registration, default to CUSTOMER role.
            // If you have different registration flows (e.g., admin registering employee),
            // you'd add role selection to the form or a different servlet.
            User registeredUser = userManagementService.register(username, password, UserRole.CUSTOMER);

            // If registration is successful, redirect to a success page or login page
            request.setAttribute("successMessage", "Registration successful! You can now log in.");
            response.sendRedirect(request.getContextPath() + "/login?registrationSuccess=true"); // Redirect with a success flag
        } catch (ValidationException e) {
            // Password strength, format issues
            request.setAttribute("errorMessage", e.getMessage());
            doGet(request, response); // Redisplay form with error
        } catch (ResourceConflictException e) {
            // Username already exists
            request.setAttribute("errorMessage", e.getMessage());
            doGet(request, response); // Redisplay form with error
        } catch (RoleNotFoundException e) {
            // This should ideally not happen if roles are pre-populated, but good to catch
            request.setAttribute("errorMessage", "System error: Required user role not found. Please contact support.");
            e.printStackTrace(); // Log for debugging
            doGet(request, response);
        } catch (Exception e) {
            // Catch any other unexpected exceptions from the EJB layer
            e.printStackTrace(); // Log the full stack trace for debugging
            request.setAttribute("errorMessage", "An unexpected error occurred during registration. Please try again later.");
            doGet(request, response); // Redisplay form with error
        }
    }
}