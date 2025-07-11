package lk.banking.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
// import lk.banking.core.entity.User; // No longer needed directly here
import lk.banking.core.entity.enums.UserRole;
import lk.banking.core.dto.LoggedInUser; // Import the LoggedInUser DTO
import lk.banking.web.util.WebUtils; // Import your WebUtils for consistent login check

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebFilter(urlPatterns = {"/pages/admin/*"}) // Applies to all requests under /pages/admin/
public class SecurityFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.info("SecurityFilter initialized for admin pages.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Retrieve the logged-in user consistently using WebUtils
        LoggedInUser loggedInUser = WebUtils.getLoggedInUser();

        // Check if user is logged in AND has the ADMIN role
        // This filter assumes AuthenticationFilter has already ensured basic login.
        // If loggedInUser is null, it means no user is logged in, and they shouldn't access admin pages.
        // If loggedInUser exists, check for the ADMIN role.
        if (loggedInUser == null || !loggedInUser.hasRole(UserRole.ADMIN)) {
            LOGGER.warn("Unauthorized access attempt to admin page by user: {}. Redirecting to dashboard.",
                    loggedInUser != null ? loggedInUser.getUsername() : "Unauthenticated");
            // Redirect to a common dashboard or access denied page
            resp.sendRedirect(req.getContextPath() + "/pages/dashboard.xhtml?faces-redirect=true");
            return; // Stop the filter chain
        }

        // If the user is an ADMIN, proceed to the requested resource
        LOGGER.debug("Access granted for ADMIN user: {} to URI: {}", loggedInUser.getUsername(), req.getRequestURI());
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        LOGGER.info("SecurityFilter destroyed.");
    }
}