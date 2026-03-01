package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import entities.Culture;
import entities.Diagnostic;
import service.DiagnosticService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DiagnosticController {

    // ══════════════════════════════════════════════════════════════════════
    // FXML
    // ══════════════════════════════════════════════════════════════════════
    @FXML private FlowPane   flowHistorique;
    @FXML private ScrollPane scrollHistorique;

    @FXML private TextField txtEtat;
    @FXML private TextField txtSymptomes;
    @FXML private TextField txtInformations;
    @FXML private TextField txtPlantType;
    @FXML private TextField txtLeafColor;
    @FXML private TextField txtSpotSize;
    @FXML private TextField txtHumidity;
    @FXML private TextField txtTemperature;

    // ══════════════════════════════════════════════════════════════════════
    // ÉTAT INTERNE
    // ══════════════════════════════════════════════════════════════════════
    private final DiagnosticService service = new DiagnosticService();
    private Culture    culture;
    private Diagnostic selectedDiagnostic;
    private int        userId = 1;

    // ✅ AJOUTÉ : référence au dashboard pour le bouton retour "←"
    private DashboardAgriculteurController dashboardController;

    // ══════════════════════════════════════════════════════════════════════
    // SETTERS INJECTÉS PAR LE DASHBOARD
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ✅ AJOUTÉ : reçoit la référence du dashboard.
     * Appelé depuis DashboardAgriculteurController.handleShowProblems()
     */
    public void setDashboardController(DashboardAgriculteurController dashboard) {
        this.dashboardController = dashboard;
    }

    /**
     * Appelé depuis CultureListController quand l'utilisateur clique
     * sur "Diagnostiquer" depuis une culture.
     */
    public void setCulture(Culture culture) {
        this.culture = culture;
        chargerHistorique();
    }

    /**
     * ✅ AJOUTÉ : permet d'initialiser avec l'utilisateur courant
     * quand on arrive depuis le menu (sans culture précise).
     */
    public void setUserId(int userId) {
        this.userId = userId;
    }

    // ══════════════════════════════════════════════════════════════════════
    // BOUTON RETOUR "←"  ✅ CORRIGÉ
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void retourDashboard() {
        if (dashboardController != null) {
            dashboardController.reloadDashboardContent();
            System.out.println("✅ Retour au dashboard");
        } else {
            System.err.println("⚠️ dashboardController est null — retour impossible");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // CHARGER L'HISTORIQUE
    // ══════════════════════════════════════════════════════════════════════
    private void chargerHistorique() {
        flowHistorique.getChildren().clear();
        try {
            List<Diagnostic> diagnostics;
            if (culture != null) {
                // Historique lié à une culture précise
                diagnostics = service.getByCultureId(culture.getIdCulture());
            } else {
                // Accès depuis le menu sans culture sélectionnée :
                // aucun historique à afficher (culture obligatoire pour diagnostic)
                diagnostics = new java.util.ArrayList<>();
            }
            for (Diagnostic d : diagnostics) {
                flowHistorique.getChildren().add(creerCarte(d));
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger l'historique : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // CRÉER UNE CARTE HISTORIQUE
    // ══════════════════════════════════════════════════════════════════════
    private VBox creerCarte(Diagnostic d) {
        VBox card = new VBox(8);
        card.setStyle(
                "-fx-background-color: #f9f9f9;" +
                        "-fx-padding: 15;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #2E8B57;"
        );
        card.setPrefWidth(250);

        Label date       = new Label("Date: " + d.getDateDiagnostic().format(DateTimeFormatter.ISO_DATE));
        date.setFont(Font.font(14));
        Label etat       = new Label("État: "         + d.getEtat());
        Label symptomes  = new Label("Symptômes: "    + d.getSymptomes());
        Label infos      = new Label("Infos: "         + d.getInformationsComplementaires());
        Label disease    = new Label("Maladie: "       + d.getDiseaseDetected());
        Label confidence = new Label("Confiance IA: "  + String.format("%.2f", d.getConfidence()) + "%");

        HBox buttons = new HBox(5);

        Button btnModifier = new Button("✏ Modifier");
        btnModifier.setStyle("-fx-background-color: #2E8B57; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        btnModifier.setOnAction(e -> remplirFormulaire(d));

        Button btnSupprimer = new Button("❌ Supprimer");
        btnSupprimer.setStyle("-fx-background-color: #E53935; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        btnSupprimer.setOnAction(e -> {
            Alert confirm = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Voulez-vous vraiment supprimer ce diagnostic ?",
                    ButtonType.YES, ButtonType.NO
            );
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) supprimerDiagnostic(d);
            });
        });

        Button btnImprimer = new Button("🖨 Imprimer");
        btnImprimer.setStyle("-fx-background-color: #66BB6A; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        btnImprimer.setOnAction(e -> imprimerRapport(d));

        buttons.getChildren().addAll(btnModifier, btnSupprimer, btnImprimer);
        card.getChildren().addAll(date, etat, symptomes, infos, disease, confidence, buttons);
        return card;
    }

    // ══════════════════════════════════════════════════════════════════════
    // REMPLIR FORMULAIRE POUR MODIFICATION
    // ══════════════════════════════════════════════════════════════════════
    private void remplirFormulaire(Diagnostic d) {
        selectedDiagnostic = d;
        txtEtat.setText(d.getEtat());
        txtSymptomes.setText(d.getSymptomes());
        txtInformations.setText(d.getInformationsComplementaires());
        txtPlantType.setText(d.getDiseaseDetected());
    }

    // ══════════════════════════════════════════════════════════════════════
    // SUPPRIMER DIAGNOSTIC
    // ══════════════════════════════════════════════════════════════════════
    private void supprimerDiagnostic(Diagnostic d) {
        try {
            service.supprimer(d.getIdDiagnostic());
            showAlert("Succès", "Diagnostic supprimé !");

            if (selectedDiagnostic != null &&
                    selectedDiagnostic.getIdDiagnostic() == d.getIdDiagnostic()) {
                clearForm();
                selectedDiagnostic = null;
            }
            chargerHistorique();

        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // SAUVEGARDER / MODIFIER
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void sauvegarderDiagnostic() {
        if (!validerSaisie()) return;

        try {
            if (selectedDiagnostic == null) {
                Diagnostic d = new Diagnostic();
                remplirDiagnostic(d);
                service.ajouter(d);
                showAlert("Succès", "Diagnostic ajouté !");
            } else {
                remplirDiagnostic(selectedDiagnostic);
                service.modifier(selectedDiagnostic);
                showAlert("Succès", "Diagnostic modifié !");
                selectedDiagnostic = null;
            }
            clearForm();
            chargerHistorique();

        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // IMPRIMER
    // ══════════════════════════════════════════════════════════════════════
    @FXML
    private void imprimerDiagnostic() {
        if (selectedDiagnostic != null) {
            imprimerRapport(selectedDiagnostic);
        } else {
            showAlert("Info", "Veuillez sélectionner ou créer un diagnostic avant d'imprimer !");
        }
    }

    private void imprimerRapport(Diagnostic d) {
        showAlert("Imprimer", "Rapport du diagnostic du " + d.getDateDiagnostic());
    }

    // ══════════════════════════════════════════════════════════════════════
    // REMPLIR OBJET DIAGNOSTIC
    // ══════════════════════════════════════════════════════════════════════
    private void remplirDiagnostic(Diagnostic d) {
        d.setIdCulture(culture != null ? culture.getIdCulture() : 0);
        d.setIdUser(userId);
        d.setEtat(txtEtat.getText().trim());
        d.setSymptomes(txtSymptomes.getText().trim());
        d.setInformationsComplementaires(txtInformations.getText().trim());
        d.setDiseaseDetected(txtPlantType.getText().trim());
        d.setConfidence(Math.random() * 100); // Simuler IA
        d.setRapportIa("Rapport IA simulé");
        d.setDateDiagnostic(java.time.LocalDate.now());
    }

    // ══════════════════════════════════════════════════════════════════════
    // VALIDATION
    // ══════════════════════════════════════════════════════════════════════
    private boolean validerSaisie() {
        if (txtEtat.getText().trim().isEmpty()) {
            showAlert("Validation", "Veuillez renseigner l'état de la culture !");
            return false;
        }
        if (txtSymptomes.getText().trim().isEmpty()) {
            showAlert("Validation", "Veuillez renseigner les symptômes !");
            return false;
        }
        if (txtInformations.getText().trim().isEmpty()) {
            showAlert("Validation", "Veuillez renseigner les informations complémentaires !");
            return false;
        }
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════
    // CLEAR FORMULAIRE
    // ══════════════════════════════════════════════════════════════════════
    private void clearForm() {
        txtEtat.clear();
        txtSymptomes.clear();
        txtInformations.clear();
        txtPlantType.clear();
        if (txtLeafColor   != null) txtLeafColor.clear();
        if (txtSpotSize    != null) txtSpotSize.clear();
        if (txtHumidity    != null) txtHumidity.clear();
        if (txtTemperature != null) txtTemperature.clear();
    }

    // ══════════════════════════════════════════════════════════════════════
    // UTILITAIRE ALERT
    // ══════════════════════════════════════════════════════════════════════
    private void showAlert(String titre, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}