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
    @FXML private Button     btnAction; // bouton unique (Ajouter / Modifier)

    // ══════════════════════════════════════════════════════════════════════
    // ÉTAT INTERNE
    // ══════════════════════════════════════════════════════════════════════
    private File    imageFile;
    private String  imagePath;
    private Culture cultureEnCours = null;

    private final CultureService service = new CultureService();

    // ✅ AJOUTÉ : callback déclenché après ajout/modification
    // Permet à CultureListController de rafraîchir sa liste automatiquement
    private Runnable onSuccess;

    // ══════════════════════════════════════════════════════════════════════
    // SETTER CALLBACK
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ✅ AJOUTÉ : appelé depuis CultureListController pour obtenir un
     * rafraîchissement automatique de la liste après ajout/modification.
     *
     * Exemple dans CultureListController :
     *   CultureController ctrl = loader.getController();
     *   ctrl.setOnSuccess(() -> chargerCultures());
     */
    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
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
    // MODE MODIFICATION
    // ══════════════════════════════════════════════════════════════════════
    public void remplirFormulaire(Culture c) {
        if (c == null) return;

        cultureEnCours = c;
        btnAction.setText("✏️ Modifier");

        txtNom.setText(c.getNom());
        txtType.setText(c.getType());
        txtSuperficie.setText(String.valueOf(c.getSuperficie()));
        txtLocalisation.setText(c.getLocalisation());

        imagePath = c.getImage();
        if (imagePath != null) {
            File f = new File(imagePath);
            if (f.exists()) {
                imageView.setImage(new Image(f.toURI().toString()));
                lblImage.setText(f.getName());
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

            // ✅ Déclencher le callback pour rafraîchir la liste
            if (onSuccess != null) {
                onSuccess.run();
            }

            fermerFenetre();

        } catch (Exception e) {
            lblMessage.setText("❌ " + e.getMessage());
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
        lblMessage.setText(msg);
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> lblMessage.setText(""));
        pause.play();
    }

    @FXML
    private void fermerFenetre() {
        Stage stage = (Stage) txtNom.getScene().getWindow();
        stage.close();
    }
}