package org.example.controllers.Diagnostic;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import org.example.entities.Culture;
import org.example.entities.Diagnostic;
import org.example.services.DiagnosticService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DiagnosticController {

    @FXML private FlowPane flowHistorique;
    @FXML private ScrollPane scrollHistorique;

    @FXML private TextField txtEtat, txtSymptomes, txtInformations;
    @FXML private TextField txtPlantType, txtLeafColor, txtSpotSize, txtHumidity, txtTemperature;

    private final DiagnosticService service = new DiagnosticService();
    private Culture culture;
    private Diagnostic selectedDiagnostic;
    private int userId = 1;

    // ================= SET CULTURE =================
    public void setCulture(Culture culture) {
        this.culture = culture;
        chargerHistorique();
    }

    // ================= CHARGER L'HISTORIQUE =================
    private void chargerHistorique() {
        flowHistorique.getChildren().clear();
        try {
            List<Diagnostic> diagnostics = service.getByCultureId(culture.getIdCulture());
            for (Diagnostic d : diagnostics) {
                flowHistorique.getChildren().add(creerCarte(d));
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger l'historique : " + e.getMessage());
        }
    }

    // ================= CREER UNE CARTE =================
    private VBox creerCarte(Diagnostic d) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color:#f9f9f9; -fx-padding:15; -fx-border-radius:10; -fx-background-radius:10; -fx-border-color:#2E8B57;");
        card.setPrefWidth(250);

        Label date = new Label("Date: " + d.getDateDiagnostic().format(DateTimeFormatter.ISO_DATE));
        date.setFont(Font.font(14));
        Label etat = new Label("État: " + d.getEtat());
        Label symptomes = new Label("Symptômes: " + d.getSymptomes());
        Label infos = new Label("Infos: " + d.getInformationsComplementaires());
        Label disease = new Label("Maladie: " + d.getDiseaseDetected());
        Label confidence = new Label("Confiance IA: " + String.format("%.2f", d.getConfidence()) + "%");

        HBox buttons = new HBox(5);

        Button btnModifier = new Button("✏ Modifier");
        btnModifier.setStyle("-fx-background-color:#2E8B57; -fx-text-fill:white; -fx-background-radius:5;");
        btnModifier.setOnAction(e -> remplirFormulaire(d));

        Button btnSupprimer = new Button("❌ Supprimer");
        btnSupprimer.setStyle("-fx-background-color:#E53935; -fx-text-fill:white; -fx-background-radius:5;");
        btnSupprimer.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer ce diagnostic ?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) supprimerDiagnostic(d);
            });
        });

        Button btnImprimer = new Button("🖨 Imprimer");
        btnImprimer.setStyle("-fx-background-color:#66BB6A; -fx-text-fill:white; -fx-background-radius:5;");
        btnImprimer.setOnAction(e -> imprimerRapport(d));

        buttons.getChildren().addAll(btnModifier, btnSupprimer, btnImprimer);

        card.getChildren().addAll(date, etat, symptomes, infos, disease, confidence, buttons);
        return card;
    }

    // ================= REMPLIR FORMULAIRE =================
    private void remplirFormulaire(Diagnostic d) {
        selectedDiagnostic = d;
        txtEtat.setText(d.getEtat());
        txtSymptomes.setText(d.getSymptomes());
        txtInformations.setText(d.getInformationsComplementaires());
        txtPlantType.setText(d.getDiseaseDetected());
    }

    // ================= SUPPRIMER DIAGNOSTIC =================
    private void supprimerDiagnostic(Diagnostic d) {
        try {
            service.supprimer(d);
            showAlert("Succès", "Diagnostic supprimé !");
            if (selectedDiagnostic != null && selectedDiagnostic.getIdDiagnostic() == d.getIdDiagnostic()) {
                clearForm();
                selectedDiagnostic = null;
            }
            chargerHistorique();
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    // ================= SAUVEGARDER / MODIFIER =================
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

    // ================= IMPRIMER DIAGNOSTIC =================
    @FXML
    private void imprimerDiagnostic() {
        if (selectedDiagnostic != null) {
            imprimerRapport(selectedDiagnostic);
        } else {
            showAlert("Info", "Veuillez sélectionner ou créer un diagnostic avant d'imprimer !");
        }
    }

    private void imprimerRapport(Diagnostic d) {
        showAlert("Imprimer", "Rapport du diagnostic de " + d.getDateDiagnostic());
    }

    // ================= REMPLIR OBJET DIAGNOSTIC =================
    private void remplirDiagnostic(Diagnostic d) {
        d.setIdCulture(culture.getIdCulture());
        d.setIdUser(userId);
        d.setEtat(txtEtat.getText());
        d.setSymptomes(txtSymptomes.getText());
        d.setInformationsComplementaires(txtInformations.getText());
        d.setDiseaseDetected(txtPlantType.getText());
        d.setConfidence(Math.random() * 100); // Simuler IA
        d.setRapportIa("Rapport IA simulé");
        d.setDateDiagnostic(java.time.LocalDate.now());
    }

    // ================= VALIDATION DE SAISIE =================
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

    // ================= CLEAR FORMULAIRE =================
    private void clearForm() {
        txtEtat.clear(); txtSymptomes.clear(); txtInformations.clear();
        txtPlantType.clear(); txtLeafColor.clear(); txtSpotSize.clear(); txtHumidity.clear(); txtTemperature.clear();
    }

    // ================= SHOW ALERT =================
    private void showAlert(String titre, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
