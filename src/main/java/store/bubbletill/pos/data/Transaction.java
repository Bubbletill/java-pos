package store.bubbletill.pos.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Transaction {

    // Details
    private int id;
    private long time;
    private boolean voided;

    // Basket
    private List<StockData> basket;
    private HashMap<PaymentType, Double> tender;

    public Transaction(int id) {
        this.id = id;
        time = System.currentTimeMillis() / 1000L;
        basket = new ArrayList<>();
        tender = new HashMap<>();
        voided = false;
    }

    public int getId() {
        return id;
    }

    public List<StockData> getBasket() {
        return basket;
    }

    public void setBasket(List<StockData> basket) {
        this.basket = basket;
    }
    public void addToBasket(StockData stockData) {
        basket.add(stockData);
    }
    public double getBasketTotal() {
        double total = 0.0;
        for (StockData sd : basket) {
            total += sd.getPrice() - sd.getPriceReduction();
        }

        return total;
    }

    public void addTender(PaymentType paymentType, double amount) {
        if (!tender.containsKey(paymentType)) {
            tender.put(paymentType, amount);
            return;
        }

        double current = tender.get(paymentType);
        tender.put(paymentType, current + amount);
    }

    public void voidTender() {
        tender.clear();
    }
    public double getTenderTotal() {
        int total = 0;
        for (Map.Entry<PaymentType, Double> e : tender.entrySet()) {
            total += e.getValue();
        }

        return total;
    }
    public boolean isTenderComplete() { System.out.println("running"); return getTenderTotal() >= getBasketTotal(); }

    public boolean isVoided() { return voided; }
    public void setVoided(boolean voided) { this.voided = voided; }
}
