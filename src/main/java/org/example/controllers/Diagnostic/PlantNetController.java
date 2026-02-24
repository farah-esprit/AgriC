package controller;

import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import javafx.scene.control.Button;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

import org.json.JSONArray;
import org.json.JSONObject;

public class PlantNetController {

    // ⚠️ IMPORTANT : Remplacez par votre nouvelle clé générée sur my.plantnet.org
    private static final String API_KEY = "2b10KGNAOKYqTeH6uQejN8qmOe";
    private File selectedFile;

    @FXML
    private Label selectedFileLabel;
    @FXML
    private Label organLabel;
    @FXML
    private Label speciesLabel;
    @FXML
    private Button identifyBtn;

    @FXML
    public void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image de plante");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.jpeg")
        );

        selectedFile = fileChooser.showOpenDialog(new Stage());

        if (selectedFile != null) {
            selectedFileLabel.setText(selectedFile.getName());
            identifyBtn.setDisable(false);
        }
    }

    @FXML
    public void identifyPlant() {
        if (selectedFile == null) return;

        identifyBtn.setDisable(true);
        organLabel.setText("Analyse en cours...");
        speciesLabel.setText("");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                HttpURLConnection conn = null;
                try {
                    String boundary = "===" + System.currentTimeMillis() + "===";

                    // ✅ MODIFICATION 1 : Clé API passée directement dans l'URL
                    String urlString = "https://my-api.plantnet.org/v2/identify/all?api-key=" + API_KEY;
                    URL url = new URL(urlString);

                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");

                    // ✅ MODIFICATION 2 : Headers simplifiés (Suppression du Authorization Header problématique)
                    conn.setRequestProperty("User-Agent", "JavaFX PlantNet Client");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                    try (OutputStream output = conn.getOutputStream();
                         PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true)) {

                        // --- PARTIE 1 : Envoi de l'organe (Obligatoire pour certaines versions de l'API) ---
                        writer.append("--").append(boundary).append("\r\n");
                        writer.append("Content-Disposition: form-data; name=\"organs\"\r\n\r\n");
                        writer.append("leaf").append("\r\n"); // On définit 'leaf' (feuille) par défaut
                        writer.flush();

                        // --- PARTIE 2 : Envoi de l'image ---
                        String mimeType = Files.probeContentType(selectedFile.toPath());
                        if (mimeType == null) mimeType = "image/jpeg";

                        writer.append("--").append(boundary).append("\r\n");
                        // Note : Le nom du champ doit être "images" pour l'endpoint /all
                        writer.append("Content-Disposition: form-data; name=\"images\"; filename=\"")
                                .append(selectedFile.getName()).append("\"\r\n");
                        writer.append("Content-Type: ").append(mimeType).append("\r\n\r\n");
                        writer.flush();

                        try (FileInputStream inputStream = new FileInputStream(selectedFile)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                output.write(buffer, 0, bytesRead);
                            }
                            output.flush();
                        }

                        writer.append("\r\n");
                        writer.append("--").append(boundary).append("--").append("\r\n");
                        writer.flush();
                    }

                    // 🔹 Lecture de la réponse
                    int responseCode = conn.getResponseCode();
                    InputStream stream = (responseCode >= 200 && responseCode < 300)
                            ? conn.getInputStream()
                            : conn.getErrorStream();

                    StringBuilder response = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                        String line;
                        while ((line = reader.readLine()) != null) response.append(line);
                    }

                    if (responseCode >= 200 && responseCode < 300) {
                        JSONObject json = new JSONObject(response.toString());
                        JSONArray results = json.optJSONArray("results");

                        if (results != null && results.length() > 0) {
                            JSONObject firstResult = results.getJSONObject(0);
                            JSONObject speciesObj = firstResult.getJSONObject("species");
                            String scientific = speciesObj.optString("scientificName", "Inconnu");

                            // Récupération de l'organe prédit par l'IA
                            JSONArray predictedOrgans = json.optJSONArray("predictedOrgans");
                            String organDetected = (predictedOrgans != null) ? predictedOrgans.optString(0, "N/A") : "N/A";

                            javafx.application.Platform.runLater(() -> {
                                organLabel.setText("Organe : " + organDetected);
                                speciesLabel.setText("Espèce : " + scientific);
                                identifyBtn.setDisable(false);
                            });
                        } else {
                            updateUI("Aucun résultat", "", false);
                        }
                    } else {
                        // Ici le code 401 sera capturé si la clé dans l'URL est toujours mauvaise
                        updateUI("Erreur API : " + responseCode, "Vérifiez votre clé API", false);
                        System.out.println("Détails erreur : " + response.toString());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    updateUI("Erreur de connexion", e.getMessage(), false);
                } finally {
                    if (conn != null) conn.disconnect();
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void updateUI(String organ, String species, boolean disableBtn) {
        javafx.application.Platform.runLater(() -> {
            organLabel.setText(organ);
            speciesLabel.setText(species);
            identifyBtn.setDisable(disableBtn);
        });
    }
}