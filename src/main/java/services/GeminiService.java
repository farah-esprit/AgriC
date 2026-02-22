package services;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class GeminiService {

    private static final String API_KEY = "AIzaSyCNcaJa8_Zgighmk9yKCZ4fo4B4ld04dsA";
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    public static String poserQuestion(String question) {
        try {
            URL url = new URL(API_URL + API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String body = "{"
                    + "\"contents\": [{"
                    + "\"parts\": [{\"text\": \"" + question.replace("\"", "\\\"") + "\"}]"
                    + "}]"
                    + "}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            Scanner sc = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8);
            StringBuilder response = new StringBuilder();
            while (sc.hasNextLine()) response.append(sc.nextLine());
            sc.close();

            // Extraire le texte de la réponse JSON
            String json = response.toString();
            int start = json.indexOf("\"text\": \"") + 9;
            int end = json.indexOf("\"", start);
            // Extraire proprement
            String texte = json.substring(start, end);
            texte = texte.replace("\\n", "\n").replace("\\\"", "\"");
            return texte;

        } catch (Exception e) {
            return "❌ Erreur IA : " + e.getMessage();
        }
    }

    public static String analyserStock(String donneesStock) {
        String question = "Tu es un assistant agricole expert. "
                + "Voici les données de stock actuelles d'une application agricole :\n"
                + donneesStock
                + "\nAnalyse ces données et donne des recommandations en français. "
                + "Identifie les produits en rupture, les stocks bas, et suggère des actions.";
        return poserQuestion(question);
    }
}