package ict.bean;

import java.io.Serializable;
import java.sql.Date;

/**
 * Represents a borrowing entity based on the 'borrowings' table.
 */
public class BorrowingBean implements Serializable {

    private int borrowingId;
    private int fruitId;
    private int borrowingShopId; // Shop lending the fruit
    private int receivingShopId; // Shop receiving the fruit
    private int quantity;
    private Date borrowingDate;
    private String status;

    // Optional fields for display purposes
    private String fruitName;
    private String borrowingShopName;
    private String receivingShopName;

    // Default constructor
    public BorrowingBean() {
    }

    // Getters and Setters...

    public int getBorrowingId() {
        return borrowingId;
    }

    public void setBorrowingId(int borrowingId) {
        this.borrowingId = borrowingId;
    }

    public int getFruitId() {
        return fruitId;
    }

    public void setFruitId(int fruitId) {
        this.fruitId = fruitId;
    }

    public int getBorrowingShopId() {
        return borrowingShopId;
    }

    public void setBorrowingShopId(int borrowingShopId) {
        this.borrowingShopId = borrowingShopId;
    }

    public int getReceivingShopId() {
        return receivingShopId;
    }

    public void setReceivingShopId(int receivingShopId) {
        this.receivingShopId = receivingShopId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Date getBorrowingDate() {
        return borrowingDate;
    }

    public void setBorrowingDate(Date borrowingDate) {
        this.borrowingDate = borrowingDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFruitName() {
        return fruitName;
    }

    public void setFruitName(String fruitName) {
        this.fruitName = fruitName;
    }

    public String getBorrowingShopName() {
        return borrowingShopName;
    }

    public void setBorrowingShopName(String borrowingShopName) {
        this.borrowingShopName = borrowingShopName;
    }

    public String getReceivingShopName() {
        return receivingShopName;
    }

    public void setReceivingShopName(String receivingShopName) {
        this.receivingShopName = receivingShopName;
    }

    @Override
    public String toString() {
        return "BorrowingBean{" +
                "borrowingId=" + borrowingId +
                ", fruitId=" + fruitId +
                ", borrowingShopId=" + borrowingShopId +
                ", receivingShopId=" + receivingShopId +
                ", quantity=" + quantity +
                ", borrowingDate=" + borrowingDate +
                ", status='" + status + '\'' +
                '}';
    }
}
