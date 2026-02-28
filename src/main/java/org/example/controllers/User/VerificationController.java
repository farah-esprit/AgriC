package org.example.controllers.User;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.entities.EtatCompte;
import org.example.entities.User;
import org.example.services.User.UserService;


import java.sql.Timestamp;

public class VerificationController {

    @FXML private TextField codeField;
    @FXML private Label messageLabel;

    private String email;
    private User user;
    private UserService userService = new UserService();
    private StackPane contentPane; // ✅ AJOUTÉ

    // ✅ SETTER POUR LE CONTENTPANE
    public void setContentPane(StackPane contentPane) {
        this.contentPane = contentPane;
    }

    public void setEmail(String email) {
        this.email = email;
        user = userService.findByEmail(email);
        if (user == null) {
            messageLabel.setText("❌ Utilisateur introuvable !");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleVerify() {
        String codeSaisi = codeField.getText().trim();

        if (codeSaisi.isEmpty()) {
            messageLabel.setText("❌ Veuillez entrer le code");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if (user == null) {
            user = userService.findByEmail(email);
            if (user == null) {
                messageLabel.setText("❌ Utilisateur introuvable");
                messageLabel.setStyle("-fx-text-fill: red;");
                return;
            }
        }

        boolean verificationSuccess = false;

        // Vérifier code et expiration
        if (user.getVerificationCode() != null && user.getVerificationCode().trim().equals(codeSaisi)) {
            if (user.getCodeExpiration() != null && user.getCodeExpiration().after(new Timestamp(System.currentTimeMillis()))) {

                user.setEtatCompte(EtatCompte.ACTIF);
                user.setVerificationCode(null);
                user.setCodeExpiration(null);
                userService.modifier(user);

                messageLabel.setText("✅ Compte activé avec succès !");
                messageLabel.setStyle("-fx-text-fill: green;");
                verificationSuccess = true;

            } else {
                messageLabel.setText("❌ Code expiré, veuillez demander un nouveau code");
                messageLabel.setStyle("-fx-text-fill: red;");
            }

        } else {
            messageLabel.setText("❌ Code invalide");
            messageLabel.setStyle("-fx-text-fill: red;");
        }

        // Revenir automatiquement au login si vérification réussie
        if (verificationSuccess) {
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    Platform.runLater(this::handleBackToLogin);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            // ✅ CHARGER LE FXML COMPLET
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();

            // ✅ RÉCUPÉRER LE CONTROLLER
            LoginController controller = loader.getController();

            // ✅ SI ON EST DANS UN STACKPANE (mode dynamique)
            if (contentPane != null) {
                // Extraire le formulaire du HBox
                if (root instanceof javafx.scene.layout.HBox hbox) {
                    if (hbox.getChildren().size() >= 2) {
                        javafx.scene.layout.StackPane whitePane =
                                (javafx.scene.layout.StackPane) hbox.getChildren().get(1);

                        if (whitePane.getChildren().size() > 0) {
                            javafx.scene.Node loginForm = whitePane.getChildren().get(0);
                            whitePane.getChildren().remove(loginForm);

                            contentPane.getChildren().clear();
                            contentPane.getChildren().add(loginForm);

                            controller.contentPane = this.contentPane;
                            System.out.println("✅ Retour au login dans StackPane");
                        }
                    }
                }
            } else {
                // ✅ SINON, FALLBACK : changer toute la Scene
                Stage stage = (Stage) messageLabel.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("AgriConnect - Login");
                System.out.println("✅ Retour au login (nouvelle Scene)");
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (messageLabel != null) {
                messageLabel.setText("❌ Erreur lors du retour au login");
                messageLabel.setStyle("-fx-text-fill: red;");
            }
        }
    }
}