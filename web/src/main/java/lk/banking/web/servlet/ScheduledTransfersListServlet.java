package lk.banking.web.servlet;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.entity.Account;
import lk.banking.core.entity.ScheduledTransfer; // ScheduledTransfer entity
import lk.banking.core.exception.BankingException; // For generic exception handling
import lk.banking.transaction.ScheduledTransferService; // To get scheduled transfers
import lk.banking.web.util.FlashMessageUtil;
import lk.banking.web.util.ServletUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Servlet for displaying a list of scheduled transfers for the logged-in user.
 */
@WebServlet("/scheduled-transfers")
public class ScheduledTransfersListServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ScheduledTransfersListServlet.class.getName());

    // Use JNDI lookup for ScheduledTransferService
    private ScheduledTransferService scheduledTransferService;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            // CONFIRM THIS JNDI NAME FROM YOUR GLASSFISH LOGS AFTER DEPLOYMENT
            String scheduledTransferServiceJndiName = "java:global/banking-system-ear/transaction-services/ScheduledTransferServiceImpl!lk.banking.transaction.ScheduledTransferService";
            LOGGER.info("ScheduledTransfersListServlet: Attempting JNDI lookup for ScheduledTransferService at: " + scheduledTransferServiceJndiName);
            scheduledTransferService = (ScheduledTransferService) new InitialContext().lookup(scheduledTransferServiceJndiName);
            LOGGER.info("ScheduledTransfersListServlet: JNDI lookup successful for ScheduledTransferService.");
        } catch (NamingException e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "ScheduledTransfersListServlet: Failed to lookup ScheduledTransferService via JNDI.", e);
            throw new ServletException("Failed to initialize ScheduledTransfersListServlet: Required service not found.", e);
        }
    }

    /**
     * Handles GET requests to display the list of scheduled transfers.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("ScheduledTransfersListServlet: Handling GET request.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.hasRole(lk.banking.core.entity.enums.UserRole.CUSTOMER)) {
            LOGGER.warning("ScheduledTransfersListServlet: Unauthorized access. Redirecting to login.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            if (scheduledTransferService == null) {
                LOGGER.severe("ScheduledTransfersListServlet: ScheduledTransferService is null. JNDI lookup failed.");
                throw new ServletException("Service 'ScheduledTransferService' unavailable. Please try again later.");
            }

            // Fetch scheduled transfers relevant to the logged-in user
            // NOTE: Your ScheduledTransferService currently only has getPendingTransfers().
            // You will need a method like getScheduledTransfersByCustomerId(Long customerId)
            // or getScheduledTransfersByUserId(Long userId) in your ScheduledTransferService.
            // For now, I'll assume a placeholder method and make a note to add it.

            List<ScheduledTransfer> scheduledTransfers = Collections.emptyList();

            // Placeholder for fetching user-specific scheduled transfers
            // For now, it will fetch ALL pending transfers (which is not secure for a customer-facing view)
            // or you'll need to modify ScheduledTransferService
            // For now, let's assume we implement getScheduledTransfersByUserId.
            // This would require a relationship or a query to link User -> Customer -> Account -> ScheduledTransfer
            // As a quick fix, let's fetch all and filter by the user's accounts. (less efficient but works for small scale)

            // OPTION A (Better, requires service modification):
            // scheduledTransfers = scheduledTransferService.getScheduledTransfersByUserId(loggedInUser.getId());

            // OPTION B (Workaround for now, relies on fetching all and filtering):
            // This is NOT ideal for performance on large datasets.
            // Get all accounts of the logged-in user to filter by
            List<Account> userAccounts = lk.banking.web.util.ServletUtil.getAccountsForLoggedInUser(request, response, loggedInUser, null);
            List<Long> userAccountIds = userAccounts.stream().map(Account::getId).collect(java.util.stream.Collectors.toList());

            if (!userAccountIds.isEmpty()) {
                // Fetch all scheduled transfers and filter them by the user's accounts
                List<ScheduledTransfer> allTransfers = scheduledTransferService.getAllScheduledTransfers(); // Assuming this method exists or you add it
                scheduledTransfers = allTransfers.stream()
                        .filter(st -> userAccountIds.contains(st.getFromAccount().getId()) || userAccountIds.contains(st.getToAccount().getId()))
                        .collect(java.util.stream.Collectors.toList());
            }

            // OPTION C (If getPendingTransfers is all you have and want to show, but it's ALL pending, not user specific)
            // scheduledTransfers = scheduledTransferService.getPendingTransfers();
            // LOGGER.warning("ScheduledTransfersListServlet: Displaying ALL pending transfers, NOT user-specific. FIX ScheduledTransferService to filter by user.");


            request.setAttribute("scheduledTransfers", scheduledTransfers);
            LOGGER.info("ScheduledTransfersListServlet: Found " + scheduledTransfers.size() + " scheduled transfers for user " + loggedInUser.getUsername());

            request.getRequestDispatcher("/WEB-INF/jsp/scheduled-transfers.jsp").forward(request, response);

        } catch (EJBException e) {
            Exception unwrappedException = ServletUtil.unwrapEJBException(e);
            String displayErrorMessage = "An error occurred while loading scheduled transfers. " + unwrappedException.getMessage();
            LOGGER.log(java.util.logging.Level.SEVERE, "ScheduledTransfersListServlet: EJBException during scheduled transfers fetch for " + loggedInUser.getUsername(), unwrappedException);
            FlashMessageUtil.putErrorMessage(request.getSession(), displayErrorMessage);
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "ScheduledTransfersListServlet: Unexpected error during scheduled transfers fetch for " + loggedInUser.getUsername(), e);
            FlashMessageUtil.putErrorMessage(request.getSession(), "An unexpected error occurred. Please try again later.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
        }
    }
}