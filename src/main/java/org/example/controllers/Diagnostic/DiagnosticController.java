package org.example.controllers.Diagnostic;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.entities.diagnostic;
import org.example.services.DiagnosticService;

import java.time.LocalDate;

public class DiagnosticController {

    @FXML private TextField txtEtat;
    @FXML private TextArea txtDetails;
    @FXML private TextArea txtRecommandations;

    private DiagnosticService service = new DiagnosticService();
    private int cultureId; // ID de la culture en cours

    public void setCultureId(int id) {
        this.cultureId = id;
    }

    @FXML
    private void handleSave() {
        diagnostic diag = new diagnostic();
        diag.setCultureId(cultureId);
        diag.setDateDiagnostic(LocalDate.now());
        diag.setEtat(txtEtat.getText());
        diag.setDetails(txtDetails.getText());
        diag.setRecommandations(txtRecommandations.getText());

        service.ajouter(diag);

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Diagnostic enregistré !");
        alert.showAndWait();

        fermer();
    }

    @FXML
    private void fermer() {
        Stage stage = (Stage) txtEtat.getScene().getWindow();
        stage.close();
    }
}
