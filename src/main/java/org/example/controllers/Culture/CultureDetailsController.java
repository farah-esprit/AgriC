package org.example.controllers.Culture;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
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

    private Culture culture;

    // Méthode pour remplir les infos
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
    }

    // Fermer la fenêtre
    @FXML
    private void fermer() {
        Stage stage = (Stage) lblNom.getScene().getWindow();
        stage.close();
    }
}
