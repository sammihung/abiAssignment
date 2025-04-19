package ict.bean;

import java.io.Serializable;
import java.sql.Date;

/**
 * Represents a delivery entity based on the 'deliveries' table.
 */
public class DeliveryBean implements Serializable {

    private int deliveryId;
    private int fruitId;
    private int fromWarehouseId;
    private int toWarehouseId;
    private int quantity;
    private Date deliveryDate;
    private String status;

    // Optional fields for display
    private String fruitName;
    private String fromWarehouseName;
    private String toWarehouseName;

    // Default constructor
    public DeliveryBean() {
    }

    // Getters and Setters...

    public int getDeliveryId() { return deliveryId; }
    public void setDeliveryId(int deliveryId) { this.deliveryId = deliveryId; }
    public int getFruitId() { return fruitId; }
    public void setFruitId(int fruitId) { this.fruitId = fruitId; }
    public int getFromWarehouseId() { return fromWarehouseId; }
    public void setFromWarehouseId(int fromWarehouseId) { this.fromWarehouseId = fromWarehouseId; }
    public int getToWarehouseId() { return toWarehouseId; }
    public void setToWarehouseId(int toWarehouseId) { this.toWarehouseId = toWarehouseId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Date getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(Date deliveryDate) { this.deliveryDate = deliveryDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFruitName() { return fruitName; }
    public void setFruitName(String fruitName) { this.fruitName = fruitName; }
    public String getFromWarehouseName() { return fromWarehouseName; }
    public void setFromWarehouseName(String fromWarehouseName) { this.fromWarehouseName = fromWarehouseName; }
    public String getToWarehouseName() { return toWarehouseName; }
    public void setToWarehouseName(String toWarehouseName) { this.toWarehouseName = toWarehouseName; }

    @Override
    public String toString() {
        return "DeliveryBean{" + "deliveryId=" + deliveryId + ", fruitId=" + fruitId + ", fromWarehouseId=" + fromWarehouseId + ", toWarehouseId=" + toWarehouseId + ", quantity=" + quantity + ", deliveryDate=" + deliveryDate + ", status='" + status + '\'' + '}';
    }
}

