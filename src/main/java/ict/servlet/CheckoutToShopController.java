package ict.servlet;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.ReservationBean;
import ict.bean.UserBean;
import ict.bean.WarehouseBean;
import ict.db.BorrowingDB;
import ict.db.WarehouseDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "CheckoutToShopController", urlPatterns = { "/checkoutToShop" })
public class CheckoutToShopController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(CheckoutToShopController.class.getName());
    private BorrowingDB borrowingDb;
    private WarehouseDB warehouseDb;

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");

        if (dbUrl == null || dbUser == null) {
            LOGGER.log(Level.SEVERE, "Database connection parameters missing in web.xml.");
            throw new ServletException("Database connection parameters missing.");
        }
        borrowingDb = new BorrowingDB(dbUrl, dbUser, dbPassword);
        warehouseDb = new WarehouseDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "CheckoutToShopController initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;
        String errorMessage = null;
        List<ReservationBean> fulfillableList = null;

        if (currentUser == null || !"Warehouse Staff".equalsIgnoreCase(currentUser.getRole()) ||
                currentUser.getWarehouseId() == null || currentUser.getWarehouseId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to GET /checkoutToShop.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=WarehouseStaffLoginRequired");
            return;
        }

        try {
            int currentWarehouseId = Integer.parseInt(currentUser.getWarehouseId());

            WarehouseBean currentWarehouse = warehouseDb.getWarehouseById(currentWarehouseId);
            boolean isCentral = (currentWarehouse != null
                    && ("0".equals(currentWarehouse.getIs_source()) || currentWarehouse.getIs_source() == null));

            if (!isCentral) {
                LOGGER.log(Level.WARNING,
                        "Access denied to /checkoutToShop. User's Warehouse (ID={0}) is a source warehouse.",
                        currentWarehouseId);
                errorMessage = "Access denied. This function is for staff at central/distribution warehouses only.";
            } else {
                fulfillableList = borrowingDb.getFulfillableReservationsForWarehouse(currentWarehouseId);
                LOGGER.log(Level.INFO, "Fetched {0} fulfillable reservations for Central WarehouseID={1}.",
                        new Object[] { (fulfillableList != null ? fulfillableList.size() : 0), currentWarehouseId });
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid Warehouse ID format for current user: " + currentUser.getWarehouseId(),
                    e);
            errorMessage = "Invalid user profile (Warehouse ID).";
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching fulfillable reservations.", e);
            errorMessage = "An error occurred while retrieving data.";
        }

        request.setAttribute("fulfillableList", fulfillableList);
        if (errorMessage != null) {
            request.setAttribute("errorMessage", errorMessage);
        }
        RequestDispatcher rd = request.getRequestDispatcher("/checkoutToShop.jsp");
        rd.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null || !"Warehouse Staff".equalsIgnoreCase(currentUser.getRole()) ||
                currentUser.getWarehouseId() == null || currentUser.getWarehouseId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Unauthorized POST attempt to /checkoutToShop.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=SessionExpired");
            return;
        }

        String reservationIdStr = request.getParameter("reservationId");
        String message = "Checkout failed.";
        boolean success = false;

        try {
            int reservationId = Integer.parseInt(reservationIdStr);
            int centralWarehouseId = Integer.parseInt(currentUser.getWarehouseId());

            WarehouseBean currentWarehouse = warehouseDb.getWarehouseById(centralWarehouseId);
            boolean isCentral = (currentWarehouse != null
                    && ("0".equals(currentWarehouse.getIs_source()) || currentWarehouse.getIs_source() == null));
            if (!isCentral) {
                message = "Action not allowed from a source warehouse.";
                LOGGER.log(Level.WARNING,
                        "POST denied to /checkoutToShop. User's Warehouse (ID={0}) is a source warehouse.",
                        centralWarehouseId);
            } else {
                LOGGER.log(Level.INFO, "Processing checkout for ReservationID={0} from Central WarehouseID={1}",
                        new Object[] { reservationId, centralWarehouseId });

                message = borrowingDb.checkoutDeliveryToShop(reservationId, centralWarehouseId);
                if (message.toLowerCase().contains("success")) {
                    success = true;
                }
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid number format received for reservationId.", e);
            message = "Invalid input: Reservation ID must be a number.";
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing checkout.", e);
            message = "An unexpected error occurred during checkout.";
        }

        String redirectUrl = "checkoutToShop";
        if (success) {
            redirectUrl += "?message=" + java.net.URLEncoder.encode(message, "UTF-8");
        } else {
            redirectUrl += "?error=" + java.net.URLEncoder.encode(message, "UTF-8");
        }
        response.sendRedirect(redirectUrl);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for Warehouse Staff to checkout deliveries to shops";
    }
}
