package service;

import entities.ForumThread;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * TranslationService - Powered by Gemini AI
 * Translates titre + contenu in ONE API call per thread to avoid rate limits.
 * Uses same API key and parser as GeminiService.
 */
public class TranslationService {

    private static final String API_KEY = "REPLACE_WITH_GEMINI_API_KEY"; // same key as GeminiService
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private final Map<String, String> cache = new HashMap<>();

    public static final String FRENCH  = "fr";
    public static final String ENGLISH = "en";
    public static final String ARABIC  = "ar";
    public static final String SPANISH = "es";
    public static final String GERMAN  = "de";

    public TranslationService() {
        System.out.println("✅ TranslationService initialized (Gemini AI)");
    }

    // ====================== DETECT LANGUAGE ======================

    public String detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) return "fr";
        String t = text.toLowerCase();
        if (t.matches(".*[\u0600-\u06FF].*")) return "ar";

        long fr = count(t, "le ","la ","les ","un ","une ","des ","est ","sont ","avec ","pour ","dans ");
        long en = count(t, "the ","is ","are ","was ","were ","this ","that ","have ","with ","from ");
        long es = count(t, "el ","los ","las ","una ","está ","son ","para ","como ");
        long de = count(t, "der ","die ","das ","ein ","eine ","ist ","sind ","mit ","für ");

        long max = Math.max(Math.max(fr, en), Math.max(es, de));
        if (max == 0)  return "en";
        if (fr == max) return "fr";
        if (en == max) return "en";
        if (es == max) return "es";
        return "de";
    }

    private long count(String text, String... words) {
        long c = 0;
        for (String w : words) if (text.contains(w)) c++;
        return c;
    }

    // ====================== TRANSLATE SINGLE TEXT ======================

    public String translateText(String text, String targetLanguage) {
        if (text == null || text.trim().isEmpty()) return text;
        String src = detectLanguage(text);
        if (src.equals(targetLanguage)) return text;
        return translateText(text, src, targetLanguage);
    }

    public String translateText(String text, String sourceLang, String targetLang) {
        if (text == null || text.trim().isEmpty()) return text;
        if (sourceLang != null && sourceLang.equals(targetLang)) return text;

        String cacheKey = text.hashCode() + "_" + targetLang;
        if (cache.containsKey(cacheKey)) {
            System.out.println("💾 Translation from cache");
            return cache.get(cacheKey);
        }

        String prompt = "Translate the following text to " + getLanguageName(targetLang) + ". " +
                "Return ONLY the translated text, no explanations:\n\n" + text;

        String result = callGemini(prompt);
        if (result != null && !result.isEmpty()) {
            cache.put(cacheKey, result);
            System.out.println("✅ Translated: " + sourceLang + " → " + targetLang);
            return result;
        }
        return text;
    }

    // ====================== TRANSLATE FULL THREAD (1 API CALL) ======================

    public ForumThread translateThread(ForumThread thread, String targetLanguage) {
        if (thread == null) return null;
        try {
            String src = detectLanguage(thread.getTitre());
            if (src.equals(targetLanguage)) return thread;

            String titreKey   = thread.getTitre().hashCode()   + "_" + targetLanguage;
            String contenuKey = thread.getContenu().hashCode() + "_" + targetLanguage;

            String translatedTitre;
            String translatedContenu;

            if (cache.containsKey(titreKey) && cache.containsKey(contenuKey)) {
                // ✅ Both cached — zero API call
                translatedTitre   = cache.get(titreKey);
                translatedContenu = cache.get(contenuKey);
                System.out.println("💾 Thread from cache");
            } else {
                // ✅ Translate titre + contenu in ONE single Gemini call
                String lang   = getLanguageName(targetLanguage);
                String prompt =
                        "Translate the following two texts to " + lang + ".\n" +
                                "Reply ONLY in this exact format, nothing else:\n" +
                                "TITRE: <translated title here>\n" +
                                "CONTENU: <translated content here>\n\n" +
                                "TITRE: " + thread.getTitre() + "\n" +
                                "CONTENU: " + thread.getContenu();

                String result = callGemini(prompt);

                if (result != null && result.contains("TITRE:") && result.contains("CONTENU:")) {
                    translatedTitre   = extractSection(result, "TITRE:",   "CONTENU:");
                    translatedContenu = extractSection(result, "CONTENU:", null);
                } else {
                    translatedTitre   = thread.getTitre();
                    translatedContenu = thread.getContenu();
                }

                cache.put(titreKey,   translatedTitre);
                cache.put(contenuKey, translatedContenu);
            }

            ForumThread translated = new ForumThread();
            translated.setThreadId(thread.getThreadId());
            translated.setTitre(translatedTitre);
            translated.setContenu(translatedContenu);
            translated.setDateCreation(thread.getDateCreation());
            translated.setStatus(thread.getStatus());
            translated.setUser(thread.getUser());
            translated.setCategory(thread.getCategory());
            translated.setTags(thread.getTags());
            translated.setViews(thread.getViews());
            translated.setLikes(thread.getLikes());
            translated.setLikedBy(thread.getLikedBy());

            System.out.println("✅ Thread translated successfully");
            return translated;

        } catch (Exception e) {
            System.err.println("❌ Error translating thread: " + e.getMessage());
            return thread;
        }
    }

    private String extractSection(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        if (start == -1) return "";
        start += startMarker.length();
        int end = endMarker != null ? text.indexOf(endMarker, start) : text.length();
        if (end == -1) end = text.length();
        return text.substring(start, end).trim();
    }

    public String translateThreadTitle(ForumThread thread, String targetLanguage) {
        if (thread == null || thread.getTitre() == null) return null;
        return translateText(thread.getTitre(), targetLanguage);
    }

    public String translateThreadContent(ForumThread thread, String targetLanguage) {
        if (thread == null || thread.getContenu() == null) return null;
        return translateText(thread.getContenu(), targetLanguage);
    }

    public boolean needsTranslation(String text, String targetLanguage) {
        if (text == null || text.trim().isEmpty()) return false;
        return !detectLanguage(text).equals(targetLanguage);
    }

    // ====================== GEMINI CALL ======================

    private String callGemini(String prompt) {
        try {
            String jsonBody = "{\"contents\":[{\"parts\":[{\"text\":" + escapeJson(prompt) + "}]}]}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) return extractText(response.body());
            System.err.println("❌ Gemini error: " + response.statusCode());

        } catch (Exception e) {
            System.err.println("❌ Gemini call error: " + e.getMessage());
        }
        return null;
    }

    private String extractText(String json) {
        try {
            String marker = "\"text\": \"";
            int start = json.indexOf(marker);
            if (start == -1) { marker = "\"text\":\""; start = json.indexOf(marker); }
            if (start == -1) return null;
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
            return result.toString().trim();
        } catch (Exception e) { return null; }
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

    // ====================== HELPERS ======================

    private String getLanguageName(String code) {
        switch (code) {
            case "fr": return "French";
            case "ar": return "Arabic";
            case "es": return "Spanish";
            case "de": return "German";
            default:   return "English";
        }
    }

    public Map<String, String> getSupportedLanguages() {
        Map<String, String> langs = new HashMap<>();
        langs.put(FRENCH,  "Français");
        langs.put(ENGLISH, "English");
        langs.put(ARABIC,  "العربية");
        langs.put(SPANISH, "Español");
        langs.put(GERMAN,  "Deutsch");
        return langs;
    }

    public void clearCache() {
        cache.clear();
        System.out.println("🗑️ Translation cache cleared");
    }

    public int getCacheSize() { return cache.size(); }
}
