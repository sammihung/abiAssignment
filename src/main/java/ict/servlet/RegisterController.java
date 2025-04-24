package ict.servlet;

import java.io.IOException;

import ict.db.UserDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "RegisterController", urlPatterns = { "/register" })
public class RegisterController extends HttpServlet {

    private UserDB db;

    public void init() {
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        db = new UserDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String email = request.getParameter("email");
        String shopId = request.getParameter("shopId");
        String warehouseId = request.getParameter("warehouseId");

        String role = "Unknown";
        if (shopId != null && !shopId.isEmpty()) {
            role = "Bakery shop staff";
        } else if (warehouseId != null && !warehouseId.isEmpty()) {
            role = "Warehouse Staff";
        }

        boolean isRegistered = db.addUser(username, password, email, role, shopId, warehouseId);

        if (isRegistered) {
            response.sendRedirect("welcome.jsp?message=Registration successful");
        } else {
            request.setAttribute("error", "Registration failed. Please try again.");
            request.getRequestDispatcher("register.jsp").forward(request, response);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("register.jsp");
    }
}
