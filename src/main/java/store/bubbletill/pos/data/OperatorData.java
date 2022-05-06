package store.bubbletill.pos.data;

public class OperatorData {

    private String id;
    private String password;
    private int manager;
    private String posperms;
    private String boperms;

    public String getOperatorId() {
        return id;
    }

    public boolean isManager() {
        return manager == 1;
    }
}
