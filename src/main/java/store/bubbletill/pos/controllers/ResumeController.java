package store.bubbletill.pos.controllers;

import javafx.fxml.FXML;
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
import store.bubbletill.commons.SuspendedListData;
import store.bubbletill.pos.POSApplication;

public class ResumeController {

    private final POSApplication app = POSApplication.getInstance();
    private final POSContainerController controller = POSContainerController.getInstance();

    // Resume View
    @FXML private TableView<SuspendedListData> resumeTable;

    @FXML private Button resumeButton;
    @FXML private Button backButton;

    @FXML
    private void initialize() {
        System.out.println("resume init");
        backButton.setVisible(true);

        resumeTable.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("date"));
        resumeTable.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("reg"));
        resumeTable.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("oper"));
        resumeTable.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("stringTotal"));

        resumeButton.setOnAction(e -> { onResumePress(); });
        backButton.setOnAction(e -> { onBackPress(); });

        resumeTable.getItems().clear();

        try {
            HttpClient httpClient = HttpClientBuilder.create().build();

            StringEntity requestEntity = new StringEntity(
                    "{\"store\":\"" + app.store + "\", \"token\":\"" + POSApplication.getInstance().accessToken + "\"}",
                    ContentType.APPLICATION_JSON);

            HttpPost postMethod = new HttpPost(POSApplication.backendUrl + "/pos/listsuspended");
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

    private void onBackPress() {
        controller.updateSubScene("mainmenu");
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
