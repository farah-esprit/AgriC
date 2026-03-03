package services;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class GeminiService {

    private static final String API_KEY = "";
    private static final String API_URL = "https://api.cohere.com/v2/chat";

    public static String poserQuestion(String question) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setDoOutput(true);
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(40000);

            String q = question
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", " ")
                    .replace("\r", "");

            String body = "{"
                    + "\"model\": \"command-r-08-2024\","
                    + "\"messages\": ["
                    + "  {\"role\": \"user\", \"content\": \"" + q + "\"}"
                    + "]"
                    + "}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            InputStream is = code == 200 ? conn.getInputStream() : conn.getErrorStream();

            Scanner sc = new Scanner(is, StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder();
            while (sc.hasNextLine()) sb.append(sc.nextLine());
            sc.close();
            conn.disconnect();

            String json = sb.toString();
            if (code != 200) return "❌ Erreur API (" + code + ") : " + json;

            // Parser réponse Cohere v2
            int idx = json.indexOf("\"text\":\"");
            if (idx == -1) return "❌ Réponse inattendue : " + json;

            idx += 8;
            StringBuilder result = new StringBuilder();
            boolean esc = false;
            for (int i = idx; i < json.length(); i++) {
                char c = json.charAt(i);
                if (esc) {
                    switch (c) {
                        case 'n'  -> result.append('\n');
                        case 't'  -> result.append('\t');
                        case '"'  -> result.append('"');
                        case '\\' -> result.append('\\');
                        default   -> result.append(c);
                    }
                    esc = false;
                } else if (c == '\\') {
                    esc = true;
                } else if (c == '"') {
                    break;
                } else {
                    result.append(c);
                }
            }
            return result.toString().trim();

        } catch (Exception e) {
            return "❌ Erreur : " + e.getMessage();
        }
    }

    public static String analyserStock(String donneesStock) {
        return poserQuestion(
                "Tu es un assistant agricole expert. " +
                        "Voici les donnees de stock : " + donneesStock +
                        " Analyse en francais, identifie ruptures et stocks bas, donne 3 recommandations."
        );
    }
}