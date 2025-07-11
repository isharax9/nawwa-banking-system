package lk.banking.web.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
// import lk.banking.core.entity.User; // No longer needed directly here
import lk.banking.core.dto.LoggedInUser; // Import the LoggedInUser DTO
import lk.banking.web.util.WebUtils;

import java.io.Serializable; // Good practice for managed beans

@Named
@RequestScoped
public class DashboardController implements Serializable { // Add Serializable

    public String getWelcomeMessage() {
        LoggedInUser loggedInUser = WebUtils.getLoggedInUser(); // Correctly get LoggedInUser DTO
        // If loggedInUser is null, it means no user is currently authenticated
        return loggedInUser != null ? "Welcome, " + loggedInUser.getUsername() + "!" : "Welcome!";
    }

    // You might also add properties or methods to display other dashboard summaries here,
    // injecting services like AccountService or TransactionManager if needed.
}