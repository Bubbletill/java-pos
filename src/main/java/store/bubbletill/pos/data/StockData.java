package store.bubbletill.pos.data;

public class StockData {

    private int category;
    private int code;
    private String description;
    private double price;

    public StockData(int category, int code, String description, double price) {
        this.category = category;
        this.code = code;
        this.description = description;
        this.price = price;
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
}
