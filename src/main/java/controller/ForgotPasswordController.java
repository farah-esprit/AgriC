package controller;

import entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import service.EmailService;
import service.PasswordResetService;
import service.UserService;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private Label messageLabel;

    private UserService userService;
    private PasswordResetService resetService;
    private EmailService emailService;

    // ================= INITIALISATION =================
    @FXML
    public void initialize() {
        userService = new UserService();
        resetService = new PasswordResetService();
        emailService = new EmailService();
        if (messageLabel != null) messageLabel.setText("");

        Platform.runLater(() -> {
            Stage stage = (Stage) messageLabel.getScene().getWindow();
            stage.setMaximized(true);
            Scene scene = stage.getScene();
            if (scene.getRoot() instanceof Region r) {
                r.prefWidthProperty().bind(scene.widthProperty());
                r.prefHeightProperty().bind(scene.heightProperty());
            }
        });
    }

    // ================= ENVOYER LE CODE =================
    @FXML
    private void handleSendCode() {
        String email = emailField.getText().trim();

        // Validation
        if (email.isEmpty()) {
            showError("❌ Veuillez entrer votre adresse email");
            return;
        }

        if (!isValidEmail(email)) {
            showError("❌ Format d'email invalide");
            return;
        }

        // Vérifier si l'utilisateur existe
        User user = userService.findByEmail(email);

        if (user == null) {
            // Pour des raisons de sécurité, on affiche le même message
            showSuccess("✅ Si cet email existe, un code a été envoyé");
            return;
        }

        // Générer le code de réinitialisation
        String resetCode = resetService.createResetRequest(user.getId());

        if (resetCode == null) {
            showError("❌ Erreur lors de la génération du code");
            return;
        }

        // Envoyer l'email
        boolean emailSent = emailService.sendPasswordResetEmail(
                user.getEmail(),
                user.getNom(),
                resetCode
        );

        if (emailSent) {
            showSuccess("✅ Un code a été envoyé à votre email");

            // Attendre 2 secondes puis rediriger vers la page de réinitialisation
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(() -> {
                        try {
                            goToResetPassword();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } else {
            showError("❌ Erreur lors de l'envoi de l'email");
        }
    }

    // ================= NAVIGATION VERS RESET PASSWORD =================
    private void goToResetPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resetPassword.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Réinitialisation");
            stage.setMaximized(true); // ✅
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Connexion");
            stage.setMaximized(true); // ✅
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // ================= VALIDATION EMAIL =================
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // ================= MESSAGES =================
    private void showError(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        }
    }

    private void showSuccess(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
        }
    }
}