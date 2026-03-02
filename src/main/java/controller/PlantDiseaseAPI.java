package controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;

public class PlantDiseaseAPI {

    private static final String API_KEY = "REPLACE_WITH_PLANT_DISEASE_API_KEY";
    private static final String API_URL = "https://api.plant.id/v2/health_assessment";

    public static String detectDisease(String imagePath) throws Exception {

        byte[] imageBytes = Files.readAllBytes(Path.of(imagePath));
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        JSONObject requestJson = new JSONObject();
        requestJson.put("images", new String[]{base64Image});
        requestJson.put("language", "en");

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Api-Key", API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString()))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Response code: " + response.statusCode());
        System.out.println("Response body: " + response.body());

        JSONObject jsonResponse = new JSONObject(response.body());

        JSONObject healthAssessment = jsonResponse.getJSONObject("health_assessment");

        boolean isHealthy = healthAssessment.getBoolean("is_healthy");

        if (isHealthy) {
            return "✅ La plante est saine.";
        }

        JSONArray diseases = healthAssessment.getJSONArray("diseases");

        if (diseases.length() > 0) {

            JSONObject topDisease = diseases.getJSONObject(0);

            String diseaseName = topDisease.getString("name");
            String probability = topDisease.getDouble("probability") * 100 + "%";

            return "🌿 Maladie détectée : " + diseaseName +
                    "\n📊 Probabilité : " + probability;
        }

        return "Aucune maladie détectée.";
    }
}
