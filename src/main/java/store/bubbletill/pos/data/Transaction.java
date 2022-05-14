package store.bubbletill.pos.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Transaction {

    // Details
    private int id;
    private long time;

    // Basket
    private List<StockData> basket;
    private List<HashMap<PaymentType, Double>> tender;

    public Transaction(int id) {
        this.id = id;
        time = System.currentTimeMillis() / 1000L;
        basket = new ArrayList<>();
        tender = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void addToBasket(StockData stockData) {
        basket.add(stockData);
    }

    public void addTender(PaymentType paymentType, double amount) {
        HashMap<PaymentType, Double> tenderData = new HashMap<PaymentType, Double>();
        tenderData.put(paymentType, amount);
        tender.add(tenderData);
    }

    public void voidTender() {
        tender.clear();
    }


}
