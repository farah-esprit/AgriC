package service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class VoiceAuthService {

    private static final String PYTHON_SCRIPT =
            "C:\\Users\\souei\\AgriC\\python-service\\voice_auth.py";;

    private static final String PYTHON_CMD = "C:\\Users\\souei\\AgriC\\python-service\\.venv\\Scripts\\python.exe";
    private static final double THRESHOLD   = 0.82;
    private static final int    TIMEOUT_SEC = 30;

    // ─────────────────────────────────────────
    // CLASSE RÉSULTAT — sans Jackson
    // ─────────────────────────────────────────
    public static class VoiceResult {
        public boolean success;
        public boolean authenticated;
        public double  similarity;
        public double  threshold;
        public String  username  = "";
        public String  message   = "";
        public String  confidence = "";
        public String  rawOutput = "";

        @Override
        public String toString() {
            return String.format(
                    "VoiceResult{success=%b, auth=%b, similarity=%.3f, user=%s, msg=%s}",
                    success, authenticated, similarity, username, message
            );
        }
    }

    // ─────────────────────────────────────────
    // MÉTHODES SIMPLES — utilisées par Login/Register
    // ─────────────────────────────────────────

    /** Vérifie si un utilisateur a une empreinte vocale enregistrée */
    public boolean hasVoicePrint(String username) {
        String output = runPython("list", "--json");
        return output.contains("\"" + username + "\"");
    }

    /** Enregistre l'empreinte vocale — retourne true si succès */
    public boolean enroll(String username, File audioFile) {
        String output = runPython("enroll",
                "--username", username,
                "--file",     audioFile.getAbsolutePath(),
                "--json");
        return output.contains("\"status\": \"success\"")
                || output.contains("\"status\":\"success\"");
    }

    /** Vérifie l'identité — retourne true si voix reconnue */
    public boolean verify(String username, File audioFile) {
        String output = runPython("verify",
                "--username",  username,
                "--file",      audioFile.getAbsolutePath(),
                "--threshold", String.valueOf(THRESHOLD),
                "--json");
        return output.contains("\"authenticated\": true")
                || output.contains("\"authenticated\":true");
    }

    // ─────────────────────────────────────────
    // MÉTHODES COMPLÈTES — retournent VoiceResult
    // ─────────────────────────────────────────

    public VoiceResult enrollFromFile(String username, File audioFile) {
        System.out.println("🎤 [VoiceAuth] Enrollment : " + username);
        String output = runPython("enroll",
                "--username", username,
                "--file",     audioFile.getAbsolutePath(),
                "--json");
        return parseOutput(output);
    }

    public VoiceResult enrollFromMic(String username, int nSamples) {
        String output = runPython("enroll",
                "--username", username,
                "--samples",  String.valueOf(nSamples),
                "--json");
        return parseOutput(output);
    }

    public VoiceResult verifyFromFile(String username, File audioFile) {
        System.out.println("🔍 [VoiceAuth] Vérification : " + username);
        String output = runPython("verify",
                "--username",  username,
                "--file",      audioFile.getAbsolutePath(),
                "--threshold", String.valueOf(THRESHOLD),
                "--json");
        return parseOutput(output);
    }

    public VoiceResult verifyFromMic(String username) {
        String output = runPython("verify",
                "--username",  username,
                "--threshold", String.valueOf(THRESHOLD),
                "--json");
        return parseOutput(output);
    }

    public VoiceResult identifyFromFile(File audioFile) {
        String output = runPython("identify",
                "--file", audioFile.getAbsolutePath(),
                "--json");
        return parseOutput(output);
    }

    public VoiceResult testSystem() {
        String output = runPython("test", "--json");
        return parseOutput(output);
    }

    public VoiceResult deleteUser(String username) {
        String output = runPython("delete",
                "--username", username,
                "--json");
        return parseOutput(output);
    }

    // ─────────────────────────────────────────
    // VÉRIFICATION INSTALLATION
    // ─────────────────────────────────────────
    public boolean isPythonAvailable() {
        try {
            Process p = new ProcessBuilder(PYTHON_CMD, "--version")
                    .redirectErrorStream(true).start();
            p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isScriptAvailable() {
        return new File(PYTHON_SCRIPT).exists();
    }

    // ─────────────────────────────────────────
    // LANCER PYTHON — méthode centrale
    // ─────────────────────────────────────────
    private String runPython(String... args) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(PYTHON_CMD);
            cmd.add(PYTHON_SCRIPT);
            for (String arg : args) cmd.add(arg);

            Process p = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();

            StringBuilder out = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    System.out.println("[Python] " + line);
                    out.append(line).append("\n");
                }
            }
            p.waitFor(TIMEOUT_SEC, java.util.concurrent.TimeUnit.SECONDS);
            return out.toString();

        } catch (Exception e) {
            System.err.println("VoiceAuth error: " + e.getMessage());
            return "";
        }
    }

    // ─────────────────────────────────────────
    // PARSER JSON SANS JACKSON
    // ─────────────────────────────────────────
    private VoiceResult parseOutput(String output) {
        VoiceResult result = new VoiceResult();
        result.rawOutput = output;

        String json = "";
        for (String line : output.split("\n")) {
            if (line.startsWith("JSON_RESULT:")) {
                json = line.substring("JSON_RESULT:".length()).trim();
                break;
            }
        }

        if (json.isEmpty()) {
            result.success = false;
            result.message = "Pas de résultat JSON";
            return result;
        }

        result.success       = json.contains("\"status\": \"success\"")
                || json.contains("\"status\":\"success\"");
        result.authenticated = json.contains("\"authenticated\": true")
                || json.contains("\"authenticated\":true");
        result.similarity    = extractDouble(json, "similarity");
        result.threshold     = extractDouble(json, "threshold");
        result.username      = extractString(json, "username");
        result.message       = extractString(json, "message");
        result.confidence    = extractString(json, "confidence");

        System.out.printf("✅ [VoiceAuth] Similarité=%.3f Auth=%b%n",
                result.similarity, result.authenticated);

        return result;
    }

    private String extractString(String json, String key) {
        try {
            String search = "\"" + key + "\"";
            int idx = json.indexOf(search);
            if (idx == -1) return "";
            int colon = json.indexOf(":", idx + search.length());
            if (colon == -1) return "";
            int q1 = json.indexOf("\"", colon + 1);
            if (q1 == -1) return "";
            int q2 = json.indexOf("\"", q1 + 1);
            if (q2 == -1) return "";
            return json.substring(q1 + 1, q2);
        } catch (Exception e) {
            return "";
        }
    }

    private double extractDouble(String json, String key) {
        try {
            String search = "\"" + key + "\"";
            int idx = json.indexOf(search);
            if (idx == -1) return 0.0;
            int colon = json.indexOf(":", idx + search.length());
            if (colon == -1) return 0.0;
            int start = colon + 1;
            while (start < json.length() &&
                    (json.charAt(start) == ' ' || json.charAt(start) == '\t'))
                start++;
            int end = start;
            while (end < json.length() &&
                    json.charAt(end) != ',' &&
                    json.charAt(end) != '}' &&
                    json.charAt(end) != ' ')
                end++;
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (Exception e) {
            return 0.0;
        }
    }
}