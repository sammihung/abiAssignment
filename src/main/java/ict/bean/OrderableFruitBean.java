package ict.bean;

import java.io.Serializable;

/**
 * Represents a fruit that can be ordered from its source, including available quantity.
 */
public class OrderableFruitBean extends FruitBean implements Serializable { // Extends FruitBean

    private int availableSourceQuantity;
    private int sourceWarehouseId; // ID of the source warehouse

    // Default constructor
    public OrderableFruitBean() {
        super(); // Call superclass constructor
    }

    // Constructor inheriting from FruitBean and adding quantity/warehouse
    public OrderableFruitBean(int fruitId, String fruitName, String sourceCountry, int availableSourceQuantity, int sourceWarehouseId) {
        super(fruitId, fruitName, sourceCountry); // Call superclass constructor
        this.availableSourceQuantity = availableSourceQuantity;
        this.sourceWarehouseId = sourceWarehouseId;
    }

    // Getters and Setters for new fields
    public int getAvailableSourceQuantity() {
        return availableSourceQuantity;
    }

    public void setAvailableSourceQuantity(int availableSourceQuantity) {
        this.availableSourceQuantity = availableSourceQuantity;
    }

    public int getSourceWarehouseId() {
        return sourceWarehouseId;
    }

    public void setSourceWarehouseId(int sourceWarehouseId) {
        this.sourceWarehouseId = sourceWarehouseId;
    }

    @Override
    public String toString() {
        return "OrderableFruitBean{" +
                "fruitId=" + getFruitId() +
                ", fruitName='" + getFruitName() + '\'' +
                ", sourceCountry='" + getSourceCountry() + '\'' +
                ", availableSourceQuantity=" + availableSourceQuantity +
                ", sourceWarehouseId=" + sourceWarehouseId +
                '}';
    }
}
