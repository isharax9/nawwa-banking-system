package lk.banking.web.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lk.banking.core.entity.User;
import lk.banking.security.AuthenticationService;
import lk.banking.web.util.WebUtils;

import java.io.Serializable;

@Named
@RequestScoped
public class LoginController implements Serializable {

    private String username;
    private String password;

    @Inject
    private AuthenticationService authenticationService;

    public String login() {
        FacesContext context = FacesContext.getCurrentInstance();
        User user = authenticationService.authenticate(username, password);
        if (user != null) {
            HttpServletRequest request = WebUtils.getRequest();
            request.getSession().setAttribute("user", user);
            return "/pages/dashboard.xhtml?faces-redirect=true";
        } else {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Invalid username or password", null));
            return null;
        }
    }

    public String logout() {
        HttpServletRequest request = WebUtils.getRequest();
        request.getSession().invalidate();
        return "/pages/login.xhtml?faces-redirect=true";
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}