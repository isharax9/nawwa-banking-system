package lk.banking.web.servlet;

import jakarta.ejb.EJBException;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.entity.Customer;
import lk.banking.core.entity.User;
import lk.banking.core.entity.enums.UserRole;
import lk.banking.core.exception.UnauthorizedAccessException;
import lk.banking.core.mapper.LoggedInUserMapper;
import lk.banking.security.AuthenticationService;
import lk.banking.services.CustomerService;
import lk.banking.web.util.FlashMessageUtil;
import lk.banking.web.util.ServletUtil; // Already imported

import java.io.IOException;
import java.util.logging.Logger;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

    @Inject
    private AuthenticationService authenticationService;

    @Inject
    private CustomerService customerService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("LoginServlet: Handling GET request to display login form.");

        FlashMessageUtil.retrieveAndClearMessages(request);

        request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        LOGGER.info("LoginServlet: Processing POST request for username: " + username);
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            request.setAttribute("errorMessage", "Username and password are required.");
            LOGGER.warning("LoginServlet: Missing username or password.");
            doGet(request, response);
            return;
        }

        try {
            User authenticatedUser = authenticationService.authenticate(username, password);

            Long customerId = null;
            if (authenticatedUser.getRoles().stream().anyMatch(role -> role.getName() == UserRole.CUSTOMER)) {
                try {
                    Customer customer = customerService.getCustomerByEmail(authenticatedUser.getEmail());
                    if (customer != null) {
                        customerId = customer.getId();
                        LOGGER.info("LoginServlet: Found customer ID " + customerId + " for user " + username);
                    } else {
                        LOGGER.warning("LoginServlet: CUSTOMER user '" + username + "' has no linked Customer entity for email: " + authenticatedUser.getEmail());
                    }
                } catch (Exception customerEx) {
                    LOGGER.log(java.util.logging.Level.WARNING, "LoginServlet: Error fetching customer for user " + username, customerEx);
                }
            }

            LoggedInUser loggedInUser = LoggedInUserMapper.toLoggedInUser(authenticatedUser, customerId);
            request.getSession().setAttribute("loggedInUser", loggedInUser);

            LOGGER.info("LoginServlet: User '" + username + "' authenticated successfully. Redirecting to dashboard.");
            response.sendRedirect(request.getContextPath() + "/dashboard");

        } catch (Exception e) { // Catch generic Exception
            // Use the new ServletUtil.getRootErrorMessage to handle all exceptions consistently
            String displayErrorMessage = ServletUtil.getRootErrorMessage(e, "An unexpected error occurred during login. Please try again later.", LOGGER);
            request.setAttribute("errorMessage", displayErrorMessage);
            doGet(request, response);
        }
    }
}