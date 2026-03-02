package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class BadWordsFilterService {

    private static final String PURGO_MALUM_URL =
            "https://www.purgomalum.com/service/json?text=";

    private static final HttpClient httpClient =
            HttpClient.newHttpClient();

    // ✅ LISTE BAD WORDS FRANÇAIS
    private static final String[] FRENCH_BAD_WORDS = {
            "con", "connard", "connasse",
            "merde", "putain", "salope",
            "salaud", "enculé", "encule",
            "bordel", "bite", "cul",
            "fdp", "fils de pute",
            "abruti", "débile", "idiot",
            "fuck"
    };

    public String filterText(String text) {

        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        // 1️⃣ FILTRAGE LOCAL FR
        String filtered = filterFrenchWords(text);

        // 2️⃣ FILTRAGE API EN
        filtered = filterWithAPI(filtered);

        return filtered;
    }

    // ================= LOCAL FR FILTER =================
    private String filterFrenchWords(String text) {

        String result = text;

        for (String badWord : FRENCH_BAD_WORDS) {

            // (?i) = case insensitive
            result = result.replaceAll("(?i)\\b" + badWord + "\\b", "****");
        }

        return result;
    }

    // ================= API EN FILTER =================
    private String filterWithAPI(String text) {

        try {
            String encodedText =
                    URLEncoder.encode(text, StandardCharsets.UTF_8);

            String url = PURGO_MALUM_URL + encodedText;

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();

            HttpResponse<String> response =
                    httpClient.send(request,
                            HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {

                String json = response.body();

                int start =
                        json.indexOf("\"result\":\"") + 10;

                int end =
                        json.lastIndexOf("\"}");

                if (start > 9 && end > start) {
                    return json.substring(start, end)
                            .replace("\\\"", "\"");
                }
            }

        } catch (Exception e) {
            System.err.println("⚠ PurgoMalum error: "
                    + e.getMessage());
        }

        return text;
    }

    public boolean containsBadWords(String text) {
        return !filterText(text).equals(text);
    }

    public void filterThread(entities.ForumThread thread) {
        thread.setTitre(filterText(thread.getTitre()));
        thread.setContenu(filterText(thread.getContenu()));
    }

    public void filterResponse(entities.Response response) {
        response.setContenu(filterText(response.getContenu()));
    }
}
