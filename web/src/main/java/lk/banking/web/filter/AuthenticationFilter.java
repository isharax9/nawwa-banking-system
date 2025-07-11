package lk.banking.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter(urlPatterns = {"/pages/*"})
public class AuthenticationFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);

        boolean loggedIn = (session != null && session.getAttribute("user") != null);

        String loginURI = req.getContextPath() + "/pages/login.xhtml";
        String registerURI = req.getContextPath() + "/pages/register.xhtml";

        boolean loginRequest = req.getRequestURI().equals(loginURI);
        boolean registerRequest = req.getRequestURI().equals(registerURI);

        // Allow access to login and registration pages without authentication
        if (!loggedIn && !loginRequest && !registerRequest) {
            resp.sendRedirect(loginURI);
        } else {
            chain.doFilter(request, response);
        }
    }
}