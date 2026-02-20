package controller;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import service.TwoFactorAuthService;
import service.UserService;
import entities.User;
import java.io.ByteArrayInputStream;
public class TwoFactorController {
    @FXML private ImageView qrCodeImage;
    @FXML private TextField codeField;
    @FXML private Label messageLabel;
    @FXML private Label secretLabel;
    @FXML private Button verifyButton;

    private TwoFactorAuthService tfaService;
    private UserService userService;
    private User currentUser;
    private String secret;

    @FXML
    public void initialize() {
        tfaService = new TwoFactorAuthService();
        userService = new UserService();

        Platform.runLater(() -> {
            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.setMaximized(true);
            Scene scene = stage.getScene();
            if (scene.getRoot() instanceof Region r) {
                r.prefWidthProperty().bind(scene.widthProperty());
                r.prefHeightProperty().bind(scene.heightProperty());
            }
        });
    }

    public void setUser(User user) {
        this.currentUser = user;
        setupTwoFactor();
    }

    private void setupTwoFactor() {
        // Générer secret
        secret = tfaService.generateSecret();

        // Générer QR Code
        byte[] qrCode = tfaService.generateQRCode(secret, currentUser.getEmail());

        if (qrCode != null) {
            Image image = new Image(new ByteArrayInputStream(qrCode));
            qrCodeImage.setImage(image);

            // Afficher le secret (au cas où le QR code ne marche pas)
            if (secretLabel != null) {
                secretLabel.setText("Code secret : " + secret);
            }

            messageLabel.setText("📱 Scannez ce QR avec Google Authenticator");
            messageLabel.setStyle("-fx-text-fill: #388e3c;");

            System.out.println("✅ QR Code généré pour : " + currentUser.getEmail());
        } else {
            messageLabel.setText("❌ Erreur génération QR Code");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleVerifyCode() {
        String code = codeField.getText().trim();

        if (code.isEmpty() || code.length() != 6) {
            messageLabel.setText("❌ Code invalide (6 chiffres requis)");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        boolean valid = tfaService.verifyCode(secret, code);

        if (valid) {
            // ✅ Sauvegarder le secret dans la BD
            userService.enable2FA(currentUser.getId(), secret);

            messageLabel.setText("✅ 2FA activé avec succès !");
            messageLabel.setStyle("-fx-text-fill: green;");

            System.out.println("✅ 2FA activé pour user ID: " + currentUser.getId());

            // Rediriger vers le profil après 2 secondes
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(() -> redirectToProfil());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        } else {
            messageLabel.setText("❌ Code incorrect");
            messageLabel.setStyle("-fx-text-fill: red;");
            codeField.clear();
        }
    }

    // ✅ CORRIGÉ : Retour vers le PROFIL (pas le dashboard admin)
    @FXML
    private void handleCancel() {
        redirectToProfil();
    }

    @FXML
    private void handleBack() {
        redirectToProfil();
    }

    // ✅ NOUVELLE MÉTHODE : Redirection vers le profil
    private void redirectToProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profil.fxml"));
            Parent root = loader.load();
            ProfilController controller = loader.getController();
            controller.setUser(currentUser);
            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Mon Profil");
            stage.setMaximized(true); // ✅
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}