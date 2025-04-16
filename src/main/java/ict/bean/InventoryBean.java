package ict.bean;

import java.io.Serializable;

/**
 * Represents an inventory entity based on the 'inventory' table.
 */
public class InventoryBean implements Serializable {

    private int inventoryId;
    private int fruitId;
    private Integer shopId; // Use Integer to allow null
    private Integer warehouseId; // Use Integer to allow null
    private int quantity;

     // Optional: Add fields to hold related object details
    private String fruitName;
    private String locationName; // Could be shop name or warehouse name

    // Default constructor
    public InventoryBean() {
    }

    // Constructor with all fields
    public InventoryBean(int inventoryId, int fruitId, Integer shopId, Integer warehouseId, int quantity) {
        this.inventoryId = inventoryId;
        this.fruitId = fruitId;
        this.shopId = shopId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
    }

    // Getters and Setters...

    public int getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(int inventoryId) {
        this.inventoryId = inventoryId;
    }

    public int getFruitId() {
        return fruitId;
    }

    public void setFruitId(int fruitId) {
        this.fruitId = fruitId;
    }

    public Integer getShopId() {
        return shopId;
    }

    public void setShopId(Integer shopId) {
        this.shopId = shopId;
    }

    public Integer getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Integer warehouseId) {
        this.warehouseId = warehouseId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

     public String getFruitName() {
        return fruitName;
    }

    public void setFruitName(String fruitName) {
        this.fruitName = fruitName;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    @Override
    public String toString() {
        return "InventoryBean{" +
                "inventoryId=" + inventoryId +
                ", fruitId=" + fruitId +
                ", shopId=" + shopId +
                ", warehouseId=" + warehouseId +
                ", quantity=" + quantity +
                '}';
    }
}
