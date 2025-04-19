package ict.servlet;

import ict.bean.InventorySummaryBean;
import ict.bean.UserBean;
import ict.db.BorrowingDB; // Assuming summary methods are added here

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet for Senior Management to view aggregated inventory reports.
 */
@WebServlet(name = "InventoryReportController", urlPatterns = {"/viewInventoryReport"})
public class InventoryReportController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(InventoryReportController.class.getName());
    private BorrowingDB borrowingDb; // Contains inventory summary methods

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");
        if (dbUrl == null || dbUser == null) {
            throw new ServletException("Database connection parameters missing.");
        }
        borrowingDb = new BorrowingDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "InventoryReportController initialized.");
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

        // --- Get Report Type Parameter (Example: default to 'bySourceCountry') ---
        String reportType = request.getParameter("type");
        if (reportType == null || reportType.isEmpty()) {
            reportType = "bySourceCountry"; // Default report
        }

        List<InventorySummaryBean> reportData = Collections.emptyList();
        String reportTitle = "Inventory Report";
        String groupBy = "Source Country"; // Default group by label

        try {
            if ("bySourceCountry".equals(reportType)) {
                reportTitle = "Inventory Summary by Source Country";
                groupBy = "Source Country";
                reportData = borrowingDb.getInventorySummaryBySourceCountry();
            }
            // Add else if blocks here for other report types (e.g., byCity, byWarehouse)
            // else if ("byShopCity".equals(reportType)) { ... call getInventorySummaryByShopCity() ... }
            else {
                 request.setAttribute("errorMessage", "Invalid report type specified.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching inventory report data", e);
            request.setAttribute("errorMessage", "Error retrieving report data.");
        }

        // --- Set Attributes and Forward ---
        request.setAttribute("reportTitle", reportTitle);
        request.setAttribute("groupByDimension", groupBy); // Tell JSP what the first column represents
        request.setAttribute("inventoryReportData", reportData);
        request.setAttribute("selectedReportType", reportType);

        RequestDispatcher rd = request.getRequestDispatcher("/inventoryReport.jsp");
        rd.forward(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for viewing aggregated inventory reports";
    }
}
