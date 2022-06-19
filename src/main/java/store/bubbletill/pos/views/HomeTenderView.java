package store.bubbletill.pos.views;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import store.bubbletill.commons.*;
import store.bubbletill.pos.POSApplication;
import store.bubbletill.pos.controllers.POSHomeController;

import java.util.Map;

public class HomeTenderView implements BubbleView {

    private final POSApplication app;
    private final POSHomeController controller;

     private final Pane mainHome;
     private final Pane preTransButtons;
     private final Pane transStartedButtons;
     private final Pane tenderButtons;
     private final Pane transModButtons;
     private final Label categoryInputLabel;
     private final TextField categoryInputField;
     private final TextField itemcodeInputField;
     private final Pane homeItemInputPane;
     private final ListView<String> basketListView;

     private final Pane homeCostsPane;
     private final Pane homeCostsTenderPane;
     private final Label homeTenderTotalLabel;
     private final Label homeTenderTenderLabel;
     private final Label homeTenderRemainLabel;

     private final Button tenderBackButton;

     private final Label transactionLabel;

    public HomeTenderView(POSApplication app, POSHomeController controller, Label transactionLabel,
                          Pane mainHome, Pane preTransButtons, Pane transStartedButtons, Pane tenderButtons,
                          Pane transModButtons, Label categoryInputLabel, TextField categoryInputField,
                          TextField itemcodeInputField, Pane homeItemInputPane, ListView<String> basketListView,
                          Pane homeCostsPane, Pane homeCostsTenderPane, Label homeTenderTotalLabel,
                          Label homeTenderTenderLabel, Label homeTenderRemainLabel, Button tenderCashButton,
                          Button tenderCardButton, Button tenderBackButton, Button tenderButton, Button itemModButton,
                          Button transModButton, Button suspendButton, Button transModVoidButton,
                          Button transModBackButton, Button logoutButton, Button homeResumeButton, Button adminButton) {

        this.app = app;
        this.controller = controller;
        this.mainHome = mainHome;
        this.preTransButtons = preTransButtons;
        this.transStartedButtons = transStartedButtons;
        this.tenderButtons = tenderButtons;
        this.transModButtons = transModButtons;
        this.categoryInputLabel = categoryInputLabel;
        this.categoryInputField = categoryInputField;
        this.itemcodeInputField = itemcodeInputField;
        this.homeItemInputPane = homeItemInputPane;
        this.basketListView = basketListView;
        this.homeCostsPane = homeCostsPane;
        this.homeCostsTenderPane = homeCostsTenderPane;
        this.homeTenderTotalLabel = homeTenderTotalLabel;
        this.homeTenderTenderLabel = homeTenderTenderLabel;
        this.homeTenderRemainLabel = homeTenderRemainLabel;
        this.tenderBackButton = tenderBackButton;
        this.transactionLabel = transactionLabel;

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
    }

    @Override
    public void show() {
        mainHome.setVisible(true);
        preTransButtons.setVisible(true);
        transStartedButtons.setVisible(false);
        tenderButtons.setVisible(false);
        transModButtons.setVisible(false);
        homeCostsPane.setVisible(true);
        homeCostsTenderPane.setVisible(false);
    }

    @Override
    public void hide() {
        mainHome.setVisible(false);
        preTransButtons.setVisible(false);
        transStartedButtons.setVisible(false);
        tenderButtons.setVisible(false);
        transModButtons.setVisible(false);
        homeCostsPane.setVisible(false);
        homeCostsTenderPane.setVisible(false);
    }

    private void onLogoutButtonPress() {
        try {
            app.dateTimeTimer.cancel();
            FXMLLoader fxmlLoader = new FXMLLoader(POSApplication.class.getResource("login.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1920, 1080);
            Stage stage = controller.getStage();
            stage.setTitle("Bubbletill POS 22.0.1");
            stage.setScene(scene);
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
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
            data = POSApplication.getCategory(Integer.parseInt(categoryInputField.getText()));
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
            data = POSApplication.getItem(Integer.parseInt(categoryInputField.getText()), Integer.parseInt(itemcodeInputField.getText()));
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
            transactionLabel.setText("" + app.transNo);
            transStartedButtons.setVisible(true);
            preTransButtons.setVisible(false);
        }

        app.transaction.addToBasket(stockData);
        basketListView.getItems().add("[" + POSApplication.getCategory(stockData.getCategory()).getMessage() + "] " + stockData.getDescription() + " - £" + Formatters.decimalFormatter.format(stockData.getPrice()) + "\n" + stockData.getCategory() + " / " + stockData.getItemCode());

        resetItemInputFields();
        homeTenderTotalLabel.setText("£" + Formatters.decimalFormatter.format(app.transaction.getBasketTotal()));
    }

    private void resetItemInputFields() {
        categoryInputLabel.setVisible(false);
        categoryInputField.setText("");
        categoryInputField.setDisable(false);
        categoryInputField.setEditable(true);
        itemcodeInputField.setText("");
        categoryInputField.requestFocus();
    }

    private void onAdminButtonPress() {
        hide();
        controller.adminView.show();
    }

    private void onTenderButtonPress() {
        tenderButtons.setVisible(true);
        homeCostsTenderPane.setVisible(true);
        transStartedButtons.setVisible(false);
        homeItemInputPane.setVisible(false);
        homeTenderRemainLabel.setText("£" + Formatters.decimalFormatter.format(app.transaction.getBasketTotal()));
    }

    private void onReturnButtonPress() { }

    private void onItemModButtonPress() { }

    private void onTransModButtonPress() {
        transModButtons.setVisible(true);
        transStartedButtons.setVisible(false);
    }

    private void onResumeButtonPress() { controller.resumeView.show(); hide(); }

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
        categoryInputLabel.requestFocus();
    }

    // Trans Mod
    private void onTmVoidButtonPress() {
        if (!app.managerLoginRequest("Transaction Void")) {
            controller.showError("Insufficient permission.");
            return;
        }

        app.transaction.setVoided(true);
        try {
            app.submit();
        } catch (Exception e) {
            e.printStackTrace();
            controller.showError(e.getMessage());
        }
    }

    private void onTmBackButtonPress() {
        transStartedButtons.setVisible(true);
        transModButtons.setVisible(false);
    }

}
