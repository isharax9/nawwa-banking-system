package lk.banking.web.servlet;

import jakarta.ejb.EJBException;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.entity.User;
import lk.banking.core.entity.enums.UserRole;
// Removed specific exception imports
// import lk.banking.core.exception.ResourceConflictException;
// import lk.banking.core.exception.RoleNotFoundException;
// import lk.banking.core.exception.ValidationException;
import lk.banking.core.util.ValidationUtils;
import lk.banking.security.UserManagementService;
import lk.banking.web.util.FlashMessageUtil;
import lk.banking.web.util.ServletUtil; // Already imported

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
            userManagementService.register(
                    username,
                    password,
                    email,
                    name,
                    address,
                    phoneNumber,
                    UserRole.CUSTOMER
            );

            LOGGER.info("RegisterServlet: User '" + username + "' registered successfully. Redirecting to login.");
            FlashMessageUtil.putSuccessMessage(request.getSession(), "Registration successful! You can now log in.");
            response.sendRedirect(request.getContextPath() + "/login");
        } catch (Exception e) { // Catch generic Exception
            // Use the new ServletUtil.getRootErrorMessage to handle all exceptions consistently
            String displayErrorMessage = ServletUtil.getRootErrorMessage(e, "An unexpected error occurred during registration. Please try again later.", LOGGER);
            request.setAttribute("errorMessage", displayErrorMessage);
            request.setAttribute("param", request.getParameterMap());
            doGet(request, response);
        }
    }
}