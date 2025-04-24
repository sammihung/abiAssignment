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
        try (Connection c = getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
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

    public BakeryShopBean getShopById(int shopId) {
        BakeryShopBean shop = null;

        String sql = "SELECT shop_id, shop_name, city, country FROM shops WHERE shop_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, shopId);
            rs = ps.executeQuery();

            if (rs.next()) {
                shop = new BakeryShopBean();

                shop.setShop_id(rs.getString("shop_id"));
                shop.setShop_name(rs.getString("shop_name"));
                shop.setCity(rs.getString("city"));
                shop.setCountry(rs.getString("country"));

            } else {

            }
        } catch (SQLException | IOException e) {

            System.err.println("Error fetching shop with ID: " + shopId);
            e.printStackTrace();
        } finally {

            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
            }
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException e) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
            }
        }
        return shop;
    }

}