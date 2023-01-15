package store.bubbletill.pos.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import store.bubbletill.pos.POSApplication;
import store.bubbletill.commons.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class POSContainerController {

    @FXML private SubScene posSubScene;
    @FXML private AnchorPane containerAnchor;

    // Top status bar
    @FXML private Label dateTimeLabel;
    @FXML private Label statusLabel;
    @FXML private Label registerLabel;
    @FXML public Label transactionLabel;
    @FXML private Label operatorLabel;
    @FXML private Pane errorPane;
    @FXML private Label errorLabel;

    private POSApplication app;

    private static POSContainerController instance;

    @FXML
    private void initialize() {
        app = POSApplication.getInstance();
        instance = this;

        if (app.cashInDraw == -9999) {
            updateSubScene("openingfloat");
            POSApplication.buzzer("double");
        } else {
            updateSubScene("mainmenu");
        }

        errorPane.setVisible(false);

        if (app.dateTimeTimer != null)
            app.dateTimeTimer.cancel();

        app.dateTimeTimer = new Timer();
        app.dateTimeTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    dateTimeLabel.setText(Formatters.dateTimeFormatter.format(LocalDateTime.now()));
                });
            }
        }, 0, 5000);

        statusLabel.setText((app.workingOnline ? "Online" : "Offline"));
        registerLabel.setText("" + app.localData.getReg());
        transactionLabel.setText("" + app.transNo);
        operatorLabel.setText(app.operator.getId());
    }

    public static POSContainerController getInstance() {
        return instance;
    }

    public void updateSubScene(String fxml) {
        try {
            if (posSubScene != null)
                containerAnchor.getChildren().remove(posSubScene);

            FXMLLoader fxmlLoader = new FXMLLoader(POSApplication.class.getResource(fxml + ".fxml"));
            posSubScene = new SubScene(fxmlLoader.load(), 1920, 1010);
            posSubScene.relocate(0, 70);
            posSubScene.setVisible(true);
            containerAnchor.getChildren().add(posSubScene);
            showError(null);
        } catch (Exception e) {
            showError("Failed to load sub-scene: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public Stage getStage() {
        return (Stage) dateTimeLabel.getScene().getWindow();
    }

    public void showError(String error) {
        showError(error, true);
    }

    public void showError(String error, boolean buzzer) {
        if (error == null) {
            errorPane.setVisible(false);
            return;
        }

        errorPane.setVisible(true);
        errorLabel.setText(error);
        if (buzzer)
            POSApplication.buzzer("double");
    }

    public void resumeTransaction(int uniqueSuspendedId) {
        Transaction resumeData;
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();

            StringEntity requestEntity = new StringEntity(
                    "{\"usid\":\"" + uniqueSuspendedId + "\", \"token\":\"" + app.localData.getToken() + "\"}",
                    ContentType.APPLICATION_JSON);

            HttpPost postMethod = new HttpPost(app.localData.getBackend() + "/pos/resume");
            postMethod.setEntity(requestEntity);

            HttpResponse rawResponse = httpClient.execute(postMethod);
            String out = EntityUtils.toString(rawResponse.getEntity());

            resumeData = POSApplication.gson.fromJson(out, Transaction.class);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        app.transNo++;
        if (app.transNo == 10000)
            app.transNo = 1;
        app.transaction = new Transaction(app.transNo);
        app.transaction.setBasket(resumeData.getBasket());
        app.transaction.setLogs(resumeData.getLogs());
        app.transaction.log("Transaction resumed at " + Formatters.dateTimeFormatter.format(LocalDateTime.now()));
        transactionLabel.setText("" + app.transNo);
        updateSubScene("mainmenu");
    }

    public void performXRead() {
        showError("Performing X Read...", false);
        TransactionListData[] listData;
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

            listData = POSApplication.gson.fromJson(out, TransactionListData[].class);

            if (listData.length == 0) {
                showError("Cannot perform X Read on 0 transactions");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError(e.getMessage());
            return;
        }

        XReadData data = new XReadData(app.localData.getStore(), app.localData.getReg(), app.operator.getId());

        data.setTransactionCount(listData.length);
        data.setSystemCashInDraw(app.cashInDraw);
        data.setRegOpened("NA");
        data.setRegClosed("NA");
        for (TransactionListData listItem : listData) {
            //Transaction items = POSApplication.gson.fromJson(listItem.getItems(), Transaction.class);
            Transaction items = new Transaction(9999);

            if (items.isVoided()) {
                data.incrementTransVoidTotal();

                data.getTotalPerTransactionType().putIfAbsent(listItem.getType(), 0.0);
                data.getTotalPerTransactionType().put(listItem.getType(), data.getTotalPerTransactionType().get(listItem.getType()) + listItem.getTotal());

                continue;
            }

            // Totals
            data.incrementGrandTotal(listItem.getTotal());
            data.incrementUnitsSold(items.getBasket().size());

            // Total per category
            for (StockData stockData : items.getBasket()) {
                data.getTotalPerCategory().putIfAbsent(stockData.getCategory(), 0.0);
                data.getTotalPerCategory().put(stockData.getCategory(), data.getTotalPerCategory().get(stockData.getCategory()) + stockData.getPrice());
            }

            // Total per payment type
            for (Map.Entry<PaymentType, Double> e : items.getTender().entrySet()) {
                data.getTotalPerPaymentType().putIfAbsent(e.getKey(), 0.0);
                data.getTotalPerPaymentType().put(e.getKey(), data.getTotalPerPaymentType().get(e.getKey()) + e.getValue());
            }

            // Total per transaction type
            data.getTotalPerTransactionType().putIfAbsent(listItem.getType(), 0.0);
            data.getTotalPerTransactionType().put(listItem.getType(), data.getTotalPerTransactionType().get(listItem.getType()) + listItem.getTotal());

            // Cash in draw
            double change = 0;
            if (items.getTender().containsKey(PaymentType.CASH)) {
                data.incrementCashInDraw(items.getTender().get(PaymentType.CASH));

                if (items.getTender().get(PaymentType.CASH) > items.getBasketTotal())
                    change = items.getTender().get(PaymentType.CASH) - items.getBasketTotal();
            }
            data.subtractCashInDraw(change);
        }

        String unformatted = POSApplication.gson.toJson(data);
        System.out.println(unformatted);
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();
            String items = unformatted.replaceAll("\"", "\\\\\"");
            StringEntity requestEntity = new StringEntity(
                    "{"
                            + "\"data\": \"" + items
                            + "\"}",
                    ContentType.APPLICATION_JSON);

            HttpPost postMethod = new HttpPost("http://localhost:5001/print/xread");
            postMethod.setEntity(requestEntity);

            HttpResponse rawResponse = httpClient.execute(postMethod);
            showError(null);
        } catch (Exception e) {
            e.printStackTrace();
            showError(e.getMessage());
        }
    }
}
