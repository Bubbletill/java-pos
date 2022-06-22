package store.bubbletill.pos.views;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Pane;
import store.bubbletill.commons.BubbleView;
import store.bubbletill.pos.POSApplication;
import store.bubbletill.pos.controllers.POSHomeController;

public class AdminView implements BubbleView {

    private final POSApplication app;
    private final POSHomeController controller;
    private Pane adminPane;


    public AdminView(POSApplication app, POSHomeController controller, Pane adminPane, Button noSaleButton,
                     Button postVoidButton, Button xReadButton, Button resyncButton, Button backButton) {
        this.app = app;
        this.controller = controller;
        this.adminPane = adminPane;

        noSaleButton.setOnAction(e -> { onNoSalePress(); });
        postVoidButton.setOnAction(e -> { onPostVoidPress(); });
        xReadButton.setOnAction(e -> { onXReadPress(); });
        backButton.setOnAction(e -> { onBackPress(); });
        resyncButton.setOnAction(e -> { onResyncPress(); });
    }


    @Override
    public void show() {
        adminPane.setVisible(true);
    }

    @Override
    public void hide() {
        adminPane.setVisible(false);
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

        if (!app.syncDatabase()) {
            controller.showError("Failed to resync database. Please contact an administrator.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Database resynced successfully.", ButtonType.OK);
        alert.setTitle("Success");
        alert.setHeaderText("Success");
        alert.showAndWait();
    }

    private void onBackPress() {
        hide();
        controller.homeTenderView.show();
    }
}
