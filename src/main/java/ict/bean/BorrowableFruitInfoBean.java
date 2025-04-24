package ict.bean;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Extends FruitBean to include information about potential lenders
 * and their available quantity for this specific fruit.
 */
public class BorrowableFruitInfoBean extends FruitBean implements Serializable {

    // List containing maps for each potential lender shop.
    // Each map could contain keys like "shopId", "shopName", "quantity".
    private List<Map<String, Object>> lenderInfo;

    /**
     * Constructor that takes an existing FruitBean and initializes.
     * 
     * @param fruit The base fruit information.
     */
    public BorrowableFruitInfoBean(FruitBean fruit) {
        // Copy properties from the base FruitBean
        super(fruit.getFruitId(), fruit.getFruitName(), fruit.getSourceCountry());
        // Initialize lenderInfo to an empty list or null as needed
        this.lenderInfo = new java.util.ArrayList<>();
    }

    // Getter and Setter for lenderInfo
    public List<Map<String, Object>> getLenderInfo() {
        return lenderInfo;
    }

    public void setLenderInfo(List<Map<String, Object>> lenderInfo) {
        this.lenderInfo = lenderInfo;
    }

    // Optional: Override toString for debugging
    @Override
    public String toString() {
        return "BorrowableFruitInfoBean{" +
                "fruitId=" + getFruitId() +
                ", fruitName='" + getFruitName() + '\'' +
                ", sourceCountry='" + getSourceCountry() + '\'' +
                ", lenderInfo=" + lenderInfo +
                '}';
    }
}
