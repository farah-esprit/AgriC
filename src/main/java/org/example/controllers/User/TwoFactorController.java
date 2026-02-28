package org.example.controllers.User;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import org.example.entities.User;
import org.example.services.User.TwoFactorAuthService;
import org.example.services.User.UserService;


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
    private ProfilController profilController;

    private AnchorPane contentPane;

    public void setContentPane(AnchorPane contentPane) {
        this.contentPane = contentPane;
    }

    @FXML
    public void initialize() {
        tfaService = new TwoFactorAuthService();
        userService = new UserService();
    }

    public void setUser(User user) {
        this.currentUser = user;
        setupTwoFactor();
    }

    private void setupTwoFactor() {
        secret = tfaService.generateSecret();
        System.out.println("🔑 Nouveau secret généré : " + secret); // ← ajoute ça

        byte[] qrCode = tfaService.generateQRCode(secret, currentUser.getEmail());

        if (qrCode != null) {
            Image image = new Image(new ByteArrayInputStream(qrCode));
            qrCodeImage.setImage(image);

            if (secretLabel != null) {
                secretLabel.setText("Code secret : " + secret);
            }

            messageLabel.setText("📱 Scannez ce QR avec Google Authenticator");
            messageLabel.setStyle("-fx-text-fill: #388e3c;");
        } else {
            messageLabel.setText("❌ Erreur génération QR Code");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleVerifyCode() {
        String code = codeField.getText().trim();

        System.out.println("🔍 Code saisi : " + code);
        System.out.println("🔍 contentPane : " + contentPane); // ← vérifier si null

        if (code.isEmpty() || code.length() != 6) {
            messageLabel.setText("❌ Code invalide (6 chiffres requis)");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        boolean valid = tfaService.verifyCode(secret, code);
        System.out.println("🔍 Code valide : " + valid); // ← vérifier si true ou false

        if (valid) {
            userService.enable2FA(currentUser.getId(), secret);
            messageLabel.setText("✅ 2FA activé avec succès !");
            messageLabel.setStyle("-fx-text-fill: green;");

            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    Platform.runLater(() -> {
                        System.out.println("🔍 Avant redirectToProfil, contentPane = " + contentPane);
                        redirectToProfil();
                    });
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

    @FXML
    private void handleCancel() {
        redirectToProfil();
    }

    private void redirectToProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profil.fxml"));
            Parent profilContent = loader.load();

            ProfilController controller = loader.getController();
            controller.setUser(currentUser);

            contentPane.getChildren().clear();
            contentPane.getChildren().add(profilContent);

            AnchorPane.setTopAnchor(profilContent, 0.0);
            AnchorPane.setBottomAnchor(profilContent, 0.0);
            AnchorPane.setLeftAnchor(profilContent, 0.0);
            AnchorPane.setRightAnchor(profilContent, 0.0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleBack() {
        redirectToProfil();
    }
}
