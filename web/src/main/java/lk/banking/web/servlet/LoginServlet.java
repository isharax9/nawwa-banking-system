package lk.banking.web.servlet;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import lk.banking.core.entity.User;
import lk.banking.security.AuthenticationService;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Inject
    private AuthenticationService authenticationService;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        User user = authenticationService.authenticate(username, password);
        if (user != null) {
            req.getSession(true).setAttribute("user", user);
            resp.sendRedirect(req.getContextPath() + "/pages/dashboard.xhtml");
        } else {
            req.setAttribute("error", "Invalid credentials");
            req.getRequestDispatcher("/pages/login.xhtml").forward(req, resp);
        }
    }
}