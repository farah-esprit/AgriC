package service;

import entities.ForumThread;
import entities.Response;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class GeminiService1 {

    // ⚠️ Replace with your actual Gemini API key from https://aistudio.google.com/app/apikey
    private static final String API_KEY = "REPLACE_WITH_GEMINI_API_KEY";

    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public String summarizeDiscussion(ForumThread thread, List<Response> responses) {

        // ✅ Build full discussion: thread + all responses
        StringBuilder discussion = new StringBuilder();
        discussion.append("TITRE: ").append(thread.getTitre()).append("\n\n");
        discussion.append("CONTENU PRINCIPAL:\n").append(thread.getContenu()).append("\n\n");

        if (responses != null && !responses.isEmpty()) {
            discussion.append("COMMENTAIRES (").append(responses.size()).append("):\n");
            int i = 1;
            for (Response r : responses) {
                discussion.append(i++).append(". ").append(r.getContenu()).append("\n");
            }
        }

        String prompt =
                "Tu es un assistant agricole. Résume cette discussion de forum en français en 5 points clés maximum.\n" +
                        "Utilise ce format exact (avec tirets):\n" +
                        "- Problème principal: ...\n" +
                        "- Cause identifiée: ...\n" +
                        "- Solutions proposées: ...\n" +
                        "- Consensus / Conclusion: ...\n" +
                        "- Recommandation finale: ...\n\n" +
                        "Discussion à analyser:\n" + discussion;

        return callGeminiAPI(prompt);
    }

    private String callGeminiAPI(String prompt) {
        try {
            String jsonBody = "{"
                    + "\"contents\": [{"
                    + "  \"parts\": [{"
                    + "    \"text\": " + escapeJson(prompt)
                    + "  }]"
                    + "}]"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractTextFromGeminiResponse(response.body());
            } else {
                System.err.println("❌ Gemini API error: " + response.statusCode() + " — " + response.body());
                return "⚠️ Impossible de générer le résumé (erreur API " + response.statusCode() + ").";
            }

        } catch (Exception e) {
            System.err.println("❌ GeminiService error: " + e.getMessage());
            return "⚠️ Impossible de joindre l'API Gemini. Vérifiez votre connexion.";
        }
    }

    private String extractTextFromGeminiResponse(String json) {
        try {
            String marker = "\"text\": \"";
            int start = json.indexOf(marker);
            if (start == -1) {
                marker = "\"text\":\"";
                start = json.indexOf(marker);
            }
            if (start == -1) {
                System.err.println("⚠️ 'text' field not found in response");
                return "⚠️ Résumé non disponible.";
            }

            start += marker.length();

            StringBuilder result = new StringBuilder();
            int i = start;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    if      (next == 'n')  result.append('\n');
                    else if (next == '"')  result.append('"');
                    else if (next == '\\') result.append('\\');
                    else if (next == 't')  result.append('\t');
                    else                   result.append(next);
                    i += 2;
                } else if (c == '"') {
                    break;
                } else {
                    result.append(c);
                    i++;
                }
            }

            String text = result.toString().trim();
            if (!text.isEmpty()) return text;

        } catch (Exception e) {
            System.err.println("⚠️ Could not parse Gemini response: " + e.getMessage());
        }
        return "⚠️ Résumé non disponible.";
    }

    private String escapeJson(String text) {
        return "\"" + text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }
}
