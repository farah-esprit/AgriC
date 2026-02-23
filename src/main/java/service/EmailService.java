package service;

import utils.ConfigLoader;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailService {

    private static final String FROM_EMAIL = ConfigLoader.get("email.address");
    private static final String PASSWORD = ConfigLoader.get("email.password");

    // 🔹 Méthode commune pour créer la session
    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });
    }

    // 🔐 EMAIL VERIFICATION
    public boolean sendVerificationEmail(String toEmail, String userName, String code) {
        try {
            Session session = createSession();
            Message message = new MimeMessage(session);

            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(toEmail));
            message.setSubject("🔐 Activation de votre compte AgriConnect");

            String htmlContent =
                    "<html><body style='font-family: Arial; padding:20px;'>" +
                            "<h2 style='color:#388e3c;'>🌱 Activation du compte</h2>" +
                            "<p>Bonjour " + userName + ",</p>" +
                            "<p>Voici votre code d'activation :</p>" +
                            "<h1 style='color:#388e3c; letter-spacing:6px;'>" + code + "</h1>" +
                            "<p>⏳ Valide 15 minutes</p>" +
                            "</body></html>";

            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ Email de vérification envoyé à " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur email vérification : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // 🔐 RESET PASSWORD
    public boolean sendPasswordResetEmail(String toEmail, String userName, String resetCode) {
        try {
            Session session = createSession();
            Message message = new MimeMessage(session);

            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(toEmail));
            message.setSubject("🔐 AgriConnect - Réinitialisation de mot de passe");

            String htmlContent =
                    "<html><body style='font-family: Arial; padding:20px;'>" +
                            "<h2 style='color:#388e3c;'>🌱 AgriConnect</h2>" +
                            "<p>Bonjour " + userName + ",</p>" +
                            "<p>Voici votre code de réinitialisation :</p>" +
                            "<h1 style='color:#388e3c; letter-spacing:6px;'>" + resetCode + "</h1>" +
                            "<p style='color:#d32f2f;'>⚠️ Valide 30 minutes</p>" +
                            "</body></html>";

            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ Email reset envoyé à " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur reset email : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // 🌱 WELCOME EMAIL
    public boolean sendWelcomeEmail(String toEmail, String userName) {
        try {
            Session session = createSession();
            Message message = new MimeMessage(session);

            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(toEmail));
            message.setSubject("🌱 Bienvenue sur AgriConnect !");

            String htmlContent =
                    "<html><body style='font-family: Arial; padding:20px;'>" +
                            "<h2 style='color:#388e3c;'>Bienvenue " + userName + " 🌱</h2>" +
                            "<p>Votre compte a été créé avec succès.</p>" +
                            "<p>Nous sommes ravis de vous accueillir sur AgriConnect !</p>" +
                            "</body></html>";

            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ Email de bienvenue envoyé à " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur email bienvenue : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}