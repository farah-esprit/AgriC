package org.example.controllers.User;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.AnchorPane;
import org.example.entities.User;
import org.example.services.User.UserService;

public class ChangePasswordController {

    @FXML private PasswordField ancienMdpField;
    @FXML private PasswordField nouveauMdpField;
    @FXML private PasswordField confirmMdpField;
    @FXML private Label messageLabel;

    private User currentUser;
    private UserService userService;
    private AnchorPane contentPane;

    // ✅ Pour admin
    private DashboardAdminController dashboardController;

    // ✅ Pour agriculteur/expert/fournisseur
    private ProfilController profilController;

    @FXML
    public void initialize() {
        userService = new UserService();
        if (messageLabel != null) messageLabel.setText("");
    }

    public void setUser(User user) {
        this.currentUser = user;
    }

    public void setContentPane(AnchorPane contentPane) {
        this.contentPane = contentPane;
    }

    // ✅ Setter admin
    public void setDashboardController(DashboardAdminController controller) {
        this.dashboardController = controller;
    }

    // ✅ Setter utilisateur normal
    public void setProfilController(ProfilController controller) {
        this.profilController = controller;
    }

    @FXML
    private void handleSave() {
        String ancien = ancienMdpField.getText();
        String nouveau = nouveauMdpField.getText();
        String confirm = confirmMdpField.getText();

        if (ancien.isEmpty() || nouveau.isEmpty() || confirm.isEmpty()) {
            showError("❌ Veuillez remplir tous les champs");
            return;
        }
        if (!nouveau.equals(confirm)) {
            showError("❌ Les mots de passe ne correspondent pas");
            return;
        }
        if (nouveau.length() < 6) {
            showError("❌ Minimum 6 caractères");
            return;
        }

        boolean success = userService.changerMotDePasse(currentUser.getId(), ancien, nouveau);
        if (success) {
            showSuccess("✅ Mot de passe changé avec succès !");
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(this::retournerAuProfil);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            showError("❌ Ancien mot de passe incorrect");
        }
    }

    @FXML
    private void handleCancel() {
        retournerAuProfil();
    }

    private void retournerAuProfil() {
        try {
            if (dashboardController != null) {
                // ✅ Cas ADMIN → retour profilAdmin.fxml
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/User/profilAdmin.fxml"));
                Parent root = loader.load();
                ProfilAdminController controller = loader.getController();
                controller.setUser(currentUser);
                controller.setDashboardController(dashboardController);
                chargerDansContentPane(root);

            } else if (profilController != null) {
                // ✅ Cas AGRICULTEUR/EXPERT/FOURNISSEUR → retour profil.fxml
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/profil.fxml"));
                Parent root = loader.load();
                ProfilController controller = loader.getController();
                controller.setUser(currentUser);

                // ✅ Passer le bon dashboardController selon le rôle
                if (profilController.getDashboardAgriculteurController() != null) {
                    controller.setDashboardController(profilController.getDashboardAgriculteurController());
                } else if (profilController.getDashboardExpertController() != null) {
                    controller.setDashboardController(profilController.getDashboardExpertController());
                } else if (profilController.getDashboardFournisseurController() != null) {
                    controller.setDashboardController(profilController.getDashboardFournisseurController());
                }

                chargerDansContentPane(root);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void chargerDansContentPane(Parent root) {
        if (contentPane != null) {
            contentPane.getChildren().clear();
            contentPane.getChildren().add(root);
            AnchorPane.setTopAnchor(root, 0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root, 0.0);
            AnchorPane.setRightAnchor(root, 0.0);
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
}
