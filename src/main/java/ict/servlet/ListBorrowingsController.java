package ict.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.BorrowingBean;
import ict.bean.UserBean;
import ict.db.BorrowingDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "ListBorrowingsController", urlPatterns = { "/listBorrowings" })
public class ListBorrowingsController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ListBorrowingsController.class.getName());
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
        LOGGER.log(Level.INFO, "ListBorrowingsController initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("userInfo") : null;
        String listTitle = "Borrowing Records";
        List<BorrowingBean> borrowingList = Collections.emptyList();

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=LoginRequired");
            return;
        }

        String userRole = currentUser.getRole();

        try {
            if ("Senior Management".equalsIgnoreCase(userRole)) {
                listTitle = "All Borrowing Records";
                borrowingList = borrowingDb.getAllBorrowings();
            } else if ("Bakery shop staff".equalsIgnoreCase(userRole) && currentUser.getShopId() != null) {
                listTitle = "My Shop's Borrowing Records";
                int shopId = Integer.parseInt(currentUser.getShopId());
                borrowingList = borrowingDb.getAllBorrowingsForShop(shopId);
            } else {
                LOGGER.log(Level.INFO, "User role ({0}) does not have access to borrowing list.", userRole);
                request.setAttribute("errorMessage", "Your role does not have access to view borrowing records.");
            }

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Invalid Shop ID format for user: " + currentUser.getUsername(), e);
            request.setAttribute("errorMessage", "Invalid user profile (Shop ID).");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching borrowing records.", e);
            request.setAttribute("errorMessage", "Error retrieving borrowing data.");
        }

        request.setAttribute("borrowingList", borrowingList);
        request.setAttribute("listTitle", listTitle);
        RequestDispatcher rd = request.getRequestDispatcher("/listBorrowings.jsp");
        rd.forward(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Servlet for listing borrowing records";
    }
}
