package ict.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level; // Import logging classes
import java.util.logging.Logger; // Import logging classes

import ict.bean.UserBean;

public class UserDB {

    private static final Logger LOGGER = Logger.getLogger(UserDB.class.getName()); // Logger instance
    String dburl, username, password;

    public String getUrl() {
        return dburl;
    }

    public void setUrl(String url) {
        this.dburl = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserDB(String dburl, String dbUser, String dbPassword) {
        this.dburl = dburl;
        this.username = dbUser;
        this.password = dbPassword;
    }

    public Connection getConnection() throws SQLException, IOException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "MySQL JDBC Driver not found", e);
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
        return DriverManager.getConnection(dburl, username, password);
    }

    public boolean isValidUser(String user, String pwd) throws SQLException, IOException {
        boolean isValid = false;
        String sql = "SELECT * FROM USERS WHERE username=? and password=?"; // Reminder: Use hashed passwords

        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            c = getConnection();
            ps = c.prepareStatement(sql);
            ps.setString(1, user);
            ps.setString(2, pwd); // Compare hashed passwords in a real app
            rs = ps.executeQuery();
            if (rs.next()) {
                isValid = true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error validating user: " + user, e);
            throw e; // Re-throw the exception after logging
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close ResultSet", e);
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close PreparedStatement", e);
            }
            try {
                if (c != null)
                    c.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close Connection", e);
            }
        }
        return isValid;
    }

    public boolean addUserInfo(String id, String user, String pwd) {
        Connection c = null;
        PreparedStatement ps = null;
        boolean isSuccess = false;
        // Note: This method seems less used compared to the more detailed addUser.
        // Consider if it's still needed or should be merged/removed.
        // Assuming 'id' here maps to 'user_id' and it's an integer.
        try {
            c = getConnection();
            // Assuming 'id' in USERS table is user_id and is auto-increment or handled
            // elsewhere
            // If 'id' param is meant to be the user_id, it should likely be int.
            // If user_id is auto-increment, don't insert it.
            // Let's assume user_id is auto-increment for this example.
            String sql = "INSERT INTO USERS (username, password) VALUES (?, ?)";
            ps = c.prepareStatement(sql);
            // ps.setString(1, id); // Removed assuming auto-increment or string id is not
            // user_id
            ps.setString(1, user);
            ps.setString(2, pwd); // HASH a real password
            int row = ps.executeUpdate();
            if (row >= 1) {
                isSuccess = true;
                LOGGER.log(Level.INFO, "User info added successfully for user: {0}", user);
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error adding user info for user: " + user, e);
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close PreparedStatement", e);
            }
            try {
                if (c != null)
                    c.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close Connection", e);
            }
        }
        return isSuccess;
    }

    public boolean deleteUserInfo(int userId) {
        Connection c = null;
        PreparedStatement ps = null;
        boolean isSuccess = false;
        try {
            c = getConnection();
            String sql = "DELETE FROM USERS WHERE user_id=?";
            ps = c.prepareStatement(sql);
            ps.setInt(1, userId);
            LOGGER.log(Level.INFO, "Attempting to delete user with ID: {0}", userId);
            int row = ps.executeUpdate();
            if (row >= 1) {
                isSuccess = true;
                LOGGER.log(Level.INFO, "User deleted successfully with ID: {0}", userId);
            } else {
                LOGGER.log(Level.WARNING, "User deletion failed. No user found with ID: {0}", userId);
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error deleting user with ID: " + userId, e);
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close PreparedStatement", e);
            }
            try {
                if (c != null)
                    c.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close Connection", e);
            }
        }
        return isSuccess;
    }

    /**
     * Updates user information in the database.
     *
     * @param userId      The ID of the user to update.
     * @param username    The new username.
     * @param password    The new password (should be hashed). Pass null or empty
     *                    string to keep existing password.
     * @param email       The new email.
     * @param role        The new role.
     * @param shopId      The new shop ID (can be null or empty).
     * @param warehouseId The new warehouse ID (can be null or empty).
     * @return true if the update was successful, false otherwise.
     */
    public boolean updateUserInfo(int userId, String username, String password, String email, String role,
            String shopId, String warehouseId) {
        Connection c = null;
        PreparedStatement ps = null;
        boolean isSuccess = false;

        // Basic validation
        if (username == null || username.trim().isEmpty() ||
                email == null || email.trim().isEmpty() ||
                role == null || role.trim().isEmpty()) {
            LOGGER.log(Level.WARNING,
                    "Update failed for user ID {0}: Required fields (username, email, role) cannot be empty.", userId);
            return false;
        }

        StringBuilder sqlBuilder = new StringBuilder(
                "UPDATE USERS SET username=?, userEmail=?, role=?, shop_id=?, warehouse_id=?");
        boolean updatePassword = (password != null && !password.trim().isEmpty());
        if (updatePassword) {
            sqlBuilder.append(", password=?"); // Add password update only if provided
        }
        sqlBuilder.append(" WHERE user_id=?");
        String sql = sqlBuilder.toString();

        try {
            c = getConnection();
            ps = c.prepareStatement(sql);
            int paramIndex = 1;
            ps.setString(paramIndex++, username);
            ps.setString(paramIndex++, email);
            ps.setString(paramIndex++, role);

            // Handle nullable foreign keys (shop_id, warehouse_id)
            if (shopId != null && !shopId.trim().isEmpty()) {
                try {
                    ps.setInt(paramIndex++, Integer.parseInt(shopId.trim()));
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "Invalid Shop ID format for user ID {0}: {1}. Setting to NULL.",
                            new Object[] { userId, shopId });
                    ps.setNull(paramIndex++, java.sql.Types.INTEGER);
                }
            } else {
                ps.setNull(paramIndex++, java.sql.Types.INTEGER);
            }

            if (warehouseId != null && !warehouseId.trim().isEmpty()) {
                try {
                    ps.setInt(paramIndex++, Integer.parseInt(warehouseId.trim()));
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "Invalid Warehouse ID format for user ID {0}: {1}. Setting to NULL.",
                            new Object[] { userId, warehouseId });
                    ps.setNull(paramIndex++, java.sql.Types.INTEGER);
                }
            } else {
                ps.setNull(paramIndex++, java.sql.Types.INTEGER);
            }

            // Conditionally set password
            if (updatePassword) {
                // **SECURITY:** HASH THE PASSWORD HERE before setting it
                // Example: ps.setString(paramIndex++, hashPassword(password.trim()));
                ps.setString(paramIndex++, password.trim()); // Storing plain text - BAD PRACTICE
                LOGGER.log(Level.INFO, "Updating password for user ID: {0}", userId);
            }

            ps.setInt(paramIndex++, userId); // Set the user ID for the WHERE clause

            LOGGER.log(Level.INFO, "Executing user update for ID: {0}", userId);
            int row = ps.executeUpdate();
            if (row >= 1) {
                isSuccess = true;
                LOGGER.log(Level.INFO, "User updated successfully: ID={0}", userId);
            } else {
                LOGGER.log(Level.WARNING, "User update failed: No rows affected for ID={0}", userId);
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error updating user with ID=" + userId, e);
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close PreparedStatement", e);
            }
            try {
                if (c != null)
                    c.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close Connection", e);
            }
        }
        return isSuccess;
    }

    /**
     * Retrieves a single user by their ID.
     * 
     * @param userId The ID of the user to retrieve.
     * @return A UserBean object if found, otherwise null.
     */
    public UserBean getUserById(int userId) {
        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        UserBean user = null;
        String sql = "SELECT * FROM USERS WHERE user_id=?";

        try {
            c = getConnection();
            ps = c.prepareStatement(sql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();

            if (rs.next()) {
                user = new UserBean();
                user.setUserId(rs.getString("user_id")); // Set the user ID
                user.setUsername(rs.getString("username"));
                // Avoid loading the password unless absolutely necessary for specific features.
                // user.setPassword(rs.getString("password"));
                user.setEmail(rs.getString("userEmail"));
                user.setRole(rs.getString("role"));
                // getObject helps handle potential NULLs directly
                Object shopIdObj = rs.getObject("shop_id");
                user.setShopId(shopIdObj == null ? null : shopIdObj.toString());

                Object warehouseIdObj = rs.getObject("warehouse_id");
                user.setWarehouseId(warehouseIdObj == null ? null : warehouseIdObj.toString());

                LOGGER.log(Level.INFO, "User found with ID: {0}", userId);

            } else {
                LOGGER.log(Level.WARNING, "No user found with ID: {0}", userId);
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching user with ID: " + userId, e);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close ResultSet", e);
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close PreparedStatement", e);
            }
            try {
                if (c != null)
                    c.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close Connection", e);
            }
        }
        return user;
    }

    public UserBean getUser(String username, String password) {
        String sql = "SELECT * FROM USERS WHERE username=? AND password=?"; // Use hashed passwords
        UserBean user = null;
        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            c = getConnection();
            ps = c.prepareStatement(sql);
            ps.setString(1, username);
            // In a real app, hash the input password and compare hashes
            ps.setString(2, password);
            rs = ps.executeQuery();

            if (rs.next()) {
                user = new UserBean();
                user.setUserId(rs.getString("user_id")); // Set user ID
                user.setUsername(rs.getString("username"));
                // Don't set password in the bean retrieved for session state
                user.setEmail(rs.getString("userEmail"));
                user.setRole(rs.getString("role"));
                Object shopIdObj = rs.getObject("shop_id");
                user.setShopId(shopIdObj == null ? null : shopIdObj.toString());
                Object warehouseIdObj = rs.getObject("warehouse_id");
                user.setWarehouseId(warehouseIdObj == null ? null : warehouseIdObj.toString());
                LOGGER.log(Level.INFO, "User authenticated successfully: {0}", username);
            } else {
                LOGGER.log(Level.WARNING, "Authentication failed for user: {0}", username);
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving user: " + username, e);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close ResultSet", e);
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close PreparedStatement", e);
            }
            try {
                if (c != null)
                    c.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close Connection", e);
            }
        }
        return user;
    }

    public boolean addUser(String username, String password, String email, String role, String shopId,
            String warehouseId) {
        Connection c = null;
        PreparedStatement ps = null;
        boolean isSuccess = false;
        try {
            c = getConnection();
            String sql = "INSERT INTO USERS (username, password, userEmail, role, shop_id, warehouse_id) VALUES (?, ?, ?, ?, ?, ?)";
            ps = c.prepareStatement(sql);
            ps.setString(1, username);
            // **SECURITY:** Hash the password BEFORE storing it
            // Example: ps.setString(2, hashPassword(password));
            ps.setString(2, password); // Storing plain text - BAD PRACTICE
            ps.setString(3, email);
            ps.setString(4, role);

            if (shopId != null && !shopId.trim().isEmpty()) {
                try {
                    ps.setInt(5, Integer.parseInt(shopId.trim()));
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "Invalid Shop ID format during add user {0}: {1}. Setting to NULL.",
                            new Object[] { username, shopId });
                    ps.setNull(5, java.sql.Types.INTEGER);
                }
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }

            if (warehouseId != null && !warehouseId.trim().isEmpty()) {
                try {
                    ps.setInt(6, Integer.parseInt(warehouseId.trim()));
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "Invalid Warehouse ID format during add user {0}: {1}. Setting to NULL.",
                            new Object[] { username, warehouseId });
                    ps.setNull(6, java.sql.Types.INTEGER);
                }
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }

            int row = ps.executeUpdate();
            if (row >= 1) {
                isSuccess = true;
                LOGGER.log(Level.INFO, "User added successfully: {0}", username);
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error adding user: " + username, e);
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close PreparedStatement", e);
            }
            try {
                if (c != null)
                    c.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close Connection", e);
            }
        }
        return isSuccess;
    }

    public List<Map<String, Object>> getUsersByRoleAsMap(String role) throws SQLException, IOException {
        List<Map<String, Object>> users = new ArrayList<>();
        // Select password hash only if needed, avoid selecting plain text password
        String sql = "SELECT user_id, username, userEmail, role, shop_id, warehouse_id FROM USERS WHERE role = ?";
        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            c = getConnection();
            ps = c.prepareStatement(sql);
            ps.setString(1, role);
            rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("userid", rs.getString("user_id")); // Use getString for consistency if ID is treated as string
                                                             // elsewhere
                user.put("username", rs.getString("username"));
                user.put("email", rs.getString("userEmail"));
                user.put("role", rs.getString("role"));
                user.put("shopId", rs.getObject("shop_id")); // getObject handles nulls
                user.put("warehouseId", rs.getObject("warehouse_id")); // getObject handles nulls
                users.add(user);
            }
            LOGGER.log(Level.INFO, "Fetched {0} users for role: {1}", new Object[] { users.size(), role });
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching users by role: " + role, e);
            throw e; // Re-throw after logging
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close ResultSet", e);
            }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close PreparedStatement", e);
            }
            try {
                if (c != null)
                    c.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close Connection", e);
            }
        }
        return users;
    }
}