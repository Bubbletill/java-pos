package store.bubbletill.pos.data;

public enum PaymentType {

    CASH("Cash", true),
    CARD("Debit/Credit", false),
    VOID("Void", false);

    private String localName;
    private boolean requiresCashDraw;

    private PaymentType(String localName, boolean requiresCashDraw) {
        this.localName = localName;
        this.requiresCashDraw = requiresCashDraw;
    }

    public String getLocalName() {return localName;}
    public boolean requiresCashDraw() { return requiresCashDraw; }
}
