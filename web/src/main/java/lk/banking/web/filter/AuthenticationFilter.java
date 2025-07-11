package lk.banking.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession; // Not strictly needed if using WebUtils, but good to keep for clarity

import lk.banking.core.dto.LoggedInUser; // Import the LoggedInUser DTO
import lk.banking.web.util.WebUtils; // Import your WebUtils for consistent login check

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebFilter(urlPatterns = {"/pages/*"}) // Applies to all requests under /pages/
public class AuthenticationFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Optional: Perform any initialization here
        LOGGER.info("AuthenticationFilter initialized.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Use WebUtils to check for logged-in user consistently
        LoggedInUser loggedInUser = WebUtils.getLoggedInUser();
        boolean loggedIn = (loggedInUser != null);

        // Define paths that do NOT require authentication
        String loginURI = req.getContextPath() + "/pages/login.xhtml";
        String registerURI = req.getContextPath() + "/pages/register.xhtml";

        // Get the requested URI (e.g., /your-app/pages/dashboard.xhtml)
        String requestURI = req.getRequestURI();

        boolean isLoginRequest = requestURI.equals(loginURI);
        boolean isRegisterRequest = requestURI.equals(registerURI);
        // You might want to allow access to CSS, JS, images even if not logged in.
        // Example: boolean isResourceRequest = requestURI.startsWith(req.getContextPath() + "/resources/");
        // if (isLoginRequest || isRegisterRequest || isResourceRequest) { ... }


        LOGGER.debug("Filtering request for URI: {}. LoggedIn: {}", requestURI, loggedIn);

        // Scenario 1: User is NOT logged in and trying to access a PROTECTED page
        if (!loggedIn && !isLoginRequest && !isRegisterRequest) {
            LOGGER.info("Unauthenticated access to protected URI: {}. Redirecting to login.", requestURI);
            resp.sendRedirect(loginURI);
        }
        // Scenario 2: User IS logged in and trying to access login/register pages (redirect to dashboard)
        // This prevents logged-in users from seeing login/register pages unless they log out.
        else if (loggedIn && (isLoginRequest || isRegisterRequest)) {
            LOGGER.info("Authenticated user accessing login/register page: {}. Redirecting to dashboard.", requestURI);
            resp.sendRedirect(req.getContextPath() + "/pages/dashboard.xhtml?faces-redirect=true");
        }
        // Scenario 3: User is logged in AND accessing a protected page, OR
        //             User is NOT logged in AND accessing an allowed page (login/register/resources)
        else {
            chain.doFilter(request, response); // Allow the request to proceed
        }
    }

    @Override
    public void destroy() {
        // Optional: Perform cleanup here
        LOGGER.info("AuthenticationFilter destroyed.");
    }
}