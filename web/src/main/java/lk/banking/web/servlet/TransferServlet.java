//package lk.banking.web.servlet;
//
//import jakarta.inject.Inject;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.annotation.WebServlet;
//import jakarta.servlet.http.*;
//import lk.banking.core.entity.User;
//import lk.banking.services.TransactionServices;
//
//import java.io.IOException;
//import java.math.BigDecimal;
//
//@WebServlet("/transfer")
//public class TransferServlet extends HttpServlet {
//    @Inject
//    private TransactionServices transactionServices;
//
//    @Override
//    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        User user = (User) req.getSession().getAttribute("user");
//        String fromAccount = req.getParameter("fromAccount");
//        String toAccount = req.getParameter("toAccount");
//        BigDecimal amount = new BigDecimal(req.getParameter("amount"));
//
//        boolean success = transactionServices.transferFunds(user.getId(), fromAccount, toAccount, amount);
//        if (success) {
//            resp.sendRedirect(req.getContextPath() + "/pages/transaction-history.xhtml?success");
//        } else {
//            req.setAttribute("error", "Transfer failed");
//            req.getRequestDispatcher("/pages/transfer-funds.xhtml").forward(req, resp);
//        }
//    }
//}