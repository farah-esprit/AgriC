package org.example.controllers.Culture;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.controllers.Diagnostic.DiagnosticController;
import org.example.entities.Culture;

import java.io.File;

public class CultureDetailsController {

    /* ── FXML — mêmes fx:id que votre code original ── */
    @FXML private ImageView imgCulture;

    // Header
    @FXML private Label lblNom;           // titre dans le header
    @FXML private Label lblTypeBadge;     // badge type (header)
    @FXML private Label lblStatutBadge;   // badge statut (header)

    // Carte info
    @FXML private Label lblNomDetail;     // ligne nom dans la carte
    @FXML private Label lblType;
    @FXML private Label lblSuperficie;
    @FXML private Label lblLocalisation;

    // Notes
    @FXML private Label lblNotes;

    // Bouton
    @FXML private Button btnFaireDiagnostic;

    // Objet métier — votre classe Culture existante
    private Culture culture;

    /* ════════════════════════════════════════════════
       setCulture — appelé depuis CultureListController
       exactement comme dans votre code original
    ════════════════════════════════════════════════ */
    public void setCulture(Culture c) {
        this.culture = c;

        // ── Header ────────────────────────────────────
        fade(lblNom, c.getNom());
        lblTypeBadge.setText("🌾 " + (c.getType() != null ? c.getType() : "—"));

        // Badge statut — adaptez le getter si votre entité
        // s'appelle différemment (ex: getEtat(), getStatut()…)
        String statut = "Active"; // valeur par défaut
        // si votre entité a getStatut() → décommenter :
        // String statut = c.getStatut() != null ? c.getStatut() : "Active";
        lblStatutBadge.setText("● " + statut);
        styleStatutBadge(statut);

        // ── Carte informations ─────────────────────────
        fade(lblNomDetail,   c.getNom());
        fade(lblType,        c.getType() != null ? c.getType() : "Non défini");
        fade(lblSuperficie,  c.getSuperficie() + " ha");
        fade(lblLocalisation,c.getLocalisation() != null ? c.getLocalisation() : "Non défini");

        // ── Notes ─────────────────────────────────────
        // si votre entité a getNotes() → décommenter :
        // fade(lblNotes, c.getNotes() != null && !c.getNotes().isBlank()
        //                 ? c.getNotes() : "Aucune note disponible.");
        fade(lblNotes, "Aucune note disponible.");

        // ── Image ─────────────────────────────────────
        if (c.getImage() != null) {
            File file = new File(c.getImage());
            if (file.exists()) {
                imgCulture.setImage(new Image(file.toURI().toString()));
            }
        }

        // ── Action bouton diagnostic ───────────────────
        if (btnFaireDiagnostic != null) {
            btnFaireDiagnostic.setOnAction(e -> ouvrirDiagnostic());
        }
    }

    /* ── Fermer ────────────────────────────────────── */
    @FXML
    private void fermer() {
        Stage stage = (Stage) lblNom.getScene().getWindow();
        stage.close();
    }

    /* ── Ouvrir diagnostic (votre logique originale) ── */
    private void ouvrirDiagnostic() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/DiagnosticForm.fxml"));
            Scene scene = new Scene(loader.load());

            DiagnosticController controller = loader.getController();
            controller.setCulture(culture);

            Stage stage = new Stage();
            stage.setTitle("🌱 Diagnostic de " + culture.getNom());
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /* ── Helpers ───────────────────────────────────── */

    /** Applique un fade-in + met à jour le texte du label */
    private void fade(Label label, String text) {
        if (label == null) return;
        label.setOpacity(0);
        label.setText(text);
        FadeTransition ft = new FadeTransition(Duration.millis(350), label);
        ft.setToValue(1.0);
        ft.play();
    }

    /** Change la couleur du badge statut selon la valeur */
    private void styleStatutBadge(String statut) {
        String base =
                "-fx-border-width:1; -fx-border-radius:20; -fx-background-radius:20;" +
                        "-fx-padding:3 13 3 13; -fx-font-size:12px; -fx-font-weight:bold;";

        switch (statut) {
            case "En attente" -> lblStatutBadge.setStyle(base +
                    "-fx-background-color:rgba(255,180,0,0.22);" +
                    "-fx-border-color:rgba(255,180,0,0.45);" +
                    "-fx-text-fill:#FFE080;");
            case "Terminée" -> lblStatutBadge.setStyle(base +
                    "-fx-background-color:rgba(150,150,150,0.18);" +
                    "-fx-border-color:rgba(150,150,150,0.35);" +
                    "-fx-text-fill:rgba(255,255,255,0.55);");
            default -> lblStatutBadge.setStyle(base +          // Active
                    "-fx-background-color:rgba(100,220,80,0.22);" +
                    "-fx-border-color:rgba(100,220,80,0.45);" +
                    "-fx-text-fill:#B8F0A0;");
        }
    }
}