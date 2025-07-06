package lk.banking.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebFilter(urlPatterns = {"/pages/*"})
public class AuthenticationFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse resp = (HttpServletResponse)response;
        HttpSession session = req.getSession(false);

        boolean loggedIn = (session != null && session.getAttribute("user") != null);

        String loginURI = req.getContextPath() + "/pages/login.xhtml";
        boolean loginRequest = req.getRequestURI().equals(loginURI);

        if (!loggedIn && !loginRequest) {
            resp.sendRedirect(loginURI);
        } else {
            chain.doFilter(request, response);
        }
    }
}