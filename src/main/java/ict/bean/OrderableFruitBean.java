package ict.bean;

import java.io.Serializable;

public class OrderableFruitBean extends FruitBean implements Serializable {

    private int availableSourceQuantity;
    private int sourceWarehouseId;

    public OrderableFruitBean() {
        super();
    }

    public OrderableFruitBean(int fruitId, String fruitName, String sourceCountry, int availableSourceQuantity,
            int sourceWarehouseId) {
        super(fruitId, fruitName, sourceCountry);
        this.availableSourceQuantity = availableSourceQuantity;
        this.sourceWarehouseId = sourceWarehouseId;
    }

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