package lk.banking.web.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.User;
import lk.banking.services.AccountService;
import lk.banking.web.util.WebUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Named
@RequestScoped
public class AccountController implements Serializable {

    @Inject
    private AccountService accountService;

    private List<Account> accounts;

    public List<Account> getAccounts() {
        if (accounts == null) {
            User user = WebUtils.getLoggedInUser();
            if (user != null) {
                // Adjust the following line to match your actual AccountService method!
                accounts = accountService.findAccountsByUserId(user.getId());
            } else {
                accounts = Collections.emptyList();
            }
        }
        return accounts;
    }
}