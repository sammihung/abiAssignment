package ict.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import ict.bean.WarehouseBean;

public class WarehouseDB {

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

    public WarehouseDB(String dburl, String dbUser, String dbPassword) {
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

    public ArrayList<WarehouseBean> getBakeryShop() throws SQLException, IOException {
        String sql = "SELECT shop_id, shop_name, city, country FROM shops";
        ArrayList<WarehouseBean> warehouses = new ArrayList<>();
        try (Connection c = getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                WarehouseBean warehouse = new WarehouseBean(
                        rs.getString("warehouse_id"),
                        rs.getString("warehouse_name"),
                        rs.getString("city"),
                        rs.getString("country"),
                        rs.getString("is_source"));
                warehouses.add(warehouse);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return warehouses;
    } // Add this method inside your existing WarehouseDB class

    /**
     * Retrieves a single warehouse by its ID.
     *
     * @param warehouseId The ID of the warehouse to retrieve.
     * @return A WarehouseBean object if found, otherwise null.
     */
    public WarehouseBean getWarehouseById(int warehouseId) {
        WarehouseBean warehouse = null;
        String sql = "SELECT warehouse_id, warehouse_name, city, country, is_source FROM warehouses WHERE warehouse_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection(); // Use the existing getConnection method
            ps = conn.prepareStatement(sql);
            ps.setInt(1, warehouseId);
            rs = ps.executeQuery();

            if (rs.next()) {
                warehouse = new WarehouseBean();
                // Make sure column names match your 'warehouses' table schema
                warehouse.setWarehouse_id(rs.getString("warehouse_id")); // Assuming WarehouseBean uses String ID
                warehouse.setWarehouse_name(rs.getString("warehouse_name"));
                warehouse.setCity(rs.getString("city"));
                warehouse.setCountry(rs.getString("country"));
                // Handle boolean/tinyint for is_source - adapt based on WarehouseBean's field
                // type
                // If WarehouseBean uses String:
                warehouse.setIs_source(rs.getBoolean("is_source") ? "1" : "0");
                // If WarehouseBean uses boolean:
                // warehouse.setIs_source(rs.getBoolean("is_source"));

                // System.out.println("Warehouse found: ID=" + warehouseId); // Optional logging
            } else {
                // System.out.println("Warehouse not found: ID=" + warehouseId); // Optional
                // logging
            }
        } catch (SQLException | IOException e) {
            // Consider adding logging here using a Logger if you have one setup
            System.err.println("Error fetching warehouse with ID: " + warehouseId);
            e.printStackTrace(); // Print stack trace for debugging
        } finally {
            // Close resources using your preferred method (e.g., individual try-catch or a
            // helper)
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
        return warehouse;
    }
    // Add this method inside your existing WarehouseDB class

    /**
     * Finds a non-source ('central') warehouse ID in a specific country.
     * Returns the first one found if multiple exist.
     *
     * @param country The target country.
     * @return The warehouse ID of a non-source warehouse, or -1 if none found.
     */
    public int findCentralWarehouseInCountry(String country) {
        int warehouseId = -1;
        // Find a warehouse in the country where is_source is false or null
        String sql = "SELECT warehouse_id FROM warehouses WHERE country = ? AND (is_source = 0 OR is_source IS NULL) LIMIT 1";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, country);
            rs = ps.executeQuery();

            if (rs.next()) {
                warehouseId = rs.getInt("warehouse_id");
                // System.out.println("Found central warehouse ID " + warehouseId + " in country
                // " + country); // Optional logging
            } else {
                // System.out.println("No central warehouse found in country " + country); //
                // Optional logging
            }
        } catch (SQLException | IOException e) {
            System.err.println("Error finding central warehouse in country: " + country);
            e.printStackTrace();
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException e) {
                /* ignore */ }
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                /* ignore */ }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                /* ignore */ }
        }
        return warehouseId;
    }

}
