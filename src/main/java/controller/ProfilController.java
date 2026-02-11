package controller;

import entities.Profil;
import entities.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import service.ProfilService;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import utils.DataBase;

public class ProfilController {
    @FXML private Label nomLabel;
    @FXML private Label prenomLabel;
    @FXML private Label telephoneLabel;
    @FXML private Label bioLabel;
    @FXML private Label emailLabel;
    @FXML private Button modifierButton;
    @FXML private Button supprimerButton;
    @FXML private Button createProfilButton;
    @FXML private Label messageLabel;
    @FXML private Circle profileCircle;

    private User currentUser;
    private Profil currentProfil;
    private ProfilService profilService;

    // ================= INITIALISATION =================
    @FXML
    public void initialize() {
        profilService = new ProfilService();
        if (messageLabel != null) {
            messageLabel.setText("");
        }
    }

    // ================= DÉFINIR L'UTILISATEUR =================
    public void setUser(User user) {
        this.currentUser = user;
        loadProfilData();
    }

    // ================= CHARGER LES DONNÉES DU PROFIL =================
    private void loadProfilData() {
        try {
            Connection conn = DataBase.getConnection();
            String sql = "SELECT * FROM profil WHERE user_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentUser.getId());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // Profil existe déjà
                currentProfil = new Profil(
                        rs.getInt("id"),
                        rs.getString("bio"),
                        rs.getString("telephone"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("image"),
                        currentUser
                );

                // Afficher les données en lecture seule
                afficherProfil();

                // Afficher boutons Modifier et Supprimer
                if (modifierButton != null) modifierButton.setVisible(true);
                if (supprimerButton != null) supprimerButton.setVisible(true);
                if (createProfilButton != null) createProfilButton.setVisible(false);

            } else {
                // Pas de profil
                currentProfil = null;
                afficherMessagePasDeprofil();

                // Afficher bouton Créer
                if (modifierButton != null) modifierButton.setVisible(false);
                if (supprimerButton != null) supprimerButton.setVisible(false);
                if (createProfilButton != null) createProfilButton.setVisible(true);
            }

        } catch (SQLException e) {
            showError("Erreur de chargement : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= AFFICHER LE PROFIL =================
    private void afficherProfil() {
        if (currentProfil == null) return;

        if (nomLabel != null) nomLabel.setText(currentProfil.getNom());
        if (prenomLabel != null) prenomLabel.setText(currentProfil.getPrenom());
        if (telephoneLabel != null) telephoneLabel.setText(currentProfil.getTelephone());
        if (bioLabel != null) bioLabel.setText(currentProfil.getBio() != null && !currentProfil.getBio().isEmpty()
                ? currentProfil.getBio()
                : "Aucune biographie");
        if (emailLabel != null) emailLabel.setText(currentUser.getEmail());

        // Charger l'image de profil
        if (currentProfil.getImage() != null && !currentProfil.getImage().isEmpty()) {
            loadProfileImage(currentProfil.getImage());
        }
    }

    // ================= AFFICHER MESSAGE PAS DE PROFIL =================
    private void afficherMessagePasDeprofil() {
        if (nomLabel != null) nomLabel.setText("Non renseigné");
        if (prenomLabel != null) prenomLabel.setText("Non renseigné");
        if (telephoneLabel != null) telephoneLabel.setText("Non renseigné");
        if (bioLabel != null) bioLabel.setText("Vous n'avez pas encore créé votre profil");
        if (emailLabel != null) emailLabel.setText(currentUser.getEmail());
    }

    // ================= CHARGER L'IMAGE =================
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

    // ================= CRÉER PROFIL =================
    @FXML
    private void handleCreateProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/editProfil.fxml"));
            Parent root = loader.load();

            EditProfilController controller = loader.getController();
            controller.setProfilController(this);
            controller.setUser(currentUser);
            controller.setMode(false); // Mode création

            Stage stage = new Stage();
            stage.setTitle("Créer mon profil");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.show();

        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= MODIFIER PROFIL =================
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
            controller.setMode(true); // Mode modification

            Stage stage = new Stage();
            stage.setTitle("Modifier mon profil");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.show();

        } catch (Exception e) {
            showError("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= SUPPRIMER PROFIL =================
    @FXML
    private void handleSupprimerProfil(ActionEvent event) {
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
                loadProfilData(); // Rafraîchir l'affichage

            } catch (Exception e) {
                showError("❌ Erreur lors de la suppression du profil");
                e.printStackTrace();
            }
        }
    }

    // ================= RAFRAÎCHIR =================
    public void refreshProfil() {
        loadProfilData();
        showSuccess("✅ Profil mis à jour !");
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

    // ================= RETOUR DASHBOARD =================
    @FXML
    private void handleBackToDashboard(MouseEvent event) {
        try {
            String fxmlFile = "";

            switch (currentUser.getRole()) {
                case ADMIN:
                    fxmlFile = "/adminDashboard.fxml";
                    break;
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
            }

            Stage stage = (Stage) nomLabel.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            System.out.println("Erreur retour dashboard : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= DÉCONNEXION =================
    @FXML
    private void handleLogout(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) nomLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}