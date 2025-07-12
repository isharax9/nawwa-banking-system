package lk.banking.web.servlet;

import jakarta.inject.Inject; // For injecting EJBs
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet; // For simpler servlet mapping
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.entity.User;
import lk.banking.core.exception.UnauthorizedAccessException; // Your custom exception
import lk.banking.security.AuthenticationService; // Your EJB authentication service

import java.io.IOException;

/**
 * Servlet for handling user login requests.
 * Displays the login form on GET and processes login on POST.
 */
@WebServlet("/login") // This maps the servlet to the URL /login
public class LoginServlet extends HttpServlet {

    @Inject // Inject the AuthenticationService EJB
    private AuthenticationService authenticationService;

    /**
     * Handles GET requests to display the login form.
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @throws ServletException If a servlet-specific error occurs.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Forward to the login JSP page
        request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
    }

    /**
     * Handles POST requests to process login form submissions.
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @throws ServletException If a servlet-specific error occurs.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            request.setAttribute("errorMessage", "Username and password are required.");
            doGet(request, response); // Redisplay form with error
            return;
        }

        try {
            // Attempt to authenticate the user using the EJB service
            User authenticatedUser = authenticationService.authenticate(username, password);

            // If authentication is successful, store user info in session and redirect
            // IMPORTANT: For production, only store non-sensitive user info (e.g., ID, username, roles)
            request.getSession().setAttribute("loggedInUser", authenticatedUser); // Storing entire User entity for simplicity, but UserDto is safer
            request.getSession().setAttribute("username", authenticatedUser.getUsername());
            request.getSession().setAttribute("userId", authenticatedUser.getId());
            // You might also store roles:
            // request.getSession().setAttribute("userRoles", authenticatedUser.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()));

            // Redirect to a dashboard or welcome page
            response.sendRedirect(request.getContextPath() + "/dashboard"); // We'll create /dashboard next
        } catch (UnauthorizedAccessException e) {
            // Authentication failed due to invalid credentials
            request.setAttribute("errorMessage", "Invalid username or password.");
            doGet(request, response); // Redisplay form with error
        } catch (Exception e) {
            // Catch any other unexpected exceptions from the EJB layer
            e.printStackTrace(); // Log the full stack trace for debugging
            request.setAttribute("errorMessage", "An unexpected error occurred during login. Please try again later.");
            doGet(request, response); // Redisplay form with error
        }
    }
}