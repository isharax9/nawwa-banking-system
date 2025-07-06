package lk.banking.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.entity.User;
import lk.banking.core.entity.enums.UserRole;

import java.io.IOException;

@WebFilter(urlPatterns = {"/pages/admin/*"})
public class SecurityFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse resp = (HttpServletResponse)response;
        User user = (User) req.getSession().getAttribute("user");
        if (user == null || user.getRoles().stream().noneMatch(r -> r.getName() == UserRole.ADMIN)) {
            resp.sendRedirect(req.getContextPath() + "/pages/dashboard.xhtml");
            return;
        }
        chain.doFilter(request, response);
    }
}