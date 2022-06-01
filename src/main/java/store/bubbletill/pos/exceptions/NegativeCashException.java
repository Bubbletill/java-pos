package store.bubbletill.pos.exceptions;

public class NegativeCashException extends Exception {

    public NegativeCashException(String errorMessage) {
        super(errorMessage);
    }

}
