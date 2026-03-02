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
    @FXML private AnchorPane contentPane;
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
    @FXML private MenuItem menuModifier;
    @FXML private MenuItem menuSupprimer;

    private User currentUser;
    private Profil currentProfil;
    private ProfilService profilService;
    private UserService userService;

    // ✅ Un controller par rôle
    private DashboardAgriculteurController dashboardAgriculteurController;
    private DashboardExpertController dashboardExpertController;
    private DashboardFournisseurController dashboardFournisseurController;

    @FXML
    public void initialize() {
        profilService = new ProfilService();
        userService = new UserService();
        if (messageLabel != null) messageLabel.setText("");
    }

    public void setUser(User user) {
        if (user == null) {
            System.err.println("❌ ERREUR : User est null dans setUser()");
            return;
        }
        this.currentUser = user;
        System.out.println("✅ User défini dans ProfilController : " + user.getNom() + " (ID: " + user.getId() + ")");

        if (roleLabel != null) {
            switch (user.getRole().toString()) {
                case "AGRICULTEUR": roleLabel.setText("Agriculteur"); break;
                case "EXPERT": roleLabel.setText("Expert"); break;
                case "FOURNISSEUR": roleLabel.setText("Fournisseur"); break;
                case "ADMIN": roleLabel.setText("Administrateur"); break;
                default: roleLabel.setText("Utilisateur");
            }
        }
        if (messageLabel != null) messageLabel.setText("");
        loadProfilData();
    }

    // ✅ Setters pour chaque type de dashboard
    public void setDashboardController(DashboardAgriculteurController controller) {
        this.dashboardAgriculteurController = controller;
    }

    public void setDashboardController(DashboardExpertController controller) {
        this.dashboardExpertController = controller;
    }

    public void setDashboardController(DashboardFournisseurController controller) {
        this.dashboardFournisseurController = controller;
    }

    // ✅ Getters
    public DashboardAgriculteurController getDashboardAgriculteurController() {
        return dashboardAgriculteurController;
    }

    public DashboardExpertController getDashboardExpertController() {
        return dashboardExpertController;
    }

    public DashboardFournisseurController getDashboardFournisseurController() {
        return dashboardFournisseurController;
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
                if (menuModifier != null) menuModifier.setVisible(true);
                if (menuSupprimer != null) menuSupprimer.setVisible(true);
                if (createProfilButton != null) createProfilButton.setVisible(false);
            } else {
                currentProfil = null;
                afficherMessagePasDeprofil();
                if (menuModifier != null) menuModifier.setVisible(false);
                if (menuSupprimer != null) menuSupprimer.setVisible(false);
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
                ? currentProfil.getBio() : "Aucune biographie");
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

    @FXML
    private void handleCreateProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/editProfil.fxml"));
            Parent root = loader.load();
            EditProfilController controller = loader.getController();
            controller.setProfilController(this);
            controller.setUser(currentUser);
            controller.setMode(false);
            controller.setContentPane(contentPane);
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
            controller.setContentPane(contentPane);
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
                int profilId = currentProfil.getId();
                String imagePath = currentProfil.getImage();

                System.out.println("🗑️ Tentative suppression profil ID=" + profilId); // ✅ log

                // ✅ Supprimer en base
                profilService.supprimer(profilId);
                System.out.println("✅ Suppression BDD OK pour ID=" + profilId); // ✅ log

                // ✅ Supprimer image physique seulement si BDD OK
                if (imagePath != null && !imagePath.isEmpty()) {
                    deleteImageFile(imagePath);
                }

                currentProfil = null; // ✅ Réinitialiser le profil local
                showSuccess("✅ Profil supprimé avec succès !");
                loadProfilData(); // ✅ Recharger l'affichage

            } catch (SQLException e) {
                System.err.println("❌ Erreur SQL suppression : " + e.getMessage()); // ✅ log précis
                e.printStackTrace();
                showError("❌ Erreur base de données : " + e.getMessage());
            } catch (Exception e) {
                System.err.println("❌ Erreur suppression : " + e.getMessage()); // ✅ log précis
                e.printStackTrace();
                showError("❌ Erreur : " + e.getMessage());
            }
        }
    }
    // ✅ NOUVELLE MÉTHODE : Supprimer le fichier image
    private void deleteImageFile(String imagePath) {
        try {
            File imageFile = new File(imagePath);

            if (imageFile.exists()) {
                boolean deleted = imageFile.delete();

                if (deleted) {
                    System.out.println("✅ Image supprimée : " + imagePath);
                } else {
                    System.err.println("⚠️ Impossible de supprimer l'image : " + imagePath);
                }
            } else {
                System.out.println("ℹ️ Fichier image introuvable : " + imagePath);
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la suppression de l'image : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleChangePassword(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/changePassword.fxml"));
            Parent root = loader.load();

            ChangePasswordController controller = loader.getController();
            controller.setUser(currentUser);
            controller.setContentPane(contentPane);
            controller.setProfilController(this); // ✅ passer this

            contentPane.getChildren().clear();
            contentPane.getChildren().add(root);
            AnchorPane.setTopAnchor(root, 0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root, 0.0);
            AnchorPane.setRightAnchor(root, 0.0);

            System.out.println("✅ ChangePassword chargé dans contentPane");
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
                controller.setContentPane(contentPane);
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

    public void refreshProfil() {
        loadProfilData();
        showSuccess("✅ Profil mis à jour !");
    }

    // ✅ Retour au bon dashboard selon le rôle
    @FXML
    private void handleBackToDashboard(MouseEvent event) {
        if (dashboardAgriculteurController != null) {
            dashboardAgriculteurController.reloadDashboardContent();
        } else if (dashboardExpertController != null) {
            dashboardExpertController.reloadDashboardContent();
        } else if (dashboardFournisseurController != null) {
            dashboardFournisseurController.reloadDashboardContent();
        } else {
            System.err.println("❌ Aucun dashboardController défini !");
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