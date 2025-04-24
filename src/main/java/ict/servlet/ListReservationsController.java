package ict.servlet;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.ReservationBean;
import ict.bean.UserBean;
import ict.db.ReservationDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "ListReservationsController", urlPatterns = { "/listReservations" })
public class ListReservationsController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ListReservationsController.class.getName());
    private ReservationDB reservationDb;

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");

        if (dbUrl == null || dbUser == null) {
            LOGGER.log(Level.SEVERE, "Database connection parameters missing in web.xml.");
            throw new ServletException("Database connection parameters missing.");
        }
        reservationDb = new ReservationDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "ListReservationsController initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null || currentUser.getShopId() == null || currentUser.getShopId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to GET /listReservations.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ShopLoginRequired");
            return;
        }

        try {
            int currentShopId = Integer.parseInt(currentUser.getShopId());

            List<ReservationBean> reservations = reservationDb.getReservationsForShop(currentShopId);

            request.setAttribute("reservationList", reservations);
            LOGGER.log(Level.INFO, "Fetched {0} reservations for ShopID={1}. Forwarding to listReservations.jsp",
                    new Object[] { reservations.size(), currentShopId });

            RequestDispatcher rd = request.getRequestDispatcher("/listReservations.jsp");
            rd.forward(request, response);

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid Shop ID format for current user: " + currentUser.getShopId(), e);
            request.setAttribute("errorMessage", "Invalid user profile (Shop ID). Cannot display reservations.");
            RequestDispatcher rd = request.getRequestDispatcher("/listReservations.jsp");
            rd.forward(request, response);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching reservations.", e);
            request.setAttribute("errorMessage", "An error occurred while retrieving reservations.");
            RequestDispatcher rd = request.getRequestDispatcher("/listReservations.jsp");
            rd.forward(request, response);
        }
    }

    @Override
    public String getServletInfo() {
        return "Servlet for listing reservation records for a shop";
    }
}
