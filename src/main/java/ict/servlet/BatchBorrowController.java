package ict.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.BakeryShopBean;
import ict.bean.BorrowableFruitInfoBean;
import ict.bean.UserBean;
import ict.db.BakeryShopDB;
import ict.db.BorrowingDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "BatchBorrowController", urlPatterns = { "/batchBorrowFruit" })
public class BatchBorrowController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(BatchBorrowController.class.getName());
    private BorrowingDB borrowingDb;
    private BakeryShopDB bakeryShopDb;

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
        LOGGER.log(Level.INFO, "BatchBorrowController initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;
        if (currentUser == null || !"Bakery shop staff".equalsIgnoreCase(currentUser.getRole()) ||
                currentUser.getShopId() == null || currentUser.getShopId().trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ShopStaffLoginRequired");
            return;
        }

        List<BorrowableFruitInfoBean> borrowableFruits = Collections.emptyList();
        List<BakeryShopBean> potentialLenders = Collections.emptyList();

        try {
            int currentShopId = Integer.parseInt(currentUser.getShopId());
            BakeryShopBean currentShop = bakeryShopDb.getShopById(currentShopId);

            if (currentShop != null && currentShop.getCity() != null) {
                String city = currentShop.getCity();
                borrowableFruits = borrowingDb.getBorrowableFruitsWithLenderInfo(city, currentShopId);
                potentialLenders = borrowingDb.getOtherShopsInCity(city, currentShopId);

                request.setAttribute("currentCity", city);
                LOGGER.log(Level.INFO, "Fetched {0} borrowable fruits with lender info for city {1}",
                        new Object[] { borrowableFruits.size(), city });
            } else {
                LOGGER.log(Level.WARNING, "Could not determine city for current shop ID: {0}", currentShopId);
                request.setAttribute("errorMessage", "Could not determine your shop's city.");
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid Shop ID format for user: " + currentUser.getUsername(), e);
            request.setAttribute("errorMessage", "Invalid user profile (Shop ID).");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error preparing batch borrow form.", e);
            request.setAttribute("errorMessage", "Error loading borrowing page.");
        }

        request.setAttribute("borrowableFruits", borrowableFruits);
        request.setAttribute("potentialLenders", potentialLenders);
        RequestDispatcher rd = request.getRequestDispatcher("/batchBorrowFruit.jsp");
        rd.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;
        if (currentUser == null || !"Bakery shop staff".equalsIgnoreCase(currentUser.getRole()) ||
                currentUser.getShopId() == null || currentUser.getShopId().trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=SessionExpired");
            return;
        }

        String message = "Borrow request submission failed.";
        boolean success = false;
        List<Integer> requestedFruitIds = new ArrayList<>();
        List<Integer> requestedQuantities = new ArrayList<>();
        String lendingShopIdStr = request.getParameter("lendingShopId");

        try {
            int borrowingShopId = Integer.parseInt(currentUser.getShopId());
            int lendingShopId = -1;

            if (lendingShopIdStr == null || lendingShopIdStr.trim().isEmpty()) {
                message = "Please select a shop to borrow from.";
            } else {
                lendingShopId = Integer.parseInt(lendingShopIdStr);

                Map<String, String[]> parameterMap = request.getParameterMap();
                for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                    String paramName = entry.getKey();
                    if (paramName.startsWith("quantity_")) {
                        try {
                            String quantityStr = entry.getValue()[0];
                            if (quantityStr != null && !quantityStr.trim().isEmpty()) {
                                int quantity = Integer.parseInt(quantityStr.trim());
                                if (quantity > 0) {
                                    int fruitId = Integer.parseInt(paramName.substring("quantity_".length()));
                                    requestedFruitIds.add(fruitId);
                                    requestedQuantities.add(quantity);
                                }
                            }
                        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                            LOGGER.log(Level.WARNING, "Invalid quantity/fruit ID format for param: " + paramName, e);
                        }
                    }
                }

                if (requestedFruitIds.isEmpty()) {
                    message = "No items requested (quantity must be greater than 0).";
                } else {
                    LOGGER.log(Level.INFO,
                            "Processing batch borrow request from ShopID={0} to ShopID={1} for {2} items.",
                            new Object[] { borrowingShopId, lendingShopId, requestedFruitIds.size() });
                    message = borrowingDb.createMultipleBorrowRequests(borrowingShopId, lendingShopId,
                            requestedFruitIds, requestedQuantities);
                    if (message.toLowerCase().contains("success")) {
                        success = true;
                    }
                }
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid number format received for Shop ID.", e);
            message = "Invalid Shop ID provided.";
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing batch borrow submission.", e);
            message = "An unexpected error occurred.";
        }

        String redirectUrl = "batchBorrowFruit";
        if (success) {
            redirectUrl += "?message=" + java.net.URLEncoder.encode(message, "UTF-8");
        } else {
            redirectUrl += "?error=" + java.net.URLEncoder.encode(message, "UTF-8");
        }
        response.sendRedirect(redirectUrl);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for batch borrowing fruits from other shops";
    }
}
