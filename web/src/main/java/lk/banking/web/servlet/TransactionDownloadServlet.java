package lk.banking.web.servlet;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.dto.TransactionDto;
import lk.banking.core.entity.Transaction;
import lk.banking.core.entity.enums.UserRole;
import lk.banking.core.mapper.TransactionMapper;
import lk.banking.services.TransactionServices;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// IMPORTANT: The URL has changed to match the filter pattern
@WebServlet("/pdf/transactions")
public class TransactionDownloadServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TransactionDownloadServlet.class.getName());

    @Inject
    private TransactionServices transactionService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOGGER.info("Preparing data for transaction PDF rendering.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.hasRole(UserRole.CUSTOMER)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized");
            return;
        }

        try {
            // Fetch a safe number of transactions
            List<Transaction> entityTransactions = transactionService.getTransactionsByUser(loggedInUser.getId(), 500);
            List<TransactionDto> transactionDtos = entityTransactions.stream()
                    .map(TransactionMapper::toDto)
                    .collect(Collectors.toList());

            // Set attributes for the PDF JSP
            request.setAttribute("transactionsForPdf", transactionDtos);
            request.setAttribute("pdfTitle", "Recent Transaction Statement");
            request.setAttribute("loggedInUser", loggedInUser); // The JSP needs this too

            // Forward to the PDF-rendering JSP. The PdfFilter will intercept the output.
            request.getRequestDispatcher("/WEB-INF/jsp/transactionPdfView.jsp").forward(request, response);

        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to prepare data for transaction PDF for user " + loggedInUser.getUsername(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred while preparing your PDF data.");
        }
    }
}