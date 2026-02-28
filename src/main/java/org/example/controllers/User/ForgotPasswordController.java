package org.example.controllers.User;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.entities.User;
import org.example.services.User.EmailService;
import org.example.services.User.PasswordResetService;
import org.example.services.User.UserService;


public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private Label messageLabel;
    @FXML private Label errorLabel;

    private UserService userService;
    private PasswordResetService resetService;
    private EmailService emailService;

    private StackPane contentPane;

    public void setContentPane(StackPane contentPane) {
        this.contentPane = contentPane;
    }    @FXML
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/User/resetPassword.fxml"));
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
            // ✅ CHARGER LE FXML COMPLET
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load(); // C'est un HBox

            // ✅ RÉCUPÉRER LE CONTROLLER
            LoginController controller = loader.getController();

            // ✅ EXTRAIRE LE VBOX DU FORMULAIRE (qui est dans le 2ème enfant du HBox)
            if (root instanceof javafx.scene.layout.HBox hbox) {
                // Le HBox contient 2 StackPane : [0] = vert, [1] = blanc
                if (hbox.getChildren().size() >= 2) {
                    javafx.scene.layout.StackPane whitePane = (javafx.scene.layout.StackPane) hbox.getChildren().get(1);

                    // Le StackPane blanc contient le VBox du formulaire
                    if (whitePane.getChildren().size() > 0) {
                        javafx.scene.Node loginForm = whitePane.getChildren().get(0);

                        // ✅ RETIRER LE FORMULAIRE DU STACKPANE BLANC
                        whitePane.getChildren().remove(loginForm);

                        // ✅ METTRE LE FORMULAIRE DANS NOTRE CONTENTPANE
                        contentPane.getChildren().clear();
                        contentPane.getChildren().add(loginForm);

                        // ✅ PASSER LE CONTENTPANE AU CONTROLLER
                        controller.contentPane = this.contentPane;

                        System.out.println("✅ Formulaire de login extrait et affiché");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (errorLabel != null) {
                errorLabel.setText("Erreur lors du retour au login");
            }
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
