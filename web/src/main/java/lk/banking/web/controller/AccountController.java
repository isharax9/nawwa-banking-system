package lk.banking.web.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.User;
import lk.banking.transaction.AccountService;
import lk.banking.web.util.WebUtils;

import java.io.Serializable;
import java.util.List;

@Named
@RequestScoped
public class AccountController implements Serializable {

    @Inject
    private AccountService accountService;

    public List<Account> getAccounts() {
        User user = WebUtils.getLoggedInUser();
        if (user == null) return List.of();
        return accountService.getAccountsByUserId(user.getId());
    }
}