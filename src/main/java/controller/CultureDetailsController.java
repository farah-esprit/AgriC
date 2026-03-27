package controller;

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
import entities.Culture;

import java.io.File;

public class CultureDetailsController {

    // ══════════════════════════════════════════════════════════════════════
    // FXML
    // ══════════════════════════════════════════════════════════════════════
    @FXML private ImageView imgCulture;

    // Header
    @FXML private Label lblNom;
    @FXML private Label lblTypeBadge;
    @FXML private Label lblStatutBadge;

    // Carte info
    @FXML private Label lblNomDetail;
    @FXML private Label lblType;
    @FXML private Label lblSuperficie;
    @FXML private Label lblLocalisation;

    // Notes
    @FXML private Label lblNotes;

    // Bouton
    @FXML private Button btnFaireDiagnostic;

    // ══════════════════════════════════════════════════════════════════════
    // ÉTAT INTERNE
    // ══════════════════════════════════════════════════════════════════════
    private Culture culture;

    // ✅ AJOUTÉ : référence au dashboard pour ouvrir le diagnostic
    //             dans le contentPane au lieu d'une popup séparée
    private DashboardAgriculteurController dashboardController;

    // ══════════════════════════════════════════════════════════════════════
    // SETTERS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ✅ AJOUTÉ : injecté depuis CultureListController.afficherDetails()
     * Permet au bouton "Faire un diagnostic" de naviguer dans le dashboard.
     */
    public void setDashboardController(DashboardAgriculteurController dashboard) {
        this.dashboardController = dashboard;
    }

    /**
     * Appelé depuis CultureListController.afficherDetails()
     * Doit être appelé APRÈS setDashboardController().
     */
    public void setCulture(Culture c) {
        this.culture = c;

        // ── Header ────────────────────────────────────────────────────
        fade(lblNom, c.getNom());
        lblTypeBadge.setText("🌾 " + (c.getType() != null ? c.getType() : "—"));

        String statut = "Active";
        // Si votre entité a getStatut() → décommenter :
        // String statut = c.getStatut() != null ? c.getStatut() : "Active";
        lblStatutBadge.setText("● " + statut);
        styleStatutBadge(statut);

        // ── Carte informations ─────────────────────────────────────────
        fade(lblNomDetail,    c.getNom());
        fade(lblType,         c.getType()         != null ? c.getType()         : "Non défini");
        fade(lblSuperficie,   c.getSuperficie()    + " ha");
        fade(lblLocalisation, c.getLocalisation()  != null ? c.getLocalisation() : "Non défini");

        // ── Notes ──────────────────────────────────────────────────────
        // Si votre entité a getNotes() → décommenter :
        // fade(lblNotes, c.getNotes() != null && !c.getNotes().isBlank()
        //                 ? c.getNotes() : "Aucune note disponible.");
        fade(lblNotes, "Aucune note disponible.");

        // ── Image ──────────────────────────────────────────────────────
        if (c.getImage() != null) {
            File file = new File(c.getImage());
            if (file.exists()) {
                imgCulture.setImage(new Image(file.toURI().toString()));
            }
        }

        // ── Bouton diagnostic ──────────────────────────────────────────
        if (btnFaireDiagnostic != null) {
            btnFaireDiagnostic.setOnAction(e -> ouvrirDiagnostic());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // FERMER LA POPUP
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void fermer() {
        Stage stage = (Stage) lblNom.getScene().getWindow();
        stage.close();
    }

    // ══════════════════════════════════════════════════════════════════════
    // OUVRIR DIAGNOSTIC ✅ CORRIGÉ
    // ══════════════════════════════════════════════════════════════════════
    private void ouvrirDiagnostic() {
        if (dashboardController != null) {
            // ✅ Fermer la popup de détails d'abord
            Stage detailStage = (Stage) lblNom.getScene().getWindow();
            detailStage.close();

            // ✅ Charger le diagnostic dans le contentPane du dashboard
            dashboardController.ouvrirDiagnosticPourCulture(culture);

        } else {
            // Fallback : ouvrir en popup si pas de dashboard (accès standalone)
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
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /** Fade-in + mise à jour du texte du label */
    private void fade(Label label, String text) {
        if (label == null) return;
        label.setOpacity(0);
        label.setText(text);
        FadeTransition ft = new FadeTransition(Duration.millis(350), label);
        ft.setToValue(1.0);
        ft.play();
    }

    /** Colore le badge statut selon sa valeur */
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
            default -> lblStatutBadge.setStyle(base +   // Active
                    "-fx-background-color:rgba(100,220,80,0.22);" +
                    "-fx-border-color:rgba(100,220,80,0.45);" +
                    "-fx-text-fill:#B8F0A0;");
        }
    }
}