package org.example.controllers.Diagnostic;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class PlantNetMultipart {

    private static final String API_KEY = "2b10RB9HZ7W4Vt0kEgqDBKuD6e";

    public static void main(String[] args) throws Exception {
        File imageFile = new File("plante.jpg"); // ton image

        String boundary = "===" + System.currentTimeMillis() + "===";
        URL url = new URL("https://my-api.plantnet.org/v2/identify/all?organs=leaf&api-key=" + API_KEY);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        OutputStream output = conn.getOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true);

        // Ajouter le fichier image
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"images\"; filename=\"")
                .append(imageFile.getName()).append("\"\r\n");
        writer.append("Content-Type: ").append("image/jpeg").append("\r\n\r\n");
        writer.flush();

        // Lire le fichier et écrire dans le flux
        FileInputStream inputStream = new FileInputStream(imageFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        output.flush();
        inputStream.close();

        writer.append("\r\n").flush();
        writer.append("--").append(boundary).append("--").append("\r\n");
        writer.close();

        // Lire la réponse
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        StringBuilder response = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        System.out.println("Réponse PlantNet : " + response.toString());
    }
}