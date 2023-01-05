package store.bubbletill.pos.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import store.bubbletill.pos.POSApplication;

public class AdminController {

    private final POSApplication app = POSApplication.getInstance();
    private final POSContainerController controller = POSContainerController.getInstance();

    // Admin View
    @FXML private Button noSaleButton;
    @FXML private Button postVoidButton;
    @FXML private Button xReadButton;
    @FXML private Button resyncButton;
    @FXML private Button backButton;

    @FXML
    private void initialize() {
        noSaleButton.setOnAction(e -> { onNoSalePress(); });
        postVoidButton.setOnAction(e -> { onPostVoidPress(); });
        xReadButton.setOnAction(e -> { onXReadPress(); });
        resyncButton.setOnAction(e -> { onResyncPress(); });
        backButton.setOnAction(e -> { onBackPress(); });
    }

    private void onNoSalePress() { }

    private void onPostVoidPress() { }

    private void onXReadPress() {
        if (!app.managerLoginRequest("X Read")) {
            controller.showError("Insufficient permission.");
            return;
        }

        controller.performXRead();
    }

    private void onResyncPress() {
        if (!app.managerLoginRequest("Database Resync")) {
            controller.showError("Insufficient permission.");
            return;
        }

        Alert receiptQuestion = new Alert(Alert.AlertType.WARNING);
        POSApplication.buzzer("double");
        receiptQuestion.setTitle("Confirm");
        receiptQuestion.setHeaderText("Are you sure you want to resync the database? This may take a long time.");
        receiptQuestion.setContentText("Please select an option.");
        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        receiptQuestion.getButtonTypes().setAll(yesButton, new ButtonType("No", ButtonBar.ButtonData.NO));
        receiptQuestion.showAndWait().ifPresent(buttonType -> {
            if (buttonType == yesButton) {
                controller.showError("Database resync in progress. Please wait.", false);

                if (!app.syncDatabase()) {
                    controller.showError("Failed to resync database. Please contact an administrator.");
                    return;
                }

                controller.showError(null);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Database resynced successfully.", ButtonType.OK);
                alert.setTitle("Success");
                alert.setHeaderText("Success");
                alert.showAndWait();
            }
        });
    }

    private void onBackPress() {
        controller.updateSubScene("mainmenu");
    }

}
