package store.bubbletill.pos.controllers;

import com.google.gson.JsonArray;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
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
import store.bubbletill.pos.data.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class POSHomeController {

    // Different views
    @FXML private Pane mainHome;
    @FXML private Pane preTransButtons;
    @FXML private Pane transStartedButtons;
    @FXML private Label categoryInputLabel;
    @FXML private TextField categoryInputField;
    @FXML private TextField itemcodeInputField;
    @FXML private ListView<String> basketListView;

    @FXML private Pane resumeTrans;
    @FXML private ListView<String> resumeList;

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

    private POSApplication app;
    private Timer dateTimeTimer;
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    @FXML
    private void initialize() {
        app = POSApplication.getInstance();

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
        preTransButtons.setVisible(true);
        transStartedButtons.setVisible(false);
        resumeTrans.setVisible(false);

        dateTimeTimer = new Timer();
        dateTimeTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    dateTimeLabel.setText(dtf.format(LocalDateTime.now()));
                });
            }
        }, 0, 5000);

        statusLabel.setText((app.workingOnline ? "Online" : "Offline"));
        registerLabel.setText("" + app.register);
        transactionLabel.setText("" + app.transNo);
        operatorLabel.setText(app.operator.getOperatorId());

        basketListView.setCellFactory(cell -> new ListCell<>() {
            @Override
            protected void updateItem(String s, boolean b) {
                super.updateItem(s, b);

                if (s != null) {
                    setText(s);

                    setFont(Font.font(20));
                }
            }
        });

        if (app.transaction != null) {
            for (StockData stockData : app.transaction.getBasket()) {
                basketListView.getItems().add("[" + POSApplication.getCategory(stockData.getCategory()).getMessage() + "] " + stockData.getDescription() + " - £" + POSApplication.df.format(stockData.getPrice()));
            }
        }
    }

    private void showError(String error) {
        if (error == null) {
            errorPane.setVisible(false);
            return;
        }

        errorPane.setVisible(true);
        errorLabel.setText(error);
        POSApplication.buzzer("double");
    }

    // Main Home

    @FXML
    private void onLogoutButtonPress() {
        try {
            dateTimeTimer.cancel();
            FXMLLoader fxmlLoader = new FXMLLoader(POSApplication.class.getResource("login.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1920, 1080);
            Stage stage = (Stage) dateTimeLabel.getScene().getWindow();
            stage.setTitle("Bubbletill POS 22.0.1");
            stage.setScene(scene);
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private void onCategoryInputKeyPress(KeyEvent event) {
        if (event.getCode().toString().equals("ESCAPE")) {
            resetItemInputFields();
            return;
        }

        if (!event.getCode().toString().equals("ENTER"))
            return;

        showError(null);
        if (categoryInputField.getText() == null || categoryInputField.getText().isEmpty()) {
            showError("Please enter a category.");
            return;
        }

        ApiRequestData data;
        try {
            data = POSApplication.getCategory(Integer.parseInt(categoryInputField.getText()));
        } catch (Exception e) {
            showError("Category should be a number.");
            return;
        }

        if (!data.isSuccess()) {
            showError(data.getMessage());
            return;
        }

        categoryInputLabel.setText(data.getMessage().toUpperCase());
        categoryInputLabel.setVisible(true);
        categoryInputField.setDisable(true);
        itemcodeInputField.requestFocus();
    }

    @FXML private void onItemcodeInputKeyPress(KeyEvent event) {
        if (event.getCode().toString().equals("ESCAPE")) {
            resetItemInputFields();
            return;
        }
        if (!event.getCode().toString().equals("ENTER"))
            return;

        showError(null);
        if (itemcodeInputField.getText() == null || itemcodeInputField.getText().isEmpty()) {
            showError("Please enter an item code.");
            return;
        }

        ApiRequestData data;
        try {
            data = POSApplication.getItem(Integer.parseInt(categoryInputField.getText()), Integer.parseInt(itemcodeInputField.getText()));
        } catch (Exception e) {
            showError("Item code should be a number.");
            return;
        }

        if (!data.isSuccess()) {
            showError(data.getMessage());
            return;
        }

        StockData stockData = POSApplication.gson.fromJson(data.getMessage(), StockData.class);

        if (app.transaction == null) {
            app.transNo++;
            app.transaction = new Transaction(app.transNo);
            transactionLabel.setText("" + app.transNo);
            transStartedButtons.setVisible(true);
            preTransButtons.setVisible(false);
        }

        app.transaction.addToBasket(stockData);
        basketListView.getItems().add("[" + POSApplication.getCategory(stockData.getCategory()).getMessage() + "] " + stockData.getDescription() + " - £" + POSApplication.df.format(stockData.getPrice()) + "\n" + stockData.getCategory() + " / " + stockData.getItemCode());

        resetItemInputFields();
    }

    private void resetItemInputFields() {
        categoryInputLabel.setVisible(false);
        categoryInputField.setText("");
        categoryInputField.setDisable(false);
        categoryInputField.setEditable(true);
        itemcodeInputField.setText("");
        categoryInputField.requestFocus();
    }

    @FXML
    private void onTenderButtonPress() {
        showError("Error: Feature not yet available.");
    }

    @FXML
    private void onSuspendButtonPress() {
        app.suspendTransaction();
    }

    @FXML
    private void onResumeButtonPress() {
        mainHome.setVisible(false);
        resumeTrans.setVisible(true);
        resumeList.getItems().clear();

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
                resumeList.getItems().add(sld.getUsid() + " - " + sld.getDate().toString() + " - " + sld.getReg() + " - " + sld.getOper());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError(e.getMessage());
            return;
        }
    }

    // Declare Opening Float

    @FXML
    private void onOpeningFloatNoButtonPress() {
        declareOpeningFloat.setVisible(false);
        mainHome.setVisible(true);
        errorPane.setVisible(false);
        app.cashInDraw = 0;
    }


    @FXML
    private void onOpeningFloatYesButtonPress() {
        dofPrompt.setVisible(false);
        dofDeclare.setVisible(true);
    }

    @FXML
    private void onDofSubmitPress() {
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
            app.cashInDraw += Integer.parseInt(dof2p.getText()) * 0.02;
            app.cashInDraw += Integer.parseInt(dof1p.getText()) * 0.01;
        } catch (Exception e) {
            showError("Please populate all fields with a valid number.");
            return;
        }

        declareOpeningFloat.setVisible(false);
        //errorPane. setVisible(false);
        showError("Cash in draw: " + app.cashInDraw);
        mainHome.setVisible(true);
        app.floatKnown = true;

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

        for (StockData stockData : resumeData.getBasket()) {
            basketListView.getItems().add("[" + POSApplication.getCategory(stockData.getCategory()).getMessage() + "] " + stockData.getDescription() + " - £" + POSApplication.df.format(stockData.getPrice()) + "\n" + stockData.getCategory() + " / " + stockData.getItemCode());
        }
    }

    @FXML
    private void onRtBackButtonPress() {
        resumeTrans.setVisible(false);
        mainHome.setVisible(true);
    }

    @FXML
    private void onRtResumeButtonPress() {
        resumeTransaction(Integer.parseInt(resumeList.getSelectionModel().getSelectedItem().split(" ")[0]));
    }

}
