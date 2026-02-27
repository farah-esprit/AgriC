package controller;

import entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import service.TwoFactorAuthService;
import service.UserService;

public class Verify2FAController {
    @FXML private Label errorLabel;

    @FXML private TextField codeField;
    @FXML private Label emailLabel;
    @FXML private Label messageLabel;
    @FXML private Button btnVerify;

    private TwoFactorAuthService tfaService;
    private UserService userService;
    private User currentUser;


    private StackPane contentPane;

    public void setContentPane(StackPane contentPane) {
        this.contentPane = contentPane;
    }
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
    }

    @FXML
    private void handleVerify() {
        String code = codeField.getText().trim();

        if (code.isEmpty() || code.length() != 6) {
            showError("❌ Code invalide (6 chiffres requis)");
            return;
        }

        String secret = userService.get2FASecret(currentUser.getId());

        if (secret == null) {
            showError("❌ Secret 2FA introuvable");
            return;
        }

        boolean isValid = tfaService.verifyCode(secret, code);

        if (isValid) {
            showSuccess("✅ Code vérifié !");

            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    Platform.runLater(this::redirectToDashboard);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } else {
            showError("❌ Code incorrect");
            codeField.clear();
        }
    }

    private void redirectToDashboard() {
        try {
            String fxmlFile = "";

            switch (currentUser.getRole()) {
                case AGRICULTEUR: fxmlFile = "/agriculteurDashboard.fxml"; break;
                case EXPERT:      fxmlFile = "/expertDashboard.fxml";      break;
                case FOURNISSEUR: fxmlFile = "/fournisseurDashboard.fxml"; break;
                case ADMIN:       fxmlFile = "/adminDashboard.fxml";       break;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            Object controller = loader.getController();

            if (controller instanceof DashboardAdminController)
                ((DashboardAdminController) controller).setUser(currentUser);
            else if (controller instanceof DashboardAgriculteurController)
                ((DashboardAgriculteurController) controller).setUser(currentUser);
            else if (controller instanceof DashboardFournisseurController)
                ((DashboardFournisseurController) controller).setUser(currentUser);
            else if (controller instanceof DashboardExpertController)
                ((DashboardExpertController) controller).setUser(currentUser);

            // ✅ OBTENIR LA FENÊTRE ACTUELLE
            Stage stage = (Stage) contentPane.getScene().getWindow();

            // ✅ CRÉER UNE NOUVELLE SCENE
            Scene scene = new Scene(root);

            // ✅ LIER LES DIMENSIONS
            if (root instanceof Region r) {
                r.prefWidthProperty().bind(scene.widthProperty());
                r.prefHeightProperty().bind(scene.heightProperty());
            }

            // ✅ REMPLACER LA SCENE
            stage.setScene(scene);
            stage.setTitle("AgriConnect - Dashboard " + currentUser.getRole());

            // ✅ MAXIMISER LA FENÊTRE
            Platform.runLater(() -> stage.setMaximized(true));

            System.out.println("✅ Redirection plein écran vers " + fxmlFile);

        } catch (Exception e) {
            System.err.println("❌ Erreur redirection :");
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

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;

        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];

        if (local.length() <= 3) {
            return local.charAt(0) + "***@" + domain;
        }

        return local.substring(0, 2) + "***" +
                local.charAt(local.length() - 1) + "@" + domain;
    }

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
    }

    private void showSuccess(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
    }
}