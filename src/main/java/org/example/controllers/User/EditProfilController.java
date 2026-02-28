package org.example.controllers.User;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.entities.Profil;
import org.example.entities.User;
import org.example.services.User.ProfilService;
import org.example.services.User.SmsService;
import org.example.utils.ValidationUtils;

import java.io.File;

public class EditProfilController {
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField telephoneField;
    @FXML private TextArea bioField;
    @FXML private Button saveButton;
    @FXML private Button deleteButton;
    @FXML private Button uploadButton;
    @FXML private Label messageLabel;
    @FXML private Label titleLabel;
    @FXML private Circle profileCircle;
    @FXML private AnchorPane contentPane;
    public ProfilController profilController;
    public Profil currentProfil;
    private User currentUser;
    private ProfilService profilService;
    private String selectedImagePath = "";
    private boolean isEditMode = false;
    private SmsService smsService;
    private boolean smsVerified = false;

    @FXML
    public void initialize() {
        profilService = new ProfilService();
        smsService = new SmsService();
        if (messageLabel != null) ValidationUtils.clearMessage(messageLabel);
        setupRealTimeValidation();
    }

    public void setContentPane(AnchorPane contentPane) {
        this.contentPane = contentPane;
    }

    private void setupRealTimeValidation() {
        if (nomField != null) {
            nomField.textProperty().addListener((obs, old, newVal) -> {
                if (!newVal.trim().isEmpty()) {
                    if (ValidationUtils.isValidName(newVal)) ValidationUtils.setFieldSuccess(nomField);
                    else ValidationUtils.setFieldError(nomField);
                } else ValidationUtils.resetFieldStyle(nomField);
            });
        }

        if (prenomField != null) {
            prenomField.textProperty().addListener((obs, old, newVal) -> {
                if (!newVal.trim().isEmpty()) {
                    if (ValidationUtils.isValidName(newVal)) ValidationUtils.setFieldSuccess(prenomField);
                    else ValidationUtils.setFieldError(prenomField);
                } else ValidationUtils.resetFieldStyle(prenomField);
            });
        }

        if (telephoneField != null) {
            telephoneField.textProperty().addListener((obs, old, newVal) -> {
                if (!newVal.trim().isEmpty()) {
                    if (ValidationUtils.isValidPhone(newVal)) ValidationUtils.setFieldSuccess(telephoneField);
                    else ValidationUtils.setFieldError(telephoneField);
                } else ValidationUtils.resetFieldStyle(telephoneField);
            });
        }
    }

    public void setProfilController(ProfilController controller) {
        this.profilController = controller;
    }

    public void setUser(User user) {
        this.currentUser = user;
    }

