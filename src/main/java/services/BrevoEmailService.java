package services;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class BrevoEmailService {

    private static final String API_KEY = "";
    private static final String API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String SENDER_EMAIL = "bahaeddine.cherif@isimg.tn";
    private static final String SENDER_NAME = "AgriConnect";

    private static void envoyerEmail(String emailDest, String nomDest, String sujet, String htmlContent) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("accept", "application/json");
            conn.setRequestProperty("api-key", API_KEY);
            conn.setRequestProperty("content-type", "application/json");
            conn.setDoOutput(true);

            String body = "{"
                    + "\"sender\":{\"name\":\"" + SENDER_NAME + "\",\"email\":\"" + SENDER_EMAIL + "\"},"
                    + "\"to\":[{\"email\":\"" + emailDest + "\",\"name\":\"" + nomDest + "\"}],"
                    + "\"subject\":\"" + sujet + "\","
                    + "\"htmlContent\":\"" + htmlContent.replace("\"", "\\\"").replace("\n", "") + "\""
                    + "}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 201) {
                System.out.println("✅ Email envoyé à " + emailDest);
            } else {
                System.err.println("❌ Erreur Brevo : code " + responseCode);
            }
            conn.disconnect();

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email : " + e.getMessage());
        }
    }

    // ✅ Confirmation commande
    public static void envoyerConfirmationCommande(
            String emailClient, String nomClient,
            String nomProduit, int quantite, double prix) {

        String sujet = "✅ Confirmation de votre commande - AgriConnect";
        String html = "<div style='font-family:Arial;max-width:600px;margin:auto;'>"
                + "<div style='background:linear-gradient(to right,#5a8c4a,#4a7c3a);padding:30px;text-align:center;'>"
                + "<h1 style='color:white;margin:0;'>🌾 AgriConnect</h1>"
                + "<p style='color:#e8f5e9;margin:5px 0;'>Confirmation de commande</p>"
                + "</div>"
                + "<div style='padding:30px;background:#f9f9f9;'>"
                + "<h2 style='color:#4a7c3a;'>Bonjour " + nomClient + " !</h2>"
                + "<p>Votre commande a été enregistrée avec succès.</p>"
                + "<div style='background:white;border-radius:10px;padding:20px;border-left:4px solid #4a7c3a;margin:20px 0;'>"
                + "<h3 style='color:#4a7c3a;margin-top:0;'>📦 Détails de la commande</h3>"
                + "<table style='width:100%;border-collapse:collapse;'>"
                + "<tr style='border-bottom:1px solid #eee;'><td style='padding:8px;color:#666;'>Produit</td>"
                + "<td style='padding:8px;font-weight:bold;'>" + nomProduit + "</td></tr>"
                + "<tr style='border-bottom:1px solid #eee;'><td style='padding:8px;color:#666;'>Quantité</td>"
                + "<td style='padding:8px;font-weight:bold;'>" + quantite + " unités</td></tr>"
                + "<tr style='border-bottom:1px solid #eee;'><td style='padding:8px;color:#666;'>Prix unitaire</td>"
                + "<td style='padding:8px;font-weight:bold;'>" + String.format("%.2f DT", prix) + "</td></tr>"
                + "<tr><td style='padding:8px;color:#666;'>Total</td>"
                + "<td style='padding:8px;font-weight:bold;color:#4a7c3a;font-size:18px;'>"
                + String.format("%.2f DT", prix * quantite) + "</td></tr>"
                + "</table></div>"
                + "<p style='color:#888;font-size:12px;'>Merci de votre confiance - AgriConnect</p>"
                + "</div>"
                + "<div style='background:#4a7c3a;padding:15px;text-align:center;'>"
                + "<p style='color:white;margin:0;font-size:12px;'>© AgriConnect 2025</p>"
                + "</div></div>";

        envoyerEmail(emailClient, nomClient, sujet, html);
    }

    // ✅ Alerte stock bas
    public static void envoyerAlerteStock(
            String emailAdmin, String nomProduit,
            int quantiteActuelle, int seuilAlerte) {

        String sujet = "⚠️ Alerte Stock Bas - " + nomProduit;
        String html = "<div style='font-family:Arial;max-width:600px;margin:auto;'>"
                + "<div style='background:linear-gradient(to right,#5a8c4a,#4a7c3a);padding:30px;text-align:center;'>"
                + "<h1 style='color:white;margin:0;'>🌾 AgriConnect</h1>"
                + "<p style='color:#e8f5e9;'>Alerte de stock</p>"
                + "</div>"
                + "<div style='padding:30px;background:#fff8f0;'>"
                + "<div style='background:#fff3cd;border:1px solid #f0ad4e;border-radius:10px;padding:20px;'>"
                + "<h2 style='color:#f0ad4e;margin-top:0;'>⚠️ Stock bas détecté !</h2>"
                + "<table style='width:100%;border-collapse:collapse;'>"
                + "<tr style='border-bottom:1px solid #eee;'><td style='padding:8px;color:#666;'>Produit</td>"
                + "<td style='padding:8px;font-weight:bold;'>" + nomProduit + "</td></tr>"
                + "<tr style='border-bottom:1px solid #eee;'><td style='padding:8px;color:#666;'>Quantité actuelle</td>"
                + "<td style='padding:8px;font-weight:bold;color:#d9534f;'>" + quantiteActuelle + " unités</td></tr>"
                + "<tr><td style='padding:8px;color:#666;'>Seuil alerte</td>"
                + "<td style='padding:8px;font-weight:bold;'>" + seuilAlerte + " unités</td></tr>"
                + "</table></div>"
                + "<p style='margin-top:20px;'>Veuillez réapprovisionner ce produit rapidement.</p>"
                + "</div>"
                + "<div style='background:#4a7c3a;padding:15px;text-align:center;'>"
                + "<p style='color:white;margin:0;font-size:12px;'>© AgriConnect 2025</p>"
                + "</div></div>";

        envoyerEmail(emailAdmin, "Admin AgriConnect", sujet, html);
    }
}