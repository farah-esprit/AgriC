package org.example.controllers.User;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import org.example.entities.User;
import org.example.services.User.SmsService;


public class VerifySmsController {

    @FXML private Label phoneLabel;
    @FXML private Label messageLabel;
    @FXML private TextField codeField;
    @FXML private Button verifyButton;

    private SmsService smsService;
    private User currentUser;
    private String phoneNumber;
    private EditProfilController editProfilController;
    private AnchorPane contentPane; // ✅ AJOUTER
    private boolean verified = false;

    @FXML
    public void initialize() {
        smsService = new SmsService();

        codeField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.matches("\\d*")) {
                codeField.setText(old);
            }
            if (newVal.length() > 6) {
                codeField.setText(newVal.substring(0, 6));
            }
        });
    }

    public void setUser(User user) {
        this.currentUser = user;
    }

    public void setPhoneNumber(String phone) {
        this.phoneNumber = phone;
        if (phoneLabel != null) {
            phoneLabel.setText(phone);
        }
    }

    public void setEditProfilController(EditProfilController controller) {
        this.editProfilController = controller;
    }

    // ✅ AJOUTER cette méthode
    public void setContentPane(AnchorPane contentPane) {
        this.contentPane = contentPane;
    }

    @FXML
    private void handleVerify() {
        String enteredCode = codeField.getText().trim();

        if (enteredCode.isEmpty()) {
            showError("❌ Veuillez entrer le code !");
            return;
        }

        if (enteredCode.length() != 6) {
            showError("❌ Le code doit contenir 6 chiffres !");
            return;
        }

        if (smsService.verifyCode(currentUser.getId(), enteredCode)) {
            showSuccess("✅ Code valide !");
            verified = true;

            if (editProfilController != null) {
                editProfilController.onSmsVerified();
            }

            closeView(); // ✅ MODIFIER

        } else {
            showError("❌ Code incorrect ! Réessayez.");
            codeField.clear();
        }
    }

    @FXML
    private void handleResend() {
        showInfo("📲 Envoi d'un nouveau code...");

        boolean sent = smsService.sendVerificationCode(phoneNumber, currentUser.getId());

        if (sent) {
            showSuccess("✅ Code renvoyé !");
            codeField.clear();
        } else {
            showError("❌ Erreur lors de l'envoi !");
        }
    }

    @FXML
    private void handleCancel() {
        smsService.clearCode(currentUser.getId());
        backToEditProfil(); // ✅ MODIFIER
    }

    // ✅ REMPLACER closeWindow() par closeView()
    private void closeView() {
        // Ne rien faire ici, la fermeture est gérée par onSmsVerified()
        // qui retourne automatiquement au profil
    }

    // ✅ AJOUTER cette méthode pour retourner à EditProfil
    private void backToEditProfil() {
        if (contentPane != null && editProfilController != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/editProfil.fxml"));
                Parent root = loader.load();

                EditProfilController controller = loader.getController();
                controller.setUser(currentUser);
                controller.setProfilController(editProfilController.profilController);
                controller.setContentPane(contentPane);

                // ✅ Restaurer le profil si c'est une modification
                if (editProfilController.currentProfil != null) {
                    controller.setProfil(editProfilController.currentProfil);
                    controller.setMode(true);
                } else {
                    controller.setMode(false);
                }

                contentPane.getChildren().clear();
                contentPane.getChildren().add(root);

                AnchorPane.setTopAnchor(root, 0.0);
                AnchorPane.setBottomAnchor(root, 0.0);
                AnchorPane.setLeftAnchor(root, 0.0);
                AnchorPane.setRightAnchor(root, 0.0);

                System.out.println("✅ Retour à EditProfil");

            } catch (Exception e) {
                System.err.println("❌ Erreur retour EditProfil");
                e.printStackTrace();
            }
        }
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

    private void showInfo(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: #2196f3; -fx-font-weight: bold;");
        }
    }

    public boolean isVerified() {
        return verified;
    }
}
