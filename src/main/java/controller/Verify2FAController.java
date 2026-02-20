package controller;
import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import service.TwoFactorAuthService;
import service.UserService;
public class Verify2FAController {
    @FXML private TextField codeField;
    @FXML private Label emailLabel;
    @FXML private Label messageLabel;
    @FXML private Button btnVerify;

    private TwoFactorAuthService tfaService;
    private UserService userService;
    private User currentUser;

    @FXML
    public void initialize() {
        tfaService = new TwoFactorAuthService();
        userService = new UserService();
    }

    public void setUser(User user) {
        this.currentUser = user;

        if (emailLabel != null) {
            String maskedEmail = maskEmail(user.getEmail());
            emailLabel.setText("Code requis pour " + maskedEmail);
        }

        System.out.println("✅ User défini dans Verify2FAController : " + user.getEmail());
    }

    @FXML
    private void handleVerify() {
        String code = codeField.getText().trim();

        if (code.isEmpty() || code.length() != 6) {
            showError("❌ Code invalide (6 chiffres requis)");
            return;
        }

        // Récupérer le secret 2FA
        String secret = userService.get2FASecret(currentUser.getId());

        if (secret == null) {
            showError("❌ Erreur : Secret 2FA introuvable");
            return;
        }

        // Vérifier le code
        boolean isValid = tfaService.verifyCode(secret, code);

        if (isValid) {
            showSuccess("✅ Code vérifié !");

            // Rediriger vers dashboard
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    javafx.application.Platform.runLater(() -> redirectToDashboard());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } else {
            showError("❌ Code incorrect");
            codeField.clear();
        }
    }

    @FXML
    private void handleResendCode() {
        showWarning("⚠️ Ouvrez Google Authenticator pour obtenir le code");
    }



    private void redirectToDashboard() {
        try {
            String fxmlFile = "";
            switch (currentUser.getRole()) {
                case ADMIN:       fxmlFile = "/adminDashboard.fxml"; break;
                case AGRICULTEUR: fxmlFile = "/agriculteurDashboard.fxml"; break;
                case EXPERT:      fxmlFile = "/expertDashboard.fxml"; break;
                case FOURNISSEUR: fxmlFile = "/fournisseurDashboard.fxml"; break;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof DashboardAdminController)
                ((DashboardAdminController) controller).setUser(currentUser);
            else if (controller instanceof DashboardAgriculteurController)
                ((DashboardAgriculteurController) controller).setUser(currentUser);
            else if (controller instanceof DashboardExpertController)
                ((DashboardExpertController) controller).setUser(currentUser);
            else if (controller instanceof DashboardFournisseurController)
                ((DashboardFournisseurController) controller).setUser(currentUser);
            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Dashboard");
            stage.setMaximized(true); // ✅
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Connexion");
            stage.setMaximized(true); // ✅
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;

        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];

        if (local.length() <= 3) {
            return local.charAt(0) + "***@" + domain;
        }

        return local.substring(0, 2) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }

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

    private void showWarning(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold;");
        }
    }
}