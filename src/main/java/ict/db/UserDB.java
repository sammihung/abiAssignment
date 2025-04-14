package ict.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ict.bean.UserBean;

public class UserDB {

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
            e.printStackTrace();
        }
        return DriverManager.getConnection(dburl, username, password);
    }

    public boolean isValidUser(String user, String pwd) throws SQLException, IOException {
        boolean isValid = false;
        String sql = "SELECT * FROM USERS WHERE username=? and password=?";

        Connection c = getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, user);
        ps.setString(2, pwd);

        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                isValid = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return isValid;
    }

    public boolean addUserInfo(String id, String user, String pwd) {
        Connection c;
        PreparedStatement ps;
        boolean isSuccess = false;
        try {
            c = getConnection();
            String sql = "INSERT INTO USERS (id, username, password) VALUES (?, ?, ?)";
            ps = c.prepareStatement(sql);
            ps.setString(1, id);
            ps.setString(2, user);
            ps.setString(3, pwd);
            int row = ps.executeUpdate();
            if (row >= 1) {
                isSuccess = true;
            }
            ps.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isSuccess;
    }

    public boolean deleteUserInfo(String id) {
        Connection c;
        PreparedStatement ps;
        boolean isSuccess = false;
        try {
            c = getConnection();
            String sql = "DELETE FROM USERS WHERE id=?";
            ps = c.prepareStatement(sql);
            ps.setString(1, id);
            int row = ps.executeUpdate();
            if (row >= 1) {
                isSuccess = true;
            }
            ps.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isSuccess;
    }

    public boolean updateUserInfo(String id, String user, String pwd) {
        Connection c;
        PreparedStatement ps;
        boolean isSuccess = false;
        try {
            c = getConnection();
            String sql = "UPDATE USERS SET username=?, password=? WHERE id=?";
            ps = c.prepareStatement(sql);
            ps.setString(1, user);
            ps.setString(2, pwd);
            ps.setString(3, id);
            int row = ps.executeUpdate();
            if (row >= 1) {
                isSuccess = true;
            }
            ps.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isSuccess;
    }

    public UserBean getUser(String username, String password) throws SQLException {
        String sql = "SELECT * FROM USERS WHERE username=? AND password=?";
        try (Connection c = getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UserBean user = new UserBean();
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("userEmail"));
                    user.setRole(rs.getString("role"));
                    user.setShopId(rs.getString("shop_id"));
                    user.setWarehouseId(rs.getString("warehouse_id"));
                    return user;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addUser(String username, String password, String email, String role, String shopId,
            String warehouseId) {
        Connection c;
        PreparedStatement ps;
        boolean isSuccess = false;
        try {
            c = getConnection();
            String sql = "INSERT INTO USERS (username, password, userEmail, role, shop_id, warehouse_id) VALUES (?, ?, ?, ?, ?, ?)";
            ps = c.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, email);
            ps.setString(4, role);
            if (shopId != null && !shopId.isEmpty()) {
                ps.setInt(5, Integer.parseInt(shopId));
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }
            if (warehouseId != null && !warehouseId.isEmpty()) {
                ps.setInt(6, Integer.parseInt(warehouseId));
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }

            int row = ps.executeUpdate();
            if (row >= 1) {
                isSuccess = true;
            }
            ps.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isSuccess;
    }

}