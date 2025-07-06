package lk.banking.web.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.User;
import lk.banking.services.TransactionService;
import lk.banking.web.util.WebUtils;

import java.io.Serializable;
import java.util.List;

@Named
@RequestScoped
public class TransactionController implements Serializable {

    @Inject
    private TransactionService transactionService;

    public List<Transaction> getUserTransactions() {
        User user = WebUtils.getLoggedInUser();
        if (user == null) return List.of();
        return transactionService.getTransactionsByUserId(user.getId());
    }
}