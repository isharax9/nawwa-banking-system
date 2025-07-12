package lk.banking.web.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lk.banking.web.util.FlashMessageUtil; // Import FlashMessageUtil

import java.io.IOException;
import java.util.logging.Logger;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LogoutServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");

        if ("confirm".equals(action)) {
            LOGGER.info("LogoutServlet: Handling GET request for logout confirmation.");
            request.getRequestDispatcher("/WEB-INF/jsp/logout-confirm.jsp").forward(request, response);
        } else {
            LOGGER.info("LogoutServlet: Direct GET to /logout without action, redirecting to confirmation.");
            response.sendRedirect(request.getContextPath() + "/logout?action=confirm");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");

        if ("logout".equals(action)) {
            LOGGER.info("LogoutServlet: Processing POST request for actual logout.");

            HttpSession session = request.getSession(false);
            String username = "unknown";
            if (session != null) {
                if (session.getAttribute("loggedInUser") != null && session.getAttribute("loggedInUser") instanceof lk.banking.core.dto.LoggedInUser) {
                    username = ((lk.banking.core.dto.LoggedInUser) session.getAttribute("loggedInUser")).getUsername();
                }
                session.invalidate();
                LOGGER.info("LogoutServlet: User '" + username + "' logged out and session invalidated.");
            } else {
                LOGGER.info("LogoutServlet: No active session found during actual logout attempt for user '" + username + "'.");
            }

            // Redirect to the login page, passing success message as a flash message
            FlashMessageUtil.putSuccessMessage(request.getSession(true), "You have been successfully logged out."); // Create new session for message
            response.sendRedirect(request.getContextPath() + "/login"); // No more URL params
        } else {
            LOGGER.warning("LogoutServlet: Invalid POST action for logout: " + action + ". Redirecting to dashboard.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
        }
    }
}