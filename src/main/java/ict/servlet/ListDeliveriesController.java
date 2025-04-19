package ict.servlet;

import ict.bean.DeliveryBean;
import ict.bean.UserBean;
import ict.db.DeliveryDB; // Use the new DB class

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
 * Servlet to list delivery records based on user role.
 */
@WebServlet(name = "ListDeliveriesController", urlPatterns = {"/listDeliveries"})
public class ListDeliveriesController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ListDeliveriesController.class.getName());
    private DeliveryDB deliveryDb;

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");
        if (dbUrl == null || dbUser == null) {
            throw new ServletException("Database connection parameters missing.");
        }
        deliveryDb = new DeliveryDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "ListDeliveriesController initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;
        String listTitle = "Delivery Records";
        List<DeliveryBean> deliveryList = Collections.emptyList();

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=LoginRequired");
            return;
        }

        String userRole = currentUser.getRole();

        try {
            if ("Senior Management".equalsIgnoreCase(userRole)) {
                listTitle = "All Delivery Records";
                deliveryList = deliveryDb.getAllDeliveries(); // Fetch all
            } else if ("Warehouse Staff".equalsIgnoreCase(userRole) && currentUser.getWarehouseId() != null) {
                 listTitle = "My Warehouse Delivery Records";
                 int warehouseId = Integer.parseInt(currentUser.getWarehouseId());
                 deliveryList = deliveryDb.getDeliveriesForWarehouse(warehouseId); // Fetch for specific warehouse
            }
            // Shop staff currently don't see warehouse delivery records
            else {
                 LOGGER.log(Level.INFO, "User role ({0}) does not have access to delivery list.", userRole);
                 request.setAttribute("errorMessage", "Your role does not have access to view delivery records.");
            }

        } catch (NumberFormatException e) {
             LOGGER.log(Level.SEVERE, "Invalid Warehouse ID format for user: " + currentUser.getUsername(), e);
             request.setAttribute("errorMessage", "Invalid user profile (Warehouse ID).");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching delivery records.", e);
            request.setAttribute("errorMessage", "Error retrieving delivery data.");
        }

        request.setAttribute("deliveryList", deliveryList);
        request.setAttribute("listTitle", listTitle);
        RequestDispatcher rd = request.getRequestDispatcher("/listDeliveries.jsp");
        rd.forward(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for listing delivery records";
    }
}
