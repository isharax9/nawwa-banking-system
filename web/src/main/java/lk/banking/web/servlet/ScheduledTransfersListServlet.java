package lk.banking.web.servlet;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.entity.ScheduledTransfer;
import lk.banking.core.entity.Account; // Import Account for filtering
import lk.banking.core.exception.BankingException;
import lk.banking.transaction.ScheduledTransferService;
import lk.banking.web.util.FlashMessageUtil;
import lk.banking.web.util.ServletUtil; // Already imported

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors; // For stream operations

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
            FlashMessageUtil.putErrorMessage(request.getSession(), "Access denied. You do not have permission to view scheduled transfers.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        FlashMessageUtil.retrieveAndClearMessages(request); // Retrieve flash messages on GET

        try {
            if (scheduledTransferService == null) {
                LOGGER.severe("ScheduledTransfersListServlet: ScheduledTransferService is null (should have been initialized).");
                throw new ServletException("Service 'ScheduledTransferService' unavailable. Please try again later.");
            }

            List<ScheduledTransfer> scheduledTransfers = Collections.emptyList();

            // Re-evaluating the best approach to get user-specific scheduled transfers:
            // The ideal is to have getScheduledTransfersByCustomerId(Long customerId) in ScheduledTransferService.
            // If that is available, use it.
            // If not, we have to filter locally, which means first getting all user accounts.

            // Get accounts for the logged-in user (customer role)
            // Passing LOGGER to ServletUtil.getAccountsForLoggedInUser
            List<Account> userAccounts = ServletUtil.getAccountsForLoggedInUser(request, response, loggedInUser, LOGGER);

            // If fetching accounts failed or no accounts, userAccounts will be empty.
            if (userAccounts.isEmpty()) {
                LOGGER.info("ScheduledTransfersListServlet: No accounts found for user " + loggedInUser.getUsername() + ", so no scheduled transfers.");
                // No need to set error message if accounts are just empty, not an error state.
            } else {
                List<Long> userAccountIds = userAccounts.stream().map(Account::getId).collect(Collectors.toList());

                // **** RECOMMENDED APPROACH (if ScheduledTransferService is updated) ****
                // if (scheduledTransferService instanceof lk.banking.transaction.ScheduledTransferServiceImpl) { // Check if it's the actual implementation (if you add a custom method)
                //      scheduledTransfers = ((lk.banking.transaction.ScheduledTransferServiceImpl) scheduledTransferService).getScheduledTransfersByCustomerId(loggedInUser.getCustomerId());
                // } else {
                //      LOGGER.warning("ScheduledTransfersListServlet: ScheduledTransferService does not have getScheduledTransfersByCustomerId. Falling back to less efficient filter.");
                //      List<ScheduledTransfer> allTransfers = scheduledTransferService.getAllScheduledTransfers(); // This method exists now
                //      scheduledTransfers = allTransfers.stream()
                //              .filter(st -> userAccountIds.contains(st.getFromAccount().getId()) || userAccountIds.contains(st.getToAccount().getId()))
                //              .collect(Collectors.toList());
                // }

                // **** CURRENT IMPLEMENTATION (using getAllScheduledTransfers() and filtering) ****
                // Assumes getAllScheduledTransfers() is modified to JOIN FETCH accounts as discussed previously
                List<ScheduledTransfer> allTransfers = scheduledTransferService.getAllScheduledTransfers();
                scheduledTransfers = allTransfers.stream()
                        .filter(st -> userAccountIds.contains(st.getFromAccount().getId()) || userAccountIds.contains(st.getToAccount().getId()))
                        .collect(Collectors.toList());
            }

            request.setAttribute("scheduledTransfers", scheduledTransfers);
            LOGGER.info("ScheduledTransfersListServlet: Found " + scheduledTransfers.size() + " scheduled transfers for user " + loggedInUser.getUsername());

            request.getRequestDispatcher("/WEB-INF/jsp/scheduled-transfers.jsp").forward(request, response);

        } catch (ServletException e) { // Catch ServletException from ServletUtil
            LOGGER.log(java.util.logging.Level.SEVERE, "ScheduledTransfersListServlet: Critical ServletException propagating from ServletUtil.", e);
            FlashMessageUtil.putErrorMessage(request.getSession(), "A critical service error occurred. Please try again later.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (EJBException e) {
            String displayErrorMessage = ServletUtil.getRootErrorMessage(e, "An error occurred while loading scheduled transfers. Please try again later.", LOGGER);
            FlashMessageUtil.putErrorMessage(request.getSession(), displayErrorMessage);
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "ScheduledTransfersListServlet: An unhandled general error occurred during scheduled transfers fetch for " + loggedInUser.getUsername(), e);
            FlashMessageUtil.putErrorMessage(request.getSession(), "An unexpected error occurred. Please try again later.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
        }
    }
}