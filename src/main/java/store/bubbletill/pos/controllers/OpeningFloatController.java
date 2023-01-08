package store.bubbletill.pos.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import store.bubbletill.commons.Formatters;
import store.bubbletill.commons.PaymentType;
import store.bubbletill.commons.TransactionListData;
import store.bubbletill.pos.POSApplication;

import java.time.LocalDateTime;

public class OpeningFloatController {

    private final POSApplication app = POSApplication.getInstance();
    private final POSContainerController controller = POSContainerController.getInstance();

    @FXML private Pane declareOpeningFloat;
    @FXML private Pane dofPrompt;
    @FXML private Pane dofDeclare;
    @FXML private TextField dof50;
    @FXML private TextField dof20;
    @FXML private TextField dof10;
    @FXML private TextField dof5;
    @FXML private TextField dof1;
    @FXML private TextField dof50p;
    @FXML private TextField dof20p;
    @FXML private TextField dof10p;
    @FXML private TextField dof5p;
    @FXML private TextField dof2p;
    @FXML private TextField dof1p;

    @FXML private Button openingFloatYesButton;
    @FXML private Button openingFloatNoButton;
    @FXML private Button openingFloatSubmitButton;

    @FXML
    public void initialize() {
        declareOpeningFloat.setVisible(true);
        openingFloatYesButton.setOnAction(e -> { onYes(); });
        openingFloatNoButton.setOnAction(e -> { onNo(); });
        openingFloatSubmitButton.setOnAction(e -> { onSubmit(); });

        dofPrompt.setVisible(true);
        dofDeclare.setVisible(false);
    }

    private void onNo() {
        controller.showError(null);
        app.cashInDraw = getDBExpectCashInDraw();
        controller.updateSubScene("mainmenu");
    }


    private void onYes() {
        controller.showError(null);
        if (app.managerLoginRequest("Opening Float")) {
            dofPrompt.setVisible(false);
            dofDeclare.setVisible(true);
        } else {
            controller.showError("Insufficient permission.");
        }
    }

    private void onSubmit() {
        try {
            app.cashInDraw = 0;
            app.cashInDraw += Integer.parseInt(dof50.getText()) * 50;
            app.cashInDraw += Integer.parseInt(dof20.getText()) * 20;
            app.cashInDraw += Integer.parseInt(dof10.getText()) * 10;
            app.cashInDraw += Integer.parseInt(dof5.getText()) * 5;
            app.cashInDraw += Integer.parseInt(dof1.getText());
            app.cashInDraw += Integer.parseInt(dof50p.getText()) * 0.5;
            app.cashInDraw += Integer.parseInt(dof20p.getText()) * 0.2;
            app.cashInDraw += Integer.parseInt(dof10p.getText()) * 0.1;
            app.cashInDraw += Integer.parseInt(dof5p.getText()) * 0.05;
            app.cashInDraw += Integer.parseInt(dof2p.getText()) * 0.02;
            app.cashInDraw += Integer.parseInt(dof1p.getText()) * 0.01;
        } catch (Exception e) {
            controller.showError("Please populate all fields with a valid number.");
            return;
        }

        controller.showError(null);
        app.floatKnown = true;
        controller.updateSubScene("mainmenu");
    }

    private double getDBExpectCashInDraw() {
        TransactionListData[] listData;
        double amount = 0;
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();

            String todaysDate = Formatters.dateFormatter.format(LocalDateTime.now());
            StringEntity requestEntity = new StringEntity(
                    "{"
                            + "\"store\": \"" + app.localData.getStore()
                            + "\", \"startDate\": \"" + todaysDate
                            + "\", \"endDate\": \"" + todaysDate
                            + "\", \"startTime\": \"" + "00:00"
                            + "\", \"endTime\": \"" + "23:59"
                            + "\", \"register\": \"" + app.localData.getReg()
                            + "\", \"operator\": \"" + ""
                            + "\", \"startTotal\": \"" + Double.MIN_VALUE
                            + "\", \"endTotal\": \"" + Double.MAX_VALUE
                            + "\", \"token\" :\"" + app.localData.getToken()
                            + "\"}",
                    ContentType.APPLICATION_JSON);

            HttpPost postMethod = new HttpPost(app.localData.getBackend() + "/bo/listtransactions");
            postMethod.setEntity(requestEntity);

            HttpResponse rawResponse = httpClient.execute(postMethod);
            String out = EntityUtils.toString(rawResponse.getEntity());

            out = out.replaceAll("\"\\[", "[");
            out = out.replaceAll("]\"", "]");

            out = out.replaceAll("\"\\{", "{");
            out = out.replaceAll("}\"", "}");

            listData = POSApplication.gson.fromJson(out, TransactionListData[].class);
        } catch (Exception e) {
            e.printStackTrace();
            controller.showError(e.getMessage());
            return 0;
        }

        for (TransactionListData listItem : listData) {
            double change = 0;
            if (listItem.getMethods().containsKey(PaymentType.CASH)) {
                amount += listItem.getMethods().get(PaymentType.CASH);

                if (listItem.getMethods().get(PaymentType.CASH) > listItem.getTotal())
                    change = listItem.getMethods().get(PaymentType.CASH) - listItem.getTotal();
            }
            amount -= change;
        }

        return amount;
    }
}
