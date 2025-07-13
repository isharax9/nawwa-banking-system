package lk.banking.web.servlet;

import jakarta.ejb.EJB; // For EJB injection
import jakarta.ejb.EJBException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.dto.UserDto; // To pass DTOs to JSP
import lk.banking.core.entity.User; // For User entity (before mapping)
import lk.banking.core.entity.enums.UserRole; // For role checks
import lk.banking.core.exception.BankingException;
import lk.banking.core.exception.UserNotFoundException; // For specific user-related errors
import lk.banking.core.mapper.UserMapper; // For mapping entities to DTOs
import lk.banking.security.UserManagementService; // To get all users
import lk.banking.web.util.FlashMessageUtil;
import lk.banking.web.util.ServletUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Servlet for displaying and managing a list of all users in the system.
 * Accessible by ADMIN and EMPLOYEE roles only.
 */
@WebServlet("/users/manage") // Maps to /users/manage
public class UserManagementListServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(UserManagementListServlet.class.getName());

    @EJB
    private UserManagementService userManagementService; // Inject UserManagementService

    /**
     * Handles GET requests to display the list of users.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("UserManagementListServlet: Handling GET request.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        // Security check: Only ADMIN or EMPLOYEE can access this page
        if (loggedInUser == null || (!loggedInUser.hasRole(UserRole.ADMIN) && !loggedInUser.hasRole(UserRole.EMPLOYEE))) {
            LOGGER.warning("UserManagementListServlet: Unauthorized access attempt by user: " + (loggedInUser != null ? loggedInUser.getUsername() : "N/A"));
            FlashMessageUtil.putErrorMessage(request.getSession(), "Access denied. You do not have permission to manage users.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        try {
            // Fetch all users from the service layer
            List<User> entityUsers = userManagementService.getAllUsers();

            // Convert User entities to UserDto for presentation (crucial for security - no passwords!)
            List<UserDto> userDtos = entityUsers.stream()
                    .map(UserMapper::toDto)
                    .collect(Collectors.toList());

            request.setAttribute("users", userDtos); // Pass DTOs to JSP
            LOGGER.info("UserManagementListServlet: Loaded " + userDtos.size() + " users for management.");

            request.getRequestDispatcher("/WEB-INF/jsp/users-manage.jsp").forward(request, response);

        } catch (EJBException e) {
            Exception unwrappedException = ServletUtil.unwrapEJBException(e);
            String displayErrorMessage;
            if (unwrappedException instanceof BankingException) {
                displayErrorMessage = "Banking error: " + unwrappedException.getMessage();
                LOGGER.warning("UserManagementListServlet: Banking exception during user fetch: " + displayErrorMessage);
            } else {
                displayErrorMessage = "An unexpected error occurred while fetching users. Please try again later.";
                LOGGER.log(java.util.logging.Level.SEVERE, "UserManagementListServlet: Unexpected EJBException during user fetch for " + loggedInUser.getUsername(), unwrappedException);
            }
            FlashMessageUtil.putErrorMessage(request.getSession(), displayErrorMessage);
            response.sendRedirect(request.getContextPath() + "/dashboard"); // Redirect on error
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "UserManagementListServlet: An unhandled general error occurred during user fetch for " + loggedInUser.getUsername(), e);
            FlashMessageUtil.putErrorMessage(request.getSession(), "An unexpected error occurred. Please try again later.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
        }
    }

    /**
     * Handles POST requests for actions on users (e.g., activate/deactivate, delete).
     * This will be expanded later.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // This method will be implemented later for user actions like activate/deactivate/delete
        LOGGER.info("UserManagementListServlet: Handling POST request (actions not yet implemented).");
        doGet(request, response); // For now, just re-display the list
    }
}