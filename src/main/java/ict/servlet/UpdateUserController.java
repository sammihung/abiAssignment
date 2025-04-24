package ict.servlet;

import java.io.IOException;
import java.util.logging.Logger;

import ict.bean.UserBean;
import ict.db.UserDB;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "UpdateUserController", urlPatterns = { "/updateUser" })
public class UpdateUserController extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(UpdateUserController.class.getName());
    private UserDB db;

    @Override
    public void init() throws ServletException {
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");
        String dbUrl = getServletContext().getInitParameter("dbUrl");
        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new ServletException("Database connection parameters missing.");
        }
        db = new UserDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userInfo") == null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        String userIdStr = request.getParameter("userId");
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            response.sendRedirect("listUsers?error=MissingUserId");
            return;
        }

        try {
            int userId = Integer.parseInt(userIdStr.trim());
            UserBean userToEdit = db.getUserById(userId);

            if (userToEdit != null) {
                request.setAttribute("userToEdit", userToEdit);
                RequestDispatcher rd = request.getRequestDispatcher("updateUser.jsp");
                rd.forward(request, response);
            } else {
                response.sendRedirect("listUsers?error=UserNotFound");
            }
        } catch (NumberFormatException e) {
            response.sendRedirect("listUsers?error=InvalidUserIdFormat");
        } catch (Exception e) {
            request.setAttribute("errorMessage", "An error occurred while fetching user data.");
            RequestDispatcher rd = request.getRequestDispatcher("listUsers.jsp");
            request.setAttribute("error", "Error fetching user data.");
            rd.forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userInfo") == null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        String userIdStr = request.getParameter("userId");
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String role = request.getParameter("role");
        String shopId = request.getParameter("shopId");
        String warehouseId = request.getParameter("warehouseId");

        int userId = -1;
        try {
            if (userIdStr == null || userIdStr.trim().isEmpty()) {
                throw new ServletException("User ID is missing in the update request.");
            }
            userId = Integer.parseInt(userIdStr.trim());
        } catch (NumberFormatException e) {
            response.sendRedirect("listUsers?error=InvalidUpdateUserId");
            return;
        }

        boolean success = db.updateUserInfo(userId, username, password, email, role, shopId, warehouseId);

        if (success) {
            response.sendRedirect("listUsers?message=User+updated+successfully");
        } else {
            request.setAttribute("errorMessage", "Failed to update user. Please check the data and try again.");
            UserBean userToEdit = db.getUserById(userId);
            if (userToEdit == null) {
                response.sendRedirect("listUsers?error=UserNotFoundAfterUpdateFail");
                return;
            }
            userToEdit.setUsername(username);
            userToEdit.setEmail(email);
            userToEdit.setRole(role);
            userToEdit.setShopId(shopId);
            userToEdit.setWarehouseId(warehouseId);
            request.setAttribute("userToEdit", userToEdit);

            RequestDispatcher rd = request.getRequestDispatcher("updateUser.jsp");
            rd.forward(request, response);
        }
    }
}