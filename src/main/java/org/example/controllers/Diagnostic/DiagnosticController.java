package org.example.controllers.Diagnostic;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.example.entities.Diagnostic;
import org.example.services.DiagnosticService;

import java.io.File;
import java.time.LocalDate;

public class DiagnosticController {

    @FXML private Label lblCulture;
    @FXML private TextField txtEtat, txtSymptomes, txtInformations;
    @FXML private Button btnSauvegarder, btnChoisirPhoto;
    @FXML private TableView<Diagnostic> tblHistorique;
    @FXML private TableColumn<Diagnostic, LocalDate> colDate;
    @FXML private TableColumn<Diagnostic, String> colEtat;
    @FXML private TableColumn<Diagnostic, String> colSymptomes;
    @FXML private TableColumn<Diagnostic, String> colInformations;

    private int cultureId;
    private String photoPath;
    private final DiagnosticService service = new DiagnosticService();

    public void setCultureId(int id) {
        this.cultureId = id;
        lblCulture.setText("Diagnostic pour Culture ID: " + id);
        chargerHistorique();
    }

    @FXML
    private void choisirPhoto() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg"));
        File file = chooser.showOpenDialog(null);
        if(file != null) photoPath = file.getAbsolutePath();
    }

    @FXML
    private void sauvegarderDiagnostic() {
        if(txtEtat.getText().isEmpty() || txtSymptomes.getText().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "État et Symptômes obligatoires").showAndWait();
            return;
        }

        Diagnostic diag = new Diagnostic();
        diag.setCultureId(cultureId);
        diag.setDateDiagnostic(LocalDate.now());
        diag.setEtat(txtEtat.getText());
        diag.setSymptomes(txtSymptomes.getText());
        diag.setInformationsComplementaires(txtInformations.getText());
        diag.setPhoto(photoPath);

        // Optionnel : générer recommandations automatiquement
        diag.setRecommandations("Recommandation automatique basée sur l'état : " + diag.getEtat());

        service.ajouter(diag);
        txtEtat.clear();
        txtSymptomes.clear();
        txtInformations.clear();
        photoPath = null;
        chargerHistorique();

        new Alert(Alert.AlertType.INFORMATION, "Diagnostic sauvegardé !").showAndWait();
    }

    private void chargerHistorique() {
        ObservableList<Diagnostic> list = FXCollections.observableArrayList(service.getByCultureId(cultureId));
        tblHistorique.setItems(list);

        colDate.setCellValueFactory(new PropertyValueFactory<>("dateDiagnostic"));
        colEtat.setCellValueFactory(new PropertyValueFactory<>("etat"));
        colSymptomes.setCellValueFactory(new PropertyValueFactory<>("symptomes"));
        colInformations.setCellValueFactory(new PropertyValueFactory<>("informationsComplementaires"));
    }
}
