package store.bubbletill.pos.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import store.bubbletill.pos.POSApplication;
import store.bubbletill.pos.data.ApiRequestData;
import store.bubbletill.pos.data.OperatorData;

public class LoginController {

    @FXML
    private TextField userIdForm;

    @FXML
    private PasswordField passwordForm;

    @FXML
    private Pane errorPane;

    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        System.out.println("LoginController Initialized");
        userIdForm.requestFocus();
        errorPane.setVisible(false);
    }

    private void showError(String error) {
        if (error == null) {
            errorPane.setVisible(false);
            return;
        }

        errorPane.setVisible(true);
        errorLabel.setText(error);
    }

    @FXML
    protected void onLoginButtonClick() {
        showError(null);
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();

            StringEntity requestEntity = new StringEntity(
                    "{\"user\":\"" + userIdForm.getText() + "\",\"password\":\"" + passwordForm.getText() + "\", \"token\":\"" + POSApplication.getInstance().accessToken + "\"}",
                    ContentType.APPLICATION_JSON);

            HttpPost postMethod = new HttpPost("http://localhost:5000/pos/login");
            postMethod.setEntity(requestEntity);

            HttpResponse rawResponse = httpClient.execute(postMethod);
            String out = EntityUtils.toString(rawResponse.getEntity());

            ApiRequestData data = POSApplication.gson.fromJson(out, ApiRequestData.class);

            if (!data.isSuccess()) {
                showError("Error: " + data.getMessage());
                POSApplication.buzzer("double");
                return;
            }

            POSApplication.getInstance().operator = POSApplication.gson.fromJson(out, OperatorData.class);

            FXMLLoader fxmlLoader = new FXMLLoader(POSApplication.class.getResource("poshome.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1920, 1080);
            Stage stage = (Stage) userIdForm.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Bubbletill POS 22.0.1");
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        } catch(Exception e) {
            e.printStackTrace();
            showError("Internal error: " + e.getMessage());
        }
    }

    @FXML
    protected void onUIDKeyPress(KeyEvent e) {
        if (e.getCode().toString().equals("ENTER")) {
            passwordForm.requestFocus();
        }
    }

    @FXML
    protected void onPasswordKeyPress(KeyEvent e) {
        if (e.getCode().toString().equals("ENTER")) {
            onLoginButtonClick();
        }
    }
}