package ict.servlet;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import ict.bean.FruitBean;
import ict.bean.InventoryBean;
import ict.bean.UserBean;
import ict.db.BorrowingDB;
import ict.db.FruitDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "UpdateWarehouseInventoryController", urlPatterns = { "/updateWarehouseInventory" })
public class UpdateWarehouseInventoryController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(UpdateWarehouseInventoryController.class.getName());
    private BorrowingDB borrowingDb;
    private FruitDB fruitDb;

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");

        if (dbUrl == null || dbUser == null) {
            throw new ServletException("Database connection parameters missing.");
        }
        borrowingDb = new BorrowingDB(dbUrl, dbUser, dbPassword);
        fruitDb = new FruitDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null || !"Warehouse Staff".equalsIgnoreCase(currentUser.getRole()) ||
                currentUser.getWarehouseId() == null || currentUser.getWarehouseId().trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=WarehouseStaffLoginRequired");
            return;
        }

        try {
            int currentWarehouseId = Integer.parseInt(currentUser.getWarehouseId());

            List<InventoryBean> inventoryList = borrowingDb.getInventoryForWarehouse(currentWarehouseId);
            request.setAttribute("inventoryList", inventoryList);

            List<FruitBean> allFruits = fruitDb.getAllFruits();
            request.setAttribute("allFruits", allFruits);

            RequestDispatcher rd = request.getRequestDispatcher("/updateWarehouseInventory.jsp");
            rd.forward(request, response);

        } catch (NumberFormatException e) {
            request.setAttribute("errorMessage", "Invalid user profile (Warehouse ID). Cannot load inventory.");
            RequestDispatcher rd = request.getRequestDispatcher("/updateWarehouseInventory.jsp");
            rd.forward(request, response);
        } catch (Exception e) {
            request.setAttribute("errorMessage", "Error loading inventory management page.");
            RequestDispatcher rd = request.getRequestDispatcher("/updateWarehouseInventory.jsp");
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
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=SessionExpired");
            return;
        }

        String fruitIdStr = request.getParameter("fruitId");
        String newQuantityStr = request.getParameter("newQuantity");
        String message = "Warehouse inventory update failed.";
        boolean success = false;

        try {
            int fruitId = Integer.parseInt(fruitIdStr);
            int newQuantity = Integer.parseInt(newQuantityStr);
            int warehouseId = Integer.parseInt(currentUser.getWarehouseId());

            if (newQuantity < 0) {
                message = "Quantity cannot be negative.";
            } else {
                boolean updated = borrowingDb.setWarehouseInventoryQuantity(fruitId, warehouseId, newQuantity);
                if (updated) {
                    message = "Warehouse inventory updated successfully!";
                    success = true;
                } else {
                    message = "Failed to update warehouse inventory in database.";
                }
            }

        } catch (NumberFormatException e) {
            message = "Invalid input: Fruit ID and New Quantity must be numbers.";
        } catch (Exception e) {
            message = "An unexpected error occurred while updating inventory.";
        }

        String redirectUrl = "updateWarehouseInventory";
        if (success) {
            redirectUrl += "?message=" + java.net.URLEncoder.encode(message, "UTF-8");
        } else {
            redirectUrl += "?error=" + java.net.URLEncoder.encode(message, "UTF-8");
        }
        response.sendRedirect(redirectUrl);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for updating warehouse fruit inventory";
    }
}
