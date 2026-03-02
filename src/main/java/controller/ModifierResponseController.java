package controller;

import entities.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import service.ResponseService;

import java.time.LocalDateTime;

public class ModifierResponseController {

    // ====================== FXML ======================
    @FXML private Label    threadTitleLabel;
    @FXML private TextArea contenuField;
    @FXML private Label    contenuError;
    @FXML private Button   submitBtn;

    // ====================== STATE =====================
    private Response    currentResponse;
    private User        currentUser;

    // Callback so ViewCommentairesController can refresh after save
    private Runnable    onSaveCallback;

    // ====================== SERVICES ==================
    private final ResponseService responseService = new ResponseService();

    // ====================== INIT ======================

    public void setResponse(Response response) {
        this.currentResponse = response;
        if (response != null) {
            // Pre-fill the text area with the existing content
            contenuField.setText(response.getContenu());

            // Show the parent thread title in the sub-header
            if (response.getThread() != null) {
                threadTitleLabel.setText("-> " + response.getThread().getTitre());
            }
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    // Called by ViewCommentairesController so it can refresh after save
    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    // ====================== SUBMIT ====================

    @FXML
    private void submitModification() {

        // ✅ CONTROLE DE SESSION — utilisateur connecte ?
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING,
                    "Session expiree",
                    "Vous devez etre connecte pour modifier un commentaire.");
            closeWindow();
            return;
        }

        // ✅ CONTROLE DE SESSION — compte actif ?
        if (currentUser.getEtatCompte() == EtatCompte.BLOQUE) {
            showAlert(Alert.AlertType.ERROR,
                    "Compte bloque",
                    "Votre compte est bloque.\nVous ne pouvez pas modifier ce commentaire.");
            closeWindow();
            return;
        }

        // ✅ CONTROLE DE PROPRIETE — seul l'auteur peut modifier
        if (currentResponse.getUser().getId() != currentUser.getId()) {
            showAlert(Alert.AlertType.ERROR,
                    "Non autorise",
                    "Vous ne pouvez modifier que vos propres commentaires.");
            closeWindow();
            return;
        }

        if (!validate()) return;

        try {
            // Update content and timestamp
            currentResponse.setContenu(contenuField.getText().trim());
            currentResponse.setDateCreation(LocalDateTime.now());
            responseService.update(currentResponse);

            showAlert(Alert.AlertType.INFORMATION,
                    "Succes",
                    "Commentaire modifie avec succes !");

            // Trigger refresh in parent view
            if (onSaveCallback != null) onSaveCallback.run();

            closeWindow();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR,
                    "Erreur",
                    "Impossible de modifier le commentaire:\n" + e.getMessage());
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