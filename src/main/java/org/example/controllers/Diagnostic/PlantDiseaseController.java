package org.example.controllers.Diagnostic;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;

public class PlantDiseaseController {

    @FXML
    private Button btnChooseImage;

    @FXML
    private ImageView imageView;

    @FXML
    private TextArea resultArea;

    @FXML
    public void initialize() {
        btnChooseImage.setOnAction(e -> chooseImage());
    }

    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png"));
        File selectedFile = fileChooser.showOpenDialog(new Stage());
        if (selectedFile != null) {
            imageView.setImage(new Image(selectedFile.toURI().toString()));
            resultArea.setText("Analyse en cours...");
            try {
                String result = PlantDiseaseAPI.detectDisease(selectedFile.getAbsolutePath());
                resultArea.setText(result);
            } catch (Exception ex) {
                ex.printStackTrace();
                resultArea.setText("Erreur lors de l'identification de la plante ou de la maladie.");
            }
        }
    }
}
