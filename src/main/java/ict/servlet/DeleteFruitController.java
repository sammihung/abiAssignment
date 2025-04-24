package ict.servlet;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.db.FruitDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "DeleteFruitController", urlPatterns = { "/deleteFruit" })
public class DeleteFruitController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DeleteFruitController.class.getName());
    private FruitDB fruitDb;

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");

        if (dbUrl == null || dbUser == null) {
            LOGGER.log(Level.SEVERE, "Database connection parameters missing in web.xml for DeleteFruitController.");
            throw new ServletException("Database connection parameters missing.");
        }
        fruitDb = new FruitDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "DeleteFruitController initialized.");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userInfo") == null) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to POST /deleteFruit. Redirecting to login.");
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=SessionExpired");
            return;
        }

        String fruitIdStr = request.getParameter("fruitId");
        String redirectUrl = "listFruits";

        if (fruitIdStr == null || fruitIdStr.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Fruit ID parameter is missing for POST request.");
            redirectUrl += "?error=MissingDeleteFruitId";
        } else {
            try {
                int fruitId = Integer.parseInt(fruitIdStr.trim());
                LOGGER.log(Level.INFO, "Attempting to delete fruit with ID: {0}", fruitId);

                boolean success = fruitDb.deleteFruit(fruitId);

                if (success) {
                    LOGGER.log(Level.INFO, "Fruit deleted successfully, ID: {0}.", fruitId);
                    redirectUrl += "?message=Fruit+deleted+successfully";
                } else {
                    LOGGER.log(Level.WARNING, "Failed to delete fruit, ID: {0}.", fruitId);
                    redirectUrl += "?error=FailedToDeleteFruit";
                }
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "Invalid Fruit ID format in POST request: {0}", fruitIdStr);
                redirectUrl += "?error=InvalidDeleteFruitIdFormat";
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error deleting fruit, ID: " + fruitIdStr, e);
                redirectUrl += "?error=DeleteError";
            }
        }

        response.sendRedirect(redirectUrl);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LOGGER.log(Level.INFO, "GET request received for DeleteFruitController. Redirecting to listFruits.");
        response.sendRedirect("listFruits");
    }

    @Override
    public String getServletInfo() {
        return "Servlet for deleting a fruit";
    }
}
