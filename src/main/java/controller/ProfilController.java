package controller;

import entities.Profil;
import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import service.ProfilService;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import utils.DataBase;

public class ProfilController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField telephoneField;
    @FXML private TextArea bioField;
    @FXML private Button saveButton;
    @FXML private Button deleteButton;
    @FXML private Button uploadButton;
    @FXML private Label messageLabel;
    @FXML private Circle profileCircle;
    @FXML private ImageView profileImageView;
    @FXML
    private Label welcomeLabel;
    private User currentUser;
    private Profil currentProfil;
    private ProfilService profilService;
    private String selectedImagePath = "";

    //INITIALISATION
    @FXML
    public void initialize() {
        profilService = new ProfilService();
        messageLabel.setText("");
    }

    //DÉFINIR L'UTILISATEUR
    public void setUser(User user) {
        this.currentUser = user;
        loadProfilData();
    }

    //CHARGER LES DONNÉES DU PROFIL
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

                // Remplir les champs
                nomField.setText(currentProfil.getNom());
                prenomField.setText(currentProfil.getPrenom());
                telephoneField.setText(currentProfil.getTelephone());
                bioField.setText(currentProfil.getBio());
                selectedImagePath = currentProfil.getImage();

                // Afficher l'image si elle existe
                if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                    loadProfileImage(selectedImagePath);
                }

                // Afficher le bouton supprimer
                deleteButton.setVisible(true);
                saveButton.setText("✏️ Modifier");

            } else {
                // Nouveau profil
                currentProfil = null;
                deleteButton.setVisible(false);
                saveButton.setText("💾 Créer profil");
            }

        } catch (SQLException e) {
            showError("Erreur de chargement : " + e.getMessage());
        }
    }

    //UPLOAD IMAGE
    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File file = fileChooser.showOpenDialog(uploadButton.getScene().getWindow());

        if (file != null) {
            selectedImagePath = file.getAbsolutePath();
            loadProfileImage(selectedImagePath);
        }
    }

    //CHARGER L'IMAGE
    private void loadProfileImage(String imagePath) {
        try {
            File file = new File(imagePath);
            if (file.exists()) {
                Image image = new Image(file.toURI().toString());
                profileCircle.setFill(new ImagePattern(image));
            }
        } catch (Exception e) {
            showError("Erreur de chargement de l'image");
        }
    }

    //ENREGISTRER/MODIFIER PROFIL
    @FXML
    private void handleSaveProfil() {
        // Validation
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String telephone = telephoneField.getText().trim();
        String bio = bioField.getText().trim();

        if (nom.isEmpty() || prenom.isEmpty() || telephone.isEmpty()) {
            showError(" Veuillez remplir tous les champs obligatoires (*)");
            return;
        }

        if (!isValidPhone(telephone)) {
            showError(" Format de téléphone invalide");
            return;
        }

        try {
            if (currentProfil == null) {
                // CRÉATION
                Profil newProfil = new Profil(bio, telephone, nom, prenom, selectedImagePath, currentUser);
                profilService.ajouter(newProfil);
                showSuccess("Profil créé avec succès !");
                loadProfilData(); // Recharger pour récupérer l'ID

            } else {
                // MODIFICATION
                currentProfil.setNom(nom);
                currentProfil.setPrenom(prenom);
                currentProfil.setTelephone(telephone);
                currentProfil.setBio(bio);
                currentProfil.setImage(selectedImagePath);

                profilService.modifier(currentProfil);
                showSuccess("Profil modifié avec succès !");
            }

        } catch (Exception e) {
            showError("Erreur lors de l'enregistrement : " + e.getMessage());
        }
    }

    //SUPPRIMER PROFIL
    @FXML
    private void handleDeleteProfil() {
        if (currentProfil == null) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer le profil ?");
        confirmation.setContentText("Cette action est irréversible.");

        if (confirmation.showAndWait().get() == ButtonType.OK) {
            try {
                profilService.supprimer(currentProfil.getId());
                showSuccess("Profil supprimé !");

                // Réinitialiser les champs
                nomField.clear();
                prenomField.clear();
                telephoneField.clear();
                bioField.clear();
                selectedImagePath = "";
                profileCircle.setFill(javafx.scene.paint.Color.web("#dcdcdc"));

                loadProfilData();

            } catch (Exception e) {
                showError("Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    //VALIDATION TÉLÉPHONE
    private boolean isValidPhone(String phone) {
        return phone.matches("^\\+?[0-9]{8,15}$");
    }

    //MESSAGES
    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
    }

    private void showSuccess(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
    }
    @FXML
    private void handleLogout(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //RETOUR DASHBOARD
    @FXML
    private void handleBackToDashboard(MouseEvent event) {
        try {
            String fxmlFile = "";

            // Choisir le dashboard selon le rôle de l'utilisateur
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

            // Passer l'utilisateur au controller approprié
            Object controller = loader.getController();

            if (controller instanceof DashboardAdminController) {
                ((DashboardAdminController) controller).setUser(currentUser);
            } else if (controller instanceof DashboardAgriculteurController) {
                ((DashboardAgriculteurController) controller).setUser(currentUser);
            }

            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AgriConnect - Dashboard");

        } catch (Exception e) {
            System.out.println("Erreur retour dashboard : " + e.getMessage());
            e.printStackTrace();
        }
    }
}