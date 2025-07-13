package lk.banking.web.servlet;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.banking.core.dto.DailyReportDto;
import lk.banking.core.dto.LoggedInUser;
import lk.banking.core.entity.enums.UserRole;
import lk.banking.core.exception.BankingException;
import lk.banking.timer.DailyReportService; // Import service interface
import lk.banking.web.util.FlashMessageUtil;
import lk.banking.web.util.ServletUtil;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.logging.Logger;

/**
 * Servlet for displaying daily banking reports (for Admin only).
 * This version only displays the report on a JSP, without PDF download functionality.
 */
@WebServlet("/reports/daily")
public class DailyReportServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DailyReportServlet.class.getName());

    @EJB
    private DailyReportService dailyReportService; // Inject the service

    /**
     * Handles GET requests to display the daily report.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.info("DailyReportServlet: Handling GET request.");

        LoggedInUser loggedInUser = (LoggedInUser) request.getSession().getAttribute("loggedInUser");
        // Security check: Only ADMIN can access this page
        if (loggedInUser == null || !loggedInUser.hasRole(UserRole.ADMIN)) {
            LOGGER.warning("DailyReportServlet: Unauthorized access attempt by user: " + (loggedInUser != null ? loggedInUser.getUsername() : "N/A"));
            FlashMessageUtil.putErrorMessage(request.getSession(), "Access denied. Only administrators can view daily reports.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        FlashMessageUtil.retrieveAndClearMessages(request); // Retrieve flash messages

        String dateStr = request.getParameter("reportDate"); // Date from form or URL parameter
        LocalDate reportDate = null;
        String errorMessage = null;

        if (dateStr != null && !dateStr.trim().isEmpty()) {
            try {
                reportDate = LocalDate.parse(dateStr);
            } catch (DateTimeParseException e) {
                errorMessage = "Invalid date format. Please use YYYY-MM-DD.";
                LOGGER.warning("DailyReportServlet: Invalid date parameter: " + dateStr);
            }
        } else {
            // Default to yesterday's report if no date is specified
            reportDate = LocalDate.now().minusDays(1);
        }

        if (errorMessage != null) {
            request.setAttribute("errorMessage", errorMessage);
            request.setAttribute("reportDate", reportDate != null ? reportDate.toString() : ""); // Pre-fill date if possible
            request.getRequestDispatcher("/WEB-INF/jsp/daily-report.jsp").forward(request, response);
            return;
        }

        try {
            // Get the report data from the EJB service
            DailyReportDto report = dailyReportService.generateReportForDate(reportDate);

            request.setAttribute("report", report);
            request.setAttribute("reportDate", reportDate.toString()); // Ensure date is passed back to JSP
            LOGGER.info("DailyReportServlet: Displaying report for date: " + reportDate);
            request.getRequestDispatcher("/WEB-INF/jsp/daily-report.jsp").forward(request, response);

        } catch (EJBException e) {
            String displayErrorMessage = ServletUtil.getRootErrorMessage(e, "Error fetching report data. Please try again later.", LOGGER);
            FlashMessageUtil.putErrorMessage(request.getSession(), displayErrorMessage);
            LOGGER.log(java.util.logging.Level.SEVERE, "DailyReportServlet: EJBException during report fetch for " + loggedInUser.getUsername(), e);
            response.sendRedirect(request.getContextPath() + "/dashboard"); // Redirect on error
        } catch (Exception e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "DailyReportServlet: An unhandled general error occurred during report generation for " + loggedInUser.getUsername(), e);
            FlashMessageUtil.putErrorMessage(request.getSession(), "An unexpected error occurred. Please try again later.");
            response.sendRedirect(request.getContextPath() + "/dashboard");
        }
    }
    // Removed generatePdfReport method
}