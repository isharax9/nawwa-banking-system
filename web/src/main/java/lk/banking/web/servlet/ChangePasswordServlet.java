package lk.banking.web.servlet;

import jakarta.ejb.EJBException;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.LoggedInUser;
// Removed specific exception imports
// import lk.banking.core.exception.UnauthorizedAccessException;
// import lk.banking.core.exception.UserNotFoundException;
// import lk.banking.core.exception.ValidationException;
import lk.banking.security.AuthenticationService;
import lk.banking.web.util.FlashMessageUtil;
import lk.banking.web.util.ServletUtil; // Already imported

import java.io.IOException;
import java.util.logging.Logger;

@WebServlet("/change-password")
public class ChangePasswordServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ChangePasswordServlet.class.getName());

    @Inject
    private AuthenticationService authenticationService;

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

        request.getRequestDispatcher("/WEB-INF/jsp/change-password.jsp").forward(request, response);
    }

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
            doGet(request, response);
            return;
        }

        try {
            authenticationService.changePassword(loggedInUser.getId(), oldPassword, newPassword);

            LOGGER.info("ChangePasswordServlet: Password successfully changed for user: " + loggedInUser.getUsername());
            FlashMessageUtil.putSuccessMessage(request.getSession(), "Your password has been changed successfully!");
            response.sendRedirect(request.getContextPath() + "/dashboard");

        } catch (Exception e) { // Catch generic Exception
            // Use the new ServletUtil.getRootErrorMessage to handle all exceptions consistently
            String displayErrorMessage = ServletUtil.getRootErrorMessage(e, "An unexpected error occurred during password change. Please try again later.", LOGGER);
            request.setAttribute("errorMessage", displayErrorMessage);
            doGet(request, response);
        }
    }
}