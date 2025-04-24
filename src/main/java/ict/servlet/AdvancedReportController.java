package ict.servlet;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.UserBean;
import ict.db.BorrowingDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "AdvancedReportController", urlPatterns = { "/viewAdvancedReport" })
public class AdvancedReportController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdvancedReportController.class.getName());
    private BorrowingDB borrowingDb;

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

        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;
        if (currentUser == null || !"Senior Management".equalsIgnoreCase(currentUser.getRole())) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ManagementLoginRequired");
            return;
        }

        String reportType = request.getParameter("reportType");
        if (reportType == null || reportType.isEmpty()) {
            reportType = "needs";
        }
        Object reportData = null;
        String reportTitle = "Advanced Report";

        try {
            if ("needs".equals(reportType)) {
                reportTitle = "Aggregated Needs Report (All Pending/Approved)";
                reportData = borrowingDb.getAllAggregatedNeeds();
                LOGGER.log(Level.INFO, "Fetching all aggregated needs.");

            } else if ("seasonalConsumption".equals(reportType)) {
                reportTitle = "Seasonal Consumption Report (All Time)";
                reportData = borrowingDb.getAllSeasonalConsumption();
                LOGGER.log(Level.INFO, "Fetching all seasonal consumption.");

            } else {
                request.setAttribute("errorMessage", "Invalid report type selected.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching report data", e);
            request.setAttribute("errorMessage", "Error retrieving report data.");
        }

        request.setAttribute("reportTitle", reportTitle);
        request.setAttribute("reportData", reportData);
        request.setAttribute("selectedReportType", reportType);

        RequestDispatcher rd = request.getRequestDispatcher("/advancedReport.jsp");
        rd.forward(request, response);
    }
}
