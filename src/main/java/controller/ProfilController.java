package controller;
import entities.Profil;
import entities.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import service.ProfilService;
import service.UserService;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import utils.MyDataBase;
public class ProfilController {
    @FXML private AnchorPane contentPane; // ✅ AJOUTÉ
    @FXML private Label nomLabel;
    @FXML private Label prenomLabel;
    @FXML private Label telephoneLabel;
    @FXML private Label bioLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;
    @FXML private Button modifierButton;
    @FXML private Button supprimerButton;
    @FXML private Button createProfilButton;
    @FXML private Label messageLabel;
    @FXML private Circle profileCircle;
    private User currentUser;
    private Profil currentProfil;
    private ProfilService profilService;
    private UserService userService;
    private DashboardAgriculteurController dashboardAgriculteurController;
    private DashboardAgriculteurController dashboardController;

    @FXML
    public void initialize() {
        profilService = new ProfilService();
        userService = new UserService();
        if (messageLabel != null) {
            messageLabel.setText("");
        }
    }

    public void setUser(User user) {
        if (user == null) {
            System.err.println("❌ ERREUR : User est null dans setUser()");
            return;
        }

        this.currentUser = user;
        System.out.println("✅ User défini dans ProfilController : " + user.getNom() + " (ID: " + user.getId() + ")");

        if (roleLabel != null) {
            String role = user.getRole().toString();
            switch (role) {
                case "AGRICULTEUR":
                    roleLabel.setText("Agriculteur");
                    break;
                case "EXPERT":
                    roleLabel.setText("Expert");
                    break;
                case "FOURNISSEUR":
                    roleLabel.setText("Fournisseur");
                    break;
                case "ADMIN":
                    roleLabel.setText("Administrateur");
                    break;
                default:
                    roleLabel.setText("Utilisateur");
            }
        }

        if (messageLabel != null) {
            messageLabel.setText("");
        }

        loadProfilData();
    }

