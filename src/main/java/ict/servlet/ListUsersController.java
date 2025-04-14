package ict.servlet;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import ict.bean.UserBean;
import ict.db.UserDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "ListUsersController", urlPatterns = { "/listUsers" })
public class ListUsersController extends HttpServlet {

    private UserDB db;

    public void init() {
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        db = new UserDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        UserBean loggedInUser = (UserBean) session.getAttribute("userInfo"); // 正確轉換為 UserBean

        if (loggedInUser == null) {
            response.sendRedirect("login.jsp?error=Please log in first.");
            return;
        }

        String loggedInUserRole = loggedInUser.getRole(); // 從 UserBean 中獲取角色

        try {
            List<Map<String, Object>> users = db.getUsersByRoleAsMap(loggedInUserRole);
            request.setAttribute("users", users); // 將數據存儲為 "users" 屬性
            request.getRequestDispatcher("listUsers.jsp").forward(request, response); // 轉發到 JSP
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Failed to retrieve users. Please try again.");
            request.getRequestDispatcher("listUsers.jsp").forward(request, response);
        }
    }
}
