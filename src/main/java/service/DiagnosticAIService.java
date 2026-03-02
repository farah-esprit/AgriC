package service;

import org.json.JSONObject;
import utils.ConfigLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service d'appel au modèle IA de diagnostic (script Python ou API).
 * Config dans config.properties :
 * - diagnostic.ai.script.path = chemin vers le script Python (ex: C:/Users/souei/Downloads/IA/IA/predict.py)
 * - diagnostic.ai.api.url = URL d'une API REST (alternative au script)
 */
public class DiagnosticAIService {

    private static final String CONFIG_SCRIPT = "diagnostic.ai.script.path";
    private static final String CONFIG_API_URL = "diagnostic.ai.api.url";
    private static final int TIMEOUT_SECONDS = 60;

    /**
     * Résultat de l'analyse IA.
     */
    public static class DiagnosticAIResult {
        private final String diseaseDetected;
        private final double confidence;
        private final String rapportIa;

        public DiagnosticAIResult(String diseaseDetected, double confidence, String rapportIa) {
            this.diseaseDetected = diseaseDetected != null ? diseaseDetected : "Non déterminé";
            this.confidence = Math.max(0, Math.min(100, confidence));
            this.rapportIa = rapportIa != null ? rapportIa : "";
        }

        public String getDiseaseDetected() { return diseaseDetected; }
        public double getConfidence() { return confidence; }
        public String getRapportIa() { return rapportIa; }
    }

    /**
     * Lance l'analyse IA à partir des champs du formulaire.
     * Si le script ou l'API est configuré et fonctionne, retourne le résultat ; sinon simulation.
     */
    public DiagnosticAIResult runDiagnostic(String etat, String symptomes, String plantType,
                                            String leafColor, String humidity, String temperature) {
        String scriptPath = ConfigLoader.get(CONFIG_SCRIPT).trim();
        String apiUrl = ConfigLoader.get(CONFIG_API_URL).trim();

        if (!apiUrl.isEmpty()) {
            DiagnosticAIResult fromApi = callApi(apiUrl, etat, symptomes, plantType, leafColor, humidity, temperature);
            if (fromApi != null) return fromApi;
        }
        if (!scriptPath.isEmpty()) {
            DiagnosticAIResult fromScript = runScript(scriptPath, etat, symptomes, plantType, leafColor, humidity, temperature);
            if (fromScript != null) return fromScript;
        }

        return fallbackResult(etat, symptomes);
    }

    /**
     * Appel API REST POST avec JSON.
     */
    private DiagnosticAIResult callApi(String apiUrl, String etat, String symptomes, String plantType,
                                       String leafColor, String humidity, String temperature) {
        try {
            JSONObject body = new JSONObject();
            body.put("etat", etat);
            body.put("symptomes", symptomes);
            body.put("plantType", plantType);
            body.put("leafColor", leafColor);
            body.put("humidity", humidity);
            body.put("temperature", temperature);

            HttpURLConnection conn = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            conn.setReadTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) return null;

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }

            JSONObject json = new JSONObject(response.toString());
            String disease = json.optString("disease", json.optString("diseaseDetected", ""));
            double confidence = json.optDouble("confidence", 0);
            String report = json.optString("rapportIa", json.optString("report", ""));
            return new DiagnosticAIResult(disease, confidence, report);
        } catch (Exception e) {
            System.err.println("❌ DiagnosticAIService (API) : " + e.getMessage());
            return null;
        }
    }

    /**
     * Exécute le script Python du projet IA.
     * Attendu : le script accepte des arguments (ou stdin) et imprime une ligne JSON sur stdout :
     * {"disease": "...", "confidence": 0.xx, "rapportIa": "..."}
     */
    private DiagnosticAIResult runScript(String scriptPath, String etat, String symptomes, String plantType,
                                         String leafColor, String humidity, String temperature) {
        try {
            Path path = resolveScriptPath(scriptPath);
            if (!path.toFile().exists()) {
                System.err.println("❌ Script IA introuvable : " + path.toAbsolutePath());
                return null;
            }

            List<List<String>> commands = Arrays.asList(
                    Arrays.asList("python"),
                    Arrays.asList("py", "-3")
            );
            for (List<String> base : commands) {
                DiagnosticAIResult result = runScriptWithInterpreter(
                        base, path, etat, symptomes, plantType, leafColor, humidity, temperature
                );
                if (result != null) return result;
            }
            System.err.println("❌ DiagnosticAIService (script) : impossible d'executer le script avec python/py");
            return null;
        } catch (Exception e) {
            System.err.println("❌ DiagnosticAIService (script) : " + e.getMessage());
            return null;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static Path resolveScriptPath(String scriptPath) {
        Path raw = Paths.get(scriptPath);
        return raw.isAbsolute() ? raw : raw.toAbsolutePath().normalize();
    }

    private DiagnosticAIResult runScriptWithInterpreter(List<String> interpreter, Path scriptPath,
                                                        String etat, String symptomes, String plantType,
                                                        String leafColor, String humidity, String temperature) {
        try {
            List<String> command = new java.util.ArrayList<>(interpreter);
            command.add(scriptPath.toAbsolutePath().toString());
            command.addAll(Arrays.asList(
                    "--etat", safe(etat),
                    "--symptomes", safe(symptomes),
                    "--plantType", safe(plantType),
                    "--leafColor", safe(leafColor),
                    "--humidity", safe(humidity),
                    "--temperature", safe(temperature)
            ));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.directory(scriptPath.getParent() != null ? scriptPath.getParent().toFile() : null);

            Process process = pb.start();
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return null;
            }

            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) out.append(line).append("\n");
            }

            String output = out.toString().trim();
            if (output.isEmpty()) return null;

            String lastLine = output;
            int lastNewline = lastLine.lastIndexOf('\n');
            if (lastNewline >= 0) lastLine = lastLine.substring(lastNewline + 1).trim();
            if (lastLine.isEmpty()) return null;

            JSONObject json = new JSONObject(lastLine);
            String disease = json.optString("disease", json.optString("diseaseDetected", ""));
            double confidence = json.optDouble("confidence", 0);
            if (confidence > 0 && confidence <= 1) confidence *= 100;
            String report = json.optString("rapportIa", json.optString("report", ""));
            return new DiagnosticAIResult(disease, confidence, report);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static DiagnosticAIResult fallbackResult(String etat, String symptomes) {
        String rapport = "Analyse basée sur les observations : " + safe(etat) + ". Symptômes : " + safe(symptomes) + ".";
        return new DiagnosticAIResult("À confirmer (modèle IA non connecté)", 0, rapport);
    }
}
