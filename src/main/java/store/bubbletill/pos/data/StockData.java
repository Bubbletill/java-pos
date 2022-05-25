package store.bubbletill.pos.data;

public class StockData {

    private int category;
    private int code;
    private String description;
    private double price;
    private double priceReduction;
    private boolean refunded;

    public StockData(int category, int code, String description, double price) {
        this.category = category;
        this.code = code;
        this.description = description;
        this.price = price;
        this.priceReduction = 0;
        this.refunded = false;
    }

    public int getCategory() {
        return category;
    }

    public int getItemCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }
    public double getPriceReduction() {return priceReduction;}

    public void setPriceReduction(double priceReduction) { this.priceReduction = priceReduction; }

    public boolean isRefunded() { return refunded; }
    public void setRefunded(boolean refunded) {this.refunded = refunded;}
}
