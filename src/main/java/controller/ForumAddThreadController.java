package controller;

import entities.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import service.MediaService;
import service.ThreadService;

import java.io.File;
import java.time.LocalDateTime;

public class ForumAddThreadController {

    // --- FXML UI elements ---
    @FXML private TextField TitreAjoutThread;
    @FXML private TextArea contenuAjoutThread;
    @FXML private ImageView imagePreview;
    @FXML private Label noImageLabel;
    @FXML private StackPane threadImageContainer;
    @FXML private ComboBox<String> categorieComboBox;
    @FXML private TextField tagsField;

    private ObservableList<Media> mediaObjects = FXCollections.observableArrayList();
    private ThreadService threadService = new ThreadService();
    private MediaService mediaService = new MediaService();
    private User currentUser;
    private File selectedImageFile;

    @FXML
    public void initialize() {
        updateImagePreview(null);

        // Categories already defined in FXML,
        // but keeping this doesn't break anything.
        if (categorieComboBox.getItems().isEmpty()) {
            ObservableList<String> categories = FXCollections.observableArrayList(
                    "🌾 Cultures & Récoltes",
                    "🐄 Élevage & Animaux",
                    "🚜 Matériel & Équipement",
                    "🌱 Semences & Plantation",
                    "💧 Irrigation & Eau",
                    "🧪 Engrais & Traitements",
                    "📊 Agroéconomie & Marché",
                    "🌍 Agroécologie & Bio",
                    "🔧 Réparations & Astuces",
                    "❓ Questions Générales"
            );
            categorieComboBox.setItems(categories);
        }
    }

    @FXML
    private void importImage() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        Stage stage = (Stage) TitreAjoutThread.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedImageFile = file;
            mediaObjects.add(new Media(file.getAbsolutePath(), MediaType.IMAGE, null, null));
            updateImagePreview(file);
        }
    }

    @FXML
    private void AnnulerLimageImporterAjTh() {
        selectedImageFile = null;
        mediaObjects.clear();
        updateImagePreview(null);
    }

    @FXML
    private void AjouterThread() {

        String title = TitreAjoutThread.getText().trim();
        String content = contenuAjoutThread.getText().trim();
        String category = categorieComboBox.getValue();
        String tags = tagsField.getText().trim();

        // Validation
        if (title.isEmpty() || content.isEmpty()) {
            showAlert("Erreur", "Titre et contenu sont obligatoires.");
            return;
        }

        if (category == null || category.isEmpty()) {
            showAlert("Erreur", "Veuillez sélectionner une catégorie.");
            return;
        }

        if (currentUser == null) {
            showAlert("Erreur", "Utilisateur non connecté.");
            return;
        }

        if (currentUser.getEtatCompte() == EtatCompte.BLOQUE) {
            showAlert("Erreur", "Votre compte est bloqué. Impossible de créer un thread.");
            return;
        }

        // Create thread
        ForumThread thread = new ForumThread();
        thread.setTitre(title);
        thread.setContenu(content);
        thread.setDateCreation(LocalDateTime.now());
        thread.setStatus(ThreadStatus.OPEN);
        thread.setUser(currentUser);
        thread.setCategory(category);
        thread.setTags(tags);

        // Save thread
        threadService.add(thread);

        // Save media
        for (Media media : mediaObjects) {
            media.setThread(thread);
            mediaService.add(media);
        }

        showAlert("Succès", "✅ Thread créé avec succès !");
        clearForm();
    }

    @FXML
    private void AnnulerAjoutThread() {
        clearForm();
    }

    @FXML
    private void openImageModal() {

        if (selectedImageFile != null) {
            try {
                ImageView fullImage = new ImageView(new Image(selectedImageFile.toURI().toString()));
                fullImage.setPreserveRatio(true);
                fullImage.setFitWidth(500);
                fullImage.setFitHeight(500);

                StackPane root = new StackPane(fullImage);
                Stage modal = new Stage();
                modal.setTitle("Aperçu de l'image");
                modal.setScene(new Scene(root, 500, 500));
                modal.show();

            } catch (Exception e) {
                showAlert("Erreur", "Impossible d'ouvrir l'image.");
            }
        }
    }

    // --------------------
    // Utility Methods
    // --------------------

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearForm() {
        TitreAjoutThread.clear();
        contenuAjoutThread.clear();
        categorieComboBox.setValue(null);
        tagsField.clear();
        mediaObjects.clear();
        selectedImageFile = null;
        updateImagePreview(null);
    }

    private void updateImagePreview(File file) {

        if (file != null) {
            try {
                imagePreview.setImage(new Image(file.toURI().toString()));
                imagePreview.setVisible(true);
                noImageLabel.setVisible(false);
            } catch (Exception e) {
                imagePreview.setVisible(false);
                noImageLabel.setVisible(true);
            }
        } else {
            imagePreview.setImage(null);
            imagePreview.setVisible(false);
            noImageLabel.setVisible(true);
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
}
