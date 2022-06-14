package store.bubbletill.pos.views;

import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import store.bubbletill.commons.*;
import store.bubbletill.pos.POSApplication;
import store.bubbletill.pos.controllers.POSHomeController;

public class ResumeView implements BubbleView {

    private final POSApplication app;
    private final POSHomeController controller;

    private final Pane resumeTrans;
    private final TableView<SuspendedListData> resumeTable;

    public ResumeView(POSApplication app, POSHomeController controller, Pane resumeTrans,
                      TableView<SuspendedListData> resumeTable, Button resumeButton, Button backButton) {

        this.app = app;
        this.controller = controller;
        this.resumeTrans = resumeTrans;
        this.resumeTable = resumeTable;

        resumeTable.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("date"));
        resumeTable.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("reg"));
        resumeTable.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("oper"));
        resumeTable.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("stringTotal"));

        resumeButton.setOnAction(e -> { onResumePress(); });
        backButton.setOnAction(e -> { onBackPress(); });
    }

    @Override
    public void show() {
        resumeTrans.setVisible(true);
        resumeTable.getItems().clear();

        try {
            HttpClient httpClient = HttpClientBuilder.create().build();

            StringEntity requestEntity = new StringEntity(
                    "{\"store\":\"" + app.store + "\", \"token\":\"" + POSApplication.getInstance().accessToken + "\"}",
                    ContentType.APPLICATION_JSON);

            HttpPost postMethod = new HttpPost("http://localhost:5000/pos/listsuspended");
            postMethod.setEntity(requestEntity);

            HttpResponse rawResponse = httpClient.execute(postMethod);
            String out = EntityUtils.toString(rawResponse.getEntity());

            SuspendedListData[] listData = POSApplication.gson.fromJson(out, SuspendedListData[].class);

            for (SuspendedListData sld : listData) {
                resumeTable.getItems().add(sld);
            }
        } catch (Exception e) {
            e.printStackTrace();
            controller.showError(e.getMessage());
        }
    }

    @Override
    public void hide() {
        resumeTrans.setVisible(false);
    }

    private void onBackPress() {
        resumeTrans.setVisible(false);
        controller.homeTenderView.show();
        controller.showError(null);
    }

    private void onResumePress() {
        controller.showError(null);
        if (resumeTable.getSelectionModel().getSelectedItem() == null) {
            controller.showError("Please select a transaction to resume.");
            return;
        }
        controller.resumeTransaction(resumeTable.getSelectionModel().getSelectedItem().getUsid());
    }
}
