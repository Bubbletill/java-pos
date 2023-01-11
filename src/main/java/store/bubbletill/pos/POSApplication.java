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
import javafx.scene.image.Image;
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
import store.bubbletill.commons.*;
import store.bubbletill.pos.controllers.StartupErrorController;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

public class POSApplication extends Application {

    private static POSApplication instance;

    public static Gson gson = new Gson();
    public Stage stage;
    public Timer dateTimeTimer;
    public DatabaseManager databaseManager;

    // General Data
    public OperatorData operator;
    public boolean workingOnline = true;
    public int transNo = 0;
    public LocalData localData;

    public Transaction transaction;

    // Cash data
    public double cashInDraw = -9999;
    public boolean floatKnown = false;

    // Cache info
    public HashMap<Integer, String> categories = new HashMap<>();
    public ArrayList<StockData> stock = new ArrayList<>();
    public HashMap<String, OperatorData> operators = new HashMap<>();

    @Override
    public void start(Stage initStage) throws IOException {
        Stage splashStage = launchSplash(initStage);
        instance = this;

        new Thread(() -> {
            // Load register specific info
            try {
                Reader dataReader = Files.newBufferedReader(Paths.get("C:\\bubbletill\\data.json"));
                localData = gson.fromJson(dataReader, LocalData.class);
                databaseManager = new DatabaseManager(localData.getDbUsername(), localData.getDbPassword(), localData);

                transNo = databaseManager.getTransactionNumber();
                if (transNo == -50) {
                    throw new Exception("Failed to get transaction number from database.");
                }

                if (!syncDatabase()) { // Load categories, stock and operators
                    Platform.runLater(() -> launchError(new Stage(), "Failed to launch POS: Database failed to sync. Please contact your system administrator."));
                } else {
                    Platform.runLater(this::postInit);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> launchError(new Stage(), "Failed to launch POS: " + (e.getMessage().startsWith("C:") ? "data.json doesn't exist or is corrupted." : e.getLocalizedMessage())));
            } finally {
                if (splashStage != null) { Platform.runLater(splashStage::close); }
            }
        }).start();
    }

    @Override
    public void stop() throws Exception {
        if (dateTimeTimer != null)
            dateTimeTimer.cancel();
        System.out.println("POS shutting down");
    }

    public static POSApplication getInstance() { return instance;}

    public static void main(String[] args) {
        launch();
    }

