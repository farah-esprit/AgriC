package controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.Animation;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DashboardAgriculteurController {

    @FXML private Label temperatureLabel;
    @FXML private Label windspeedLabel;
    @FXML private Label weatherCodeLabel;

    private final String API_URL = "https://api.open-meteo.com/v1/forecast?latitude=36.8065&longitude=10.1815&current_weather=true";

    @FXML
    public void initialize() {
        loadWeather();
        startAutoRefresh();
    }

    private void loadWeather() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    URL url = new URL(API_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setRequestMethod("GET");

                    int responseCode = conn.getResponseCode();
                    System.out.println("Response Code: " + responseCode);

                    if (responseCode == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        conn.disconnect();

                        JSONObject json = new JSONObject(response.toString());
                        JSONObject current = json.getJSONObject("current_weather");

                        double temperature = current.getDouble("temperature");
                        double windspeed = current.getDouble("windspeed");
                        int weathercode = current.getInt("weathercode");
                        String description = getWeatherDescription(weathercode);

                        // IMPORTANT: Mise à jour des labels dans le thread JavaFX
                        Platform.runLater(() -> {
                            temperatureLabel.setText(temperature + " °C");
                            windspeedLabel.setText(windspeed + " km/h");
                            weatherCodeLabel.setText(description);
                        });
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        temperatureLabel.setText("--");
                        windspeedLabel.setText("--");
                        weatherCodeLabel.setText("Erreur API ⚠");
                    });
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private String getWeatherDescription(int code) {
        return switch (code) {
            case 0 -> "Ensoleillé ☀";
            case 1,2,3 -> "Partiellement nuageux ⛅";
            case 45,48 -> "Brouillard 🌫";
            case 51,53,55 -> "Pluie légère 🌦";
            case 61,63,65 -> "Pluie 🌧";
            case 71,73,75 -> "Neige ❄";
            case 95 -> "Orage ⛈";
            default -> "Conditions inconnues";
        };
    }

    private void startAutoRefresh() {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.minutes(30), e -> loadWeather())
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }
}