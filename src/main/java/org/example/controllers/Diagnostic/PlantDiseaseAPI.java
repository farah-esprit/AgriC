package org.example.controllers.Diagnostic;

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

    private static final String API_KEY = "2b10RB9HZ7W4Vt0kEgqDBKuD6e";
    private static final String API_URL = "https://api.plant.id/v2/identify";

    public static String detectDisease(String imagePath) throws Exception {
        byte[] imageBytes = Files.readAllBytes(Path.of(imagePath));
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        JSONObject requestJson = new JSONObject();
        requestJson.put("api_key", API_KEY);
        requestJson.put("images", new String[]{base64Image});
        requestJson.put("modifiers", new String[]{"disease_similar_images"});
        requestJson.put("plant_language", "en");
        requestJson.put("plant_details", new String[]{"common_names", "wiki_description", "disease"});

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject jsonResponse = new JSONObject(response.body());

        // Extraction des infos principales
        JSONArray suggestions = jsonResponse.getJSONArray("suggestions");
        if (suggestions.length() > 0) {
            JSONObject topSuggestion = suggestions.getJSONObject(0);
            String plantName = topSuggestion.getJSONArray("plant_details")
                    .optJSONObject(0)
                    .optJSONArray("common_names") != null
                    ? topSuggestion.getJSONArray("plant_details")
                    .getJSONObject(0)
                    .getJSONArray("common_names")
                    .optString(0)
                    : "Unknown";

            JSONObject disease = topSuggestion.optJSONObject("disease");
            String diseaseName = (disease != null) ? disease.optString("name", "No disease detected") : "No disease detected";
            String recommendation = (disease != null) ? disease.optString("treatment", "No recommendation") : "No recommendation";

            return "Plante: " + plantName + "\nMaladie: " + diseaseName + "\nRecommandation: " + recommendation;
        } else {
            return "Aucune plante ou maladie détectée.";
        }
    }
}