    private Stage launchSplash(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(POSApplication.class.getResource("splash.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 600, 200);
            stage.setScene(scene);
            stage.setAlwaysOnTop(true);
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.initStyle(StageStyle.UNDECORATED);
            stage.show();

            return stage;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void postInit() {
        try {
            Stage primaryStage = new Stage(StageStyle.DECORATED);
            this.stage = primaryStage;
            FXMLLoader fxmlLoader = new FXMLLoader(POSApplication.class.getResource("login.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1920, 1080);
            primaryStage.setTitle("Bubbletill POS 22.0.1");
            primaryStage.getIcons().add(new Image(POSApplication.class.getResourceAsStream("icon.png")));
            primaryStage.setScene(scene);
            primaryStage.initStyle(StageStyle.UNDECORATED);
            primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            primaryStage.setX(0);
            primaryStage.setY(0);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void buzzer(String type) {

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

    public boolean syncDatabase() {
        System.out.println("Syncing database");
        try {
            ArrayList<CategoryData> categoryData = databaseManager.getCategories();
            ArrayList<StockData> stockData = databaseManager.getStock();
            ArrayList<OperatorData> operatorData = databaseManager.getOperators();

            if (categoryData == null || stockData == null || operatorData == null)
                return false;

            for (CategoryData cd : categoryData)
                categories.put(cd.getId(), cd.getDescription());
            System.out.println(categories);


            stock.addAll(stockData);
            System.out.println(stock);

            for (OperatorData od : databaseManager.getOperators())
                operators.put(od.getId(), od);
            System.out.println(operators);

            System.out.println("Sync complete");
            return true;
        } catch (Exception e) {
            System.out.println("Sync fail");
            e.printStackTrace();
            return false;
        }
    }


    public ApiRequestData getCategory(int number) {
        if (categories.containsKey(number))
            return new ApiRequestData(true, categories.get(number));

        return new ApiRequestData(false, "Invalid category.");
    }

    public ApiRequestData getItem(int category, int code) {
        if (stock.stream().anyMatch(i -> i.getCategory() == category && i.getItemCode() == code))
            return new ApiRequestData(true, gson.toJson(stock.stream().filter(i -> i.getCategory() == category && i.getItemCode() == code).findFirst().get()));

        return new ApiRequestData(false, "Invalid item.");
    }


    public void reset() {
        transaction = null;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(POSApplication.class.getResource("poscontainer.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1920, 1080);
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void suspendTransaction() {
        transNo--;
        transaction.log("Transaction suspended at " + Formatters.dateTimeFormatter.format(LocalDateTime.now()));
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();

            String items = gson.toJson(transaction).replaceAll("\"", "\\\\\"");

            StringEntity requestEntity = new StringEntity(
                    "{"
                            + "\"store\": \"" + localData.getStore()
                            + "\", \"date\": \"" + Formatters.dateFormatter.format(LocalDateTime.now())
                            + "\", \"reg\": \"" + localData.getReg()
                            + "\", \"oper\": \"" + operator.getId()
                            + "\", \"items\": \"" + items
                            + "\", \"total\": \"" + transaction.getBasketTotal()
                            + "\", \"token\": \"" + localData.getToken()
                            + "\"}",
                    ContentType.APPLICATION_JSON);

            HttpPost postMethod = new HttpPost(localData.getBackend() + "/pos/suspend");
            postMethod.setEntity(requestEntity);
            HttpResponse rawResponse = httpClient.execute(postMethod);

            reset();
        } catch (Exception e) {
            System.out.println("Suspend failed: " + e.getMessage());
        }
    }

    public void submit() throws Exception {
        double change = 0;
        TransactionType transType = transaction.determineTransType();

        if (transaction.getTender().containsKey(PaymentType.CASH)) {
            cashInDraw += transaction.getTender().get(PaymentType.CASH);

            if (transaction.getTender().get(PaymentType.CASH) > transaction.getBasketTotal())
                change = transaction.getTender().get(PaymentType.CASH) - transaction.getBasketTotal();
        }

        /*if (floatKnown) {
            if (cashInDraw - change < 0) {
                throw new NegativeCashException("There is not enough cash to provide the change.");
            }
        }*/

        cashInDraw -= change;

        if (change != 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "£" + Formatters.decimalFormatter.format(change), ButtonType.OK);
            alert.setTitle("Change");
            alert.setHeaderText("Please give the following change:");
            alert.showAndWait();
            transaction.log("CHANGE £" + Formatters.decimalFormatter.format(change));
        }

        if (transType.promptReceipt()) {
            Alert receiptQuestion = new Alert(Alert.AlertType.CONFIRMATION);
            receiptQuestion.setTitle("Receipt");
            receiptQuestion.setHeaderText("Would the customer like a receipt?");
            receiptQuestion.setContentText("Please select an option.");
            ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
            receiptQuestion.getButtonTypes().setAll(yesButton, new ButtonType("No", ButtonBar.ButtonData.NO));
            receiptQuestion.showAndWait().ifPresent(buttonType -> {
                if (buttonType == yesButton) {
                    printReceipt(localData.getStore(), localData.getReg(), transNo, operator.getId(), Formatters.dateTimeFormatter.format(LocalDateTime.now()), POSApplication.gson.toJson(transaction), "NA", false);
                    transaction.log("RECEIPT PRINTED");
                } else {
                    transaction.log("RECEIPT **DECLINED**");
                }
            });
        }

        transaction.log("Transaction " + (transaction.isVoided() ? "Voided" : "Completed") + " at " + Formatters.dateTimeFormatter.format(LocalDateTime.now()));

        HttpClient httpClient = HttpClientBuilder.create().build();

        String items = POSApplication.gson.toJson(transaction.getBasket()).replaceAll("\"", "\\\\\"");
        String data = POSApplication.gson.toJson(transaction.getLogs()).replaceAll("\"", "\\\\\"");
        String methods = POSApplication.gson.toJson(transaction.getTender()).replaceAll("\"", "\\\\\"");

        StringEntity requestEntity = new StringEntity(
                "{"
                        + "\"store\": \"" + localData.getStore()
                        + "\",\"date\": \"" + Formatters.dateFormatter.format(LocalDateTime.now())
                        + "\", \"time\": \"" + Formatters.timeFormatter.format(LocalDateTime.now())
                        + "\", \"register\": \"" + localData.getReg()
                        + "\", \"oper\": \"" + operator.getId()
                        + "\", \"trans\": \"" + transaction.getId()
                        + "\", \"type\": \"" + transType
                        + "\", \"basket\": \"" + items
                        + "\", \"data\": \"" + data
                        + "\", \"total\": \"" + Formatters.decimalFormatter.format(transaction.getBasketTotal())
                        + "\", \"methods\": \"" + methods
                        + "\", \"token\": \"" + localData.getToken()
                        + "\"}",
                ContentType.APPLICATION_JSON);

        HttpPost postMethod = new HttpPost(localData.getBackend() + "/pos/submit");
        postMethod.setEntity(requestEntity);

        HttpResponse rawResponse = httpClient.execute(postMethod);

        reset();
    }

    public boolean checkAndSubmit() throws Exception {
        if (transaction.isTenderComplete()) {
            submit();
            return true;
        }

        return false;
    }

    public void printReceipt(int store, int register, int transNo, String operator, String formattedDate, String unformattedItems, String paydata, boolean copy) {

    }

    public boolean managerLoginRequest(String actionId) {
        if (operator.isManager())
            return true;

        // Create the custom dialog.
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Authentication");
        dialog.setHeaderText("Manager permission is required.\nPlease sign-in to authenticate " + actionId + ".");


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
            if (operators.containsKey(username)) {
                OperatorData od = operators.get(username);
                if (!od.getPassword().equals(password)) {
                    return false;
                }

                return od.isManager();
            }

            HttpClient httpClient = HttpClientBuilder.create().build();

            StringEntity requestEntity = new StringEntity(
                    "{\"user\":\"" + username + "\",\"password\":\"" + password + "\", \"token\":\"" + localData.getToken() + "\"}",
                    ContentType.APPLICATION_JSON);

            HttpPost postMethod = new HttpPost(localData.getBackend() + "/pos/login");
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

