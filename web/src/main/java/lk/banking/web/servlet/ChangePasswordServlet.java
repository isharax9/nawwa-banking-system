package lk.banking.web.servlet;

import jakarta.ejb.EJBException;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.exception.BankingException;
import lk.banking.core.exception.UnauthorizedAccessException; // For old password mismatch
import lk.banking.core.exception.UserNotFoundException;       // If user not found (shouldn't happen for logged-in)
import lk.banking.core.exception.ValidationException;         // For new password strength/format
import lk.banking.security.AuthenticationService;             // To change password
import lk.banking.web.util.FlashMessageUtil;
import lk.banking.web.util.ServletUtil;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Servlet for handling user password change requests.
 * Displays the form on GET and processes password update on POST.
 */
@WebServlet("/change-password")
public class ChangePasswordServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ChangePasswordServlet.class.getName());

    @Inject
    private AuthenticationService authenticationService; // Inject the AuthenticationService EJB

    /**
     * Handles GET requests to display the change password form.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("ChangePasswordServlet: Handling GET request to display form.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null) {
            LOGGER.warning("ChangePasswordServlet: No logged-in user found. Redirecting to login.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Just forward to the JSP, no data to pre-fill (passwords are never pre-filled)
        request.getRequestDispatcher("/WEB-INF/jsp/change-password.jsp").forward(request, response);
    }

    /**
     * Handles POST requests to process password change form submissions.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("ChangePasswordServlet: Processing POST request for password change.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null) {
            LOGGER.warning("ChangePasswordServlet: No logged-in user found during POST. Redirecting to login.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String oldPassword = request.getParameter("oldPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmNewPassword = request.getParameter("confirmNewPassword");

        String errorMessage = null;

        // Basic server-side validation
        if (oldPassword == null || oldPassword.trim().isEmpty() ||
                newPassword == null || newPassword.trim().isEmpty() ||
                confirmNewPassword == null || confirmNewPassword.trim().isEmpty()) {
            errorMessage = "All password fields are required.";
        } else if (!newPassword.equals(confirmNewPassword)) {
            errorMessage = "New password and confirmation do not match.";
        }

        if (errorMessage != null) {
            request.setAttribute("errorMessage", errorMessage);
            LOGGER.warning("ChangePasswordServlet: Validation error: " + errorMessage);
            doGet(request, response); // Redisplay form with error
            return;
        }

        try {
            // Call the AuthenticationService to change the password
            // It will validate old password and new password strength
            authenticationService.changePassword(loggedInUser.getId(), oldPassword, newPassword);

            LOGGER.info("ChangePasswordServlet: Password successfully changed for user: " + loggedInUser.getUsername());
            FlashMessageUtil.putSuccessMessage(request.getSession(), "Your password has been changed successfully!");
            response.sendRedirect(request.getContextPath() + "/dashboard"); // Redirect to dashboard on success

        } catch (EJBException e) {
            Exception unwrappedException = ServletUtil.unwrapEJBException(e);
            String displayErrorMessage;

            if (unwrappedException instanceof UnauthorizedAccessException) {
                displayErrorMessage = "Old password is incorrect. Please try again.";
                LOGGER.warning("ChangePasswordServlet: Old password mismatch for user '" + loggedInUser.getUsername() + "'.");
            } else if (unwrappedException instanceof ValidationException) {
                displayErrorMessage = "New password validation failed: " + unwrappedException.getMessage();
                LOGGER.warning("ChangePasswordServlet: New password validation failed for user '" + loggedInUser.getUsername() + "': " + unwrappedException.getMessage());
            } else if (unwrappedException instanceof UserNotFoundException) {
                // This case indicates a serious issue if loggedInUser.getId() is valid but user not found by service
                displayErrorMessage = "System error: Your user account could not be found for password change. Please contact support.";
                LOGGER.log(java.util.logging.Level.SEVERE, "ChangePasswordServlet: UserNotFoundException during password change for logged-in user ID " + loggedInUser.getId(), unwrappedException);
            }
            else if (unwrappedException instanceof BankingException) { // Generic BankingException fallback
                displayErrorMessage = "A banking-related error occurred: " + unwrappedException.getMessage();
                LOGGER.warning("ChangePasswordServlet: Banking error during password change: " + unwrappedException.getMessage());
            }
            else { // Unexpected system exception
                displayErrorMessage = "An unexpected error occurred during password change. Please try again later.";
                LOGGER.log(java.util.logging.Level.SEVERE, "ChangePasswordServlet: An unexpected system error during password change for user " + loggedInUser.getUsername(), unwrappedException);
            }
            request.setAttribute("errorMessage", displayErrorMessage);
            doGet(request, response); // Redisplay form with error
        } catch (Exception e) {
            // Final fallback for any other unhandled exceptions
            LOGGER.log(java.util.logging.Level.SEVERE, "ChangePasswordServlet: An unhandled general error occurred during password change for user " + loggedInUser.getUsername(), e);
            request.setAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
            doGet(request, response);
        }
    }
}