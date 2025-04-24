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

import ict.bean.DeliveryBean;

public class DeliveryDB {

    private static final Logger LOGGER = Logger.getLogger(DeliveryDB.class.getName());
    private String dburl, username, password;

    public DeliveryDB(String dburl, String dbUser, String dbPassword) {
        this.dburl = dburl;
        this.username = dbUser;
        this.password = dbPassword;
    }

    public Connection getConnection() throws SQLException, IOException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IOException("Database driver not found.", e);
        }
        return DriverManager.getConnection(dburl, username, password);
    }

    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to close resource", e);
            }
        }
    }

    public List<DeliveryBean> getAllDeliveries() {
        List<DeliveryBean> deliveries = new ArrayList<>();
        String sql = "SELECT d.*, f.fruit_name, w_from.warehouse_name AS from_warehouse_name, w_to.warehouse_name AS to_warehouse_name "
                +
                "FROM deliveries d " +
                "JOIN fruits f ON d.fruit_id = f.fruit_id " +
                "JOIN warehouses w_from ON d.from_warehouse_id = w_from.warehouse_id " +
                "JOIN warehouses w_to ON d.to_warehouse_id = w_to.warehouse_id " +
                "ORDER BY d.delivery_date DESC, d.delivery_id DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                DeliveryBean bean = mapRowToDeliveryBean(rs);
                deliveries.add(bean);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching all deliveries", e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return deliveries;
    }

    public List<DeliveryBean> getDeliveriesForWarehouse(int warehouseId) {
        List<DeliveryBean> deliveries = new ArrayList<>();
        String sql = "SELECT d.*, f.fruit_name, w_from.warehouse_name AS from_warehouse_name, w_to.warehouse_name AS to_warehouse_name "
                +
                "FROM deliveries d " +
                "JOIN fruits f ON d.fruit_id = f.fruit_id " +
                "JOIN warehouses w_from ON d.from_warehouse_id = w_from.warehouse_id " +
                "JOIN warehouses w_to ON d.to_warehouse_id = w_to.warehouse_id " +
                "WHERE d.from_warehouse_id = ? OR d.to_warehouse_id = ? " +
                "ORDER BY d.delivery_date DESC, d.delivery_id DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, warehouseId);
            ps.setInt(2, warehouseId);
            rs = ps.executeQuery();
            while (rs.next()) {
                DeliveryBean bean = mapRowToDeliveryBean(rs);
                deliveries.add(bean);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching deliveries for warehouse " + warehouseId, e);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
        return deliveries;
    }

    private DeliveryBean mapRowToDeliveryBean(ResultSet rs) throws SQLException {
        DeliveryBean bean = new DeliveryBean();
        bean.setDeliveryId(rs.getInt("delivery_id"));
        bean.setFruitId(rs.getInt("fruit_id"));
        bean.setFromWarehouseId(rs.getInt("from_warehouse_id"));
        bean.setToWarehouseId(rs.getInt("to_warehouse_id"));
        bean.setQuantity(rs.getInt("quantity"));
        bean.setDeliveryDate(rs.getDate("delivery_date"));
        bean.setStatus(rs.getString("status"));

        bean.setFruitName(rs.getString("fruit_name"));
        bean.setFromWarehouseName(rs.getString("from_warehouse_name"));
        bean.setToWarehouseName(rs.getString("to_warehouse_name"));
        return bean;
    }
}