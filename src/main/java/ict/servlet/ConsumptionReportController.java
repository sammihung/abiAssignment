package ict.servlet;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate; // Using BorrowingDB as per user's file
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.ConsumptionDataBean;
import ict.bean.UserBean;
import ict.db.BorrowingDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Servlet for Senior Management to view consumption reports.
 */
@WebServlet(name = "ConsumptionReportController", urlPatterns = { "/viewConsumptionReport" })
public class ConsumptionReportController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ConsumptionReportController.class.getName());
    private BorrowingDB borrowingDb; // Changed from ReservationDB to BorrowingDB

    @Override
    public void init() throws ServletException {
        // Initialize DB connection
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");

        if (dbUrl == null || dbUser == null /* || dbPassword == null */) {
            LOGGER.log(Level.SEVERE, "Database connection parameters missing in web.xml.");
            throw new ServletException("Database connection parameters missing.");
        }
        // Initialize BorrowingDB
        borrowingDb = new BorrowingDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "ConsumptionReportController initialized.");
    }

    /**
     * Handles GET requests: Displays the consumption report page.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check ---
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null || !"Senior Management".equalsIgnoreCase(currentUser.getRole())) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to GET /viewConsumptionReport.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ManagementLoginRequired");
            return;
        }

        // --- Date Range Handling (Example: Default last 30 days) ---
        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");
        Date startDate;
        Date endDate;

        try {
            // Default end date: today
            endDate = (endDateStr != null && !endDateStr.isEmpty()) ? Date.valueOf(endDateStr)
                    : Date.valueOf(LocalDate.now());
            // Default start date: 30 days before end date
            startDate = (startDateStr != null && !startDateStr.isEmpty()) ? Date.valueOf(startDateStr)
                    : Date.valueOf(endDate.toLocalDate().minusDays(30));

            // Ensure start date is not after end date
            if (startDate.after(endDate)) {
                startDate = Date.valueOf(endDate.toLocalDate().minusDays(30)); // Reset start date
                request.setAttribute("errorMessage", "Start date cannot be after end date. Defaulting start date.");
            }

        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid date format received.", e);
            request.setAttribute("errorMessage", "Invalid date format. Please use yyyy-MM-dd.");
            // Default dates on error
            endDate = Date.valueOf(LocalDate.now());
            startDate = Date.valueOf(endDate.toLocalDate().minusDays(30));
        }

        // --- Fetch Report Data ---
        List<ConsumptionDataBean> reportData = Collections.emptyList();
        try {
            // Call method on BorrowingDB instance
            reportData = borrowingDb.getConsumptionSummaryByFruit(startDate, endDate);
            LOGGER.log(Level.INFO, "Fetched consumption report data for range: {0} to {1}",
                    new Object[] { startDate, endDate });
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching consumption report data.", e);
            request.setAttribute("errorMessage", "Error retrieving report data from database.");
        }

        // --- Set Attributes and Forward ---
        request.setAttribute("consumptionReportData", reportData);
        request.setAttribute("selectedStartDate", startDate.toString()); // Pass back selected dates
        request.setAttribute("selectedEndDate", endDate.toString());

        RequestDispatcher rd = request.getRequestDispatcher("/consumptionReport.jsp");
        rd.forward(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for viewing consumption reports";
    }
}
