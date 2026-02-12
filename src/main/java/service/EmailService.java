package service;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailService {

    // ⚠️ REMPLACEZ PAR VOS VRAIES INFORMATIONS
    private static final String FROM_EMAIL = "agriconnect3a6@gmail.com\n"; // Votre email Gmail
    private static final String PASSWORD = "ofhd nvdm stky rifc"; // Le mot de passe d'application (16 caractères)

    /**
     * Envoie un email de réinitialisation de mot de passe
     */
    public boolean sendPasswordResetEmail(String toEmail, String userName, String resetCode) {
        try {
            // Configuration SMTP Gmail
            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");

            // Authentification
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
                }
            });

            // Debug mode (optionnel - pour voir les logs détaillés)
            session.setDebug(true);

            // Créer le message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("🔐 AgriConnect - Réinitialisation de mot de passe");

            // Contenu HTML
            String htmlContent =
                    "<html>" +
                            "<body style='font-family: Arial, sans-serif; padding: 20px;'>" +
                            "<div style='background-color: #f5f5f5; padding: 30px; border-radius: 10px; max-width: 600px; margin: auto;'>" +
                            "<h2 style='color: #388e3c; text-align: center;'>🌱 AgriConnect</h2>" +
                            "<h3>Bonjour " + userName + ",</h3>" +
                            "<p style='font-size: 16px;'>Vous avez demandé la réinitialisation de votre mot de passe.</p>" +
                            "<p style='font-size: 16px;'>Voici votre code de réinitialisation :</p>" +
                            "<div style='background-color: white; padding: 25px; border-radius: 8px; text-align: center; margin: 25px 0; border: 2px solid #388e3c;'>" +
                            "<h1 style='color: #388e3c; letter-spacing: 8px; margin: 0; font-size: 40px;'>" + resetCode + "</h1>" +
                            "</div>" +
                            "<p style='font-size: 14px; color: #d32f2f;'><strong>⚠️ Ce code est valable pendant 30 minutes.</strong></p>" +
                            "<p style='font-size: 14px; color: #666;'>Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.</p>" +
                            "<hr style='margin-top: 30px; border: none; border-top: 1px solid #ddd;'>" +
                            "<p style='color: #999; font-size: 12px; text-align: center;'>© 2026 AgriConnect - Votre plateforme agricole</p>" +
                            "</div>" +
                            "</body>" +
                            "</html>";

            message.setContent(htmlContent, "text/html; charset=utf-8");

            // Envoyer
            Transport.send(message);

            System.out.println("✅ Email envoyé avec succès à " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}