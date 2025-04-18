    package ict.bean;

    import java.io.Serializable;

    /**
     * Represents a row in a consumption report (e.g., consumption per item).
     */
    public class ConsumptionDataBean implements Serializable {

        private String itemName; // Can be fruit name, shop name, country, etc.
        private long totalConsumedQuantity; // Use long for potentially large sums

        // Default constructor
        public ConsumptionDataBean() {
        }

        // Constructor
        public ConsumptionDataBean(String itemName, long totalConsumedQuantity) {
            this.itemName = itemName;
            this.totalConsumedQuantity = totalConsumedQuantity;
        }

        // Getters and Setters
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
    