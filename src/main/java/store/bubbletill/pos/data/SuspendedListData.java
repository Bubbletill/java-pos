package store.bubbletill.pos.data;

import java.util.Date;

public class SuspendedListData {
    private String date;
    private String oper;
    private Integer reg;
    private Integer usid;

    public SuspendedListData() { }

    public String getDate() {
        return date;
    }

    public String getOper() {
        return oper;
    }

    public Integer getReg() {
        return reg;
    }

    public Integer getUsid() {
        return usid;
    }
}
