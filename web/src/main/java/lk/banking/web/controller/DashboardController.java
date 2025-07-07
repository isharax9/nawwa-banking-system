package lk.banking.web.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import lk.banking.core.entity.User;
import lk.banking.web.util.WebUtils;

@Named
@RequestScoped
public class DashboardController {

    public String getWelcomeMessage() {
        User user = WebUtils.getLoggedInUser();
        return user != null ? "Welcome, " + user.getUsername() + "!" : "Welcome!";
    }
}