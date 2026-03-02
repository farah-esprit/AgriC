package controller;

import entities.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import service.MediaService;
import service.ResponseService;

import java.io.File;
import java.time.LocalDateTime;

public class AddCommentaireController {

    // ====================== FXML ======================
    @FXML private Label     threadTitleLabel;
    @FXML private TextArea  contenuField;
    @FXML private Label     contenuError;

    // Media
    @FXML private Button      importImageBtn;
    @FXML private Button      importVideoBtn;
    @FXML private Button      cancelMediaBtn;
    @FXML private StackPane   previewContainer;
    @FXML private ImageView   imagePreview;
    @FXML private VBox        videoPlaceholder;
    @FXML private Label       videoNameLabel;

    @FXML private Button submitBtn;

    // ====================== STATE =====================
    private ForumThread currentThread;
    private User        currentUser;
    private File        selectedMediaFile;
    private MediaType   selectedMediaType;

    // ====================== SERVICES ==================
    private final ResponseService responseService = new ResponseService();
    private final MediaService    mediaService    = new MediaService();

    // ====================== INIT ======================

    public void setThread(ForumThread thread) {
        this.currentThread = thread;
        if (thread != null) {
            threadTitleLabel.setText("-> " + thread.getTitre());
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    // ====================== MEDIA IMPORT ==============

    @FXML
    private void importImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File file = fc.showOpenDialog(contenuField.getScene().getWindow());
        if (file != null) {
            selectedMediaFile = file;
            selectedMediaType = MediaType.IMAGE;
            showImagePreview(file);
        }
    }

    @FXML
    private void importVideo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une video");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.avi", "*.mov", "*.mkv")
        );
        File file = fc.showOpenDialog(contenuField.getScene().getWindow());
        if (file != null) {
            selectedMediaFile = file;
            selectedMediaType = MediaType.VIDEO;
            showVideoPreview(file);
        }
    }

    @FXML
    private void cancelMedia() {
        selectedMediaFile = null;
        selectedMediaType = null;
        hidePreview();
    }

    // ====================== PREVIEW ===================

    private void showImagePreview(File file) {
        try {
            Image img = new Image(file.toURI().toString(), 500, 180, true, true);
            if (!img.isError()) {
                imagePreview.setImage(img);
                imagePreview.setVisible(true);
                imagePreview.setManaged(true);
                videoPlaceholder.setVisible(false);
                videoPlaceholder.setManaged(false);
                showPreviewContainer();
            }
        } catch (Exception e) {
            System.err.println("Image preview error: " + e.getMessage());
        }
    }

    private void showVideoPreview(File file) {
        videoNameLabel.setText(file.getName());
        videoPlaceholder.setVisible(true);
        videoPlaceholder.setManaged(true);
        imagePreview.setVisible(false);
        imagePreview.setManaged(false);
        showPreviewContainer();
    }

    private void showPreviewContainer() {
        previewContainer.setVisible(true);
        previewContainer.setManaged(true);
        cancelMediaBtn.setVisible(true);
        cancelMediaBtn.setManaged(true);
    }

    private void hidePreview() {
        previewContainer.setVisible(false);
        previewContainer.setManaged(false);
        cancelMediaBtn.setVisible(false);
        cancelMediaBtn.setManaged(false);
        imagePreview.setImage(null);
        imagePreview.setVisible(false);
        imagePreview.setManaged(false);
        videoPlaceholder.setVisible(false);
        videoPlaceholder.setManaged(false);
        videoNameLabel.setText("");
    }

    // ====================== SUBMIT ====================

    @FXML
    private void submitComment() {

        // ✅ CONTROLE DE SESSION — utilisateur connecte ?
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING,
                    "Session expiree",
                    "Vous devez etre connecte pour commenter.\nVeuillez vous reconnecter.");
            closeWindow();
            return;
        }

        // ✅ CONTROLE DE SESSION — compte actif ?
        if (currentUser.getEtatCompte() == EtatCompte.BLOQUE) {
            showAlert(Alert.AlertType.ERROR,
                    "Compte bloque",
                    "Votre compte est bloque.\nVous ne pouvez pas publier de commentaire.");
            closeWindow();
            return;
        }

        if (!validate()) return;

        try {
            // 1) Save response
            Response response = new Response();
            response.setContenu(contenuField.getText().trim());
            response.setDateCreation(LocalDateTime.now());
            response.setUser(currentUser);
            response.setThread(currentThread);
            responseService.add(response);

            // 2) Save media if selected
            if (selectedMediaFile != null && selectedMediaType != null) {
                Media media = new Media(
                        selectedMediaFile.getAbsolutePath(),
                        selectedMediaType,
                        null,
                        response
                );
                mediaService.add(media);
            }

            showAlert(Alert.AlertType.INFORMATION, "Succes", "Commentaire ajoute !");
            closeWindow();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter le commentaire:\n" + e.getMessage());
        }
    }

    // ====================== VALIDATION ================

    private boolean validate() {
        String text = contenuField.getText();
        if (text == null || text.trim().isEmpty()) {
            contenuError.setText("Le commentaire ne peut pas etre vide.");
            return false;
        }
        contenuError.setText("");
        return true;
    }

    // ====================== CLOSE =====================

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) contenuField.getScene().getWindow();
        stage.close();
    }

    // ====================== HELPERS ===================

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}