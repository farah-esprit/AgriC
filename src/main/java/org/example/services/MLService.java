package org.example.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MLService {

    private static final String ML_URL = "http://localhost:8000/predict";

    public static String predictDisease(
            String plantType,
            String leafColor,
            double spotSize,
            double humidity,
            double temperature) {

        try {
            String json = String.format("""
                {
                  "Plant_Type": "%s",
                  "Leaf_Color": "%s",
                  "Leaf_Spot_Size": %f,
                  "Humidity": %f,
                  "Temperature": %f
                }
                """,
                    plantType,
                    leafColor,
                    spotSize,
                    humidity,
                    temperature);

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ML_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.body();

        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur ML";
        }
    }
}
