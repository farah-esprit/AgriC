package controller;
import entities.Profil;
import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import service.ProfilService;
import utils.ValidationUtils;
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
    private User currentUser;
    private Profil currentProfil;
    private ProfilService profilService;
    private ProfilController profilController;
    private String selectedImagePath = "";
    private boolean isEditMode = false;

    @FXML
    public void initialize() {
        profilService = new ProfilService();
        ValidationUtils.clearMessage(messageLabel);
        setupRealTimeValidation();
    }

    // ================= VALIDATION EN TEMPS RÉEL =================
    private void setupRealTimeValidation() {
        nomField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                if (ValidationUtils.isValidName(newVal)) {
                    ValidationUtils.setFieldSuccess(nomField);
                } else {
                    ValidationUtils.setFieldError(nomField);
                }
            } else {
                ValidationUtils.resetFieldStyle(nomField);
            }
        });

        prenomField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                if (ValidationUtils.isValidName(newVal)) {
                    ValidationUtils.setFieldSuccess(prenomField);
                } else {
                    ValidationUtils.setFieldError(prenomField);
                }
            } else {
                ValidationUtils.resetFieldStyle(prenomField);
            }
        });

        telephoneField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                if (ValidationUtils.isValidPhone(newVal)) {
                    ValidationUtils.setFieldSuccess(telephoneField);
                } else {
                    ValidationUtils.setFieldError(telephoneField);
                }
            } else {
                ValidationUtils.resetFieldStyle(telephoneField);
            }
        });
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
            nomField.setText(profil.getNom());
            prenomField.setText(profil.getPrenom());
            telephoneField.setText(profil.getTelephone());
            bioField.setText(profil.getBio());
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
            ValidationUtils.showError(messageLabel, "Erreur de chargement de l'image");
        }
    }

    @FXML
    private void handleSaveProfil() {
        String nom = ValidationUtils.sanitize(nomField.getText());
        String prenom = ValidationUtils.sanitize(prenomField.getText());
        String telephone = ValidationUtils.sanitize(telephoneField.getText());
        String bio = ValidationUtils.sanitize(bioField.getText());

        // Validation
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

        try {
            if (!isEditMode || currentProfil == null) {
                // CRÉATION
                Profil newProfil = new Profil(
                        bio,
                        telephone,
                        nom,
                        prenom,
                        selectedImagePath,
                        currentUser
                );

                profilService.ajouter(newProfil);
                ValidationUtils.showSuccess(messageLabel, "Profil créé avec succès !");

            } else {
                // MODIFICATION
                currentProfil.setNom(nom);
                currentProfil.setPrenom(prenom);
                currentProfil.setTelephone(telephone);
                currentProfil.setBio(bio);
                currentProfil.setImage(selectedImagePath);

                profilService.modifier(currentProfil);
                ValidationUtils.showSuccess(messageLabel, "Profil modifié avec succès !");
            }

            if (profilController != null) {
                profilController.refreshProfil();
            }

            // Fermer après 1 seconde
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    javafx.application.Platform.runLater(() -> handleCancel());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            ValidationUtils.showError(messageLabel, "Erreur : " + e.getMessage());
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

                if (profilController != null) {
                    profilController.refreshProfil();
                }

                handleCancel();

            } catch (Exception e) {
                ValidationUtils.showError(messageLabel, "Erreur : " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }
}