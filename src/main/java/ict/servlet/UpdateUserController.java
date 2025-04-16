package ict.servlet;

import ict.bean.UserBean;
import ict.db.UserDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession; // Import HttpSession

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


@WebServlet(name = "UpdateUserController", urlPatterns = {"/updateUser"})
public class UpdateUserController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(UpdateUserController.class.getName());
    private UserDB db;

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");
        if (dbUrl == null || dbUser == null || dbPassword == null) {
             LOGGER.log(Level.SEVERE, "Database connection parameters missing in web.xml");
            throw new ServletException("Database connection parameters missing.");
        }
        db = new UserDB(dbUrl, dbUser, dbPassword);
        LOGGER.log(Level.INFO, "UpdateUserController initialized.");

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false); // Don't create session if it doesn't exist
        if (session == null || session.getAttribute("userInfo") == null) {
             LOGGER.log(Level.WARNING, "User not logged in. Redirecting to login page.");
             request.setAttribute("errorMessage", "Please login to update users."); // Set error message
             response.sendRedirect(request.getContextPath() + "/"); // Redirect to login
            return;
        }
         // Optional: Check if the logged-in user has permission to update others

        String userIdStr = request.getParameter("userId");
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "User ID parameter is missing for GET request.");
            request.setAttribute("errorMessage", "User ID is required to edit.");
            // Redirect back to the list or show an error page
             response.sendRedirect("listUsers?error=MissingUserId");
            return;
        }

        try {
            int userId = Integer.parseInt(userIdStr.trim());
            UserBean userToEdit = db.getUserById(userId);

            if (userToEdit != null) {
                 LOGGER.log(Level.INFO, "Fetching user data for editing, ID: {0}", userId);
                request.setAttribute("userToEdit", userToEdit);
                // Optional: Load lists of available shops/warehouses if needed for dropdowns
                // request.setAttribute("shops", shopDb.getAllShops());
                // request.setAttribute("warehouses", warehouseDb.getAllWarehouses());
                RequestDispatcher rd = request.getRequestDispatcher("updateUser.jsp");
                rd.forward(request, response);
            } else {
                LOGGER.log(Level.WARNING, "User not found for editing, ID: {0}", userId);
                request.setAttribute("errorMessage", "User with ID " + userId + " not found.");
                 // Redirect back to the list
                 response.sendRedirect("listUsers?error=UserNotFound");
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid User ID format in GET request: {0}", userIdStr);
            request.setAttribute("errorMessage", "Invalid User ID format.");
             response.sendRedirect("listUsers?error=InvalidUserIdFormat");
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Error fetching user for editing, ID: " + userIdStr, e);
            request.setAttribute("errorMessage", "An error occurred while fetching user data.");
             // Forward to an error page or back to the list
             RequestDispatcher rd = request.getRequestDispatcher("listUsers.jsp"); // Or an error page
             request.setAttribute("error", "Error fetching user data."); // Use request attribute for error message
             rd.forward(request, response);
        }
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

         HttpSession session = request.getSession(false);
         if (session == null || session.getAttribute("userInfo") == null) {
             LOGGER.log(Level.WARNING, "User not logged in during POST. Redirecting to login page.");
             request.setAttribute("errorMessage", "Your session has expired. Please login again.");
             response.sendRedirect(request.getContextPath() + "/"); // Redirect to login
             return;
         }
          // Optional: Permission check

        // Retrieve form data
        String userIdStr = request.getParameter("userId");
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password"); // Get password (might be empty)
        String role = request.getParameter("role");
        String shopId = request.getParameter("shopId");
        String warehouseId = request.getParameter("warehouseId");

         LOGGER.log(Level.INFO, "Received update request for User ID: {0}, Username: {1}, Email: {2}, Role: {3}, ShopID: {4}, WarehouseID: {5}",
                   new Object[]{userIdStr, username, email, role, shopId, warehouseId});


        int userId = -1;
        try {
             if (userIdStr == null || userIdStr.trim().isEmpty()) {
                  throw new ServletException("User ID is missing in the update request.");
             }
            userId = Integer.parseInt(userIdStr.trim());
        } catch (NumberFormatException e) {
             LOGGER.log(Level.SEVERE, "Invalid User ID format in POST request: {0}", userIdStr);
             request.setAttribute("errorMessage", "Invalid user ID provided.");
             // Consider re-showing the form with an error
             // For simplicity, redirecting back to list
             response.sendRedirect("listUsers?error=InvalidUpdateUserId");
             return;
        }


        // Call the database update method
        // Pass the password field. If it's empty/null, the DB logic should ignore it.
        boolean success = db.updateUserInfo(userId, username, password, email, role, shopId, warehouseId);

        if (success) {
            LOGGER.log(Level.INFO, "User updated successfully, ID: {0}. Redirecting to list.", userId);
            // Redirect back to the user list page with a success message
            response.sendRedirect("listUsers?message=User+updated+successfully");
        } else {
             LOGGER.log(Level.WARNING, "Failed to update user, ID: {0}. Forwarding back to edit form.", userId);
            // If update fails, forward back to the edit page with an error message
            request.setAttribute("errorMessage", "Failed to update user. Please check the data and try again.");
             // Re-fetch user data to pre-populate the form again
            UserBean userToEdit = db.getUserById(userId); // Fetch again in case some data is needed
            if (userToEdit == null) {
                 // Handle case where user might have been deleted in the meantime
                 response.sendRedirect("listUsers?error=UserNotFoundAfterUpdateFail");
                 return;
             }
             // Set the potentially modified (but failed to save) data back into the request
             // so the form shows what the user entered.
             userToEdit.setUsername(username);
             userToEdit.setEmail(email);
             userToEdit.setRole(role);
             userToEdit.setShopId(shopId);
             userToEdit.setWarehouseId(warehouseId);
             // Don't put password back into the request scope for security
             request.setAttribute("userToEdit", userToEdit);


            RequestDispatcher rd = request.getRequestDispatcher("updateUser.jsp");
            rd.forward(request, response);
        }
    }
}