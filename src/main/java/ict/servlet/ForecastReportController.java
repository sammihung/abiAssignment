package ict.servlet;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.ForecastBean;
import ict.bean.UserBean;
import ict.db.BorrowingDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "ForecastReportController", urlPatterns = { "/viewForecastReport" })
public class ForecastReportController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ForecastReportController.class.getName());
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
        LOGGER.log(Level.INFO, "ForecastReportController initialized.");
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

        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");
        Date startDate;
        Date endDate;

        try {
            endDate = (endDateStr != null && !endDateStr.isEmpty()) ? Date.valueOf(endDateStr)
                    : Date.valueOf(LocalDate.now());
            startDate = (startDateStr != null && !startDateStr.isEmpty()) ? Date.valueOf(startDateStr)
                    : Date.valueOf(endDate.toLocalDate().minusDays(90));

            if (startDate.after(endDate)) {
                startDate = Date.valueOf(endDate.toLocalDate().minusDays(90));
                request.setAttribute("errorMessage", "Start date cannot be after end date. Defaulting start date.");
            }
            if (startDate.equals(endDate)) {
                startDate = Date.valueOf(endDate.toLocalDate().minusDays(1));
            }

        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid date format received for forecast.", e);
            request.setAttribute("errorMessage", "Invalid date format. Please use yyyy-MM-dd.");
            endDate = Date.valueOf(LocalDate.now());
            startDate = Date.valueOf(endDate.toLocalDate().minusDays(90));
        }

        List<ForecastBean> forecastData = Collections.emptyList();
        try {
            forecastData = borrowingDb.getAverageDailyConsumptionByFruitAndCountry(startDate, endDate);
            LOGGER.log(Level.INFO, "Fetched forecast report data for range: {0} to {1}",
                    new Object[] { startDate, endDate });
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching forecast report data.", e);
            request.setAttribute("errorMessage", "Error retrieving forecast data from database.");
        }

        request.setAttribute("forecastReportData", forecastData);
        request.setAttribute("selectedStartDate", startDate.toString());
        request.setAttribute("selectedEndDate", endDate.toString());

        RequestDispatcher rd = request.getRequestDispatcher("/forecastReport.jsp");
        rd.forward(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for viewing forecast reports based on consumption";
    }
}
