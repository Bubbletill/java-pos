package store.bubbletill.pos.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import store.bubbletill.commons.*;
import store.bubbletill.pos.POSApplication;

import java.time.LocalDateTime;
import java.util.Map;

public class MainMenuController {

    private final POSApplication app = POSApplication.getInstance();
    private final POSContainerController controller = POSContainerController.getInstance();

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

    @FXML private Button tenderButton;
    @FXML private Button tenderCashButton;
    @FXML private Button tenderCardButton;
    @FXML private Button tenderBackButton;
    @FXML private Button itemModButton;
    @FXML private Button transModButton;
    @FXML private Button suspendButton;
    @FXML private Button transModVoidButton;
    @FXML private Button transModBackButton;
    @FXML private Button logoutButton;
    @FXML private Button adminButton;

    @FXML private Button homeResumeButton;

    @FXML private Pane saleInfoPane;
    @FXML private Pane tenderInfoPane;

    @FXML
    private void initialize() {
        mainHome.setVisible(true);
        preTransButtons.setVisible(true);
        transStartedButtons.setVisible(false);
        tenderButtons.setVisible(false);
        transModButtons.setVisible(false);
        homeCostsPane.setVisible(true);
        homeCostsTenderPane.setVisible(false);

        saleInfoPane.setVisible(true);
        tenderInfoPane.setVisible(false);

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

        tenderCashButton.setOnAction(e -> { onTenderTypePress(PaymentType.CASH); });
        tenderCardButton.setOnAction(e -> { onTenderTypePress(PaymentType.CARD); });
        tenderButton.setOnAction(e -> { onTenderButtonPress(); });
        tenderBackButton.setOnAction(e -> { onTenderBackButtonPress(); });

        itemModButton.setOnAction(e -> { onItemModButtonPress(); });
        transModButton.setOnAction(e -> { onTransModButtonPress(); });
        suspendButton.setOnAction(e -> { onSuspendButtonPress(); });

        categoryInputField.setOnKeyReleased(this::onCategoryInputKeyPress);
        itemcodeInputField.setOnKeyReleased(this::onItemcodeInputKeyPress);

        transModBackButton.setOnAction(e -> { onTmBackButtonPress(); });
        transModVoidButton.setOnAction(e -> { onTmVoidButtonPress(); });

        homeResumeButton.setOnAction(e -> { onResumeButtonPress(); });

        logoutButton.setOnAction(e -> { onLogoutButtonPress(); });

        adminButton.setOnAction(e -> { onAdminButtonPress(); });

        // Resume trans?
        if (app.transaction != null) {
            app.transaction.log("Transaction resumed at " + Formatters.dateTimeFormatter.format(LocalDateTime.now()));
            for (StockData stockData : app.transaction.getBasket()) {
                basketListView.getItems().add("[" + app.getCategory(stockData.getCategory()).getMessage() + "] " + stockData.getDescription() + " - £" + Formatters.decimalFormatter.format(stockData.getPrice()));
            }
            homeTenderTotalLabel.setText("£" + Formatters.decimalFormatter.format(app.transaction.getBasketTotal()));

            transStartedButtons.setVisible(true);
            preTransButtons.setVisible(false);
        }

        resetItemInputFields();
    }

