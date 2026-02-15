package org.example.controllers.Culture;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.controllers.Diagnostic.DiagnosticController;
import org.example.entities.Culture;

import java.io.File;

public class CultureDetailsController {

    @FXML
    private ImageView imgCulture;
    @FXML
    private Label lblNom;
    @FXML
    private Label lblType;
    @FXML
    private Label lblSuperficie;
    @FXML
    private Label lblLocalisation;
    @FXML
    private Button btnFaireDiagnostic;

    private Culture culture;

    // ================= SET CULTURE =================
    public void setCulture(Culture c) {
        this.culture = c;

        lblNom.setText("Nom: " + c.getNom());
        lblType.setText("Type: " + (c.getType() != null ? c.getType() : "Non défini"));
        lblSuperficie.setText("Superficie: " + c.getSuperficie() + " ha");
        lblLocalisation.setText("Localisation: " + (c.getLocalisation() != null ? c.getLocalisation() : "Non défini"));

        if (c.getImage() != null) {
            File file = new File(c.getImage());
            if (file.exists()) {
                imgCulture.setImage(new Image(file.toURI().toString()));
            }
        }

        // Action bouton
        if (btnFaireDiagnostic != null) {
            btnFaireDiagnostic.setOnAction(e -> ouvrirDiagnostic());
        }
    }

    // ================= FERMER =================
    @FXML
    private void fermer() {
        Stage stage = (Stage) lblNom.getScene().getWindow();
        stage.close();
    }

    // ================= OUVRIR DIAGNOSTIC =================
    private void ouvrirDiagnostic() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DiagnosticForm.fxml"));
            Scene scene = new Scene(loader.load());

            DiagnosticController controller = loader.getController();

            // ✅ PASSER L'OBJET COMPLET
            controller.setCulture(culture);

            Stage stage = new Stage();
            stage.setTitle("🌱 Diagnostic de " + culture.getNom());
            stage.setScene(scene);
            stage.setMaximized(true); // 🔥 plein écran
            stage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
