package store.bubbletill.pos;

import com.google.gson.Gson;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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

import java.io.IOException;

public class POSApplication extends Application {

    public static POSApplication instance;

    public static Gson gson = new Gson();

    // General Data
    public OperatorData operator;
    public boolean workingOnline = true;
    public int store;
    public int register;
    public int transaction = 0;
    public String accessToken;

    // Cash data
    public double cashInDraw = -9999;

    @Override
    public void start(Stage stage) throws IOException {
        instance = this;

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
            transaction = Integer.parseInt(out);
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
}