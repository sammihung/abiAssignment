package ict.servlet;

import ict.bean.UserBean;
import ict.bean.WarehouseBean;
import ict.db.FruitDB; // Needed if fetching fruit details
import ict.db.ReservationDB;
import ict.db.WarehouseDB;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet for Warehouse Staff to arrange deliveries based on approved needs.
 */
@WebServlet(name = "ArrangeDeliveryController", urlPatterns = {"/arrangeDelivery"})
public class ArrangeDeliveryController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ArrangeDeliveryController.class.getName());
    private ReservationDB reservationDb;
    private WarehouseDB warehouseDb;
    private FruitDB fruitDb; // Optional, for displaying fruit names

    @Override
    public void init() throws ServletException {
        // Initialize DB connections
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");

        if (dbUrl == null || dbUser == null /*|| dbPassword == null*/) {
            LOGGER.log(Level.SEVERE, "Database connection parameters missing in web.xml.");
            throw new ServletException("Database connection parameters missing.");
        }
        reservationDb = new ReservationDB(dbUrl, dbUser, dbPassword);
        warehouseDb = new WarehouseDB(dbUrl, dbUser, dbPassword);
        fruitDb = new FruitDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "ArrangeDeliveryController initialized.");
    }

    /**
     * Handles GET requests: Displays approved needs ready for delivery arrangement.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check ---
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

            // Fetch approved needs grouped by fruit and target country, originating from this warehouse
            List<ReservationDB.DeliveryNeedBean> deliveryNeeds = reservationDb.getApprovedNeedsGroupedByFruitAndCountry(currentWarehouseId);

            request.setAttribute("deliveryNeedsList", deliveryNeeds);
             LOGGER.log(Level.INFO, "Fetched {0} delivery needs groups for WarehouseID={1}.", new Object[]{deliveryNeeds.size(), currentWarehouseId});


            // Forward to the JSP page
            RequestDispatcher rd = request.getRequestDispatcher("/arrangeDelivery.jsp");
            rd.forward(request, response);

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid Warehouse ID format for current user: " + currentUser.getWarehouseId(), e);
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

    /**
     * Handles POST requests: Processes the delivery arrangement action.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check ---
        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null || !"Warehouse Staff".equalsIgnoreCase(currentUser.getRole()) ||
            currentUser.getWarehouseId() == null || currentUser.getWarehouseId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Unauthorized POST attempt to /arrangeDelivery.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=SessionExpired");
            return;
        }

        // --- Get parameters ---
        String fruitIdStr = request.getParameter("fruitId");
        String targetCountry = request.getParameter("targetCountry");
        String message = "Delivery arrangement failed.";
        boolean success = false;

        try {
            int fruitId = Integer.parseInt(fruitIdStr);
            int fromWarehouseId = Integer.parseInt(currentUser.getWarehouseId());

            LOGGER.log(Level.INFO, "Processing delivery arrangement: FruitID={0}, FromWarehouseID={1}, TargetCountry={2}",
                       new Object[]{fruitId, fromWarehouseId, targetCountry});

            if (targetCountry == null || targetCountry.trim().isEmpty()) {
                 message = "Target country is missing.";
                 LOGGER.log(Level.WARNING, "Target country parameter missing in arrange delivery request.");
            } else {
                 // --- Call the core transaction logic ---
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

        // --- Redirect back to the GET handler to refresh the page ---
        String redirectUrl = "arrangeDelivery"; // Relative URL to self (doGet)
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
