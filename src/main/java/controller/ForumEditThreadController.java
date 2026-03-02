package controller;

import entities.*;
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
import java.util.List;

public class ForumEditThreadController {

    @FXML private TextField        TitreAjoutThread;
    @FXML private TextArea         contenuAjoutThread;
    @FXML private ImageView        imagePreview;
    @FXML private Label            noImageLabel;
    @FXML private StackPane        threadImageContainer;
    @FXML private ComboBox<String> categorieComboBox;
    @FXML private TextField        tagsField;

    private ForumThread thread;
    private File        selectedImageFile;
    private User        currentUser;

    private final ThreadService threadService = new ThreadService();
    private final MediaService  mediaService  = new MediaService();

    // ─────────────────────────── INIT ───────────────────────────────

    @FXML
    public void initialize() {
        updateImagePreview(null);
    }

    /**
     * Called by dashboard before showing the window.
     * Populates all fields AND loads the existing image from DB.
     */
    public void setThread(ForumThread thread) {
        this.thread = thread;
        if (thread == null) return;

        TitreAjoutThread.setText(thread.getTitre());
        contenuAjoutThread.setText(thread.getContenu());
        categorieComboBox.setValue(thread.getCategory());
        tagsField.setText(thread.getTags());

        // ✅ Load existing image from DB so the user can see the current photo
        loadExistingImage();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    // ─────────────────────────── LOAD EXISTING IMAGE ────────────────

    private void loadExistingImage() {
        try {
            List<Media> mediaList = mediaService.getMediaByThread(thread.getThreadId());
            if (mediaList != null && !mediaList.isEmpty()) {
                for (Media m : mediaList) {
                    if (m.getType() == MediaType.IMAGE && m.getUrl() != null) {
                        File imgFile = new File(m.getUrl());
                        if (imgFile.exists()) {
                            // Show current image in preview (but selectedImageFile stays null
                            // so we only replace it in DB if the user picks a NEW file)
                            imagePreview.setImage(new Image(imgFile.toURI().toString()));
                            imagePreview.setVisible(true);
                            noImageLabel.setVisible(false);
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Impossible de charger l'image existante: " + e.getMessage());
        }
        // No image found — show placeholder
        updateImagePreview(null);
    }

    // ─────────────────────────── SAVE ───────────────────────────────

    @FXML
    private void modifierThread() {
        if (thread == null) return;

        String title    = TitreAjoutThread.getText().trim();
        String content  = contenuAjoutThread.getText().trim();
        String category = categorieComboBox.getValue();
        String tags     = tagsField.getText().trim();

        if (title.isEmpty() || content.isEmpty()) {
            showAlert("Erreur", "Titre et contenu sont obligatoires.");
            return;
        }

        // Update thread fields
        thread.setTitre(title);
        thread.setContenu(content);
        thread.setCategory(category);
        thread.setTags(tags);
        thread.setStatus(ThreadStatus.OPEN);
        thread.setDateCreation(LocalDateTime.now());

        threadService.update(thread);

        // ✅ Only update media if the user actually picked a NEW image
        if (selectedImageFile != null) {
            // 1) Delete all old media rows for this thread
            List<Media> oldMedia = mediaService.getMediaByThread(thread.getThreadId());
            for (Media m : oldMedia) {
                mediaService.delete(m.getMediaId());
            }

            // 2) Insert the new media row
            Media newMedia = new Media(
                    selectedImageFile.getAbsolutePath(),
                    MediaType.IMAGE,
                    thread,
                    null
            );
            mediaService.add(newMedia);
            System.out.println("✅ Image mise à jour: " + selectedImageFile.getAbsolutePath());
        }

        showAlert("Succès", "✅ Thread modifié avec succès !");
        closeWindow();
    }

    // ─────────────────────────── IMAGE ACTIONS ──────────────────────

    @FXML
    private void importImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        Stage stage = (Stage) TitreAjoutThread.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedImageFile = file;
            updateImagePreview(file); // show new image in preview immediately
        }
    }

    @FXML
    private void AnnulerLimageImporterAjTh() {
        selectedImageFile = null;
        // Reload the original image from DB (don't just blank it)
        loadExistingImage();
    }

    @FXML
    private void openImageModal() {
        // Show whichever image is currently in preview
        Image current = imagePreview.getImage();
        if (current != null && !current.isError()) {
            ImageView fullImage = new ImageView(current);
            fullImage.setPreserveRatio(true);
            fullImage.setFitWidth(600);
            fullImage.setFitHeight(600);
            Stage modal = new Stage();
            modal.setTitle("Aperçu image");
            modal.setScene(new Scene(new StackPane(fullImage), 600, 600));
            modal.show();
        }
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) TitreAjoutThread.getScene().getWindow();
        stage.close();
    }

    // ─────────────────────────── HELPERS ────────────────────────────

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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}