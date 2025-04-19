package ict.db;

import ict.bean.DeliveryBean;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles database operations for the 'deliveries' table.
 */
public class DeliveryDB {

    private static final Logger LOGGER = Logger.getLogger(DeliveryDB.class.getName());
    private String dburl, username, password;

    public DeliveryDB(String dburl, String dbUser, String dbPassword) {
        this.dburl = dburl;
        this.username = dbUser;
        this.password = dbPassword;
    }

    public Connection getConnection() throws SQLException, IOException {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) { throw new IOException("Database driver not found.", e); }
        return DriverManager.getConnection(dburl, username, password);
    }

    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try { resource.close(); }
            catch (Exception e) { LOGGER.log(Level.WARNING, "Failed to close resource", e); }
        }
    }

    /**
     * Retrieves all delivery records, joining related tables for names.
     * @return List of DeliveryBean objects.
     */
    public List<DeliveryBean> getAllDeliveries() {
        List<DeliveryBean> deliveries = new ArrayList<>();
        String sql = "SELECT d.*, f.fruit_name, w_from.warehouse_name AS from_warehouse_name, w_to.warehouse_name AS to_warehouse_name " +
                     "FROM deliveries d " +
                     "JOIN fruits f ON d.fruit_id = f.fruit_id " +
                     "JOIN warehouses w_from ON d.from_warehouse_id = w_from.warehouse_id " +
                     "JOIN warehouses w_to ON d.to_warehouse_id = w_to.warehouse_id " +
                     "ORDER BY d.delivery_date DESC, d.delivery_id DESC";
        Connection conn = null; PreparedStatement ps = null; ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                DeliveryBean bean = mapRowToDeliveryBean(rs);
                deliveries.add(bean);
            }
        } catch (Exception e) { LOGGER.log(Level.SEVERE, "Error fetching all deliveries", e); }
        finally { closeQuietly(rs); closeQuietly(ps); closeQuietly(conn); }
        return deliveries;
    }

     /**
     * Retrieves delivery records related to a specific warehouse (either as sender or receiver).
     * @param warehouseId The ID of the warehouse.
     * @return List of DeliveryBean objects.
     */
    public List<DeliveryBean> getDeliveriesForWarehouse(int warehouseId) {
        List<DeliveryBean> deliveries = new ArrayList<>();
         String sql = "SELECT d.*, f.fruit_name, w_from.warehouse_name AS from_warehouse_name, w_to.warehouse_name AS to_warehouse_name " +
                     "FROM deliveries d " +
                     "JOIN fruits f ON d.fruit_id = f.fruit_id " +
                     "JOIN warehouses w_from ON d.from_warehouse_id = w_from.warehouse_id " +
                     "JOIN warehouses w_to ON d.to_warehouse_id = w_to.warehouse_id " +
                     "WHERE d.from_warehouse_id = ? OR d.to_warehouse_id = ? " + // Filter by from OR to
                     "ORDER BY d.delivery_date DESC, d.delivery_id DESC";
        Connection conn = null; PreparedStatement ps = null; ResultSet rs = null;
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
        } catch (Exception e) { LOGGER.log(Level.SEVERE, "Error fetching deliveries for warehouse " + warehouseId, e); }
        finally { closeQuietly(rs); closeQuietly(ps); closeQuietly(conn); }
        return deliveries;
    }

    // Helper method to map ResultSet row to DeliveryBean
    private DeliveryBean mapRowToDeliveryBean(ResultSet rs) throws SQLException {
        DeliveryBean bean = new DeliveryBean();
        bean.setDeliveryId(rs.getInt("delivery_id"));
        bean.setFruitId(rs.getInt("fruit_id"));
        bean.setFromWarehouseId(rs.getInt("from_warehouse_id"));
        bean.setToWarehouseId(rs.getInt("to_warehouse_id"));
        bean.setQuantity(rs.getInt("quantity"));
        bean.setDeliveryDate(rs.getDate("delivery_date"));
        bean.setStatus(rs.getString("status"));
        // Joined fields
        bean.setFruitName(rs.getString("fruit_name"));
        bean.setFromWarehouseName(rs.getString("from_warehouse_name"));
        bean.setToWarehouseName(rs.getString("to_warehouse_name"));
        return bean;
    }
}
