package ict.bean;

import java.io.Serializable;

public class ConsumptionDataBean implements Serializable {

    private String itemName;
    private long totalConsumedQuantity;

    public ConsumptionDataBean() {
    }

    public ConsumptionDataBean(String itemName, long totalConsumedQuantity) {
        this.itemName = itemName;
        this.totalConsumedQuantity = totalConsumedQuantity;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public long getTotalConsumedQuantity() {
        return totalConsumedQuantity;
    }

    public void setTotalConsumedQuantity(long totalConsumedQuantity) {
        this.totalConsumedQuantity = totalConsumedQuantity;
    }

    @Override
    public String toString() {
        return "ConsumptionDataBean{" +
                "itemName='" + itemName + '\'' +
                ", totalConsumedQuantity=" + totalConsumedQuantity +
                '}';
    }
}