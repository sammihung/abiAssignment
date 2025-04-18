package ict.servlet;

import ict.bean.UserBean;
// Updated imports
import ict.bean.BakeryShopBean;
import ict.bean.AggregatedNeedBean; // Assuming this exists from previous steps
import ict.db.ReservationDB; // Assuming report methods are here
// Updated imports
import ict.db.BakeryShopDB;
// import ict.db.WarehouseDB; // Not directly needed in this version

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet for Senior Management advanced reports (Needs & Consumption).
 */
@WebServlet(name = "AdvancedReportController", urlPatterns = {"/viewAdvancedReport"})
public class AdvancedReportController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdvancedReportController.class.getName());
    private ReservationDB reservationDb;
    // Updated field type
    private BakeryShopDB bakeryShopDb;

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");
        if (dbUrl == null || dbUser == null /*|| dbPassword == null*/) { // Allow empty password
            throw new ServletException("Database connection parameters missing.");
        }
        reservationDb = new ReservationDB(dbUrl, dbUser, dbPassword);
        // Updated initialization
        bakeryShopDb = new BakeryShopDB(dbUrl, dbUser, dbPassword);
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

        // --- Get Filter Parameters ---
        String reportType = request.getParameter("reportType"); // e.g., "needs", "seasonalConsumption"
        String filterType = request.getParameter("filterType"); // e.g., "shop", "city", "country", "none"
        String filterValue = request.getParameter("filterValue");
        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");

        // --- Set Defaults ---
        if (reportType == null || reportType.isEmpty()) reportType = "needs"; // Default report
        if (filterType == null || filterType.isEmpty()) filterType = "none";
        if (filterValue == null) filterValue = "";

        // --- Date Range Handling ---
        Date startDate;
        Date endDate;
        try {
            endDate = (endDateStr != null && !endDateStr.isEmpty()) ? Date.valueOf(endDateStr) : Date.valueOf(LocalDate.now());
            startDate = (startDateStr != null && !startDateStr.isEmpty()) ? Date.valueOf(startDateStr) : Date.valueOf(endDate.toLocalDate().minusMonths(1)); // Default last month
            if (startDate.after(endDate)) startDate = Date.valueOf(endDate.toLocalDate().minusMonths(1));
        } catch (IllegalArgumentException e) {
            endDate = Date.valueOf(LocalDate.now());
            startDate = Date.valueOf(endDate.toLocalDate().minusMonths(1));
            request.setAttribute("errorMessage", "Invalid date format. Using default range.");
        }

        // --- Fetch Data Based on Report Type ---
        Object reportData = null; // Use Object to hold different list types
        String reportTitle = "Advanced Report";

        try {
            if ("needs".equals(reportType)) {
                reportTitle = "Aggregated Needs Report";
                // Assuming getAggregatedNeeds is in reservationDb
                reportData = reservationDb.getAggregatedNeeds(filterType, filterValue, startDate, endDate);
            } else if ("seasonalConsumption".equals(reportType)) {
                reportTitle = "Seasonal Consumption Report";
                 // Assuming getSeasonalConsumption is in reservationDb
                reportData = reservationDb.getSeasonalConsumption(filterType, filterValue, startDate, endDate);
            } else {
                 request.setAttribute("errorMessage", "Invalid report type selected.");
            }
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error fetching report data", e);
             request.setAttribute("errorMessage", "Error retrieving report data.");
        }


        // --- Fetch Filter Dropdown Data ---
        // Updated variable type and method call
        List<BakeryShopBean> allShops = Collections.emptyList();
        List<String> allCities = Collections.emptyList();
        List<String> allCountries = Collections.emptyList();
        try {
             // Updated method call
             allShops = bakeryShopDb.getBakeryShop(); // Use the method from BakeryShopDB
             if (allShops != null) {
                 // Updated stream mapping to use BakeryShopBean getters
                 allCities = allShops.stream().map(BakeryShopBean::getCity).distinct().sorted().collect(Collectors.toList());
                 allCountries = allShops.stream().map(BakeryShopBean::getCountry).distinct().sorted().collect(Collectors.toList());
             }
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error fetching filter data (shops/cities/countries)", e);
             // Check if the exception is due to DB connection or the method call itself
             request.setAttribute("errorMessage", "Could not load filter options. DB Error: " + e.getMessage());
        }


        // --- Set Attributes and Forward ---
        request.setAttribute("reportTitle", reportTitle);
        request.setAttribute("reportData", reportData); // List of AggregatedNeedBean or SeasonalConsumptionBean
        request.setAttribute("selectedReportType", reportType);
        request.setAttribute("selectedFilterType", filterType);
        request.setAttribute("selectedFilterValue", filterValue);
        request.setAttribute("selectedStartDate", startDate.toString());
        request.setAttribute("selectedEndDate", endDate.toString());
        request.setAttribute("allShops", allShops); // Pass the list of BakeryShopBean
        request.setAttribute("allCities", allCities);
        request.setAttribute("allCountries", allCountries);


        RequestDispatcher rd = request.getRequestDispatcher("/advancedReport.jsp");
        rd.forward(request, response);
    }
}
