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
import store.bubbletill.commons.*;
import store.bubbletill.pos.controllers.StartupErrorController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

public class POSApplication extends Application {

    public static POSApplication instance;

    public static Gson gson = new Gson();
    private Stage stage;
    public Timer dateTimeTimer;

    // General Data
    public OperatorData operator;
    public boolean workingOnline = true;
    public int store;
    public int register;
    public int transNo = 0;
    public String accessToken;
    public static String backendUrl;

    public Transaction transaction;

    // Cash data
    public double cashInDraw = -9999;
    public boolean floatKnown = false;

    // Cache info
    public HashMap<Integer, String> categories = new HashMap<>();
    public ArrayList<StockData> stock = new ArrayList<>();
    public HashMap<String, OperatorData> operators = new HashMap<>();

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

            // Backend url
            method = new HttpGet("http://localhost:5001/info/backend");
            rawResponse = httpClient.execute(method);
            out = EntityUtils.toString(rawResponse.getEntity());
            backendUrl = out;
            System.out.println("Loaded backend url " + out);

            // Transaction number
            StringEntity requestEntity = new StringEntity(
                    "{\"store\":\"" + store + "\",\"reg\":\"" + register + "\", \"token\":\"" + POSApplication.getInstance().accessToken + "\"}",
                    ContentType.APPLICATION_JSON);

            HttpPost methodPost = new HttpPost(backendUrl + "/pos/today");
            methodPost.setEntity(requestEntity);
            rawResponse = httpClient.execute(methodPost);
            out = EntityUtils.toString(rawResponse.getEntity());
            transNo = Integer.parseInt(out);
            System.out.println("Loaded transaction number");

            // Load categories
            requestEntity = new StringEntity(
                    "{\"token\":\"" + POSApplication.getInstance().accessToken + "\"}",
                    ContentType.APPLICATION_JSON);

            methodPost = new HttpPost(backendUrl + "/stock/categories");
            methodPost.setEntity(requestEntity);
            rawResponse = httpClient.execute(methodPost);
            out = EntityUtils.toString(rawResponse.getEntity());
            CategoryData[] categoryData = gson.fromJson(out, CategoryData[].class);
            for (CategoryData c : categoryData) {
                categories.put(c.getId(), c.getDescription());
            }
            System.out.println("Loaded categories");

            // Load stock
            requestEntity = new StringEntity(
                    "{\"token\":\"" + POSApplication.getInstance().accessToken + "\"}",
                    ContentType.APPLICATION_JSON);

            methodPost = new HttpPost(backendUrl + "/stock/items");
            methodPost.setEntity(requestEntity);
            rawResponse = httpClient.execute(methodPost);
            out = EntityUtils.toString(rawResponse.getEntity());
            StockData[] stockData = gson.fromJson(out, StockData[].class);
            stock.addAll(Arrays.asList(stockData));
            System.out.println("Loaded stock");

            // Load operators
            requestEntity = new StringEntity(
                    "{\"store\": \"" + store + "\", \"token\":\"" + POSApplication.getInstance().accessToken + "\"}",
                    ContentType.APPLICATION_JSON);

