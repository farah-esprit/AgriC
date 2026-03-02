package service;

import entities.User;
import entities.Role;
import entities.ForumThread;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import utils.ConfigLoader;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;

public class EmailService1 {

    // ✅ SMTP_HOST et SMTP_PORT supprimés (inutilisés, hardcodés dans createSession)
    private static final String FROM_EMAIL    = ConfigLoader.get("email.address");
    private static final String FROM_PASSWORD = ConfigLoader.get("email.password");
    private static final String FROM_NAME     = "AgroConnect Forum";

    private final UserService userService = new UserService();

    // ✅ Session SSL port 465
    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host",                   "smtp.gmail.com");
        props.put("mail.smtp.port",                   "465");
        props.put("mail.smtp.auth",                   "true");
        props.put("mail.smtp.socketFactory.port",     "465");
        props.put("mail.smtp.socketFactory.class",    "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.smtp.ssl.trust",              "smtp.gmail.com");
        props.put("mail.smtp.ssl.protocols",          "TLSv1.2");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
            }
        });
    }

    // ✅ Méthode commune d'envoi - UnsupportedEncodingException gérée
    private void sendEmail(String toEmail, String subject, String htmlBody)
            throws MessagingException {

        Session session = createSession();
        Message message = new MimeMessage(session);

        try {
            message.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            message.setFrom(new InternetAddress(FROM_EMAIL)); // ✅ fallback sans encodage
        }

        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setContent(htmlBody, "text/html; charset=utf-8");

        Transport.send(message);
    }

    // ✅ Notifier tous les Agriculteurs d'un nouveau thread Expert
    public void notifyAgriculteursOfNewExpertThread(ForumThread thread, User expert) {
        try {
            List<User> allUsers = userService.getAll();

            List<User> agriculteurs = allUsers.stream()
                    .filter(user -> user.getRole() == Role.AGRICULTEUR)
                    .filter(user -> user.getEmail() != null && !user.getEmail().isEmpty())
                    .toList();

            if (agriculteurs.isEmpty()) {
                System.out.println("⚠️ No Agriculteurs found to notify");
                return;
            }

            String subject  = "🎓 Nouveau sujet d'expert : " + thread.getTitre();
            String htmlBody = buildExpertThreadEmailHTML(thread, expert);

            int successCount = 0;
            for (User agriculteur : agriculteurs) {
                try {
                    sendEmail(agriculteur.getEmail(), subject, htmlBody);
                    successCount++;
                    System.out.println("✅ Email sent to: " + agriculteur.getEmail());
                } catch (MessagingException e) {
                    System.err.println("❌ Failed to send email to "
                            + agriculteur.getEmail() + ": " + e.getMessage());
                }
            }

            System.out.println("📧 Email notification complete: "
                    + successCount + "/" + agriculteurs.size() + " sent");

        } catch (Exception e) {
            System.err.println("❌ Error in notifyAgriculteursOfNewExpertThread: "
                    + e.getMessage());
        }
    }

    // ✅ HTML du thread expert
    private String buildExpertThreadEmailHTML(ForumThread thread, User expert) {
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'><style>" +
                "body{font-family:Arial,sans-serif;line-height:1.6;color:#333}" +
                ".container{max-width:600px;margin:0 auto;padding:20px}" +
                ".header{background:linear-gradient(135deg,#4CAF50 0%,#2E7D32 100%);" +
                "color:white;padding:30px;text-align:center;border-radius:10px 10px 0 0}" +
                ".header h1{margin:0;font-size:24px}" +
                ".header p{margin:10px 0 0 0;font-size:14px;opacity:.9}" +
                ".content{background:white;padding:30px;border:1px solid #e0e0e0;border-top:none}" +
                ".expert-badge{display:inline-block;background:#2196F3;color:white;" +
                "padding:5px 15px;border-radius:20px;font-size:12px;font-weight:bold;margin-bottom:15px}" +
                ".thread-title{color:#2E7D32;font-size:20px;font-weight:bold;margin:15px 0}" +
                ".thread-content{background:#f5f5f5;padding:15px;border-left:4px solid #4CAF50;" +
                "margin:20px 0;border-radius:5px}" +
                ".category{display:inline-block;background:#FFF3E0;color:#F57C00;" +
                "padding:5px 12px;border-radius:15px;font-size:11px;font-weight:bold;margin-right:8px}" +
                ".tags{color:#666;font-size:13px;margin-top:10px}" +
                ".cta-button{display:inline-block;background:#4CAF50;color:white;" +
                "padding:12px 30px;text-decoration:none;border-radius:25px;" +
                "font-weight:bold;margin:20px 0}" +
                ".footer{background:#f9f9f9;padding:20px;text-align:center;" +
                "font-size:12px;color:#666;border-radius:0 0 10px 10px}" +
                ".footer a{color:#4CAF50;text-decoration:none}" +
                "</style></head><body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>🌾 AgroConnect Forum</h1>" +
                "<p>Nouveau sujet d'expert disponible</p>" +
                "</div>" +
                "<div class='content'>" +
                "<span class='expert-badge'>🎓 Expert : " + expert.getNom() + "</span>" +
                "<h2 class='thread-title'>" + thread.getTitre() + "</h2>" +
                "<div class='thread-content'><p>" +
                (thread.getContenu().length() > 200
                        ? thread.getContenu().substring(0, 200) + "..."
                        : thread.getContenu()) +
                "</p></div>" +
                (thread.getCategory() != null && !thread.getCategory().isEmpty()
                        ? "<span class='category'>" + thread.getCategory() + "</span>" : "") +
                (thread.getTags() != null && !thread.getTags().isEmpty()
                        ? "<p class='tags'>🏷️ " + thread.getTags() + "</p>" : "") +
                "<center><a href='#' class='cta-button'>📖 Lire et Répondre</a></center>" +
                "<p style='margin-top:30px;font-size:13px;color:#666;'>" +
                "Un expert de notre communauté a partagé ses connaissances. " +
                "N'hésitez pas à poser vos questions et à participer à la discussion !" +
                "</p></div>" +
                "<div class='footer'>" +
                "<p>Vous recevez cet email car vous êtes inscrit comme Agriculteur " +
                "sur <strong>AgroConnect</strong></p>" +
                "<p>© 2025 AgroConnect - Forum d'entraide agricole</p>" +
                "<p><a href='#'>Se désabonner</a> | <a href='#'>Paramètres</a></p>" +
                "</div></div></body></html>";
    }

    // ✅ Test email
    public void sendTestEmail(String toEmail) {
        try {
            String subject  = "🌾 Test Email - AgroConnect";
            String htmlBody =
                    "<!DOCTYPE html><html><body style='font-family:Arial;padding:20px;'>" +
                            "<h2 style='color:#4CAF50;'>✅ Email Configuration Test</h2>" +
                            "<p>Si vous recevez cet email, votre configuration fonctionne !</p>" +
                            "<p style='color:#666;font-size:12px;'>AgroConnect Forum - Test Email</p>" +
                            "</body></html>";

            sendEmail(toEmail, subject, htmlBody);
            System.out.println("✅ Test email sent successfully to: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("❌ Test email failed: " + e.getMessage());
        }
    }
}