    private void onLogoutButtonPress() {
        try {
            app.dateTimeTimer.cancel();
            FXMLLoader fxmlLoader = new FXMLLoader(POSApplication.class.getResource("login.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1920, 1080);
            Stage stage = controller.getStage();
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onCategoryInputKeyPress(KeyEvent event) {
        if (event.getCode().toString().equals("ESCAPE")) {
            resetItemInputFields();
            return;
        }

        if (!event.getCode().toString().equals("ENTER"))
            return;

        controller.showError(null);
        if (categoryInputField.getText() == null || categoryInputField.getText().isEmpty()) {
            controller.showError("Please enter a category.");
            return;
        }

        ApiRequestData data;
        try {
            data = app.getCategory(Integer.parseInt(categoryInputField.getText()));
        } catch (Exception e) {
            controller.showError("Category should be a number.");
            return;
        }

        if (!data.isSuccess()) {
            controller.showError(data.getMessage());
            return;
        }

        categoryInputLabel.setText(data.getMessage().toUpperCase());
        categoryInputLabel.setVisible(true);
        categoryInputField.setDisable(true);
        itemcodeInputField.setDisable(true);
        itemcodeInputField.setDisable(false);
        itemcodeInputField.requestFocus();
    }

    private void onItemcodeInputKeyPress(KeyEvent event) {
        if (event.getCode().toString().equals("ESCAPE")) {
            resetItemInputFields();
            return;
        }
        if (!event.getCode().toString().equals("ENTER"))
            return;

        controller.showError(null);
        if (itemcodeInputField.getText() == null || itemcodeInputField.getText().isEmpty()) {
            controller.showError("Please enter an item code.");
            return;
        }

        ApiRequestData data;
        try {
            data = app.getItem(Integer.parseInt(categoryInputField.getText()), Integer.parseInt(itemcodeInputField.getText()));
        } catch (Exception e) {
            controller.showError("Item code should be a number.");
            return;
        }

        if (!data.isSuccess()) {
            controller.showError(data.getMessage());
            return;
        }

        StockData stockData = POSApplication.gson.fromJson(data.getMessage(), StockData.class);

        if (app.transaction == null) {
            app.transNo++;
            app.transaction = new Transaction(app.transNo);
            controller.transactionLabel.setText("" + app.transNo);
            transStartedButtons.setVisible(true);
            preTransButtons.setVisible(false);

            app.transaction.log("Transaction " + app.transNo + " started " + Formatters.dateTimeFormatter.format(LocalDateTime.now()));
            app.transaction.log("Transaction type of " + app.transaction.determineTransType());
        }

        app.transaction.addToBasket(stockData);
        basketListView.getItems().add("[" + app.getCategory(stockData.getCategory()).getMessage() + "] " + stockData.getDescription() + " - £" + Formatters.decimalFormatter.format(stockData.getPrice()) + "\n" + stockData.getCategory() + " / " + stockData.getItemCode());

        resetItemInputFields();
        homeTenderTotalLabel.setText("£" + Formatters.decimalFormatter.format(app.transaction.getBasketTotal()));
    }

    private void resetItemInputFields() {
        categoryInputLabel.setVisible(false);
        categoryInputField.setText("");
        categoryInputField.setDisable(false);
        itemcodeInputField.setText("");
        itemcodeInputField.setDisable(true);
        categoryInputField.requestFocus();
    }

    private void onAdminButtonPress() {
        controller.updateSubScene("admin");
    }

    private void onTenderButtonPress() {
        tenderButtons.setVisible(true);
        homeCostsTenderPane.setVisible(true);
        transStartedButtons.setVisible(false);
        homeItemInputPane.setVisible(false);
        saleInfoPane.setVisible(false);
        tenderInfoPane.setVisible(true);
        homeTenderRemainLabel.setText("£" + Formatters.decimalFormatter.format(app.transaction.getBasketTotal()));
        app.transaction.log("Going to tender, Subtotal £" + Formatters.decimalFormatter.format(app.transaction.getBasketSubTotal()));
    }

    private void onReturnButtonPress() { }

    private void onItemModButtonPress() { }

    private void onTransModButtonPress() {
        transModButtons.setVisible(true);
        transStartedButtons.setVisible(false);
    }

    private void onResumeButtonPress() {
        controller.updateSubScene("resume");
    }

    private void onSuspendButtonPress() {
        app.suspendTransaction();
    }

    // Tender
    private void onTenderTypePress(PaymentType type) {
        controller.showError(null);
        TextInputDialog dialog = new TextInputDialog("" + Formatters.decimalFormatter.format(app.transaction.getRemainingTender()));
        dialog.setTitle(type.getLocalName() + " Tender");
        dialog.setHeaderText("Please enter tender amount");
        dialog.setContentText("£");
        dialog.showAndWait().ifPresent(input -> handleTender(type, input));
    }

    private void handleTender(PaymentType type, String input) {
        double amount;
        try {
            amount = Double.parseDouble(input);
        } catch (Exception e) {
            controller.showError("Failed to tender: please enter a valid amount.");
            return;
        }

        if (type == PaymentType.CARD && amount > app.transaction.getBasketTotal()) {
            controller.showError("Failed to tender: card tender cannot be greater than basket total.");
            return;
        }

        app.transaction.addTender(type, amount);

        try {
            if (app.checkAndSubmit())
                return;

            basketListView.getItems().add(type.getLocalName() + " - £" + Formatters.decimalFormatter.format(amount));
            tenderBackButton.setText("Void Tender");
            homeTenderTenderLabel.setText("£" + Formatters.decimalFormatter.format(app.transaction.getTenderTotal()));
            homeTenderRemainLabel.setText("£" + Formatters.decimalFormatter.format((app.transaction.getBasketTotal() - app.transaction.getTenderTotal())));
        } catch (Exception e) {
            e.printStackTrace();
            app.transaction.getTender().remove(type);
            controller.showError(e.getMessage());
        }
    }

    private void onTenderBackButtonPress() {
        if (tenderBackButton.getText().equals("Void Tender")) {
            tenderBackButton.setText("Back");
            for (Map.Entry<PaymentType, Double> e : app.transaction.getTender().entrySet()) {
                String toRemove = e.getKey().getLocalName() + " - £" + Formatters.decimalFormatter.format(e.getValue());
                basketListView.getItems().removeIf(item -> item.equals(toRemove));
                basketListView.refresh();
            }
            app.transaction.voidTender();
            homeTenderTenderLabel.setText("£0.00");
            homeTenderRemainLabel.setText("£0.00");
        }

        transStartedButtons.setVisible(true);
        homeCostsTenderPane.setVisible(false);
        tenderButtons.setVisible(false);
        homeItemInputPane.setVisible(true);
        saleInfoPane.setVisible(true);
        tenderInfoPane.setVisible(false);
        categoryInputLabel.requestFocus();
    }

    // Trans Mod
    private void onTmVoidButtonPress() {

        if (!app.managerLoginRequest("Transaction Void")) {
            controller.showError("Insufficient permission.");
            return;
        }

        Alert receiptQuestion = new Alert(Alert.AlertType.WARNING);
        POSApplication.buzzer("double");
        receiptQuestion.setTitle("Confirm");
        receiptQuestion.setHeaderText("Are you sure you want to void this transaction?");
        receiptQuestion.setContentText("Please select an option.");
        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        receiptQuestion.getButtonTypes().setAll(yesButton, new ButtonType("No", ButtonBar.ButtonData.NO));
        receiptQuestion.showAndWait().ifPresent(buttonType -> {
            if (buttonType == yesButton) {
                app.transaction.setVoided(true);
                try {
                    app.submit();
                } catch (Exception e) {
                    e.printStackTrace();
                    controller.showError(e.getMessage());
                }
            }
        });
    }

    private void onTmBackButtonPress() {
        transStartedButtons.setVisible(true);
        transModButtons.setVisible(false);
    }

}
