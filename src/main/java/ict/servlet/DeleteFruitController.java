package ict.servlet;

import ict.bean.UserBean; // Assuming you use UserBean for session info
import ict.db.FruitDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet to handle deleting a fruit.
 * Processes POST requests containing the fruit ID to be deleted.
 */
@WebServlet(name = "DeleteFruitController", urlPatterns = {"/deleteFruit"})
public class DeleteFruitController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DeleteFruitController.class.getName());
    private FruitDB fruitDb; // Use the FruitDB class

    @Override
    public void init() throws ServletException {
        // Initialize FruitDB using context parameters from web.xml
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");

        if (dbUrl == null || dbUser == null /*|| dbPassword == null*/) {
            LOGGER.log(Level.SEVERE, "Database connection parameters missing in web.xml for DeleteFruitController.");
            throw new ServletException("Database connection parameters missing.");
        }
        fruitDb = new FruitDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "DeleteFruitController initialized.");
    }

    /**
     * Handles POST requests to delete a fruit.
     * Expects a 'fruitId' parameter.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // --- Security Check: Ensure user is logged in ---
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userInfo") == null) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to POST /deleteFruit. Redirecting to login.");
            // Redirect to login, maybe with an error message if desired
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=SessionExpired");
            return;
        }
        // Optional: Add role-based access control here

        // --- Get Fruit ID from request parameter ---
        String fruitIdStr = request.getParameter("fruitId"); // Expecting this from the form submission
        String redirectUrl = "listFruits"; // Default redirect target

        if (fruitIdStr == null || fruitIdStr.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Fruit ID parameter is missing for POST request.");
            redirectUrl += "?error=MissingDeleteFruitId"; // Add error param
        } else {
            try {
                int fruitId = Integer.parseInt(fruitIdStr.trim());
                LOGGER.log(Level.INFO, "Attempting to delete fruit with ID: {0}", fruitId);

                // --- Call the database delete method ---
                boolean success = fruitDb.deleteFruit(fruitId);

                if (success) {
                    LOGGER.log(Level.INFO, "Fruit deleted successfully, ID: {0}.", fruitId);
                    redirectUrl += "?message=Fruit+deleted+successfully"; // Add success param
                } else {
                    LOGGER.log(Level.WARNING, "Failed to delete fruit, ID: {0}.", fruitId);
                    // Could be because it didn't exist or due to constraints/DB error
                    redirectUrl += "?error=FailedToDeleteFruit"; // Add error param
                }
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "Invalid Fruit ID format in POST request: {0}", fruitIdStr);
                redirectUrl += "?error=InvalidDeleteFruitIdFormat"; // Add error param
            } catch (Exception e) {
                // Catch other potential exceptions during DB access
                LOGGER.log(Level.SEVERE, "Error deleting fruit, ID: " + fruitIdStr, e);
                redirectUrl += "?error=DeleteError"; // Add generic error param
            }
        }

        // --- Redirect back to the list page ---
        response.sendRedirect(redirectUrl);
    }

    /**
     * GET requests are not typically used for deletion actions. Redirect to list.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Redirect GET requests to the list page, as deletion should happen via POST
        LOGGER.log(Level.INFO, "GET request received for DeleteFruitController. Redirecting to listFruits.");
        response.sendRedirect("listFruits");
    }


    @Override
    public String getServletInfo() {
        return "Servlet for deleting a fruit";
    }
}
