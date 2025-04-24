package ict.servlet;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.UserBean;
import ict.db.FruitDB;
import ict.db.ReservationDB;
import ict.db.WarehouseDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "ArrangeDeliveryController", urlPatterns = { "/arrangeDelivery" })
public class ArrangeDeliveryController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ArrangeDeliveryController.class.getName());
    private ReservationDB reservationDb;
    private WarehouseDB warehouseDb;
    private FruitDB fruitDb;

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
        warehouseDb = new WarehouseDB(dbUrl, dbUser, dbPassword);
        fruitDb = new FruitDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "ArrangeDeliveryController initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null || !"Warehouse Staff".equalsIgnoreCase(currentUser.getRole()) ||
                currentUser.getWarehouseId() == null || currentUser.getWarehouseId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to GET /arrangeDelivery.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=WarehouseStaffLoginRequired");
            return;
        }

        try {
            int currentWarehouseId = Integer.parseInt(currentUser.getWarehouseId());
            List<ReservationDB.DeliveryNeedBean> deliveryNeeds = reservationDb
                    .getApprovedNeedsGroupedByFruitAndCountry(currentWarehouseId);

            request.setAttribute("deliveryNeedsList", deliveryNeeds);
            LOGGER.log(Level.INFO, "Fetched {0} delivery needs groups for WarehouseID={1}.",
                    new Object[] { deliveryNeeds.size(), currentWarehouseId });

            RequestDispatcher rd = request.getRequestDispatcher("/arrangeDelivery.jsp");
            rd.forward(request, response);

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid Warehouse ID format for current user: " + currentUser.getWarehouseId(),
                    e);
            request.setAttribute("errorMessage", "Invalid user profile (Warehouse ID).");
            RequestDispatcher rd = request.getRequestDispatcher("/arrangeDelivery.jsp");
            rd.forward(request, response);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching delivery needs.", e);
            request.setAttribute("errorMessage", "An error occurred while retrieving delivery needs.");
            RequestDispatcher rd = request.getRequestDispatcher("/arrangeDelivery.jsp");
            rd.forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null || !"Warehouse Staff".equalsIgnoreCase(currentUser.getRole()) ||
                currentUser.getWarehouseId() == null || currentUser.getWarehouseId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Unauthorized POST attempt to /arrangeDelivery.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=SessionExpired");
            return;
        }

        String fruitIdStr = request.getParameter("fruitId");
        String targetCountry = request.getParameter("targetCountry");
        String message = "Delivery arrangement failed.";
        boolean success = false;

        try {
            int fruitId = Integer.parseInt(fruitIdStr);
            int fromWarehouseId = Integer.parseInt(currentUser.getWarehouseId());

            LOGGER.log(Level.INFO,
                    "Processing delivery arrangement: FruitID={0}, FromWarehouseID={1}, TargetCountry={2}",
                    new Object[] { fruitId, fromWarehouseId, targetCountry });

            if (targetCountry == null || targetCountry.trim().isEmpty()) {
                message = "Target country is missing.";
                LOGGER.log(Level.WARNING, "Target country parameter missing in arrange delivery request.");
            } else {
                message = reservationDb.arrangeDeliveryTransaction(fruitId, fromWarehouseId, targetCountry);
                if (message.toLowerCase().contains("success")) {
                    success = true;
                }
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid number format received for fruitId.", e);
            message = "Invalid input: Fruit ID must be a number.";
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing delivery arrangement.", e);
            message = "An unexpected error occurred during delivery arrangement.";
        }

        String redirectUrl = "arrangeDelivery";
        if (success) {
            redirectUrl += "?message=" + java.net.URLEncoder.encode(message, "UTF-8");
        } else {
            redirectUrl += "?error=" + java.net.URLEncoder.encode(message, "UTF-8");
        }
        response.sendRedirect(redirectUrl);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for Warehouse Staff to arrange deliveries";
    }
}