    public void setProfil(Profil profil) {
        this.currentProfil = profil;
        if (profil != null) {
            if (nomField != null) nomField.setText(profil.getNom());
            if (prenomField != null) prenomField.setText(profil.getPrenom());
            if (telephoneField != null) telephoneField.setText(profil.getTelephone());
            if (bioField != null) bioField.setText(profil.getBio());
            selectedImagePath = profil.getImage();
            if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                loadProfileImage(selectedImagePath);
            }
        }
    }

    public void setMode(boolean isEdit) {
        this.isEditMode = isEdit;
        if (isEdit) {
            if (titleLabel != null) titleLabel.setText("✏️ Modifier mon profil");
            if (saveButton != null) saveButton.setText("💾 Enregistrer");
            if (deleteButton != null) deleteButton.setVisible(true);
        } else {
            if (titleLabel != null) titleLabel.setText("➕ Créer mon profil");
            if (saveButton != null) saveButton.setText("💾 Créer");
            if (deleteButton != null) deleteButton.setVisible(false);
        }
    }

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

    private void loadProfileImage(String imagePath) {
        try {
            File file = new File(imagePath);
            if (file.exists() && profileCircle != null) {
                Image image = new Image(file.toURI().toString());
                profileCircle.setFill(new ImagePattern(image));
            }
        } catch (Exception e) {
            if (messageLabel != null) ValidationUtils.showError(messageLabel, "Erreur de chargement de l'image");
        }
    }

    @FXML
    private void handleSaveProfil() {
        String nom = ValidationUtils.sanitize(nomField.getText());
        String prenom = ValidationUtils.sanitize(prenomField.getText());
        String telephone = ValidationUtils.sanitize(telephoneField.getText());
        String bio = ValidationUtils.sanitize(bioField.getText());

        if (!ValidationUtils.isNotEmpty(nom) || !ValidationUtils.isNotEmpty(prenom) || !ValidationUtils.isNotEmpty(telephone)) {
            ValidationUtils.showError(messageLabel, "Veuillez remplir tous les champs obligatoires (*)");
            return;
        }
        if (!ValidationUtils.isValidName(nom)) {
            ValidationUtils.showError(messageLabel, "Format de nom invalide");
            ValidationUtils.setFieldError(nomField);
            return;
        }
        if (!ValidationUtils.isValidName(prenom)) {
            ValidationUtils.showError(messageLabel, "Format de prénom invalide");
            ValidationUtils.setFieldError(prenomField);
            return;
        }
        if (!ValidationUtils.isValidPhone(telephone)) {
            ValidationUtils.showError(messageLabel, "Format de téléphone invalide (ex: +216XXXXXXXX)");
            ValidationUtils.setFieldError(telephoneField);
            return;
        }

        // ✅ VÉRIFIER SI LE NUMÉRO A CHANGÉ
        boolean phoneChanged = false;
        if (currentProfil != null) {
            phoneChanged = !telephone.equals(currentProfil.getTelephone());
        } else {
            phoneChanged = true;
        }

        // ✅ SI LE NUMÉRO A CHANGÉ → VÉRIFICATION SMS
        if (phoneChanged && !smsVerified) {
            ValidationUtils.showInfo(messageLabel, "📲 Envoi du code de vérification...");

            // ✅ Envoyer SMS
            boolean sent = smsService.sendVerificationCode(telephone, currentUser.getId());

            if (!sent) {
                ValidationUtils.showError(messageLabel, "❌ Erreur envoi SMS !");
                return;
            }

            // ✅ Ouvrir popup de vérification
            openSmsVerificationDialog(telephone);
            return;
        }

        // ✅ SI SMS VÉRIFIÉ OU NUMÉRO INCHANGÉ → SAUVEGARDER
        saveProfil(nom, prenom, telephone, bio);
    }

    private void openSmsVerificationDialog(String phone) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/verifySms.fxml"));
            Parent root = loader.load();

            VerifySmsController controller = loader.getController();
            controller.setUser(currentUser);
            controller.setPhoneNumber(phone);
            controller.setEditProfilController(this);
            controller.setContentPane(contentPane); // ✅ Passer le contentPane

            // ✅ Charger dans le contentPane au lieu d'une nouvelle fenêtre
            contentPane.getChildren().clear();
            contentPane.getChildren().add(root);

            AnchorPane.setTopAnchor(root, 0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root, 0.0);
            AnchorPane.setRightAnchor(root, 0.0);

            System.out.println("✅ Vérification SMS chargée dans contentPane");

        } catch (Exception e) {
            ValidationUtils.showError(messageLabel, "❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void onSmsVerified() {
        smsVerified = true;

        String nom = ValidationUtils.sanitize(nomField.getText());
        String prenom = ValidationUtils.sanitize(prenomField.getText());
        String telephone = ValidationUtils.sanitize(telephoneField.getText());
        String bio = ValidationUtils.sanitize(bioField.getText());

        saveProfil(nom, prenom, telephone, bio);
    }

    private void saveProfil(String nom, String prenom, String telephone, String bio) {
        try {
            if (!isEditMode || currentProfil == null) {
                Profil newProfil = new Profil(bio, telephone, nom, prenom, selectedImagePath, currentUser);
                profilService.ajouter(newProfil);
                ValidationUtils.showSuccess(messageLabel, "✅ Profil créé avec succès !");
            } else {
                currentProfil.setNom(nom);
                currentProfil.setPrenom(prenom);
                currentProfil.setTelephone(telephone);
                currentProfil.setBio(bio);
                currentProfil.setImage(selectedImagePath);
                profilService.modifier(currentProfil);
                ValidationUtils.showSuccess(messageLabel, "✅ Profil modifié avec succès !");
            }

            if (profilController != null) profilController.refreshProfil();

            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    Platform.runLater(this::handleCancel);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            ValidationUtils.showError(messageLabel, "❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

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
                if (profilController != null) profilController.refreshProfil();
                handleCancel();
            } catch (Exception e) {
                ValidationUtils.showError(messageLabel, "Erreur : " + e.getMessage());
            }
        }
    }

    private void redirectToProfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/profil.fxml"));
            Parent profilContent = loader.load();

            ProfilController controller = loader.getController();
            controller.setUser(currentUser);

            if (profilController != null) {
                if (profilController.getDashboardAgriculteurController() != null) {
                    controller.setDashboardController(profilController.getDashboardAgriculteurController());
                } else if (profilController.getDashboardExpertController() != null) {
                    controller.setDashboardController(profilController.getDashboardExpertController());
                } else if (profilController.getDashboardFournisseurController() != null) {
                    controller.setDashboardController(profilController.getDashboardFournisseurController());
                }
            }

            if (contentPane != null) {
                contentPane.getChildren().clear();
                contentPane.getChildren().add(profilContent);
                AnchorPane.setTopAnchor(profilContent, 0.0);
                AnchorPane.setBottomAnchor(profilContent, 0.0);
                AnchorPane.setLeftAnchor(profilContent, 0.0);
                AnchorPane.setRightAnchor(profilContent, 0.0);
                System.out.println("✅ Retour au profil");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur retour profil");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        redirectToProfil();
    }
}
