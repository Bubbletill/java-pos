package store.bubbletill.pos.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class StartupErrorController {

    @FXML private Label errorLabel;
    private String errMessage;

    public StartupErrorController(String errMessage) {
        this.errMessage = errMessage;
    }

    @FXML
    private void initialize() {
        errorLabel.setText(errMessage);
    }

    @FXML
    private void onRestartButtonPressed() {
        System.exit(0);
    }

}
