package store.bubbletill.pos.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
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
import store.bubbletill.pos.views.HomeTenderView;
import store.bubbletill.pos.views.OpeningFloatView;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class POSHomeController {

    // Different views
    @FXML private Pane mainHome;
    @FXML private Pane preTransButtons;
    @FXML private Pane transStartedButtons;
    @FXML private Pane tenderButtons;
    @FXML private Pane transModButtons;
    @FXML private Label categoryInputLabel;
    @FXML private TextField categoryInputField;
    @FXML private TextField itemcodeInputField;
    @FXML private Pane homeItemInputPane;
    @FXML private ListView<String> basketListView;

    @FXML private Pane homeCostsPane;
    @FXML private Pane homeCostsTenderPane;
    @FXML private Label homeTenderTotalLabel;
    @FXML private Label homeTenderTenderLabel;
    @FXML private Label homeTenderRemainLabel;

    @FXML private Button tenderCashButton;
    @FXML private Button tenderCardButton;
    @FXML private Button tenderBackButton;

    @FXML private Pane resumeTrans;
    @FXML private TableView<SuspendedListData> resumeTable;

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

    // Top status bar
    @FXML private Label dateTimeLabel;
    @FXML private Label statusLabel;
    @FXML private Label registerLabel;
    @FXML private Label transactionLabel;
    @FXML private Label operatorLabel;
    @FXML private Pane errorPane;
    @FXML private Label errorLabel;

    // Views
    private HomeTenderView homeTenderView;
    private OpeningFloatView openingFloatView;

    private POSApplication app;

    @FXML
    private void initialize() {
        app = POSApplication.getInstance();

        // Setup the views!
        homeTenderView = new HomeTenderView(app, this, (Stage) dateTimeLabel.getScene().getWindow(),
                transactionLabel, mainHome, preTransButtons, transStartedButtons, tenderButtons, transModButtons,
                categoryInputLabel, categoryInputField, itemcodeInputField, homeItemInputPane, basketListView,
                homeCostsPane, homeCostsTenderPane, homeTenderTotalLabel, homeTenderTenderLabel, homeTenderRemainLabel,
                tenderCashButton, tenderCardButton, tenderBackButton);

        openingFloatView = new OpeningFloatView()

        dofPrompt.setVisible(true);
        dofDeclare.setVisible(false);

        if (app.cashInDraw == -9999) {
            declareOpeningFloat.setVisible(true);
            mainHome.setVisible(false);
            POSApplication.buzzer("double");
        } else {
            declareOpeningFloat.setVisible(false);
            mainHome.setVisible(true);
        }

        errorPane.setVisible(false);
        resumeTrans.setVisible(false);


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
        registerLabel.setText("" + app.register);
        transactionLabel.setText("" + app.transNo);
        operatorLabel.setText(app.operator.getOperatorId());


        // Resume trans?
        if (app.transaction != null) {
            for (StockData stockData : app.transaction.getBasket()) {
                basketListView.getItems().add("[" + POSApplication.getCategory(stockData.getCategory()).getMessage() + "] " + stockData.getDescription() + " - £" + Formatters.decimalFormatter.format(stockData.getPrice()));
            }
            homeTenderTotalLabel.setText("£" + Formatters.decimalFormatter.format(app.transaction.getBasketTotal()));
        }

        resumeTable.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("date"));
        resumeTable.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("reg"));
        resumeTable.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("oper"));
        resumeTable.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("stringTotal"));
    }

    public void showError(String error) {
        if (error == null) {
            errorPane.setVisible(false);
            return;
        }

        errorPane.setVisible(true);
        errorLabel.setText(error);
        POSApplication.buzzer("double");
    }


    @FXML
    private void onResumeButtonPress() {
        mainHome.setVisible(false);
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
            showError(e.getMessage());
        }
    }

    // Resume

    private void resumeTransaction(int uniqueSuspendedId) {
        Transaction resumeData;
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();

            StringEntity requestEntity = new StringEntity(
                    "{\"usid\":\"" + uniqueSuspendedId + "\", \"token\":\"" + POSApplication.getInstance().accessToken + "\"}",
                    ContentType.APPLICATION_JSON);

            HttpPost postMethod = new HttpPost("http://localhost:5000/pos/resume");
            postMethod.setEntity(requestEntity);

            HttpResponse rawResponse = httpClient.execute(postMethod);
            String out = EntityUtils.toString(rawResponse.getEntity());

            resumeData = POSApplication.gson.fromJson(out, Transaction.class);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        resumeTrans.setVisible(false);
        mainHome.setVisible(true);
        app.transNo++;
        app.transaction = new Transaction(app.transNo);
        app.transaction.setBasket(resumeData.getBasket());
        transactionLabel.setText("" + app.transNo);
        transStartedButtons.setVisible(true);
        preTransButtons.setVisible(false);
        homeTenderTotalLabel.setText("£" + Formatters.decimalFormatter.format(app.transaction.getBasketTotal()));

        for (StockData stockData : resumeData.getBasket()) {
            basketListView.getItems().add("[" + POSApplication.getCategory(stockData.getCategory()).getMessage() + "] " + stockData.getDescription() + " - £" + Formatters.decimalFormatter.format(stockData.getPrice()) + "\n" + stockData.getCategory() + " / " + stockData.getItemCode());
        }
    }

    @FXML
    private void onRtBackButtonPress() {
        resumeTrans.setVisible(false);
        mainHome.setVisible(true);
        showError(null);
    }

    @FXML
    private void onRtResumeButtonPress() {
        showError(null);
        if (resumeTable.getSelectionModel().getSelectedItem() == null) {
            showError("Please select a transaction to resume.");
            return;
        }
        resumeTransaction(resumeTable.getSelectionModel().getSelectedItem().getUsid());
    }

}
