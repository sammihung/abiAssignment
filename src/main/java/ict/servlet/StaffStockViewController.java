package ict.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.BakeryShopBean;
import ict.bean.InventoryBean;
import ict.bean.UserBean;
import ict.bean.WarehouseBean;
import ict.db.BakeryShopDB;
import ict.db.BorrowingDB;
import ict.db.WarehouseDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "StaffStockViewController", urlPatterns = { "/viewStaffStock" })
public class StaffStockViewController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(StaffStockViewController.class.getName());
    private BorrowingDB borrowingDb;
    private BakeryShopDB bakeryShopDb;
    private WarehouseDB warehouseDb;

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");
        if (dbUrl == null || dbUser == null) {
            throw new ServletException("Database connection parameters missing.");
        }
        borrowingDb = new BorrowingDB(dbUrl, dbUser, dbPassword);
        bakeryShopDb = new BakeryShopDB(dbUrl, dbUser, dbPassword);
        warehouseDb = new WarehouseDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "StaffStockViewController initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;

        if (currentUser == null ||
                (!"Bakery shop staff".equalsIgnoreCase(currentUser.getRole())
                        && !"Warehouse Staff".equalsIgnoreCase(currentUser.getRole()))) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=StaffLoginRequired");
            return;
        }

        String userRole = currentUser.getRole();
        List<InventoryBean> ownStock = Collections.emptyList();
        List<InventoryBean> otherShopStock = Collections.emptyList();
        List<InventoryBean> sourceWarehouseStock = Collections.emptyList();
        List<InventoryBean> centralWarehouseStock = Collections.emptyList();

        try {
            if ("Bakery shop staff".equalsIgnoreCase(userRole)) {
                int shopId = Integer.parseInt(currentUser.getShopId());
                BakeryShopBean shop = bakeryShopDb.getShopById(shopId);
                if (shop != null) {
                    ownStock = borrowingDb.getInventoryForShop(shopId);
                    otherShopStock = borrowingDb.getInventoryForOtherShopsInCity(shop.getCity(), shopId);
                    centralWarehouseStock = borrowingDb.getInventoryForCentralWarehouses(shop.getCountry());
                    request.setAttribute("locationName", shop.getShop_name() + " (Shop)");
                    request.setAttribute("locationCity", shop.getCity());
                    request.setAttribute("locationCountry", shop.getCountry());
                } else {
                    request.setAttribute("errorMessage", "Could not find your shop details.");
                }

            } else if ("Warehouse Staff".equalsIgnoreCase(userRole)) {
                int warehouseId = Integer.parseInt(currentUser.getWarehouseId());
                WarehouseBean warehouse = warehouseDb.getWarehouseById(warehouseId);
                if (warehouse != null) {
                    ownStock = borrowingDb.getInventoryForWarehouse(warehouseId);
                    sourceWarehouseStock = borrowingDb.getInventoryForSourceWarehouses(warehouse.getCountry());
                    centralWarehouseStock = borrowingDb.getInventoryForCentralWarehouses(warehouse.getCountry());
                    request.setAttribute("locationName", warehouse.getWarehouse_name() + " (Warehouse)");
                    request.setAttribute("locationCity", warehouse.getCity());
                    request.setAttribute("locationCountry", warehouse.getCountry());
                } else {
                    request.setAttribute("errorMessage", "Could not find your warehouse details.");
                }
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid ID format for user: " + currentUser.getUsername(), e);
            request.setAttribute("errorMessage", "Invalid user profile ID.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching stock view data.", e);
            request.setAttribute("errorMessage", "Error retrieving stock data.");
        }

        request.setAttribute("ownStock", ownStock);
        request.setAttribute("otherShopStock", otherShopStock);
        request.setAttribute("sourceWarehouseStock", sourceWarehouseStock);
        request.setAttribute("centralWarehouseStock", centralWarehouseStock);

        RequestDispatcher rd = request.getRequestDispatcher("/viewStaffStock.jsp");
        rd.forward(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for staff to view relevant stock levels";
    }
}
