package ict.servlet;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import ict.bean.BakeryShopBean;
import ict.bean.UserBean;
import ict.db.BakeryShopDB;
import ict.db.ReservationDB;
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
    private ReservationDB reservationDb;
    private BakeryShopDB bakeryShopDb;

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");
        if (dbUrl == null || dbUser == null) {
            throw new ServletException("Database connection parameters missing.");
        }
        reservationDb = new ReservationDB(dbUrl, dbUser, dbPassword);
        bakeryShopDb = new BakeryShopDB(dbUrl, dbUser, dbPassword);
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
        String filterType = request.getParameter("filterType");
        String filterValue = request.getParameter("filterValue");
        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");

        if (reportType == null || reportType.isEmpty())
            reportType = "needs";
        if (filterType == null || filterType.isEmpty())
            filterType = "none";
        if (filterValue == null)
            filterValue = "";

        Date startDate;
        Date endDate;
        try {
            endDate = (endDateStr != null && !endDateStr.isEmpty()) ? Date.valueOf(endDateStr)
                    : Date.valueOf(LocalDate.now());
            startDate = (startDateStr != null && !startDateStr.isEmpty()) ? Date.valueOf(startDateStr)
                    : Date.valueOf(endDate.toLocalDate().minusMonths(1));
            if (startDate.after(endDate))
                startDate = Date.valueOf(endDate.toLocalDate().minusMonths(1));
        } catch (IllegalArgumentException e) {
            endDate = Date.valueOf(LocalDate.now());
            startDate = Date.valueOf(endDate.toLocalDate().minusMonths(1));
            request.setAttribute("errorMessage", "Invalid date format. Using default range.");
        }

        Object reportData = null;
        String reportTitle = "Advanced Report";

        try {
            if ("needs".equals(reportType)) {
                reportTitle = "Aggregated Needs Report";
                reportData = reservationDb.getAggregatedNeeds(filterType, filterValue, startDate, endDate);
            } else if ("seasonalConsumption".equals(reportType)) {
                reportTitle = "Seasonal Consumption Report";
                reportData = reservationDb.getSeasonalConsumption(filterType, filterValue, startDate, endDate);
            } else {
                request.setAttribute("errorMessage", "Invalid report type selected.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching report data", e);
            request.setAttribute("errorMessage", "Error retrieving report data.");
        }

        List<BakeryShopBean> allShops = Collections.emptyList();
        List<String> allCities = Collections.emptyList();
        List<String> allCountries = Collections.emptyList();
        try {
            allShops = bakeryShopDb.getBakeryShop();
            if (allShops != null) {
                allCities = allShops.stream().map(BakeryShopBean::getCity).distinct().sorted()
                        .collect(Collectors.toList());
                allCountries = allShops.stream().map(BakeryShopBean::getCountry).distinct().sorted()
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching filter data (shops/cities/countries)", e);
            request.setAttribute("errorMessage", "Could not load filter options. DB Error: " + e.getMessage());
        }

        request.setAttribute("reportTitle", reportTitle);
        request.setAttribute("reportData", reportData);
        request.setAttribute("selectedReportType", reportType);
        request.setAttribute("selectedFilterType", filterType);
        request.setAttribute("selectedFilterValue", filterValue);
        request.setAttribute("selectedStartDate", startDate.toString());
        request.setAttribute("selectedEndDate", endDate.toString());
        request.setAttribute("allShops", allShops);
        request.setAttribute("allCities", allCities);
        request.setAttribute("allCountries", allCountries);

        RequestDispatcher rd = request.getRequestDispatcher("/advancedReport.jsp");
        rd.forward(request, response);
    }
}
