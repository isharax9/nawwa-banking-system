package lk.banking.web.servlet;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.dto.UserDto;
import lk.banking.core.entity.User;
import lk.banking.core.entity.enums.UserRole;
import lk.banking.core.exception.BankingException;
import lk.banking.core.exception.UserNotFoundException;
import lk.banking.core.mapper.UserMapper;
import lk.banking.security.UserManagementService;
import lk.banking.web.util.FlashMessageUtil;
import lk.banking.web.util.ServletUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@WebServlet("/users/manage")
public class UserManagementListServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(UserManagementListServlet.class.getName());

    @EJB
    private UserManagementService userManagementService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("UserManagementListServlet: Handling GET request.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null || (!loggedInUser.hasRole(UserRole.ADMIN) && !loggedInUser.hasRole(UserRole.EMPLOYEE))) {
            LOGGER.warning("UserManagementListServlet: Unauthorized access attempt by user: " + (loggedInUser != null ? loggedInUser.getUsername() : "N/A"));
            FlashMessageUtil.putErrorMessage(request.getSession(), "Access denied. You do not have permission to manage users.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        FlashMessageUtil.retrieveAndClearMessages(request); // Retrieve flash messages on GET

        try {
            List<User> entityUsers = userManagementService.getAllUsers();
            List<UserDto> userDtos = entityUsers.stream()
                    .map(UserMapper::toDto)
                    .collect(Collectors.toList());

            request.setAttribute("users", userDtos);
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
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "UserManagementListServlet: An unhandled general error occurred during user fetch for " + loggedInUser.getUsername(), e);
            FlashMessageUtil.putErrorMessage(request.getSession(), "An unexpected error occurred. Please try again later.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
        }
    }

    /**
     * Handles POST requests for actions on users (e.g., deactivate/activate).
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("UserManagementListServlet: Handling POST request for user action.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null || (!loggedInUser.hasRole(UserRole.ADMIN) && !loggedInUser.hasRole(UserRole.EMPLOYEE))) {
            LOGGER.warning("UserManagementListServlet: Unauthorized POST access attempt by user: " + (loggedInUser != null ? loggedInUser.getUsername() : "N/A"));
            FlashMessageUtil.putErrorMessage(request.getSession(), "Access denied. You do not have permission to manage users.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        String userIdStr = request.getParameter("userId");
        String action = request.getParameter("action"); // "deactivate" or "activate"

        Long userId = null;
        String errorMessage = null;

        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            errorMessage = "User ID is missing.";
        } else {
            try { userId = Long.parseLong(userIdStr); }
            catch (NumberFormatException e) { errorMessage = "Invalid User ID format."; }
        }

        if (errorMessage == null && (action == null || action.trim().isEmpty())) {
            errorMessage = "Action (activate/deactivate) is required.";
        }

        if (errorMessage != null) {
            FlashMessageUtil.putErrorMessage(request.getSession(), errorMessage);
            LOGGER.warning("UserManagementListServlet: Validation error for user action: " + errorMessage);
            response.sendRedirect(request.getContextPath() + "/users/manage"); // Redirect back to list
            return;
        }

        try {
            String successMessage = null;
            if ("deactivate".equals(action)) {
                userManagementService.removeUser(userId); // This now performs soft delete
                successMessage = "User " + userId + " successfully deactivated.";
                LOGGER.info("UserManagementListServlet: Deactivated user ID: " + userId + " by " + loggedInUser.getUsername());
            } else if ("activate".equals(action)) {
                userManagementService.activateUser(userId); // Call the new activateUser method
                successMessage = "User " + userId + " successfully activated.";
                LOGGER.info("UserManagementListServlet: Activated user ID: " + userId + " by " + loggedInUser.getUsername());
            } else {
                errorMessage = "Invalid action specified.";
                LOGGER.warning("UserManagementListServlet: Invalid action: " + action + " for user ID: " + userId);
            }

            if (errorMessage != null) {
                FlashMessageUtil.putErrorMessage(request.getSession(), errorMessage);
            } else {
                FlashMessageUtil.putSuccessMessage(request.getSession(), successMessage);
            }
            response.sendRedirect(request.getContextPath() + "/users/manage"); // Redirect back to list

        } catch (EJBException e) {
            Exception unwrappedException = ServletUtil.unwrapEJBException(e);
            String displayErrorMessage;
            if (unwrappedException instanceof UserNotFoundException) {
                displayErrorMessage = "User not found: " + unwrappedException.getMessage();
                LOGGER.warning("UserManagementListServlet: User not found during action: " + displayErrorMessage);
            } else if (unwrappedException instanceof BankingException) {
                displayErrorMessage = "Banking error during user action: " + unwrappedException.getMessage();
                LOGGER.warning("UserManagementListServlet: Banking exception during user action: " + displayErrorMessage);
            } else {
                displayErrorMessage = "An unexpected error occurred during user action. Please try again later.";
                LOGGER.log(java.util.logging.Level.SEVERE, "UserManagementListServlet: Unexpected EJBException during user action for " + loggedInUser.getUsername(), unwrappedException);
            }
            FlashMessageUtil.putErrorMessage(request.getSession(), displayErrorMessage);
            response.sendRedirect(request.getContextPath() + "/users/manage");
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "UserManagementListServlet: An unhandled general error occurred during user action for " + loggedInUser.getUsername(), e);
            FlashMessageUtil.putErrorMessage(request.getSession(), "An unexpected error occurred. Please try again later.");
            response.sendRedirect(request.getContextPath() + "/users/manage");
        }
    }
}