package store.bubbletill.pos;

import com.google.gson.Gson;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Pair;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import store.bubbletill.pos.controllers.StartupErrorController;
import store.bubbletill.pos.data.ApiRequestData;
import store.bubbletill.pos.data.OperatorData;
import store.bubbletill.pos.data.PaymentType;
import store.bubbletill.pos.data.Transaction;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class POSApplication extends Application {

    public static POSApplication instance;

    public static Gson gson = new Gson();
    public static final DecimalFormat df = new DecimalFormat("0.00");
    private Stage stage;

    // General Data
    public OperatorData operator;
    public boolean workingOnline = true;
    public int store;
    public int register;
    public int transNo = 0;
    public String accessToken;

    public Transaction transaction;

    // Cash data
    public double cashInDraw = -9999;
    public boolean floatKnown = false;

    @Override
    public void start(Stage stage) throws IOException {
        instance = this;
        this.stage = stage;

        // Load register specific info
        try {
            // Reg number
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpGet method = new HttpGet("http://localhost:5001/info/regno");
            HttpResponse rawResponse = httpClient.execute(method);
            String out = EntityUtils.toString(rawResponse.getEntity());
            register = Integer.parseInt(out);
            System.out.println("Loaded regno");

            // Store number
            method = new HttpGet("http://localhost:5001/info/storeno");
            rawResponse = httpClient.execute(method);
            out = EntityUtils.toString(rawResponse.getEntity());
            store = Integer.parseInt(out);
            System.out.println("Loaded storeno");

            // Access token
            method = new HttpGet("http://localhost:5001/info/accesstoken");
            rawResponse = httpClient.execute(method);
            out = EntityUtils.toString(rawResponse.getEntity());
            accessToken = out;
            System.out.println("Loaded access token");

            // Transaction number
            StringEntity requestEntity = new StringEntity(
                    "{\"store\":\"" + store + "\",\"reg\":\"" + register + "\", \"token\":\"" + POSApplication.getInstance().accessToken + "\"}",
                    ContentType.APPLICATION_JSON);

            HttpPost methodPost = new HttpPost("http://localhost:5000/pos/today");
            methodPost.setEntity(requestEntity);
            rawResponse = httpClient.execute(methodPost);
            out = EntityUtils.toString(rawResponse.getEntity());
            transNo = Integer.parseInt(out);
            System.out.println("Loaded transaction number");
        } catch (Exception e) {
            System.out.println("Reg get failed: " + e.getMessage());
            launchError(stage, "Failed to retrieve register information: " + e.getMessage());
            return;
        }

        FXMLLoader fxmlLoader = new FXMLLoader(POSApplication.class.getResource("login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1920, 1080);
        stage.setTitle("Bubbletill POS 22.0.1");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.show();
    }

    public static POSApplication getInstance() { return instance;}

    public static void main(String[] args) {
        launch();
    }

    public static void buzzer(String type) {
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpGet method = new HttpGet("http://localhost:5001/buzzer/" + type);
            httpClient.execute(method);
        } catch (Exception e) {
            System.out.println("Buzzer failed: " + e.getMessage());
        }
    }

    public static void launchError(Stage stage, String message) {
        buzzer("double");
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(POSApplication.class.getResource("startuperror.fxml"));
            StartupErrorController sec = new StartupErrorController(message);
            fxmlLoader.setController(sec);

            Scene scene = new Scene(fxmlLoader.load());
            stage.setTitle("Bubbletill POS 22.0.1 - LAUNCH ERROR");
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setScene(scene);
            stage.setAlwaysOnTop(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ApiRequestData getCategory(int number) {
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();

            StringEntity requestEntity = new StringEntity(
                    "{\"category\":\"" + number + "\", \"token\":\"" + POSApplication.getInstance().accessToken + "\"}",
                    ContentType.APPLICATION_JSON);

            HttpPost postMethod = new HttpPost("http://localhost:5000/stock/category");
            postMethod.setEntity(requestEntity);

            HttpResponse rawResponse = httpClient.execute(postMethod);
            return POSApplication.gson.fromJson(EntityUtils.toString(rawResponse.getEntity()), ApiRequestData.class);
        } catch (Exception e) {
            System.out.println("Category get failed: " + e.getMessage());
            return new ApiRequestData(false, "Internal server error. Try again later.");
        }
    }

    public static ApiRequestData getItem(int category, int code) {
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();

            StringEntity requestEntity = new StringEntity(
                    "{\"category\":\"" + category + "\",\"code\":\"" + code + "\", \"token\":\"" + POSApplication.getInstance().accessToken + "\"}",
                    ContentType.APPLICATION_JSON);

            HttpPost postMethod = new HttpPost("http://localhost:5000/stock/item");
            postMethod.setEntity(requestEntity);

            HttpResponse rawResponse = httpClient.execute(postMethod);
            String out = EntityUtils.toString(rawResponse.getEntity());
            ApiRequestData data = POSApplication.gson.fromJson(out, ApiRequestData.class);
            if (!data.isSuccess()) {
                return data;
            }

            return new ApiRequestData(true, out);
        } catch (Exception e) {
            System.out.println("Item get failed: " + e.getMessage());
            return new ApiRequestData(false, "Internal server error. Try again later.");
        }
    }


    public void reset() {
        transaction = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(POSApplication.class.getResource("poshome.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1920, 1080);
            stage.setScene(scene);
            stage.setTitle("Bubbletill POS 22.0.1");
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void suspendTransaction() {
        transNo--;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yy");
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();

            String items = gson.toJson(transaction).replaceAll("\"", "\\\\\"");

            StringEntity requestEntity = new StringEntity(
                    "{\"store\":\"" + store + "\",\"date\":\"" + dtf.format(LocalDateTime.now()) + "\", \"reg\":" + register + ", \"oper\": \"" + operator.getOperatorId() + "\", \"items\": \"" + items + "\", \"token\": \"" + accessToken + "\"}",
                    ContentType.APPLICATION_JSON);

            HttpPost postMethod = new HttpPost("http://localhost:5000/pos/suspend");
            postMethod.setEntity(requestEntity);
            HttpResponse rawResponse = httpClient.execute(postMethod);

            reset();
        } catch (Exception e) {
            System.out.println("Suspend failed: " + e.getMessage());
        }
    }

    public void submit() throws Exception {
        double change = 0;

        if (transaction.getTender().containsKey(PaymentType.CASH)) {
            cashInDraw += transaction.getTender().get(PaymentType.CASH);
            change = transaction.getTender().get(PaymentType.CASH) - transaction.getBasketTotal();
        }

        /*if (floatKnown) {
            if (cashInDraw - change < 0) {
                throw new NegativeCashException("There is not enough cash to provide the change.");
            }
        }*/

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yy");
        HttpClient httpClient = HttpClientBuilder.create().build();

        String items = POSApplication.gson.toJson(transaction).replaceAll("\"", "\\\\\"");

        StringEntity requestEntity = new StringEntity(
                "{"
                        + "\"store\": \"" + store
                        + "\",\"date\": \"" + dtf.format(LocalDateTime.now())
                        + "\", \"time\": \"" + transaction.getTime()
                        + "\", \"register\": \"" + register
                        + "\", \"oper\": \"" + operator.getOperatorId()
                        + "\", \"trans\": \"" + transaction.getId()
                        + "\", \"items\": \"" + items
                        + "\", \"total\": \"" + df.format(transaction.getBasketTotal())
                        + "\", \"primary_method\": \"" + transaction.getPrimaryTender().toString()
                        + "\", \"token\": \"" + accessToken
                        + "\"}",
                ContentType.APPLICATION_JSON);

        HttpPost postMethod = new HttpPost("http://localhost:5000/pos/submit");
        postMethod.setEntity(requestEntity);

        HttpResponse rawResponse = httpClient.execute(postMethod);

        cashInDraw -= change;

        if (change != 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "£" + df.format(change), ButtonType.OK);
            alert.setTitle("Change");
            alert.setHeaderText("Please give the following change:");
            alert.showAndWait();
        }

        reset();
    }

    public boolean checkAndSubmit() throws Exception {
        if (transaction.isTenderComplete()) {
            submit();
            return true;
        }

        return false;
    }

    public boolean managerLoginRequest(String actionId) {
        if (operator.isManager())
            return true;

        // Create the custom dialog.
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Authentication");
        dialog.setHeaderText("Manager permission is required. Please sign-in.");


        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("User ID");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        grid.add(new Label("User ID"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password"), 0, 1);
        grid.add(password, 1, 1);

        // Enable/Disable login button depending on whether a username was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        username.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    password.requestFocus();
                }
            }
        });

        // Do some validation (using the Java 8 lambda syntax).
        password.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(username::requestFocus);

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), password.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        boolean isManager = result.filter(stringStringPair -> isManager(stringStringPair.getKey(), stringStringPair.getValue())).isPresent();

        if (isManager && transaction != null)
            transaction.addManagerAction(actionId, result.get().getKey());

        return isManager;
    }

    private boolean isManager(String username, String password) {
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();

            StringEntity requestEntity = new StringEntity(
                    "{\"user\":\"" + username + "\",\"password\":\"" + password + "\", \"token\":\"" + POSApplication.getInstance().accessToken + "\"}",
                    ContentType.APPLICATION_JSON);

            HttpPost postMethod = new HttpPost("http://localhost:5000/pos/login");
            postMethod.setEntity(requestEntity);

            HttpResponse rawResponse = httpClient.execute(postMethod);
            String out = EntityUtils.toString(rawResponse.getEntity());

            ApiRequestData data = POSApplication.gson.fromJson(out, ApiRequestData.class);

            if (!data.isSuccess()) {
                return false;
            }

            OperatorData od = POSApplication.gson.fromJson(out, OperatorData.class);

            return od.isManager();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

