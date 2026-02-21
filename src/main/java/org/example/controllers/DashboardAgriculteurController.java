package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.concurrent.Task;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DashboardAgriculteurController {

    @FXML
    private StackPane contentPane;

    // Labels météo
    @FXML
    private Label temperatureLabel;

    @FXML
    private Label windspeedLabel;

    @FXML
    private Label weatherCodeLabel;

    // URL Open-Meteo
    private final String API_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=36.8065&longitude=10.1815&current_weather=true";

    // ================= NAVIGATION =================
    @FXML
    public void handleAccueil() {
        loadPage("/DashboardHome.fxml");
    }
    @FXML
    public void handleScanPlant() {
        loadPage("/PlantNetView.fxml"); // chemin vers ton FXML de scan
    }
    @FXML
    public void handleGoToCulture() {
        loadPage("/CultureList.fxml");
    }

    @FXML
    public void handleWeather() {
        loadWeather();
    }

    private void loadPage(String fxml) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource(fxml));
            contentPane.getChildren().setAll(page);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= MÉTÉO =================
    @FXML
    public void initialize() {
        loadWeather();
    }

    private void loadWeather() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    URL url = new URL(API_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                    );

                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(response.toString());
                    JSONObject current = json.getJSONObject("current_weather");

                    double temperature = current.getDouble("temperature");
                    double windspeed = current.getDouble("windspeed");
                    int weathercode = current.getInt("weathercode");

                    // Mise à jour des labels sur le thread JavaFX
                    javafx.application.Platform.runLater(() -> {
                        temperatureLabel.setText("🌡 Température : " + temperature + " °C");
                        windspeedLabel.setText("💨 Vitesse du vent : " + windspeed + " km/h");
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        new Thread(task).start();
    }
}