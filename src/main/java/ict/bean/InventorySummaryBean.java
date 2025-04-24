package ict.bean;

import java.io.Serializable;

public class InventorySummaryBean implements Serializable {

    private String groupingDimension;
    private int fruitId;
    private String fruitName;
    private long totalQuantity;

    public InventorySummaryBean() {
    }

    public String getGroupingDimension() {
        return groupingDimension;
    }

    public void setGroupingDimension(String groupingDimension) {
        this.groupingDimension = groupingDimension;
    }

    public int getFruitId() {
        return fruitId;
    }

    public void setFruitId(int fruitId) {
        this.fruitId = fruitId;
    }

    public String getFruitName() {
        return fruitName;
    }

    public void setFruitName(String fruitName) {
        this.fruitName = fruitName;
    }

    public long getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(long totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    @Override
    public String toString() {
        return "InventorySummaryBean{" +
                "groupingDimension='" + groupingDimension + '\'' +
                ", fruitId=" + fruitId +
                ", fruitName='" + fruitName + '\'' +
                ", totalQuantity=" + totalQuantity +
                '}';
    }
}