package store.bubbletill.pos.views;

import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import store.bubbletill.commons.BubbleView;
import store.bubbletill.commons.SuspendedListData;

public class ResumeView implements BubbleView {

    private Pane resumeTrans;
    private TableView<SuspendedListData> resumeTable;

    public ResumeView(Pane resumeTrans, TableView<SuspendedListData> resumeTable) {
        this.resumeTrans = resumeTrans;
        this.resumeTable = resumeTable;
    }

    @Override
    public void show() {

    }

    @Override
    public void hide() {

    }
}
