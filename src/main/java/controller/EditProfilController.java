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
    private boolean isEditMode = false; // true = modification, false = création

    // ================= INITIALISATION =================
    @FXML
    public void initialize() {
        profilService = new ProfilService();
        if (messageLabel != null) {
            messageLabel.setText("");
        }
    }

    // ================= SETTERS =================
    public void setProfilController(ProfilController controller) {
        this.profilController = controller;
    }

    public void setUser(User user) {
        this.currentUser = user;
    }

    public void setProfil(Profil profil) {
        this.currentProfil = profil;

        // Remplir les champs
        if (profil != null) {
            nomField.setText(profil.getNom());
            prenomField.setText(profil.getPrenom());
            telephoneField.setText(profil.getTelephone());
            bioField.setText(profil.getBio());
            selectedImagePath = profil.getImage();

            // Afficher l'image
            if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                loadProfileImage(selectedImagePath);
            }
        }
    }

    public void setMode(boolean isEdit) {
        this.isEditMode = isEdit;

        if (isEdit) {
            // Mode modification
            if (titleLabel != null) titleLabel.setText("✏️ Modifier mon profil");
            if (saveButton != null) saveButton.setText("💾 Enregistrer");
            if (deleteButton != null) deleteButton.setVisible(true);
        } else {
            // Mode création
            if (titleLabel != null) titleLabel.setText("➕ Créer mon profil");
            if (saveButton != null) saveButton.setText("💾 Créer");
            if (deleteButton != null) deleteButton.setVisible(false);
        }
    }

    // ================= UPLOAD IMAGE =================
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

    // ================= CHARGER L'IMAGE =================
    private void loadProfileImage(String imagePath) {
        try {
            File file = new File(imagePath);
            if (file.exists() && profileCircle != null) {
                Image image = new Image(file.toURI().toString());
                profileCircle.setFill(new ImagePattern(image));
            }
        } catch (Exception e) {
            showError("Erreur de chargement de l'image");
        }
    }

    // ================= ENREGISTRER PROFIL =================
    @FXML
    private void handleSaveProfil() {
        // Récupérer les valeurs
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String telephone = telephoneField.getText().trim();
        String bio = bioField.getText().trim();

        // Validation
        if (nom.isEmpty() || prenom.isEmpty() || telephone.isEmpty()) {
            showError("❌ Veuillez remplir tous les champs obligatoires (*)");
            return;
        }

        if (!isValidPhone(telephone)) {
            showError("❌ Format de téléphone invalide");
            return;
        }

        try {
            if (!isEditMode || currentProfil == null) {
                // ✅ CRÉATION - ORDRE CORRECT DES PARAMÈTRES
                Profil newProfil = new Profil(
                        bio,              // 1. bio
                        telephone,        // 2. telephone
                        nom,              // 3. nom
                        prenom,           // 4. prenom
                        selectedImagePath,// 5. image
                        currentUser       // 6. user
                );

                profilService.ajouter(newProfil);
                showSuccess("✅ Profil créé avec succès !");

            } else {
                // ✅ MODIFICATION
                currentProfil.setNom(nom);
                currentProfil.setPrenom(prenom);
                currentProfil.setTelephone(telephone);
                currentProfil.setBio(bio);
                currentProfil.setImage(selectedImagePath);

                profilService.modifier(currentProfil);
                showSuccess("✅ Profil modifié avec succès !");
            }

            // Rafraîchir le profil principal
            if (profilController != null) {
                profilController.refreshProfil();
            }

            // Fermer la popup après 1 seconde
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    javafx.application.Platform.runLater(() -> handleCancel());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            showError("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================= SUPPRIMER PROFIL =================
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

                // Rafraîchir le profil principal
                if (profilController != null) {
                    profilController.refreshProfil();
                }

                handleCancel();

            } catch (Exception e) {
                showError("❌ Erreur : " + e.getMessage());
            }
        }
    }

    // ================= ANNULER =================
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }

    // ================= VALIDATION TÉLÉPHONE =================
    private boolean isValidPhone(String phone) {
        return phone.matches("^\\+?[0-9]{8,15}$");
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
    }}