package store.bubbletill.pos.data;

import store.bubbletill.pos.POSApplication;

import java.util.Date;

public class SuspendedListData {
    private String date;
    private String oper;
    private Integer reg;
    private Integer usid;
    private double total;

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

    public double getTotal() {
        return total;
    }

    public String getStringTotal() {
        return "£" + POSApplication.df.format(getTotal());
    }
}
