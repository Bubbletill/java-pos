package store.bubbletill.pos.controllers;

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
import store.bubbletill.pos.POSApplication;
import store.bubbletill.pos.data.ApiRequestData;
import store.bubbletill.pos.data.StockData;
import store.bubbletill.pos.data.Transaction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @FXML private Pane declareOpeningFloat;

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
        transactionLabel.setText("" + app.transaction);
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

        if (app.currentTransaction == null) {
            app.transaction++;
            app.currentTransaction = new Transaction(app.transaction);
            transactionLabel.setText("" + app.transaction);
            transStartedButtons.setVisible(true);
            preTransButtons.setVisible(false);
        }

        app.currentTransaction.addToBasket(stockData);
        basketListView.getItems().add("[" + POSApplication.getCategory(stockData.getCategory()).getMessage() + "] " + stockData.getDescription() + " - £" + POSApplication.df.format(stockData.getPrice()));

        resetItemInputFields();
    }

    private void resetItemInputFields() {
        categoryInputLabel.setVisible(false);
        categoryInputField.setText("");
        categoryInputField.setDisable(false);
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

    // Declare Opening Float

    @FXML
    private void onOpeningFloatNoButtonPress() {
        declareOpeningFloat.setVisible(false);
        mainHome.setVisible(true);
        errorPane.setVisible(false);
        app.cashInDraw = 0;
    }

    @FXML private void onOpeningFloatYesButtonPress() {
        showError("Error: Feature not yet available.");
    }

}