            methodPost = new HttpPost(backendUrl + "/bo/listoperators");
            methodPost.setEntity(requestEntity);
            rawResponse = httpClient.execute(methodPost);
            out = EntityUtils.toString(rawResponse.getEntity());
            OperatorData[] operatorData = gson.fromJson(out, OperatorData[].class);
            for (OperatorData o : operatorData) {
                operators.put(o.getOperatorId(), o);
            }
            System.out.println("Loaded operators");

        } catch (Exception e) {
            e.printStackTrace();
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

    public static void buzzer(String type) {
        new Thread(() -> {
            try {
                HttpClient httpClient = HttpClientBuilder.create().build();
                HttpGet method = new HttpGet("http://localhost:5001/buzzer/" + type);

                httpClient.execute(method);
            } catch (Exception e) {
                System.out.println("Buzzer failed: " + e.getMessage());
            }
        }).start();
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

    public ApiRequestData getCategory(int number) {
        if (categories.containsKey(number))
            return new ApiRequestData(true, categories.get(number));

        try {
            HttpClient httpClient = HttpClientBuilder.create().build();

            StringEntity requestEntity = new StringEntity(
                    "{\"category\":\"" + number + "\", \"token\":\"" + POSApplication.getInstance().accessToken + "\"}",
                    ContentType.APPLICATION_JSON);

            HttpPost postMethod = new HttpPost(backendUrl + "/stock/category");
            postMethod.setEntity(requestEntity);

            HttpResponse rawResponse = httpClient.execute(postMethod);
            return POSApplication.gson.fromJson(EntityUtils.toString(rawResponse.getEntity()), ApiRequestData.class);
        } catch (Exception e) {
            System.out.println("Category get failed: " + e.getMessage());
            return new ApiRequestData(false, "Internal server error. Try again later.");
        }
    }

    public ApiRequestData getItem(int category, int code) {
        if (stock.stream().anyMatch(i -> i.getCategory() == category && i.getItemCode() == code))
            return new ApiRequestData(true, gson.toJson(stock.stream().filter(i -> i.getCategory() == category && i.getItemCode() == code).findFirst().get()));

        try {
            HttpClient httpClient = HttpClientBuilder.create().build();

            StringEntity requestEntity = new StringEntity(
                    "{\"category\":\"" + category + "\",\"code\":\"" + code + "\", \"token\":\"" + POSApplication.getInstance().accessToken + "\"}",
                    ContentType.APPLICATION_JSON);

            HttpPost postMethod = new HttpPost(backendUrl + "/stock/item");
            postMethod.setEntity(requestEntity);

            HttpResponse rawResponse = httpClient.execute(postMethod);
            String out = EntityUtils.toString(rawResponse.getEntity());
            ApiRequestData data = POSApplication.gson.fromJson(out, ApiRequestData.class);
            if (!data.isSuccess()) {
                return data;
            }

            StockData stockData = POSApplication.gson.fromJson(out, StockData.class);
            stock.add(stockData);

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
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();

            String items = gson.toJson(transaction).replaceAll("\"", "\\\\\"");

            StringEntity requestEntity = new StringEntity(
                    "{"
                            + "\"store\": \"" + store
                            + "\", \"date\": \"" + Formatters.dateFormatter.format(LocalDateTime.now())
                            + "\", \"reg\": \"" + register
                            + "\", \"oper\": \"" + operator.getOperatorId()
                            + "\", \"items\": \"" + items
                            + "\", \"total\": \"" + transaction.getBasketTotal()
                            + "\", \"token\": \"" + accessToken
                            + "\"}",
                    ContentType.APPLICATION_JSON);

            HttpPost postMethod = new HttpPost(backendUrl + "/pos/suspend");
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

            if (transaction.getTender().get(PaymentType.CASH) > transaction.getBasketTotal())
                change = transaction.getTender().get(PaymentType.CASH) - transaction.getBasketTotal();
        }

        /*if (floatKnown) {
            if (cashInDraw - change < 0) {
                throw new NegativeCashException("There is not enough cash to provide the change.");
            }
        }*/

        HttpClient httpClient = HttpClientBuilder.create().build();

        String items = POSApplication.gson.toJson(transaction).replaceAll("\"", "\\\\\"");

        StringEntity requestEntity = new StringEntity(
                "{"
                        + "\"store\": \"" + store
                        + "\",\"date\": \"" + Formatters.dateFormatter.format(LocalDateTime.now())
                        + "\", \"time\": \"" + Formatters.timeFormatter.format(LocalDateTime.now())
                        + "\", \"register\": \"" + register
                        + "\", \"oper\": \"" + operator.getOperatorId()
                        + "\", \"trans\": \"" + transaction.getId()
                        + "\", \"type\": \"" + transaction.determineTransType().toString()
                        + "\", \"items\": \"" + items
                        + "\", \"total\": \"" + Formatters.decimalFormatter.format(transaction.getBasketTotal())
                        + "\", \"primary_method\": \"" + transaction.getPrimaryTender().toString()
                        + "\", \"token\": \"" + accessToken
                        + "\"}",
                ContentType.APPLICATION_JSON);

        HttpPost postMethod = new HttpPost(backendUrl + "/pos/submit");
        postMethod.setEntity(requestEntity);

        HttpResponse rawResponse = httpClient.execute(postMethod);

        cashInDraw -= change;

        if (change != 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "£" + Formatters.decimalFormatter.format(change), ButtonType.OK);
            alert.setTitle("Change");
            alert.setHeaderText("Please give the following change:");
            alert.showAndWait();
        }

        if (transaction.determineTransType() != TransactionType.VOID) {
            Alert receiptQuestion = new Alert(Alert.AlertType.CONFIRMATION);
            receiptQuestion.setTitle("Receipt");
            receiptQuestion.setHeaderText("Would the customer like a receipt?");
            receiptQuestion.setContentText("Please select an option.");
            ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
            receiptQuestion.getButtonTypes().setAll(yesButton, new ButtonType("No", ButtonBar.ButtonData.NO));
            receiptQuestion.showAndWait().ifPresent(buttonType -> {
                if (buttonType == yesButton) {
                    printReceipt(store, register, transNo, operator.getOperatorId(), Formatters.dateTimeFormatter.format(LocalDateTime.now()), POSApplication.gson.toJson(transaction), "NA", false);
                }
            });
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

    public void printReceipt(int store, int register, int transNo, String operator, String formattedDate, String unformattedItems, String paydata, boolean copy) {
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();
            String items = unformattedItems.replaceAll("\"", "\\\\\"");
            StringEntity requestEntity = new StringEntity(
                    "{"
                            + "\"store\": \"" + store
                            + "\", \"reg\": \"" + register
                            + "\", \"trans\": \"" + transNo
                            + "\", \"oper\": \"" + operator
                            + "\", \"datetime\": \"" + formattedDate
                            + "\", \"items\": \"" + items
                            + "\", \"paydata\": \"" + paydata
                            + "\", \"copy\": " + copy
                            + "}",
                    ContentType.APPLICATION_JSON);

            HttpPost postMethod = new HttpPost("http://localhost:5001/print/receipt");
            postMethod.setEntity(requestEntity);

            HttpResponse rawResponse = httpClient.execute(postMethod);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                    "{\"user\":\"" + username + "\",\"password\":\"" + password + "\", \"token\":\"" + POSApplication.getInstance().accessToken + "\"}",
                    ContentType.APPLICATION_JSON);

            HttpPost postMethod = new HttpPost(backendUrl + "/pos/login");
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

