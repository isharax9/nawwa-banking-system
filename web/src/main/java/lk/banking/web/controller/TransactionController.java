package lk.banking.web.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lk.banking.core.entity.Transaction;
import lk.banking.services.TransactionServices;
import lk.banking.web.util.WebUtils;
import lk.banking.core.entity.User;

import java.util.List;

@Named
@RequestScoped
public class TransactionController {

    @Inject
    private TransactionServices transactionService;

    public List<Transaction> getMyTransactions() {
        User user = WebUtils.getLoggedInUser();
        return user != null ? transactionService.getTransactionsByUser(user.getId()) : List.of();
    }
}