    private void loadProfilData() {
        if (currentUser == null) {
            System.err.println("❌ ERREUR : currentUser est null dans loadProfilData()");
            return;
        }

        try {
            Connection conn = MyDataBase.getConnection();
            String sql = "SELECT * FROM profil WHERE user_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentUser.getId());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                currentProfil = new Profil(
                        rs.getInt("id"),
                        rs.getString("bio"),
                        rs.getString("telephone"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("image"),
                        currentUser
                );

                afficherProfil();

                if (modifierButton != null) modifierButton.setVisible(true);
                if (supprimerButton != null) supprimerButton.setVisible(true);
                if (createProfilButton != null) createProfilButton.setVisible(false);

            } else {
                currentProfil = null;
                afficherMessagePasDeprofil();

                if (modifierButton != null) modifierButton.setVisible(false);
                if (supprimerButton != null) supprimerButton.setVisible(false);
                if (createProfilButton != null) createProfilButton.setVisible(true);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur SQL : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void afficherProfil() {
        if (currentProfil == null) return;

        if (nomLabel != null) nomLabel.setText(currentProfil.getNom());
        if (prenomLabel != null) prenomLabel.setText(currentProfil.getPrenom());
        if (telephoneLabel != null) telephoneLabel.setText(currentProfil.getTelephone());
        if (bioLabel != null) bioLabel.setText(currentProfil.getBio() != null && !currentProfil.getBio().isEmpty()
                ? currentProfil.getBio()
                : "Aucune biographie");
        if (emailLabel != null) emailLabel.setText(currentUser.getEmail());

        if (currentProfil.getImage() != null && !currentProfil.getImage().isEmpty()) {
            loadProfileImage(currentProfil.getImage());
        }
    }

    private void afficherMessagePasDeprofil() {
        if (nomLabel != null) nomLabel.setText("Non renseigné");
        if (prenomLabel != null) prenomLabel.setText("Non renseigné");
        if (telephoneLabel != null) telephoneLabel.setText("Non renseigné");
        if (bioLabel != null) bioLabel.setText("Vous n'avez pas encore créé votre profil");

        if (emailLabel != null) {
            if (currentUser != null && currentUser.getEmail() != null) {
                emailLabel.setText(currentUser.getEmail());
            } else {
                emailLabel.setText("Email non disponible");
            }
        }
    }

    private void loadProfileImage(String imagePath) {
        try {
            File file = new File(imagePath);
            if (file.exists() && profileCircle != null) {
                Image image = new Image(file.toURI().toString());
                profileCircle.setFill(new ImagePattern(image));
            }
        } catch (Exception e) {
            System.out.println("Erreur chargement image : " + e.getMessage());
        }
    }

    // ✅ CRÉER PROFIL - Charger dans contentPane
    @FXML
    private void handleCreateProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/editProfil.fxml"));
            Parent root = loader.load();

            EditProfilController controller = loader.getController();
            controller.setProfilController(this);
            controller.setUser(currentUser);
            controller.setMode(false);
            controller.setContentPane(contentPane); // 🔥 IMPORTANT

            contentPane.getChildren().clear();
            contentPane.getChildren().add(root);

            AnchorPane.setTopAnchor(root, 0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root, 0.0);
            AnchorPane.setRightAnchor(root, 0.0);

            System.out.println("✅ EditProfil chargé dans contentPane");

        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleModifierProfil() {
        if (currentProfil == null) {
            showError("Aucun profil à modifier");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/editProfil.fxml"));
            Parent root = loader.load();

            EditProfilController controller = loader.getController();
            controller.setProfilController(this);
            controller.setUser(currentUser);
            controller.setProfil(currentProfil);
            controller.setMode(true);
            controller.setContentPane(contentPane); // 🔥 IMPORTANT

            contentPane.getChildren().clear();
            contentPane.getChildren().add(root);

            AnchorPane.setTopAnchor(root, 0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root, 0.0);
            AnchorPane.setRightAnchor(root, 0.0);

            System.out.println("✅ EditProfil (modification) chargé dans contentPane");

        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSupprimerProfil(ActionEvent event) {
        if (currentProfil == null) {
            showError("Aucun profil à supprimer");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de suppression");
        confirmation.setHeaderText("Supprimer votre profil ?");
        confirmation.setContentText("⚠️ ATTENTION : Cette action supprimera définitivement votre profil.\n\nVoulez-vous continuer ?");

        ButtonType buttonOui = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonNon = new ButtonType("Non, annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(buttonOui, buttonNon);

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == buttonOui) {
            try {
                profilService.supprimer(currentProfil.getId());
                showSuccess("✅ Profil supprimé avec succès !");
                loadProfilData();

            } catch (Exception e) {
                showError("❌ Erreur lors de la suppression du profil");
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleChangePassword(ActionEvent event) {
        try {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Changer le mot de passe");
            dialog.setHeaderText("Modifier votre mot de passe");

            ButtonType validerButton = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(validerButton, ButtonType.CANCEL);

            VBox content = new VBox(15);
            content.setPadding(new Insets(20));

            PasswordField ancienMdpField = new PasswordField();
            ancienMdpField.setPromptText("Ancien mot de passe");
            ancienMdpField.setPrefHeight(40);

            PasswordField nouveauMdpField = new PasswordField();
            nouveauMdpField.setPromptText("Nouveau mot de passe");
            nouveauMdpField.setPrefHeight(40);

            PasswordField confirmMdpField = new PasswordField();
            confirmMdpField.setPromptText("Confirmer le mot de passe");
            confirmMdpField.setPrefHeight(40);

            content.getChildren().addAll(
                    new Label("Ancien mot de passe :"),
                    ancienMdpField,
                    new Label("Nouveau mot de passe :"),
                    nouveauMdpField,
                    new Label("Confirmer le mot de passe :"),
                    confirmMdpField
            );

            dialog.getDialogPane().setContent(content);

            Optional<ButtonType> result = dialog.showAndWait();

            if (result.isPresent() && result.get() == validerButton) {
                String ancienMdp = ancienMdpField.getText();
                String nouveauMdp = nouveauMdpField.getText();
                String confirmMdp = confirmMdpField.getText();

                if (ancienMdp.isEmpty() || nouveauMdp.isEmpty() || confirmMdp.isEmpty()) {
                    showError("❌ Veuillez remplir tous les champs");
                    return;
                }

                if (!nouveauMdp.equals(confirmMdp)) {
                    showError("❌ Les mots de passe ne correspondent pas");
                    return;
                }

                if (nouveauMdp.length() < 6) {
                    showError("❌ Le mot de passe doit contenir au moins 6 caractères");
                    return;
                }

                boolean success = userService.changerMotDePasse(currentUser.getId(), ancienMdp, nouveauMdp);

                if (success) {
                    showSuccess("✅ Mot de passe changé avec succès !");
                } else {
                    showError("❌ Ancien mot de passe incorrect");
                }
            }

        } catch (Exception e) {
            showError("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleManage2FA(ActionEvent event) {
        boolean is2FAEnabled = userService.is2FAEnabled(currentUser.getId());

        if (is2FAEnabled) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("2FA activé");
            alert.setHeaderText("L'authentification à deux facteurs est active");
            alert.setContentText("Votre compte est protégé par 2FA.");
            alert.showAndWait();
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/twoFactor.fxml"));
                Parent root = loader.load();

                TwoFactorController controller = loader.getController();
                controller.setUser(currentUser);
                controller.setContentPane(contentPane); // ✅ AJOUTÉ

                contentPane.getChildren().clear();
                contentPane.getChildren().add(root);

                AnchorPane.setTopAnchor(root, 0.0);
                AnchorPane.setBottomAnchor(root, 0.0);
                AnchorPane.setLeftAnchor(root, 0.0);
                AnchorPane.setRightAnchor(root, 0.0);

            } catch (Exception e) {
                showError("❌ Erreur : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // ✅ RECHARGER LA VUE PROFIL
    public void refreshProfil() {
        loadProfilData();
        showSuccess("✅ Profil mis à jour !");
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

    @FXML
    private void handleBackToDashboard(MouseEvent event) {
        if (currentUser == null) return;

        try {
            String fxmlFile = "";

            switch (currentUser.getRole()) {
              
                case AGRICULTEUR:
                    fxmlFile = "/agriculteurDashboard.fxml";
                    break;
                case EXPERT:
                    fxmlFile = "/expertDashboard.fxml";
                    break;
                case FOURNISSEUR:
                    fxmlFile = "/fournisseurDashboard.fxml";
                    break;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            Object controller = loader.getController();

            if (controller instanceof DashboardAdminController) {
                ((DashboardAdminController) controller).setUser(currentUser);
            } else if (controller instanceof DashboardAgriculteurController) {
                ((DashboardAgriculteurController) controller).setUser(currentUser);
            } else if (controller instanceof DashboardExpertController) {
                ((DashboardExpertController) controller).setUser(currentUser);
            } else if (controller instanceof DashboardFournisseurController) {
                ((DashboardFournisseurController) controller).setUser(currentUser);
            }

            Stage stage = (Stage) nomLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Dashboard");

        } catch (Exception e) {
            System.err.println("❌ Erreur retour dashboard : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) nomLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDashboardController(DashboardAgriculteurController dashboardController) {
        this.dashboardController = dashboardController;
    }

    public DashboardAgriculteurController getDashboardController() {
        return dashboardController;
    }
}