package controller;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import entities.Culture;
import service.CultureService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class CultureController {

    // ══════════════════════════════════════════════════════════════════════
    // FXML
    // ══════════════════════════════════════════════════════════════════════
    @FXML private TextField  txtNom;
    @FXML private TextField  txtType;
    @FXML private TextField  txtSuperficie;
    @FXML private TextField  txtLocalisation;
    @FXML private Label      lblMessage;
    @FXML private Label      lblImage;
    @FXML private ImageView  imageView;
    @FXML private Button     btnAction;

    // ══════════════════════════════════════════════════════════════════════
    // ÉTAT INTERNE
    // ══════════════════════════════════════════════════════════════════════
    private File    imageFile;
    private String  imagePath;
    private Culture cultureEnCours = null;

    private final CultureService service = new CultureService();

    // ✅ Callback après sauvegarde réussie (retour à la liste + refresh)
    private Runnable onSuccess;

    // ✅ Callback après annulation (retour à la liste sans modification)
    private Runnable onCancel;

    // ══════════════════════════════════════════════════════════════════════
    // SETTERS CALLBACKS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Injecté depuis CultureListController.
     * Déclenché après ajout/modification réussi → revient à la liste.
     */
    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    /**
     * ✅ AJOUTÉ : injecté depuis CultureListController.
     * Déclenché quand l'utilisateur clique "Annuler" → revient à la liste.
     */
    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    // ══════════════════════════════════════════════════════════════════════
    // CHOISIR IMAGE
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void choisirImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );
        File file = chooser.showOpenDialog(null);

        if (file != null) {
            imageFile = file;
            imageView.setImage(new Image(file.toURI().toString()));
            lblImage.setText(file.getName());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // MODE MODIFICATION — pré-remplir le formulaire
    // ══════════════════════════════════════════════════════════════════════
    public void remplirFormulaire(Culture c) {
        if (c == null) return;

        cultureEnCours = c;
        if (btnAction != null) btnAction.setText("✏️ Modifier");

        txtNom.setText(c.getNom());
        txtType.setText(c.getType() != null ? c.getType() : "");
        txtSuperficie.setText(String.valueOf(c.getSuperficie()));
        txtLocalisation.setText(c.getLocalisation() != null ? c.getLocalisation() : "");

        imagePath = c.getImage();
        if (imagePath != null) {
            File f = new File(imagePath);
            if (f.exists()) {
                imageView.setImage(new Image(f.toURI().toString()));
                if (lblImage != null) lblImage.setText(f.getName());
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ACTION PRINCIPALE (Ajouter ou Modifier)
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void handleAction() {
        try {
            Culture c = getCultureFromForm();

            if (cultureEnCours == null) {
                service.ajouter(c);
                afficherMessage("✅ Culture ajoutée !");
            } else {
                c.setIdCulture(cultureEnCours.getIdCulture());
                service.modifier(c);
                afficherMessage("✏️ Culture modifiée !");
            }

            // ✅ Attendre 1s que le message soit lu, puis déclencher le retour
            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> {
                if (onSuccess != null) {
                    // Chargé dans le contentPane → callback de navigation
                    onSuccess.run();
                } else {
                    // Fallback : ouvert en popup → fermer le Stage
                    fermerStage();
                }
            });
            pause.play();

        } catch (Exception e) {
            if (lblMessage != null) lblMessage.setText("❌ " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ANNULER — ✅ CORRIGÉ : retour à la liste via callback
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void fermerFenetre() {
        if (onCancel != null) {
            // Chargé dans le contentPane → retour à la liste via dashboard
            onCancel.run();
        } else {
            // Fallback : ouvert en popup → fermer le Stage
            fermerStage();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // CONSTRUIRE L'OBJET CULTURE DEPUIS LE FORMULAIRE
    // ══════════════════════════════════════════════════════════════════════
    private Culture getCultureFromForm() {
        if (txtNom.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom est obligatoire !");
        }
        if (txtSuperficie.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("La superficie est obligatoire !");
        }

        double superficie;
        try {
            superficie = Double.parseDouble(txtSuperficie.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La superficie doit être un nombre valide !");
        }

        String img = (imageFile != null) ? copierImage(imageFile) : imagePath;

        return new Culture(
                0,
                txtNom.getText().trim(),
                txtType.getText().trim(),
                superficie,
                txtLocalisation.getText().trim(),
                img
        );
    }

    // ══════════════════════════════════════════════════════════════════════
    // COPIER L'IMAGE DANS LE DOSSIER LOCAL
    // ══════════════════════════════════════════════════════════════════════
    private String copierImage(File file) {
        try {
            File dir = new File("images");
            if (!dir.exists()) dir.mkdirs();

            String nom  = System.currentTimeMillis() + "_" + file.getName();
            File   dest = new File(dir, nom);

            Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return dest.getPath();

        } catch (Exception e) {
            System.err.println("⚠️ Erreur copie image : " + e.getMessage());
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // UTILITAIRES
    // ══════════════════════════════════════════════════════════════════════
    private void afficherMessage(String msg) {
        if (lblMessage != null) {
            lblMessage.setText(msg);
            lblMessage.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        }
    }

    /** Ferme le Stage uniquement si le formulaire est ouvert en popup */
    private void fermerStage() {
        if (txtNom != null && txtNom.getScene() != null) {
            Stage stage = (Stage) txtNom.getScene().getWindow();
            if (stage != null) stage.close();
        }
    }
}