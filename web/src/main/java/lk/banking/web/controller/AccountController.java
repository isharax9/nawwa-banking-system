package lk.banking.web.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lk.banking.core.entity.Account;
import lk.banking.services.AccountService;
import lk.banking.web.util.WebUtils;
import lk.banking.core.dto.LoggedInUser; // Import the new LoggedInUser DTO

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Named
@RequestScoped
public class AccountController implements Serializable {

    @Inject
    private AccountService accountService;

    private List<Account> accounts; // Still returning Account entities for display, which is fine for JSF

    public List<Account> getAccounts() {
        if (accounts == null) {
            LoggedInUser loggedInUser = WebUtils.getLoggedInUser(); // Get LoggedInUser DTO
            if (loggedInUser != null) {
                // Call AccountService with the ID from the LoggedInUser DTO
                accounts = accountService.findAccountsByUserId(loggedInUser.getId());
            } else {
                accounts = Collections.emptyList();
            }
        }
        return accounts;
    }

    // You might add methods here for creating/updating/deleting accounts
    // For example:
    /*
    private AccountDto newAccountDto; // For a form to create a new account

    public void createNewAccount() {
        if (newAccountDto != null) {
            LoggedInUser loggedInUser = WebUtils.getLoggedInUser();
            if (loggedInUser != null && loggedInUser.getCustomerId() != null) {
                newAccountDto.setCustomerId(loggedInUser.getCustomerId()); // Set customer ID for the new account
                accountService.createAccount(newAccountDto);
                // After creation, clear the list to force re-fetch
                accounts = null;
                newAccountDto = null; // Clear form
                WebUtils.addInfoMessage("Account created successfully!"); // Example feedback
            } else {
                 WebUtils.addErrorMessage("Could not create account: User or Customer ID not found.");
            }
        }
    }

    public AccountDto getNewAccountDto() {
        if (newAccountDto == null) {
            newAccountDto = new AccountDto();
        }
        return newAccountDto;
    }
    public void setNewAccountDto(AccountDto newAccountDto) { this.newAccountDto = newAccountDto; }

    // (You'd also need a WebUtils.addInfoMessage / addErrorMessage for JSF messages)
    */
}