package service;

import entities.MLResponse;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MLService {

    private static final String ML_URL = "http://127.0.0.1:8000/predict";

    public static MLResponse predictDisease(
            String plantType,
            String leafColor,
            double spotSize,
            double humidity,
            double temperature) {

        try {
            // Préparer le JSON
            String json = String.format("""
                    {
                        "Plant_Type": "%s",
                        "Leaf_Color": "%s",
                        "Leaf_Spot_Size": %f,
                        "Humidity": %f,
                        "Temperature": %f
                    }
                    """, plantType, leafColor, spotSize, humidity, temperature);

            // Afficher le JSON envoyé
            System.out.println("JSON envoyé à FastAPI : " + json);

            // Requête HTTP POST
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ML_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            // Afficher la réponse pour debug
            System.out.println("Réponse FastAPI : " + response.body());

            JSONObject obj = new JSONObject(response.body());

            // Vérifier si FastAPI renvoie une erreur
            if (obj.has("error")) {
                System.err.println("Erreur IA : " + obj.getString("error"));
                return null;
            }

            // Récupérer Disease_Status et Confidence avec protection
            String disease = obj.optString("Disease_Status", "Inconnu");
            double confidence = obj.optDouble("Confidence", 0.0);

            return new MLResponse(disease, confidence);

        } catch (Exception e) {
            System.err.println("Erreur lors de l'appel à FastAPI : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
