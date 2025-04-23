package ict.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.List; // Assuming summary methods are added here
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.InventorySummaryBean;
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
 * Servlet for Senior Management to view aggregated inventory reports.
 * Handles different grouping options.
 */
@WebServlet(name = "InventoryReportController", urlPatterns = { "/viewInventoryReport" })
public class InventoryReportController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(InventoryReportController.class.getName());
    private BorrowingDB borrowingDb; // Contains inventory summary methods

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");
        // Ensure parameters are present
        if (dbUrl == null || dbUser == null /* || dbPassword == null */) { // Allow empty password
            throw new ServletException("Database connection parameters missing in web.xml.");
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
        // Ensure user is logged in and is Senior Management
        if (currentUser == null || !"Senior Management".equalsIgnoreCase(currentUser.getRole())) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to GET /viewInventoryReport.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ManagementLoginRequired");
            return;
        }

        // --- Get Grouping Parameter ---
        String groupBy = request.getParameter("groupBy");
        // Default grouping if parameter is missing or invalid
        if (groupBy == null || !List.of("sourceCountry", "shop", "city", "country").contains(groupBy)) {
            groupBy = "sourceCountry"; // Default to source country
        }

        List<InventorySummaryBean> reportData = Collections.emptyList();
        String reportTitle = "Inventory Report";
        String groupByLabel = "Group"; // Default label for table header

        try {
            // --- Call appropriate DB method based on groupBy parameter ---
            switch (groupBy) {
                case "sourceCountry":
                    reportTitle = "Inventory Summary by Fruit Source Country";
                    groupByLabel = "Source Country";
                    // Assumes getInventorySummaryBySourceCountry exists in borrowingDb
                    reportData = borrowingDb.getInventorySummaryBySourceCountry();
                    break;
                case "shop":
                    reportTitle = "Inventory Summary by Shop";
                    groupByLabel = "Shop Name";
                    // Assumes getInventorySummaryByShop exists in borrowingDb
                    reportData = borrowingDb.getInventorySummaryByShop();
                    break;
                case "city":
                    reportTitle = "Inventory Summary by City";
                    groupByLabel = "City";
                    // Assumes getInventorySummaryByCity exists in borrowingDb
                    reportData = borrowingDb.getInventorySummaryByCity();
                    break;
                case "country":
                    reportTitle = "Inventory Summary by Location Country";
                    groupByLabel = "Country";
                    // Assumes getInventorySummaryByCountry exists in borrowingDb
                    reportData = borrowingDb.getInventorySummaryByCountry();
                    break;
                // No default needed due to initial check, but could add one for safety
            }
            LOGGER.log(Level.INFO, "Generating inventory report grouped by: {0}", groupBy);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching inventory report data for groupBy=" + groupBy, e);
            request.setAttribute("errorMessage", "Error retrieving report data.");
        }

        // --- Set Attributes and Forward ---
        request.setAttribute("reportTitle", reportTitle);
        request.setAttribute("groupByDimension", groupByLabel); // Pass label for table header
        request.setAttribute("inventoryReportData", reportData);
        request.setAttribute("selectedGroupBy", groupBy); // Pass back selected grouping for the dropdown

        RequestDispatcher rd = request.getRequestDispatcher("/inventoryReport.jsp");
        rd.forward(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for viewing aggregated inventory reports";
    }
}
