package ict.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ict.bean.WarehouseBean;

public class WarehouseDB {

    private static final Logger LOGGER = Logger.getLogger(WarehouseDB.class.getName());
    String dburl, username, password;

    public WarehouseDB(String dburl, String dbUser, String dbPassword) {
        this.dburl = dburl;
        this.username = dbUser;
        this.password = dbPassword;
    }

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

    public Connection getConnection() throws SQLException, IOException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {

            LOGGER.log(Level.SEVERE, "MySQL JDBC Driver not found", e);
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
        return DriverManager.getConnection(dburl, username, password);
    }

    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to close resource: " + resource.getClass().getSimpleName(), e);
            }
        }
    }

    public WarehouseBean getWarehouseById(int warehouseId) {
        WarehouseBean warehouse = null;

        String sql = "SELECT warehouse_id, warehouse_name, city, country, is_source FROM warehouses WHERE warehouse_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, warehouseId);
            rs = ps.executeQuery();

            if (rs.next()) {
                warehouse = mapRowToWarehouseBean(rs);
                LOGGER.log(Level.INFO, "Warehouse found: ID={0}", warehouseId);
            } else {
                LOGGER.log(Level.WARNING, "Warehouse not found: ID={0}", warehouseId);
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching warehouse with ID: " + warehouseId, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return warehouse;
    }

    public int findCentralWarehouseInCountry(String country) {
        int warehouseId = -1;

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
            }
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error finding central warehouse in country: " + country, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return warehouseId;
    }

    public List<WarehouseBean> getAllWarehouses() {
        List<WarehouseBean> warehouses = new ArrayList<>();

        String query = "SELECT warehouse_id, warehouse_name, city, country, is_source FROM warehouses ORDER BY country, city, warehouse_name";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();
            while (rs.next()) {
                WarehouseBean warehouse = mapRowToWarehouseBean(rs);
                warehouses.add(warehouse);
            }
            LOGGER.log(Level.INFO, "Fetched {0} warehouses.", warehouses.size());
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error fetching all warehouses", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(stmt);
            closeQuietly(conn);
        }
        return warehouses;
    }

    private WarehouseBean mapRowToWarehouseBean(ResultSet rs) throws SQLException {
        WarehouseBean warehouse = new WarehouseBean();

        warehouse.setWarehouse_id(rs.getString("warehouse_id"));
        warehouse.setWarehouse_name(rs.getString("warehouse_name"));
        warehouse.setCity(rs.getString("city"));
        warehouse.setCountry(rs.getString("country"));

        warehouse.setIs_source(rs.getBoolean("is_source") ? "1" : "0");

        return warehouse;
    }

}