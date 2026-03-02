package service;

import utils.ConfigLoader;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {

    private static final String FROM_EMAIL = ConfigLoader.get("email.address");
    private static final String PASSWORD   = ConfigLoader.get("email.password");

    // ✅ Session commune avec Jakarta Mail + SSL port 465
    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host",                 "smtp.gmail.com");
        props.put("mail.smtp.port",                 "465");
        props.put("mail.smtp.auth",                 "true");
        props.put("mail.smtp.socketFactory.port",   "465");
        props.put("mail.smtp.socketFactory.class",  "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.smtp.ssl.trust",            "smtp.gmail.com");
        props.put("mail.smtp.ssl.protocols",        "TLSv1.2");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });
    }

    // ✅ Méthode commune d'envoi
    private boolean sendEmail(String toEmail, String subject, String htmlContent) {
        try {
            Session session = createSession();
            Message message = new MimeMessage(session);

            message.setFrom(new InternetAddress(FROM_EMAIL, "AgriConnect", "UTF-8"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email à " + toEmail + " : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // 🔐 EMAIL VERIFICATION
    public boolean sendVerificationEmail(String toEmail, String userName, String code) {
        String subject = "🔐 Activation de votre compte AgriConnect";
        String html =
                "<html><body style='font-family:Arial;padding:20px;'>" +
                        "<div style='max-width:500px;margin:auto;border:1px solid #e0e0e0;" +
                        "border-radius:10px;overflow:hidden;'>" +
                        "<div style='background:#388e3c;padding:20px;text-align:center;'>" +
                        "<h2 style='color:white;margin:0;'>🌱 AgriConnect</h2>" +
                        "</div>" +
                        "<div style='padding:30px;'>" +
                        "<p>Bonjour <strong>" + userName + "</strong>,</p>" +
                        "<p>Voici votre code d'activation :</p>" +
                        "<div style='text-align:center;margin:20px 0;'>" +
                        "<span style='font-size:32px;font-weight:bold;color:#388e3c;" +
                        "letter-spacing:8px;background:#f5f5f5;padding:15px 25px;" +
                        "border-radius:8px;'>" + code + "</span>" +
                        "</div>" +
                        "<p style='color:#999;font-size:12px;'>⏳ Valide pendant 15 minutes</p>" +
                        "</div>" +
                        "<div style='background:#f9f9f9;padding:15px;text-align:center;" +
                        "font-size:11px;color:#aaa;'>" +
                        "© 2025 AgriConnect - Ne pas répondre à cet email" +
                        "</div></div></body></html>";

        boolean sent = sendEmail(toEmail, subject, html);
        if (sent) System.out.println("✅ Email de vérification envoyé à " + toEmail);
        else      System.err.println("❌ Erreur vérification email : " + toEmail);
        return sent;
    }

    // 🔐 RESET PASSWORD
    public boolean sendPasswordResetEmail(String toEmail, String userName, String resetCode) {
        String subject = "🔐 AgriConnect - Réinitialisation de mot de passe";
        String html =
                "<html><body style='font-family:Arial;padding:20px;'>" +
                        "<div style='max-width:500px;margin:auto;border:1px solid #e0e0e0;" +
                        "border-radius:10px;overflow:hidden;'>" +
                        "<div style='background:#d32f2f;padding:20px;text-align:center;'>" +
                        "<h2 style='color:white;margin:0;'>🔐 Réinitialisation</h2>" +
                        "</div>" +
                        "<div style='padding:30px;'>" +
                        "<p>Bonjour <strong>" + userName + "</strong>,</p>" +
                        "<p>Voici votre code de réinitialisation :</p>" +
                        "<div style='text-align:center;margin:20px 0;'>" +
                        "<span style='font-size:32px;font-weight:bold;color:#d32f2f;" +
                        "letter-spacing:8px;background:#fff3f3;padding:15px 25px;" +
                        "border-radius:8px;'>" + resetCode + "</span>" +
                        "</div>" +
                        "<p style='color:#d32f2f;font-size:12px;'>⚠️ Valide pendant 30 minutes</p>" +
                        "<p style='color:#999;font-size:11px;'>Si vous n'avez pas demandé cette " +
                        "réinitialisation, ignorez cet email.</p>" +
                        "</div>" +
                        "<div style='background:#f9f9f9;padding:15px;text-align:center;" +
                        "font-size:11px;color:#aaa;'>" +
                        "© 2025 AgriConnect - Ne pas répondre à cet email" +
                        "</div></div></body></html>";

        boolean sent = sendEmail(toEmail, subject, html);
        if (sent) System.out.println("✅ Email reset envoyé à " + toEmail);
        else      System.err.println("❌ Erreur reset email : " + toEmail);
        return sent;
    }

    // 🌱 WELCOME EMAIL
    public boolean sendWelcomeEmail(String toEmail, String userName) {
        String subject = "🌱 Bienvenue sur AgriConnect !";
        String html =
                "<html><body style='font-family:Arial;padding:20px;'>" +
                        "<div style='max-width:500px;margin:auto;border:1px solid #e0e0e0;" +
                        "border-radius:10px;overflow:hidden;'>" +
                        "<div style='background:linear-gradient(135deg,#4CAF50,#2E7D32);" +
                        "padding:30px;text-align:center;'>" +
                        "<h1 style='color:white;margin:0;'>🌾 AgriConnect</h1>" +
                        "<p style='color:#c8e6c9;margin:5px 0 0 0;'>Plateforme agricole</p>" +
                        "</div>" +
                        "<div style='padding:30px;'>" +
                        "<h2 style='color:#388e3c;'>Bienvenue " + userName + " ! 🎉</h2>" +
                        "<p>Votre compte a été créé et activé avec succès.</p>" +
                        "<p>Vous pouvez maintenant accéder à toutes les fonctionnalités :</p>" +
                        "<ul style='color:#555;line-height:1.8;'>" +
                        "<li>🌱 Gérer vos cultures</li>" +
                        "<li>💬 Participer au forum</li>" +
                        "<li>🛒 Commander des produits</li>" +
                        "<li>📊 Suivre vos statistiques</li>" +
                        "</ul>" +
                        "</div>" +
                        "<div style='background:#f9f9f9;padding:15px;text-align:center;" +
                        "font-size:11px;color:#aaa;'>" +
                        "© 2025 AgriConnect - Ne pas répondre à cet email" +
                        "</div></div></body></html>";

        boolean sent = sendEmail(toEmail, subject, html);
        if (sent) System.out.println("✅ Email de bienvenue envoyé à " + toEmail);
        else      System.err.println("❌ Erreur email bienvenue : " + toEmail);
        return sent;
    }
}