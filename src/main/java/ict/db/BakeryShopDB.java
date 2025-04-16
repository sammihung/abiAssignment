package ict.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import ict.bean.BakeryShopBean;

public class BakeryShopDB {

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

    public BakeryShopDB(String dburl, String dbUser, String dbPassword) {
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

    public ArrayList<BakeryShopBean> getBakeryShop() throws SQLException, IOException {
        String sql = "SELECT shop_id, shop_name, city, country FROM shops";
        ArrayList<BakeryShopBean> bakeryShops = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BakeryShopBean shop = new BakeryShopBean(
                        rs.getString("shop_id"),
                        rs.getString("shop_name"),
                        rs.getString("city"),
                        rs.getString("country"));
                bakeryShops.add(shop);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bakeryShops;
    }
    // Add this method inside your existing BakeryShopDB class

    /**
     * Retrieves a single shop by its ID.
     *
     * @param shopId The ID of the shop to retrieve.
     * @return A BakeryShopBean object if found, otherwise null.
     */
    public BakeryShopBean getShopById(int shopId) {
        BakeryShopBean shop = null;
        // Assumes table name is 'shops' and columns match BakeryShopBean fields
        String sql = "SELECT shop_id, shop_name, city, country FROM shops WHERE shop_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection(); // Use the existing getConnection method
            ps = conn.prepareStatement(sql);
            ps.setInt(1, shopId);
            rs = ps.executeQuery();

            if (rs.next()) {
                shop = new BakeryShopBean();
                // Make sure column names match your 'shops' table schema exactly
                shop.setShop_id(rs.getString("shop_id")); // Assuming BakeryShopBean uses String ID
                shop.setShop_name(rs.getString("shop_name"));
                shop.setCity(rs.getString("city"));
                shop.setCountry(rs.getString("country"));
                // System.out.println("Shop found: ID=" + shopId); // Optional logging
            } else {
                // System.out.println("Shop not found: ID=" + shopId); // Optional logging
            }
        } catch (SQLException | IOException e) {
            // Consider adding logging here using a Logger if you have one setup
            System.err.println("Error fetching shop with ID: " + shopId);
            e.printStackTrace(); // Print stack trace for debugging
        } finally {
            // Close resources in reverse order of creation
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                /* ignore */ }
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException e) {
                /* ignore */ }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                /* ignore */ }
        }
        return shop;
    }

}
