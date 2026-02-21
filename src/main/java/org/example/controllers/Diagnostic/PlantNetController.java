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

    private static final String API_KEY = "2b10RB9HZ7W4Vt0kEgqDBKuD6e";
    private File selectedFile;

    @FXML
    private Label selectedFileLabel;

    @FXML
    private Label organLabel;

    @FXML
    private Label speciesLabel;

    @FXML
    private Button identifyBtn;

    // ================= Choisir image =================
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

    // ================= Identifier plante =================
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

                    URL url = new URL(
                            "https://my-api.plantnet.org/v2/identify/all?api-key="
                                    + API_KEY
                    );

                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type",
                            "multipart/form-data; boundary=" + boundary);

                    OutputStream output = conn.getOutputStream();
                    PrintWriter writer = new PrintWriter(
                            new OutputStreamWriter(output, "UTF-8"), true);

                    String mimeType = Files.probeContentType(selectedFile.toPath());
                    if (mimeType == null) mimeType = "application/octet-stream";

                    // ==== Envoi image ====
                    writer.append("--").append(boundary).append("\r\n");
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
                    writer.close();

                    // ==== Lire réponse ====
                    int responseCode = conn.getResponseCode();

                    InputStream stream = (responseCode >= 200 && responseCode < 300)
                            ? conn.getInputStream()
                            : conn.getErrorStream();

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(stream));

                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    reader.close();

                    if (responseCode >= 200 && responseCode < 300) {

                        JSONObject json = new JSONObject(response.toString());

                        // 🔹 Organe détecté
                        JSONArray predictedOrgans = json.optJSONArray("predictedOrgans");
                        String organ = "N/A";

                        if (predictedOrgans != null && predictedOrgans.length() > 0) {
                            organ = predictedOrgans.getJSONObject(0)
                                    .optString("organ", "N/A");
                        }

                        // 🔹 Nom scientifique
                        JSONArray results = json.getJSONArray("results");

                        if (results.length() > 0) {

                            JSONObject firstResult = results.getJSONObject(0);
                            JSONObject speciesObj = firstResult.getJSONObject("species");

                            String scientific = speciesObj
                                    .optString("scientificName", "N/A");

                            String finalOrgan = organ;

                            javafx.application.Platform.runLater(() -> {
                                organLabel.setText("Organe détecté : " + finalOrgan);
                                speciesLabel.setText("Nom scientifique : " + scientific);
                                identifyBtn.setDisable(false);
                            });

                        } else {
                            javafx.application.Platform.runLater(() -> {
                                organLabel.setText("Organe détecté : N/A");
                                speciesLabel.setText("Aucun résultat trouvé");
                                identifyBtn.setDisable(false);
                            });
                        }

                    } else {
                        javafx.application.Platform.runLater(() -> {
                            organLabel.setText("Erreur API : HTTP " + responseCode);
                            speciesLabel.setText("");
                            identifyBtn.setDisable(false);
                        });
                    }

                } catch (Exception e) {

                    e.printStackTrace();

                    javafx.application.Platform.runLater(() -> {
                        organLabel.setText("Erreur : " + e.getMessage());
                        speciesLabel.setText("");
                        identifyBtn.setDisable(false);
                    });

                } finally {
                    if (conn != null) conn.disconnect();
                }

                return null;
            }
        };

        new Thread(task).start();
    }
}