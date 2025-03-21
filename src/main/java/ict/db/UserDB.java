package ict.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
            String sql = "INSERT INTO USERINFO (id, username, password) VALUES (?, ?, ?)";
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
}
