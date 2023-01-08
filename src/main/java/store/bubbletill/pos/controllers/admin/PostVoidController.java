package store.bubbletill.pos.controllers.admin;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import store.bubbletill.pos.POSApplication;
import store.bubbletill.pos.controllers.POSContainerController;

public class PostVoidController {

    private final POSApplication app = POSApplication.getInstance();
    private final POSContainerController controller = POSContainerController.getInstance();

    @FXML private TextField registerForm;
    @FXML private TextField transForm;
    @FXML private DatePicker dateForm;

    @FXML
    private void initialize() {
        registerForm.requestFocus();
    }

    @FXML
    protected void onBackPress() {
        controller.updateSubScene("admin");
    }

    @FXML
    protected void onAcceptPress() {

    }

    @FXML
    protected void onLastPress() {

    }

}
