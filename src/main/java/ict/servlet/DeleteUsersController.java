package ict.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ict.db.UserDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "DeleteUsersController", urlPatterns = { "/deleteUsers" })
public class DeleteUsersController extends HttpServlet {

    private UserDB db;

    @Override
    public void init() {
        String dbUser = this.getServletContext().getInitParameter("dbUser");
        String dbPassword = this.getServletContext().getInitParameter("dbPassword");
        String dbUrl = this.getServletContext().getInitParameter("dbUrl");
        db = new UserDB(dbUrl, dbUser, dbPassword);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        try {
            // Parse the JSON request body manually
            StringBuilder jsonBuffer = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                jsonBuffer.append(line);
            }
            String json = jsonBuffer.toString();
            System.out.println("Received JSON: " + json); // 添加日誌

            // Extract user IDs from the JSON string
            List<Integer> userIds = parseUserIds(json);

            // Delete users from the database
            boolean allDeleted = true;
            for (int userId : userIds) {
                if (!db.deleteUserInfo(userId)) {
                    allDeleted = false;
                    break;
                }
            }

            // Construct JSON response manually
            String jsonResponse = constructJsonResponse(allDeleted,
                    allDeleted ? "Users deleted successfully." : "Failed to delete some users.");
            response.getWriter().write(jsonResponse);
        } catch (Exception e) {
            e.printStackTrace();
            String errorResponse = constructJsonResponse(false, "An error occurred.");
            response.getWriter().write(errorResponse);
        }
    }

    private List<Integer> parseUserIds(String json) {
        List<Integer> userIds = new ArrayList<>();
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            // Extract the "userIds" array from the JSON object
            int startIndex = json.indexOf("[");
            int endIndex = json.indexOf("]");
            if (startIndex != -1 && endIndex != -1) {
                String arrayContent = json.substring(startIndex + 1, endIndex); // Extract array content
                String[] ids = arrayContent.split(",");
                for (String id : ids) {
                    try {
                        userIds.add(Integer.parseInt(id.trim().replaceAll("^\"|\"$", ""))); // Remove quotes and parse
                                                                                            // as integer
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return userIds;
    }

    private String constructJsonResponse(boolean success, String message) {
        return "{ \"success\": " + success + ", \"message\": \"" + message + "\" }";
    }
}
