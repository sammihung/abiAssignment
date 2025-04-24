package ict.servlet;

import ict.bean.UserBean;
// Import necessary beans for report data
import ict.bean.AggregatedNeedBean;
import ict.db.ReservationDB.SeasonalConsumptionBean; // Assuming inner class
import ict.db.BorrowingDB; // Assuming report methods are here

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
// Removed Date/LocalDate imports as filters are removed
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet for Senior Management advanced reports (Needs & Consumption).
 * Simplified version without filters.
 */
@WebServlet(name = "AdvancedReportController", urlPatterns = {"/viewAdvancedReport"})
public class AdvancedReportController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdvancedReportController.class.getName());
    private BorrowingDB borrowingDb; // Contains report methods

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");
        if (dbUrl == null || dbUser == null) {
            throw new ServletException("Database connection parameters missing.");
        }
        borrowingDb = new BorrowingDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "AdvancedReportController initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check ---
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;
        if (currentUser == null || !"Senior Management".equalsIgnoreCase(currentUser.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ManagementLoginRequired");
            return;
        }

        // --- Get Report Type Parameter ---
        String reportType = request.getParameter("reportType");
        if (reportType == null || reportType.isEmpty()) {
            reportType = "needs"; // Default report
        }

        // --- Fetch Data Based on Report Type ---
        Object reportData = null;
        String reportTitle = "Advanced Report";

        try {
            if ("needs".equals(reportType)) {
                reportTitle = "Aggregated Needs Report (All Pending/Approved)";
                // *** UPDATED: Call the new method without filters ***
                reportData = borrowingDb.getAllAggregatedNeeds();
                LOGGER.log(Level.INFO, "Fetching all aggregated needs.");

            } else if ("seasonalConsumption".equals(reportType)) {
                reportTitle = "Seasonal Consumption Report (All Time)";
                 // *** UPDATED: Call the new method without filters ***
                reportData = borrowingDb.getAllSeasonalConsumption();
                 LOGGER.log(Level.INFO, "Fetching all seasonal consumption.");

            } else {
                 request.setAttribute("errorMessage", "Invalid report type selected.");
            }
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error fetching report data", e);
             request.setAttribute("errorMessage", "Error retrieving report data.");
        }

        // --- Set Attributes and Forward ---
        request.setAttribute("reportTitle", reportTitle);
        request.setAttribute("reportData", reportData);
        request.setAttribute("selectedReportType", reportType);

        RequestDispatcher rd = request.getRequestDispatcher("/advancedReport.jsp");
        rd.forward(request, response);
    }
}
